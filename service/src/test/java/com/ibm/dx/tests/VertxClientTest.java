package com.ibm.dx.tests;

import com.ibm.wch.utilities.unit.CompletableRunner;
import com.ibm.wch.utilities.unit.vertx.AbstractVertxTestCase;
import com.ibm.wps.utilities.vertx.CompletableHttpClient;
import com.ibm.wps.utilities.vertx.CompletableHttpClientResponse;
import io.vertx.core.http.HttpClientOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.URI;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

@RunWith(CompletableRunner.class)
public class VertxClientTest extends AbstractVertxTestCase {
    private static Logger LOGGER = LogManager.getLogger(VertxClientTest.class.getName());
    private CompletableHttpClient httpClient;

    @Before
    public void setUp() {
        super.setUp();
        LOGGER.info("System properties: {}", System.getProperties());
        final HttpClientOptions opts = new HttpClientOptions()
                .setKeepAlive(false)
                .setVerifyHost(false)
                .setTrustAll(true)
                .setLogActivity(true)
                .setSsl(true);

        httpClient = certx.createHttpClient(opts);
    }

    @Test
    @Ignore
    public CompletionStage<?> testAccess() {
        final URI uri = URI.create("https://aas129.watsoncommerce.ibm.com/wcs/resources/store/1/categoryview/@top");
        final Executor exec = context.getContextExecutor();

        return httpClient.get(uri)
                .thenComposeAsync(CompletableHttpClientResponse::body, exec)
                .thenAcceptAsync(buffer -> LOGGER.info("Response: {}", buffer.toString()), exec);
    }
}