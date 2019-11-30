/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.tests;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.dx.test.BaseTest;
import com.ibm.dx.test.mocks.MockUSSVerticle.Mode;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import junit.framework.Assert;
import test.publishing.common.HttpUnitTestHelper;

@RunWith(VertxUnitRunner.class)
public class TestUssMock extends BaseTest {

	public static final Logger LOGGER = LogManager.getLogger(TestUssMock.class.getName());
    @Test(timeout = 100000)
    public void testUSSMock() throws Exception {
        mockUssVerticle.setMode(Mode.replay);
        JsonObject json = HttpUnitTestHelper.getUrlAsJsonObject(ussURL + "/shop/v1/categories/@top?store=11", 200);
        Assert.assertFalse(json.isEmpty());
    }
}
