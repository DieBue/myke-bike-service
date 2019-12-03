package myke.beans.bikes;

import io.vertx.core.json.JsonObject;
import myke.beans.common.JsonBean;

public class Bike extends JsonBean {

	enum Status {FREE, UNAVAILABLE, BOOKED};
	
	public Bike(JsonObject json) {
		super(json);
	}
	
	public Bike(String id, String name, String owner, double longitude, double latitude, Status status) {
		super(new JsonObject()
				.put(BikeSchema.Bike.PROP_ID, id)
				.put(BikeSchema.Bike.PROP_NAME, name)
				.put(BikeSchema.Bike.PROP_OWNER, owner)
				.put(BikeSchema.Bike.PROP_LONGITUDE, longitude)
				.put(BikeSchema.Bike.PROP_LATITUDE, latitude)
				.put(BikeSchema.Bike.PROP_STATUS, status.toString()));
	}
	
	public Bike(String id, String name, String owner, Status status) {
		super(new JsonObject()
				.put(BikeSchema.Bike.PROP_ID, id)
				.put(BikeSchema.Bike.PROP_NAME, name)
				.put(BikeSchema.Bike.PROP_OWNER, owner)
				.put(BikeSchema.Bike.PROP_STATUS, status.toString()));
	}

	public String getId() {
		return get(BikeSchema.Bike.PROP_ID);
	}
	public String getName() {
		return get(BikeSchema.Bike.PROP_NAME);
	}
	public String getOwner() {
		return get(BikeSchema.Bike.PROP_OWNER);
	}
	public Double getLongitude() {
		return get(BikeSchema.Bike.PROP_LONGITUDE);
	}

	public Double getLatitude() {
		return get(BikeSchema.Bike.PROP_LATITUDE);
	}
	
	public Status getStatus() {
		String status = get(BikeSchema.Bike.PROP_STATUS); 
		return Status.valueOf(status); 
	}
}
