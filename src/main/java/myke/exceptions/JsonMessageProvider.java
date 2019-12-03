package myke.exceptions;

import io.vertx.core.json.JsonObject;

public interface JsonMessageProvider {
	JsonObject getJsonMessage();
}
