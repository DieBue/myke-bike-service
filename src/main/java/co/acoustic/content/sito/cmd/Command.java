package co.acoustic.content.sito.cmd;

import java.util.Map;

public interface Command {
	public String execute(RuntimeContext ctx) throws Exception;

	public String getName();
	public String getHelp();
	public Map<String, String> getHelpParams();
}
