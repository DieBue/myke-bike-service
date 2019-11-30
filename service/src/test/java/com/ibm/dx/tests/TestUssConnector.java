/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.xml.sax.SAXException;

import com.ibm.dx.publishing.common.utils.URLUtil;
import com.ibm.dx.publishing.connectorservice.idc.PageKinds;
import com.ibm.dx.publishing.connectorservice.idc.exceptions.ConnectorException;
import com.ibm.dx.test.BaseTest;
import com.ibm.dx.test.mocks.MockUSSVerticle.Mode;
import com.ibm.wps.util.StringUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import test.publishing.common.HttpUnitTestHelper;
import test.publishing.common.TestException;

@RunWith(VertxUnitRunner.class)
public class TestUssConnector extends BaseTest {

    public static final Logger LOGGER = LogManager.getLogger(TestUssConnector.class.getName());
    public static final String CONNECTOR_ITEM_ID = "IdcDefaultConnector";
    public static final String CATEGORY_DEFAULT_PAGE_CONTENT_ITEM_ID = "6188e436-5df0-4798-805e-ff2bf7bc9248";
    public static final String CATEGORY_PRODUCTS_DEFAULT_PAGE_CONTENT_ITEM_ID = "CategoryProductsDefaultPage-ContentItemId";
    public static final String PRODUCTS_DEFAULT_PAGE_CONTENT_ITEM_ID = "6188e436-5df0-4798-805e-ff2bf7bc9247";

    @Test(timeout = 100000)
    public void testCreateDeleteProductOverridePage() throws Exception {
        mockUssVerticle.setMode(Mode.replay);
        initSearchData();
        initPages(false);
        initTRS();
        initSite("Stockholm", "111");

        JsonObject result = triggerIndex(200);
        Assert.assertTrue(result != null);

        assertPageKind("Bender Toothbrush Holder", "BR-ACCE-0002", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.PRODUCT_PAGE));

        initPages(true);

        result = triggerTemplateChange("item-override-page", 200);
        Assert.assertTrue(result != null);

        assertPage("product", "item-override-page name", "/Accessories/seg", "Accessories", "some-product-page-content", "BR-ACCE-0002", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");
        assertPageKind("item-override-page name", "BR-ACCE-0002", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.ITEM_OVERRIDE_PAGE).add(PageKinds.PRODUCT_PAGE));

        assertPage("category", "Bath", "/Bath", "catalog-page", CATEGORY_DEFAULT_PAGE_CONTENT_ITEM_ID, "Bath", CONNECTOR_ITEM_ID, "/Bath", new String[] { "Bath" }, "11");
        assertPage("product", "Makeup Mirror", "/Accessories/Makeup Mirror", "Accessories", PRODUCTS_DEFAULT_PAGE_CONTENT_ITEM_ID, "BR-ACCE-0001", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");

        triggerTemplateDelete("item-override-page", 204);

        assertPage("product", "Bender Toothbrush Holder", "/Accessories/Bender Toothbrush Holder", "Accessories", PRODUCTS_DEFAULT_PAGE_CONTENT_ITEM_ID, "BR-ACCE-0002", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");
        assertPageKind("Bender Toothbrush Holder", "BR-ACCE-0002", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.PRODUCT_PAGE));

        assertPage("category", "Bath", "/Bath", "catalog-page", CATEGORY_DEFAULT_PAGE_CONTENT_ITEM_ID, "Bath", CONNECTOR_ITEM_ID, "/Bath", new String[] { "Bath" }, "11");
        assertPage("product", "Makeup Mirror", "/Accessories/Makeup Mirror", "Accessories", PRODUCTS_DEFAULT_PAGE_CONTENT_ITEM_ID, "BR-ACCE-0001", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");
    }

    @Test(timeout = 100000)
    public void testCreateDeleteCategoryOverridePage() throws Exception {
        mockUssVerticle.setMode(Mode.replay);
        initSearchData();
        initPages(false);

        initTRS();
        initSite("Stockholm", "111");

        JsonObject result = triggerIndex(200);
        Assert.assertTrue(result != null);

        assertPageKind("Bath", "Bath", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.CATEGORY_PAGE));

        initPages(true);

        result = triggerTemplateChange("category-item-override-page", 200);
        Assert.assertTrue(result != null);

        assertPage("category", "category-item-override-page name", "/seg2", "catalog-page", "some-category-override-page-content", "Bath", CONNECTOR_ITEM_ID, "Bath", new String[] { "Bath" }, "11");
        assertPageKind("category-item-override-page name", "Bath", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.ITEM_OVERRIDE_PAGE).add(PageKinds.CATEGORY_PAGE));

        triggerTemplateDelete("category-item-override-page", 204);
        assertPage("category", "Bath", "/Bath", "catalog-page", CATEGORY_DEFAULT_PAGE_CONTENT_ITEM_ID, "Bath", CONNECTOR_ITEM_ID, "/Bath", new String[] { "Bath" }, "11");
        assertPageKind("Bath", "Bath", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.CATEGORY_PAGE));
    }

    @Test(timeout = 100000)
    public void testCreateDeleteCategoryProductsOverridePage() throws Exception {
        mockUssVerticle.setMode(Mode.replay);
        initSearchData();
        initPages(false);

        initTRS();
        initSite("Stockholm", "111");

        JsonObject result = triggerIndex(200);
        Assert.assertTrue(result != null);

        assertPageKind("Buffets", "Buffets", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.CATEGORY_PRODUCTS_PAGE));

        initPages(true);

        result = triggerTemplateChange("category-products-item-override-page", 200);
        Assert.assertTrue(result != null);

        assertPageKind("category-products-item-override-page name", "Buffets", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.ITEM_OVERRIDE_PAGE).add(PageKinds.CATEGORY_PRODUCTS_PAGE));

        triggerTemplateDelete("category-products-item-override-page", 204);
        assertPageKind("Buffets", "Buffets", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.CATEGORY_PRODUCTS_PAGE));
    }

    @Test(timeout = 100000)
    public void testCatalogPageTrigger() throws Exception {
        mockUssVerticle.setMode(Mode.replay);
        initSearchData();
        initPages(true);
        initTRS();
        initSite("Stockholm", "111");

        JsonObject result = triggerIndex(200);
        Assert.assertTrue(result != null);
        Assert.assertEquals(1, mockKafkaConsumer.getMessages().keySet().size());
        String key = mockKafkaConsumer.getMessages().keySet().iterator().next();
        Assert.assertTrue(key, key.contains("page"));
        Object val = mockKafkaConsumer.getMessages().get(key);
        Assert.assertNotNull(val);
        Assert.assertTrue(val.toString(), val.toString().contains("catalog-page"));
        
        String deleteQuery = mockSearchServiceVerticle.getDeleteQueryParamsPassed().iterator().next();
        Assert.assertTrue("actual: " + deleteQuery, deleteQuery.contains("systemTags:\"dyn-connector:IdcDefaultConnector\""));
        Assert.assertTrue("actual: " + deleteQuery, deleteQuery.contains("lastModified:["));

        assertPage("category", "Accessories", "/Bath/Accessories", "Bath", CATEGORY_PRODUCTS_DEFAULT_PAGE_CONTENT_ITEM_ID, "Accessories", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");
        assertPage("category", "Bath", "/Bath", "catalog-page", CATEGORY_DEFAULT_PAGE_CONTENT_ITEM_ID, "Bath", CONNECTOR_ITEM_ID, "/Bath", new String[] { "Bath" }, "11");
        assertPage("product", "Makeup Mirror", "/Accessories/Makeup Mirror", "Accessories", PRODUCTS_DEFAULT_PAGE_CONTENT_ITEM_ID, "BR-ACCE-0001", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");
        assertPage("product", "item-override-page name", "/Accessories/seg", "Accessories", "some-product-page-content", "BR-ACCE-0002", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");

        JsonObject doc = getPageByNameAndId("Makeup Mirror", "BR-ACCE-0001");
        Assert.assertEquals(doc.encodePrettily(), doc.getString("route"), "/Accessories/Makeup%20Mirror");

        assertPageKind("Accessories", "Accessories", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.CATEGORY_PRODUCTS_PAGE));
        assertPageKind("Bath", "Bath", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.CATEGORY_PAGE));
        assertPageKind("Makeup Mirror", "BR-ACCE-0001", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.PRODUCT_PAGE));
        assertPageKind("item-override-page name", "BR-ACCE-0002", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.ITEM_OVERRIDE_PAGE).add(PageKinds.PRODUCT_PAGE));
    }

    @Test(timeout = 100000)
    public void testMultiSiteTrigger() throws Exception {
        mockUssVerticle.setMode(Mode.replay);
        initSearchData();
        initPages(true);
        initTRS();
        initSite("Stockholm", "111");
        
        initSite("mysite", "Stockholm", "111");
        initPages(true, "mysite");
        initSearchData("mysite");
        initSite("mysite2", null, null);
        initPages(true, "mysite2");
        initSearchData("mysite2");
        initSite("mysite3", "Stockholm", "111");
        initPages(true, "mysite3");
        initSearchData("mysite3");
        initSite("mysite4", null, null);
        initPages(true, "mysite4");
        initSearchData("mysite4");

        triggerIndex(204);
        Assert.assertEquals(1, mockKafkaConsumer.getMessages().keySet().size());
        String key = mockKafkaConsumer.getMessages().keySet().iterator().next();
        Assert.assertTrue(key, key.contains("page"));
        Object val = mockKafkaConsumer.getMessages().get(key);
        Assert.assertNotNull(val);
        Assert.assertTrue(val.toString(), val.toString().contains("catalog-page"));
        
        String deleteQuery = mockSearchServiceVerticle.getDeleteQueryParamsPassed().iterator().next();
        Assert.assertTrue("actual: " + deleteQuery, deleteQuery.contains("systemTags:\"dyn-connector:IdcDefaultConnector\""));
        Assert.assertTrue("actual: " + deleteQuery, deleteQuery.contains("lastModified:["));

        // asserts for default site
        assertPage("category", "Accessories", "/Bath/Accessories", "Bath", CATEGORY_PRODUCTS_DEFAULT_PAGE_CONTENT_ITEM_ID, "Accessories", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");
        assertPage("category", "Bath", "/Bath", "catalog-page", CATEGORY_DEFAULT_PAGE_CONTENT_ITEM_ID, "Bath", CONNECTOR_ITEM_ID, "/Bath", new String[] { "Bath" }, "11");
        assertPage("product", "Makeup Mirror", "/Accessories/Makeup Mirror", "Accessories", PRODUCTS_DEFAULT_PAGE_CONTENT_ITEM_ID, "BR-ACCE-0001", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");
        assertPage("product", "item-override-page name", "/Accessories/seg", "Accessories", "some-product-page-content", "BR-ACCE-0002", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");

        JsonObject doc = getPageByNameAndId("Makeup Mirror", "BR-ACCE-0001");
        Assert.assertEquals(doc.encodePrettily(), doc.getString("route"), "/Accessories/Makeup%20Mirror");

        assertPageKind("Accessories", "Accessories", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.CATEGORY_PRODUCTS_PAGE));
        assertPageKind("Bath", "Bath", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.CATEGORY_PAGE));
        assertPageKind("Makeup Mirror", "BR-ACCE-0001", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.PRODUCT_PAGE));
        assertPageKind("item-override-page name", "BR-ACCE-0002", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.ITEM_OVERRIDE_PAGE).add(PageKinds.PRODUCT_PAGE));

        // asserts for my mysite3
        assertPage("mysite", "category", "Accessories", "/Bath/Accessories", "Bath", CATEGORY_PRODUCTS_DEFAULT_PAGE_CONTENT_ITEM_ID, "Accessories", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");
        assertPage("mysite", "category", "Bath", "/Bath", "catalog-page", CATEGORY_DEFAULT_PAGE_CONTENT_ITEM_ID, "Bath", CONNECTOR_ITEM_ID, "/Bath", new String[] { "Bath" }, "11");
        assertPage("mysite", "product", "Makeup Mirror", "/Accessories/Makeup Mirror", "Accessories", PRODUCTS_DEFAULT_PAGE_CONTENT_ITEM_ID, "BR-ACCE-0001", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");
        assertPage("mysite", "product", "item-override-page name", "/Accessories/seg", "Accessories", "some-product-page-content", "BR-ACCE-0002", CONNECTOR_ITEM_ID, "/Bath/Accessories", new String[] { "Bath", "Accessories" }, "11");

        doc = getPageByNameAndIdAndSite("Makeup Mirror", "BR-ACCE-0001-mysite3", "mysite3");
        Assert.assertEquals(doc.encodePrettily(), doc.getString("route"), "/Accessories/Makeup%20Mirror");

        assertPageKind("Accessories", "Accessories", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.CATEGORY_PRODUCTS_PAGE), "mysite3");
        assertPageKind("Bath", "Bath", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.CATEGORY_PAGE), "mysite3");
        assertPageKind("Makeup Mirror", "BR-ACCE-0001", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.DEFAULT_OVERRIDE_PAGE).add(PageKinds.PRODUCT_PAGE), "mysite3");
        assertPageKind("item-override-page name", "BR-ACCE-0002", new JsonArray().add(PageKinds.DYNAMIC_PAGE).add(PageKinds.ITEM_OVERRIDE_PAGE).add(PageKinds.PRODUCT_PAGE), "mysite3");

    }

    @Test(timeout = 100000)
    public void testNoSiteConfig() throws Exception {
        mockUssVerticle.setMode(Mode.replay);
        initSearchData();
        initPages(true);
        initTRS();
        initSite(null, null);

        triggerIndex(204);
    }

    
    @Test(timeout = 100000)
    public void testMultiSiteTriggerSuccessCode() throws Exception {
        mockUssVerticle.setMode(Mode.replay);
        initSearchData();
        initPages(true);
        initTRS();
        initSite("Stockholm", "111");
        
        initSite("mysite", "Stockholm", "111");
        initPages(true, "mysite");
        initSearchData("mysite");

        triggerIndex(200);
    }

    @Test(timeout = 100000)
    public void testSiteId() throws Exception {
        initSearchData();
        initPages(true);
        initTRS();
        initSite("Stockholm", "2");

        JsonObject result = triggerIndex(200);
        Assert.assertTrue(result != null);
        assertPage("category", "Bath", "/Bath", "catalog-page", CATEGORY_DEFAULT_PAGE_CONTENT_ITEM_ID, "Bath", CONNECTOR_ITEM_ID, "/Bath", new String[] { "Bath" }, "2");
    }

    @Test(timeout = 100000)
    public void testDelete() throws Exception {
        triggerDelete();
        Set<String> deleteQueries = mockSearchServiceVerticle.getDeleteQueryParamsPassed();
        Assert.assertTrue(deleteQueries.toString(), deleteQueries.contains("systemTags:\"dyn-connector:IdcDefaultConnector\""));
        Assert.assertEquals(1, deleteQueries.size());
    }

    @Test(timeout = 100000)
    public void testMissingTemplate() throws Exception {
        initSearchData();
        initPagesIncomplete();
        initTRS();
        initSite("Stockholm", "11");

        JsonObject result = triggerIndex(404);
        assertErrorMessageContains(result, "template page was not found");

        LOGGER.traceEntry();
    }

    @Test(timeout = 100000)
    public void testMissingCategories() throws Exception {
        initSearchData();
        initPages(true);
        initTRS();
        initSite("foo", "11");

        JsonObject result = triggerIndex(404);
        assertErrorMessageContains(result, "No categories found");
        assertErrorCode(result, ConnectorException.ERROR_CODE_NO_CATEGORIES_FOUND);

        LOGGER.traceEntry();
    }

    @Test(timeout = 100000)
    public void testSkipMissingCategories() throws Exception {
        initSearchData();
        initPages(true);
        initTRS();
        initSite("foo", "11");

        triggerIndex(204, true);

        LOGGER.traceEntry();
    }

    @Test(timeout = 100000)
    public void testMissingCataloguePage() throws Exception {
        initSearchData();
        initTRS();
        initSite("Stockholm", "foo");

        JsonObject result = triggerIndex(404);
        assertErrorMessageContains(result, "No catalog page found");
        assertErrorCode(result, ConnectorException.ERROR_CODE_NO_CATALOG_NODE);

        LOGGER.traceEntry();
    }

    @Test(timeout = 100000)
    public void testSkipMissingCataloguePage() throws Exception {
        initSearchData();
        initTRS();
        initSite("Stockholm", "foo");

        triggerIndex(204, true);
        LOGGER.traceEntry();
    }

    @Test(timeout = 100000)
    public void testMissingProductPage() throws Exception {
        initSearchData();
        initPagesNoProductPage();
        initTRS();
        initSite("Stockholm", "foo");

        JsonObject result = triggerIndex(404);
        assertErrorMessageContains(result, "template page was not found");
        assertErrorCode(result, ConnectorException.ERROR_CODE_TEMPLATE_PAGE_NOT_FOUND);

        LOGGER.traceEntry();
    }

    @Test(timeout = 100000)
    public void testSkipMissingProductPage() throws Exception {
        initSearchData();
        initPagesNoProductPage();
        initTRS();
        initSite("Stockholm", "foo");

        triggerIndex(204, true);
        LOGGER.traceEntry();
    }

    private void assertErrorMessageContains(JsonObject json, String string) {
        String message = json.getJsonArray("errors").getJsonObject(0).getString("message");
        assertTrue(message, message.contains(string));
    }

    private void assertErrorCode(JsonObject json, int errorCode) {
        Integer code = json.getInteger("errorCode");
        Assert.assertEquals(json.encodePrettily(), errorCode, code.intValue());
    }

    public JsonObject triggerIndex(int responseCode) throws IOException, TestException, SAXException, URISyntaxException {
        URI uri = URLUtil.addParams(connectorServiceURL + "/publishing/v1/connector/idc/trigger");
        String str = HttpUnitTestHelper.postJson(rc, new JsonObject(), uri.toString(), responseCode);
        System.out.println(str);
        JsonObject result = StringUtils.isStringNonEmpty(str) ? new JsonObject(str) : null;
        return result;
    }

    public void triggerIndex(int responseCode, boolean skipOnMissingConfig) throws IOException, TestException, SAXException, URISyntaxException {
        URI uri = URLUtil.addParams(connectorServiceURL + "/publishing/v1/connector/idc/trigger?skipOnMissingConfig=" + skipOnMissingConfig);
        HttpUnitTestHelper.postJson(rc, new JsonObject(), uri.toString(), responseCode);
    }

    public JsonObject triggerTemplateChange(String templatePageId, int responseCode) throws IOException, TestException, SAXException, URISyntaxException {
        URI uri = URLUtil.addParams(connectorServiceURL + "/publishing/v1/connector/idc/templates/" + templatePageId);
        String result = HttpUnitTestHelper.postJson(rc, new JsonObject(), uri.toString(), responseCode);
        System.out.println(result);
        return new JsonObject(result);
    }

    public void triggerTemplateDelete(String templatePageId, int responseCode) throws IOException, TestException, SAXException, URISyntaxException {
        URI uri = URLUtil.addParams(connectorServiceURL + "/publishing/v1/connector/idc/templates/" + templatePageId);
        HttpUnitTestHelper.delete(rc, uri.toString(), responseCode);
    }

    private void triggerDelete() throws IOException, TestException, SAXException, URISyntaxException {
        URI uri = URLUtil.addParams(connectorServiceURL + "/publishing/v1/connector/idc/trigger/");
        HttpUnitTestHelper.delete(rc, uri.toString(), 204);
    }

    public void assertPage(String siteId, String type, String name, String path, String parentIdSuffix, String pageContentItemId, String id, String connectorItemId, String categoryNamePath, String[] categories, String storeId) throws URISyntaxException, IOException, SAXException, TestException {
    	JsonObject sr = (siteId == null) ? getPageByNameAndId(name, id): getPageByNameAndIdAndSite(name, id + "-" +siteId, siteId);
        LOGGER.trace(sr.encodePrettily());
        Assert.assertNotNull(sr);
        assertValue(sr, "classification", "page");
        assertValue(sr, "path", path);
        assertValues(sr, "paths", new String[] { path, "/" + type + "/" + id });
        assertValue(sr, "categoryLeaves", categoryNamePath);
        assertValues(sr, "categories", categories);
        assertValue(sr, "contentId", pageContentItemId);
        assertValue(sr, "contentId", pageContentItemId);
        if (siteId == null) {
            assertSuffix(sr, "id", "-" + id);
            assertSuffix(sr, "parentId", parentIdSuffix);
        }
        else {
            assertSuffix(sr, "id", "-" + id + "-" + siteId);
            assertSuffix(sr, "parentId", parentIdSuffix + "-" + siteId);
        }
        assertSystemTag(sr, "dyn-type:" + type);
        assertSystemTag(sr, "dyn-connector:" + connectorItemId);
        assertSystemTag(sr, "dyn-id:" + id);

        String docString = sr.getString("document");
        Assert.assertNotNull(docString);
        JsonObject doc = new JsonObject(docString);
        LOGGER.trace("document: " + doc);
        JsonObject extContext = doc.getJsonObject("externalContext");
        LOGGER.trace("externalContext: " + extContext);

        assertValue(extContext, "id", id);
        assertValue(extContext, "type", type);
        assertPrefix(extContext, "baseUrl", "http");
        assertSuffix(extContext, "baseUrl", "/");
        assertPrefix(extContext, "previewUrl", "http");
        assertSuffix(extContext, "previewUrl", "/");
        assertSuffix(extContext, "storeId", storeId);

        if (type.equals("category")) {
            Assert.assertFalse(sr.encodePrettily(), sr.getBoolean("hideFromNavigation"));
        } else {
            Assert.assertTrue(sr.encodePrettily(), sr.getBoolean("hideFromNavigation"));
        }

    }

    public void assertPage(String type, String name, String path, String parentIdSuffix, String pageContentItemId, String id, String connectorItemId, String categoryNamePath, String[] categories, String storeId) throws URISyntaxException, IOException, SAXException, TestException {
    	assertPage(null, type, name, path, parentIdSuffix, pageContentItemId, id, connectorItemId, categoryNamePath, categories, storeId);
    }

    public void assertPageKind(String name, String id, JsonArray kind) throws URISyntaxException, IOException, SAXException, TestException {
    	assertPageKind(name, id, kind, null);
    }

    public void assertPageKind(String name, String id, JsonArray kind, String siteId) throws URISyntaxException, IOException, SAXException, TestException {
        JsonObject sr = (siteId == null) ?  getPageByNameAndId(name, id) : getPageByNameAndIdAndSite(name, id + "-" + siteId, siteId);
        LOGGER.trace(sr.encodePrettily());
        Assert.assertNotNull(sr);
        JsonArray ar = sr.getJsonArray("kind");
        Assert.assertEquals("expected : " + kind + " actual: " + ar, kind.size(), ar.size());

        for (int i = 0; i < kind.size(); i++) {
            Assert.assertTrue("expected : " + kind + " actual: " + ar, ar.contains(kind.getString(i)));
        }

    }

    public void assertPageTemplate(String name, String pageTemplate) throws URISyntaxException, IOException, SAXException, TestException {
        JsonObject sr = getPageByName(name);
        LOGGER.trace(sr.encodePrettily());
        Assert.assertNotNull(sr);
        JsonArray tags = sr.getJsonArray("systemTags");
        Assert.assertTrue("tags : " + tags, tags.contains("dyn-page-name:" + pageTemplate));
    }
}
