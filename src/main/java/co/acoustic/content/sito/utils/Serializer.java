package co.acoustic.content.sito.utils;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Serializer {
	private static final Logger LOGGER = LogManager.getLogger(Serializer.class);

	private static String generateString (Object obj) {
		if (obj != null) {
			try {
				if (obj instanceof JSONObject) {
					return ((JSONObject)obj).toString(2);
				}
				else if (obj instanceof JSONArray) {
					return ((JSONArray)obj).toString(2);
				}
			} catch (JSONException e) {
				return e.toString();
			}
			return obj.toString();
		}
		return "null";
	}

	public static String generateResult(String result, String fileName) throws IOException {
		if (fileName != null) {
			FileAccess.writeFile(fileName, result);
		}
		return result;
	}

	public static String generateResult (JSONObject json, String fileName, String ... keys) throws IOException {
		LOGGER.traceEntry("json: {}, keys: {}", json.toString(), keys);
		String result; 
		Object filtered;
		try {
			if (keys == null) {
				return generateString(json);
			}
			filtered = json != null ? getFilteredJson(json, keys) : null;
			result = generateString(filtered);
		} catch (JSONException e) {
			result = e.toString();
		}
		
		if (fileName != null) {
			FileAccess.writeFile(fileName, result);
		}
		
		return LOGGER.traceExit(result);
	}

	public static String generateResult (JSONArray json, String fileName, String ... keys) throws IOException {
		LOGGER.traceEntry("json: {}, keys: {}", json.toString(), keys);
		String result; 
		Object filtered;
		try {
			if (keys == null) {
				return generateString(json);
			}
			filtered = json != null ? getFilteredJsonArray(json, keys) : null;
			result = generateString(filtered);
		} catch (JSONException e) {
			result = e.toString();
		}
		
		if (fileName != null) {
			FileAccess.writeFile(fileName, result);
		}
		
		return LOGGER.traceExit(result);
	}

	private static Object getFilteredJson(JSONObject json, String[] keys) throws JSONException {
		LOGGER.traceEntry();
		JSONArray ar = json.has("items") ? json.getJSONArray("items") : null;
		if (ar == null) {
			ar = json.has("documents") ? json.getJSONArray("documents") : null;
		}
		Object result = (ar == null) ? new JSONObject(json, keys) : getFilteredJsonArray(ar, keys); 
		return LOGGER.traceExit(result);
	}

	private static JSONArray getFilteredJsonArray(JSONArray ar, String[] keys) throws JSONException {
		LOGGER.traceEntry();
		JSONArray result = new JSONArray();
		for (int i=0; i<ar.length(); i++) {
			result.put(new JSONObject(ar.getJSONObject(i), keys));
		}
		return LOGGER.traceExit(result);
	}

}
