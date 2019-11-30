package co.acoustic.content.sito.cmd;

import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import co.acoustic.content.sito.clients.WCHClient;
import co.acoustic.content.sito.utils.Serializer;

/**
 * @author DieterBuehler
 *
 */
public class CopyContentCommand extends BaseCommand {

	private static final Logger LOGGER = LogManager.getLogger(CopyContentCommand.class);
	
	private static final String PARAM_ID = "-id";
	private static final String PARAM_NAME = "-newName";
	
	
	public CopyContentCommand() {
		super("copyContent");
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx, PARAM_ID, PARAM_NAME);
		WCHClient wch = ctx.getWch().login();
		String sourceId = ctx.getParams().get(PARAM_ID);
		JSONObject content = wch.loadAuthoringContent(sourceId);
		String id = UUID.randomUUID().toString();
		content.put("id", id);
		content.put("name", ctx.getParams().get(PARAM_NAME));
		wch.postContent(content);
		
		return LOGGER.traceExit(Serializer.generateResult(content, getFileName(ctx)));
	}
	
	@Override
	public String getHelp() {
		return "Copies a content item and sets a new name";
	}

	@Override
	public Map<String, String> getHelpParams() {
		Map<String, String> params = super.getHelpParams();
		params.put(PARAM_NAME, "The name of the new content item to be created");
		params.put(PARAM_ID, "The id of the content item to by copied");
		return params;
	}
	
	

}