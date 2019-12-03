package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;

import io.vertx.core.json.JsonObject;
import myke.Main;

public class FileAccess {
	public static JsonObject readJsonFromClasspath(String personalizationDataFileName) throws IOException {
		return new JsonObject(readFromClasspath(personalizationDataFileName));
	}

	public static String readFromClasspath(String personalizationDataFileName) throws IOException {
		StringWriter writer = new StringWriter();
		InputStream is = Main.class.getResourceAsStream(personalizationDataFileName);
		IOUtils.copy(is, writer, "UTF-8");
		return writer.toString();
	}

	public static String readFile(String fileName) throws IOException {
		return new String(Files.readAllBytes(Paths.get(fileName)), "UTF-8");
	}

	public static JsonObject readJsonObject(String fileName) throws IOException {
		return new JsonObject(readFile(fileName));
	}
}
