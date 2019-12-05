package myke;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.StaticHandler;
import myke.beans.bikes.Bike;
import myke.beans.bikes.BikeController;
import myke.beans.bikes.BikeList;
import myke.beans.common.JsonBean;
import myke.clients.AcousticContentClient;
import myke.clients.AcousticContentSchema;
import myke.exceptions.HttpStatusProvider;
import myke.exceptions.JsonMessageProvider;
import myke.exceptions.MissingParameterException;
import myke.exceptions.ParamValueMissmatchException;

@SuppressWarnings("deprecation")
public class Service extends AbstractVerticle {
	private static final Logger LOGGER = LogManager.getLogger(Service.class.getName());

	private static final String PARAM_USER_ID = "user_id";
	private static final String PARAM_BIKE_ID = "bike_id";

	private static final String ROUTE_API_DOC = "/*";
	private static final String ROUTE_GET_BIKES_BY_USER = "/api/bikes/by_user";
	private static final String ROUTE_GET_BIKE = "/api/bikes/:" + PARAM_BIKE_ID;
	private static final String ROUTE_PUT_BIKE = "/api/bikes/:" + PARAM_BIKE_ID;


	private AcousticContentClient client;
	private BikeController bikeController;
	private final Config config;


	public Service(Config config) throws IOException {
		this.config = config;
	}

	@Override
	public void init(Vertx vertx, Context context) {
		super.init(vertx, context);
		client = new AcousticContentClient(vertx, config);
		bikeController = new BikeController(vertx, client);
	}

	public void start(Future<Void> startFuture) {
		LOGGER.traceEntry();
		HttpServer server = vertx.createHttpServer();
		final Router router = Router.router(vertx);

		router.route().handler(getCorsHandler());
		router.get(ROUTE_GET_BIKES_BY_USER).handler(this::handleGetBikesByUser);
		router.get(ROUTE_GET_BIKE).handler(this::handleGetBike);
		router.put(ROUTE_PUT_BIKE).handler(BodyHandler.create());
		router.put(ROUTE_PUT_BIKE).handler(this::handlePutBike);
		router.route(ROUTE_API_DOC).handler(StaticHandler.create());
		server.requestHandler(router::accept);
		server.listen(config.getServerPort(), res -> {
			if (res.succeeded()) {
				LOGGER.trace("completed");
				startFuture.complete();
			} else {
				LOGGER.error("bind failed", res.cause());
				startFuture.fail(res.cause());
			}
		});

		LOGGER.traceExit();
	}

	public void stop(Future<Void> endFuture) {
		LOGGER.traceEntry();
		endFuture.complete();
		LOGGER.traceExit();
	}

	private void handleGetBikesByUser(RoutingContext ctx) {
		LOGGER.traceEntry();
		HttpServerResponse response = ctx.response();
		try {
			String userId = validateParam(ctx, PARAM_USER_ID);
			CompletableFuture<BikeList> bikeListFuture = bikeController.getMyBikes(userId);
			bikeListFuture.whenComplete((bikeList, th) -> {
				if (th != null) {
					sendError(response, th);
				} else {
					sendJsonBean(response, bikeList);
				}
			});
		} catch (Exception e) {
			sendError(response, e);
		}
		LOGGER.traceExit();
	}

	private void handleGetBike(RoutingContext ctx) {
		LOGGER.traceEntry();
		HttpServerResponse response = ctx.response();
		HttpServerRequest request = ctx.request();
		String bikeId = request.params().get(PARAM_BIKE_ID);
		try {
			CompletableFuture<Bike> bikeFuture = bikeController.getBike(bikeId);
			bikeFuture.whenComplete((bike, th) -> {
				if (th != null) {
					sendError(response, th);
				} else {
					sendJsonBean(response, bike);
				}
			});
		} catch (Exception e) {
			sendError(response, e);
		}
		LOGGER.traceExit();
	}


	private void handlePutBike(RoutingContext ctx) {
		LOGGER.traceEntry();
		HttpServerResponse response = ctx.response();
		
		try {
			String bikeId = validateParam(ctx, PARAM_BIKE_ID);
			JsonObject bike = ctx.getBodyAsJson();
			validateParamValue(bikeId, bike.getString(AcousticContentSchema.Content.PROP_ID));
			LOGGER.trace("Posted body: " + bike.encode());
			CompletableFuture<Bike> bikeFuture = bikeController.updateBike(new Bike(bike));
			bikeFuture.whenComplete((updatedBike, th) -> {
				if (th != null) {
					sendError(response, th);
				} else {
					sendJsonBean(response, updatedBike);
				}
			});
		} catch (Exception e) {
			sendError(response, e);
		}
		LOGGER.traceExit();
	}
	
	private String validateParam(RoutingContext ctx, String key) throws MissingParameterException {
		String result = ctx.request().params().get(key);
		if (result == null) {
			throw new MissingParameterException(key);
		}
		return result;
	}

	private void validateParamValue(String actual, String expected) throws ParamValueMissmatchException {
		if (!expected.equals(actual)) {
			throw new ParamValueMissmatchException(expected, actual);
		}
	}

	private CorsHandler getCorsHandler() {
		Set<String> allowedHeaders = new HashSet<>();
	    allowedHeaders.add("x-requested-with");
	    allowedHeaders.add("Access-Control-Allow-Origin");
	    allowedHeaders.add("Access-Control-Allow-Method");
	    allowedHeaders.add("Access-Control-Allow-Credentials");
	    allowedHeaders.add("Access-Control-Allow-Origin");
	    allowedHeaders.add("origin");
	    allowedHeaders.add("Content-Type");
	    allowedHeaders.add("accept");

	    Set<HttpMethod> allowedMethods = new HashSet<>();
	    allowedMethods.add(HttpMethod.GET);
	    allowedMethods.add(HttpMethod.PUT);
	    allowedMethods.add(HttpMethod.OPTIONS);

	    return CorsHandler.create(".*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods);
	}

	private void sendJsonBean(HttpServerResponse response, JsonBean bean) {
		response.setStatusCode(200);
		response.putHeader("content-type", "application/json");
		response.end(bean.getJson().encode());
	}

	private void sendError(HttpServerResponse response, Throwable th) {
		LOGGER.catching(th);
		Throwable cause = (th instanceof CompletionException) ? th.getCause() : th; 
		int status = (cause instanceof HttpStatusProvider) ? ((HttpStatusProvider)cause).getStatusCode() : 500;
		JsonObject jsonMessage = (cause instanceof JsonMessageProvider) ? ((JsonMessageProvider)cause).getJsonMessage() : null;
		response.setStatusCode(status);
		if (jsonMessage != null) {
			response.headers().add("Content-Type", "application/json");
			response.end(jsonMessage.encodePrettily());
		}
		else {
			response.headers().add("Content-Type", "text/plain");
			response.end(cause.getMessage());
		}
	}
}
