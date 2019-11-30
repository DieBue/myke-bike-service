/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.idc.exceptions;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public class ServiceAccessException extends ConnectorException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int HTTP_STATUS = 500;
    private static final int ERROR_CODE = ERROR_CODE_SERVICE_ACCESS;

    public ServiceAccessException() {
        super(HTTP_STATUS, ERROR_CODE);
    }

    public ServiceAccessException(String ... messageParams) {
        super(HTTP_STATUS, ERROR_CODE);
        setMessageParams(messageParams);
    }

    @Override
    protected String buildMessage() {
        return "The service invocation failed. Service: " + getMessageParam(0) +  " route: " + getMessageParam(1);
    }
}
