/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.util;


import java.util.concurrent.CompletionException;

import com.ibm.dx.publishing.common.api.ErrorMessageBuilderFactory;
import com.ibm.dx.publishing.common.api.ErrorMessageFactory;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.common.api.messages.ErrorMessage;
import com.ibm.dx.publishing.common.api.services.ServiceConstants;
import com.ibm.dx.publishing.common.utils.ErrorMessageUtil;
import com.ibm.dx.publishing.common.utils.HTTPUtil;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.ConnectorException;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Utility methods for exception and handling and error message generation.
 * @author buehlerd
 *
 */
public class ExceptionUtil {
    
    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2016";

    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(ExceptionUtil.class);

    private static final ErrorMessageUtil ERROR_MESSAGE_UTIL = ErrorMessageUtil.getInstance(ErrorMessageBuilderFactory.getInstance(), ErrorMessageFactory.getInstance());
    
    private static final String REQUEST_METHOD = "requestMethod";
    private static final String REQUEST_URI = "requestUri";
    private static final String REQUEST_ID = "requestId";
    private static final String SERVICE = "service";
    private static final String ERRORS = "errors";
    private static final String ERROR_CODE = "errorCode";
    private static final String MESSAGE = "message";
    
    private ExceptionUtil() {
    }

    public static void sendErrorMessage(final RuntimeContext rc, final HttpServerRequest incomingRequest, final HttpServerResponse httpServerResponse, Throwable t) {
        LOGGER.rcEntry(rc, incomingRequest, httpServerResponse, t);
        ErrorMessage msg = getErrorMessage(rc, t);
        Throwable cause = getCompletionExceptionCause(rc, t);
        final int statusCode = HTTPUtil.determineResponseStatusCode(cause, 500); 
        
        final String encodedErrorMessage = msg.encode();
        
        final JsonArray errors = new JsonArray();
        JsonObject responseBody = new JsonObject();
            final JsonObject error = new JsonObject();
            error.put(MESSAGE, cause.toString());
            errors.add(error);
        
        responseBody.put(ERRORS, errors);
        if (cause instanceof ConnectorException) {
            responseBody.put(ERROR_CODE, ((ConnectorException)cause).getErrorCode());
        }
        
        responseBody.put(SERVICE, "prod-publishing-connector-service");
        responseBody.put(REQUEST_ID, rc.getRequestId());
        if (incomingRequest != null) {
            responseBody.put(REQUEST_METHOD, incomingRequest.method().toString());
            responseBody.put(REQUEST_URI, incomingRequest.uri());
        }
        
        httpServerResponse.setStatusCode(statusCode).setStatusMessage(responseBody.encode()).end(responseBody.encodePrettily());
        
        LOGGER.rcRequestExit(rc, httpServerResponse, Buffer.buffer(encodedErrorMessage));
        LOGGER.rcExit(rc, msg);
	}
	
    public static ErrorMessage getErrorMessage(final RuntimeContext rc, Throwable throwable) {
        LOGGER.rcEntry(rc, throwable);
        final Throwable cause = getCompletionExceptionCause(rc, throwable);
        final String statusMessage = cause.getMessage();

        final int errorCode = (cause instanceof ConnectorException) ? ((ConnectorException)cause).getErrorCode() : 1000;
        final ErrorMessage msg;
        msg = ERROR_MESSAGE_UTIL.buildErrorMessage(rc, ServiceConstants.PUBLISHING_CONNECTOR_SERVICE_NAME, errorCode, statusMessage, null);
        
        return LOGGER.rcExit(rc, msg);
    }

    
    public static Throwable getCompletionExceptionCause(RuntimeContext rc, Throwable t) {
        LOGGER.rcEntry(rc, t);
        Throwable result = t;
        int breaker = 100;
        while (result instanceof CompletionException) {
            result = result.getCause();
            if (--breaker<0) {
                // just in case we get a cyclic CompletionException ....
                LOGGER.rcError(rc, "unexpected chain size {} of completion exceptions", breaker);
                break;
            }
        }
        return LOGGER.rcExit(rc, result);
    }

}
