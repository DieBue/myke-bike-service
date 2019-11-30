/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.idc.exceptions;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public class SkipTemplatePageUpdateException extends ConnectorException  implements SkipException{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int HTTP_STATUS = 200;
    private static final int ERROR_CODE = ERROR_CODE_SKIP_TEMPLATE_PAGE_UPDATE;
    
    public SkipTemplatePageUpdateException() {
        super(HTTP_STATUS, ERROR_CODE);
    }

    @Override
    protected String buildMessage() {
        return "No need to handle this page template";
    }

}
