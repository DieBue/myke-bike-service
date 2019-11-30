/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.test;

import static com.ibm.dx.publishing.common.api.services.ServiceConfig.CONFIG_HTTP_PORT;
import static com.ibm.dx.publishing.common.api.services.ServiceConstants.ASSET_SERVICE_NAME;
import static com.ibm.dx.publishing.common.api.services.ServiceConstants.CONTENT_SERVICE_NAME;
import static test.publishing.common.TestHelper.deployVerticleAsync;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.xml.sax.SAXException;

import com.ibm.dx.publishing.common.api.RuntimeContextFactory;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.common.api.services.ServiceConstants;
import com.ibm.dx.publishing.common.utils.URLUtil;
import com.ibm.dx.publishing.connectorservice.idc.SiteConfig;
import com.ibm.dx.publishing.connectorservice.idc.TRSConfig;
import com.ibm.dx.publishing.connectorservice.vertx.ConnectorServiceVerticle;
import com.ibm.dx.test.mocks.MockIDCVerticle;
import com.ibm.dx.test.mocks.MockUSSVerticle;
import com.ibm.wch.utilities.CompletableUtils;
import com.ibm.wps.util.StringUtils;
import com.ibm.wps.utilities.vertx.CompletableVertx;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import test.publishing.common.HttpUnitTestHelper;
import test.publishing.common.TestException;
import test.publishing.common.TestHelper;
import test.publishing.mocks.MockCompletableServiceRegistryClient;
import test.publishing.mocks.reporting.MockKafkaConsumer;
import test.publishing.mocks.reporting.MockKafkaProducer;
import test.publishing.mocks.verticles.MockAssetServiceVerticle;
import test.publishing.mocks.verticles.MockContentServiceVerticle;
import test.publishing.mocks.verticles.MockFileAccessServiceVerticle;
import test.publishing.mocks.verticles.MockResourceServiceVerticle;
import test.publishing.mocks.verticles.MockSearchServiceVerticle;
import test.publishing.mocks.verticles.MockSiteServiceVerticle;
import test.publishing.mocks.verticles.MockTenantRegistryServiceVerticle;
import test.publishing.mocks.verticles.helpers.MockData;

public abstract class BaseTest {

    private static RuntimeContextLogger LOGGER = RuntimeContextLogger.create(BaseTest.class);

    protected static Vertx vertx;
    protected static CompletableVertx certx;

    /**
     * List that holds the ids of all deployed verticles.
     * Prior to executing a test method, this list is automatically populated with all active deployment ids.
     * After executing a test method, all deployed verticles whose ids are present in that list are automatically
     * un-deployed.
     **/
    protected static List<String> deployedVerticles = Collections.synchronizedList(new ArrayList<>());


    protected MockCompletableServiceRegistryClient mockServiceRegistryClient; 
    protected MockContentServiceVerticle mockContentServiceVerticle;
    protected MockAssetServiceVerticle mockAssetServiceVerticle;
    protected MockResourceServiceVerticle mockResourceServiceVerticle;
    protected MockSearchServiceVerticle mockSearchServiceVerticle;
    protected MockSiteServiceVerticle mockSiteServiceVerticle;
    protected MockFileAccessServiceVerticle mockFileAccessServiceVerticle;
    protected MockTenantRegistryServiceVerticle mockTenantRegistryServiceVerticle;
    protected MockIDCVerticle mockIdcVerticle;
    protected MockUSSVerticle mockUssVerticle;
    protected ConnectorServiceVerticle connectorVerticle; 
    protected ConnectorKafkaConsumer mockKafkaConsumer;
    
    public static final String DEFAULT_EDGE_TOKEN = "{\"r\":\"111915\",\"s\":\"502576\",\"id\":\"8\",\"h\":\"dch-dxcloud.rtp.raleigh.ibm.com\"}";
    public static final String DEFAULT_TENANT = "00000000-0000-0000-0000-000000000010";
    
    public static final String CUSTOM_HOSTNAME_EDGE_TOKEN = "{\"r\":\"114821\",\"s\":\"507159\",\"id\":\"127\",\"ch\":\"uwe.wchtest.xyz\",\"sec\":true,\"ssl\":\"ENABLED\",\"eid\":\"4f9b3093caf478cf\"}";
    public static final String CUSTOM_HOSTNAME_TENANT = "552fd080-e4c6-4aca-9f5b-b2d618c0d617";

    protected RuntimeContext rc = getDefaultRC();
    protected RuntimeContext customHostnameRC = getCustomHostnameRC();

    protected String contentServiceURL;
    protected String siteServiceURL;
    protected String searchServiceURL;
    protected String connectorServiceURL;
    protected String assetServiceURL;
    protected String resourceServiceURL;
    protected String tenantRegistryServiceURL;
    protected String idcURL;
    protected String ussURL;

    @BeforeClass
    public static void setupClass(final TestContext context){
        vertx = Vertx.vertx();
        certx = CompletableVertx.get(vertx);
    }

    @Before
    public void setUp(final TestContext context) throws Exception {
        LOGGER.traceEntry();

        mockServiceRegistryClient = new MockCompletableServiceRegistryClient();
        mockKafkaConsumer = new ConnectorKafkaConsumer();		
		MockKafkaProducer mockKafkaProducer = new MockKafkaProducer(mockKafkaConsumer);

        // keep track of deployment status
        Collection<CompletionStage<String>> deploymentFutures = new ArrayList<>();
        deployedVerticles.clear();

        final int contentServicePort = TestHelper.getPort();
        mockContentServiceVerticle = new MockContentServiceVerticle();
        deploymentFutures.add(deployVerticleAsync(vertx, mockContentServiceVerticle, new JsonObject().put(CONFIG_HTTP_PORT, contentServicePort)));
        mockServiceRegistryClient.registerServiceInstance(CONTENT_SERVICE_NAME, new URI("http://localhost:" + contentServicePort));

        
        final int searchServicePort = TestHelper.getPort();
        mockSearchServiceVerticle = new MockSearchServiceVerticle(true);
        mockSearchServiceVerticle.setAddPushedDocumentsToExistingDocuments(true);
        deploymentFutures.add(deployVerticleAsync(vertx, mockSearchServiceVerticle, new JsonObject().put(CONFIG_HTTP_PORT, searchServicePort)));
        mockServiceRegistryClient.registerServiceInstance(ServiceConstants.SEARCH_FACADE_DELIVERY_SERVICE_NAME, new URI("http://localhost:" + searchServicePort));

        final int assetServicePort = TestHelper.getPort();
        mockAssetServiceVerticle = new MockAssetServiceVerticle();
        deploymentFutures.add(deployVerticleAsync(vertx, mockAssetServiceVerticle, new JsonObject().put(CONFIG_HTTP_PORT, assetServicePort)));
        mockServiceRegistryClient.registerServiceInstance(ASSET_SERVICE_NAME, new URI("http://localhost:" + assetServicePort));
        
        final int ressourceServicePort = TestHelper.getPort();
        mockResourceServiceVerticle = new MockResourceServiceVerticle();
        deploymentFutures.add(deployVerticleAsync(vertx, mockResourceServiceVerticle, new JsonObject().put(CONFIG_HTTP_PORT, ressourceServicePort)));
        mockServiceRegistryClient.registerServiceInstance(ServiceConstants.RESOURCE_SERVICE_NAME, new URI("http://localhost:" + assetServicePort));

        final int siteServicePort = TestHelper.getPort();
        mockSiteServiceVerticle = new MockSiteServiceVerticle();
        deploymentFutures.add(TestHelper.deployVerticleAsync(vertx, mockSiteServiceVerticle, new JsonObject().put(CONFIG_HTTP_PORT,  siteServicePort)));
        mockServiceRegistryClient.registerServiceInstance(ServiceConstants.SITE_SERVICE_NAME, new URI("http://localhost:" + siteServicePort));

        final int tenantRegistryServicePort = TestHelper.getPort();
        mockTenantRegistryServiceVerticle = new MockTenantRegistryServiceVerticle();
        deploymentFutures.add(TestHelper.deployVerticleAsync(vertx, mockTenantRegistryServiceVerticle, new JsonObject().put(CONFIG_HTTP_PORT,  tenantRegistryServicePort)));
        mockServiceRegistryClient.registerServiceInstance(ServiceConstants.TENANT_REGISTRY_SERVICE_NAME, new URI("http://localhost:" + tenantRegistryServicePort));

        final int deliveryResourceServicePort = TestHelper.getPort();
        mockFileAccessServiceVerticle = new MockFileAccessServiceVerticle();
        deploymentFutures.add(TestHelper.deployVerticleAsync(vertx, mockFileAccessServiceVerticle, new JsonObject().put(CONFIG_HTTP_PORT,  deliveryResourceServicePort)));
        mockServiceRegistryClient.registerServiceInstance(ServiceConstants.DELIVERY_RESOURCE_SERVICE_NAME, new URI("http://localhost:" + deliveryResourceServicePort));

        final int idcPort = TestHelper.getPort();
        mockIdcVerticle = new MockIDCVerticle();
        deploymentFutures.add(TestHelper.deployVerticleAsync(vertx, mockIdcVerticle, new JsonObject().put(CONFIG_HTTP_PORT,  idcPort)));

        final int ussPort = TestHelper.getPort();
        mockUssVerticle = new MockUSSVerticle();
        deploymentFutures.add(TestHelper.deployVerticleAsync(vertx, mockUssVerticle, new JsonObject().put(CONFIG_HTTP_PORT,  ussPort)));
        mockServiceRegistryClient.registerServiceInstance(ServiceConstants.COMMERCE_DELIVERY_SEARCH_FACADE, new URI("http://localhost:" + ussPort));

        final int connectorServicePort = TestHelper.getPort();
        
        // using a small page size to tes paging ...
        connectorVerticle = new ConnectorServiceVerticle(mockServiceRegistryClient, mockKafkaProducer, 2);
        deploymentFutures.add(TestHelper.deployVerticleAsync(vertx, connectorVerticle, new JsonObject().put(CONFIG_HTTP_PORT,  connectorServicePort).put("httpPort", connectorServicePort)));
        mockServiceRegistryClient.registerServiceInstance(ServiceConstants.PUBLISHING_CONNECTOR_SERVICE_NAME, new URI("http://localhost:" + connectorServicePort));

        contentServiceURL = "http://localhost:" + contentServicePort + "/authoring/v1/content/";
        siteServiceURL = "http://localhost:" + siteServicePort + "/authoring/v1/sites/";
        searchServiceURL = "http://localhost:" + searchServicePort + "/delivery/v1/search";
        connectorServiceURL = "http://localhost:" + connectorServicePort;
        assetServiceURL = "http://localhost:" + assetServicePort + "/authoring/v1/assets/";
        resourceServiceURL = "http://localhost:" + ressourceServicePort + "/authoring/v1/resources/";
        tenantRegistryServiceURL = "http://localhost:" + tenantRegistryServicePort;
        idcURL = "http://localhost:" + idcPort;
        ussURL = "http://localhost:" + ussPort;
        
        // wait for deployment
        awaitDeploymentAndStoreDeploymentIds(context, deploymentFutures);

        LOGGER.traceExit();
    }

    @After
    public void tearDown(final TestContext context) throws Exception {
        LOGGER.traceEntry();

        // undeploy verticles
        awaitUndeploymentOfVerticles(context);

        LOGGER.traceExit();
    }

    @AfterClass
    public static void tearDownClass(final TestContext context) {
        LOGGER.traceEntry();
        Async asyncCompletion = context.async();
        certx.close().whenComplete((aVoid, throwable) -> {
            if(throwable != null) {
                context.fail(throwable);
            } else {
                asyncCompletion.complete();
            }
        });
        LOGGER.traceExit();
    }

    private void awaitDeploymentAndStoreDeploymentIds(final TestContext context, final Collection<CompletionStage<String>> deploymentFutures) {
        LOGGER.traceEntry();

        Async asyncCompletion = context.async();
        CompletableUtils.allOf(deploymentFutures).whenComplete((objects, throwable) -> {
            if(throwable != null) {
                LOGGER.catching(throwable);
                context.fail(throwable);
            }
            if(objects != null) {
                Arrays.stream(objects).map(o -> (String) o).forEach(id -> deployedVerticles.add(id));
                LOGGER.trace("Done!");
                asyncCompletion.complete();
            }
        });
        LOGGER.traceExit();
    }

    private void awaitUndeploymentOfVerticles(final TestContext context) {
        List<CompletionStage<Void>> collect = deployedVerticles.stream()
                .map(certx::undeploy)
                .collect(Collectors.toList());

        // wait for completion
        Async asyncCompletion = context.async();
        CompletableUtils.allOf(collect).whenComplete((objects, throwable) -> {
            if(throwable != null) {
                context.fail(throwable);
            } else {
                asyncCompletion.complete();
            }
        });
    }
    
    public static RuntimeContext getDefaultRC() {
        return getDefaultRC(null);
    }
    
    public static RuntimeContext getCustomHostnameRC() {
        LOGGER.traceEntry();
        final JsonObject ctxData = new JsonObject();
        ctxData.put(RuntimeContext.HEADER_TENANT_ID, CUSTOM_HOSTNAME_TENANT);
        ctxData.put(RuntimeContext.HEADER_EDGE_TOKEN, CUSTOM_HOSTNAME_EDGE_TOKEN);
        ctxData.put(RuntimeContext.HEADER_TIER, "trial");
        final RuntimeContext rc = RuntimeContextFactory.getInstance().newRuntimeContext(ctxData);
        LOGGER.traceExit(rc);
        return rc;
    }

    public static RuntimeContext getDefaultRC(String revisionId) {
        LOGGER.traceEntry();
        final JsonObject ctxData = new JsonObject();
        ctxData.put(RuntimeContext.HEADER_TENANT_ID, DEFAULT_TENANT);
        ctxData.put(RuntimeContext.HEADER_EDGE_TOKEN, DEFAULT_EDGE_TOKEN);
        ctxData.put(RuntimeContext.HEADER_TIER, "trial");
        if (StringUtils.isStringNonEmpty(revisionId)) {
            ctxData.put(RuntimeContext.HEADER_REVISION_ID, revisionId);
        }
        final RuntimeContext rc = RuntimeContextFactory.getInstance().newRuntimeContext(ctxData);
        LOGGER.traceExit(rc);
        return rc;
    }
    
    protected JsonObject getPageByNameAndId(String name, String pageIdSuffix) throws URISyntaxException, IOException, SAXException, TestException {
    	return getPageByNameAndIdAndSite(name, pageIdSuffix, "default");
    }

    protected JsonObject getPageByNameAndIdAndSite(String name, String pageIdSuffix, String siteId) throws URISyntaxException, IOException, SAXException, TestException {
        LOGGER.entry(name, pageIdSuffix);
        URI uri = URLUtil.addParams(searchServiceURL, "q", "classification:page AND name:\"" + name + "\"", "fl", "*");
        JsonArray a = HttpUnitTestHelper.getUrlAsJsonObject(uri.toString(), 200).getJsonArray("documents");
        LOGGER.trace("result: " + a.encodePrettily());
        for (int i=0; i<a.size(); i++) {
            JsonObject page = a.getJsonObject(i);
            String id = page.getString("id");
            String pageSiteId = page.getString("siteId");
            pageSiteId = (pageSiteId == null) ? "default" : pageSiteId;
            if ((id.endsWith(pageIdSuffix)) && (siteId.equals(pageSiteId))) {
                return LOGGER.traceExit(page);
            }
            else {
            	LOGGER.trace("skipping page: " + page);
            }
        }
        LOGGER.traceExit();
        return null;
    }

    protected JsonObject getPageByName(String name) throws URISyntaxException, IOException, SAXException, TestException {
        URI uri = URLUtil.addParams(searchServiceURL, "q", "classification:page AND name:\"" + name + "\"", "fl", "*");
        JsonArray a = HttpUnitTestHelper.getUrlAsJsonObject(uri.toString(), 200).getJsonArray("documents");
        if (a.size() != 1) {
            throw new TestException("unexpected result: " + a);
        }
        return a.getJsonObject(0);
    }

    protected JsonObject getById(String id) throws URISyntaxException, IOException, SAXException, TestException {
        URI uri = URLUtil.addParams(searchServiceURL, "q", "id:" + id, "fl", "*");
        JsonArray a = HttpUnitTestHelper.getUrlAsJsonObject(uri.toString(), 200).getJsonArray("documents");
        if (a != null) {
            if (a.size() == 1) {
                return a.getJsonObject(0);
            }
        }
        return null;
    } 

    protected void assertValue(JsonObject page, String key, String expectedValue) {
        Object v = page.getValue(key);
        if (v instanceof JsonArray) {
            Assert.assertTrue(page.toString(), ((JsonArray)v).contains(expectedValue));
        }
        else {
            Assert.assertEquals("expected: " + expectedValue + " but was: " + v + "\n" + page.toString(), expectedValue, v);
        }
    }
    protected void assertValues(JsonObject page, String key, String[] expectedValues) {
        JsonArray a = page.getJsonArray(key);
        Assert.assertEquals(page.encodePrettily(), expectedValues.length, a.size());
        for (String s :expectedValues) {
            Assert.assertTrue("missing value: " + s + "\n" + page.encodePrettily(), a.contains(s));
        }
    }
    protected void assertSuffix(JsonObject page, String key, String value) {
        Assert.assertTrue(page.toString(), page.getString(key).endsWith(value));
    }

    protected void assertPrefix(JsonObject page, String key, String value) {
        Assert.assertTrue(page.toString(), page.getString(key).startsWith(value));
    }
    
    protected void assertSystemTag(JsonObject page, String value) {
        Assert.assertTrue(page.toString(), page.getJsonArray("systemTags").contains(value));
    }

    protected void legacyInitSearchData() throws IOException, TestException, SAXException {
        JsonArray docs = new JsonArray();
        JsonObject json = new JsonObject().put("documents", docs);
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/CategoryDefaultPage.json"));
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/CategoryProductsDefaultPage.json"));
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/ProductDefaultPage.json"));
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/BundleDefaultPage.json"));
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/KitDefaultPage.json"));
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/SkuDefaultPage.json"));
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/MayflowerSilverSyntheticFlatsPage.json"));
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/ApparelCategoryPage.json"));
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/ApparelSkuPage.json"));
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/ElectronicsCategoryPage.json"));
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/ApparelProductPage.json"));
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/AntChairProductPage.json"));
        docs.add(MockData.INSTANCE.getAsJsonObject("/searchdocs/Shop.json"));

        HttpUnitTestHelper.postJson(json, searchServiceURL, 201);
    }

    protected void initSite(String storeIdentifier, String storeId) {
        JsonObject site = new JsonObject();
        site.put(SiteConfig.PROP_STORE_ID, storeId);
        site.put(SiteConfig.PROP_STORE_IDENTIFIER, storeIdentifier);
        site.put("id", "default");
        mockSiteServiceVerticle.setDefaultSite(site);
    }

    protected void initSite(String siteId, String storeIdentifier, String storeId) {
        JsonObject site = new JsonObject();
        if (storeId != null) {
            site.put(SiteConfig.PROP_STORE_ID, storeId);
        }
        if (storeIdentifier != null) {
        	site.put(SiteConfig.PROP_STORE_IDENTIFIER, storeIdentifier);
        }
        site.put("id", siteId);
        mockSiteServiceVerticle.addSite(site);
    }

    protected void initTRS() {
        JsonObject json = new JsonObject();
        JsonObject config = new JsonObject();
        json.put(TRSConfig.PROP_IBM_COMMERCE, config);
        config.put(TRSConfig.PROP_LIVE_SEARCH_HOST, idcURL);
        config.put(TRSConfig.PROP_PREVIEW_SEARCH_HOST, idcURL);
        config.put(TRSConfig.PROP_LIVE_TRANSACTION_HOST, idcURL);
        config.put(TRSConfig.PROP_PREVIEW_TRANSACTION_HOST, idcURL);
        mockTenantRegistryServiceVerticle.setCurrentTenantJson(json);
    }

    protected void initPages(boolean withPageOverride) throws IOException {
    	initPages(withPageOverride, null);
    }

    protected void initPages(boolean withPageOverride, String siteId) throws IOException {
        JsonObject catalogPage = loadCatalogPage(siteId, "/pages/catalog-page.json");
        JsonObject itemOverridePage = loadPage(siteId,"/pages/item-override-page.json");
        JsonObject categoryOverridePage = loadPage(siteId,"/pages/category-item-override-page.json");
        JsonObject categoryProductsOverridePage = loadPage(siteId,"/pages/category-products-item-override-page.json");
        
        JsonObject catPage = loadPage(siteId,"/pages/DefaultCategoryPage.json");
        JsonObject prodPage = loadPage(siteId,"/pages/DefaultProductPage.json");
        JsonObject skuPage = loadPage(siteId,"/pages/DefaultSkuPage.json");
        JsonObject bundlePage = loadPage(siteId,"/pages/DefaultBundlePage.json");
        JsonObject kitPage = loadPage(siteId,"/pages/DefaultKitPage.json");
        JsonObject catProdPage = loadPage(siteId,"/pages/DefaultCategoryProductsPage.json");
        
        if (siteId == null) {
            mockSiteServiceVerticle.addPagesByPageKind("catalog-page", new JsonArray().add(catalogPage));
        }
        else {
            mockSiteServiceVerticle.addPagesByPageKind(siteId, "catalog-page", new JsonArray().add(catalogPage));
        }
        if (withPageOverride) {
        	if (siteId == null) {
                mockSiteServiceVerticle.addPagesByPageKind("dynamic-item-override-page", new JsonArray().add(itemOverridePage));
        	}
        	else {
        		mockSiteServiceVerticle.addPagesByPageKind(siteId, "dynamic-item-override-page", new JsonArray().add(itemOverridePage));
        	}
            mockSiteServiceVerticle.addPage(itemOverridePage); 
            mockSiteServiceVerticle.addPage(categoryOverridePage); 
            mockSiteServiceVerticle.addPage(categoryProductsOverridePage); 
        }
        else {
        	if (siteId == null) {
                mockSiteServiceVerticle.addPagesByPageKind("dynamic-item-override-page", new JsonArray());
        	}
        	else {
                mockSiteServiceVerticle.addPagesByPageKind(siteId, "dynamic-item-override-page", new JsonArray());
        	}
        }
        if (siteId == null) {
            mockSiteServiceVerticle.addPagesByPageKind("catalog-default-page", new JsonArray().add(catPage).add(prodPage).add(skuPage).add(bundlePage).add(kitPage).add(catProdPage));
        }
        else {
            mockSiteServiceVerticle.addPagesByPageKind(siteId, "catalog-default-page", new JsonArray().add(catPage).add(prodPage).add(skuPage).add(bundlePage).add(kitPage).add(catProdPage));
        }
    }

    protected void initPagesNoProductPage() throws IOException {
        JsonObject catalogPage = new JsonObject(MockData.INSTANCE.getAsString("/pages/catalog-page.json"));
        
        JsonObject catPage = new JsonObject(MockData.INSTANCE.getAsString("/pages/DefaultCategoryPage.json"));
        JsonObject skuPage = new JsonObject(MockData.INSTANCE.getAsString("/pages/DefaultSkuPage.json"));
        JsonObject bundlePage = new JsonObject(MockData.INSTANCE.getAsString("/pages/DefaultBundlePage.json"));
        JsonObject kitPage = new JsonObject(MockData.INSTANCE.getAsString("/pages/DefaultKitPage.json"));
        JsonObject catProdPage = new JsonObject(MockData.INSTANCE.getAsString("/pages/DefaultCategoryProductsPage.json"));
        
        mockSiteServiceVerticle.addPagesByPageKind("catalog-page", new JsonArray().add(catalogPage));
        mockSiteServiceVerticle.addPagesByPageKind("dynamic-item-override-page", new JsonArray());
        mockSiteServiceVerticle.addPagesByPageKind("catalog-default-page", new JsonArray().add(catPage).add(skuPage).add(bundlePage).add(kitPage).add(catProdPage));
    }

    protected void initPagesIncomplete() throws IOException {
        JsonObject catalogPage = new JsonObject(MockData.INSTANCE.getAsString("/pages/catalog-page.json"));
        JsonObject itemOverridePage = new JsonObject(MockData.INSTANCE.getAsString("/pages/item-override-page.json"));
        
        JsonObject catPage = new JsonObject(MockData.INSTANCE.getAsString("/pages/DefaultCategoryPage.json"));
        JsonObject prodPage = new JsonObject(MockData.INSTANCE.getAsString("/pages/DefaultProductPage.json"));
        JsonObject skuPage = new JsonObject(MockData.INSTANCE.getAsString("/pages/DefaultSkuPage.json"));
        JsonObject bundlePage = new JsonObject(MockData.INSTANCE.getAsString("/pages/DefaultBundlePage.json"));
        JsonObject catProdPage = new JsonObject(MockData.INSTANCE.getAsString("/pages/DefaultCategoryProductsPage.json"));
        
        mockSiteServiceVerticle.addPagesByPageKind("catalog-page", new JsonArray().add(catalogPage));
        mockSiteServiceVerticle.addPagesByPageKind("dynamic-item-override-page", new JsonArray().add(itemOverridePage));
        mockSiteServiceVerticle.addPagesByPageKind("catalog-default-page", new JsonArray().add(catPage).add(prodPage).add(skuPage).add(bundlePage).add(catProdPage));
    }

    protected void initSearchData() throws IOException, TestException, SAXException {
    	initSearchData(null);
    }

    protected void initSearchData(String siteId) throws IOException, TestException, SAXException {
        JsonArray docs = new JsonArray();
        JsonObject json = new JsonObject().put("documents", docs);
        docs.add(loadPage(siteId, "/searchdocs/TheCategoryDefaultPage.json"));
        docs.add(loadPage(siteId, "/searchdocs/TheCategoryProductsDefaultPage.json"));
        docs.add(loadPage(siteId, "/searchdocs/TheProductDefaultPage.json"));
        docs.add(loadPage(siteId, "/searchdocs/TheBundleDefaultPage.json"));
        docs.add(loadPage(siteId, "/searchdocs/TheKitDefaultPage.json"));
        docs.add(loadPage(siteId, "/searchdocs/TheSkuDefaultPage.json"));
        docs.add(loadPage(siteId, "/searchdocs/item-override-page.json"));
                
        HttpUnitTestHelper.postJson(json, searchServiceURL, 201);
    }
    
    private JsonObject loadPage(String siteId, String path) throws IOException {
    	JsonObject json = MockData.INSTANCE.getAsJsonObject(path);
    	if (siteId != null) {
    		json = rewritePageForSite(siteId, json);
    	}
    	return json;
    }
    
    private JsonObject loadCatalogPage(String siteId, String path) throws IOException {
    	JsonObject json = MockData.INSTANCE.getAsJsonObject(path);
    	if (siteId != null) {
    		json = rewriteCatalogPageForSite(siteId, json);
    	}
    	return json;
    }

    private JsonObject rewritePageForSite(String siteId, JsonObject page) {
    	JsonObject result = page.copy();
    	result.put("siteId", siteId);
    	addSuffix(result, "id", siteId);
    	if (result.containsKey("parentId")) {
        	addSuffix(result, "parentId", siteId);
    	}
    	return result;
    }

    private JsonObject rewriteCatalogPageForSite(String siteId, JsonObject page) {
    	LOGGER.entry(siteId, page);
    	JsonObject result = rewritePageForSite(siteId, page);
    	JsonObject ids = result.getJsonObject("defaultCatalogPageIds");
    	addSuffix(ids, "category-page", siteId);
    	addSuffix(ids, "product-page", siteId);
    	addSuffix(ids, "bundle-page", siteId);
    	addSuffix(ids, "sku-page", siteId);
    	addSuffix(ids, "kit-page", siteId);
    	addSuffix(ids, "category-products-page", siteId);
    	return LOGGER.exit(result);
    }
    
    private void addSuffix(JsonObject json, String key, String suffix) {
    	json.put(key, json.getString(key) + "-" + suffix);
    }
    
    public static class ConnectorKafkaConsumer implements MockKafkaConsumer {

    	private HashMap<String, Object> msgs = new HashMap<>();
    	
		@Override
		public void messageReceived(String kafkaTopic, Object key, Object value) {
			msgs.put(kafkaTopic+key, value);
		}
		
		public HashMap<String, Object> getMessages() {
			return msgs;
		}
    	
    }

}