/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.idc.exceptions;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public class MultipleCatalogNodesFound extends ConnectorException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int HTTP_STATUS = 404;
    private static final int ERROR_CODE = ERROR_CODE_MULTIPLE_CATALOG_NODES;
    
    public MultipleCatalogNodesFound() {
        super(HTTP_STATUS, ERROR_CODE);
    }

    @Override
    protected String buildMessage() {
        return "Multiple catalog pages found.";
    }

}
