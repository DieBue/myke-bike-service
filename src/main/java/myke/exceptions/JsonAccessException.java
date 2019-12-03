package myke.exceptions;

public class JsonAccessException extends BaseException implements HttpStatusProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public JsonAccessException(String message) {
		super(500, message);
	}


}
