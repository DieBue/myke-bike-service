/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice;


@SuppressWarnings("unused")
public interface SearchServiceConstants {
    
    public static final String ENTRYPOINT_DELIVERY_FACADE_SETUP = "/delivery/v1/search/setup";
    
    /**
     * The name of the search collection that will be created for delivery 
     */
    public static final String SEARCHCOLLECTION_NAME_DELIVERY = "delivery";
    
    public static final String JSON_FIELD_DOCUMENTS = "documents";
    public static final String JSON_FIELD_NUMFOUND = "numFound";
    
    public static final String JSON_FIELD_FIELDS = "fields";
    public static final String JSON_FIELD_NAME = "name";
    public static final String QUERY_PARAM_FL = "fl";
    public static final String QUERY_PARAM_FQ = "fq";
    public static final String QUERY_PARAM_ROWS = "rows";
    
    public static final String AUTHORING_FACADE_ENTRYPOINT = "/authoring/v1/search";
    public static final String DELIVERY_FACADE_ENTRYPOINT = "/delivery/v1/search";
}
