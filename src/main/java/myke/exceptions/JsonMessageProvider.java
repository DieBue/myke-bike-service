package myke.exceptions;

/**
 * This object has an associated JSON (error) message. This interface is typically implemented by {@link BaseException} instances.
 * 
 * @author DieterBuehler
 *
 */
import io.vertx.core.json.JsonObject;

public interface JsonMessageProvider {
	JsonObject getJsonMessage();
}
