/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/

package com.ibm.dx.publishing.connectorservice.idc;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;

import io.vertx.core.json.JsonObject;

public class TRSConfig {
    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2017";
    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(TRSConfig.class);


    public static final String PROP_IBM_COMMERCE = "ibmCommerce";
    public static final String PROP_PREVIEW_SEARCH_HOST = "previewSearchHost";
    public static final String PROP_LIVE_SEARCH_HOST = "liveSearchHost";
    public static final String PROP_PREVIEW_TRANSACTION_HOST = "previewTransactionHost";
    public static final String PROP_LIVE_TRANSACTION_HOST = "liveTransactionHost";

    private final boolean valid;
    private String previewSearchHost;
    private String liveSearchHost;
    private String previewTransactionHost;
    private String liveTransactionHost;
    
    public TRSConfig(RuntimeContext rc, JsonObject trsRecord) {
        LOGGER.rcEntry(rc);
        if (trsRecord != null) {
            JsonObject config = trsRecord.getJsonObject(PROP_IBM_COMMERCE);
            if ((config != null) && (!config.isEmpty())) {
                valid = true;
                previewSearchHost = config.getString(PROP_PREVIEW_SEARCH_HOST);
                liveSearchHost = config.getString(PROP_LIVE_SEARCH_HOST);
                previewTransactionHost = config.getString(PROP_PREVIEW_TRANSACTION_HOST);
                liveTransactionHost = config.getString(PROP_LIVE_TRANSACTION_HOST);
            }
            else {
                LOGGER.rcDebug(rc, "no IDC config found in TRS record");
                valid = false;
            }
        }
        else {
            LOGGER.rcDebug(rc, "no TRS record found");
            valid = false;
        }
        LOGGER.rcExit(rc, toString());
    }

    public boolean isValid() {
        return valid;
    }

    public String getPreviewSearchHost() {
        return previewSearchHost;
    }

    public String getLiveSearchHost() {
        return liveSearchHost;
    }

    public String getPreviewTransactionHost() {
        return previewTransactionHost;
    }

    public String getLiveTransactionHost() {
        return liveTransactionHost;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("IDCConfig [valid=");
        builder.append(valid);
        builder.append(", liveSearchHost=");
        builder.append(liveSearchHost);
        builder.append(", previewSearchHost=");
        builder.append(previewSearchHost);
        builder.append(", liveTransactionHost=");
        builder.append(liveTransactionHost);
        builder.append(", previewTransactionHost=");
        builder.append(previewTransactionHost);
        builder.append("]");
        return builder.toString();
    }

    

}
