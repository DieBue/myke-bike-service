/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.idc.exceptions;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public class TemplatePageNotFoundException extends ConnectorException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int HTTP_STATUS = 404;
    private static final int ERROR_CODE = ERROR_CODE_TEMPLATE_PAGE_NOT_FOUND;

    public TemplatePageNotFoundException() {
        super(HTTP_STATUS, ERROR_CODE);
    }

    public TemplatePageNotFoundException(String ... messageParts) {
        super(HTTP_STATUS, ERROR_CODE);
        setMessageParams(messageParts);
    }

    @Override
    protected String buildMessage() {
        return "TemplatePageNotFoundException: The template page was not found: " + getMessageParams();
    }

}
