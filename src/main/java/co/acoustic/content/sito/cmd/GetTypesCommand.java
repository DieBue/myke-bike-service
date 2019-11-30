package co.acoustic.content.sito.cmd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.acoustic.content.sito.clients.WCHClient;

public class GetTypesCommand extends JsonCommand {

	private static final Logger LOGGER = LogManager.getLogger(GetTypesCommand.class);
	
	private static final String[] FIELDS = new String[] {"id", "name"};

	public GetTypesCommand() {
		super("getTypes", FIELDS);
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx);
		WCHClient wch = ctx.getWch().login();
		return LOGGER.traceExit(format(ctx, wch.loadTypes()));
	}
	
	@Override
	public String getHelp() {
		return "Loads all types";
	}

}