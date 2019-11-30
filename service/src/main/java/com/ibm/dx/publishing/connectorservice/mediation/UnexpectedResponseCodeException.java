/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.mediation;

/**
 * Exceptions representing unexpected response codes from other micro-services.
 */
public class UnexpectedResponseCodeException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public UnexpectedResponseCodeException() {
        super();
        // TODO Auto-generated constructor stub
    }

    public UnexpectedResponseCodeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        // TODO Auto-generated constructor stub
    }

    public UnexpectedResponseCodeException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public UnexpectedResponseCodeException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public UnexpectedResponseCodeException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

}
