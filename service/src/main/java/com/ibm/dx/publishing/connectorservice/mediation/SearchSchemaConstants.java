/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.mediation;
public final class SearchSchemaConstants {

    public abstract static class Common {
        public static final String ID = "id";
        public static final String CLASSIFICATION = "classification";
        public static final String REV = "rev";
        public static final String NAME = "name";
        public static final String DESCRIPTION = "description";
        public static final String DOCUMENT = "document";
        public static final String LAST_MODIFIED = "lastModified";
        public static final String LAST_MODIFIER_ID = "lastModifierId";
        public static final String LAST_MODIFIER = "lastModifier"; 
        public static final String CREATED = "created";
        public static final String CREATOR_ID = "creatorId";
        public static final String CREATOR = "creator";
        public static final String TAGS = "tags";
        public static final String CATEGORIES = "categories";
        public static final String CATEGORY_LEAVES = "categoryLeaves";
        public static final String CATEGORY_PATHS = "categoryPaths";
        public static final String STATUS = "status";
        public static final String THUMBNAIL = "thumbnail";
        public static final String IS_MANAGED = "isManaged";
        public static final String __REVISION_ID__ = "__revisionId__"; // NOSONAR
        public static final String __DOC_ID__ = "__docId__"; // NOSONAR
        public static final String __DRAFT_ID__ = "__draftId__"; // NOSONAR 
        public static final String __DELETED__ = "__deleted__"; // NOSONAR 
        public static final String __PROJECT_ID__ = "__projectId__"; // NOSONAR 
        
        
        public static final String SYSTEM_MODIFIED = "systemModified";
        public static final String SYSTEM_TAGS = "systemTags";
        public static final String VALID = "valid";
        public static final String DRAFT_STATUS = "draftStatus";
        public static final String TEXT = "text";
        public static final String TYPE = "type";
        public static final String TYPE_ID = "typeId";
        public static final String RESTRICTED = "restricted";
        public static final String PATH = "path";
        
        
        
        private Common() {}
    }
    
    public static final class Asset extends Common {
        public static final String MEDIA_TYPE = "mediaType";
        public static final String LOCATION = "location";
        public static final String LOCATION_PATHS = "locationPaths";
        public static final String ASSET_TYPE = "assetType";
        public static final String RESOURCE = "resource";
        public static final String KEYWORDS = "keywords";
        public static final String FILE_SIZE = "fileSize";
        public static final String URL = "url";
        public static final String HEIGHT = "height";
        public static final String WIDTH = "width";
        public static final String MEDIA = "media";
        public static final String DOMINANT_COLOR = "dominantColor";
        
        
        private Asset() {}
    }
    
    public static final class Category extends Common {
        public static final String PATH = "path";
    }
    
    public static final class Site extends Common {
        private Site() {}
    }
    
    public static final class Content extends Common {
        public static final String LOCALE = "locale";
        public static final String LOCATIONS = "locations";
        public static final String GENERATED_FILES = "generatedFiles";
        private Content() {}
    }
    
    public static final class Page extends Common {
        public static final String SITE_ID = "siteId";
        public static final String LAYOUT_ID = "layoutId";
        public static final String CONTENT_ID = "contentId";
        public static final String SEGMENT = "segment";
        public static final String ROUTE = "route";
        public static final String POSITION = "position";
        public static final String PARENT_IDS = "parentIds";
        public static final String PARENT_ID = "parentId";
        public static final String AGGREGATED_CONTENT_IDS = "aggregatedContentIds";
        public static final String AGGREGATED_IDS = "aggregatedIds";
        public static final String KIND = "kind";
        public static final String URL = "url";
        public static final String HIDE_FROM_NAVIGATION = "hideFromNavigation";
        public static final String PATHS = "paths";
        public static final String EXTERNAL_RESOURCE_URL = "externalResourceUrl";
        
        private Page() {}
    }
        
}

