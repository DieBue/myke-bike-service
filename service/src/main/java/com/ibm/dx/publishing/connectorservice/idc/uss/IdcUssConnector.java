/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.publishing.connectorservice.idc.uss;

import static com.ibm.dx.publishing.connectorservice.idc.uss.ItemImpl.ITEM_COMPARATOR;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.ibm.dx.publishing.common.api.http.MicroServiceHttpClient;
import com.ibm.dx.publishing.common.api.http.RetryPolicy;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.common.api.reporting.MessageBusProducerClient;
import com.ibm.dx.publishing.common.api.services.ServiceConstants;
import com.ibm.dx.publishing.common.utils.URLUtil;
import com.ibm.dx.publishing.connectorservice.ConnectorConstants;
import com.ibm.dx.publishing.connectorservice.KafkaInvalidationManager;
import com.ibm.dx.publishing.connectorservice.idc.Constants;
import com.ibm.dx.publishing.connectorservice.idc.DataAccessor;
import com.ibm.dx.publishing.connectorservice.idc.PageKinds;
import com.ibm.dx.publishing.connectorservice.idc.PageOverrides;
import com.ibm.dx.publishing.connectorservice.idc.SiteConfig;
import com.ibm.dx.publishing.connectorservice.idc.TRSConfig;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.ConnectorException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.InvalidCatalogNodeException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.MultipleCatalogNodesFound;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.NoCatalogNodeFoundException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.NoCategoriesFoundException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.NoCommerceConfigFoundException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.SkipException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.SkipOnMissingConfigException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.SkipPageDeletionException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.SkipTemplatePageUpdateException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.TemplatePageNotFoundException;
import com.ibm.dx.publishing.connectorservice.mediation.SearchSchemaConstants;
import com.ibm.dx.publishing.connectorservice.util.ExceptionUtil;
import com.ibm.dx.publishing.connectorservice.util.StringUtil;
import com.ibm.dx.publishing.connectorservice.vertx.ConnectorServiceVerticle;
import com.ibm.wps.util.StringUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class IdcUssConnector {
    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2017";

    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(IdcUssConnector.class);

    private static final IdcUssConnector INSTANCE = new IdcUssConnector();

    private static final String ASS_CATALOG_PAGE_FIELDS = "id," + Constants.AUTHORING_SITES_DEFAULT_CATALOG_PAGE_IDS;

    private RetryPolicy RETRY_POLICY = RetryPolicy.on50x(2);
    
    private static final JsonArray EMPTY_JSON_ARRAY = new JsonArray();
    
	// deleting products deleted up to 10 seconds before the sync call 
	// this is required to avoid race conditions if multiple sync requests are issued for the same tenant
    private static final int ITEM_DELETION_GRACE_PERIOD_IN_SECONDS = 16;


    public static final IdcUssConnector getInstance() {
        return INSTANCE;
    }

    private IdcUssConnector() {
    }

    public CompletableFuture<List<SiteProcessingContext>> trigger(RuntimeContext rc, MicroServiceHttpClient microServiceHttpClient, MessageBusProducerClient messageBusProducerClient) {
        return trigger(rc, ConnectorServiceVerticle.getSiteProcessingContexts(rc, microServiceHttpClient, messageBusProducerClient, false));
    }
    
    public CompletableFuture<SiteProcessingContext> publishItemOverride(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc, ctx);
        CompletableFuture<SiteProcessingContext> result =
        		resolveSiteIdForItemOverrides(ctx)
                .thenCompose(this::resolveCatalogPage)
                .thenCompose(this::loadIdcConfigFromSite)
                .thenCompose(this::loadIdcConfigFromTRS)
                .thenCompose(this::loadItemOverridesForSinglePage)
                .thenCompose(this::loadDefaultPageTemplates)
                .thenCompose(this::loadCategories)
                .thenCompose(this::checkAndProcessCategoryOverride)
                .thenCompose(ProductResolutionHelper.INSTANCE::resolveProduct);

        return LOGGER.rcExit(rc, result);
    }
    
    public CompletableFuture<SiteProcessingContext> deleteItemOverride(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc, ctx);
        CompletableFuture<SiteProcessingContext> result = 
        		resolveCatalogResourceIdForDeletedPage(ctx)
                .thenCompose(this::resolveCatalogPage)
                .thenCompose(this::loadIdcConfigFromSite)
                .thenCompose(this::loadIdcConfigFromTRS)
                .thenCompose(this::buildEmptyItemOverrides)
                .thenCompose(this::loadDefaultPageTemplates)
                .thenCompose(this::loadCategories)
                .thenCompose(this::checkAndProcessCategoryOverride)
                .thenCompose(ProductResolutionHelper.INSTANCE::resolveProduct);

        return LOGGER.rcExit(rc, result);
    }

    public CompletableFuture<List<SiteProcessingContext>> trigger(RuntimeContext rc, CompletableFuture<List<SiteProcessingContext>> sites) {
        LOGGER.rcEntry(rc, sites);
        final CompletableFuture<List<SiteProcessingContext>> result = new CompletableFuture<List<SiteProcessingContext>>();
        sites.thenAccept(listOfSiteProcessingContexts -> {
        	recursiveTriggerSite(rc, listOfSiteProcessingContexts, 0, CompletableFuture.completedFuture(null)).thenAccept(v -> {
            	result.complete(listOfSiteProcessingContexts);
        	}).exceptionally(th -> {
            	LOGGER.rcCatching(rc, th);
            	result.completeExceptionally(th);
            	return null;
            });
        }).exceptionally(th -> {
        	LOGGER.rcCatching(rc, th);
        	result.completeExceptionally(th);
        	return null;
        });
        return LOGGER.rcExit(rc, result);
    }

	public CompletableFuture<SiteProcessingContext> recursiveTriggerSite(final RuntimeContext rc, final List<SiteProcessingContext> listOfSiteProcessingContexts, final int index, final CompletableFuture<SiteProcessingContext> result) {
		LOGGER.rcEntry(rc, index, listOfSiteProcessingContexts);
		if (index == listOfSiteProcessingContexts.size()) {
			result.complete(null);
			LOGGER.exit("end of recusrsion");
			return result;
		} else {
			CompletableFuture<SiteProcessingContext> newFuture = new CompletableFuture<>();
			triggerSite(rc, listOfSiteProcessingContexts.get(index)).thenAccept(v -> {
				recursiveTriggerSite(rc, listOfSiteProcessingContexts, index + 1, newFuture).thenAccept(r -> {
					newFuture.complete(listOfSiteProcessingContexts.get(index));
				}).exceptionally(th -> {
					LOGGER.rcCatching(rc, th);
					newFuture.completeExceptionally(th);
					return null;
				});
			}).exceptionally(th -> {
                Throwable cause = ExceptionUtil.getCompletionExceptionCause(rc, th);
                if (cause instanceof SkipException) {
                	LOGGER.rcTrace(rc,  "skipping context:" + listOfSiteProcessingContexts.get(index));
                	listOfSiteProcessingContexts.get(index).setSkipped(true);
                	recursiveTriggerSite(rc, listOfSiteProcessingContexts, index + 1, newFuture).thenAccept(r -> {
    					newFuture.complete(listOfSiteProcessingContexts.get(index));
    				}).exceptionally(th2 -> {
    					LOGGER.rcCatching(rc, th2);
    					newFuture.completeExceptionally(th2);
    					return null;
    				});                }
                else {
    				LOGGER.rcCatching(rc, th);
    				newFuture.completeExceptionally(th);
                }
				return null;
			});
			return LOGGER.rcExit(rc, newFuture);
		}
	}

    private CompletableFuture<SiteProcessingContext> triggerSite(RuntimeContext rc, SiteProcessingContext sps) {
        LOGGER.rcEntry(rc, sps);
        CompletableFuture<SiteProcessingContext> result = 
        		loadIdcConfigFromSite(sps)
                .thenCompose(this::resolveCatalogPage)
                .thenCompose(this::loadIdcConfigFromTRS)
                .thenCompose(this::loadItemOverrides)
                .thenCompose(this::loadDefaultPageTemplates)
                .thenCompose(this::loadCategories)
                .thenCompose(ProductResolutionHelper.INSTANCE::resolveProducts)
                .thenCompose(this::postCategoryPages)
                .thenCompose(this::removeUnneededDynPages)
        		.thenCompose(this::sendCatalogPageInvalidationMessage);
        return LOGGER.rcExit(rc, result);
    }

    
    public CompletableFuture<SiteProcessingContext> delete(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc, ctx);
        CompletableFuture<SiteProcessingContext> result = removeAllConnectorDynPages(ctx);
        return LOGGER.rcExit(rc, result);
    }

    private CompletableFuture<SiteProcessingContext> removeAllConnectorDynPages(SiteProcessingContext ctx) {
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

        try {
            URI route = URLUtil.addParams("/delivery/v1/search", "q", "systemTags:\"" + Model.SYSTEM_TAG_PREFIX_CONNECTOR + ctx.getConnectorId() + "\"");
            LOGGER.rcEntry(ctx.getRc(), "Deleting page route: " + route.toString());
            ctx.getMicroServiceHttpClient().delete(ctx.getRc(), ServiceConstants.SEARCH_FACADE_DELIVERY_SERVICE_NAME, route.toString(), RETRY_POLICY, ConnectorConstants.DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT).whenComplete((sr, th) -> {
                if (th != null) {
                    LOGGER.rcCatching(ctx.getRc(), th);
                    result.completeExceptionally(th);
                } else {
                    LOGGER.rcTrace(ctx.getRc(), "Dyn pages clean up suceeded.");
                    result.complete(ctx);
                }
            });
        } catch (URISyntaxException e) {
            LOGGER.rcCatching(ctx.getRc(), e);
            result.completeExceptionally(e);
        }

        return result;
    }

    private CompletableFuture<SiteProcessingContext> removeUnneededDynPages(SiteProcessingContext ctx) {
        LOGGER.rcEntry(ctx.getRc());
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

        try {
            URI route = URLUtil.addParams("/delivery/v1/search", "q", "lastModified:[* TO "+ ctx.getTimeStamp().minusSeconds(ITEM_DELETION_GRACE_PERIOD_IN_SECONDS).toString() + "] AND systemTags:\"" + Model.SYSTEM_TAG_PREFIX_CONNECTOR + ctx.getConnectorId() + "\"", "fq", "classification:page", "fq", "siteId:" + ctx.getSiteId());
            LOGGER.rcTrace(ctx.getRc(), "Deleting page route: " + route.toString());
            ctx.getMicroServiceHttpClient().delete(ctx.getRc(), ServiceConstants.SEARCH_FACADE_DELIVERY_SERVICE_NAME, route.toString(), RETRY_POLICY, ConnectorConstants.DEFAULT_REQUEST_CONSUMER_WITH_TIMEOUT).whenComplete((sr, th) -> {
                if (th != null) {
                    LOGGER.rcCatching(ctx.getRc(), th);
                    result.completeExceptionally(th);
                } else {
                    LOGGER.rcTrace(ctx.getRc(), "Dyn pages clean up suceeded.");
                    result.complete(ctx);
                }
            });
        } catch (URISyntaxException e) {
            LOGGER.rcCatching(ctx.getRc(), e);
            result.completeExceptionally(e);
        }

        return LOGGER.rcExit(ctx.getRc(), result);
    }

	private CompletableFuture<SiteProcessingContext> sendCatalogPageInvalidationMessage(SiteProcessingContext ctx) {
		RuntimeContext rc = ctx.getRc();
		LOGGER.rcEntry(rc);
		CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

		String catalogPageId = ctx.getCatalogPage().getString("id");
		if (StringUtils.isStringNonEmpty(catalogPageId)) {
			KafkaInvalidationManager
					.sendInvalidationMessage(ctx.getRc(), ctx.getMessageBusProducerClient(), catalogPageId)
					.thenAccept(v -> {
						result.complete(ctx);
					}).exceptionally(th -> {
						LOGGER.rcCatching(rc, th);
						result.completeExceptionally(th);
						return null;
					});
		} else {
			result.complete(ctx);
		}

		return LOGGER.rcExit(rc, result);
	}

    private CompletableFuture<SiteProcessingContext> loadIdcConfigFromTRS(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc);
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

        DataAccessor.getInstance().loadTRSRecord(rc, ctx.getMicroServiceHttpClient()).thenAccept(json -> {
            LOGGER.rcTrace(rc, "TRS record: " + StringUtil.truncateObject(json, 2000));
            TRSConfig config = new TRSConfig(ctx.getRc(), json);
            if (config.isValid()) {
                ctx.setTRSConfig(config);
                result.complete(ctx);
            } else {
                if (ctx.isSkipOnMissingConfig()) {
                    result.completeExceptionally(new SkipOnMissingConfigException());
                }
                else {
                    result.completeExceptionally(new NoCommerceConfigFoundException());
                }
            }
        }).exceptionally(th -> {
            LOGGER.rcCatching(rc, th);
            result.completeExceptionally(th);
            return null;
        });

        return result;
    }

    private CompletableFuture<SiteProcessingContext> resolveSiteIdForItemOverrides(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc);
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

        String pageId = ctx.getItemOverrideContext().getId();
        if (pageId.contains(":")) {
            LOGGER.rcTrace(rc, "Skipping draft page");
            result.completeExceptionally(new SkipTemplatePageUpdateException());
        }
        else {
            DataAccessor.getInstance().getFullPageById(rc, ctx.getMicroServiceHttpClient(), pageId).thenAccept(page -> {
                LOGGER.rcTrace(rc, "modified page: " + StringUtil.truncateObject(page, 2000));
                String siteId = page.getString("siteId");
                if (siteId == null) {
                    LOGGER.rcWarn(rc, "Item override page does not have a siteId value. Assuming default");
                	siteId = "default";
                }
                LOGGER.rcTrace(rc, "siteId: " + siteId);
                ctx.setSiteId(ctx.getRc(), siteId);
                result.complete(ctx);
            }).exceptionally(th -> {
                LOGGER.rcCatching(rc, th);
                Throwable cause = ExceptionUtil.getCompletionExceptionCause(rc, th);
                if (cause instanceof ConnectorException) {
                	if (((ConnectorException)cause).getStatusCode() == 404) {
                		if (ctx.isSkipOnMissingConfig()) {
                	        LOGGER.rcTrace(rc, "skipping ...");
                			result.completeExceptionally(new SkipOnMissingConfigException());
                			return null;
                		}
                	}
                }
                result.completeExceptionally(th);
                return null;
            });
        }

        return LOGGER.rcExit(rc, result);
    }

    private CompletableFuture<SiteProcessingContext> loadIdcConfigFromSite(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc);
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

        DataAccessor.getInstance().loadSiteRecord(rc, ctx.getMicroServiceHttpClient(), ctx.getSiteId()).thenAccept(json -> {
            LOGGER.rcTrace(rc, "Site record: " + StringUtil.truncateObject(json, 2000));
            SiteConfig config = new SiteConfig(ctx.getRc(), json);
            if (config.isValid()) {
                ctx.setSiteConfig(config);
                result.complete(ctx);
            } else {
                result.completeExceptionally(new SkipOnMissingConfigException());
            }
        }).exceptionally(th -> {
            LOGGER.rcCatching(rc, th);
            result.completeExceptionally(th);
            return null;
        });

        return LOGGER.rcExit(rc, result);
    }

    private CompletableFuture<SiteProcessingContext> loadItemOverrides(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc);
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

        DataAccessor.getInstance().getFullPagesByKind(rc, ctx.getMicroServiceHttpClient(), ctx.getSiteId(), PageKinds.ITEM_OVERRIDE_PAGE).thenAccept(json -> {
            LOGGER.rcTrace(rc, "Item overrides loaded: " + StringUtil.truncateObject(json, 5000));
            JsonArray itemOverrides = json.getJsonArray("items");
            PageOverrides po;
            try {
                po = new PageOverrides(rc, ctx.getCatalogPage().getJsonObject(Constants.AUTHORING_SITES_DEFAULT_CATALOG_PAGE_IDS), itemOverrides);
                ctx.setPageOverrides(po);
                result.complete(ctx);
            } catch (InvalidCatalogNodeException e) {
                LOGGER.rcCatching(rc, e);
                result.completeExceptionally(e);
            }
        }).exceptionally(th -> {
            LOGGER.rcCatching(rc, th);
            result.completeExceptionally(th);
            return null;
        });

        return LOGGER.rcExit(rc, result);
    }

    private CompletableFuture<SiteProcessingContext> buildEmptyItemOverrides(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc);
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

        PageOverrides po;
        try {
            po = new PageOverrides(rc, ctx.getCatalogPage().getJsonObject(Constants.AUTHORING_SITES_DEFAULT_CATALOG_PAGE_IDS), EMPTY_JSON_ARRAY);
            ctx.setPageOverrides(po);
            result.complete(ctx);
        } catch (InvalidCatalogNodeException e) {
            LOGGER.rcCatching(rc, e);
            result.completeExceptionally(e);
        }

        return LOGGER.rcExit(rc, result);
    }


    private CompletableFuture<SiteProcessingContext> loadItemOverridesForSinglePage(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc);
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

        String pageId = ctx.getItemOverrideContext().getId();
        if (pageId.contains(":")) {
            LOGGER.rcTrace(rc, "Skipping draft page");
            result.completeExceptionally(new SkipTemplatePageUpdateException());
        }
        else {
            DataAccessor.getInstance().getFullPageById(rc, ctx.getMicroServiceHttpClient(), pageId).thenAccept(json -> {
                LOGGER.rcTrace(rc, "Item override loaded: " + StringUtil.truncateObject(json, 5000));
                ctx.getItemOverrideContext().setPageTemplate(json);
                JsonArray itemOverrides = new JsonArray().add(json);
                PageOverrides po;
                try {
                    po = new PageOverrides(rc, ctx.getCatalogPage().getJsonObject(Constants.AUTHORING_SITES_DEFAULT_CATALOG_PAGE_IDS), itemOverrides);
                    ctx.setPageOverrides(po);
                    result.complete(ctx);
                } catch (InvalidCatalogNodeException e) {
                    LOGGER.rcCatching(rc, e);
                    result.completeExceptionally(e);
                }
            }).exceptionally(th -> {
                LOGGER.rcCatching(rc, th);
                result.completeExceptionally(th);
                return null;
            });
        }

        return LOGGER.rcExit(rc, result);
    }

    /**
     * Loads the externalResourceId value from the page in delivery collection. This is required for the delete item override use case.
     * @param ctx
     * @return
     */
	private CompletableFuture<SiteProcessingContext> resolveCatalogResourceIdForDeletedPage(SiteProcessingContext ctx) {
		RuntimeContext rc = ctx.getRc();
		LOGGER.rcEntry(rc);
		CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

		String pageId = ctx.getItemOverrideContext().getId();
		if (pageId.contains(":")) {
			LOGGER.rcTrace(rc, "Skipping draft page");
			result.completeExceptionally(new SkipTemplatePageUpdateException());
		} else {
			DataAccessor.getInstance().searchDynamicItemOverridePage(rc, ctx.getMicroServiceHttpClient(), pageId).thenAccept(json -> {
				LOGGER.rcTrace(rc, "search result: " + json);
				JsonArray docs = json.getJsonArray("documents");
				if (docs != null) {
					if (docs.size() > 0) {
						JsonObject itemOverridePage = docs.getJsonObject(0);
						String siteId = itemOverridePage.getString("siteId");
						siteId = (siteId == null) ? "default" : siteId;
						LOGGER.rcTrace(rc, "site id: " + json);
						ctx.setSiteId(rc, siteId);
						String catalogResourceId = Model.getSystemTagValue(rc, itemOverridePage.getJsonArray(SearchSchemaConstants.Common.SYSTEM_TAGS), Model.SYSTEM_TAG_PREFIX_ID);
						ctx.getItemOverrideContext().setCatalogResourceId(catalogResourceId);
						result.complete(ctx);
					} else {
						LOGGER.rcWarn(rc, "No override found. This is not a problem. The deleted page just was no item override page");
						result.completeExceptionally(new SkipPageDeletionException());
					}
				} else {
					LOGGER.rcWarn(rc, "No override found. This is not a problem. The deleted page just was no item override page");
					result.completeExceptionally(new SkipPageDeletionException());
				}
			}).exceptionally(th -> {
				LOGGER.rcCatching(rc, th);
				result.completeExceptionally(th);
				return null;
			});
		}
		return LOGGER.rcExit(rc, result);
	}

    private CompletableFuture<SiteProcessingContext> resolveCatalogPage(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc);
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

        DataAccessor.getInstance().getPagesByKind(rc, ctx.getMicroServiceHttpClient(), ctx.getSiteId(), PageKinds.CATALOG_PAGE, ASS_CATALOG_PAGE_FIELDS).thenAccept(json -> {
            LOGGER.rcTrace(rc, "catalog page loaded: " + json);
            JsonArray items = json.getJsonArray("items");
            if (items != null) {
                if (items.size() == 1) {
                    JsonObject catalogPage = items.getJsonObject(0);
                    ctx.setCatalogPage(catalogPage);
                    result.complete(ctx);
                } else if (items.size() > 1) {
                    LOGGER.rcWarn(rc, "Multiple catalog pages found: " + items);
                    result.completeExceptionally(new MultipleCatalogNodesFound());
                }
                else {
                    LOGGER.rcWarn(rc, "No catalog pages found.");
                    if (ctx.isSkipOnMissingConfig()) {
                        result.completeExceptionally(new SkipOnMissingConfigException());
                    }
                    else {
                        result.completeExceptionally(new NoCatalogNodeFoundException());
                    }
                }
            }
            else {
                LOGGER.rcWarn(rc, "No catalog pages found.");
                result.completeExceptionally(new NoCatalogNodeFoundException());
            }
        }).exceptionally(th -> {
            LOGGER.rcCatching(rc, th);
            result.completeExceptionally(th);
            return null;
        });

        return LOGGER.rcExit(rc, result);
    }

    private CompletableFuture<SiteProcessingContext> loadDefaultPageTemplates(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc);
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

        DataAccessor.getInstance().getFullPagesByKind(rc, ctx.getMicroServiceHttpClient(), ctx.getSiteId(), PageKinds.CATALOG_DEFAULT_PAGE.toString()).thenAccept(json -> {
            LOGGER.rcTrace(rc, "Default page templates loaded: " + StringUtil.truncateObject(json, 5000));
            JsonArray defaultTemplates = json.getJsonArray("items");
            try {
                ctx.getPageOverrides().addDefaultTemplatePages(rc, defaultTemplates);
            } catch (TemplatePageNotFoundException e) {
            	if (ctx.isSkipOnMissingConfig()) {
                    LOGGER.rcTrace(rc, "Not all template pages are availbale. Assuming no problem since we run with skipOnMissingConfig=true");
                    result.completeExceptionally(new SkipOnMissingConfigException());
            	}
            	else {
                    LOGGER.rcCatching(rc, e);
                    result.completeExceptionally(e);
            	}
            }
            result.complete(ctx);
        }).exceptionally(th -> {
            LOGGER.rcCatching(rc, th);
            result.completeExceptionally(th);
            return null;
        });

        return LOGGER.rcExit(rc, result);
    }

    private CompletableFuture<SiteProcessingContext> loadCategories(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc);
                
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();
        
        DataAccessor.getInstance().loadTopLevelCategories(rc, ctx.getMicroServiceHttpClient(), ctx.getSiteConfig().getStoreIdentifier()).thenAccept(json -> {
            LOGGER.rcTrace(rc, "category pages loaded: " + json);
            if (json.isEmpty()) {
                LOGGER.rcWarn(rc, "No categories found: siteConfig" + ctx.getSiteConfig() + " catalogConfig: " + ctx.getCatalogPage());
                if (ctx.isSkipOnMissingConfig()) {
                    result.completeExceptionally(new SkipOnMissingConfigException());
                }
                else {
                    result.completeExceptionally(new NoCategoriesFoundException());
                }
            }
            else {
                ctx.setCategories(UssModel.getCategories(rc, json));
                result.complete(ctx);
            }
        }).exceptionally(th -> {
            LOGGER.rcCatching(rc, th);
            result.completeExceptionally(th);
            return null;
        });
        return LOGGER.rcExit(rc, result);
    }

    private CompletableFuture<SiteProcessingContext> checkAndProcessCategoryOverride(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc);
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<>();

        ItemOverrideContext overrideCtx = ctx.getItemOverrideContext();
        if (overrideCtx == null) {
            result.complete(ctx);
        } else {
            String catalogResourceId = overrideCtx.getCatalogResourceId();
            Category cat = ctx.getCategories().get(catalogResourceId);
            if (cat == null) {
                LOGGER.rcTrace(rc, "No category found for catalogResourceId: " + catalogResourceId + ", assuming product id ...");
                ctx.setSkipProductResolution(false);
                result.complete(ctx);
            } else {
                ctx.setSkipProductResolution(true);
                resolvePositionValues(ctx);

                JsonArray pages = new JsonArray();
                JsonObject searchDoc = new JsonObject().put("documents", pages);
				DataAccessor.getInstance().loadProductsForCategory(rc, ctx.getMicroServiceHttpClient(), ctx.getSiteConfig().getStoreIdentifier(), cat.getId(), 1, 1).thenAccept(products -> {
	                try {
	                	UssModel.checkHasProducts(rc, products, cat);
	                    pages.add(Model.buildDynPage(rc, ctx, cat, ctx.getCategories().get(cat.getParentId())));
	                    LOGGER.rcTrace(rc, "Posting category page for item override: " + pages);
	                    ctx.getDeliverySearchController().updateDocuments(searchDoc).whenComplete((sr, throwable) -> {
	                        if (throwable != null) {
	                            LOGGER.rcCatching(rc, throwable);
	                            result.completeExceptionally(throwable);
	                        } else {
	                            LOGGER.rcTrace(rc, "Search post successful");
	                            result.complete(ctx);
	                        }
	                    });
	                } catch (ConnectorException | URISyntaxException e) {
	                    LOGGER.rcTrace(rc, "missing template", ctx);
	                    result.completeExceptionally(e);
	                }

				}).exceptionally(th -> {
					LOGGER.rcCatching(rc, th);
                    result.completeExceptionally(th);
                    return null;
				});
            }
        }

        return LOGGER.rcExit(rc, result);
    }


	private String getParentCategory(RuntimeContext rc, Object parentCategories) {
		LOGGER.rcEntry(rc, parentCategories);
		String result = null;

		if (parentCategories != null) {
			if (parentCategories instanceof JsonArray) {
				JsonArray ca = (JsonArray) parentCategories;
				if (ca.size() > 0) {
					result = ca.getString(0);
				}
			}
			// workaround for Defect 150744
//        	else if (parentCategories instanceof String) {
//        		result = (String)parentCategories;
//        	}
		}

		return LOGGER.rcExit(rc, result);
	}


    private CompletableFuture<SiteProcessingContext> postCategoryPages(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc);
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<SiteProcessingContext>();

        resolvePositionValues(ctx);
        
        JsonArray pages = new JsonArray();
        JsonObject searchDoc = new JsonObject().put("documents", pages);

        HashMap<String, Category> cats = ctx.getCategories();
        Iterator<Category> it = cats.values().iterator();
        while (it.hasNext()) {
            Category c = it.next();
            LOGGER.rcTrace(rc, "Adding search doc for category: " + c.getName());
            try {
                pages.add(Model.buildDynPage(rc, ctx, c, cats.get(c.getParentId())));
            } catch (ConnectorException | URISyntaxException e) {
                LOGGER.rcTrace(rc, "missing template", ctx);
                result.completeExceptionally(e);
                return LOGGER.rcExit(rc, result);
            }
        }

        if (pages.isEmpty()) {
            LOGGER.rcTrace(rc, "No categories found. Skipping");
            result.complete(ctx);
        } else {
            LOGGER.rcTrace(rc, "Posting category pages. Size: " + pages.size());
            ctx.getDeliverySearchController().updateDocuments(searchDoc).whenComplete((sr, throwable) -> {
                if (throwable != null) {
                    LOGGER.rcCatching(rc, throwable);
                    result.completeExceptionally(throwable);
                } else {
                    LOGGER.rcTrace(rc, "Search post successful");
                    result.complete(ctx);
                }
            });
        }

        return LOGGER.rcExit(rc, result);
    }

    private void resolvePositionValues(SiteProcessingContext ctx) {
        Collection<Category> cats = ctx.getCategories().values();
        List<Category> parentCategories = new ArrayList<>();

        for (Category category : cats) {
            category.resolveChildPositionValues(ctx.getRc(), ctx.getCategories());
            if (category.getParentId() == null) {
                parentCategories.add(category);
            }
        }

        // resolve position values for parent categories (categories where no parent id is set)
        parentCategories.sort(ITEM_COMPARATOR);
        for (int i = 0; i < parentCategories.size(); i++) {
            parentCategories.get(i).setPosition(i);
        }
    }
}
