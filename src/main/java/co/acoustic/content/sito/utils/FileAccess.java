package co.acoustic.content.sito.utils;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import co.acoustic.content.sito.Main;

public class FileAccess {
	public static JSONObject readJsonFromClasspath(String personalizationDataFileName) throws IOException, JSONException {
		return new JSONObject(readFromClasspath(personalizationDataFileName));
	}

	public static String readFromClasspath(String personalizationDataFileName) throws IOException {
		StringWriter writer = new StringWriter();
		InputStream is = Main.class.getResourceAsStream(personalizationDataFileName);
		IOUtils.copy(is, writer, "UTF-8");
		return writer.toString();
	}

	public static void writeFile(String fileName, String data) throws IOException {
		
		Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(fileName), "UTF-8"));
		try {
			out.write(data);
		} finally {
			out.close();
		}
	}

	public static String readFile(String fileName) throws IOException {
		return new String(Files.readAllBytes(Paths.get(fileName)), "UTF-8");
	}

	public static JSONObject readJsonObject(String fileName) throws IOException, JSONException {
		return new JSONObject(readFile(fileName));
	}
}
