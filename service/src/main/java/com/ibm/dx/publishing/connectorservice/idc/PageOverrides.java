package com.ibm.dx.publishing.connectorservice.idc;

import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.connectorservice.idc.IDCConstants.Type;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.InvalidCatalogNodeException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.TemplatePageNotFoundException;
import com.ibm.dx.publishing.connectorservice.util.ConnectorUtils;
import com.ibm.dx.publishing.connectorservice.util.StringUtil;
import com.ibm.wps.util.StringUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/*
 * Sample JSON
 * 
{
    "defaults": {
        "category": "11111",
        "product": "222"
        "sku": "3333",
        "bundle": "4444444",
        "kit": "55555",
        "categorywithproducts":"666666"
    },
    "itemOverrides": {
        "idc-id-111111": "page-id-2222"
    },
}
 */

public class PageOverrides {
    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(PageOverrides.class);

    private final EnumMap<IDCConstants.Type, String> defaultMappings;
    private final HashMap<String, String> itemMappings;
    private HashMap<String, JsonObject> templatePages;

    public PageOverrides(RuntimeContext rc, JsonObject defaultCatalogPageIds, JsonArray itemOverrides) throws InvalidCatalogNodeException {
        LOGGER.rcEntry(rc, defaultCatalogPageIds);

        defaultMappings = parseDefaults(rc, defaultCatalogPageIds);
        templatePages = new HashMap<>();
        itemMappings = new HashMap<String, String>();
        parseItemOverrides(rc, itemOverrides);

        LOGGER.rcDebug(rc, toString());
        LOGGER.rcExit(rc);
    }

    private void parseItemOverrides(RuntimeContext rc, JsonArray itemOverrides) throws InvalidCatalogNodeException {
        LOGGER.rcEntry(rc, StringUtil.truncateObject(itemOverrides, 2000));
        if (itemOverrides != null) {
            for (int i=0; i<itemOverrides.size(); i++) {
                JsonObject page = itemOverrides.getJsonObject(i);
                String id = page.getString("id");
                itemMappings.put(page.getString(Constants.AUTHORING_SITES_CATALOG_RESOURCE_ID), id);
                templatePages.put(id, page);
            }
        }
        LOGGER.rcExit(rc, StringUtil.truncateObject(templatePages, 4000));
    }

    private EnumMap<IDCConstants.Type, String> parseDefaults(RuntimeContext rc, JsonObject defaultCatalogPageIds) throws InvalidCatalogNodeException {
        LOGGER.rcEntry(rc, defaultCatalogPageIds);
        EnumMap<IDCConstants.Type, String> result = new EnumMap<>(IDCConstants.Type.class);
        if (defaultCatalogPageIds != null) {
            addDefaultMapping(rc, Type.bundle, defaultCatalogPageIds, result);
            addDefaultMapping(rc, Type.category, defaultCatalogPageIds, result);
            addDefaultMapping(rc, Type.categorywithproducts, defaultCatalogPageIds, result);
            addDefaultMapping(rc, Type.kit, defaultCatalogPageIds, result);
            addDefaultMapping(rc, Type.product, defaultCatalogPageIds, result);
            addDefaultMapping(rc, Type.sku, defaultCatalogPageIds, result);
        } else {
            throw new InvalidCatalogNodeException("Default template page IDs missing");
        }
        return LOGGER.rcExit(rc, result);
    }

    private void addDefaultMapping(RuntimeContext rc, Type type, JsonObject defaultCatalogPageIds, EnumMap<Type, String> result) throws InvalidCatalogNodeException {
        LOGGER.rcEntry(rc, type, defaultCatalogPageIds);
        String id = defaultCatalogPageIds.getString(PageKinds.fromType(type));
        if (StringUtils.isStringNonEmpty(id)) {
            result.put(type, id);
        } else {
            throw new InvalidCatalogNodeException("Default template page IDs missing for type " + type);
        }
        LOGGER.rcExit(rc, result);
    }

    public boolean isDefaultMapping(RuntimeContext rc, String templatePageId, Type type) {
        return defaultMappings.containsValue(templatePageId);
    }

    /**
     * Merge the inheriting overrides with explicit category overrides done on the
     * given category. Item overrides are ignored.
     * 
     * @param rc
     * @param categoryName
     * @param fallbackMappings
     * @return
     */
    public EnumMap<IDCConstants.Type, String> mergeTemplatePageIds(RuntimeContext rc, String categoryId, EnumMap<IDCConstants.Type, String> fallbackMappings) {
        LOGGER.rcEntry(rc);
        EnumMap<IDCConstants.Type, String> result;

        if (fallbackMappings == null) {
            result = defaultMappings;
        } else {
            result = fallbackMappings;
        }
        return LOGGER.rcExit(rc, result);
    }

    public void setPageTemplates(RuntimeContext rc, JsonArray documents) throws TemplatePageNotFoundException {
        LOGGER.rcEntry(rc, StringUtil.truncateObject(documents, 8000));
        if (documents != null) {
            for (int i = 0; i < documents.size(); i++) {
                JsonObject doc = documents.getJsonObject(i);
                String id = doc.getString("id");
                LOGGER.rcTrace(rc, "Adding page template for: " + id);
                templatePages.put(id, doc);
            }
        }
        LOGGER.rcExit(rc);
    }
    
    public void addDefaultTemplatePages(RuntimeContext rc, JsonArray defaultTemplates) throws TemplatePageNotFoundException {
        LOGGER.rcEntry(rc, defaultTemplates);
        
        if (defaultTemplates != null) {
            for (int i=0; i<defaultTemplates.size(); i++) {
                JsonObject page = defaultTemplates.getJsonObject(i);
                templatePages.put(page.getString("id"), page);
            }
        }
        
        Collection<String> keys = defaultMappings.values();
        for (String key : keys) {
            LOGGER.rcTrace(rc, "validating: " + key);
            JsonObject page = templatePages.get(key);
            if (page == null) {
                throw new TemplatePageNotFoundException(key);
            }
            LOGGER.rcTrace(rc, "found: " + page.encodePrettily());
        }
        
        LOGGER.rcExit(rc);
        
    }


    public JsonObject getTemplatePage(RuntimeContext rc, String id) {
        LOGGER.rcEntry(rc, id);
        JsonObject result = templatePages.get(id);
        LOGGER.rcExit(rc, ConnectorUtils.toSummary(result));
        return result;
    }

    public String getTemplatePageIdForItem(RuntimeContext rc, String itemId) {
        LOGGER.rcEntry(rc, itemId);
        String result = itemMappings.get(itemId);
        return LOGGER.rcExit(rc, result);
    }
    
    public JsonObject getDefaultProductPageTemplate() {
        String id = defaultMappings.get(Type.product);
        return templatePages.get(id);
    }

    public JsonObject getDefaultPageTemplate(Type type) {
        String id = defaultMappings.get(type);
        return templatePages.get(id);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PageOverrides [defaultMappings=");
        builder.append(defaultMappings);
        builder.append(", itemMappings=");
        builder.append(StringUtil.truncateObject(itemMappings, 2000));
        builder.append(", templatePages=");
        builder.append(StringUtil.truncateObject(templatePages, 2000));
        builder.append("]");
        return builder.toString();
    }



   
}
