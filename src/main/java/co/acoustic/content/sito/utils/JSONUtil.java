package co.acoustic.content.sito.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtil {
	private static final Logger LOGGER = LogManager.getLogger(JSONUtil.class);

	public static Object getMemberPath(final JSONObject jsonObject, final String... memberNamePath) throws JSONException {
		LOGGER.traceEntry("{}", jsonObject);
		Object current = jsonObject;
		for (String name : memberNamePath) {
			LOGGER.trace("name: {}", name);
			LOGGER.trace("current: {}, {}", current, (current instanceof JSONObject));
			if ((current != null) && (current instanceof JSONObject)) {
				JSONObject json = (JSONObject) current;
				LOGGER.trace("{} has {}: {}", json, name, json.has(name));
				current = json.has(name) ? json.get(name) : null;
			} else {
				current = null;
				break;
			}
			LOGGER.trace("current: {}", current);
		}

		return current != null ? current : null;
	}
	

}
