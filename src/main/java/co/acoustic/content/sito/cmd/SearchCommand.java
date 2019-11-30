package co.acoustic.content.sito.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class SearchCommand extends JsonCommand {

	private static final Logger LOGGER = LogManager.getLogger(SearchCommand.class);
	private static final String[] FIELDS = new String[] {"id", "name"};

	@SuppressWarnings("serial")
	private static final HashMap<String, String> SEARCH_PARAMS = new HashMap<String, String>() {
		{
			put("-q", "The corresponding lucene query param");
			put("-fq", "The corresponding lucene query param");
			put("-fl", "The corresponding lucene query param");
			put("-sort", "The corresponding lucene query param");
			put("-rows", "THe number of docuents to return");
		}
	};
	
	
	public SearchCommand(String name, String[] fields2) {
		super(name, FIELDS);
	}
	
	@Override
	public Map<String, String> getHelpParams() {
		Map<String, String> map = super.getHelpParams();
		map.putAll(SEARCH_PARAMS);
		return map;
	}
	
	protected String[] extractUrlParams(RuntimeContext ctx) {
		LOGGER.traceEntry(ctx.getParams().toString());
		ArrayList<String> list = new ArrayList<>();
		Collection<String> supportedQueryParams = SEARCH_PARAMS.keySet();
		Set<Entry<String, String>> cmdLineParams = ctx.getParams().entrySet();
		for (Entry<String, String> entry : cmdLineParams) {
			if (supportedQueryParams.contains(entry.getKey())) {
				list.add(entry.getKey().substring(1));
				list.add(entry.getValue());
			}	
		}
		String[] result = (String[])list.toArray(new String[list.size()]);
		LOGGER.traceExit(Arrays.asList(result));
		return result;
	}
	

}