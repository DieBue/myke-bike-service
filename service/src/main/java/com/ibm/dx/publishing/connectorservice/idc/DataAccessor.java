/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.publishing.connectorservice.idc;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import com.ibm.dx.publishing.common.api.http.MicroServiceHttpClient;
import com.ibm.dx.publishing.common.api.http.RetryPolicy;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.common.api.services.ServiceConstants;
import com.ibm.dx.publishing.common.utils.URLUtil;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.ServiceAccessException;
import com.ibm.dx.publishing.connectorservice.idc.uss.Model;
import com.ibm.dx.publishing.connectorservice.mediation.ResponseHelper;
import com.ibm.wps.util.StringUtils;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;

import static com.ibm.dx.publishing.connectorservice.ConnectorConstants.DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT;

public class DataAccessor {
    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2017";

    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(DataAccessor.class);

    private static final DataAccessor INSTANCE = new DataAccessor();
    
    private static RetryPolicy RETRY_POLICY = RetryPolicy.on50x(2);
    
    private static final int[] RESPONSE_CODES_200_204_404 = new int[] { 200, 204, 404 };
    private static final int[] RESPONSE_CODES_200 = new int[] { 200 };
    
    public static final DataAccessor getInstance() {
        return INSTANCE;
    }

    private DataAccessor() {
    }

    public CompletableFuture<JsonObject> getPagesByKind(RuntimeContext rc, MicroServiceHttpClient client, String siteId, String kind, String fields) {
        LOGGER.rcEntry(rc, siteId, kind);
        try {
            final URI uri;
            if (fields != null) {
                uri = URLUtil.addParams("/authoring/v1/sites/" + siteId + "/pages", "kind", kind, "fields", fields);
            }
            else {
                uri = URLUtil.addParams("/authoring/v1/sites/" + siteId + "/pages", "kind", kind);
            }
            CompletableFuture<JsonObject> result = client.get(rc, ServiceConstants.SITE_SERVICE_NAME, uri.toString(), RETRY_POLICY, DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT).thenCompose(siteServiceResponse -> {
                LOGGER.rcTrace(rc, "loadPageByKind response: " + siteServiceResponse.statusMessage());
                return ResponseHelper.assertResponseCode(rc, ServiceConstants.SITE_SERVICE_NAME, RESPONSE_CODES_200, siteServiceResponse).thenCompose(response -> {
                    return toJsonObject(rc, ServiceConstants.SITE_SERVICE_NAME, uri.toString(), response);
                });
            });
            return LOGGER.rcExit(rc, result);
        } catch (URISyntaxException e) {
            return getFailedFuture(rc, e);
        }
    }

    public CompletableFuture<JsonObject> searchDynamicItemOverridePage(RuntimeContext rc, MicroServiceHttpClient client, String templatePageId) {
        LOGGER.rcEntry(rc, templatePageId);
        try {
            StringBuilder q = new StringBuilder();
            q.append("systemTags:\"").append(Model.SYSTEM_TAG_PREFIX_PAGE).append(templatePageId).append("\"");
            
            StringBuilder fq = new StringBuilder();
            fq.append("classification:page AND kind:").append(PageKinds.ITEM_OVERRIDE_PAGE);

            final URI uri = URLUtil.addParams("/delivery/v1/search/", "q", q.toString(), "fq", fq.toString(), "fl", "id,name,systemTags");
            CompletableFuture<JsonObject> result = client.get(rc, ServiceConstants.SEARCH_FACADE_DELIVERY_SERVICE_NAME, uri.toString(), RETRY_POLICY, DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT).thenCompose(siteServiceResponse -> {
                LOGGER.rcTrace(rc, "getDynamicItemOverridePage response: " + siteServiceResponse.statusMessage());
                return ResponseHelper.assertResponseCode(rc, ServiceConstants.SEARCH_FACADE_DELIVERY_SERVICE_NAME, RESPONSE_CODES_200, siteServiceResponse).thenCompose(response -> {
                    return toJsonObject(rc, ServiceConstants.SEARCH_FACADE_DELIVERY_SERVICE_NAME, uri.toString(), response);
                });
            });
            return LOGGER.rcExit(rc, result);
        } catch (URISyntaxException e) {
            return getFailedFuture(rc, e);
        }
    }

    public CompletableFuture<JsonObject> getFullPagesByKind(RuntimeContext rc, MicroServiceHttpClient client, String siteId, String kind) {
        LOGGER.rcEntry(rc, siteId, kind);
        try {
            final URI uri = URLUtil.addParams("/authoring/v1/sites/" + siteId + "/pages", "kind", kind, "include", "route");
            CompletableFuture<JsonObject> result = client.get(rc, ServiceConstants.SITE_SERVICE_NAME, uri.toString(), RETRY_POLICY, DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT).thenCompose(siteServiceResponse -> {
                LOGGER.rcTrace(rc, "loadPageByKind response: " + siteServiceResponse.statusMessage());
                return ResponseHelper.assertResponseCode(rc, ServiceConstants.SITE_SERVICE_NAME, RESPONSE_CODES_200, siteServiceResponse).thenCompose(response -> {
                    return toJsonObject(rc, ServiceConstants.SITE_SERVICE_NAME, uri.toString(), response);
                });
            });
            return LOGGER.rcExit(rc, result);
        } catch (URISyntaxException e) {
            return getFailedFuture(rc, e);
        }
    }

    public CompletableFuture<JsonObject> getFullPageById(RuntimeContext rc, MicroServiceHttpClient client, String pageId) {
        LOGGER.rcEntry(rc, pageId);
        try {
            final URI uri = URLUtil.addParams("/authoring/v1/sites/pages/" + pageId, "include", "route");
            CompletableFuture<JsonObject> result = client.get(rc, ServiceConstants.SITE_SERVICE_NAME, uri.toString(), RETRY_POLICY, DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT).thenCompose(siteServiceResponse -> {
                LOGGER.rcTrace(rc, "getFullPageById response: " + siteServiceResponse.statusMessage());
                return ResponseHelper.assertResponseCode(rc, ServiceConstants.SITE_SERVICE_NAME, RESPONSE_CODES_200, siteServiceResponse).thenCompose(response -> {
                    return toJsonObject(rc, ServiceConstants.SITE_SERVICE_NAME, uri.toString(), response);
                });
            });
            return LOGGER.rcExit(rc, result);
        } catch (URISyntaxException e) {
            return getFailedFuture(rc, e);
        }
    }
    
    public CompletableFuture<JsonObject> loadTRSRecord(RuntimeContext rc, MicroServiceHttpClient client) {
        LOGGER.rcEntry(rc);
        try {
            URI uri = URLUtil.addParams("/rest/currenttenant");
            CompletableFuture<JsonObject> result = client.get(rc, ServiceConstants.TENANT_REGISTRY_SERVICE_NAME, uri.toString(), RETRY_POLICY, DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT).thenCompose(trsResponse -> {
                LOGGER.rcTrace(rc, "loadTRS response: " + trsResponse.statusMessage());
                return ResponseHelper.assertResponseCode(rc, ServiceConstants.TENANT_REGISTRY_SERVICE_NAME, RESPONSE_CODES_200, trsResponse).thenCompose(response -> {
                    return toJsonObject(rc, ServiceConstants.TENANT_REGISTRY_SERVICE_NAME, uri.toString(), response);
                });
            });
            return LOGGER.rcExit(rc, result);
        } catch (URISyntaxException e) {
            return getFailedFuture(rc, e);
        }
    }

    public CompletableFuture<JsonObject> loadSiteRecord(RuntimeContext rc, MicroServiceHttpClient client, String siteId) {
        LOGGER.rcEntry(rc);
        try {
            URI uri = URLUtil.addParams("/authoring/v1/sites/" + siteId);
            CompletableFuture<JsonObject> result = client.get(rc, ServiceConstants.SITE_SERVICE_NAME, uri.toString(), RETRY_POLICY, DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT).thenCompose(siteResponse -> {
                LOGGER.rcTrace(rc, "loadSite response: " + siteResponse.statusMessage());
                return ResponseHelper.assertResponseCode(rc, ServiceConstants.SITE_SERVICE_NAME, RESPONSE_CODES_200, siteResponse).thenCompose(response -> {
                    return toJsonObject(rc, ServiceConstants.SITE_SERVICE_NAME, uri.toString(), response);
                });
            });
            return LOGGER.rcExit(rc, result);
        } catch (URISyntaxException e) {
            return getFailedFuture(rc, e);
        }
    }
    
    public CompletableFuture<JsonObject> loadSites(RuntimeContext rc, MicroServiceHttpClient client) {
        LOGGER.rcEntry(rc);
        try {
            URI uri = URLUtil.addParams("/authoring/v1/sites");
            CompletableFuture<JsonObject> result = client.get(rc, ServiceConstants.SITE_SERVICE_NAME, uri.toString(), RETRY_POLICY, DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT).thenCompose(siteResponse -> {
                LOGGER.rcTrace(rc, "loadSites response: " + siteResponse.statusMessage());
                return ResponseHelper.assertResponseCode(rc, ServiceConstants.SITE_SERVICE_NAME, RESPONSE_CODES_200, siteResponse).thenCompose(response -> {
                    return toJsonObject(rc, ServiceConstants.SITE_SERVICE_NAME, uri.toString(), response);
                });
            });
            return LOGGER.rcExit(rc, result);
        } catch (URISyntaxException e) {
            return getFailedFuture(rc, e);
        }
    }

    public CompletableFuture<JsonObject> loadTopLevelCategories(RuntimeContext rc, MicroServiceHttpClient client, String store) {
        LOGGER.rcEntry(rc);
        try {
            URI uri = URLUtil.addParams("/shop/v1/categories/@top", "store", store);
            CompletableFuture<JsonObject> result = client.get(rc, ServiceConstants.COMMERCE_DELIVERY_SEARCH_FACADE, uri.toString(), RETRY_POLICY, DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT).thenCompose(resp -> {
                LOGGER.rcTrace(rc, "loadTopLevelCategories response: " + resp.statusMessage());
                return ResponseHelper.assertResponseCode(rc, ServiceConstants.COMMERCE_DELIVERY_SEARCH_FACADE, RESPONSE_CODES_200_204_404, resp).thenCompose(res -> {
                    return toJsonObject(rc, ServiceConstants.COMMERCE_DELIVERY_SEARCH_FACADE, uri.toString(), res);
                });
            });
            return LOGGER.rcExit(rc, result);
        } catch (URISyntaxException e) {
            return getFailedFuture(rc, e);
        }
    }


    public CompletableFuture<JsonObject> loadProductsForCategory(RuntimeContext rc, MicroServiceHttpClient client, String store, String category, int pageSize, int pageNumber) {
        LOGGER.rcEntry(rc, category);
        try {
            URI uri = URLUtil.addParams("/shop/v1/products", IDCConstants.SHOP_API_PARAM_STORE, store, IDCConstants.SHOP_API_PARAM_CATEGORY, category, IDCConstants.SHOP_API_PARAM_PAGE_SIZE, Integer.toString(pageSize));
            CompletableFuture<JsonObject> result = client.get(rc, ServiceConstants.COMMERCE_DELIVERY_SEARCH_FACADE, uri.toString(), RETRY_POLICY, DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT).thenCompose(resp -> {
                LOGGER.rcTrace(rc, "loadProductsForCategory response: " + resp.statusMessage());
                return ResponseHelper.assertResponseCode(rc, ServiceConstants.COMMERCE_DELIVERY_SEARCH_FACADE, RESPONSE_CODES_200, resp).thenCompose(res -> {
                    return toJsonObject(rc, ServiceConstants.COMMERCE_DELIVERY_SEARCH_FACADE, uri.toString(), res);
                });
            });
            return LOGGER.rcExit(rc, result);
        } catch (URISyntaxException e) {
            return getFailedFuture(rc, e);
        }
    }

    public CompletableFuture<JsonObject> loadProductById(RuntimeContext rc, MicroServiceHttpClient client, String store, String productId) {
        LOGGER.rcEntry(rc, store, productId);
        try {
            URI uri = URLUtil.addParams("/shop/v1/products/" + productId, "store", store);
            CompletableFuture<JsonObject> result = client.get(rc, ServiceConstants.COMMERCE_DELIVERY_SEARCH_FACADE, uri.toString(), RETRY_POLICY, DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT).thenCompose(resp -> {
                LOGGER.rcTrace(rc, "loadProductsForCategory response: " + resp.statusMessage());
                return ResponseHelper.assertResponseCode(rc, ServiceConstants.COMMERCE_DELIVERY_SEARCH_FACADE, RESPONSE_CODES_200, resp).thenCompose(res -> {
                    return toJsonObject(rc, ServiceConstants.COMMERCE_DELIVERY_SEARCH_FACADE, uri.toString(), res);
                });
            });
            return LOGGER.rcExit(rc, result);
        } catch (URISyntaxException e) {
            return getFailedFuture(rc, e);
        }
    }

    private CompletableFuture<JsonObject> getFailedFuture(RuntimeContext rc, URISyntaxException e) {
        LOGGER.rcCatching(rc, e);
        CompletableFuture<JsonObject> failed = new CompletableFuture<JsonObject>();
        failed.completeExceptionally(e);
        return failed;
    }

    public static CompletableFuture<JsonObject> toJsonObject(final RuntimeContext rc, final String serviceName, final String requestURI, final HttpClientResponse response) {
        final CompletableFuture<JsonObject> res = new CompletableFuture<>();
        response.exceptionHandler(res::completeExceptionally);
        response.bodyHandler(buff -> {
            Object responseBody = null;
            if (response.statusCode() == 404) {
                res.complete(new JsonObject());
            }
            else {
                // also in the error case, try to read the response body
                if ((buff == null) || (!StringUtils.isStringNonEmpty(buff.toString()))) {
                    // no response body
                } else {
                    try {
                        responseBody = buff.toJsonObject();
                    } catch (DecodeException e) { // NOSONAR
                        responseBody = buff.toString(StandardCharsets.UTF_8);
                        LOGGER.rcDebug(rc, "Response Body could not be decoded as JSON serviceName: " + serviceName + ", buffer: " + buff + " response: " + response);
                    }
                }
                if (responseBody == null) {
                    res.complete(new JsonObject());
                } else if (responseBody instanceof JsonObject) {
                    res.complete((JsonObject) responseBody);
                } else {
                    res.completeExceptionally(new ServiceAccessException("Failed: " + responseBody));
                }
            }
        });
        return res;
    }

}
