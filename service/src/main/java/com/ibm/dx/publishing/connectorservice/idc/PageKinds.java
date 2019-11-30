package com.ibm.dx.publishing.connectorservice.idc;

import java.util.EnumMap;

import com.ibm.dx.publishing.connectorservice.idc.IDCConstants.Type;

public class PageKinds {
    public static final String DYNAMIC_PAGE = "dynamic-page";
    public static final String CATALOG_PAGE = "catalog-page";
    public static final String CATALOG_DEFAULT_PAGE = "catalog-default-page";
    
    public static final String CATEGORY_PAGE = "category-page";
    public static final String SKU_PAGE = "sku-page";
    public static final String BUNDLE_PAGE = "bundle-page";
    public static final String KIT_PAGE = "kit-page";
    public static final String PRODUCT_PAGE = "product-page";
    public static final String CATEGORY_PRODUCTS_PAGE = "category-products-page";
    
    public static final String ITEM_OVERRIDE_PAGE ="dynamic-item-override-page";
    public static final String CATEGORY_OVERRIDE_PAGE ="dynamic-category-override-page";
    public static final String INHERITED_OVERRIDE_PAGE ="dynamic-inherited-override-page";
    public static final String DEFAULT_OVERRIDE_PAGE ="dynamic-default-override-page";

    private static final EnumMap<IDCConstants.Type, String> TYPE_TO_PAGE_KIND_MAP = new EnumMap<>(IDCConstants.Type.class); 
    
    static {
        TYPE_TO_PAGE_KIND_MAP.put(Type.bundle, BUNDLE_PAGE);
        TYPE_TO_PAGE_KIND_MAP.put(Type.category, CATEGORY_PAGE);
        TYPE_TO_PAGE_KIND_MAP.put(Type.categorywithproducts, CATEGORY_PRODUCTS_PAGE);
        TYPE_TO_PAGE_KIND_MAP.put(Type.kit, KIT_PAGE);
        TYPE_TO_PAGE_KIND_MAP.put(Type.product, PRODUCT_PAGE);
        TYPE_TO_PAGE_KIND_MAP.put(Type.sku, SKU_PAGE);
    };
    
    public static String fromType(Type type) {
        return TYPE_TO_PAGE_KIND_MAP.get(type);
    }
    
}
