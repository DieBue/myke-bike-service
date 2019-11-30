/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.publishing.connectorservice.idc.uss;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.connectorservice.idc.IDCConstants;
import com.ibm.dx.publishing.connectorservice.idc.IDCConstants.Type;
import com.ibm.dx.publishing.connectorservice.idc.PageOverrides;
import com.ibm.dx.publishing.connectorservice.idc.TemplatePageAssignment;
import com.ibm.dx.publishing.connectorservice.idc.TemplatePageAssignment.Mode;
import com.ibm.wps.util.StringUtils;

import io.vertx.core.json.JsonObject;

public class Category extends ItemImpl {
    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2017";
    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(Category.class);


    private final CompletableFuture<Void> productsResolved;
    private final ArrayList<String> parentIds;
    private String path;
    private boolean hasProducts;
    

    public Category(RuntimeContext rc, JsonObject categoryJson, Category parent) {
        super(rc, categoryJson);
        setType(IDCConstants.Type.category);
        if (parent != null) {
            this.parentIds = new ArrayList<>(parent.getParentIds().size() + 1);
            parentIds.addAll(parent.getParentIds());
            parentIds.add(parent.getId());
            this.path = parent.getPath() + "/" + urlName;
        }
        else {
            parentIds = new ArrayList<>();
            this.path = "/" + urlName;
        }
        productsResolved = new CompletableFuture<Void>();
        hasProducts = false;
    }

    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }


    public CompletableFuture<Void> getProductsResolved() {
        return productsResolved;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public ArrayList<String> getParentIds() {
        return parentIds;
    }
    
    public String getParentId() {
        return ((parentIds != null) && (!parentIds.isEmpty())) ? parentIds.get(parentIds.size()-1): null;
    }

    @Override
    public TemplatePageAssignment getPageTemplateAssignment(RuntimeContext rc, PageOverrides overrides) {
        return internalGetPageTemplateAssigement(rc, overrides, id, Type.category);
    }

    public TemplatePageAssignment getPageTemplate(RuntimeContext rc, PageOverrides overrides, String productId, Type type) {
        return internalGetPageTemplateAssigement(rc, overrides, productId, type);
    }

    private TemplatePageAssignment internalGetPageTemplateAssigement(RuntimeContext rc, PageOverrides overrides, String id, Type type) {
        LOGGER.rcEntry(rc, name, id, type);
        final TemplatePageAssignment result;
        String templatePageIdFromItemOverride = overrides.getTemplatePageIdForItem(rc, id);
        
        if (StringUtils.isStringNonEmpty(templatePageIdFromItemOverride)) {
            LOGGER.rcTrace(rc, "Item override detected: " + templatePageIdFromItemOverride);
            result = new TemplatePageAssignment(overrides.getTemplatePage(rc, templatePageIdFromItemOverride), Mode.itemOverride);
        }
        else {
            Type lookupType = (Type.category.equals(type) && hasProducts) ? Type.categorywithproducts : type;
            LOGGER.rcTrace(rc, "lookupType: " + lookupType);
            JsonObject templatePage = overrides.getDefaultPageTemplate(lookupType); 
            result = new TemplatePageAssignment(templatePage, Mode.inheritedFromDefault);
        }
        return LOGGER.rcExit(rc, result);
    }

    public void resolveChildPositionValues(RuntimeContext rc, HashMap<String, Category> categories) {
        LOGGER.rcEntry(rc, name);
        List<Category> childCategories = getChildCategories(rc, categories);
        Collections.sort(childCategories, ITEM_COMPARATOR);
        for (int i=0; i<childCategories.size(); i++) {
            childCategories.get(i).setPosition(i);
        }
        LOGGER.rcExit(rc);
    }

    private List<Category> getChildCategories(RuntimeContext rc, HashMap<String, Category> categories) {
        LOGGER.rcEntry(rc, name);
        ArrayList<Category> result = new ArrayList<>();
        Collection<Category> cats = categories.values();
        for (Category category : cats) {
            if (id.equals(category.getParentId())) {
                result.add(category);
            }
        }
        return LOGGER.rcExit(rc, result);
    }

    public boolean hasProducts() {
        return hasProducts;
    }

    public void setHasProducts(RuntimeContext rc) {
        LOGGER.rcEntry(rc, name);
        hasProducts = true;
        LOGGER.rcExit(rc);
    }
    

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Category [path=");
        builder.append(path);
        builder.append(", id=");
        builder.append(id);
        builder.append(", name=");
        builder.append(name);
        builder.append(", productsResolved=");
        builder.append(productsResolved);
        builder.append(", parentIds=");
        builder.append(parentIds);
        builder.append(", hasProducts=");
        builder.append(hasProducts);
        builder.append(", name=");
        builder.append(name);
        builder.append("]");
        return builder.toString();
    }

}
