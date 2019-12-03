package myke.exceptions;

public class RequestGenerationException extends BaseException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public RequestGenerationException(Exception cause) {
		super(500, cause);
	}

}
