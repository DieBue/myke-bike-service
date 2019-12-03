package myke.exceptions;

public class BaseException extends Exception implements HttpStatusProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final int httpStatus;
	
	public BaseException(int httpStatus) {
		super();
		this.httpStatus = httpStatus;
	}

	public BaseException(int httpStatus, String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.httpStatus = httpStatus;
	}

	public BaseException(int httpStatus, String message, Throwable cause) {
		super(message, cause);
		this.httpStatus = httpStatus;
	}

	public BaseException(int httpStatus, String message) {
		super(message);
		this.httpStatus = httpStatus;
	}

	public BaseException(int httpStatus, Throwable cause) {
		super(cause);
		this.httpStatus = httpStatus;
	}

	@Override
	public int getStatusCode() {
		return httpStatus;
	}

}
