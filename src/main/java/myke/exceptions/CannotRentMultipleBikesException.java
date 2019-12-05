package myke.exceptions;


/**
 * Users cannot own more than on bike at any given point in time. Update operation that would violate this constraint will generate this exception.
 * @author DieterBuehler
 *
 */
public class CannotRentMultipleBikesException extends BaseException implements HttpStatusProvider {

	private static final long serialVersionUID = 1L;

	public CannotRentMultipleBikesException(String ownedBikeId) {
		super(403, "The user already booked the bike with id" + ownedBikeId);
	}


}
