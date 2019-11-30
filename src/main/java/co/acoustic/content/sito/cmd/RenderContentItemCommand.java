package co.acoustic.content.sito.cmd;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import co.acoustic.content.sito.clients.WCHClient;

public class RenderContentItemCommand extends JsonCommand {

	private static final Logger LOGGER = LogManager.getLogger(RenderContentItemCommand.class);
	
	private static final String ID = "-id";
	
	private static final String[] FIELDS = new String[] {"id", "name"};

	public RenderContentItemCommand() {
		super("render", FIELDS);
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx, ID);
		WCHClient wch = ctx.getWch();
		String result = wch.render(ctx.getParams().get(ID)); 
		
		return LOGGER.traceExit(format(ctx, result));
	}
	
	@Override
	public String getHelp() {
		return "Loads all types";
	}

	@Override
	public Map<String, String> getHelpParams() {
		Map<String, String> result = super.getHelpParams();
		result.put(ID, "The id of the content item to be rendered");
		return result;
	}

}