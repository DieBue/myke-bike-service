package myke.clients;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import myke.Config;

/**
 * HTTP client to access the Acoustic Content REST API
 * (https://developer.ibm.com/api/view/dx-prod:ibm-watson-content-hub:title-IBM_Watson_Content_Hub#Overview)
 * 
 * @author DieterBuehler
 *
 */
public class AcousticContentClient extends HttpClient {
	private static final Logger LOGGER = LogManager.getLogger(AcousticContentClient.class);

	// login route
	private static final String ROUTE_LOGIN = "/login/v1/basicauth";

	private static final String[] LOGIN_PARAMS = new String[] { "accept-privacy-notice", "true" };

	// Search on published content
	private static final String ROUTE_DELIVERY_SEARCH = "/delivery/v1/search";

	// Search on published content
	private static final String ROUTE_AUTHORING_SEARCH = "/authoring/v1/search";

	// Load content item
	private static final String ROUTE_AUTHORING_CONTENT = "/authoring/v1/content/";

	// Authorization header
	public static final String HEADER_AUTHORIZATION = "Authorization";

	// Authorization header value encoding prefix
	public static final String HEADER_VALUE_PREFIX_AUTHORIZATION = "Basic ";

	// The API key won't time out so we cache the authorization header in this value
	private final String authorizationHeaderValue;

	// Cache time for the authentication cookies. We cache 20 minutes which is guaranteed to the be shorter than the authentication cookie expiration time.
	private static final int AUTHENTICATION_CACHE_TIME_IN_SECONDS = 60 * 20;
	
	// Cache authentication cookies
	private CompletableFuture<List<Cookie>> cachedAuthenticationCookies;

	// Time stamp to handle cache expiration
	private Instant nextExpiration = Instant.now();

	/**
	 * Constructs this client. We can use a single shared client per verticle.
	 */
	public AcousticContentClient(Vertx vertx, Config config) {
		super(vertx, config, "/api/" + config.getTenantId());
		String enc = new Base64().encodeAsString(("apikey:" + config.getApKey()).getBytes());
		authorizationHeaderValue = HEADER_VALUE_PREFIX_AUTHORIZATION + enc;
	}

	/**
	 * Performs a search on published content.
	 * 
	 * @param params Query parameters as defined by the Acoustic Content search API
	 */
	public CompletableFuture<JsonObject> search(String... params) {
		return buildUrl(ROUTE_DELIVERY_SEARCH, params).thenCompose(url -> {
			return getAsJsonObject(url);
		});
	}

	/**
	 * Performs a search on authoring content (potentially not yet published).
	 * 
	 * @param params Query parameters as defined by the Acoustic Content search API
	 */
	public CompletableFuture<JsonObject> searchAuthoring(String... params) {
		return getLoginCookiesWithCache().thenCompose(cookies -> {
			return buildUrl(ROUTE_AUTHORING_SEARCH, params).thenCompose(url -> {
				return getAsJsonObject(url, cookies);
			});
		});
	}

	/**
	 * @return A future for a content item loaded by id
	 */
	public CompletableFuture<JsonObject> getContent(String id) {
		return getLoginCookiesWithCache().thenCompose(cookies -> {
			return buildUrl(ROUTE_AUTHORING_CONTENT + id).thenCompose(url -> {
				return getAsJsonObject(url, cookies);
			});
		});
	}

	/**
	 * @return A future for updating an existing content item
	 */
	public CompletableFuture<JsonObject> putContent(JsonObject content) {
		return getLoginCookiesWithCache().thenCompose(cookies -> {
			return buildUrl(ROUTE_AUTHORING_CONTENT + content.getString("id")).thenCompose(url -> {
				return putJsonObject(url, content, cookies);
			});
		});
	}

	/**
	 * Go gets the login cookies from cache. If the cache is empty or expired, new login cookies will be obtained from the back-end.
	 * 
	 */
	private CompletableFuture<List<Cookie>> getLoginCookiesWithCache() {
		if (Instant.now().isAfter(nextExpiration) || (cachedAuthenticationCookies == null)) {
			LOGGER.trace("Performing login ...");
			cachedAuthenticationCookies = getLoginCookies();
			nextExpiration = Instant.now().plusSeconds(AUTHENTICATION_CACHE_TIME_IN_SECONDS);
		} else {
			LOGGER.trace("Using cookies from cache.");
		}
		return cachedAuthenticationCookies;

	}

	/**
	 * Loads new login cookies from the back-end by calling the login API
	 */
	public CompletableFuture<List<Cookie>> getLoginCookies() {
		return buildUrl(ROUTE_LOGIN, LOGIN_PARAMS).thenCompose(url -> {
			return post(url, HEADER_AUTHORIZATION, authorizationHeaderValue).thenCompose(response -> {
				ArrayList<Cookie> result = new ArrayList<>();
				List<String> cookieHeaders = response.cookies();
				for (String cookieHeader : cookieHeaders) {
					addCookie(result, cookieHeader);
				}
				LOGGER.trace("Login cookies: " + result);
				return CompletableFuture.completedFuture(result);
			});
		});
	}

	/*
	 * Helper method to build a Cookie object from a Set-Cookie header
	 */
	private void addCookie(ArrayList<Cookie> result, String cookieHeader) {
		LOGGER.traceEntry(cookieHeader);
		if (cookieHeader != null) {
			String value = cookieHeader.substring(0, cookieHeader.indexOf(';'));
			String[] split = value.split("=");
			if (split.length == 2) {
				Cookie cookie = Cookie.cookie(split[0], split[1]);
				LOGGER.trace("Adding cookie {}", cookie.encode());
				result.add(cookie);
			}
		}
		LOGGER.traceExit();
	}

}