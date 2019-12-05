package myke.beans.bikes;

import io.vertx.core.json.JsonObject;
import myke.beans.common.JsonBean;

/**
 * Represents a bike record. Bikes have location, {@link Status} and owner information. 
 * 
 * @author DieterBuehler
 *
 */
public class Bike extends JsonBean {

	/**
	 * The name of the property holding the bike id    
	 */
	public static final String PROP_ID = "id";
	/**
	 * The name of the property holding the bike name    
	 */
	public static final String PROP_NAME = "name";
	/**
	 * The name of the property holding the location latitude information    
	 */
	public static final String PROP_LATITUDE = "latitude";
	/**
	 * The name of the property holding the location longitude information    
	 */
	public static final String PROP_LONGITUDE = "longitude";

	/**
	 * The name of the property holding the status information    
	 */
	public static final String PROP_STATUS = "status";

	/**
	 * The name of the property holding the owner information    
	 */
	public static final String PROP_OWNER = "owner";
	
	public enum Status {FREE, UNAVAILABLE, BOOKED};
	/**
	 * Constructs this bike object from a JSON object. 
	 */
	public Bike(JsonObject json) {
		super(json);
	}
	
	/**
	 * Constructs this bike object from data values. 
	 */
	public Bike(String id, String name, String owner, double longitude, double latitude, Status status) {
		super(new JsonObject()
				.put(PROP_ID, id)
				.put(PROP_NAME, name)
				.put(PROP_OWNER, owner)
				.put(PROP_LONGITUDE, longitude)
				.put(PROP_LATITUDE, latitude)
				.put(PROP_STATUS, status.toString()));
	}
	
	/**
	 * Constructs this bike object from data values. 
	 */
	public Bike(String id, String name, String owner, Status status) {
		super(new JsonObject()
				.put(PROP_ID, id)
				.put(PROP_NAME, name)
				.put(PROP_OWNER, owner)
				.put(PROP_STATUS, status.toString()));
	}

	/**
	 * @return The unique id of this bike.  
	 */
	public String getId() {
		return get(PROP_ID);
	}

	/**
	 * @return The name of this bike.  
	 */
	public String getName() {
		return get(PROP_NAME);
	}

	/**
	 * @return The name of the user that has booked this bike. Only bike in <code>Status.BOOKED</code> will have an owner.   
	 */
	public String getOwner() {
		return get(PROP_OWNER);
	}

	/**
	 * @return The longitude aspect of the bike's current location    
	 */
	public Double getLongitude() {
		return get(PROP_LONGITUDE);
	}

	/**
	 * @return The latitude aspect of the bike's current location    
	 */
	public Double getLatitude() {
		return get(PROP_LATITUDE);
	}
	
	/**
	 * @return The current booking status of this bike    
	 */
	public Status getStatus() {
		String status = get(PROP_STATUS); 
		return Status.valueOf(status); 
	}
}
