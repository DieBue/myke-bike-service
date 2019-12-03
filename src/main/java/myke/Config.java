package myke;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonObject;
import utils.FileAccess;

public class Config  {
	private static final Logger LOGGER = LogManager.getLogger(Config.class);
	public static final String PARAM_HOST = "host";
	public static final String PARAM_TENANT_ID = "tenantId";
	public static final String PARAM_API_KEY = "apiKey";
	public static final String PARAM_SERVER_PORT = "serverPort";

	private final JsonObject config;
	private static final String CONFIG_FILE_NAME = "myke-config.json";
	
	public Config() throws IOException {
		// try the user home first ...
		String homeFileName = System.getProperty("user.home") + System.getProperty("file.separator") + CONFIG_FILE_NAME;
		File f = new File(homeFileName);
		if (f.exists()) {
			LOGGER.debug("Using config file: " + homeFileName);
			config = FileAccess.readJsonObject(homeFileName);
		}
		else {
			LOGGER.debug("Using config file: " + CONFIG_FILE_NAME);
			config = FileAccess.readJsonObject(CONFIG_FILE_NAME);
		}
	}
	
	public String getHost() {
		return config.getString(PARAM_HOST);
	}
	
	public String getTenantId() {
		return config.getString(PARAM_TENANT_ID);
	}

	public String getApKey() {
		return config.getString(PARAM_API_KEY);
	}

	public int getServerPort() {
		return config.getInteger(PARAM_SERVER_PORT);
	}

	@Override
	public String toString() {
		return config.encodePrettily();
	}
	
	public JsonObject getJson() {
		return config;
	}
	
	
}
