package myke.exceptions;

/**
 * Invoking a remote API did fail.
 */
import io.vertx.core.json.JsonObject;

public class RemoteServiceInvocationException extends HttpJsonException {

	private static final long serialVersionUID = 1L;
	
	
	public RemoteServiceInvocationException(int statusCode, JsonObject jsonMessage, String message) {
		super(statusCode, jsonMessage, message);
	}
	
}
