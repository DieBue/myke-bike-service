/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.publishing.connectorservice.idc.uss;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.connectorservice.idc.IDCConstants;
import com.ibm.dx.publishing.connectorservice.idc.PageOverrides;
import com.ibm.dx.publishing.connectorservice.idc.TemplatePageAssignment;

import io.vertx.core.json.JsonObject;

public class Product extends ItemImpl {
    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2017";
    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(Product.class);


    private final Category category;
    private final String partNumber;

    public Product(RuntimeContext rc, JsonObject productJson, Category category) {
        super(rc, productJson);
        setType(IDCConstants.CATALOG_ENTRY_TYPE_CODES.get(productJson.getString(UssModel.PRODUCT_TYPE)));
        this.partNumber = productJson.getString(UssModel.PART_NUMBER);
        category.setHasProducts(rc);
        this.category = category;
    }

    public String getCategoryId() {
        return category.getId();
    }

    public String getPath() {
        return category.getPath();
    }

    public Category getCategory() {
        return category;
    }

    @Override
    public TemplatePageAssignment getPageTemplateAssignment(RuntimeContext rc, PageOverrides overrides) {
        LOGGER.rcEntry(rc, name, type);
        TemplatePageAssignment result = category.getPageTemplate(rc, overrides, id, type);
        return result;
    }

    public String getPartNumber() {
        return partNumber;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Product [id=");
        builder.append(id);
        builder.append(", name=");
        builder.append(name);
        builder.append(", partNumber=");
        builder.append(partNumber);
        builder.append(", urlName=");
        builder.append(urlName);
        builder.append(", type=");
        builder.append(type);
        builder.append(", floatPosition=");
        builder.append(floatPosition);
        builder.append(", position=");
        builder.append(position);
        builder.append(", category=");
        builder.append(category);
        builder.append("]");
        return builder.toString();
    }

    
    
}
