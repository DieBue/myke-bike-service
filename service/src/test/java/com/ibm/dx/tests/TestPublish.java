/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.tests;

import java.net.URI;
import java.util.Set;

import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.dx.publishing.connectorservice.ConnectorConstants;
import com.ibm.utilities.collections.sets.Sets;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.dx.publishing.common.utils.URLUtil;
import com.ibm.dx.test.BaseTest;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import test.publishing.common.HttpUnitTestHelper;
import test.publishing.mocks.verticles.helpers.MockData;

import static com.ibm.dx.publishing.connectorservice.ConnectorConstants.ALLOWED_CLASSIFICATIONS;

@RunWith(VertxUnitRunner.class)
public class TestPublish extends BaseTest {
    private static RuntimeContextLogger LOGGER = RuntimeContextLogger.create(TestPublish.class);

    private static final String ROUTE_PUBLISH_V2 = "/publishing/v2/connector/idc/publish";
    private static final String SEARCH_DOC = "/streams/searchDoc.json";
    private static final String SEARCH_DOC_LARGE = "/streams/largeSearchDoc.json";
    private static final String SEARCH_DOC_VERY_LARGE = "/streams/veryLargeSearchDoc.json";
    private static final String SEARH_DOC_INVALID_CLASSIFICATION = "/streams/searchDocInvalidClassification.json";

    @Before
    public void setup() throws Exception {
        initSearchData();
        initPages(true);
        initTRS();
        initSite("Stockholm", "11");
    }

    @Test(timeout = 100000)
    public void testPublishStream() throws Exception {
        testPublish(connectorServiceURL + ROUTE_PUBLISH_V2, 202, SEARCH_DOC, null);
    }

    @Test(timeout = 100000)
    public void testPublishLargeStream() throws Exception {
        testPublish(connectorServiceURL + ROUTE_PUBLISH_V2, 202, SEARCH_DOC_LARGE, null);
    }

    @Test(timeout = 100000)
    public void testPublishVeryLargeStream() throws Exception {
        testPublish(connectorServiceURL + ROUTE_PUBLISH_V2, 202, SEARCH_DOC_VERY_LARGE, null);
    }

    @Test(timeout = 100000)
    public void testPublishVeryLargeStreamWithForceClassification() throws Exception {
        URI uri = URLUtil.addParams(connectorServiceURL + ROUTE_PUBLISH_V2, "forceClassifications", "testforceclass-1");
        testPublish(uri.toString(), 202, SEARCH_DOC_VERY_LARGE, Sets.unmodifiableStringSet("testforceclass-1"));
    }

    @Test(timeout = 100000)
    public void testPublishVeryLargeStreamWithForceClassifications() throws Exception {
        URI uri = URLUtil.addParams(connectorServiceURL + ROUTE_PUBLISH_V2, "forceClassifications", "testforceclass-1,testforceclass-2");
        testPublish(uri.toString(), 202, SEARCH_DOC_VERY_LARGE, Sets.unmodifiableStringSet("testforceclass-1", "testforceclass-2"));
    }

    @Test(timeout = 100000)
    public void testPublishSearchRespondsWith500() throws Exception {
        mockSearchServiceVerticle.setMethodResponseCode(HttpMethod.PUT, 500);
        mockSearchServiceVerticle.setMethodResponseCode(HttpMethod.POST, 500);
        testPublish(connectorServiceURL + ROUTE_PUBLISH_V2, 500, SEARCH_DOC, null);
    }

    @Test(timeout = 100000)
    public void testPublishSearchRequestTimesOut() throws Exception {
        mockSearchServiceVerticle.setDoEndResponse(false);
        testPublish(connectorServiceURL + ROUTE_PUBLISH_V2, 500, SEARCH_DOC, null);
    }

    @Test(timeout = 100000)
    public void testPublishDocWithInvalidClassification() throws Exception {
        testPublish(connectorServiceURL + ROUTE_PUBLISH_V2, 500, SEARH_DOC_INVALID_CLASSIFICATION, null);
    }

    private void testPublish(final String baseUrl, final int expectedCode, final String sampleData, final Set<String> forcedClassifications) throws Exception {
        String json = MockData.INSTANCE.getAsString(sampleData);
        URI uri = URLUtil.addParams(baseUrl);
        final String response = HttpUnitTestHelper.postJson(new JsonObject(json), uri.toString(), expectedCode);
        LOGGER.rcInfo(rc, "Received response: {}", response);
        if (expectedCode < 300) {
            final JsonArray docs = new JsonObject(json).getJsonArray("documents");
            for (int i = 0; i < docs.size(); i++) {
                // single document from input stream
                final JsonObject sourceDocument = docs.getJsonObject(i);

                // single published document
                final JsonObject publishedDocument = getById(sourceDocument.getString("id"));

                // key properties from published document
                final String classification = sourceDocument.getString("classification");
                final String name = sourceDocument.getString("name");
                final String id = sourceDocument.getString("id");

                // check if it was correctly published or filtered out
                if (ALLOWED_CLASSIFICATIONS.contains(classification) || (forcedClassifications != null && forcedClassifications.contains(classification))) {
                    assertItem(publishedDocument, classification, name, classification + ":" + id);
                } else {
                    Assert.assertNull(getById(id));
                }
            }
        }
    }

    private void assertItem(JsonObject doc, String classification, String name, String docId) {
        Assert.assertNotNull("Did not find expected document in search index: " + docId, doc);
        Assert.assertEquals(classification, doc.getString("classification"));
        Assert.assertEquals(name, doc.getString("name"));
        Assert.assertEquals(docId, doc.getString("__docId__"));
        Assert.assertNotNull(doc.getString("systemModified"));
    }
}
