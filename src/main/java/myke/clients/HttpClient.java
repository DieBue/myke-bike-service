package myke.clients;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.client.predicate.ResponsePredicate;
import io.vertx.ext.web.codec.BodyCodec;
import myke.Config;

/**
 * Small wrapper around {@link io.vertx.core.http.HttpClient} that applies our configuration settings and error converters.
 * 
 * @author DieterBuehler
 *
 */
public class HttpClient {
	private static final Logger LOGGER = LogManager.getLogger(HttpClient.class);

	private static final String HEADER_COOKIE = "Cookie"; 
	
	private WebClient client = null; 
	protected final Config config;
	private final String baseUrl;
	
	/**
	 *  The default success predicate that leverages a {@link DefaultErrorConverter} to capture error messages sent by the downstream service.
	 */
	private static final ResponsePredicate SUCCESS = ResponsePredicate.create(ResponsePredicate.SC_SUCCESS, DefaultErrorConverter.INSTANCE);
	
	/**
	 * Create this client
	 * @param vertx The vertx instance
	 * @param config The configuration
	 * @param baseUrl The base URL
	 */
	public HttpClient(Vertx vertx, Config config, String baseUrl) {
		this.config = config;
		this.baseUrl = baseUrl;
		client = WebClient.create(vertx,  new WebClientOptions().setSsl(true));
	}
	
	
	/**
	 * Builds a absolute URLs based on your configured base URL.
	 * @param route The API route 
	 * @param queryParams URL query parameters 
	 * @return A future on the resulting URI. 
	 */
	protected CompletableFuture<URI> buildUrl(String route, String... queryParams) {
		CompletableFuture<URI> result = new CompletableFuture<>();
		try {
			URIBuilder builder = new URIBuilder(baseUrl + route);
			if (queryParams != null) {
				String[] p = queryParams;
				int i = 0;
				if ((p.length % 2) != 0) {
					result.completeExceptionally(new IllegalArgumentException("Unexpected number of parameters"));
				}
				while (i < p.length) {
					builder.addParameter(p[i++], p[i++]);
				}
			}
			result.complete(builder.build());
		} catch (URISyntaxException e) {
			result.completeExceptionally(e);
		}
		return result;
	}

	/**
	 * Loads the data from the given URL and transforms it into a JSON object
	 * 
	 * @return A future on the loaded JSON object
	 */
	protected CompletableFuture<JsonObject> getAsJsonObject(URI url) {
		return getAsJsonObject(url, null);
	}

	/**
	 * Loads the data from the given URL and transforms it into a JSON object
	 * 
	 * @param cookies Additional cookies to be attached to the out-bound request
 	 * @return A future on the loaded JSON object
	 */
	protected CompletableFuture<JsonObject> getAsJsonObject(URI url, List<Cookie> cookies) {
		LOGGER.debug("url: {}", url);
		CompletableFuture<JsonObject> result = new CompletableFuture<>();
		HttpRequest<JsonObject> request = client.get(443, config.getHost(), url.toString())
		.as(BodyCodec.jsonObject())
		.expect(SUCCESS);
		addCookies(cookies, request);
		request.send(asyncResult -> {
			if (asyncResult.succeeded()) {
				HttpResponse<JsonObject> response = asyncResult.result();
				LOGGER.trace("GET {} succeeded. Response: {}", url, response);
				result.complete(response.body());
			}
			else {
				LOGGER.error("Request failed", asyncResult.cause());
				result.completeExceptionally(asyncResult.cause());
			}
		});
		return result;
	}

	/**
	 * Passes the provided JSON object to the given URL using a PUT request, reads the response and turns it into a JSON object. 
	 * @return A future on the updated loaded JSON object
	 */
	protected CompletableFuture<JsonObject> putJsonObject(URI url, JsonObject json) {
		return putJsonObject(url, json, null);
	}

	/**
	 * Passes the provided JSON object to the given URL using a PUT request, reads the response and turns it into a JSON object.
	 * @param cookies Additional cookies to be added to the out-bound request 
	 * @return A future on the updated loaded JSON object
	 */
	protected CompletableFuture<JsonObject> putJsonObject(URI url, JsonObject json, List<Cookie> cookies) {
		LOGGER.debug("url: {}, json: {}", url, json);
		CompletableFuture<JsonObject> result = new CompletableFuture<>();
		HttpRequest<JsonObject> request = client.put(443, config.getHost(), url.toString())
		.as(BodyCodec.jsonObject())
		.expect(SUCCESS);
		addCookies(cookies, request);
		
		request.sendJsonObject(json, asyncResult -> {
			if (asyncResult.succeeded()) {
				HttpResponse<JsonObject> response = asyncResult.result();
				LOGGER.trace("GET {} succeeded. Response: {}", url, response);
				JsonObject resultJson = response.body();
				result.complete(resultJson);
			}
			else {
				LOGGER.error("Request failed", asyncResult.cause());
				result.completeExceptionally(asyncResult.cause());
			}
		});
		return result;
	}

	/**
	 * Performs a POST operation on a given URL 
	 * @param headerName The name of an additional header to be added to the out-bound request
	 * @param headerValue The value of an additional header to be added to the out-bound request
	 * @return A future on response object
	 */
	public CompletableFuture<HttpResponse<Buffer>> post(URI url, String headerName, String headerValue) {
		LOGGER.debug("url: {}", url);
		CompletableFuture<HttpResponse<Buffer>> result = new CompletableFuture<>();
		HttpRequest<Buffer> request = client.post(443, config.getHost(), url.toString())
		.expect(SUCCESS);
		if (headerName != null) {
			request.putHeader(headerName, headerValue);
		}
		
		request.send(asyncResult -> {
			if (asyncResult.succeeded()) {
				HttpResponse<Buffer> response = asyncResult.result();
				LOGGER.trace("POST {} succeeded. Response headers: {}", response);
				result.complete(response);
			}
			else {
				LOGGER.error("Request failed", asyncResult.cause());
				result.completeExceptionally(asyncResult.cause());
			}
		});
		return result;
	}

	/**
	 * Adds the provided cookies to the request object
	 * @param cookies
	 * @param request
	 */
	private void addCookies(List<Cookie> cookies, HttpRequest<JsonObject> request) {
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				request.putHeader(HEADER_COOKIE, cookie.encode());
			}
		}
	}
}
