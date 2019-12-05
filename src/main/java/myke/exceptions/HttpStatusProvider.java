package myke.exceptions;

/**
 * This object has an associated HTTP response status code. This interface is typically implemented by {@link BaseException} instances.
 * @author DieterBuehler
 *
 */
public interface HttpStatusProvider {
	/**
	 * @return The HTTP response status code.
	 */
	int getStatusCode();
}
