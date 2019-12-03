package utils;

import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import myke.exceptions.HttpJsonException;
import myke.exceptions.JsonAccessException;
/**
 * Some simple JSON helper methods
 * @author DieterBuehler
 *
 */
public class JsonUtils {
	private static final Logger LOGGER = LogManager.getLogger(JsonUtils.class);

	/**
	 * Returns the object found at the given JSON path
	 * @param jsonObject The JSON object
	 * @param defaultValue This value is returned in case the given JSON object does not contain this path
	 * @param path The JSON path, represented by a list of property names
	 * @return the found object value or the default value
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(final JsonObject jsonObject, final T defaultValue, final String... path) {
		LOGGER.traceEntry("json: {}, default: {}, path: {}", jsonObject, defaultValue, Arrays.toString(path));
		LOGGER.debug("what ????");
		Object current = jsonObject;
		for (String name : path) {
			if ((current != null) && (current instanceof JsonObject)) {
				current = ((JsonObject) current).getValue(name);
			} else {
				current = null;
				break;
			}
		}
		T result = current != null ? (T) current : defaultValue;
		return LOGGER.traceExit(result);
	}

	/**
	 * Sets the value on the provided JSON path. The direct parent of the value to be set needs to 
	 * already be present in the JSON object.
	 * @param jsonObject The JSON object
	 * @param value The value to be set
	 * @param path The JSON path, represented by a list of property names
	 * @throws HttpJsonException 
	 */
	public static void put(final JsonObject jsonObject, final Object value, final String... path) throws JsonAccessException {
		Object current = jsonObject;
		int pathToContainerLength = path.length-1;
		for (int i=0; i<pathToContainerLength; i++) {
			if ((current != null) && (current instanceof JsonObject)) {
				current = ((JsonObject) current).getValue(path[i]);
			} else {
				current = null;
				break;
			}
		}
		if (current == null) {
			throw new JsonAccessException("Path not found: " + Arrays.asList(path));
		}
		else if (current instanceof JsonObject) {
			JsonObject parent = (JsonObject) current; 
			if (value == null) {
				parent.putNull(path[pathToContainerLength]);
			}
			else if ((value instanceof JsonObject) || (value instanceof JsonArray) || (value instanceof Double)) {
				parent.put(path[pathToContainerLength], value);
			}
			else {
				parent.put(path[pathToContainerLength], value.toString());
			}
		}
		else {
			throw new JsonAccessException("Unexpected parent type: " + current);
		}
	}

	/**
	 * Removes the identified property from the given JSON object 
	 * already be present in the JSON object.
	 * @param jsonObject The JSON object
	 * @param path The JSON path, represented by a list of property names
	 * @throws HttpJsonException 
	 */
	public static void remove(final JsonObject jsonObject, final String... path) throws JsonAccessException {
		Object current = jsonObject;
		int pathToContainerLength = path.length-1;
		for (int i=0; i<pathToContainerLength; i++) {
			if ((current != null) && (current instanceof JsonObject)) {
				current = ((JsonObject) current).getValue(path[i]);
			} else {
				current = null;
				break;
			}
		}
		if (current instanceof JsonObject) {
			((JsonObject) current).remove(path[pathToContainerLength]);
		}
		else {
			throw new JsonAccessException("Unexpected parent type: " + current);
		}
	}
}
