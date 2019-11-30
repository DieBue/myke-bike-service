package co.acoustic.content.sito.cmd;

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import co.acoustic.content.sito.clients.WCHClient;
import co.acoustic.content.sito.utils.JSONUtil;

public class GetLayoutMappingsCommand extends JsonCommand {

	private static final Logger LOGGER = LogManager.getLogger(GetLayoutMappingsCommand.class);

	private static final String[] FIELDS = new String[] { "id", "name" };

	public GetLayoutMappingsCommand() {
		super("getLayoutMappings", FIELDS);
	}

	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx);
		WCHClient wch = ctx.getWch().login();

		JSONObject result = new JSONObject();
		JSONArray resultMappings = new JSONArray();
		result.put("mappings", resultMappings);

		JSONArray layouts = wch.loadLayouts().getJSONArray("items");
		;
		JSONArray mappings = wch.loadLayoutMappings().getJSONArray("items");
		JSONArray types = wch.loadTypes().getJSONArray("items");
		JSONArray deliveryAssets = wch.deliverySearch("q", "classification:asset", "fl", "name,id,path,resource,url", "rows", "10000").getJSONArray("documents");
		JSONArray authoringAssets = wch.deliverySearch("q", "classification:asset", "fl", "name,id,path,resource", "rows", "10000").getJSONArray("documents");
		HashMap<String, JSONObject> typeMap = toMap("typeMap", types, "id");
		HashMap<String, JSONObject> layoutMap = toMap("layoutMap", layouts, "id");
		HashMap<String, JSONObject> layoutMappingMap = toMap("layoutMappingMap", mappings, "type", "id");
		HashMap<String, JSONObject> deliveryAssetMap = toMap("deliveryAssetMap", deliveryAssets, "path");
		HashMap<String, JSONObject> authoringAssetMap = toMap("authoringAssetMap", authoringAssets, "path");

		boolean valid = true;

		for (String type : layoutMappingMap.keySet()) {
			valid = valid && handleType(type, typeMap, layoutMap, layoutMappingMap, deliveryAssetMap, authoringAssetMap, valid, resultMappings);
		}

		result.put("valid", valid);
		
		return LOGGER.traceExit(format(ctx, getResultSring(ctx, result)));
	}

	private String getResultSring(RuntimeContext ctx, JSONObject jsonResult) throws JSONException {
		final String result;
		if ("full".equals(getFormat(ctx))) {
			result = jsonResult.toString(2);
		}
		else {
			StringBuilder sb = new StringBuilder();
			sb.append("[Type Name] --> [Layout ID] --> [Template Path]\n");
			JSONArray ar = jsonResult.getJSONArray("mappings");
			for (int i=0; i<ar.length(); i++) {
				JSONObject mapping = ar.getJSONObject(i);
				LOGGER.trace("mapping: {}", mapping);
				sb.append(mapping.getString("typeName")).append(" --> ");
				sb.append(mapping.getString("layoutId")).append(" --> ");
				
				sb.append(ctx.getConfig().getWebRootURL() + JSONUtil.getMemberPath(mapping, "validatedAsset", "path"));
				sb.append("\n");
			}
			result = sb.toString();
		}
		return result;
	}

	private boolean handleType(String type, HashMap<String, JSONObject> typeMap, HashMap<String, JSONObject> layoutMap, HashMap<String, JSONObject> layoutMappingMap, HashMap<String, JSONObject> deliveryAssetMap,
			HashMap<String, JSONObject> authoringAssetMap, boolean valid, JSONArray result) throws JSONException {
		LOGGER.traceEntry(type);
		JSONObject json = new JSONObject();
		result.put(json);
		json.put("id", layoutMappingMap.get(type).getString("id"));
		json.put("typeId", type);
		json.put("typeName", typeMap.get(type).getString("name"));
		JSONArray mappingsArray = layoutMappingMap.get(type).getJSONArray("mappings");
		if ((mappingsArray != null) && (mappingsArray.length() > 0)) {
			String layoutId = (String) JSONUtil.getMemberPath(mappingsArray.getJSONObject(0), "defaultLayout", "id");
			json.put("layoutId", layoutId);
			json.put("layoutName", layoutMap.get(layoutId).getString("name"));
			String template = layoutMap.get(layoutId).getString("template");
			json.put("template", template);
			JSONObject validatedAsset = getValidatedAsset(deliveryAssetMap, authoringAssetMap, template);
			if (!validatedAsset.getBoolean("valid")) {
				valid = false;
			}
			json.put("validatedAsset", validatedAsset);
		}
		return LOGGER.traceExit(valid);
	}

	private static JSONObject getValidatedAsset(HashMap<String, JSONObject> deliveryAssetMap, HashMap<String, JSONObject> authoringAssetMap, String template) throws JSONException {
		JSONObject del = deliveryAssetMap.get(template);
		JSONObject auth = authoringAssetMap.get(template);

		if (del != null) {
			if (del.has("duplicate")) {
				return new JSONObject().put("duplicate", del).put("valid", false);
			}
			if (!del.getString("id").equals(auth.getString("id"))) {
				return new JSONObject().put("id missmatch", "auth: " + auth.getString("id") + " del: " + del.getString("id")).put("valid", false);
			}
			if (!del.getString("path").equals(auth.getString("path"))) {
				return new JSONObject().put("path missmatch", "auth: " + auth.getString("path") + " del: " + del.getString("path")).put("valid", false);
			}
			if (!del.getString("resource").equals(auth.getString("resource"))) {
				JSONObject res = new JSONObject().put("resource missmatch", "auth: " + auth.getString("resource") + " del: " + del.getString("resource")).put("valid", false);
				return res;
			}
		} else {
			JSONObject res = new JSONObject().put("missing template on delivery", template).put("valid", false);
			return res;
		}

		return del.put("valid", true);

	}

	public static HashMap<String, JSONObject> toMap(String msg, JSONArray input, String... fieldNames) throws JSONException {
		HashMap<String, JSONObject> result = null;
		if ((input != null) && (input.length() > 0) && (fieldNames != null)) {
			result = new HashMap<>(input.length());
			for (int i = 0; i < input.length(); i++) {
				JSONObject val = input.getJSONObject(i);
				// String key = val.getString(fieldName);
				String key = (String) JSONUtil.getMemberPath(val, fieldNames);

				if ((key != null) && (!key.trim().isEmpty())) {
					JSONObject current = result.get(key);
					if (current != null) {
						current.put("duplicate", val);
					} else {
						result.put(key, val);
					}
				}
			}
		}
		return result;
	}

	@Override
	public String getHelp() {
		return "Loads all effective layout mappings";
	}

}