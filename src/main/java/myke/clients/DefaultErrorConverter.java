package myke.clients;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private static final Logger LOGGER = LogManager.getLogger(AcousticContentClient.class);

	public static ErrorConverter INSTANCE = ErrorConverter.createFullBody(result -> {

		// Invoked after the response body is fully received
		HttpResponse<Buffer> response = result.response();

		if (response.getHeader("content-type").equals("application/json")) {
			// Error body is JSON data
			JsonObject body = response.bodyAsJsonObject();
			LOGGER.trace("Error message body: {}", body);
			return new RemoteServiceInvocationException(response.statusCode(), body, response.statusMessage());
		}

		Buffer msg = response.bodyAsBuffer();
		LOGGER.trace("msg: {}", msg.toString());
		return new RemoteServiceInvocationException(response.statusCode(), new JsonObject().put("status",  response.statusMessage()).put("message", msg), response.statusMessage());
	});
}