package com.ibm.dx.publishing.connectorservice.idc;

public interface Constants {
    enum LayoutAssignmentMode {defaultOverride, itemOverride, explicitCategoryOverride, inheritedCategoryOverride};
    
    public static final String AUTHORING_SITES_CATALOG_RESOURCE_ID = "catalogResourceId";
    public static final String AUTHORING_SITES_DEFAULT_CATALOG_PAGE_IDS = "defaultCatalogPageIds";
    public static final String AUTHORING_SITES_STORE_ID = "storeId";
    public static final String AUTHORING_SITES_SEGMENT = "segment";
    public static final String AUTHORING_SITES_ITEMS = "items";
    
    public static final String CONNECTOR_PARAM_SKIP_ON_MSSING_CONFIG = "skipOnMissingConfig";
}
