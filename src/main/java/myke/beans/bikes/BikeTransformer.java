package myke.beans.bikes;

import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import myke.clients.AcousticContentSchema.Content;
import myke.clients.AcousticContentSchema.SearchResult;
import myke.exceptions.JsonAccessException;
import utils.JsonUtils;

public class BikeTransformer {
	private static final Logger LOGGER = LogManager.getLogger(BikeTransformer.class);
	
	public static final String[] ACCESSOR_NAME = new String[] {Content.PROP_ELEMENTS, Content.ELEMENT_NAME, Content.PROP_VALUE};
	public static final String[] ACCESSOR_OWNER = new String[] {Content.PROP_ELEMENTS, Content.ELEMENT_OWNER, Content.PROP_VALUE};
	public static final String[] ACCESSOR_STATUS = new String[] {Content.PROP_ELEMENTS, Content.ELEMENT_STATUS, Content.PROP_VALUE, Content.PROP_SELECTION};
	public static final String[] ACCESSOR_LATITUDE = new String[] {Content.PROP_ELEMENTS, Content.ELEMENT_LOCATION, Content.PROP_LATITUDE};
	public static final String[] ACCESSOR_LONGITUDE = new String[] {Content.PROP_ELEMENTS, Content.ELEMENT_LOCATION, Content.PROP_LONGITUDE};
	private static final String EMPTY_STRING ="";
	
	public static CompletableFuture<BikeList> toBikeList(JsonObject searchResult) {
		LOGGER.traceEntry(searchResult.encode());
		BikeList result = new BikeList();
		JsonArray documents = searchResult.getJsonArray(SearchResult.PROP_DOCUMENTS);
		if (documents != null) {
			for (int i=0; i<documents.size(); i++) {
				JsonObject bikeJson = documents.getJsonObject(i).getJsonObject(SearchResult.PROP_DOCUMENT);
				result.add(buildBike(bikeJson));
			}
		}
		return LOGGER.traceExit(CompletableFuture.completedFuture(result));
	}

	public static CompletableFuture<Bike> toBike(JsonObject bike) {
		LOGGER.traceEntry(bike.encode());
		return CompletableFuture.completedFuture(buildBike(bike));
	}
	
	private static Bike buildBike(final JsonObject json) {
		LOGGER.traceEntry();
		final Bike result;
		String name = JsonUtils.get(json, EMPTY_STRING, ACCESSOR_NAME);
		String owner = JsonUtils.get(json, EMPTY_STRING, ACCESSOR_OWNER);
		String status = JsonUtils.get(json, Bike.Status.UNAVAILABLE.toString(), ACCESSOR_STATUS);
		Double latitude = JsonUtils.get(json, null, ACCESSOR_LATITUDE);
		Double longitude = JsonUtils.get(json, null, ACCESSOR_LONGITUDE);
		String id = json.getString(Content.PROP_ID);
		if ((latitude != null) && (longitude != null)) {
			result = new Bike(id, name, owner, longitude, latitude, Bike.Status.valueOf(status)); 
		}
		else {
			result = new Bike(id, name, owner, Bike.Status.valueOf(status));
		}
		return LOGGER.traceExit(result);
	}

	public static CompletableFuture<JsonObject> mergeBike(Bike newBike, JsonObject bike) {
		try {
			mergeProperty(bike, newBike.getName(), ACCESSOR_NAME);
			mergeProperty(bike, newBike.getOwner(), ACCESSOR_OWNER);
			mergeProperty(bike, newBike.getStatus(), ACCESSOR_STATUS);
			mergeProperty(bike, newBike.getLatitude(), ACCESSOR_LATITUDE);
			mergeProperty(bike, newBike.getLongitude(), ACCESSOR_LONGITUDE);
		} catch (JsonAccessException e) {
			CompletableFuture<JsonObject> failed = new CompletableFuture<JsonObject>();
			failed.completeExceptionally(e);
			return failed;
		}
		return CompletableFuture.completedFuture(bike);
	}
	
	private static void mergeProperty(JsonObject json, Object value, String[] accessor) throws JsonAccessException {
		if ((value == null) || (EMPTY_STRING.equals(value))) {
			JsonUtils.remove(json, accessor);
		}
		else {
			JsonUtils.put(json, value, accessor);
		}
	}
	
	
}
