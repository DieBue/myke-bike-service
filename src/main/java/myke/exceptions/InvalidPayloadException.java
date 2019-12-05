package myke.exceptions;

public class InvalidPayloadException extends BaseException implements HttpStatusProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidPayloadException(String message) {
		super(400, message);
	}


}
