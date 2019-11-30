/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.publishing.connectorservice.idc.uss;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;

import io.vertx.core.json.JsonObject;

public class ExternalContext {
    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2017";

    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(ExternalContext.class);

    public static JsonObject asJson(RuntimeContext rc, ProcessingContext pc, Item i) {
        LOGGER.rcEntry(rc, i);
        JsonObject result = new JsonObject();
        result.put("type", i.getType());
        result.put("id", i.getId());
        result.put("identifier", i.getId());
        result.put("baseUrl", assertTrailingSlash(pc.getTRSConfig().getLiveSearchHost()));
        result.put("previewUrl", assertTrailingSlash(pc.getTRSConfig().getPreviewSearchHost()));
        result.put("storeId", pc.getSiteConfig().getStoreId());
        result.put("storeIdentifier", pc.getSiteConfig().getStoreIdentifier());
        return LOGGER.rcExit(rc, result);
    }
    
    private static String assertTrailingSlash(String str) {
        if (str == null) {
            return "/";
        }
        else {
            return str.endsWith("/") ? str : str + "/";
        }
    }
}
