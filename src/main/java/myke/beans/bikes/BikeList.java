package myke.beans.bikes;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import myke.beans.common.JsonBean;

/**
 * Represents a list of bikes. The list of bikes is represented by a corresponding JSON array. Additional paging properties are to be added ....
 * 
 * @author DieterBuehler
 *
 */
public class BikeList extends JsonBean {
	
	/*
	 * The name of the property containing the JSON array of bike objects
	 */
	public static final String PROP_ITEMS = "items";


	/**
	 * Constructs this BikeList from JSON data
	 * @param json
	 */
	public BikeList(JsonObject json) {
		super(json);
	}

	/**
	 * Constructs an empty BikeList
	 * @param json
	 */
	public BikeList() {
		super(new JsonObject().put(PROP_ITEMS, new JsonArray()));
	}

	/**
	 * @return The bike found at the given index
	 */
	public Bike get(int index) {
		return new Bike(getOrCreateItemsArray().getJsonObject(index));
	}
	
	/**
	 * Adds a bike to this BikeList
	 */
	public void add(Bike bike) {
		getOrCreateItemsArray().add(bike.getJson());
	}

	/*
	 * Returns the items array. If no such array exists yet, the array gets created and added.
	 */
	private JsonArray getOrCreateItemsArray() {
		JsonArray result = json.getJsonArray(BikeList.PROP_ITEMS);
		if (result == null) {
			result = new JsonArray();
			json.put(PROP_ITEMS, result);
		}
		return result;
	}

}
