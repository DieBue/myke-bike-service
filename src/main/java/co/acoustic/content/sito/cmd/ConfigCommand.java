package co.acoustic.content.sito.cmd;

import java.util.Map;

public class ConfigCommand extends BaseCommand {
	
	public ConfigCommand() {
		super("config");
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		return ctx.getConfig().toString();
	}

	@Override
	public String getHelp() {
		return "Shows the current acoustic content configuration. This configuration is read from the sito-config.json file found in the current directoy or your user home directory.";
	}

	@Override
	public Map<String, String> getHelpParams() {
		return null;
	}

}