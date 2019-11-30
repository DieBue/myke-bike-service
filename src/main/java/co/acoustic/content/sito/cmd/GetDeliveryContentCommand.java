package co.acoustic.content.sito.cmd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.acoustic.content.sito.clients.WCHClient;

public class GetDeliveryContentCommand extends GetItemCommand {

	private static final Logger LOGGER = LogManager.getLogger(GetDeliveryContentCommand.class);
	
	public GetDeliveryContentCommand() {
		super("getDeliveryContent");
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx, PARAM_ID);
		WCHClient wch = ctx.getWch();
		return LOGGER.traceExit(format(ctx, wch.loadDeliveryContent(ctx.getParams().get(PARAM_ID))));
	}
	
	@Override
	public String getHelp() {
		return "Loads a content item";
	}
}