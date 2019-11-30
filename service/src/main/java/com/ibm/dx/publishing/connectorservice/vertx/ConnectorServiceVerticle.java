/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.publishing.connectorservice.vertx;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.http.HttpStatus;

import com.ibm.dx.publishing.common.api.MessageBusProducerClientFactory;
import com.ibm.dx.publishing.common.api.MicroServiceHttpClientFactory;
import com.ibm.dx.publishing.common.api.RuntimeContextFactory;
import com.ibm.dx.publishing.common.api.http.MicroServiceHttpClient;
import com.ibm.dx.publishing.common.api.http.ServiceRegistryClient;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.common.api.reporting.MessageBusProducerClient;
import com.ibm.dx.publishing.common.impl.handlers.ServerMetricsHandler;
import com.ibm.dx.publishing.connectorservice.Main;
import com.ibm.dx.publishing.connectorservice.idc.Constants;
import com.ibm.dx.publishing.connectorservice.idc.DataAccessor;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.MissingParameterException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.NoCatalogNodeFoundException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.SkipException;
import com.ibm.dx.publishing.connectorservice.idc.uss.IdcUssConnector;
import com.ibm.dx.publishing.connectorservice.idc.uss.SiteProcessingContext;
import com.ibm.dx.publishing.connectorservice.util.ExceptionUtil;
import com.ibm.dx.publishing.connectorservice.vertx.handlers.StreamingPublishHandler;
import com.ibm.dx.search.vertx.http.apidoc.ApiDocProvider;
import com.ibm.dx.search.vertx.http.dump.DumpProvider;
import com.ibm.wps.util.StringUtils;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.MetricsService;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.StaticHandler;

public class ConnectorServiceVerticle extends AbstractVerticle {

    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2017";

    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(ConnectorServiceVerticle.class);

    private static final RuntimeContext systemRc = RuntimeContextFactory.getInstance().newSystemRuntimeContext();

    public static final String REST_ENDPOINT_V1 = "/connectorservice/v1";

    private final ServiceRegistryClient serviceRegistryClient;

    private HttpServer httpServer;
    private HttpClient httpClient;
    private MicroServiceHttpClient microServiceHttpClient;
    private MessageBusProducerClient messageBusProducerClient;
    private final int ussPageSize;
    

    public ConnectorServiceVerticle(final ServiceRegistryClient serviceRegistryClient, int ussPageSize) {
    	this(serviceRegistryClient, null, ussPageSize);
    }

    public ConnectorServiceVerticle(final ServiceRegistryClient serviceRegistryClient, MessageBusProducerClient messageBusProducerClient, int ussPageSize) {
        this.serviceRegistryClient = serviceRegistryClient;
        this.messageBusProducerClient = messageBusProducerClient;
        this.ussPageSize = ussPageSize;
    }

    @Override
    public void start(final Future<Void> startFuture) throws Exception {
        LOGGER.rcDebug(systemRc, "Starting ConnectorServiceVerticle...");

        try {

            final int httpPort = determinePort();
            httpClient = vertx.createHttpClient(getDefaultHttpClientOptions(systemRc));
            microServiceHttpClient = MicroServiceHttpClientFactory.getInstance().newMicroserviceHttpClient(systemRc, vertx.getDelegate(), serviceRegistryClient, null, null);
            if (messageBusProducerClient == null) {
                messageBusProducerClient = MessageBusProducerClientFactory.getInstance().newConsulProducerClient(systemRc, this.serviceRegistryClient);
            }

            httpServer = vertx.createHttpServer();
            final Router router = createRouter();
            httpServer.requestHandler(router::accept);
            httpServer.rxListen(httpPort).doOnSuccess(v -> startFuture.complete())
                    // always complete successfully. In case of failure, marathon will restart the service.
                    .subscribe(h -> LOGGER.rcInfo(systemRc, "Verticle {} listening on port {}", getClass().getSimpleName(), httpPort), t -> LOGGER.rcCatching(systemRc, t));

        } catch (final Exception e) {
            LOGGER.rcCatching(systemRc, e);
            startFuture.fail(e);
        }
    }

    @Override
    public void stop() {
        httpServer.close();
        httpClient.close();
    }

    private Router createRouter() {
        final Router router = Router.router(vertx);
        ServerMetricsHandler serverMetrics = new ServerMetricsHandler(MetricsService.create(vertx.getDelegate()));
        router.get("/metrics/:name").handler(ctx -> serverMetrics.handleRoute(ctx.getDelegate()));
        router.get("/test/marathon").handler(this::handleTestMarathon);
        router.get("/test/consul").handler(this::handleTestConsul);
        router.post("/publishing/v1/connector/idc/trigger").handler(this::handleUpdateTrigger);
        router.delete("/publishing/v1/connector/idc/trigger").handler(this::handleDeleteTrigger);
        router.post("/publishing/v1/connector/idc/templates/:id").handler(this::handleUpdateTemplate);
        router.delete("/publishing/v1/connector/idc/templates/:id").handler(this::handleDeleteTemplate);
        router.getDelegate().route("/publishing/v1/connector/idc/publish").handler(BodyHandler.create());
        router.post("/publishing/v2/connector/idc/publish").handler(new StreamingPublishHandler(vertx.getOrCreateContext(), microServiceHttpClient, messageBusProducerClient));

        // static route to the API doc
        router.route("/v1/api/*").handler(StaticHandler.create("api-doc/v1").setDirectoryListing(true));

        final ApiDocProvider apiDocProvider = new ApiDocProvider("api-doc/v1/service_v1.yaml", REST_ENDPOINT_V1, context);
        apiDocProvider.register(router.getDelegate(), true);

        final DumpProvider dumpProvider = new DumpProvider();
        dumpProvider.register(router.getDelegate());

        router.route().handler(this::handleNotRouted);

        return router;
    }

    private void handleUpdateTemplate(final RoutingContext context) {
        io.vertx.ext.web.RoutingContext ctx = context.getDelegate();
        final RuntimeContext rc = RuntimeContextFactory.getInstance().newRuntimeContext(ctx);
        LOGGER.rcRequestEntry(rc, context.getDelegate());
        LOGGER.rcEntry(rc, context.request());

        String id = context.request().getParam("id");
        if (!StringUtils.isStringNonEmpty(id)) {
            MissingParameterException th = new MissingParameterException("id");
            ExceptionUtil.sendErrorMessage(rc, ctx.request(), ctx.response(), th);
        } else {
            boolean skipOnMissingConfig = Boolean.parseBoolean(context.request().getParam(Constants.CONNECTOR_PARAM_SKIP_ON_MSSING_CONFIG));
            final SiteProcessingContext spc = new SiteProcessingContext(rc, null, microServiceHttpClient, messageBusProducerClient, id, ussPageSize, skipOnMissingConfig);
            IdcUssConnector.getInstance().publishItemOverride(spc).thenAccept(r -> {
                ctx.response().setStatusCode(200);
                final String responseMessage = r.toResult().encode();
                ctx.response().headers().add("Content-Type", "application/json");
                ctx.response().end(responseMessage);
                LOGGER.rcRequestExit(rc, context.getDelegate().response());
            }).exceptionally(th -> {
                Throwable cause = ExceptionUtil.getCompletionExceptionCause(rc, th);
                if (cause instanceof SkipException) {
                    LOGGER.rcTrace(rc, "Skipping  ...");
                    ctx.response().setStatusCode(200);
                    ctx.response().end();
                    LOGGER.rcRequestExit(rc, context.getDelegate().response());
                }
                else {
                    LOGGER.rcError(rc, th);
                    LOGGER.rcCatching(rc, th);
                    ExceptionUtil.sendErrorMessage(rc, ctx.request(), ctx.response(), th);
                }
                return null;
            });
        }

        LOGGER.rcExit(rc, context.request());
    }
    
    private void handleDeleteTemplate(final RoutingContext context) {
        io.vertx.ext.web.RoutingContext ctx = context.getDelegate();
        final RuntimeContext rc = RuntimeContextFactory.getInstance().newRuntimeContext(ctx);
        LOGGER.rcRequestEntry(rc, context.getDelegate());
        LOGGER.rcEntry(rc, context.request());

        String id = context.request().getParam("id");
        if (!StringUtils.isStringNonEmpty(id)) {
            MissingParameterException th = new MissingParameterException("id");
            ExceptionUtil.sendErrorMessage(rc, ctx.request(), ctx.response(), th);
        } else {
            boolean skipOnMissingConfig = Boolean.parseBoolean(context.request().getParam(Constants.CONNECTOR_PARAM_SKIP_ON_MSSING_CONFIG));
            final SiteProcessingContext spc = new SiteProcessingContext(rc, null, microServiceHttpClient, messageBusProducerClient, id, ussPageSize, skipOnMissingConfig);

            IdcUssConnector.getInstance().deleteItemOverride(spc).thenAccept(r -> {
                ctx.response().setStatusCode(204);
                ctx.response().end();
                LOGGER.rcRequestExit(rc, context.getDelegate().response());
            }).exceptionally(th -> {
                if (ExceptionUtil.getCompletionExceptionCause(rc, th) instanceof NoCatalogNodeFoundException) {
                    LOGGER.rcTrace(rc, "Skipping since no catalog page is defined ...");
                    ctx.response().setStatusCode(200);
                    ctx.response().end();
                    LOGGER.rcRequestExit(rc, context.getDelegate().response());
                }
                else if (ExceptionUtil.getCompletionExceptionCause(rc, th) instanceof SkipException) {
                    LOGGER.rcTrace(rc, "Skipping ...");
                    ctx.response().setStatusCode(204);
                    ctx.response().end();
                    LOGGER.rcRequestExit(rc, context.getDelegate().response());
                }
                else {
                    LOGGER.rcError(rc, th);
                    LOGGER.rcCatching(rc, th);
                    ExceptionUtil.sendErrorMessage(rc, ctx.request(), ctx.response(), th);
                }
                return null;
            });
        }

        LOGGER.rcExit(rc, context.request()); 
    }
    
	private void handleUpdateTrigger(final RoutingContext context) {
		io.vertx.ext.web.RoutingContext ctx = context.getDelegate();
		final RuntimeContext rc = RuntimeContextFactory.getInstance().newRuntimeContext(ctx);
		LOGGER.rcRequestEntry(rc, context.getDelegate());
		LOGGER.rcEntry(rc, context.request());

		boolean skipOnMissingConfig = Boolean.parseBoolean(context.request().getParam(Constants.CONNECTOR_PARAM_SKIP_ON_MSSING_CONFIG));

		final CompletableFuture<List<SiteProcessingContext>> sites = getSiteProcessingContexts(rc, microServiceHttpClient, messageBusProducerClient, ussPageSize, skipOnMissingConfig); 
		
		IdcUssConnector.getInstance().trigger(rc, sites).thenAccept(r -> {
			int status = containsSkippedContext(rc, sites) ? 204 : 200;
			ctx.response().setStatusCode(status);
			final String responseMessage = new JsonObject().put("status", "success").encode();
			ctx.response().headers().add("Content-Type", "application/json");
			ctx.response().end(responseMessage);
			LOGGER.rcRequestExit(rc, context.getDelegate().response());
		}).exceptionally(th -> {
			if (ExceptionUtil.getCompletionExceptionCause(rc, th) instanceof SkipException) {
				LOGGER.rcTrace(rc, "Skipping ...");
				ctx.response().setStatusCode(204);
				ctx.response().end();
				LOGGER.rcRequestExit(rc, context.getDelegate().response());
			} else {
				LOGGER.rcError(rc, th);
				LOGGER.rcCatching(rc, th);
				ExceptionUtil.sendErrorMessage(rc, ctx.request(), ctx.response(), th);
			}
			return null;
		});
		LOGGER.rcExit(rc, context.request());
	}
	

	private boolean containsSkippedContext(RuntimeContext rc, CompletableFuture<List<SiteProcessingContext>> sites) {
		LOGGER.rcEntry(rc);
		boolean result = false;
		List<SiteProcessingContext> lst = sites.getNow(null);
		if (lst != null) {
			for (SiteProcessingContext siteProcessingContext : lst) {
				if (siteProcessingContext.isSkipped()) {
					result = true;
					break;
				}
			}
		}
		return LOGGER.rcExit(rc, result);
	}

	public static CompletableFuture<List<SiteProcessingContext>> getSiteProcessingContexts(RuntimeContext rc, MicroServiceHttpClient client, MessageBusProducerClient messageBusClient, boolean skipOnMissingConfig) {
		return getSiteProcessingContexts(rc, client, messageBusClient, Main.DEFAULT_USS_PAGE_SIZE, skipOnMissingConfig);
	}

	public static CompletableFuture<List<SiteProcessingContext>> getSiteProcessingContexts(RuntimeContext rc, MicroServiceHttpClient client, MessageBusProducerClient messageBusClient, int ussPageSize, boolean skipOnMissingConfig) {
		LOGGER.rcEntry(rc);
		CompletableFuture<List<SiteProcessingContext>> result = new CompletableFuture<List<SiteProcessingContext>>();
		DataAccessor.getInstance().loadSites(rc, client).thenAccept(sites -> {
			JsonArray items = sites.getJsonArray(Constants.AUTHORING_SITES_ITEMS);
			if ((items != null) && (items.size() > 0)) {
				ArrayList<SiteProcessingContext> lst = new ArrayList<>(items.size());
				for (int i = 0; i < items.size(); i++) {
					JsonObject site = items.getJsonObject(i);
					if ((site.getString("id") != null) && (!site.getString("id").contains(":"))) {
						LOGGER.rcTrace(rc, "Adding site: " + site);
						lst.add(new SiteProcessingContext(rc, site.getString("id"), client, messageBusClient, ussPageSize, skipOnMissingConfig));
					}
					else {
						LOGGER.rcTrace(rc, "Skipping site: " + site);
					}
				}
				result.complete(lst);
			} else {
				LOGGER.rcTrace(rc, "Not sites found. Returning empty list.");
				result.complete(Collections.emptyList());
			}
		}).exceptionally(th -> {
			LOGGER.rcCatching(rc, th);
			result.completeExceptionally(th);
			return null;
		});
		return LOGGER.rcExit(rc, result);
	}

	private void handleDeleteTrigger(final RoutingContext context) {
        io.vertx.ext.web.RoutingContext ctx = context.getDelegate();
        final RuntimeContext rc = RuntimeContextFactory.getInstance().newRuntimeContext(ctx);
        LOGGER.rcRequestEntry(rc, context.getDelegate());
        LOGGER.rcEntry(rc, context.request());

        boolean skipOnMissingConfig = Boolean.parseBoolean(context.request().getParam(Constants.CONNECTOR_PARAM_SKIP_ON_MSSING_CONFIG));
        final SiteProcessingContext spc = new SiteProcessingContext(rc, null, microServiceHttpClient, messageBusProducerClient, ussPageSize, skipOnMissingConfig);

        IdcUssConnector.getInstance().delete(spc).thenAccept(r -> {
            ctx.response().setStatusCode(204);
            ctx.response().end();
            LOGGER.rcRequestExit(rc, context.getDelegate().response());
        }).exceptionally(th -> {
            if (ExceptionUtil.getCompletionExceptionCause(rc, th) instanceof SkipException) {
                LOGGER.rcTrace(rc, "Skipping ...");
                ctx.response().setStatusCode(204);
                ctx.response().end();
                LOGGER.rcRequestExit(rc, context.getDelegate().response());
            } else {
                LOGGER.rcError(rc, th);
                LOGGER.rcCatching(rc, th);
                ExceptionUtil.sendErrorMessage(rc, ctx.request(), ctx.response(), th);
            }
            return null;
        });
        LOGGER.rcExit(rc, context.request());
    }
    

    private void handleTestMarathon(final RoutingContext context) {
        final RuntimeContext rc = RuntimeContextFactory.getInstance().newRuntimeContext(context.getDelegate());
        LOGGER.rcRequestEntry(rc, context.getDelegate());

        final HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json").end(new JsonObject().put("Stauts", "OK").encodePrettily());

        LOGGER.rcRequestExit(rc, response.getDelegate());
    }

    private void handleTestConsul(final RoutingContext context) {
        final RuntimeContext rc = RuntimeContextFactory.getInstance().newRuntimeContext(context.getDelegate());
        LOGGER.rcRequestEntry(rc, context.getDelegate());

        final HttpServerResponse response = context.response();
        response.putHeader("Content-Type", "application/json").end(new JsonObject().put("Stauts", "OK").encodePrettily());

        LOGGER.rcRequestExit(rc, response.getDelegate());
    }

    private void handleNotRouted(final RoutingContext context) {
        final RuntimeContext rc = RuntimeContextFactory.getInstance().newRuntimeContext(context.getDelegate());
        LOGGER.rcRequestEntry(rc, context.getDelegate());

        final HttpServerResponse response = context.response();
        response.setStatusCode(HttpStatus.SC_NOT_FOUND).putHeader("Content-Type", "application/json").end(new JsonObject().put("Stauts", "NOT FOUND").encodePrettily());

        LOGGER.rcRequestExit(rc, response.getDelegate());
    }

    private int determinePort() throws IOException {
        int port = config().getInteger("httpPort");
        if (port == 0) {
            final ServerSocket socket = new ServerSocket(0);
            port = socket.getLocalPort();
            socket.close();
        }

        return port;
    }

    static HttpClientOptions getDefaultHttpClientOptions(final RuntimeContext rc) {
        LOGGER.rcEntry(rc);

        // retrieve default values for compression and connection timeout
        // it's up to the discretion of each microservice owner to expose those
        // configuration options in their troubleshooting guidelines
        boolean defaultUseCompression = Boolean.parseBoolean(System.getenv().getOrDefault("MS_CLIENT_USE_COMPRESSION", "true"));
        boolean defaultUseKeepAlive = Boolean.parseBoolean(System.getenv().getOrDefault("MS_CLIENT_USE_KEEPALIVE", "true"));
        int defaultConnectTimeout = Integer.parseInt(System.getenv().getOrDefault("MS_CLIENT_CONNECT_TIMEOUT_MILLIS", "10000"));
        int defaultIdleTimeout = Integer.parseInt(System.getenv().getOrDefault("MS_CLIENT_IDLE_TIMEOUT_SECONDS", "600"));
        boolean defaultSetLogActivity = Boolean.parseBoolean(System.getenv().getOrDefault("MS_CLIENT_SET_LOG_ACTIVITY", "false"));
        int defaultMaxPoolSize = Integer.parseInt(System.getenv().getOrDefault("MS_CLIENT_MAX_POOLSIZE", "20"));

        final HttpClientOptions defaultHttpClientOptions = new HttpClientOptions().setTryUseCompression(defaultUseCompression).setConnectTimeout(defaultConnectTimeout).setKeepAlive(defaultUseKeepAlive).setIdleTimeout(defaultIdleTimeout).setLogActivity(defaultSetLogActivity).setVerifyHost(false).setTrustAll(true).setMaxPoolSize(defaultMaxPoolSize);

        LOGGER.rcDebug(rc, "explicitly set options: HttpClientOptions[tryUseCompression={}, connectTimeout={}, keepAlive={}, idleTimeout={}, logActivitiy={}, maxPoolSize={}]", defaultHttpClientOptions.isTryUseCompression(), defaultHttpClientOptions.getConnectTimeout(), defaultHttpClientOptions.isKeepAlive(), defaultHttpClientOptions.getIdleTimeout(), defaultHttpClientOptions.getLogActivity(), defaultHttpClientOptions.getMaxPoolSize());

        return LOGGER.rcExit(rc, defaultHttpClientOptions);
    }

	public int getUssPageSize() {
		return ussPageSize;
	}
}
