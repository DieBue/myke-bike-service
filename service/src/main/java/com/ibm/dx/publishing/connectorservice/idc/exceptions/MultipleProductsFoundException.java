/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.idc.exceptions;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public class MultipleProductsFoundException extends ConnectorException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int HTTP_STATUS = 400;
    private static final int ERROR_CODE = ERROR_CODE_MULTIPLE_PRODUCTS_FOUND;

    public MultipleProductsFoundException() {
        super(HTTP_STATUS, ERROR_CODE);
    }

    public MultipleProductsFoundException(String ... messageParts) {
        super(HTTP_STATUS, ERROR_CODE);
        setMessageParams(messageParts);
    }

    @Override
    protected String buildMessage() {
        return "Multiple products found for id: " + getMessageParams();
    }

}
