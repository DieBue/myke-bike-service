package myke.exceptions;

/**
 * A provided API parameter does not have the expected value.
 */
import java.text.MessageFormat;

public class ParamValueMissmatchException extends BaseException implements HttpStatusProvider {

	private static final long serialVersionUID = 1L;

	static private String PATTERN = "Parameter mismatch. Expected value: {0}, actual: {1}";
	
	public ParamValueMissmatchException(String expected, String actual) {
		super(400, MessageFormat.format(PATTERN, expected, actual));
	}


}
