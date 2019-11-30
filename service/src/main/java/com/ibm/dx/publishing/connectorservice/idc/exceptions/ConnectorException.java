/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.idc.exceptions;

import java.util.Arrays;

import com.ibm.dx.publishing.common.api.util.HTTPStatusCodeProvider;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public abstract class ConnectorException extends Exception implements HTTPStatusCodeProvider {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private static final String NA = "n/a";
    
    String[] messageParams;
    String message;
    int statusCode;
    int errorCode;
    
    // missing config
    public static final int ERROR_CODE_NO_CATALOG_NODE = 1;
    public static final int ERROR_CODE_NO_CATEGORIES_FOUND = 2;
    public static final int ERROR_CODE_NO_SITE_CONFIG_FOUND = 3;
    public static final int ERROR_CODE_NO_TEMPLATE_PAGES_FOUND = 4;
    public static final int ERROR_CODE_NO_COMMERCE_CONFIG_FOUND = 5;

    // invlaid config
    public static final int ERROR_CODE_INVALID_CATALOG_NODE = 100;
    public static final int ERROR_CODE_MULTIPLE_CATALOG_NODES = 101;
    public static final int ERROR_CODE_TEMPLATE_PAGE_NOT_FOUND = 102;


    // invalid import data
    public static final int ERROR_CODE_MISSING_ITEM_DISPLAYY_NAME = 500;
    public static final int ERROR_CODE_MISSING_ITEM_ID = 501;
    public static final int ERROR_CODE_MISSING_ITEM_URL_NAME = 502;
    public static final int ERROR_CODE_SCHEMA_VALIDATION = 503;
    public static final int ERROR_CODE_MISSING_PARENT_CATEGORIES = 504;

    // invalid invocation
    public static final int ERROR_CODE_MISSING_PARAMETER = 1000;
    public static final int ERROR_CODE_INVALID_CLASSIFICATION_FORMAT = 1001;

    // data not found
    public static final int ERROR_CODE_OBJECT_NOT_FOUND = 4000;
    public static final int ERROR_CODE_CATEGORY_NOT_FOUND = 4001;
    public static final int ERROR_CODE_PRODUCT_NOT_FOUND = 4002;
    public static final int ERROR_CODE_MULTIPLE_PRODUCTS_FOUND = 4003;

    // server errors
    public static final int ERROR_CODE_OUTBOUND_REQUEST_FAILED = 50001;
    public static final int ERROR_CODE_SERVICE_ACCESS = 50002;

    // skip exceptions
    public static final int ERROR_CODE_SKIP_PAGE_DELETION = 20000;
    public static final int ERROR_CODE_SKIP_TEMPLATE_PAGE_UPDATE = 20001;
    public static final int ERROR_CODE_SKIP_ON_MISSING_CONFIG = 20002;
    
    
    public ConnectorException(int statusCode, int errorCode) {
        super();
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public ConnectorException(int statusCode, int errorCode, String message, Throwable cause, String ... messageParams) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.messageParams = messageParams;
    }

    public ConnectorException(int statusCode, int errorCode, String message, String ... messageParams) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
        this.messageParams = messageParams;
    }

    public ConnectorException(int statusCode, int errorCode, Throwable cause) {
        super(cause);
    }
    
    public void setMessageParams(String[] messageParams) {
        this.messageParams = messageParams;
    }
    
    protected abstract String buildMessage();
    
    protected String getMessageParam(int i) {
        if ((messageParams != null) && (messageParams.length<i)) {
            return messageParams[i];
        }
        else {
            return NA;
        }
    }

    protected String getMessageParams() {
        if (messageParams != null) {
            return Arrays.asList(messageParams).toString();
        }
        else {
            return NA;
        }
    }

    
    public int getErrorCode() {
        return errorCode;
    }

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        return buildMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return buildMessage();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConnectorException msg: ").append(buildMessage()).append(" status: ").append(statusCode).append(super.toString());
        return sb.toString();
    }
    
    
}
