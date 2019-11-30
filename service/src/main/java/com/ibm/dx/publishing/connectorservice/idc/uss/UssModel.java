/*******************************************************************************
 * Copyright IBM Corp. 2018
 *******************************************************************************/

package com.ibm.dx.publishing.connectorservice.idc.uss;

import java.util.ArrayList;
import java.util.HashMap;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.connectorservice.util.StringUtil;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class UssModel {
    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(UssModel.class);
    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2017";

    public static final String DATA = "data";
    public static final String IDENTIFIER = "identifier";
    public static final String PARENT_IDENTIFIER = "parentIdentifier";
    public static final String CATALOG = "catalog";
    public static final String STORE = "store";
    public static final String DISPLAY_NAME = "displayName";
    public static final String URL_NAME = "urlName";
    public static final String CHILDREN = "children";
    public static final String SEQUENCE = "sequence";
    public static final String PART_NUMBER = "partNumber";
    public static final String PRODUCT_TYPE = "productType";
    public static final String BUYABLE = "buyable";
    public static final String MANUFACTURER = "manufacturer";
    public static final String DESCRIPTION = "shortDescription";

    public static HashMap<String, Category> getCategories(RuntimeContext rc, JsonObject json) {
        LOGGER.rcEntry(rc, json);
        
        final HashMap<String, Category> result = new HashMap<>();
        if (json != null) {
            JsonArray data = json.getJsonArray(DATA);
            recursiveGetCategries(rc, data, null, result);
        }
        return LOGGER.rcExit(rc, result);
    }

    private static void recursiveGetCategries(RuntimeContext rc, JsonArray cats, String parentId, HashMap<String, Category> result) {
        LOGGER.rcEntry(rc, cats);
        
        if (cats != null) {
            for (int i=0; i<cats.size(); i++) {
                JsonObject catJson = cats.getJsonObject(i);
                Category parentCategory = (parentId != null) ? result.get(parentId) : null; 
                Category cat = new Category(rc, catJson, parentCategory);
                result.put(cat.getId(), cat);
                recursiveGetCategries(rc, catJson.getJsonArray(CHILDREN), cat.getId(), result);
            }
        }
        LOGGER.rcExit(rc);
    }

    public static ArrayList<Product> getProducts(RuntimeContext rc, JsonObject json, Category category) {
        LOGGER.rcEntry(rc, StringUtil.truncateObject(json, 2000), category.getName());
        ArrayList<Product> result = new ArrayList<>();
        if (json != null) {
            JsonArray data = json.getJsonArray(DATA);
            if (data != null) {
                result = new ArrayList<>(data.size());
                for (int i=0; i<data.size(); i++) {
                    JsonObject productJson = data.getJsonObject(i);
                    result.add(new Product(rc, productJson, category));
                }
            }
        }
        LOGGER.rcExit(rc);
        return result;
    }
    
    public static ArrayList<Product> checkHasProducts(RuntimeContext rc, JsonObject json, Category category) {
        LOGGER.rcEntry(rc, StringUtil.truncateObject(json, 1000), category.getName());
        ArrayList<Product> result = new ArrayList<>();
        if (json != null) {
            JsonArray data = json.getJsonArray(DATA);
            if (data != null) {
            	if (data.size()>0) {
            		category.setHasProducts(rc);
            	}
            	else {
                    LOGGER.rcTrace(rc, "no products");
            	}
            }
        }
        LOGGER.rcExit(rc);
        return result;
    }
    
}
