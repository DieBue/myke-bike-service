package myke.clients;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Vertx;
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

	private WebClient client = null; 
	protected final Config config;
	private final String baseUrl;
	
	private static final ResponsePredicate SUCCESS = ResponsePredicate.create(ResponsePredicate.SC_SUCCESS, DefaultErrorConverter.INSTANCE);
	
	public HttpClient(Vertx vertx, Config config, String baseUrl) {
		this.config = config;
		this.baseUrl = baseUrl;
		client = WebClient.create(vertx,  new WebClientOptions().setSsl(true));
	}
	
	
	protected URI buildUrl(String route, String... queryParams) throws URISyntaxException {
		URIBuilder builder = new URIBuilder(baseUrl + route);
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
		return builder.build();
	}

	protected CompletableFuture<JsonObject> getAsJsonObject(URI url, String headerName, String headerValue) {
		LOGGER.debug("url: {}, config: {}", url, config);
		CompletableFuture<JsonObject> result = new CompletableFuture<>();
		HttpRequest<JsonObject> request = client.get(443, config.getHost(), url.toString())
		.as(BodyCodec.jsonObject())
		.expect(SUCCESS);
		if (headerName != null) {
			request.putHeader(headerName, headerValue);
		}
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

	protected CompletableFuture<JsonObject> putJsonObject(URI url, JsonObject json, String headerName, String headerValue) {
		LOGGER.debug("url: {}, json: {}, config: {}", url, json, config);
		CompletableFuture<JsonObject> result = new CompletableFuture<>();
		HttpRequest<JsonObject> request = client.put(443, config.getHost(), url.toString())
		.as(BodyCodec.jsonObject())
		.expect(SUCCESS);
		if (headerName != null) {
			request.putHeader(headerName, headerValue);
		}
		
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
}
