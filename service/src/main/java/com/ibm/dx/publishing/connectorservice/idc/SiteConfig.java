/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/

package com.ibm.dx.publishing.connectorservice.idc;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.wps.util.StringUtils;

import io.vertx.core.json.JsonObject;

public class SiteConfig {
    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2017";
    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(SiteConfig.class);


    public static final String PROP_STORE_ID = "storeId";
    public static final String PROP_STORE_IDENTIFIER = "storeIdentifier";

    private final boolean valid;
    private String storeId = null;
    private String storeIdentifier = null;
    
    public SiteConfig(RuntimeContext rc, JsonObject siteRecord) {
        LOGGER.rcEntry(rc);
        if (siteRecord != null) {
            storeId = siteRecord.getString(PROP_STORE_ID);
            storeIdentifier = siteRecord.getString(PROP_STORE_IDENTIFIER);
        }
        
        valid = StringUtils.isStringNonEmpty(storeIdentifier);
        LOGGER.rcExit(rc, toString());
    }

    public String getStoreId() {
        return storeId;
    }

    public String getStoreIdentifier() {
        return storeIdentifier;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SiteConfig [storeId=");
        builder.append(storeId);
        builder.append(", storeIdentifier=");
        builder.append(storeIdentifier);
        builder.append(", valid=");
        builder.append(valid);
        builder.append("]");
        return builder.toString();
    }
    
    
    
}
