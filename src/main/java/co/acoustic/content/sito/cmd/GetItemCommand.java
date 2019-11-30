package co.acoustic.content.sito.cmd;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.acoustic.content.sito.clients.WCHClient;

public abstract class GetItemCommand extends JsonCommand {

	private static final Logger LOGGER = LogManager.getLogger(GetItemCommand.class);
	
	private static final String[] FIELDS = new String[] {"id", "name"};
	protected static final String PARAM_ID = "-id";

	public GetItemCommand(String name) {
		super(name, FIELDS);
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
		return "Loads all types";
	}

	@Override
	public Map<String, String> getHelpParams() {
		Map<String, String> map = super.getHelpParams();
		map.put(PARAM_ID, "The id of the item");
		return map;
	}


}