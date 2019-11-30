/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.mediation;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;

import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;


/**
 * Internal helper class that provides access to frequently needed utility functions.
 */
public class ResponseHelper {

    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2016";

    /**
     * Class logger
     */
    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(ResponseHelper.class);


    private ResponseHelper(){
    }

    /**
     * Validates if the response code matches the expectation.
     *
     * @param rc the runtime context
     * @param serviceName the logical name of the service that sent the {@link HttpClientResponse}
     * @param validResponseStatusCodes the set of valid response status codes (must not be empty)
     * @param clientResponse the {@link HttpClientResponse} returned by the service.
     * @return a {@link CompletableFuture} that is completed with the given {@link HttpClientResponse} instance or with an
     * {@link UnexpectedResponseCodeException}.
     */
    public static CompletableFuture<HttpClientResponse> assertResponseCode(final RuntimeContext rc,
                                                                     final String serviceName,
                                                                     final int[] validResponseStatusCodes, final HttpClientResponse clientResponse) {
        LOGGER.rcEntry(rc, serviceName, validResponseStatusCodes, clientResponse.statusCode());

        CompletableFuture<HttpClientResponse> resultFuture = new CompletableFuture<>();

        // check if actual response code is in array of valid response codes
        int actualResponseCode = clientResponse.statusCode();
        boolean responseValid = Arrays.stream(validResponseStatusCodes).filter(validResponse -> validResponse == actualResponseCode).count() > 0;

        final HttpClientRequest request = clientResponse.request();
        final String uri = request != null ? request.uri() : null;
        
        if (!responseValid) {
            LOGGER.rcWarn(rc, "service {} at uri {} returned with an unexpected status code {}", serviceName, uri, clientResponse.statusCode());
            resultFuture.completeExceptionally(new UnexpectedResponseCodeException(serviceName + clientResponse.statusCode()));

            // asynchronous logging: log body if it can be retrieved
            clientResponse.bodyHandler(event ->
                LOGGER.rcDebug(rc, "service {} at uri {} returned the following response for status code {}: {}", serviceName, uri, clientResponse.statusCode(), event)
            );

            // asynchronous logging: log exception if needed
            clientResponse.exceptionHandler(event ->
                LOGGER.rcWarn(rc, "an exception occurred while retrieving the response from " + serviceName + ".", event)
            );

            // make sure to resume response (it may have been paused before)
            clientResponse.resume();

        } else {
            resultFuture.complete(clientResponse);
        }
        return LOGGER.rcExit(rc, resultFuture);
    }

}
