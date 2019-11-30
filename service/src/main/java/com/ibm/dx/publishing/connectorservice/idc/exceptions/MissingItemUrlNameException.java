/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.idc.exceptions;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public class MissingItemUrlNameException extends ConnectorException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int HTTP_STATUS = 400;
    private static final int ERROR_CODE = ERROR_CODE_MISSING_ITEM_URL_NAME;

    public MissingItemUrlNameException() {
        super(HTTP_STATUS, ERROR_CODE);
    }

    public MissingItemUrlNameException(String ... messageParts) {
        super(HTTP_STATUS, ERROR_CODE);
        setMessageParams(messageParts);
    }

    @Override
    protected String buildMessage() {
        return "Missing urlName in item: " + getMessageParams();
    }

}
