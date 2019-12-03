package myke;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import myke.beans.bikes.Bike;
import myke.beans.bikes.BikeController;
import myke.beans.bikes.BikeList;
import myke.beans.common.JsonBean;
import myke.clients.AcousticContentClient;
import myke.exceptions.HttpStatusProvider;
import myke.exceptions.JsonMessageProvider;

@SuppressWarnings("deprecation")
public class Service extends AbstractVerticle {
	private static final Logger LOGGER = LogManager.getLogger(Service.class.getName());

	private static final String PARAM_USER_ID = "user_id";
	private static final String PARAM_BIKE_ID = "bike_id";

	private static final String ROUTE_API_DOC = "/*";
	private static final String ROUTE_GET_BIKES = "/api/bikes";
	private static final String ROUTE_GET_BIKE = "/api/bikes/:" + PARAM_BIKE_ID;
	private static final String ROUTE_PUT_BIKES = "/api/bikes/:" + PARAM_BIKE_ID;


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
		//HttpServerOptions options = new HttpServerOptions().setSsl(true);
		HttpServer server = vertx.createHttpServer();
		final Router router = Router.router(vertx);

		router.get(ROUTE_GET_BIKES).handler(this::handleGetBikes);
		router.get(ROUTE_GET_BIKE).handler(this::handleGetBike);
		router.put(ROUTE_PUT_BIKES).handler(BodyHandler.create());
		router.put(ROUTE_PUT_BIKES).handler(this::handlePutBike);
		router.route(ROUTE_API_DOC).handler(StaticHandler.create());
//		router.route("/*").handler(this::handleUnknown);
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

	private void handleUnknown(RoutingContext ctx) {
		LOGGER.traceEntry();
		HttpServerResponse response = ctx.response();
		response.setStatusCode(404);
		response.putHeader("content-type", "application/json");
		response.end(new JsonObject().put("message", "API not found.").encode());
		LOGGER.traceExit();
	}

	private void handleGetBikes(RoutingContext ctx) {
		LOGGER.traceEntry();
		HttpServerResponse response = ctx.response();
		HttpServerRequest request = ctx.request();
		String userId = request.params().get(PARAM_USER_ID);
		try {
			CompletableFuture<BikeList> bikeListFuture = (userId == null) ? bikeController.getFreeBikes() : bikeController.getMyBikes(userId);
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
		HttpServerRequest request = ctx.request();
		String bikeId = request.params().get(PARAM_BIKE_ID);
		JsonObject bike = ctx.getBodyAsJson();
		LOGGER.trace("Posted body: " + bike.encode());
		
		try {
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

	private void sendJsonBean(HttpServerResponse response, JsonBean bean) {
		response.setStatusCode(200);
		response.putHeader("content-type", "application/json");
		response.end(bean.getJson().encode());
	}

	private void sendError(HttpServerResponse response, Throwable th) {
		LOGGER.catching(th);
		int status = (th instanceof HttpStatusProvider) ? ((HttpStatusProvider)th).getStatusCode() : 500;
		JsonObject jsonMessage = (th instanceof JsonMessageProvider) ? ((JsonMessageProvider)th).getJsonMessage() : null;
		response.setStatusCode(status);
		if (jsonMessage != null) {
			response.headers().add("Content-Type", "application/json");
			response.end(jsonMessage.encodePrettily());
		}
		else {
			response.headers().add("Content-Type", "text/plain");
			response.end(th.getMessage());
		}
	}
}
