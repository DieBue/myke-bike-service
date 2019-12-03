package myke;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import myke.beans.bikes.BikeSchema;



public class TestService {

	private static Vertx vertx = Vertx.vertx(new VertxOptions().setBlockedThreadCheckInterval(1000*10));
	private static Service service;
	private static Config config;
	private static String baseURL;

	@BeforeClass
	public static void setUpClass() throws IOException, InterruptedException, ExecutionException {
		CompletableFuture<Void> ready = new CompletableFuture<>();
		config = new Config();
		baseURL = "http://localhost:" + config.getServerPort();
		service = new Service(config);
		DeploymentOptions options = new DeploymentOptions().setConfig(config.getJson());
		System.out.println("Deploying verticle ...");
		vertx.deployVerticle(service, options, res -> {
			if (res.succeeded()) {
				System.out.println("Service ready. Listening on port: " + config.getServerPort());
				ready.complete(null);
			} else {
				System.out.println("Deployment failed: " + res.cause());
				ready.completeExceptionally(res.cause());
			}
		});
		ready.get();
	}

	@Test
	public void testBikes() throws TestException, IOException, SAXException {
		JsonObject bikes = HttpUnitTestHelper.getUrlAsJsonObject(baseURL + "/api/bikes", 200);
		Assert.assertNotNull(bikes);
		JsonArray ar = bikes.getJsonArray(BikeSchema.BikeList.PROP_ITEMS);
		Assert.assertTrue(ar.size()>0);
		JsonObject bike0 = ar.getJsonObject(0);
		Assert.assertNotNull(bike0.getString(BikeSchema.Bike.PROP_NAME));
		Assert.assertNotNull(bike0.getString(BikeSchema.Bike.PROP_STATUS));
	}

	@Test
	public void testBike() throws TestException, IOException, SAXException {
		JsonObject bike = HttpUnitTestHelper.getUrlAsJsonObject(baseURL + "/api/bikes/be6e1fa8-fc02-47e4-993c-e259525cd474", 200);
		Assert.assertNotNull(bike);
		Assert.assertEquals("Bike Number 1", bike.getString(BikeSchema.Bike.PROP_NAME));
		Assert.assertTrue(Arrays.asList(new String[] {"FREE", "BOOKED"}).contains(bike.getString(BikeSchema.Bike.PROP_STATUS)));
	}

	@Test
	public void testBikeUpdate() throws TestException, IOException, SAXException {
		String route = baseURL + "/api/bikes/be6e1fa8-fc02-47e4-993c-e259525cd474";
		
		// get the current bike
		JsonObject bike = HttpUnitTestHelper.getUrlAsJsonObject(route, 200);
		Assert.assertNotNull(bike);
		
		// keep track of current properties
		String initialStatus = bike.getString(BikeSchema.Bike.PROP_STATUS);
		String name = bike.getString(BikeSchema.Bike.PROP_NAME);
		Double lat = bike.getDouble(BikeSchema.Bike.PROP_LATITUDE);
		Double lon = bike.getDouble(BikeSchema.Bike.PROP_LONGITUDE);
		Assert.assertNotNull(lat);
		Assert.assertNotNull(lon);
		String owner = bike.getString(BikeSchema.Bike.PROP_OWNER);

		// toggle the status
		String newStatus = "FREE".equals(initialStatus) ? "BOOKED" : "FREE";
		bike.put(BikeSchema.Bike.PROP_STATUS, newStatus);

		// Put the updated bike
		HttpUnitTestHelper.putJson(bike, route, 200);

		// make sure status is new and the rest untouched
		JsonObject updatedBike = HttpUnitTestHelper.getUrlAsJsonObject(route, 200);
		Assert.assertEquals(name, updatedBike.getString(BikeSchema.Bike.PROP_NAME));
		Assert.assertEquals(owner, updatedBike.getString(BikeSchema.Bike.PROP_OWNER));
		Assert.assertEquals(lat, updatedBike.getDouble(BikeSchema.Bike.PROP_LATITUDE));
		Assert.assertEquals(lon, updatedBike.getDouble(BikeSchema.Bike.PROP_LONGITUDE));
		
		Assert.assertEquals(newStatus, updatedBike.getString(BikeSchema.Bike.PROP_STATUS));
	}

	//@Test
	public void testNonexistingAPI() throws TestException, IOException, SAXException {
		HttpUnitTestHelper.getUrlAsJsonObject(baseURL + "/api/foo", 404);
	}


}