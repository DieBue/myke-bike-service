package co.acoustic.content.sito.cmd;

import java.util.HashMap;

import co.acoustic.content.sito.Config;
import co.acoustic.content.sito.clients.WCHClient;

public class RuntimeContext {
	private final Config config;
	private final WCHClient wch;
	private final WCHClient wchPreview;
	private final HashMap<String, String> params;

	public RuntimeContext(Config config, WCHClient wch, WCHClient wchPreview, HashMap<String, String> params) {
		this.config = config;
		this.wch = wch;
		this.wchPreview = wchPreview;
		this.params = params;
	}

	public Config getConfig() {
		return config;
	}

	public WCHClient getWch() {
		return wch;
	}

	public WCHClient getWchPreview() {
		return wchPreview;
	}

	public HashMap<String, String> getParams() {
		return params;
	}

	public boolean isEnabled(String paramName) {
		return Boolean.parseBoolean(params.get(paramName));
	}

}
