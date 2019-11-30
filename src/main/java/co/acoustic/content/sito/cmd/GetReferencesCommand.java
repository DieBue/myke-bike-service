package co.acoustic.content.sito.cmd;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import co.acoustic.content.sito.clients.WCHClient;

public class GetReferencesCommand extends JsonCommand {

	private static final Logger LOGGER = LogManager.getLogger(GetReferencesCommand.class);
	
	private static final String ID = "-id";
	private static final String DIRECTION = "-direction";
	private static final String CLASSIFICATION = "-classification";
	private static final String DIRECTION_IN = "in";
	
	private static final String[] FIELDS = new String[] {"id", "name"};

	public GetReferencesCommand() {
		super("getReferences", FIELDS);
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx, ID, DIRECTION);
		WCHClient wch = ctx.getWch().login();
		
		String id = ctx.getParams().get(ID);
		String classification = ctx.getParams().get(CLASSIFICATION);
		
		
		JSONObject result = DIRECTION_IN.equals(ctx.getParams().get(DIRECTION)) ? wch.loadIncomingReferences(id, classification) : wch.loadOutgoingReferences(id, classification); 
		
		return LOGGER.traceExit(format(ctx, result));
	}
	
	@Override
	public String getHelp() {
		return "Loads all types";
	}

	@Override
	public Map<String, String> getHelpParams() {
		Map<String, String> result = super.getHelpParams();
		result.put(ID, "The id of the item");
		result.put(DIRECTION, "Set to \"in\" or \"out\"");
		result.put(CLASSIFICATION, "The classification of the item");
		return result;
	}

}