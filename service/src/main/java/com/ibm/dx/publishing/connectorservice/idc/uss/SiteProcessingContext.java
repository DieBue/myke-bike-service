package com.ibm.dx.publishing.connectorservice.idc.uss;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.ibm.dx.publishing.common.api.http.MicroServiceHttpClient;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.common.api.reporting.MessageBusProducerClient;

import io.vertx.core.json.JsonObject;

public class SiteProcessingContext extends ProcessingContext {
    
    private final HashMap<String, Category> EMPTY_MAP = new HashMap<>();
    
    private final ItemOverrideContext itemOverrideContext;
    private HashMap<String, Category> categoriesById;
    private HashMap<String, ArrayList<Product>> products;
    private final boolean skipOnMissingConfig;
    private boolean skipProductResolution = false;
    private final int ussPageSize;
   
    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(SiteProcessingContext.class);
    
    public SiteProcessingContext(RuntimeContext rc, String siteId, MicroServiceHttpClient microServiceHttpClient, MessageBusProducerClient messageBusProducerClient, int ussPageSize, boolean skipOnMissingConfig) {
        this(rc, siteId, microServiceHttpClient, messageBusProducerClient, null, ussPageSize, skipOnMissingConfig);
    }

    public SiteProcessingContext(RuntimeContext rc, String siteId, MicroServiceHttpClient microServiceHttpClient, MessageBusProducerClient messageBusProducerClient, String itemOverridePageId, int ussPageSize, boolean skipOnMissingConfig) {
        super(rc, microServiceHttpClient, messageBusProducerClient, siteId);
        this.categoriesById = new HashMap<>();
        itemOverrideContext = (itemOverridePageId != null) ? new ItemOverrideContext(itemOverridePageId) : null;
        this.skipOnMissingConfig = skipOnMissingConfig;
        this.ussPageSize = ussPageSize;
    }

    public void setCategories(HashMap<String, Category> c) {
        this.categoriesById = (c != null) ? c : EMPTY_MAP;
        products = new HashMap<>(categoriesById.size());
        Set<String> keys = categoriesById.keySet();
        for (String categoryId : keys) {
            products.put(categoryId, new ArrayList<>());
        }
    }

    public HashMap<String, Category> getCategories() {
        return categoriesById;
    }

    public void addProducts(String categoryId, ArrayList<Product> p) {
        products.get(categoryId).addAll(p);
    }

    @SuppressWarnings("rawtypes")
    public CompletableFuture[] getProductResolutionFutures() {
        LOGGER.rcEntry(rc);
        Collection<Category> values = categoriesById.values();
        CompletableFuture[] result = new CompletableFuture[values.size()];
        Iterator<Category> it = values.iterator();
        int i=0;
        while (it.hasNext()) { 
            result[i++] = it.next().getProductsResolved();
        }
        LOGGER.rcExit(rc, result.length);
        return result;
    }
    
    public void setSkipProductResolution(boolean skipProductResolution) {
        this.skipProductResolution = skipProductResolution;
    }

    public boolean skipProductResolution() {
        return skipProductResolution;
    }

    public String toSummary() {
        StringBuilder sb = new StringBuilder();
        Collection<Category> values = categoriesById.values();
        for (Category category : values) {
            sb.append(category.getPath()).append("\n");
        }
        return sb.toString();
    }
    
    public JsonObject toResult() {
        return new JsonObject().put("status", "success");
    }

    public boolean hasValidItemOverrideContext() {
        return (itemOverrideContext != null) && (itemOverrideContext.getCatalogResourceId() != null);
        
    }

    public ItemOverrideContext getItemOverrideContext() {
        return itemOverrideContext;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SiteProcessingContext [toString()=");
        builder.append(super.toString());
        builder.append("]");
        return builder.toString();
    }

    public String dump() {
        return super.toString() + "/" + toString();
    }

    public boolean isSkipOnMissingConfig() {
        return skipOnMissingConfig;
    }

	public int getUssPageSize() {
		return ussPageSize;
	}

}
