package myke.clients;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.predicate.ErrorConverter;
import myke.exceptions.RemoteServiceInvocationException;

/**
 * This converter is automatically invoked on all failed requests. It captures the JSON error message and HTTP status code provided by the failed response.
 * 
 * @author DieterBuehler
 *
 */
public class DefaultErrorConverter {
	public static ErrorConverter INSTANCE = ErrorConverter.createFullBody(result -> {

		// Invoked after the response body is fully received
		HttpResponse<Buffer> response = result.response();

		if (response.getHeader("content-type").equals("application/json")) {
			// Error body is JSON data
			JsonObject body = response.bodyAsJsonObject();
			return new RemoteServiceInvocationException(response.statusCode(), body, response.statusMessage());
		}

		// Fallback to defaut message
		return new RemoteServiceInvocationException(response.statusCode(), new JsonObject().put("message",  response.statusMessage()), response.statusMessage());
	});
}