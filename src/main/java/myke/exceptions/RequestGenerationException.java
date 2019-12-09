package myke.exceptions;

/**
 * The creation of a request object failed. 
 * @author DieterBuehler
 *
 */
public class RequestGenerationException extends BaseException {

	private static final long serialVersionUID = 1L;
	
	public RequestGenerationException(Exception cause) {
		super(500, cause);
	}

}
