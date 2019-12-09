package myke.exceptions;

/**
 * Provided request payload is invalid 
 * @author DieterBuehler
 *
 */
public class InvalidPayloadException extends BaseException implements HttpStatusProvider {

	private static final long serialVersionUID = 1L;

	public InvalidPayloadException(String message) {
		super(400, message);
	}


}
