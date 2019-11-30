/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.publishing.connectorservice.idc;

import java.util.concurrent.CompletableFuture;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.SchemaValidationException;
import com.ibm.dx.publishing.connectorservice.mediation.SearchSchemaConstants;
import com.ibm.dx.publishing.connectorservice.util.StringUtil;
//import com.ibm.wps.streaming.fasterxml.JSONHandlerFactoryImpl;
import com.ibm.wps.util.StringUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class IDCPublish { 
    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2017";

    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(IDCPublish.class);

    private static final IDCPublish INSTANCE = new IDCPublish();
    
    public static final String HEADER_CONTENT_TYPE = "content-type";
    public static final String HEADER_VALUE_JSON = "application/json";
    public static final String ROUTE = "/delivery/v1/search";
    
    public static final IDCPublish getInstance() {
        return INSTANCE;
    }

    private IDCPublish() {
        //jsonHandlerFac = new JSONHandlerFactoryImpl(new JsonFactory());
    }

    //final JSONHandlerFactory jsonHandlerFac;
    
    private final JsonObject filterUnexpectedDocuments(final PublishContext ctx) {
        JsonObject json = ctx.getContext().getBodyAsJson();
        
        JsonArray documents = json.getJsonArray("documents");
        JsonArray filteredDocuments = new JsonArray();
        int filteredOut = 0;
        for (int i=0 ; i<documents.size();i++) {
            final JsonObject docJson = documents.getJsonObject(i);
            if (acceptDocumentClassification(ctx, docJson)) {
                filteredDocuments.add(docJson);
            } else {
                filteredOut++;
            }
        }
        
        LOGGER.rcTrace(ctx.getRc(), "filtered out {} documents due to unaccepted classification", filteredOut);
        json.put("documents", filteredDocuments);
        LOGGER.rcExit(ctx.getRc());
        return json;
    }
    
    
    private boolean acceptDocumentClassification(final PublishContext ctx, final JsonObject doc) {
        final String classification = doc.getString(SearchSchemaConstants.Common.CLASSIFICATION);
        if (StringUtils.isStringNonEmpty(classification)) {
            if ("product".equals(classification) || "category".equals(classification) || "group".equals(classification)) {
                return true;
            } else if (ctx.getForceClassifications().contains(classification)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    public CompletableFuture<PublishContext> process(PublishContext ctx) {
        RuntimeContext rc = ctx.getRc();
        CompletableFuture<PublishContext> result = new CompletableFuture<PublishContext>();
        
        final JsonObject json = filterUnexpectedDocuments(ctx);
        
        if (json != null) {
            JsonArray documents = json.getJsonArray("documents");
            if (documents != null) {
                for (int i=0; i<documents.size(); i++) {
                    try {
                        processDocment(ctx, documents.getJsonObject(i));
                    } catch (SchemaValidationException e) {
                        LOGGER.rcCatching(rc, e);
                        result.completeExceptionally(e);
                        return result;
                    }
                }
                ctx.getDeliverySearchController().updateDocuments(json).thenAccept(v -> {
                    LOGGER.rcTrace(rc, "post complete");
                    result.complete(ctx);
                }).exceptionally(th -> {
                    LOGGER.rcCatching(rc, th);
                    result.completeExceptionally(th);
                    return null;
                });
            }
            else {
                LOGGER.rcTrace(rc, "No docs. we are done.");
                result.complete(ctx);
            }
        }
        else {
            LOGGER.rcTrace(rc, "Empty body. we are done.");
            result.complete(ctx);
        }
        return result;
    }
    
    private void processDocment(PublishContext pc, JsonObject doc) throws SchemaValidationException {
        RuntimeContext rc = pc.getRc();
        LOGGER.rcEntry(rc, StringUtil.truncateObject(doc, 512));
        assertDocId(pc, doc);
        assertSystemModified(pc, doc);
        LOGGER.rcExit(rc, StringUtil.truncateObject(doc, 512));
    }

    private void assertDocId(PublishContext pc, JsonObject doc) throws SchemaValidationException {
        String id = doc.getString(SearchSchemaConstants.Common.ID);
        String classification = doc.getString(SearchSchemaConstants.Common.CLASSIFICATION);
        if (!StringUtils.isStringNonEmpty(id)) {
            throw new SchemaValidationException("Missing id property");
        }
        if (!StringUtils.isStringNonEmpty(id)) {
            throw new SchemaValidationException("Missing classification property");
        }
        String docId = classification + ":" + id;
        LOGGER.rcTrace(pc.getRc(), "__docId__" + docId);
        doc.put(SearchSchemaConstants.Common.__DOC_ID__, docId);
    }

    private void assertSystemModified(PublishContext pc, JsonObject doc) throws SchemaValidationException {
        doc.put(SearchSchemaConstants.Common.SYSTEM_MODIFIED, pc.getTimeStamp());
    }
}
