package co.acoustic.content.sito.cmd;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.JsonUtils;
import org.json.JSONObject;

import co.acoustic.content.sito.clients.WCHClient;
import co.acoustic.content.sito.utils.JSONUtil;
import co.acoustic.content.sito.utils.Serializer;

/**
 * @author DieterBuehler
 *
 */
public class CreateSitesContentCommand extends BaseCommand {

	private static final Logger LOGGER = LogManager.getLogger(CreateSitesContentCommand.class);
	
	private static final String PARAM_ID = "-typeId";
	private static final String PARAM_ELEMENT_NAME = "-elementName";
	private static final String PARAM_NAME = "-newName";
	
	
	public CreateSitesContentCommand() {
		super("createSitesContent");
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx, PARAM_NAME, PARAM_ELEMENT_NAME);
		String typeId = (ctx.getParams().get(PARAM_ID)) != null ? ctx.getParams().get(PARAM_ID) : "21a8b4fd-0236-4187-bfea-7a94283e7b80";
		String elementName = ctx.getParams().get(PARAM_ELEMENT_NAME);
		WCHClient wch = ctx.getWch().login();
		JSONObject content = wch.loadEmptyContent(typeId);
		content.put("name", ctx.getParams().get(PARAM_NAME));
		content.put("status", "ready");
		JSONObject selection = new JSONObject().put("selection", elementName);
		content.getJSONObject("elements").getJSONObject("selected").put("value", selection);
		
		String elementTypeId = (String)JSONUtil.getMemberPath(content, "elements", elementName, "typeRef", "id");
		JSONObject element = wch.loadEmptyContent(elementTypeId);
		content.getJSONObject("elements").getJSONObject(elementName).put("value", element.getJSONObject("elements"));
		
		System.out.print(content);
		
		wch.postContent(content);
		return LOGGER.traceExit(Serializer.generateResult(content.toString(), getFileName(ctx)));
	}
	
	@Override
	public String getHelp() {
		return "Creates and empty content item of a given type";
	}

	@Override
	public Map<String, String> getHelpParams() {
		Map<String, String> params = super.getHelpParams();
		params.put(PARAM_ID, "The id of the content type");
		params.put(PARAM_ELEMENT_NAME, "The name of the element to fill");
		params.put(PARAM_NAME, "The name of the new item");
		return params;
	}
	
	

}