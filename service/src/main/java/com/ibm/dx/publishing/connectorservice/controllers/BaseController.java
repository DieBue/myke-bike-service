package com.ibm.dx.publishing.connectorservice.controllers;


import java.util.concurrent.CompletableFuture;

import com.ibm.dx.publishing.common.api.http.MicroServiceHttpClient;
import com.ibm.dx.publishing.common.api.http.RetryPolicy;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;

import io.vertx.core.http.HttpClientResponse;

public class BaseController {
    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(BaseController.class);

    protected final MicroServiceHttpClient client;
    protected final RetryPolicy retryPolicy;
    protected final String service;
    protected final RuntimeContext rc;

    public BaseController(RuntimeContext rc, MicroServiceHttpClient client, String service){
       this.rc = rc;
       this.client = client;
       this.service = service;
       retryPolicy = RetryPolicy.on50x(3);
    }

    protected CompletableFuture<HttpClientResponse> logResponse(HttpClientResponse res) {
        LOGGER.rcTrace(rc, "Response for service " + service + ": " + res.statusCode() + ", " + res.statusMessage());
        return CompletableFuture.completedFuture(res);
    }
}
