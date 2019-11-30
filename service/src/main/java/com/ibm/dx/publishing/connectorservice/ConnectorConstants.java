/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice;


import com.ibm.utilities.collections.sets.Sets;
import io.vertx.core.http.HttpClientRequest;

import java.util.Set;
import java.util.function.Consumer;

public final class ConnectorConstants {

    public static final String HEADER_NAME_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_VALUE_APPLICATION_JSON = "application/json";

    public static final String QUERY_PARAM_FORCE_CLASSIFICATIONS = "forceClassifications";
    public static final Set<String> ALLOWED_CLASSIFICATIONS = Sets.unmodifiableStringSet("product", "category", "group");

    public static final Long DEFAULT_REQUEST_TIMEOUT = 10000L;
    public static final Consumer<HttpClientRequest> DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT = httpClientRequest ->  {
        httpClientRequest.setTimeout(DEFAULT_REQUEST_TIMEOUT);
        httpClientRequest.end();
    };

    private ConnectorConstants() {
    }
}
