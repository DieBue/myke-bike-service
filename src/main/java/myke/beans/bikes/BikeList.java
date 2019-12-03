package myke.beans.bikes;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import myke.beans.common.JsonBean;

public class BikeList extends JsonBean {
	public BikeList(JsonObject json) {
		super(json);
	}

	public BikeList() {
		super(new JsonObject().put(BikeSchema.BikeList.PROP_ITEMS, new JsonArray()));
	}

	public Bike get(int index) {
		return new Bike(getOrCreateItemsArray().getJsonObject(index));
	}
	
	public void add(Bike bike) {
		getOrCreateItemsArray().add(bike.getJson());
	}

	private JsonArray getOrCreateItemsArray() {
		JsonArray result = json.getJsonArray(BikeSchema.BikeList.PROP_ITEMS);
		if (result == null) {
			result = new JsonArray();
			json.put(BikeSchema.BikeList.PROP_ITEMS, result);
		}
		return result;
	}

}
