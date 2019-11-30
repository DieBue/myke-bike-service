/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.publishing.connectorservice.idc.uss;

import java.util.Comparator;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.connectorservice.idc.IDCConstants;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.ConnectorException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.MissingItemDisplayNameException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.MissingItemIdException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.MissingItemUrlNameException;
import com.ibm.wps.util.StringUtils;

import io.vertx.core.json.JsonObject;

public abstract class ItemImpl implements Item {
    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2017";

    protected final String id;
    protected final String name;
    protected final String description;
    protected final String urlName;
    protected IDCConstants.Type type;
    protected final float floatPosition;
    protected int position;

    public static final ItemComperator ITEM_COMPARATOR = new ItemComperator();
    
    public static class ItemComperator implements Comparator<ItemImpl> {

        @Override
        public int compare(ItemImpl o1, ItemImpl o2) {
            float f1 = o1.getFloatPosition();
            float f2 = o2.getFloatPosition();
            if (f1>f2) {
                return 1;
            }
            else if (f1<f2) {
                return -1;
            }
            return 0;
        }
        
    }

    
    public ItemImpl(RuntimeContext rc, JsonObject json) {
        String identifier = json.getString(UssModel.IDENTIFIER); 
        id = (identifier != null) ? identifier : json.getString(UssModel.PART_NUMBER);  
        this.name = json.getString(UssModel.DISPLAY_NAME);
        this.urlName = json.getString(UssModel.URL_NAME);
        this.description = json.getString(UssModel.DESCRIPTION);
        if (json.containsKey(UssModel.SEQUENCE)) {
            this.floatPosition = json.getFloat(UssModel.SEQUENCE);
        }
        else {
            floatPosition = 0;
        }
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTitle() {
        // no seperate title for default itme impl
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getUrlName() {
        return urlName;
    }

    @Override
    public IDCConstants.Type getType() {
        return type;
    }

    @Override
    public float getFloatPosition() {
        return floatPosition;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public int getPosition() {
        return position;
    }

    public void setType(IDCConstants.Type type) {
        this.type = type;
    }
    
    @Override
    public void validate() throws ConnectorException {
        if (!StringUtils.isStringNonEmpty(id)) {
            throw new MissingItemIdException(this.toString());
        }
        if (!StringUtils.isStringNonEmpty(urlName)) {
            throw new MissingItemUrlNameException(this.toString());
        }
        if (!StringUtils.isStringNonEmpty(name)) {
            throw new MissingItemDisplayNameException(this.toString());
        }
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Item [id=");
        builder.append(id);
        builder.append(", name=");
        builder.append(name);
        builder.append(", urlName=");
        builder.append(urlName);
        builder.append(", type=");
        builder.append(type);
        builder.append(", floatPosition=");
        builder.append(floatPosition);
        builder.append(", position=");
        builder.append(position);
        builder.append("]");
        return builder.toString();
    }
    
    
    
}
