/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.publishing.connectorservice;

import com.ibm.dx.publishing.common.api.RuntimeContextFactory;
import com.ibm.dx.publishing.common.api.ServiceRegistryClientFactory;
import com.ibm.dx.publishing.common.api.http.ServiceRegistryClient;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.connectorservice.vertx.ConnectorServiceVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.rx.java.ObservableHandler;
import io.vertx.rx.java.RxHelper;
import io.vertx.rxjava.core.Vertx;
import rx.Single;

public class Main {
    @SuppressWarnings("unused")
    private static final String               COPYRIGHT = "Copyright IBM Corp. 2017";

    private static final RuntimeContextLogger LOGGER    = RuntimeContextLogger.create(Main.class);

    private static final RuntimeContext       RC        = RuntimeContextFactory.getInstance()
        .newSystemRuntimeContext();

    private static Main                       main;

    private final Vertx                       vertx;
    private final int                         httpPort;
    private final ServiceRegistryClient       serviceRegistryClient;

    private String                            deploymentId;
    public static final int DEFAULT_USS_PAGE_SIZE = 50;

    public Main(final int httpPort) {
        this(httpPort, ServiceRegistryClientFactory.getInstance()
            .newConsulServiceRegistryClient(RC));
    }

    public Main(final int httpPort, final ServiceRegistryClient serviceRegistryClient) {
        this.httpPort = httpPort;
        this.serviceRegistryClient = serviceRegistryClient;
        vertx = Vertx.vertx(getVertxOptions());
    }

    public static void main(final String[] args) {
        main = new Main(8080);
        main.start();

        Runtime.getRuntime()
            .addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        main.stop();
                    } catch (final Exception e) {
                        LOGGER.rcError(RC, e);
                    }
                    main.shutDown();
                }
            });
    }

    public Single<Void> start() {
        LOGGER.rcDebug(RC, "Deploying ConnectorServiceVerticle...");
        final JsonObject jsonConfig = new JsonObject().put("httpPort", httpPort);
        final ConnectorServiceVerticle csVerticle = new ConnectorServiceVerticle(serviceRegistryClient, DEFAULT_USS_PAGE_SIZE);
        final ObservableHandler<AsyncResult<String>> observableHandler = RxHelper.observableHandler();
        
        vertx.getDelegate()
            .deployVerticle(csVerticle, new DeploymentOptions().setConfig(jsonConfig), observableHandler.toHandler());
        
        return observableHandler.toSingle()
            .map(AsyncResult::result)
            .doOnSuccess(id -> deploymentId = id)
            .map(id -> null);
    }

    private VertxOptions getVertxOptions() {
        final VertxOptions vertxOptions = new VertxOptions();

        // enable Vert.x application metrics for advanced monitoring capabilities
        final boolean enableApplicationMetrics = Boolean.parseBoolean(System.getenv().getOrDefault("ENABLE_VERTX_APPLICATION_METRICS", "true"));
        if (enableApplicationMetrics) {
            vertxOptions.setMetricsOptions(new DropwizardMetricsOptions().setEnabled(true));
        }
        LOGGER.rcInfo(RC, "Init Vert.x with options", vertxOptions);
        return vertxOptions;
    }

    public Single<Void> stop() {
        if (deploymentId != null) {
            return vertx.rxUndeploy(deploymentId)
                .flatMap(v -> vertx.rxClose());
        } else
            return vertx.rxClose();
    }

    public void shutDown() {
        // do nothing - stop() handles everything
    }

    public static Main getMain() {
        return main;
    }
}
