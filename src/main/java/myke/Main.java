/*******************************************************************************
 * Copyright IBM Corp. 2017
 *******************************************************************************/
package myke;

import java.io.IOException;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

public class Main {
	public static void main(final String[] args) throws IOException {
		Config config = new Config();
		
		@SuppressWarnings("deprecation")
		Vertx vertx = Vertx.vertx(new VertxOptions().setFileResolverCachingEnabled(false));
		Service service = new Service(config);
		DeploymentOptions options = new DeploymentOptions().setConfig(config.getJson());
		vertx.deployVerticle(service, options, res -> {
			if (res.succeeded()) {
				System.out.println("Service ready. Listening on port: " + config.getServerPort());
			} else {
				System.out.println("Deployment failed: " + res.cause());
			}
		});
	}
}
