package myke.exceptions;

public class MissingParameterException extends BaseException implements HttpStatusProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MissingParameterException(String key) {
		super(400, "Missing parameter: " + key);
	}


}
