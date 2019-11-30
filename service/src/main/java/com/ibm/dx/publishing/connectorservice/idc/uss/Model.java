/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.publishing.connectorservice.idc.uss;

import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;

import org.apache.http.client.utils.URIBuilder;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.connectorservice.idc.Constants;
import com.ibm.dx.publishing.connectorservice.idc.IDCConstants;
import com.ibm.dx.publishing.connectorservice.idc.IDCConstants.Type;
import com.ibm.dx.publishing.connectorservice.idc.PageKinds;
import com.ibm.dx.publishing.connectorservice.idc.TemplatePageAssignment;
import com.ibm.dx.publishing.connectorservice.idc.TemplatePageAssignment.Mode;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.ConnectorException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.TemplatePageNotFoundException;
import com.ibm.dx.publishing.connectorservice.mediation.SearchSchemaConstants;
import com.ibm.dx.publishing.connectorservice.util.ConnectorUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Model {
    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2017";

    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(Model.class);
    
    private static final String PROP_KEY_EXTERNAL_CONTEXT = "externalContext";
    
    public static final String SYSTEM_TAG_PREFIX_CONNECTOR = "dyn-connector:";
    public static final String SYSTEM_TAG_PREFIX_TYPE = "dyn-type:";
    public static final String SYSTEM_TAG_PREFIX_ID = "dyn-id:";
    public static final String SYSTEM_TAG_PREFIX_STORE = "dyn-store:";
    public static final String SYSTEM_TAG_PREFIX_PAGE = "dyn-page:";
    public static final String SYSTEM_TAG_PREFIX_PAGE_NAME = "dyn-page-name:";
    
    public static final String DEFAULT_SEO_PATH_FORMAT = "{0}/{1}/{2}";
    public static final String SHORT_SEO_PATH_FORMAT = "{0}/{1}";
    
    private static final EnumMap<IDCConstants.Type, String> idcType2PageKind = new EnumMap<IDCConstants.Type, String>(IDCConstants.Type.class) {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        {
            put(IDCConstants.Type.bundle, PageKinds.BUNDLE_PAGE);
            put(IDCConstants.Type.kit, PageKinds.KIT_PAGE);
            put(IDCConstants.Type.sku, PageKinds.SKU_PAGE);
            put(IDCConstants.Type.product, PageKinds.PRODUCT_PAGE);
            put(IDCConstants.Type.category, PageKinds.CATEGORY_PAGE);
            put(IDCConstants.Type.categorywithproducts, PageKinds.CATEGORY_PRODUCTS_PAGE);
        }
    };
    
    private static final EnumMap<TemplatePageAssignment.Mode, String> templatePageAssignmentMode2PageKind = new EnumMap<TemplatePageAssignment.Mode, String>(TemplatePageAssignment.Mode.class) {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        {
            put(TemplatePageAssignment.Mode.categoryOverride, PageKinds.CATEGORY_OVERRIDE_PAGE);
            put(TemplatePageAssignment.Mode.inherited, PageKinds.INHERITED_OVERRIDE_PAGE);
            put(TemplatePageAssignment.Mode.inheritedFromDefault, PageKinds.DEFAULT_OVERRIDE_PAGE);
            put(TemplatePageAssignment.Mode.itemOverride, PageKinds.ITEM_OVERRIDE_PAGE);
        }
    };

    public static JsonObject buildDynPage(RuntimeContext rc, ProcessingContext pc, Item item, Category cat) throws URISyntaxException, ConnectorException {
        LOGGER.rcEntry(rc, item, cat);
        JsonObject result = null;
        item.validate();
        TemplatePageAssignment templatePageAssignment = item.getPageTemplateAssignment(rc, pc.getPageOverrides());
        if (templatePageAssignment != null) {
            boolean isItemOverride = templatePageAssignment.isItemOverride();
            String parentId = getPageId(pc, cat);

            JsonObject templatePageJson = templatePageAssignment.getTemplatePage();
            if (templatePageJson == null) {
                throw new TemplatePageNotFoundException(item.getName());
            }
            result = templatePageAssignment.getTemplatePage().copy();
            result.remove("catalogId");
            result.remove("children");
            if (!isItemOverride) {
                result.put(SearchSchemaConstants.Common.NAME, item.getName());
                result.put(SearchSchemaConstants.Common.DESCRIPTION, item.getDescription());
                result.put(SearchSchemaConstants.Page.SEGMENT, item.getUrlName());
                result.put(SearchSchemaConstants.Common.TEXT, new JsonArray().add(item.getName()).add(item.getId()));
            }
            
            String id = getPageId(pc, item);
            result.put(SearchSchemaConstants.Page.SITE_ID, pc.getSiteId());
            result.put(SearchSchemaConstants.Common.ID, id);
            result.put(SearchSchemaConstants.Common.__DOC_ID__, "page:" + id);
            result.put(SearchSchemaConstants.Page.POSITION, item.getPosition()+1);
            result.put(SearchSchemaConstants.Common.LAST_MODIFIED, Instant.now().toString());
            result.put(SearchSchemaConstants.Page.PARENT_ID, parentId);
            result.put(SearchSchemaConstants.Common.SYSTEM_MODIFIED, pc.getTimeStamp());
            
            result.put(SearchSchemaConstants.Common.SYSTEM_TAGS, buildTags(pc, templatePageAssignment, item));
            result.put(SearchSchemaConstants.Page.KIND, buildKind(rc, item, templatePageAssignment.getMode()));
            if (item instanceof Product) {
                result.put(SearchSchemaConstants.Page.HIDE_FROM_NAVIGATION, true);
            }
            else {
                result.put(SearchSchemaConstants.Page.HIDE_FROM_NAVIGATION, false);
            }
            handleRouteAndUrl(rc, pc, item, cat, isItemOverride, result);
            handleDocument(rc, pc, item, isItemOverride, result);

            // do not include categories in the document
            handleCategories(item, result);
            handlePathValues(rc, pc, item, cat, isItemOverride, result);
            result.remove("contentTypeId");
            result.remove("contentStatus");
            result.remove("catalogResourceId");
            result.remove("rev");
            removeValuesBasedOnSchema(rc, result);
        }
        else {
            throw new TemplatePageNotFoundException("No template found for category " + item.getName());
        }
        LOGGER.rcDebug(rc, result);
        return LOGGER.rcExit(rc, result);
    }

    private static void removeValuesBasedOnSchema(RuntimeContext rc, JsonObject result) {
        LOGGER.rcEntry(rc);
        ArrayList<String> namesToRemove = null;
        Iterator<String> names = result.fieldNames().iterator();
        while (names.hasNext()) {
            String name = names.next();
            if (!SchemaWhiteList.SCHEMA_WHITE_LIST.contains(name)) {
                if (namesToRemove == null) {
                    namesToRemove = new ArrayList<>();
                }
                namesToRemove.add(name);
            }
        }
        if (namesToRemove != null) {
            LOGGER.rcTrace(rc, "Removing names: " + namesToRemove);
            names = namesToRemove.iterator();
            while (names.hasNext()) {
                String name = names.next();
                result.remove(name);
            }
        }
        LOGGER.rcExit(rc);
    }

    private static void handleCategories(Item i, JsonObject result) {
        Category cat = getCategory(i);
        result.put("categoryLeaves", new JsonArray().add(cat.getPath()).add(cat.getId()));
        ArrayList<String> categories = ConnectorUtils.addToNew(cat.getParentIds(), cat.getId());
        result.put("categories", categories);
    }
    
    private static void handleDocument(RuntimeContext rc, ProcessingContext pc, Item i, boolean isItemOverride, JsonObject result) {
        result.remove("document");
        JsonObject externalContext = ExternalContext.asJson(rc, pc, i);
        result.put(PROP_KEY_EXTERNAL_CONTEXT, externalContext);
        
        if (!isItemOverride) {
            result.put("title", i.getName());
        }
        
        result.put(SearchSchemaConstants.Common.DOCUMENT, result.encode());
        result.remove(PROP_KEY_EXTERNAL_CONTEXT);
        result.remove("title");
    }

    private static void handleRouteAndUrl(RuntimeContext rc, ProcessingContext pc, Item item, Category cat, boolean isItemOverride, JsonObject page) throws URISyntaxException {
        LOGGER.rcEntry(rc, item);
        final String path = buildPathValue(pc, item, cat, isItemOverride, page); 
        String route;
        route = new URIBuilder().setPath(path).build().toString();
        page.put(SearchSchemaConstants.Page.ROUTE, route);
        page.put(SearchSchemaConstants.Page.URL, pc.getSiteURL() + route);
        LOGGER.rcEntry(rc, route);
    }

    private static void handlePathValues(RuntimeContext rc, ProcessingContext pc, Item item, Category cat, boolean isItemOverride, JsonObject page) {
        
        final StringBuilder idPath = new StringBuilder();
        idPath.append(pc.getUrlBasePath()).append("/").append(item.getType()).append("/").append(item.getId());

        final String path = buildPathValue(pc, item, cat, isItemOverride, page); 
        page.put(SearchSchemaConstants.Page.PATH, path);
        page.put("paths", new JsonArray().add(path).add(idPath.toString()));
    }
    
    private static String buildPathValue(ProcessingContext pc, Item item, Category cat, boolean isItemOverride, JsonObject page) {
        final String result; 
        String urlName = isItemOverride ? page.getString(Constants.AUTHORING_SITES_SEGMENT) : item.getUrlName();

        if (cat != null) {
            result = MessageFormat.format(DEFAULT_SEO_PATH_FORMAT, pc.getUrlBasePath(), cat.getUrlName(), urlName);
        }
        else {
            result = MessageFormat.format(SHORT_SEO_PATH_FORMAT, pc.getUrlBasePath(), urlName);
        }
        return result;
    }
    
    private static Category getCategory(Item i) {
        if (i instanceof Category) {
            return (Category)i;
        }
        else return ((Product)i).getCategory();
    }

    private static JsonArray buildTags(ProcessingContext pc, TemplatePageAssignment template, Item i) {
        LOGGER.rcEntry(pc.getRc(), i);
        JsonArray result = new JsonArray();
        result.add(SYSTEM_TAG_PREFIX_TYPE + i.getType());
        result.add(SYSTEM_TAG_PREFIX_ID + i.getId());
        result.add(getConnectorTag(pc.getConnectorId()));
        result.add(SYSTEM_TAG_PREFIX_STORE + pc.getSiteConfig().getStoreId());
        result.add(SYSTEM_TAG_PREFIX_PAGE + template.getTemplatePage().getString("id"));
        result.add(SYSTEM_TAG_PREFIX_PAGE_NAME + template.getTemplatePage().getString("name"));
        return LOGGER.rcExit(pc.getRc(), result);
    }
    
    public static String getConnectorTag(String connectorId) {
        return SYSTEM_TAG_PREFIX_CONNECTOR + connectorId;
    }

    public static String getPageId(ProcessingContext pc, Item i) {
        LOGGER.rcEntry(pc.getRc(), i, pc.getSiteId());
        StringBuilder id = new StringBuilder();
        if (i == null) {
            id.append(pc.getCatalogPage().getString("id"));
        }
        else {
            id.append("idc-").append(i.getType()).append("-").append(i.getId());
            String siteId = pc.getSiteId();
        	if ((!siteId.equals("default") && (pc.getSiteId() != null))) {
        		id.append("-").append(pc.getSiteId());
        	}

        }
        return LOGGER.rcExit(pc.getRc(), id.toString());
    }
    
    private static JsonArray buildKind(RuntimeContext rc, Item i, Mode mode) {
        // This should be improved by caching the JsonArrays .....
        LOGGER.rcEntry(rc, i, mode);
        JsonArray result = new JsonArray().add(PageKinds.DYNAMIC_PAGE);
        Type type = ((i instanceof Category) && ((Category)i).hasProducts()) ? Type.categorywithproducts : i.getType();
        String k = idcType2PageKind.get(type);
        if (k != null) {
            result.add(k);
        }
        k = templatePageAssignmentMode2PageKind.get(mode);
        if (k != null) {
            result.add(k);
        }
        
        return LOGGER.rcExit(rc, result);
    }

    public static String getSystemTagValue(RuntimeContext rc, JsonArray systemTags, String key) {
        LOGGER.rcEntry(rc, key);
        String result = null;
        if (systemTags != null) {
            for (int i=0; i<systemTags.size(); i++) {
                String tag = systemTags.getString(i);
                if (tag.startsWith(key)) {
                    result = tag.substring(key.length());
                    break;
                }
            }
        }
        return LOGGER.rcExit(rc, result);
    }
    

}
