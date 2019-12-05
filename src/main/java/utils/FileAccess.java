package utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;

import io.vertx.core.json.JsonObject;
import myke.Main;

/**
 * Simple helper to read JSON files from the file system or the current classpath.
 * @author DieterBuehler
 *
 */
public class FileAccess {
	public static JsonObject readJsonFromClasspath(String fileName) throws IOException {
		return new JsonObject(readFromClasspath(fileName));
	}

	public static String readFromClasspath(String fileName) throws IOException {
		StringWriter writer = new StringWriter();
		InputStream is = Main.class.getResourceAsStream(fileName);
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
