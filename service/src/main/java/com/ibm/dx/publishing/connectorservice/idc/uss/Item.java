/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.publishing.connectorservice.idc.uss;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.connectorservice.idc.IDCConstants;
import com.ibm.dx.publishing.connectorservice.idc.PageOverrides;
import com.ibm.dx.publishing.connectorservice.idc.TemplatePageAssignment;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.ConnectorException;

public interface Item {
    @SuppressWarnings("unused")
    static final String COPYRIGHT = "Copyright IBM Corp. 2017";

    public String getId();

    public String getName();
    
    public String getTitle();

    public String getDescription();

    public String getUrlName();

    public IDCConstants.Type getType();

    public float getFloatPosition();

    public int getPosition();
    
    public void validate() throws ConnectorException;
    /**
     * Returns the page template for this item
     * @param rc
     * @param mappings
     * @return
     */
    public TemplatePageAssignment getPageTemplateAssignment(RuntimeContext rc, PageOverrides overrides);
}
