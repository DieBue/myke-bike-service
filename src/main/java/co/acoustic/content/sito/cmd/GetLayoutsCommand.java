package co.acoustic.content.sito.cmd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.acoustic.content.sito.clients.WCHClient;

public class GetLayoutsCommand extends JsonCommand {

	private static final Logger LOGGER = LogManager.getLogger(GetLayoutsCommand.class);
	
	private static final String[] FIELDS = new String[] {"id", "name", "template"};

	public GetLayoutsCommand() {
		super("getLayouts", FIELDS);
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx);
		WCHClient wch = ctx.getWch().login();
		return LOGGER.traceExit(format(ctx, wch.loadLayouts()));
	}
	
	@Override
	public String getHelp() {
		return "Loads all layouts";
	}
}