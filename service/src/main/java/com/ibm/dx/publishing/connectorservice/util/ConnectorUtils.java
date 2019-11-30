package com.ibm.dx.publishing.connectorservice.util;

import java.util.ArrayList;

import com.ibm.dx.publishing.connectorservice.idc.IDCConstants;
import com.ibm.dx.publishing.connectorservice.mediation.SearchSchemaConstants;

import io.vertx.core.json.JsonObject;

public class ConnectorUtils {
    public static ArrayList<String> addToNew(ArrayList<String> target, String value) {
        ArrayList<String> result = (target != null) ? new ArrayList<String>(target) : new ArrayList<String>();
        result.add(value);
        return result;
    }
    
    public static String toSummary(JsonObject json) {
        if (json != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(SearchSchemaConstants.Common.ID).append("=").append(json.getString(SearchSchemaConstants.Common.ID));
            sb.append(" ");
            sb.append(SearchSchemaConstants.Common.NAME).append("=").append(json.getString(SearchSchemaConstants.Common.NAME));
            
            if (json.containsKey(IDCConstants.UNIQUE_ID)) {
                sb.append(" ").append(IDCConstants.UNIQUE_ID).append("=").append(json.getString(IDCConstants.UNIQUE_ID));
            }
            return sb.toString();
        }
        return "null";
    }

}
