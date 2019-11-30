package com.ibm.dx.test.mocks;

import com.ibm.dx.publishing.common.api.RuntimeContextFactory;
import com.ibm.dx.publishing.common.api.logging.RuntimeContext;
import com.ibm.dx.publishing.common.api.services.ServiceConfig;
import com.ibm.dx.test.mocks.Client.Result;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;

@SuppressWarnings({ "squid:S2142" })
public class MockUSSVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LogManager.getLogger(MockUSSVerticle.class.getName());
    private static final RuntimeContext defaultRc = RuntimeContextFactory.getInstance().newSystemRuntimeContext();
    
    private final Client client;
    
    private final JsonObject CONFIG;
    private final String baseDir;
    private Mode mode;
    public enum Mode {record, replay, proxy};

    private static final String localResourcesPath = "/uss-resources/";
    private static final String localPath = "/prod-publishing-connector-service/service/src/main/resources" + localResourcesPath;


    public MockUSSVerticle() throws IOException { 
        this(new JsonObject().put("proto", "http").put("port", -1).put("host", "dch-dxcloud.rtp.raleigh.ibm.com").put("baseFolder", "C:/src/prisma").put("mode", "replay").put("baseUrlPath", "/api/5e67d94e-ec9e-45f9-b598-ba83dea195cc"));
    }
    
    public MockUSSVerticle(JsonObject config) throws IOException {
        LOGGER.traceEntry(() -> config);
        client = new Client();
        CONFIG = config;
        baseDir = config.getString("baseFolder") + localPath;
        this.mode = Mode.valueOf(config.getString("mode"));
        LOGGER.traceExit(this);
    }
    

    @Override
    public void init(Vertx vertx, Context context) {
        super.init(vertx, context);
    }

    public void start(Future<Void> startFuture) {
        LOGGER.traceEntry();
        HttpServer server = vertx.createHttpServer();

        final Router router = Router.router(vertx);
        
        // mock only entry points 
        router.get("/*").handler(this::handleRoot);
            
        server.requestHandler(router::accept);
        server.listen(ServiceConfig.getPort(defaultRc, this), res -> {
            if (res.succeeded()) {
                LOGGER.trace("completing ...");
                LOGGER.trace("listening on port: " + ServiceConfig.getPort(defaultRc, this));
                startFuture.complete();
            } else {
                LOGGER.error("bind failed", res.cause());
                startFuture.fail(res.cause());
            }
        });

        LOGGER.traceExit();
    }

    public void setMode(Mode mode) {
        this.mode = mode; 
    }

    private void handleRoot(RoutingContext ctx) {
        LOGGER.traceEntry(() -> ctx);
        HttpServerResponse response = ctx.response();
        response.putHeader("content-type", "application/json");
        Result r = null;
        URI uri;
        try {
            uri = new URI(ctx.request().absoluteURI());
            r = get(uri);
            record(uri, r);
            response.setStatusCode(r.getCode());
            response.headers().add("Content-Type", r.getContentType());
            response.end(r.getBody());
        } catch (Exception e) {
            LOGGER.catching(e);
            response.setStatusCode(500);
            response.headers().add("Content-Type", "text/plain");
            response.end(e.getMessage());
        } 
        LOGGER.traceExit(r);
    }

    private Result get(URI uri) throws URISyntaxException, UnsupportedOperationException, IOException {
        LOGGER.traceEntry(uri.toString());
        Result result;
        if (mode == Mode.replay) {
            result = readFromRecording(uri);
        }
        else {
            URI newURI = new URI(CONFIG.getString("proto"), uri.getUserInfo(), CONFIG.getString("host"), CONFIG.getInteger("port"), CONFIG.getString("baseUrlPath") + uri.getPath(), uri.getQuery(), uri.getFragment());
            result = client.getUrl(newURI.toString());
        }
        LOGGER.traceExit(result);
        return result;
            
    }

    private void record(URI uri, Result r) throws IOException {
        LOGGER.traceEntry(uri.toString());
        if (mode == Mode.record && r.getCode()<400) {
            String fileName = getFileSystemFileName(uri);
            FileWriter fw = new FileWriter(fileName, false);
            fw.write(r.getBody());
            fw.flush();
            fw.close();
        }
        LOGGER.traceExit();
    }

    private Result readFromRecording(URI uri) throws IOException {
        LOGGER.traceEntry(uri.toString());

        Result result;
        String fileName = getResourcesFileName(uri);
        try {
            String str = MockData.INSTANCE.getAsString(fileName);
            result = new Result(str, 200);
        }
        catch (FileNotFoundException e) {
            result = new Result("The given file name: " + fileName + " was not found in the recording. Consider re-recording.", 404);
            LOGGER.traceExit();
            return result;
        }

        LOGGER.traceExit();
        return result;
    }

    private String getFileSystemFileName(URI uri) throws UnsupportedEncodingException {
        return getFileContent(baseDir, uri);
    }

    private String getResourcesFileName(URI uri) throws UnsupportedEncodingException {
        return getFileContent(localResourcesPath, uri);
    }

    private String getFileContent(String basePath, URI uri) throws UnsupportedEncodingException {
        LOGGER.traceEntry(uri.toString());
        
        String str = uri.toString();
        
        if (str.startsWith("http://")) {
            str = str.substring(7);
        }
        else if (str.startsWith("https://")) {
            str = str.substring(8);
        }
        
        str = str.substring(str.indexOf("/")+1);

        String enc = URLEncoder.encode(str, "UTF-8");
        enc = enc.replace("%2F", "--");

        String result = basePath + enc + ".json"; 
        LOGGER.traceExit(result);
        return result;
    }

}
