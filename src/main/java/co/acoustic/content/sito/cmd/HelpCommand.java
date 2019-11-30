package co.acoustic.content.sito.cmd;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import co.acoustic.content.sito.Main;


public class HelpCommand extends BaseCommand {

	private static final String CMD = "-cmd";
	
	public HelpCommand() {
		super("help");
	}

	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		
		String selectedCommand = ctx.getParams().get(CMD);
		
		if (selectedCommand != null) {
			return getCommandHelp(Main.COMMAND_MAP.get(selectedCommand));
		}
		StringBuilder sb = new StringBuilder();
		
		sb.append("java -jar dist\\sito.jar command [params]\r\n" + 
				"Supported commands:");

		for (int i=0; i<Main.COMMANDS.length; i++) {
			Command cmd = Main.COMMANDS[i];
			sb.append("\n- ").append(cmd.getName()).append(":").append(cmd.getHelp());
		}
		sb.append("\n\nYou can get help on individual commands using java -jar dist\\\\sito.jar help -cmd [command]");
		
		return sb.toString();
	}
	
	private String getCommandHelp(Command command) {
		StringBuilder sb = new StringBuilder();
		sb.append("Description: ").append(command.getHelp());
		sb.append("\nParams:");
		Set<Entry<String, String>> entries = command.getHelpParams().entrySet();
		for (Entry<String, String> entry : entries) {
			sb.append("\n").append(entry.getKey()).append(": ").append(entry.getValue());
		}
		return sb.toString();
	}

	@Override
	public String getHelp() {
		return "Shows help information.";
	}

	@Override
	public Map<String, String> getHelpParams() {
		return Collections.singletonMap(CMD, "The cammand to show help for");
	}
}