package myke.exceptions;

/**
 * Access to JSON data failed - typically caused by missing data and/or unexpected schema.  
 *
 */
public class JsonAccessException extends BaseException implements HttpStatusProvider {

	private static final long serialVersionUID = 1L;

	public JsonAccessException(String message) {
		super(500, message);
	}


}
