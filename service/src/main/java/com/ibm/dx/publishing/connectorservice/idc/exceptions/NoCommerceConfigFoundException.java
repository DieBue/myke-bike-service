/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.idc.exceptions;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public class NoCommerceConfigFoundException extends ConnectorException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int HTTP_STATUS = 404;
    private static final int ERROR_CODE = ERROR_CODE_NO_COMMERCE_CONFIG_FOUND;
    
    public NoCommerceConfigFoundException() {
        super(HTTP_STATUS, ERROR_CODE);
    }

    @Override
    protected String buildMessage() {
        return "No commerce configuration found in TRS record.";
    }

}
