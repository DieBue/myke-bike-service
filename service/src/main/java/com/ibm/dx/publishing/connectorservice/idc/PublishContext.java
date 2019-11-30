    package com.ibm.dx.publishing.connectorservice.idc;

import java.time.Instant;
import java.util.Set;

import com.ibm.dx.publishing.common.api.http.MicroServiceHttpClient;
import com.ibm.dx.publishing.common.api.http.ServiceRegistryClient;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.connectorservice.controllers.DeliverySearchController;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;

public class PublishContext {
    
    private final MicroServiceHttpClient microServiceHttpClient;
    private final RuntimeContext rc;
    private final ServiceRegistryClient serviceRegistryClient;
    private final RoutingContext context;
    private final Vertx vertx;
    private final Set<String> forceClassifications;
    private final DeliverySearchController deliverySearchController;
    private final String timeStamp = Instant.now().toString();
    
    public PublishContext(RuntimeContext rc, Vertx vertx, RoutingContext context, Set<String> forceClassifications, ServiceRegistryClient serviceRegistryClient, MicroServiceHttpClient microServiceHttpClient) {
        this.rc = rc;
        this.context = context;
        this.microServiceHttpClient = microServiceHttpClient;
        deliverySearchController = new DeliverySearchController(rc, microServiceHttpClient);
        this.serviceRegistryClient = serviceRegistryClient;
        this.forceClassifications = forceClassifications;
        this.vertx = vertx;
        
    }

    public MicroServiceHttpClient getMicroServiceHttpClient() {
        return microServiceHttpClient;
    }

    public RuntimeContext getRc() {
        return rc;
    }

    public ServiceRegistryClient getServiceRegistryClient() {
        return serviceRegistryClient;
    }

    public Vertx getVertx() {
        return vertx;
    }

    public RoutingContext getContext() {
        return context;
    }
    
    public Set<String> getForceClassifications() {
        return forceClassifications;
    }

    public JsonObject getSucessResult() {
        return new JsonObject().put("status","success");
    }

    public DeliverySearchController getDeliverySearchController() {
        return deliverySearchController;
    }

    public String getTimeStamp() {
        return timeStamp;
    }


}
