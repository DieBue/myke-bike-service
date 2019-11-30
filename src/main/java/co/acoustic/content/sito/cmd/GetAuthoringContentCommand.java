package co.acoustic.content.sito.cmd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.acoustic.content.sito.clients.WCHClient;

public class GetAuthoringContentCommand extends GetItemCommand {

	private static final Logger LOGGER = LogManager.getLogger(GetAuthoringContentCommand.class);
	
	public GetAuthoringContentCommand() {
		super("getAuthoringContent");
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx, PARAM_ID);
		WCHClient wch = ctx.getWch().login();
		return LOGGER.traceExit(format(ctx, wch.loadAuthoringContent(ctx.getParams().get(PARAM_ID))));
	}
	
	@Override
	public String getHelp() {
		return "Loads a content item";
	}
}