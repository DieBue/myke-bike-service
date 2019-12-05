package myke.beans.common;

import io.vertx.core.json.JsonObject;

/**
 * Simple JSON bean that avoids unnecessary serialization/deserialization of JSON data in purely 
 * JSON based systems.
 * 
 * @author DieterBuehler
 *
 */
public class JsonBean {
	
	protected final JsonObject json;
	
	public JsonBean(JsonObject json) {
		this.json = json;
	}

	public JsonObject getJson() {
		return json;
	}
	@Override
	public String toString() {
		return json.encode();
	}
	
	/**
	 * Simple generic getter
	 * @param key
	 * @return
	 */
    @SuppressWarnings("unchecked")
	public <T> T get(final String key) {
    	return (T)json.getValue(key);
    }
	
}
