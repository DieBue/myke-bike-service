package co.acoustic.content.sito.cmd;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import co.acoustic.content.sito.clients.WCHClient;
import co.acoustic.content.sito.utils.JSONUtil;

/**
 * Incomplete ....
 * @author DieterBuehler
 *
 */
public class AddBlockToPageCommand extends BaseCommand {

	private static final Logger LOGGER = LogManager.getLogger(AddBlockToPageCommand.class);
	
	private static final String PARAM_PAGE_ID = "-pageId";
	private static final String PARAM_BLOCK_ID = "-blockId";
	private static final String PARAM_ROW = "-row";
	private static final String PARAM_COLUMN = "-col";
	private static final String PARAM_SPAN = "-span";
	
	private static final String EMPTY_CELL = 
			"{\r\n" + 
			"    \"span\": {\r\n" + 
			"      \"elementType\": \"number\",\r\n" + 
			"      \"value\": 2\r\n" + 
			"    },\r\n" + 
			"    \"margin\": {\r\n" + 
			"      \"elementType\": \"group\",\r\n" + 
			"      \"value\": {\r\n" + 
			"        \"bottom\": {\r\n" + 
			"          \"elementType\": \"number\"\r\n" + 
			"        },\r\n" + 
			"        \"left\": {\r\n" + 
			"          \"elementType\": \"number\"\r\n" + 
			"        },\r\n" + 
			"        \"key\": {\r\n" + 
			"          \"elementType\": \"text\",\r\n" + 
			"          \"value\": \"2\"\r\n" + 
			"        },\r\n" + 
			"        \"top\": {\r\n" + 
			"          \"elementType\": \"number\"\r\n" + 
			"        },\r\n" + 
			"        \"right\": {\r\n" + 
			"          \"elementType\": \"number\"\r\n" + 
			"        }\r\n" + 
			"      },\r\n" + 
			"      \"typeRef\": {\r\n" + 
			"        \"id\": \"d403f72d-5383-423c-ba33-5cedd61c9224\"\r\n" + 
			"      }\r\n" + 
			"    },\r\n" + 
			"    \"key\": {\r\n" + 
			"      \"elementType\": \"text\",\r\n" + 
			"      \"value\": \"3\"\r\n" + 
			"    },\r\n" + 
			"    \"content\": {\r\n" + 
			"      \"values\": [\r\n" + 
			"        \r\n" + 
			"      ],\r\n" + 
			"      \"elementType\": \"group\",\r\n" + 
			"      \"typeRef\": {\r\n" + 
			"        \"id\": \"21a8b4fd-0236-4187-bfea-7a94283e7b80\"\r\n" + 
			"      }\r\n" + 
			"    },\r\n" + 
			"    \"padding\": {\r\n" + 
			"      \"elementType\": \"group\",\r\n" + 
			"      \"value\": {\r\n" + 
			"        \"bottom\": {\r\n" + 
			"          \"elementType\": \"number\"\r\n" + 
			"        },\r\n" + 
			"        \"left\": {\r\n" + 
			"          \"elementType\": \"number\"\r\n" + 
			"        },\r\n" + 
			"        \"key\": {\r\n" + 
			"          \"elementType\": \"text\",\r\n" + 
			"          \"value\": \"1\"\r\n" + 
			"        },\r\n" + 
			"        \"top\": {\r\n" + 
			"          \"elementType\": \"number\"\r\n" + 
			"        },\r\n" + 
			"        \"right\": {\r\n" + 
			"          \"elementType\": \"number\"\r\n" + 
			"        }\r\n" + 
			"      },\r\n" + 
			"      \"typeRef\": {\r\n" + 
			"        \"id\": \"d403f72d-5383-423c-ba33-5cedd61c9224\"\r\n" + 
			"      }\r\n" + 
			"    }\r\n" + 
			"  }\r\n" + 
			"}";
	
	
	public AddBlockToPageCommand() {
		super("addBlock");
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx, PARAM_PAGE_ID, PARAM_BLOCK_ID, PARAM_ROW, PARAM_COLUMN, PARAM_SPAN);
		WCHClient wch = ctx.getWch().login();
		String pageId = ctx.getParams().get(PARAM_PAGE_ID);
		String blockId = ctx.getParams().get(PARAM_BLOCK_ID);
		int rowIndex = Integer.parseInt(ctx.getParams().get(PARAM_ROW));
		int colIndex = Integer.parseInt(ctx.getParams().get(PARAM_COLUMN));
		int span = Integer.parseInt(ctx.getParams().get(PARAM_SPAN));
		JSONObject page = wch.loadAuthoringContent(pageId);
		JSONObject block = wch.loadAuthoringContent(blockId);
		LOGGER.trace("page: {}", page);
		JSONObject row = ((JSONArray)JSONUtil.getMemberPath(page, "elements", "rows", "values")).getJSONObject(rowIndex);
		JSONArray cells = ((JSONArray)JSONUtil.getMemberPath(row, "cells", "values"));
		final JSONObject cell = buildCell(block, span);
		if (colIndex < cells.length()) {
			JSONObject existingCell = cells.getJSONObject(colIndex);
			mergeObjects(cell, existingCell);
		}
		else {
			cells.put(cell);
		}
		LOGGER.debug("PUT: " + page);
		String result = wch.putContent(page, page.getString("id"));
		
		return LOGGER.traceExit(result);
	}

	private void mergeObjects(final JSONObject source, JSONObject target) throws JSONException {
		// clean target:
		Iterator<String> keys = target.keys();
		while (keys.hasNext()) {
			target.remove(keys.next());
		}
		
		// fill target
		keys = source.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			target.put(key, source.get(key));
		}
	}
	

	private JSONObject buildCell(JSONObject block, int span) throws JSONException {
		LOGGER.traceEntry("block: {}, span: {}", block, span);
		JSONObject newCell = new JSONObject(EMPTY_CELL);
		newCell.getJSONObject("span").put("value", span);
		newCell.getJSONObject("key").put("value", UUID.randomUUID().toString());
		JSONArray values = (JSONArray)JSONUtil.getMemberPath(newCell, "content", "values");
		values.put(block.getJSONObject("elements"));
		return LOGGER.traceExit(newCell);
	}

	@Override
	public String getHelp() {
		return "Adds a block to an existing page";
	}

	@Override
	public Map<String, String> getHelpParams() {
		Map<String, String> params = super.getHelpParams();
		params.put(PARAM_PAGE_ID, "The ID of the page to be updated");
		params.put(PARAM_BLOCK_ID, "The id the block to be copied into the page");
		params.put(PARAM_ROW, "The row index wher to put the block");
		params.put(PARAM_COLUMN, "The column index wher to put the block");
		params.put(PARAM_SPAN, "The column span value for the block");
		return params;
	}
	
	

}