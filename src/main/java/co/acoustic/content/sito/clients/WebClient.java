package co.acoustic.content.sito.clients;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.codec.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.config.RequestConfig.Builder;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

public class WebClient {
	private static final Logger LOGGER = LogManager.getLogger(WebClient.class);

	public static final String REQUEST_CONTENT_TYPE = "Content-Type";
	public static final String APP_CONTENT_TYPE = "application/x-www-form-urlencoded";
	public static final String JSON_CONTENT_TYPE = "application/json";
	public static final String TEXT_CONTENT_TYPE = "text/xml;charset=utf-8";
	public static final String AUTHORIZATION = "Authorization";
	public static final String USER_AGENT = "User-Agent";
	public static final String TRANSACT_CLIENT = "Transact-Client";

	protected HttpClient client;
	private static SSLContext sslContext = SSLContexts.createDefault();
	private static SSLContext trustedSslContext;


	private static final TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		}
	} };

	private static final HostnameVerifier hostNameVerifier = new HostnameVerifier() {

		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};

	private static final SSLConnectionSocketFactory SSL_FACTORY = new SSLConnectionSocketFactory(sslContext, new String[] { "TLSv1.2" }, null, hostNameVerifier);
	private static final SSLConnectionSocketFactory TRUSTED_SSL_FACTORY;

	static {
		try {
			trustedSslContext = SSLContext.getInstance("SSL");
			trustedSslContext.init(null, trustAllCerts, new SecureRandom());
			TRUSTED_SSL_FACTORY = new SSLConnectionSocketFactory(trustedSslContext, new String[] { "TLSv1.2" }, null, hostNameVerifier);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		} catch (KeyManagementException e) {
			throw new IllegalStateException(e);
		}
	}
	
	BasicCookieStore cookieStore = new BasicCookieStore();

	public WebClient(boolean trusted) {
		client = getHttpClient(60, trusted);
	}

	protected HttpClient getHttpClient(int timeoutSeconds, boolean trusted) {
		Builder b = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).setConnectTimeout(timeoutSeconds * 1000).setConnectionRequestTimeout(timeoutSeconds * 1000).setSocketTimeout(timeoutSeconds * 1000).setMaxRedirects(20);
		RequestConfig config = b.build();
		SSLConnectionSocketFactory sslFactory = trusted ? TRUSTED_SSL_FACTORY : SSL_FACTORY;
		return HttpClients.custom().setDefaultRequestConfig(config).setSSLSocketFactory(sslFactory).setDefaultCookieStore(cookieStore).build();
	}

	public String getAsString(String url, String... requestHeaders) throws UnsupportedOperationException, IOException {
		System.out.println("Requesting: " + url);
		String result = null;

		HttpGet req = new HttpGet(url);

		if (requestHeaders != null) {
			for (int i = 0; i < requestHeaders.length; i += 2) {
				req.addHeader(requestHeaders[i], requestHeaders[i + 1]);
			}
		}

		String status = "undefined";
		try {
			HttpResponse res = client.execute(req);
			status = res.getStatusLine().toString();
			result = IOUtils.toString(res.getEntity().getContent(), Charsets.UTF_8);
			if (res.getStatusLine().getStatusCode() >= 400) {
				throw new RuntimeException("GET: " + req.getURI() + ": " + res.getStatusLine());
			}
		} finally {
			req.releaseConnection();
			LOGGER.debug("GET: " + url + ", status: " + status);
		}
		return result;
	}

	public JSONObject getAsJsonObject(String url, String... headers) throws UnsupportedOperationException, IOException {
		String str = getAsString(url, headers);
		JSONObject result = null;
		try {
			result = new JSONObject(str);
		} catch (Exception e) {
			LOGGER.error("cannot decode: " + result);
			LOGGER.catching(e);
			throw new IOException(e);
		}

		return result;
	}

	public String postJson(String url, JSONObject json, List<Header> responseHeaders, String... requestHeaders) throws ClientProtocolException, IOException {
		LOGGER.traceEntry("{},{}", url, json);
		HttpPost req = new HttpPost(url);
		return LOGGER.traceExit(sendJson(req, json, responseHeaders, requestHeaders));
	}

	public String postString(String url, String data, List<Header> responseHeaders, String... requestHeaders) throws ClientProtocolException, IOException {
		LOGGER.traceEntry("{},{}", url, data);
		HttpPost req = new HttpPost(url);
		return LOGGER.traceExit(sendJson(req, data, responseHeaders, requestHeaders));
	}

	public String putJson(String url, JSONObject json, List<Header> responseHeaders, String... requestHeaders) throws ClientProtocolException, IOException {
		LOGGER.traceEntry("{},{}", url, json);
		HttpPut req = new HttpPut(url);
		return LOGGER.traceExit(sendJson(req, json, responseHeaders, requestHeaders));
	}

	public String putString(String url, String data, List<Header> responseHeaders, String... requestHeaders) throws ClientProtocolException, IOException {
		LOGGER.traceEntry("{},{}", url, data);
		HttpPut req = new HttpPut(url);
		return LOGGER.traceExit(sendJson(req, data, responseHeaders, requestHeaders));
	}

	private String sendJson(HttpEntityEnclosingRequestBase req, Object data, List<Header> responseHeaders, String... requestHeaders) throws ClientProtocolException, IOException {
		String result = "";
		HttpResponse res = null;
		try {
			if (requestHeaders != null) {
				for (int i = 0; i < requestHeaders.length; i += 2) {
					req.addHeader(requestHeaders[i], requestHeaders[i + 1]);
				}
			}
			if (data != null) {
				String payload;
				if (data instanceof String) {
					payload = (String) data;
				} else {
					payload = (data instanceof JSONObject) ? ((JSONObject) data).toString() : ((JSONArray) data).toString();
				}

				StringEntity body = new StringEntity(payload, Charsets.UTF_8);
				req.setEntity(body);
			}
			res = client.execute(req);
			if (res.getStatusLine().getStatusCode() >= 400) {
				String body;
				try {
					body = IOUtils.toString(res.getEntity().getContent(), Charsets.UTF_8);
				}
				catch (Exception e) {
					// ignore
					body = "No body: " + e.getMessage();
				}
				throw new RuntimeException(req.getMethod() + ": " + req.getURI() + ": " + res.getStatusLine() + " body: " + body);
			}
			LOGGER.debug("{} uri: {} status: {}", req.getMethod(), req.getURI(), res.getStatusLine());
			LOGGER.trace("Response headers: {}" + Arrays.asList(res.getAllHeaders()));
			if (responseHeaders != null) {
				responseHeaders.addAll(Arrays.asList(res.getAllHeaders()));
			}

			result = IOUtils.toString(res.getEntity().getContent());
		} finally {
			req.releaseConnection();
			LOGGER.traceExit();
		}
		return LOGGER.traceExit(result);
	}

	public JSONObject getAsJsonObject(String url) throws ClientProtocolException, IOException {
		return getAsJsonObject(url, (String[]) null);
	}

	protected String buildUrl(String base, String... queryParams) throws URISyntaxException {
		URIBuilder builder = new URIBuilder(base);
		if (queryParams != null) {
			String[] p = queryParams;
			int i = 0;
			if ((p.length % 2) != 0) {
				throw new IllegalArgumentException("Unexpected number of parameters");
			}
			while (i < p.length) {
				builder.addParameter(p[i++], p[i++]);
			}
		}
		return builder.build().toString();
	}
}