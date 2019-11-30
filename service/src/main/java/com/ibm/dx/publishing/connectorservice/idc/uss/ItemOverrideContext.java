package com.ibm.dx.publishing.connectorservice.idc.uss;

import com.ibm.dx.publishing.connectorservice.idc.Constants;

import io.vertx.core.json.JsonObject;

public class ItemOverrideContext  {
    
    private final String pageId;
    private String catalogResourceId;
    private JsonObject pageTemplate;
    
    public ItemOverrideContext(String pageId) {
        this.pageId = pageId;
    }

    public JsonObject getPageTemplate() {
        return pageTemplate;
    }

    public String getId() {
        return pageId;
    }

    public String getCatalogResourceId() {
        return catalogResourceId;
    }

    public void setPageTemplate(JsonObject pageTemplate) {
        this.pageTemplate = pageTemplate;
        catalogResourceId = pageTemplate.getString(Constants.AUTHORING_SITES_CATALOG_RESOURCE_ID);
    }
    
    public void setCatalogResourceId(String id) {
        this.catalogResourceId = id;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ItemOverrideContext [pageId=");
        builder.append(pageId);
//        builder.append(", pageTemplate=");
//        builder.append(pageTemplate);
        builder.append("]");
        return builder.toString();
    }
    
    

}
