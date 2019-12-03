package myke.beans.common;

import io.vertx.core.json.JsonObject;

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
	
    @SuppressWarnings("unchecked")
	public <T> T get(final String key) {
    	return (T)json.getValue(key);
    }
	
}
