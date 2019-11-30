package co.acoustic.content.sito.cmd;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class BaseCommand implements Command {

	public static final String CONFIG = "-config";
	public static final String OUTPUT_FILE_NAME = "-out";
	private final String name;
	
	public BaseCommand(String name) {
		this.name = name;
	}
	
	protected void assertParam(RuntimeContext ctx, String key) {
		if (!ctx.getParams().containsKey(key)) {
			throw new IllegalArgumentException("Missing parameter: " + key);
		}
	}
	
	protected void validateParams(RuntimeContext ctx, String ... mandatoryParams) {
		Map<String, String> supportedParams = getHelpParams();
		Set<String> params = ctx.getParams().keySet();
		for (String param : params) {
			if (!supportedParams.containsKey(param)) {
				throw new IllegalArgumentException("Unexpected parameter: " + param);
			}
		}
		if (mandatoryParams != null) {
			for (String mp : mandatoryParams) {
				if (!params.contains(mp)) {
					throw new IllegalArgumentException("Missing parameter: " + mp);
				}
			}
		}
	}
	
	@Override
	public Map<String, String> getHelpParams() {
		HashMap<String, String> result = new HashMap<>();
		result.put(OUTPUT_FILE_NAME, "This parameter can be set to store the result in a file with the given file name");
		result.put(CONFIG, "This parameter can be set to select a specific configuration from your sito-config.json file. Default config is \"default\"");
		return result;
	}

	protected String getFileName(RuntimeContext rc) {
		return rc.getParams().get(OUTPUT_FILE_NAME);
	}
	
	public String getName() {
		return name;
	}
}
