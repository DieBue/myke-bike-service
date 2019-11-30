package co.acoustic.content.sito.cmd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AuthoringSearchCommand extends SearchCommand {

	private static final Logger LOGGER = LogManager.getLogger(AuthoringSearchCommand.class);
	
	private static final String[] FIELDS = new String[] {"id", "name"};

	public AuthoringSearchCommand() {
		super("searchAuthoring", FIELDS);
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx);
		return format(ctx, ctx.getWch().login().auhtoringSearch(extractUrlParams(ctx)));
	}

	
	@Override
	public String getHelp() {
		return "search delivery data";
	}
	
	

}