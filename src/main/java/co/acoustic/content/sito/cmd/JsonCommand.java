package co.acoustic.content.sito.cmd;

import java.io.IOException;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import co.acoustic.content.sito.utils.Serializer;

public abstract class JsonCommand extends BaseCommand {
	
	/**
	 * The name of the mail template / content type to be cloned [mandatory]
	 */
	private static final String FORMAT = "-format";
	private static final String FORMAT_FULL = "full";

	private final String[] fields;

	protected JsonCommand(String name, String[] fields) {
		super(name);
		this.fields = fields;
	}
	
	
	protected String format(RuntimeContext rc, JSONObject json) {
		try {
			return Serializer.generateResult(json, getFileName(rc), getFields(rc));
		} catch (IOException e) {
			return e.getMessage();
		}
	}
	
	protected String format(RuntimeContext rc, String str) {
		try {
			return Serializer.generateResult(str, getFileName(rc));
		} catch (IOException e) {
			return e.getMessage();
		}
	}

	protected String format(RuntimeContext rc, JSONArray json) {
		try {
			return Serializer.generateResult(json, getFileName(rc), getFields(rc));
		} catch (IOException e) {
			return e.getMessage();
		}
	}
	
	protected String getFormat(RuntimeContext ctx) {
		return ctx.getParams().get(FORMAT);
	}
	
	private String[] getFields(RuntimeContext ctx) {
		String format = ctx.getParams().get(FORMAT);
		return (FORMAT_FULL.equals(format)) ? null : fields;
	}
	
	public Map<String, String> getHelpParams() {
		Map<String, String> result = super.getHelpParams();
		result.put(FORMAT, "You can set this param to control the output format of the result. "
				+ "Supported value are:  " + FORMAT_FULL + " default");
		return result;
	}

}