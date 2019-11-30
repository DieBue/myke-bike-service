package co.acoustic.content.sito.clients;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.cookie.ClientCookie;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import co.acoustic.content.sito.Config;
import co.acoustic.content.sito.utils.FileAccess;
import co.acoustic.content.sito.utils.TimeHelper;

public class WCHClient extends WebClient {
	private static final Logger LOGGER = LogManager.getLogger(WCHClient.class);

	private static final String ROUTE_LOGIN = "/login/v1/basicauth";
	private static final String ROUTE_DELIVERY_SEARCH = "/delivery/v1/search";
	private static final String ROUTE_AUTHORING_SEARCH = "/authoring/v1/search";
	private static final String ROUTE_DELIVERY_CONTENT = "/delivery/v1/content";
	private static final String ROUTE_DELIVERY_RENDER = "/delivery/v1/rendering/render/content";
	private static final String ROUTE_AUTHORING_CONTENT = "/authoring/v1/content";
	private static final String ROUTE_TYPES = "/authoring/v1/types";
	private static final String ROUTE_LAYOUTS = "/authoring/v1/layouts";
	private static final String ROUTE_LAYOUT_MAPPINGS = "/authoring/v1/layout-mappings";
	private static final String ROUTE_REFERENCES_INCOMING = "/authoring/v1/references/incoming";
	private static final String ROUTE_REFERENCES_OUTGOING = "/authoring/v1/references/outgoing";
	
	private static final String ROUTE_ASSETS = "/authoring/v1/assets";
	private static final String ROUTE_DELIVERY_RESOURCES = "/delivery/v1/resources";
	private static final String ROUTE_AUTHORING_RESOURCES = "/authoring/v1/resources";

	public static final String HEADER_AUTHORIZATION = "Authorization";

	public static final String PARAM_LAST_MODIFIED = "fields";

	public static final String FIELD_LAST_MODIFIED = "lastModified";
	public static final String FIELD_RESOURCE = "resource";
	public static final String FIELD_ID = "id";
	public static final String FIELD_PATH = "path";
	public static final String FIELD_NAME = "name";
	public static final String FIELD_FILE_NAME = "fileName";
	public static final String FIELD_LOCALE = "locale";
	public static final String FIELD_TYPE_ID = "typeId";
	public static final String FIELD_DOCUMENTS = "documents";
	public static final String FIELD_DIGEST = "digest";
	public static final String FIELD_REV = "rev";
	public static final String FIELD_ELEMENTS = "elements";
	public static final String FIELD_ASSET = "asset";

	private String baseUrl;
	private Config config;
	private final boolean isPreview;

	private final String CACHE_FILE_NAME = System.getProperty("user.home") + System.getProperty("file.separator")
			+ "sito-cache.json";

	public WCHClient(Config config, boolean isPreview) throws JSONException {
		super(config.isTrustedHost());
		this.isPreview = isPreview;
		this.baseUrl = isPreview ? config.getWchPreviewApiURL() : config.getWchApiURL();
		this.config = config;
	}

	public WCHClient login() throws ClientProtocolException, IOException, URISyntaxException, JSONException {
		LOGGER.traceEntry();

		List<Header> responseHeaders = tryLoadLoginResponseHeader();
		if (responseHeaders == null) {
			responseHeaders = new ArrayList<Header>();
			String enc = new Base64()
					.encodeAsString((config.getWchUserName() + ":" + config.getWchPassword()).getBytes());
			String url = buildUrl(baseUrl + ROUTE_LOGIN, "accept-privacy-notice", "true");
			postString(url, null, responseHeaders, HEADER_AUTHORIZATION, "Basic " + enc);
			storeLoginResponseHeaders(responseHeaders);
		}

		addLoginCookies(responseHeaders);
		LOGGER.traceExit(responseHeaders);
		return this;
	}

	private void storeLoginResponseHeaders(List<Header> responseHeaders) throws JSONException, IOException {
		JSONObject cache = loadOrCreateResponseCache();
		JSONObject entry = new JSONObject();
		cache.put(config.getName(), entry);
		entry.put("time", Instant.now().toString());
		JSONArray ar = new JSONArray();
		entry.put("headers", ar);
		for (Header header : responseHeaders) {
			JSONObject json = new JSONObject();
			json.put("key", header.getName());
			json.put("value", header.getValue());
			ar.put(json);
		}
		FileAccess.writeFile(CACHE_FILE_NAME, cache.toString());
	}

	private List<Header> tryLoadLoginResponseHeader() {
		try {
			JSONObject entry = loadOrCreateResponseCache().getJSONObject(config.getName());
			if (entry == null) {
				return null;
			}
			Instant creationTime = Instant.parse(entry.getString("time"));
			
			if (Duration.between(creationTime, Instant.now()).toMinutes() > 20) {
				LOGGER.debug("no cached creds (time out)");
				return null;
			}
			List<Header> result = new ArrayList<>();
			JSONArray ar = entry.getJSONArray("headers");
			for (int i = 0; i < ar.length(); i++) {
				JSONObject json = ar.getJSONObject(i);
				String key = json.getString("key");
				String value = json.getString("value");
				result.add(new BasicHeader(key, value));
			}
			LOGGER.debug("using cached creds");
			return result;
		} catch (JSONException e) {
			LOGGER.debug("no cached creds");
			return null;
		}
	}

	private JSONObject loadOrCreateResponseCache() {
		try {
			List<Header> result = new ArrayList<>();
			String str = FileAccess.readFile(CACHE_FILE_NAME);
			return new JSONObject(str);
		} catch (IOException | JSONException e) {
			LOGGER.debug("no cached creds");
			LOGGER.trace(e);
			return new JSONObject();
		}
	}

	public JSONObject auhtoringSearch(String ... params) throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_AUTHORING_SEARCH, params));
	}

	public JSONObject deliverySearch(String ... params) throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_DELIVERY_SEARCH, params));
	}

	public String loadResource(String id) throws ClientProtocolException, IOException, URISyntaxException {
		return getAsString(buildUrl(baseUrl + ROUTE_DELIVERY_RESOURCES + "/" + id));
	}

	public JSONObject loadAuthoringContent(String id) throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_AUTHORING_CONTENT + "/" + id));
	}

	public JSONObject loadEmptyContent(String typeId) throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_TYPES + "/" + typeId + "/new-content"));
	}
	
	public String putContent(JSONObject content, String id) throws ClientProtocolException, IOException, URISyntaxException {
		return putJson(buildUrl(baseUrl + ROUTE_AUTHORING_CONTENT + "/" + id), content, null);
	}

	public String postContent(JSONObject content) throws ClientProtocolException, IOException, URISyntaxException {
		return postJson(buildUrl(baseUrl + ROUTE_AUTHORING_CONTENT), content, null);
	}

	public JSONObject loadDeliveryContent(String id) throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_DELIVERY_CONTENT + "/" + id));
	}

	public String render(String id) throws UnsupportedOperationException, IOException, URISyntaxException {
		return getAsString(buildUrl(baseUrl + ROUTE_DELIVERY_RENDER + "/" + id));
	}

	public JSONObject loadAsset(String id) throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_ASSETS + "/" + id));
	}

	public JSONObject loadType(String id) throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_TYPES + "/" + id));
	}

	public JSONObject loadTypes() throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_TYPES, "limit", "1000"));
	}

	public JSONObject loadLayout(String id) throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_LAYOUTS + "/" + id));
	}

	public JSONObject loadLayouts() throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_LAYOUTS, "limit", "1000"));
	}

	public JSONObject loadLayoutMappings() throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_LAYOUT_MAPPINGS, "limit", "1000"));
	}

	public JSONObject loadOutgoingReferences(String id, String classification) throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_REFERENCES_OUTGOING + "/" + classification + "/" + id, "depth", "1000"));
	}

	public JSONObject loadIncomingReferences(String id, String classification) throws ClientProtocolException, IOException, URISyntaxException {
		return getAsJsonObject(buildUrl(baseUrl + ROUTE_REFERENCES_INCOMING + "/" + classification + "/" + id, "depth", "1000"));
	}

	public JSONObject createType(JSONObject type)
			throws ClientProtocolException, IOException, URISyntaxException, JSONException {
		return new JSONObject(postJson(buildUrl(baseUrl + ROUTE_TYPES), type, null));
	}

	public JSONObject createContent(JSONObject content)
			throws ClientProtocolException, IOException, URISyntaxException, JSONException {
		return new JSONObject(postJson(buildUrl(baseUrl + ROUTE_AUTHORING_CONTENT), content, null));
	}

	/**
	 * Returns the id of the newly created resource
	 * 
	 * @param data
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws JSONException
	 */
	public String createResource(String data, String resourceName)
			throws ClientProtocolException, IOException, URISyntaxException, JSONException {
		JSONObject postResponse = new JSONObject(
				postString(buildUrl(baseUrl + ROUTE_AUTHORING_RESOURCES, "name", resourceName), data, null,
						"Content-Type", "text/html"));
		return postResponse.getString("id");
	}

	public JSONObject createAsset(JSONObject json)
			throws ClientProtocolException, IOException, URISyntaxException, JSONException {
		return new JSONObject(postJson(buildUrl(baseUrl + ROUTE_ASSETS), json, null));

	}

	public long getContentLastModified(String id)
			throws ClientProtocolException, IOException, URISyntaxException, JSONException {
		JSONObject json = getAsJsonObject(
				buildUrl(baseUrl + ROUTE_DELIVERY_CONTENT + "/" + id, PARAM_LAST_MODIFIED, FIELD_LAST_MODIFIED));
		return TimeHelper.parse(json.getString(FIELD_LAST_MODIFIED));
	}

	public long getAssetLastModified(String id)
			throws ClientProtocolException, IOException, URISyntaxException, JSONException {
		JSONObject json = deliverySearch("q", "id:" + id, "fq", "classification:asset", "fl", FIELD_LAST_MODIFIED)
				.getJSONArray(FIELD_DOCUMENTS).getJSONObject(0);
		return TimeHelper.parse(json.getString(FIELD_LAST_MODIFIED));
	}

	private void addLoginCookies(List<Header> headers) throws URISyntaxException, JSONException {
		LOGGER.traceEntry(headers.toString());
		String host = isPreview ? new URI(config.getWchPreviewApiURL()).getHost()
				: new URI(config.getWchApiURL()).getHost();

		for (Header header : headers) {
			if (header.getName().equalsIgnoreCase("set-cookie")) {
				StringTokenizer tok = new StringTokenizer(header.getValue(), ";");
				String val = tok.nextToken();
				String[] v = val.split("=");
				BasicClientCookie cookie = new BasicClientCookie(v[0], v[1]);
				cookie.setDomain(host);
				cookie.setAttribute(ClientCookie.DOMAIN_ATTR, "true");
				cookieStore.addCookie(cookie);
			}
		}

		LOGGER.traceExit(cookieStore);
	}

	public boolean isPreview() {
		return isPreview;
	}



}