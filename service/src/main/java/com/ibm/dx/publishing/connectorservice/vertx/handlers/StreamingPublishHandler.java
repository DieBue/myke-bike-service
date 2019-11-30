package com.ibm.dx.publishing.connectorservice.vertx.handlers;

import static com.ibm.portal.streaming.json.helper.JsonEventsUtils.END_DOCUMENT;
import static com.ibm.portal.streaming.json.helper.JsonEventsUtils.END_MEMBER;
import static com.ibm.portal.streaming.json.helper.JsonEventsUtils.END_OBJECT;
import static com.ibm.portal.streaming.json.helper.JsonEventsUtils.START_DOCUMENT;
import static com.ibm.portal.streaming.json.helper.JsonEventsUtils.START_OBJECT;
import static com.ibm.portal.streaming.json.helper.JsonEventsUtils.startMember;

import java.time.Instant;
import java.util.Set;

import com.ibm.dx.publishing.common.api.RuntimeContextFactory;
import com.ibm.dx.publishing.common.api.http.MicroServiceHttpClient;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.common.api.reporting.MessageBusProducerClient;
import com.ibm.dx.publishing.common.api.services.ServiceConstants;
import com.ibm.dx.publishing.connectorservice.ConnectorConstants;
import com.ibm.dx.publishing.connectorservice.controllers.DeliverySearchController;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.InvalidClassificationFormatException;
import com.ibm.dx.publishing.connectorservice.idc.uss.IdcUssConnector;
import com.ibm.dx.publishing.connectorservice.mediation.SearchSchemaConstants;
import com.ibm.dx.publishing.connectorservice.mediation.UnexpectedResponseCodeException;
import com.ibm.dx.publishing.connectorservice.util.ExceptionUtil;
import com.ibm.dx.publishing.connectorservice.vertx.ConnectorServiceVerticle;
import com.ibm.portal.streaming.json.JSONHandlerFactory;
import com.ibm.portal.streaming.json.JsonEvents;
import com.ibm.utilities.collections.sets.Sets;
import com.ibm.utilities.collections.sets.UnmodifiableSet;
import com.ibm.wch.rx.json.RxJsonUtils;
import com.ibm.wch.rx.json.parser.JsonParserTransform;
import com.ibm.wch.rx.json.serializer.InnerEventsTransform;
import com.ibm.wch.rx.json.serializer.RxJsonSerializer;
import com.ibm.wch.rx.json.serializer.SerializeJsonTransform;
import com.ibm.wch.rx.json.utils.ValidatingTransform;
import com.ibm.wch.rx.vertx.streams.StreamableTransform;
import com.ibm.wch.utilities.vertx.json.serialization.JsonObjectSerializer;
import com.ibm.wps.streaming.fasterxml.JSONHandlerFactoryImpl;
import com.ibm.wps.streaming.json.serialization.JsonPropertySerializer;
import com.ibm.wps.streaming.json.serialization.SequenceSerializer;
import com.ibm.wps.util.StringUtils;

import io.reactivex.Flowable;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Context;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClientResponse;
import io.vertx.reactivex.ext.web.RoutingContext;

public class StreamingPublishHandler implements Handler<RoutingContext> {

    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(ConnectorServiceVerticle.class);
    private final Context rxContext;
    private final MicroServiceHttpClient microServiceHttpClient;
    private final MessageBusProducerClient messageBusProducerClient;

    public StreamingPublishHandler(final Context aRxContext, final MicroServiceHttpClient aMicroserviceHttpClient, MessageBusProducerClient messageBusProducerClient) {
        this.rxContext = aRxContext;
        this.microServiceHttpClient = aMicroserviceHttpClient;
        this.messageBusProducerClient = messageBusProducerClient;
    }

    @Override
    public void handle(final RoutingContext context) {
        // request entry
        final RuntimeContext rc = RuntimeContextFactory.getInstance().newRuntimeContext(context.getDelegate());
        LOGGER.rcRequestEntry(rc, context.getDelegate());
        LOGGER.rcEntry(rc, context.request());

        // pause incoming request until we are ready to write (if we omit this, data chunks might get lost!)
        context.request().pause();

        // query params
        String forceClassificationsParam = context.getDelegate().request().getParam(ConnectorConstants.QUERY_PARAM_FORCE_CLASSIFICATIONS); // a comma separated list of classifications that should be force indexed without processing
        final UnmodifiableSet<String> forceClassifications = StringUtils.isStringNonEmpty(forceClassificationsParam) ? Sets.unmodifiableSet(forceClassificationsParam.split(",")) : Sets.emptySet();


        // instantiate some helpers
        final DeliverySearchController deliverySearchController = new DeliverySearchController(rc, microServiceHttpClient);
        final JSONHandlerFactory fct = JSONHandlerFactoryImpl.create();

        // deserialize a single object
        final JsonObjectSerializer objSer = JsonObjectSerializer.create();

        // deserialize an array
        final SequenceSerializer<JsonObject> seqSer = new SequenceSerializer<>(objSer);
        final RxJsonSerializer<JsonObject> rxSeq = RxJsonUtils.createJsonSerializer(seqSer);

        // take the "documents" element
        final JsonPropertySerializer<JsonObject> propSer = new JsonPropertySerializer<>("documents", seqSer);
        final RxJsonSerializer<JsonObject> rxSer = RxJsonUtils.createJsonSerializer(propSer);

        // transform incoming documents
        Flowable<JsonObject> rxWchObjects = context.request().toFlowable()
                .compose(StreamableTransform.create())
                .compose(JsonParserTransform.create(fct))
                // decompose into objects
                .compose(rxSer.getDeserializer())
                // business logic: filter documents
                .filter(doc -> this.filterDocument(rc, doc, forceClassifications))
                // business logic: update remaining documents
                .map(doc -> this.augmentDocument(rc, doc))
                // business logic: emit 0..n documents for single document
                .concatMap(doc -> this.duplicateDocument(rc, doc))
                // log errors
                .doOnError(err -> LOGGER.rcError(rc, "Exception while reading from input stream.", err));

        // prefix and suffix for serialization
        final Flowable<JsonEvents.UnmodifiableEvent> rxPrefix = Flowable.just(START_DOCUMENT, START_OBJECT, startMember("documents"));
        final Flowable<JsonEvents.UnmodifiableEvent> rxSuffix = Flowable.just(END_MEMBER, END_OBJECT, END_DOCUMENT);

        final Flowable<JsonEvents.UnmodifiableEvent> rxBody = rxWchObjects
                // serialize the sequence of objects into events
                .compose(rxSeq.getSerializer())
                // ignore start/end document events
                .compose(InnerEventsTransform.create());

        // sequence of events that represents our JSON structure
        final Flowable<JsonEvents.UnmodifiableEvent> rxWchEvents = Flowable.concat(rxPrefix, rxBody, rxSuffix)
                .compose(new ValidatingTransform<>());

        // data uploaded to search
        Flowable<Buffer> rxUploadContext = rxWchEvents.compose(SerializeJsonTransform.create(128))
                .map(Buffer::buffer)
                .doOnComplete(() -> LOGGER.rcInfo(rc, "Done streaming"));

        // response from search
        Single<HttpClientResponse> rxSearchResponse = rxUploadContext.compose(deliverySearchController.createUpdateRequest(rxContext, context.request())).firstOrError();

        rxSearchResponse
                .flatMap(httpClientResponse -> Single.create(emitter -> {
                    LOGGER.rcInfo(rc, "Received response status code from search: {}", httpClientResponse.statusCode());
                    httpClientResponse.endHandler(aVoid -> {
                        if (httpClientResponse.statusCode() == 201) {
                            emitter.onSuccess(Boolean.TRUE);
                        } else {
                            emitter.onError(new UnexpectedResponseCodeException("Unexpected response code from " + ServiceConstants.SEARCH_FACADE_DELIVERY_SERVICE_NAME + ": " + httpClientResponse.statusCode()));
                        }
                    });
                    httpClientResponse.exceptionHandler(err -> {
                        LOGGER.rcError(rc, "An error occurred.", err);
                        emitter.onError(err);
                    });
                }))
                .flatMap(o -> Single.create(emitter -> IdcUssConnector.getInstance()
                        .trigger(rc, microServiceHttpClient, messageBusProducerClient)
                        .whenComplete((siteProcessingContext, throwable) -> {
                            if (throwable != null) {
                                emitter.onError(throwable);
                            } else {
                                emitter.onSuccess(siteProcessingContext);
                            }
                        })))
                .map(object -> object)
                .doOnSuccess(ctx -> handleResponse(rc, context, 202))
                .doOnError(err -> {
                    LOGGER.rcError(rc, "An error occurred.", err);
                    ExceptionUtil.sendErrorMessage(rc, context.getDelegate().request(), context.getDelegate().response(), err);                })
                .subscribe();
    }

    private void handleResponse(final RuntimeContext rc, final RoutingContext ctx, final int statusCode) {
        ctx.request().response().setStatusCode(statusCode).end();
        LOGGER.rcRequestExit(rc, ctx.request().response().getDelegate());
    }

    /**
     * Sample function that shows how to filter documents from an incoming stream.
     *
     * @return <code>true</code> if the document should be forwarded, <code>false</code> otherwise
     */
    private boolean filterDocument(final RuntimeContext rc, final JsonObject doc, final Set<String> forceClassifications) throws InvalidClassificationFormatException {
        try {
            final String classification = doc.getString(SearchSchemaConstants.Common.CLASSIFICATION);
            if (StringUtils.isStringNonEmpty(classification)) {
                if (ConnectorConstants.ALLOWED_CLASSIFICATIONS.contains(classification)) {
                    return true;
                } else {
                    return forceClassifications.contains(classification);
                }
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new InvalidClassificationFormatException(doc.encode());
        }
    }

    /**
     * Sample function that shows how to forward multiple (0..n) documents for a single incoming document.
     *
     * @return {@link Flowable} that emits 0..n {@link JsonObject}s for the provided object
     */
    private Flowable<JsonObject> duplicateDocument(final RuntimeContext rc, final JsonObject doc) {
        final boolean copyDoc = doc.getBoolean("copyDoc", false);
        if (copyDoc) {
            final JsonObject aCopy = doc.copy();
            return Flowable.just(doc, aCopy);
        } else {
            return Flowable.just(doc);
        }
    }

    /**
     * Sample function that shows how to update a document from an incoming stream before it is forwarded.
     *
     * @return the updated document
     */
    private JsonObject augmentDocument(final RuntimeContext rc, final JsonObject doc) {
        LOGGER.rcEntry(rc, doc);
        final String id = doc.getString(SearchSchemaConstants.Common.ID);
        final String classification = doc.getString(SearchSchemaConstants.Common.CLASSIFICATION);
        final String docId = classification + ":" + id;
        doc.put(SearchSchemaConstants.Common.__DOC_ID__, docId);
        doc.put(SearchSchemaConstants.Common.SYSTEM_MODIFIED, Instant.now().toString());
        return LOGGER.rcExit(rc, doc);
    }
}
