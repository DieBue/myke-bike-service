package co.acoustic.content.sito;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.acoustic.content.sito.clients.WCHClient;
import co.acoustic.content.sito.cmd.AddBlockToPageCommand;
import co.acoustic.content.sito.cmd.AuthoringSearchCommand;
import co.acoustic.content.sito.cmd.BaseCommand;
import co.acoustic.content.sito.cmd.Command;
import co.acoustic.content.sito.cmd.ConfigCommand;
import co.acoustic.content.sito.cmd.CopyContentCommand;
import co.acoustic.content.sito.cmd.CreateSitesContentCommand;
import co.acoustic.content.sito.cmd.DeliverySearchCommand;
import co.acoustic.content.sito.cmd.GetAuthoringContentCommand;
import co.acoustic.content.sito.cmd.GetDeliveryContentCommand;
import co.acoustic.content.sito.cmd.GetLayoutMappingsCommand;
import co.acoustic.content.sito.cmd.GetLayoutsCommand;
import co.acoustic.content.sito.cmd.GetReferencesCommand;
import co.acoustic.content.sito.cmd.GetTypesCommand;
import co.acoustic.content.sito.cmd.HelpCommand;
import co.acoustic.content.sito.cmd.RenderContentItemCommand;
import co.acoustic.content.sito.cmd.RuntimeContext;


public class Main {

	private static final Logger LOGGER = LogManager.getLogger(Main.class);

	private static final Command HELP = new HelpCommand();
	public static final Command[] COMMANDS = new Command[] {
			HELP,
			new ConfigCommand(),
			new GetLayoutsCommand(),
			new GetLayoutMappingsCommand(),
			new GetTypesCommand(),
			new GetReferencesCommand(),
			new RenderContentItemCommand(),
			new DeliverySearchCommand(),
			new AuthoringSearchCommand(),
			new GetAuthoringContentCommand(),
			new GetDeliveryContentCommand(),
			new AddBlockToPageCommand(),
			new CopyContentCommand(),
			new CreateSitesContentCommand()
			
	};
	
	public static final HashMap<String, Command> COMMAND_MAP = new HashMap<String, Command>(); 

	static {
		for (int i=0; i<COMMANDS.length; i++) {
			Command cmd = COMMANDS[i];
			COMMAND_MAP.put(cmd.getName(), cmd);
		}
	}
	
	public static void main(String[] args) throws Exception {
		LOGGER.traceEntry(Arrays.asList(args).toString());
		HashMap<String, String> params = getParams(args);
		Config config = new Config(params.get(BaseCommand.CONFIG));
		WCHClient wchPreview = new WCHClient(config, true);
		WCHClient wch = new WCHClient(config, false);

		long start = System.currentTimeMillis();
		RuntimeContext ctx = new RuntimeContext(config, wch, wchPreview, params);

		if (args.length > 0) {
			Command cmd = COMMAND_MAP.get(args[0]);
			if (cmd == null) {
				System.out.print("unknown command: " + args[0]);
			}
			else {
				//LOGGER.info("Executing command {} with parameters:  {}.", cmd.getClass().getName(), ctx.getParams());
				System.out.println(cmd.execute(ctx));
			}
		}
		else {
			System.out.println(HELP.execute(ctx));
		}
		long end = System.currentTimeMillis();
		
		//System.out.println("Duration: " + ((end-start) / 1000) + " seconds.");
		LOGGER.traceExit();
	}

	private static HashMap<String, String> getParams(String[] args) {
		HashMap<String, String> result = new HashMap<String, String>();

		int i=1;
		while (i<args.length) {
			if (((i+1) < args.length) && args[i+1].startsWith("-")) {
				// not the last arg and next arg is a key: we encountered a flag
				result.put(args[i++], "true");
			}
			else if (((i+1) < args.length)) {
				// not the last arg and next arg is a not a key: we encountered a key/value param
				result.put(args[i++], args[i++]);
			}
			else {
				// last arg - so it is a flag 
				result.put(args[i++], "true");
			}
		}
		return result;
	}




}