package myke.exceptions;

import io.vertx.core.json.JsonObject;

/**
 * Base exception for exceptions that provide a JSON error message
 * @author DieterBuehler
 *
 */
public class HttpJsonException extends BaseException implements JsonMessageProvider {

	private static final long serialVersionUID = 1L;
	
	private final JsonObject jsonMessage;
	
	public HttpJsonException(int statusCode, JsonObject jsonMessage, String message) {
		super(statusCode, message);
		this.jsonMessage = jsonMessage;
	}

	@Override
	public JsonObject getJsonMessage() {
		return jsonMessage;
	}

	
}
