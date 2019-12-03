package myke.beans.bikes;

import java.text.MessageFormat;
import java.util.concurrent.CompletableFuture;
import static myke.clients.AcousticContentSchema.Search;

import io.vertx.core.Vertx;
import myke.beans.bikes.Bike.Status;
import myke.clients.AcousticContentClient;
import myke.clients.AcousticContentSchema.Content;

public class BikeController {
	
	private static final String QUERY_FRAGMENT_BIKES = "(classification:content AND type:Bike)";
	private static final String QUERY_BIKES_BY_STATUS = QUERY_FRAGMENT_BIKES + "AND string2:{0}"; 
	private static final String QUERY_MY_BIKES = QUERY_FRAGMENT_BIKES + "AND (string1:{0} OR string2:FREE)"; 
	
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
		return client.search(Search.FIELD_QUERY, query, Search.FIELD_FORMAT, Search.VALUE_FORMAT_DOCUMENT).thenCompose(BikeTransformer::toBikeList);
	}
	
	public CompletableFuture<Bike> updateBike(Bike newBike) {
		return client.getContent(newBike.getId()).thenCompose(currentBikeJson -> {
			return BikeTransformer.mergeBike(newBike, currentBikeJson);
		}).thenCompose(newBikeJson -> {
			return client.putContent(newBikeJson); 
		}).thenCompose(BikeTransformer::toBike);
	}
}
