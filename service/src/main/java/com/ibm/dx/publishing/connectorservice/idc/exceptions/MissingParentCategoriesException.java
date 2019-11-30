/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.idc.exceptions;

import com.ibm.dx.publishing.connectorservice.idc.IDCConstants;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public class MissingParentCategoriesException extends ConnectorException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int HTTP_STATUS = 400;
    private static final int ERROR_CODE = ERROR_CODE_MISSING_PARENT_CATEGORIES;

    public MissingParentCategoriesException() {
        super(HTTP_STATUS, ERROR_CODE);
    }

    public MissingParentCategoriesException(String ... messageParts) {
        super(HTTP_STATUS, ERROR_CODE);
        setMessageParams(messageParts);
    }

    @Override
    protected String buildMessage() {
        return "The mandataory property \"" + IDCConstants.PARENT_CATEGORIES +"\" is missing in the following item: " + getMessageParams();
    }

}
