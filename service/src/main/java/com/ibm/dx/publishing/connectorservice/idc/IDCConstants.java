package com.ibm.dx.publishing.connectorservice.idc;

import java.util.HashMap;

public interface IDCConstants {
    enum Type {product, category, sku, bundle, kit, categorywithproducts };
    
    public static final String UNIQUE_ID = "uniqueID";
    public static final String NAME = "name";
    public static final String CATALOG_ENTRY_VIEW = "catalogEntryView";
    public static final String CATALOG_ENTRY_VIEW_LEGACY = "CatalogEntryView";
    public static final String CATALOG_GROUP_VIEW = "catalogGroupView";
    public static final String CATALOG_GROUP_VIEW_LEGACY = "CatalogGroupView";
    public static final String DATA = "data";
    public static final String RESOURCE_ID = "resourceId";
    public static final String IDENTIFIER = "identifier";
    public static final String PARENT_CATEGORIES = "parentCategories";
    public static final String PAGINATION = "pagination";
    public static final String NUM_FOUND = "numFound";
    
    public static final String CATALOG_ENTRY_TYPE_CODE = "catalogEntryTypeCode";
    public static final String PRODUCT_BEAN = "ProductBean";
    public static final String BUNDLE_BEAN = "BundleBean";
    public static final String PACKAGE_BEAN = "PackageBean";
    public static final String ITEM_BEAN = "ItemBean";
    
    public static final String SHOP_API_PARAM_CATEGORY = "category";
    public static final String SHOP_API_PARAM_STORE = "store";
    public static final String SHOP_API_PARAM_PAGE_SIZE = "pageSize";
    
    
    public static final HashMap<String, IDCConstants.Type> CATALOG_ENTRY_TYPE_CODES = new HashMap<String, IDCConstants.Type>() {
        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        {
            put(PRODUCT_BEAN, Type.product);
            put(BUNDLE_BEAN, Type.bundle);
            put(PACKAGE_BEAN, Type.kit);
            put(ITEM_BEAN, Type.sku);
        }
    };

    
    
    
}
