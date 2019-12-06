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
import myke.beans.bikes.Bike;
import myke.beans.bikes.Bike.Status;
import myke.beans.bikes.BikeList;


/**
 * This test depends on a Acoustic Content service hosting at least two bikes with well known IDs ("be6e1fa8-fc02-47e4-993c-e259525cd474", "239421b6-b2c6-4c96-a977-e4999a1312be").
 * For offline unit testing, the Acoustic Content service should be mocked.  
 * @author DieterBuehler
 *
 */
public class MykeTest {

	private static Vertx vertx = Vertx.vertx(new VertxOptions().setBlockedThreadCheckInterval(1000*10));
	private static Service service;
	private static Config config;
	private static String baseURL;
	
	private static String BIKE_1 = "be6e1fa8-fc02-47e4-993c-e259525cd474";
	private static String BIKE_2 = "239421b6-b2c6-4c96-a977-e4999a1312be";
	private static String USER = "MykeTestUser";
	

	/**
	 * Deploy the Service verticle and wait till its ready 
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 */
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

	/**
	 * Load a specific bike and verify data
	 * @throws TestException
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void testBike() throws TestException, IOException, SAXException {
		// load a bike and verify JSON data
		JsonObject bikeJson = HttpUnitTestHelper.getUrlAsJsonObject(baseURL + "/api/bikes/be6e1fa8-fc02-47e4-993c-e259525cd474", 200);
		Assert.assertNotNull(bikeJson);
		Assert.assertEquals("Bike 1", bikeJson.getString(Bike.PROP_NAME));
		Assert.assertTrue(Arrays.asList(new String[] {Status.FREE.toString(), Status.BOOKED.toString()}).contains(bikeJson.getString(Bike.PROP_STATUS)));
		
		// validate Bike bean
		Bike bike = new Bike(bikeJson);
		Assert.assertEquals("Bike 1", bike.getName());
		Assert.assertNotNull(bike.getStatus());
	}

	/**
	 * Load bikes for a user and verify returned data
	 * @throws TestException
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void testBikes() throws TestException, IOException, SAXException {
		JsonObject bikes = HttpUnitTestHelper.getUrlAsJsonObject(baseURL + "/api/bikes/by_user?user_id=" + USER, 200);
		
		// validate the JSON
		Assert.assertNotNull(bikes);
		JsonArray ar = bikes.getJsonArray(BikeList.PROP_ITEMS);
		Assert.assertTrue(ar.size()>0);
		JsonObject bike0 = ar.getJsonObject(0);
		Assert.assertNotNull(bike0.getString(Bike.PROP_NAME));
		Assert.assertNotNull(bike0.getString(Bike.PROP_STATUS));
		
		// valdate the BikeList bean
		BikeList bikeList = new BikeList(bikes);
		Assert.assertTrue(bikeList.size()>0);
		Assert.assertNotNull(bikeList.get(0).getName());
		Assert.assertNotNull(bikeList.get(0).getStatus());
	}


	/**
	 * Update a bike and verify new data gets persisted as expected 
	 * @throws TestException
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void testBikeUpdate() throws TestException, IOException, SAXException {
		String route = baseURL + "/api/bikes/" + BIKE_1;
		
		// get the current bike
		Bike bike = new Bike(HttpUnitTestHelper.getUrlAsJsonObject(route, 200));
		Assert.assertNotNull(bike);
		
		// keep track of current properties
		Status initialStatus = bike.getStatus();
		String name = bike.getName();
		Double lat = bike.getLatitude();
		Double lon = bike.getLongitude();
		Assert.assertTrue(lat != 0);
		Assert.assertTrue(lon != 0);

		// toggle the status and owner
		Status newStatus = Status.FREE.equals(initialStatus) ? Status.BOOKED : Status.FREE;
		String newOwner = Status.FREE.equals(initialStatus) ? USER : "";
		updateStatus(bike, newStatus, USER, 200);
		
		// make sure status is new and the rest untouched
		Bike updatedBike = new Bike(HttpUnitTestHelper.getUrlAsJsonObject(route, 200));
		Assert.assertEquals(name, updatedBike.getName());
		Assert.assertEquals(newOwner, updatedBike.getOwner());
		Assert.assertEquals(lat, updatedBike.getLatitude());
		Assert.assertEquals(lon, updatedBike.getLongitude());
		Assert.assertEquals(newStatus, updatedBike.getStatus());
	}
	
	@Test
	public void testOneBikePerUserEnforcement() throws TestException, IOException, SAXException {
		String route1 = baseURL + "/api/bikes/" + BIKE_1;
		String route2 = baseURL + "/api/bikes/" + BIKE_2;
		
		// make sure user testOneBikePerUserEnforcement is not owning any bike
		BikeList bikeList = new BikeList(HttpUnitTestHelper.getUrlAsJsonObject(baseURL + "/api/bikes/by_user?user_id=" + USER, 200));
		Bike bikeToGiveBack = null;
		for (int i=0; i<bikeList.size(); i++) {
			Bike bike = bikeList.get(i);
			if (USER.equals(bike.getOwner())) {
				if (bikeToGiveBack != null) {
					Assert.fail("User " + USER + " ownes more than one bike. This should never happen");
				}
				bikeToGiveBack = bike;
			}
		}

		// in case we already own a bike, we give it back to have a defined starting point ....
		if (bikeToGiveBack != null) {
			updateStatus(bikeToGiveBack, Status.FREE, null, 200);
		}

		// now we claim bike1 - this should succeed
		Bike bike1 = new Bike(HttpUnitTestHelper.getUrlAsJsonObject(route1, 200));
		Assert.assertEquals(bike1.getStatus(), Status.FREE);
		updateStatus(bike1, Status.BOOKED, USER, 200);

		// now we try to claim bike2 - this should fail with 403
		Bike bike2 = new Bike(HttpUnitTestHelper.getUrlAsJsonObject(route2, 200));
		Assert.assertEquals(bike2.getStatus(), Status.FREE);
		updateStatus(bike2, Status.BOOKED, USER, 403);
		
		// expected final state: bike1 is booked bike 2 is free
		bike1 = new Bike(HttpUnitTestHelper.getUrlAsJsonObject(route1, 200));
		Assert.assertEquals(bike1.getStatus(), Status.BOOKED);
		bike2 = new Bike(HttpUnitTestHelper.getUrlAsJsonObject(route2, 200));
		Assert.assertEquals(bike2.getStatus(), Status.FREE);
	}

	/**
	 * Trigger a error and validate the correct error code.
	 * @throws TestException
	 * @throws IOException
	 * @throws SAXException
	 */
	@Test
	public void test404() throws TestException, IOException, SAXException {
		String route = baseURL + "/api/bikes/whatever";
		// mkae sure we get a 404
		HttpUnitTestHelper.getUrlAsJsonObject(route, 404);
	}
	
	private String updateStatus(JsonObject bikeJson, Status status, String user, int expectedResponseCode) throws IOException, SAXException, TestException {
		if (Status.FREE.equals(status)) {
			bikeJson.put(Bike.PROP_STATUS, Status.FREE.toString());
			bikeJson.put(Bike.PROP_OWNER, "");
		}
		else {
			bikeJson.put(Bike.PROP_STATUS, Status.BOOKED.toString());
			bikeJson.put(Bike.PROP_OWNER, user);
		}
		return HttpUnitTestHelper.putJson(bikeJson, baseURL + "/api/bikes/" + bikeJson.getString("id"), expectedResponseCode);
	}

	private String updateStatus(Bike bike, Status status, String user, int expectedResponseCode) throws IOException, SAXException, TestException {
		return (updateStatus(bike.getJson(), status, user, expectedResponseCode));
	}
}
