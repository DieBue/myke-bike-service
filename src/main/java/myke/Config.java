package myke;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.vertx.core.json.JsonObject;
import utils.FileAccess;

/**
 * The configuration JSON file used by the Myke service. The data is loaded from the <code>myke-config.json</code>
 * located in the users home directory. If this file does not exist, the file is loaded from the current working directory.
 *  
 * @author DieterBuehler
 *
 */
public class Config  {
	private static final Logger LOGGER = LogManager.getLogger(Config.class);

	// The name of the Acoustic Content Host serving the bike data. 
	public static final String PARAM_HOST = "host";

	// The ID of the Acoustic Content tenant serving the bike data. 
	public static final String PARAM_TENANT_ID = "tenantId";

	// The Acoustic Content API key used to authenticate to Acoustic Content. 
	public static final String PARAM_API_KEY = "apiKey";

	// The port number the Myke service is listening to
	public static final String PARAM_SERVER_PORT = "serverPort";

	private final JsonObject config;
	private static final String CONFIG_FILE_NAME = "myke-config.json";
	
	/**
	 * Loads the configuration and wraps it in a Config object.
	 * @throws IOException
	 */
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
	
	/**
	 * @return The name of the Acoustic Content Host serving the bike data.
	 */
	public String getHost() {
		return config.getString(PARAM_HOST);
	}
	
	/**
	 * @return The ID of the Acoustic Content tenant serving the bike data.
	 */
	public String getTenantId() {
		return config.getString(PARAM_TENANT_ID);
	}

	/**
	 * @return The Acoustic Content API key used to authenticate to Acoustic Content.
	 */
	public String getApKey() {
		return config.getString(PARAM_API_KEY);
	}

	/**
	 * @return The port number the Myke service is listening to
	 */
	public int getServerPort() {
		return config.getInteger(PARAM_SERVER_PORT);
	}

	@Override
	public String toString() {
		return config.encodePrettily();
	}
	
	/**
	 * @return The configuration as JSON data.
	 */
	public JsonObject getJson() {
		return config;
	}
	
	
}
