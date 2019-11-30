package com.ibm.dx.test.mocks;

import java.io.IOException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

import com.ibm.dx.publishing.common.api.logging.RuntimeContextLogger;
import com.ibm.wps.util.StringUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class Client {

    private static final RuntimeContextLogger LOGGER = RuntimeContextLogger.create(Client.class);

    public static final String PROPERTIES_FILE_NAME = "wch.properties";

    SSLContext sslContext = SSLContexts.createDefault();
    HostnameVerifier hostNameVerifier = new HostnameVerifier() {
    
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };
    protected SSLConnectionSocketFactory ssFactory = new SSLConnectionSocketFactory(sslContext, new String[] { "TLSv1.2" }, null, hostNameVerifier);
    protected final HttpClient client;
    protected final HttpClient longTimeoutClient;  
    BasicCookieStore cookieStore = new BasicCookieStore();
    
    public Client() {
        client = getHttpClient(60);
        longTimeoutClient = getHttpClient(600);
    }
    
    protected HttpClient getHttpClient(int timeoutSeconds) {
        Builder b = RequestConfig.custom().setConnectTimeout(timeoutSeconds * 1000).setConnectionRequestTimeout(timeoutSeconds * 1000).setSocketTimeout(timeoutSeconds * 1000).setMaxRedirects(20);
        RequestConfig config = b.build();
        return HttpClients.custom().setDefaultRequestConfig(config).setSSLSocketFactory(ssFactory).setDefaultCookieStore(cookieStore).build();
    }
    
    public static String truncateString(String str, int len) {
        if (str == null) {
            return null;
        }
        else {
            return (str.length() < len) ? str : str.substring(0, (len-1)) + "...";
        }
    }
    
    public Result getUrl(String url) throws UnsupportedOperationException, IOException {
        LOGGER.entry(url);
        Result result = null;
        
        LOGGER.debug("GET " + url);
        HttpGet req = new HttpGet(url);

        String status = "undefined";
        try {
            HttpResponse res = client.execute(req);
            status = res.getStatusLine().toString();
            result = new Result(IOUtils.toString(res.getEntity().getContent()), res.getStatusLine().getStatusCode());
        } finally {
            req.releaseConnection();
            LOGGER.trace("Response status: " + status);
        }
        LOGGER.exit(result);
        return result;
    }
    
    public JsonObject postJson(String url, JsonObject json) throws ClientProtocolException, IOException {
        LOGGER.entry(url, json.encode());
        HttpPost req = new HttpPost(url);
        String str = "";
        HttpResponse res = null;
        try {
            req.addHeader("Content-Type", "application/json");
            StringEntity body = new StringEntity(json.encode());
            req.setEntity(body);
            LOGGER.debug("POST against {} with payload {}", url, json);
            res = client.execute(req);
            LOGGER.debug(res.getStatusLine());
            str = IOUtils.toString(res.getEntity().getContent());
            return new JsonObject(str);
        } catch (io.vertx.core.json.DecodeException e) {
            LOGGER.trace("unexpected content: " + str);
            LOGGER.trace("location: " + res.getFirstHeader("location"));
            LOGGER.catching(e);
            throw e;
        }
        finally {
            req.releaseConnection();
            LOGGER.exit();
        }
    }
    
    public static class Result {
        
        private final String body;
        private final int code;
        
        public Result(String body, int code) {
            this.body = body;
            this.code = code;
        }

        public String getBody() {
            return body;
        }

        public String getContentType() {
            if ((body != null) && body.startsWith("<")) {
                return "text/html";
            }
            return "application/json";
        }

        public int getCode() {
            return code;
        }

        public JsonObject toJsonObject() {
            return StringUtils.isStringNonEmpty(body) ? new JsonObject(body) : null;
        }
        
        public JsonArray toJsonArray() {
            return StringUtils.isStringNonEmpty(body) ? new JsonArray(body) : null;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Result [code=");
            builder.append(code);
            builder.append(", body=");
            builder.append(truncateString(body, 3000));
            builder.append("]");
            return builder.toString();
        }
        
        
    }
    
}