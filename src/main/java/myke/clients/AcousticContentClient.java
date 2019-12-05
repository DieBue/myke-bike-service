package myke.clients;

import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import myke.Config;
import myke.exceptions.RequestGenerationException;

/**
 * HTTP client to access the Acoustic Content REST API
 * (https://developer.ibm.com/api/view/dx-prod:ibm-watson-content-hub:title-IBM_Watson_Content_Hub#Overview)
 * @author DieterBuehler
 *
 */
public class AcousticContentClient extends HttpClient {
	private static final Logger LOGGER = LogManager.getLogger(AcousticContentClient.class);

	// Search on published content
	private static final String ROUTE_DELIVERY_SEARCH = "/delivery/v1/search";

	// Load content item
	private static final String ROUTE_AUTHORING_CONTENT = "/authoring/v1/content/";

	// Authorization header
	public static final String HEADER_AUTHORIZATION = "Authorization";

	// Authorization header value encoding prefix
	public static final String HEADER_VALUE_PREFIX_AUTHORIZATION = "Basic ";

	// The API key won't time out so we cache the authorization header in this value
	private final String authHeader;

	/**
	 * Constructs this client. We can use a single shared client per verticle. 
	 */
	public AcousticContentClient(Vertx vertx, Config config) {
		super(vertx, config, "/api/" + config.getTenantId());
		String enc = new Base64().encodeAsString(("apikey:" + config.getApKey()).getBytes());
		authHeader =  HEADER_VALUE_PREFIX_AUTHORIZATION + enc;
	}

	/**
	 * Performs a search on published content.
	 * @param params Query parameters as defined by the Acoustic Content search API 
	 */
	public CompletableFuture<JsonObject> search(String... params) {
		try {
			return getAsJsonObject(buildUrl(ROUTE_DELIVERY_SEARCH, params), null, null);
		} catch (URISyntaxException e) {
			// should never happen
			return getFailedFuture(e);
		}
	}

	/**
	 * @return A future for a content item loaded by id  
	 */
	public CompletableFuture<JsonObject> getContent(String id) {
		LOGGER.debug(id);
		try {
			return getAsJsonObject(buildUrl(ROUTE_AUTHORING_CONTENT + id), HEADER_AUTHORIZATION, authHeader);
		} catch (URISyntaxException e) {
			// should never happen
			return getFailedFuture(e);
		}
	}

	/**
	 * @return A future for updating an existing content item  
	 */
	public CompletableFuture<JsonObject> putContent(JsonObject content) {
		try {
			return putJsonObject(buildUrl(ROUTE_AUTHORING_CONTENT + content.getString("id")), content, HEADER_AUTHORIZATION, authHeader);
		} catch (URISyntaxException e) {
			// should never happen
			return getFailedFuture(e);
		}
	}

	/*
	 * Constructs a future failed with a specific exception
	 */
	private CompletableFuture<JsonObject> getFailedFuture(URISyntaxException e) {
		CompletableFuture<JsonObject> fail = new CompletableFuture<JsonObject>();
		fail.completeExceptionally(new RequestGenerationException(e));
		return fail;
	}
	

}