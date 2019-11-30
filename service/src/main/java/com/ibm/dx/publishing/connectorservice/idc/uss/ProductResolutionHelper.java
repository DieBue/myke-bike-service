/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package com.ibm.dx.publishing.connectorservice.idc.uss;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;

import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.connectorservice.idc.DataAccessor;
import com.ibm.dx.publishing.connectorservice.idc.IDCConstants;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.CategoryNotFoundException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.ConnectorException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.MissingParentCategoriesException;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.SkipTemplatePageUpdateException;
import com.ibm.dx.publishing.connectorservice.util.StringUtil;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class ProductResolutionHelper {

	private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(ProductResolutionHelper.class);
	
	public static final ProductResolutionHelper INSTANCE = new ProductResolutionHelper();
    private static final int IDC_PRODUCTS_PAGE_SIZE = 50;

	
    public CompletableFuture<SiteProcessingContext> resolveProduct(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc, ctx.toSummary());
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<>();
        if (ctx.skipProductResolution()) {
            LOGGER.rcTrace(rc, "skipping product resolution step");
            result.complete(ctx);
        }
        else {
			CompletableFuture<JsonObject> loadProduct = DataAccessor.getInstance().loadProductById(rc, ctx.getMicroServiceHttpClient(), ctx.getSiteConfig().getStoreIdentifier(), ctx.getItemOverrideContext().getCatalogResourceId());
			loadProduct.thenAccept(loadProductResult -> {
				JsonArray data = loadProductResult.getJsonArray(IDCConstants.DATA);
				if ((data != null) && (data.size() == 1)) {
					JsonObject product = data.getJsonObject(0);
		            LOGGER.rcTrace(rc, "Product: " + StringUtil.truncateObject(product, 2000));
					String categoryId = getParentCategory(rc, product.getValue(IDCConstants.PARENT_CATEGORIES));
					if (categoryId != null) {
						Category cat = ctx.getCategories().get(categoryId);
						if (cat != null) {
							LOGGER.rcTrace(rc, "Containing category for product " + product.getString(IDCConstants.IDENTIFIER) + ": " + cat);
							resolveProductsForCategory(new ProductResolutionContext(ctx, cat, IDC_PRODUCTS_PAGE_SIZE)).thenAccept(v -> {
								LOGGER.rcTrace(rc, "product resolved.");
								result.complete(ctx);
							}).exceptionally(th -> {
								LOGGER.rcCatching(ctx.getRc(), th);
								result.completeExceptionally(th);
								return null;
							});
						} else {
							LOGGER.rcError(rc, "Category not found: " + categoryId);
							result.completeExceptionally(new CategoryNotFoundException(categoryId));
						}
					} else {
						LOGGER.rcError(rc, "parentCategories property is missing: " + product);
						result.completeExceptionally(new MissingParentCategoriesException(product.encode()));
					}
				} else {
					LOGGER.rcWarn(rc, "product not found or ambigous: " + ctx.getItemOverrideContext().getCatalogResourceId() + ". items found: " + data.size());
					if (data.size() == 0) {
						result.completeExceptionally(new SkipTemplatePageUpdateException());
					} else {
						result.completeExceptionally(new SkipTemplatePageUpdateException());
					}
				}
			}).exceptionally(th -> {
				LOGGER.rcCatching(ctx.getRc(), th);
				result.completeExceptionally(th);
				return null;
			});
        }
        return LOGGER.rcExit(rc, result);
    }

	public CompletableFuture<SiteProcessingContext> resolveProducts(SiteProcessingContext ctx) {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc, ctx.toSummary());
        CompletableFuture<SiteProcessingContext> result = new CompletableFuture<>();
        if (ctx.skipProductResolution()) {
            LOGGER.rcTrace(rc, "skipping product resolution step");
            result.complete(ctx);
        }
        else {
            Collection<Category> cats = ctx.getCategories().values();
            @SuppressWarnings("rawtypes")
            CompletableFuture[] futures = new CompletableFuture[cats.size()];
            int i=0;
			for (Category category : cats) {
				futures[i++] = category.getProductsResolved();
				ProductResolutionContext prc = new ProductResolutionContext(ctx, category, IDC_PRODUCTS_PAGE_SIZE);
				resolveProductsForCategory(prc).thenAccept(res -> {
					LOGGER.rcTrace(rc, "product resolution done for category: " + category);
					prc.getCategory().getProductsResolved().complete(null);
				}).exceptionally(th -> {
					LOGGER.rcCatching(rc, th);
					prc.getCategory().getProductsResolved().completeExceptionally(th);
					return null;
				});
			}

            CompletableFuture.allOf(futures).thenAccept(v -> {
                LOGGER.rcTrace(ctx.getRc(), "Products resolved.");
                result.complete(ctx);
            }).exceptionally(th -> {
                LOGGER.rcCatching(ctx.getRc(), th);
                result.completeExceptionally(th);
                return null;
            });
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
		}

		return LOGGER.rcExit(rc, result);
	}

	private CompletableFuture<ProductResolutionContext> resolveProductsForCategory(final ProductResolutionContext prc) {
		CompletableFuture<ProductResolutionContext> result = new CompletableFuture<ProductResolutionContext>(); 
		countProductsInCategory(prc).thenAccept(ctx -> {
			recursiveResolveProductsForCategory(prc, CompletableFuture.completedFuture(prc)).thenAccept(res -> {
				result.complete(prc);
			}).exceptionally(th -> {
				LOGGER.rcCatching(prc.getRc(), th);
				result.completeExceptionally(th);
				return null;
			});;
		}).exceptionally(th -> {
			LOGGER.rcCatching(prc.getRc(), th);
			result.completeExceptionally(th);
			return null;
		});
		return result;
	}

	private CompletableFuture<ProductResolutionContext> recursiveResolveProductsForCategory(final ProductResolutionContext prc, CompletableFuture<ProductResolutionContext> result) {
        RuntimeContext rc = prc.getRc();
        LOGGER.rcEntry(rc, prc);
        if (!prc.hasMoreData(rc)) {
        	result.complete(prc);
        	LOGGER.rcTrace(rc, "end of recursion");
        	return LOGGER.rcExit(rc, result);
        }
		CompletableFuture<ProductResolutionContext> newFuture = new CompletableFuture<ProductResolutionContext>();
		doResolveProducts(prc).thenAccept(res -> {
			recursiveResolveProductsForCategory(prc.nextPage(), newFuture).thenAccept(v -> {
				newFuture.complete(prc);
			}).exceptionally(th -> {
				LOGGER.rcCatching(rc, th);
				newFuture.completeExceptionally(th);
				return null;
			});
		}).exceptionally(th -> {
			LOGGER.rcCatching(rc, th);
			newFuture.completeExceptionally(th);
			return null;
		});		
        
        return newFuture;
    }

	private CompletableFuture<ProductResolutionContext> doResolveProducts(final ProductResolutionContext prc) {
		SiteProcessingContext ctx = prc.getCtx();
        RuntimeContext rc = ctx.getRc();
        Category category = prc.getCategory();
        int pageSize = prc.getPageSize();
        int pageNumber = prc.getPageNumberZeroBased()+1;
        CompletableFuture<ProductResolutionContext> result = new CompletableFuture<ProductResolutionContext>();
		LOGGER.rcEntry(rc, category.getName(), pageNumber);
		DataAccessor.getInstance().loadProductsForCategory(rc, ctx.getMicroServiceHttpClient(), ctx.getSiteConfig().getStoreIdentifier(), category.getId(), pageSize, pageNumber).thenAccept(json -> {
            LOGGER.rcTrace(rc, "page " + pageNumber + " of products loaded for category " + category.getName() + ": " + StringUtil.truncateObject(json, 5000));
            ArrayList<Product> products = UssModel.getProducts(rc, json, category);
            if (products.size()>0) {
                for (int i = 0; i < products.size(); i++) {
                    products.get(i).setPosition((pageSize * pageNumber) + i);
                }
                JsonObject productsDocument;
                try {
                    productsDocument = buildProductsDocument(ctx, category, products);
                    if (productsDocument == null) {
                        LOGGER.rcTrace(rc, "Completing category that does not contain the item override product without posting products");
                        result.complete(prc);
                    }
                    else {
                        ctx.getDeliverySearchController().updateDocuments(productsDocument).whenComplete((sr, throwable) -> {
                            if (throwable != null) {
                                LOGGER.rcCatching(rc, throwable);
                                result.completeExceptionally(throwable);
                            } else {
                                LOGGER.rcTrace(rc, "Products posted successfully");
                                result.complete(prc);
                            }
                        });
                    }
                } catch (ConnectorException | URISyntaxException e) {
                    LOGGER.rcCatching(rc, e);
                    result.completeExceptionally(e);
                } 
            }
            else {
                LOGGER.rcTrace(rc, "No products found.");
                result.complete(prc);
            }
        }).exceptionally(th -> {
            LOGGER.rcCatching(rc, th);
            result.completeExceptionally(th);
            return null;
        });
		return LOGGER.rcExit(rc, result);
	}

    
    private CompletableFuture<Integer> countProductsInCategory(ProductResolutionContext prc) {
        RuntimeContext rc = prc.getRc();
        LOGGER.rcEntry(rc, prc.getCategory());
        SiteProcessingContext ctx = prc.getCtx();
        Category category = prc.getCategory();
        CompletableFuture<Integer> result = new CompletableFuture<Integer>();
        DataAccessor.getInstance().loadProductsForCategory(rc, ctx.getMicroServiceHttpClient(), ctx.getSiteConfig().getStoreIdentifier(), category.getId(), 1, 1).thenAccept(json -> {
        	Integer numFound = null;
        	JsonObject pagination = json.getJsonObject(IDCConstants.PAGINATION);
        	if (pagination != null) {
        		numFound = pagination.getInteger(IDCConstants.NUM_FOUND);
        	}
        	if (numFound != null) {
                LOGGER.rcDebug(rc, "Number of products found in category " + category.getName() + ": " + numFound);
            	prc.setNumFound(numFound);
        		result.complete(numFound);
        	}
        	else {
                LOGGER.rcWarn(rc, "No numFound given for category " + category.getName() + ". Assuming 0");
            	prc.setNumFound(0);
        		result.complete(0);
        	}
        }).exceptionally(th -> {
        	LOGGER.rcCatching(rc, th);
        	result.completeExceptionally(th);
        	return null;
        });
        
        return LOGGER.rcExit(rc, result);
    	
    }

    private JsonObject buildProductsDocument(SiteProcessingContext ctx, Category category, ArrayList<Product> products) throws ConnectorException, URISyntaxException {
        RuntimeContext rc = ctx.getRc();
        LOGGER.rcEntry(rc, products.size());

        JsonObject result = null;
        
        Iterator<Product> it = products.iterator();
        if (ctx.hasValidItemOverrideContext()) {
            LOGGER.rcTrace(rc, "filtering for item override ...");
            String ref = ctx.getItemOverrideContext().getCatalogResourceId();
            while (it.hasNext()) {
                Product p = it.next();
                if (ref.equals(p.getId())) {
                    LOGGER.rcTrace(rc, "Adding item override doc for product: " + p.getName());
                    JsonArray pages = new JsonArray();
                    pages.add(Model.buildDynPage(ctx.getRc(), ctx, p, category));
                    result = new JsonObject().put("documents", pages);
                }
            }
        }
        else {
            JsonArray pages = new JsonArray();
            result = new JsonObject().put("documents", pages);
            while (it.hasNext()) {
                Product p = it.next();
                LOGGER.rcTrace(rc, "Adding search doc for product: " + p.getName());
                pages.add(Model.buildDynPage(ctx.getRc(), ctx, p, category));
            }
        }
        LOGGER.rcExit(rc, StringUtil.truncateObject(result, 4000));
        return result;
    }

}
