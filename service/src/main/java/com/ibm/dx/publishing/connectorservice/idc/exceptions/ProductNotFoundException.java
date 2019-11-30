/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.idc.exceptions;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public class ProductNotFoundException extends ConnectorException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int HTTP_STATUS = 404;
    private static final int ERROR_CODE = ERROR_CODE_PRODUCT_NOT_FOUND;

    public ProductNotFoundException() {
        super(HTTP_STATUS, ERROR_CODE);
    }

    public ProductNotFoundException(String ... messageParts) {
        super(HTTP_STATUS, ERROR_CODE);
        setMessageParams(messageParts);
    }

    @Override
    protected String buildMessage() {
        return "Product not found: " + getMessageParams();
    }

}
