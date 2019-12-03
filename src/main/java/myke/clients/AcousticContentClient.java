package myke.clients;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import myke.Config;
import myke.exceptions.RequestGenerationException;

public class AcousticContentClient extends HttpClient {
	private static final Logger LOGGER = LogManager.getLogger(AcousticContentClient.class);

	private static final String ROUTE_LOGIN = "/login/v1/basicauth";
	private static final String ROUTE_DELIVERY_SEARCH = "/delivery/v1/search";
	private static final String ROUTE_AUTHORING_CONTENT = "/authoring/v1/content";
	public static final String HEADER_AUTHORIZATION = "Authorization";
	public static final String HEADER_VALUE_PREFIX_AUTHORIZATION = "Basic ";
	private final String authHeader;

	public AcousticContentClient(Vertx vertx, Config config) {
		super(vertx, config, "/api/" + config.getTenantId());
		String enc = new Base64().encodeAsString(("apikey:" + config.getApKey()).getBytes());
		authHeader =  HEADER_VALUE_PREFIX_AUTHORIZATION + enc;
	}

//	public CompletableFuture<JsonObject> login() {
//		String enc = new Base64().encodeAsString(("apikey:" + config.getApKey()).getBytes());
//		URI url;
//		try {
//			url = buildUrl(ROUTE_LOGIN, "accept-privacy-notice", "true");
//			return postWithHeader(url, HEADER_AUTHORIZATION, HEADER_VALUE_PREFIX_AUTHORIZATION + enc);
//		} catch (URISyntaxException e) {
//			// should never happen
//			return getFailedFuture(e);
//		}
//	}
	
	public CompletableFuture<JsonObject> search(String... params) {
		try {
			return getAsJsonObject(buildUrl(ROUTE_DELIVERY_SEARCH, params), null, null);
		} catch (URISyntaxException e) {
			// should never happen
			return getFailedFuture(e);
		}
	}

	public CompletableFuture<JsonObject> getContent(String id) {
		LOGGER.debug(id);
		try {
			return getAsJsonObject(buildUrl(ROUTE_AUTHORING_CONTENT + "/" + id), HEADER_AUTHORIZATION, authHeader);
		} catch (URISyntaxException e) {
			// should never happen
			return getFailedFuture(e);
		}
	}

	public CompletableFuture<JsonObject> putContent(JsonObject content) {
		try {
			return putJsonObject(buildUrl(ROUTE_AUTHORING_CONTENT + "/" + content.getString("id")), content, HEADER_AUTHORIZATION, authHeader);
		} catch (URISyntaxException e) {
			// should never happen
			return getFailedFuture(e);
		}
	}

	private CompletableFuture<JsonObject> getFailedFuture(URISyntaxException e) {
		CompletableFuture<JsonObject> fail = new CompletableFuture<JsonObject>();
		fail.completeExceptionally(new RequestGenerationException(e));
		return fail;
	}
	

}