package co.acoustic.content.sito.cmd;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import co.acoustic.content.sito.clients.WCHClient;
import co.acoustic.content.sito.utils.JSONUtil;

/**
 * Incomplete ....
 * @author DieterBuehler
 *
 */
public class CopyTypeCommand extends BaseCommand {

	private static final Logger LOGGER = LogManager.getLogger(CopyTypeCommand.class);
	
	private static final String PARAM_ID = "-id";
	private static final String PARAM_NAME = "-newName";
	
	
	public CopyTypeCommand() {
		super("copyType");
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx, PARAM_ID, PARAM_NAME);
		WCHClient wch = ctx.getWch().login();
		String sourceTypeId = ctx.getParams().get(PARAM_ID);
		JSONObject type = wch.loadType(sourceTypeId);
		JSONArray mappings = wch.loadLayoutMappings().getJSONArray("items");
		for (int i=0; i<mappings.length(); i++) {
			JSONObject mapping = mappings.getJSONObject(i);
			String typeId = (String)JSONUtil.getMemberPath(mapping, "type", "id");
			if (sourceTypeId.equals(typeId)) {
				copyType(wch, type, mapping);
			}
		}
		
		
		return LOGGER.traceExit("done");
	}
	
	private void copyType(WCHClient wch, JSONObject type, JSONObject mapping) {
		JSONArray layouts = new JSONArray();
		//JSONObject layout = wch.loadLayout(id)
	}

	@Override
	public String getHelp() {
		return "Copies a type together with its associated layout and temalate";
	}

	@Override
	public Map<String, String> getHelpParams() {
		Map<String, String> params = super.getHelpParams();
		params.put(PARAM_NAME, "The name of the new type to be created");
		params.put(PARAM_ID, "The id of the type to by copied");
		return params;
	}
	
	

}