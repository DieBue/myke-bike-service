package com.ibm.dx.publishing.connectorservice.controllers;


import com.ibm.dx.publishing.common.api.http.MicroServiceHttpClient;
import com.ibm.dx.publishing.common.api.http.RetryPolicy;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.common.api.services.ServiceConstants;
import com.ibm.dx.publishing.connectorservice.ConnectorConstants;
import com.ibm.dx.publishing.connectorservice.SearchServiceConstants;
import com.ibm.dx.publishing.connectorservice.util.ResponseUtil;
import com.ibm.wch.rx.vertx.streams.WriteStreamTransform;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Context;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpClientResponse;
import io.vertx.reactivex.core.http.HttpServerRequest;
import org.reactivestreams.Publisher;

import java.lang.reflect.Proxy;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.ibm.dx.publishing.connectorservice.ConnectorConstants.DEFAULT_REQUEST_TIMEOUT;

public class DeliverySearchController extends BaseController {
    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(DeliverySearchController.class);

    public DeliverySearchController(RuntimeContext rc, MicroServiceHttpClient client) {
        super(rc, client, ServiceConstants.SEARCH_FACADE_DELIVERY_SERVICE_NAME);
    }

    private final class SendSearchRequestTransformer implements FlowableTransformer<Buffer, HttpClientResponse> {
        private final Context context;
        private final HttpServerRequest serverRequest;

        private SendSearchRequestTransformer(final Context aContext, final HttpServerRequest aServerRequest) {
            this.context = aContext;
            this.serverRequest = aServerRequest;
        }

        @Override
        public Publisher<HttpClientResponse> apply(Flowable<Buffer> upstream) {
            return client.rxRequest(rc, HttpMethod.POST, service, SearchServiceConstants.DELIVERY_FACADE_ENTRYPOINT, RetryPolicy.NEVER, req -> {

                // Attention: ugly workaround ahead
                //
                // WriteStreamOperator (referenced by WriteStreamTransform below) overwrites the exception handler on the
                // HttpClientRequest instance. As a result, TimeoutExceptions are not propagated further.
                // This fix ensures that no one can overwrite the exception handler that is currently set.
                final HttpClientRequest reqProxy = (HttpClientRequest) Proxy.newProxyInstance(this.getClass().getClassLoader(),
                        new Class[]{HttpClientRequest.class}, ((proxy, method, args) -> {
                            if (method.getName().equals("exceptionHandler")) {
                                return req;
                            } else {
                                return method.invoke(req, args);
                            }
                        }));

                // wrap as client request from reactive package
                final io.vertx.reactivex.core.http.HttpClientRequest rxRequest = new io.vertx.reactivex.core.http.HttpClientRequest(reqProxy);

                // set chunked encoding (length not known in advance) and content type
                rxRequest.setChunked(true);
                rxRequest.setTimeout(DEFAULT_REQUEST_TIMEOUT);
                rxRequest.headers().add(ConnectorConstants.HEADER_NAME_CONTENT_TYPE, ConnectorConstants.HEADER_VALUE_APPLICATION_JSON);

                // start subscription
                upstream
                        // reset request timeout whenever a data chunk is received
                        .doOnNext(data -> rxRequest.setTimeout(DEFAULT_REQUEST_TIMEOUT))
                        .compose(WriteStreamTransform.create(() -> rxRequest, context))
                        .subscribe();

                // resume read stream
                serverRequest.resume();
            }).toFlowable().map(HttpClientResponse::newInstance);
        }
    }

    public FlowableTransformer<Buffer, HttpClientResponse> createUpdateRequest(final Context aContext, final HttpServerRequest aServerRequest) {
        return new SendSearchRequestTransformer(aContext, aServerRequest);
    }

    public CompletableFuture<Void> updateDocuments(JsonObject documents) {
        LOGGER.rcDebug(rc, com.ibm.dx.publishing.connectorservice.util.StringUtil.truncateString(documents.toString(), 2048));

        Consumer<HttpClientRequest> req = httpClientRequest -> {
            httpClientRequest.headers().add(ConnectorConstants.HEADER_NAME_CONTENT_TYPE, ConnectorConstants.HEADER_VALUE_APPLICATION_JSON);
            httpClientRequest.setTimeout(DEFAULT_REQUEST_TIMEOUT);
            httpClientRequest.end(documents.encode());
        };

        CompletableFuture<Void> result = client.put(rc, service, SearchServiceConstants.DELIVERY_FACADE_ENTRYPOINT, retryPolicy, req)
                .thenCompose(this::logResponse)
                .thenCompose(response -> ResponseUtil.validateVoidResponse(response, false));
        return LOGGER.rcExit(rc, result);
    }
}
