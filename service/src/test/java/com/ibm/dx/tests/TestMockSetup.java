/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.dx.test.BaseTest;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import test.publishing.common.HttpUnitTestHelper;

@RunWith(VertxUnitRunner.class)
public class TestMockSetup extends BaseTest {

    public static final String ID_PAGE1 = "page1";
    public static final String ID_ITEM1 = "item1-b6b-3b3e-4d1a-b0a5-5e994d19d7cd";

	private static Logger LOGGER = LogManager.getLogger(TestMockSetup.class.getName());
	
    @Test(timeout = 10000)
    public void testIt() throws Exception {
        LOGGER.traceEntry();

        JsonObject item = HttpUnitTestHelper.getUrlAsJsonObject(rc, contentServiceURL + ID_ITEM1, 200);
        assertNotNull(item);
        assertEquals("unexpected JSON: " + item.encodePrettily(), ID_ITEM1, item.getString("id"));
        
        LOGGER.traceExit();
    }

}
