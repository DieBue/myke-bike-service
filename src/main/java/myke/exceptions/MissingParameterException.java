package myke.exceptions;

/**
 * A mandatory API parameter is missing.
 * @author DieterBuehler
 *
 */
public class MissingParameterException extends BaseException implements HttpStatusProvider {

	private static final long serialVersionUID = 1L;

	public MissingParameterException(String key) {
		super(400, "Missing parameter: " + key);
	}


}
