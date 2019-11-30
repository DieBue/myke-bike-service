/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.idc.exceptions;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public class SkipPageDeletionException extends ConnectorException  implements SkipException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static final int HTTP_STATUS = 200;
    private static final int ERROR_CODE = ERROR_CODE_SKIP_PAGE_DELETION;
    
    public SkipPageDeletionException() {
        super(HTTP_STATUS, ERROR_CODE);
    }

    @Override
    protected String buildMessage() {
        return "No need to actually delete this page";
    }

}
