package myke.beans.bikes;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import myke.beans.bikes.Bike.Status;
import myke.clients.AcousticContentClient;
import myke.clients.AcousticContentSchema.Content;
import myke.clients.AcousticContentSchema.Search;
import myke.clients.AcousticContentSchema.SearchResult;
import myke.exceptions.CannotRentMultipleBikesException;
import myke.exceptions.InvalidPayloadException;

public class BikeController {

	private static final Logger LOGGER = LogManager.getLogger(BikeController.class);

	private static final String QUERY_FRAGMENT_BIKES = "(classification:content AND type:Bike)";
	private static final String QUERY_BIKES_BY_STATUS = QUERY_FRAGMENT_BIKES + "AND string2:{0}"; 
	private static final String QUERY_MY_BIKES = QUERY_FRAGMENT_BIKES + "AND (string1:{0} OR string2:FREE)"; 
	private static final String QUERY_OWNED_BIKES = QUERY_FRAGMENT_BIKES + "AND string1:{0}"; 
	
	private AcousticContentClient client;

	public BikeController(Vertx vertx, AcousticContentClient client) {
		this.client = client;
	}

	public CompletableFuture<BikeList> getFreeBikes() {
		return getBikes(MessageFormat.format(QUERY_BIKES_BY_STATUS, Status.FREE));
	}

	public CompletableFuture<BikeList> getMyBikes(String userId) {
		return getBikes(MessageFormat.format(QUERY_MY_BIKES, userId)); 
	}
	
	public CompletableFuture<Bike> getBike(String id) {
		return client.getContent(id).thenCompose(BikeTransformer::toBike);
	}

	private CompletableFuture<BikeList> getBikes(String query) {
		return client.search(Search.FIELD_QUERY, query, Search.FIELD_FORMAT, Search.VALUE_FORMAT_DOCUMENT, Search.FIELD_SEED, Long.toString(System.currentTimeMillis())).thenCompose(BikeTransformer::toBikeList);
	}
	

	public CompletableFuture<Bike> updateBike(Bike newBike) {
		return client.getContent(newBike.getId()).thenCompose(currentBikeJson -> {
			return validateBikeUpdate(currentBikeJson, newBike);
		}).thenCompose(currentBikeJson -> {
			return BikeTransformer.mergeBike(newBike, currentBikeJson);
		}).thenCompose(newBikeJson -> {
			return client.putContent(newBikeJson); 
		}).thenCompose(BikeTransformer::toBike);
	}

	/**
	 * Make sure the given user will not own more than one bike after performing the update 
	 * @param currentBike the bike record to be updated
	 * @param newBike the new data for the bike record
	 * @return a future for the currentBike record (to allow passing it on in case of success)
	 */
	private CompletableFuture<JsonObject> validateBikeUpdate(JsonObject currentBike, Bike newBike) {
		LOGGER.debug("currentBike: {}, newBike: {}", currentBike, newBike);
		
		CompletableFuture<JsonObject> result = new CompletableFuture<JsonObject>();
		if(Bike.Status.BOOKED.equals(newBike.getStatus())) {
			String userId = newBike.getOwner();
			if ((userId != null) && (!userId.isEmpty())){
				String query = MessageFormat.format(QUERY_OWNED_BIKES, userId);
				LOGGER.trace("query: " + query);
				client.search(Search.FIELD_QUERY, query).thenAccept(ownedBikes -> {
					int numFound = ownedBikes.getInteger(SearchResult.PROP_NUM_FOUND); 
					if (numFound == 0) {
						LOGGER.traceEntry("success (user does not own any bikes)");
						result.complete(currentBike);
					}
					else {
						JsonObject firstOwnedBike = ownedBikes.getJsonArray(SearchResult.PROP_DOCUMENTS).getJsonObject(0);
						if ((numFound == 1) && (firstOwnedBike.getString(Content.PROP_ID).equals(newBike.getId()))) {
							LOGGER.traceEntry("success (The user already ownes the updated bike)");
							result.complete(currentBike);
						}
						else {
							LOGGER.traceEntry("fail (The user already ownes at least one more bike)");
							result.completeExceptionally(new CannotRentMultipleBikesException(firstOwnedBike.getString(Content.PROP_ID)));
						}
					}
				}).exceptionally(th -> {
					LOGGER.catching(th);
					result.completeExceptionally(th);
					return null;
				});
			}
			else {
				LOGGER.traceEntry("fail (The bike has status BOOKED but no owner)");
				result.completeExceptionally(new InvalidPayloadException("Missing property: Owner"));
			}
		}
		else {
			result.complete(currentBike);
		}
		return LOGGER.traceExit(result);
	}
}
