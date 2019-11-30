package co.acoustic.content.sito;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import co.acoustic.content.sito.utils.FileAccess;

public class Config {
	private static final Logger LOGGER = LogManager.getLogger(Config.class);

	public static final String PROCESSOR_DEFAULT_OUTPUT_FILE_NAME = "defaultOutputFileName";
	
	public static final String API_URL = "apiUrl";
	public static final String PREVIEW_API_URL = "previewApiUrl";
	public static final String USER_NAME = "userName";
	public static final String PASSWORD = "password";
	public static final String IS_TRUSTED = "isTrusted";

	private final JSONObject config;
	private static final String DEFAULT_CONFIG_NAME = "default";
	private final String name;
	
	private static final String CONFIG_FILE_NAME = "sito-config.json";
	
	public Config(final String n) throws IOException, JSONException {
		name = (n != null) ? n : DEFAULT_CONFIG_NAME;
		// try the user home first ...
		String homeFileName = System.getProperty("user.home") + System.getProperty("file.separator") + CONFIG_FILE_NAME;
		File f = new File(homeFileName);
		if (f.exists()) {
			LOGGER.debug("Using config file: " + homeFileName);
			config = FileAccess.readJsonObject(homeFileName).getJSONObject(name);
		}
		else {
			LOGGER.debug("Using config file: " + CONFIG_FILE_NAME);
			config = FileAccess.readJsonObject(CONFIG_FILE_NAME).getJSONObject(name);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isTrustedHost() throws JSONException {
		return config.getBoolean(IS_TRUSTED);
	}

	public String getWchApiURL() throws JSONException {
		return config.getString(API_URL);
	}

	public String getWebRootURL() throws JSONException {
		return config.getString(API_URL).replace("/api/", "/");
	}
	
	public String getWchPreviewApiURL() throws JSONException {
		return config.getString(PREVIEW_API_URL);
	}
	
	public String getWchUserName() throws JSONException {
		return config.getString(USER_NAME);
	}
	
	public String getWchPassword() throws JSONException {
		return config.getString(PASSWORD);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("config:\n");
		builder.append(config.toString());
		return builder.toString();
	}
	
	
}
