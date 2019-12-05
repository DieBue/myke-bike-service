package myke.exceptions;

public class CannotRentMultipleBikesException extends BaseException implements HttpStatusProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CannotRentMultipleBikesException(String ownedBikeId) {
		super(403, "The user already booked the bike with id" + ownedBikeId);
	}


}
