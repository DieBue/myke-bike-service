/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.idc.exceptions;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public class InvalidCatalogNodeException extends ConnectorException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int HTTP_STATUS = 400;
    private static final int ERROR_CODE = ERROR_CODE_INVALID_CATALOG_NODE;
    
    public InvalidCatalogNodeException(String ... messageParams) {
        super(HTTP_STATUS, ERROR_CODE);
        setMessageParams(messageParams);
    }

    @Override
    protected String buildMessage() {
        return "invalid catalog node: " + getMessageParams();
    }

}
