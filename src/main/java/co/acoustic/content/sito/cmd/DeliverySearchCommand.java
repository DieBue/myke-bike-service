package co.acoustic.content.sito.cmd;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DeliverySearchCommand extends SearchCommand {

	private static final Logger LOGGER = LogManager.getLogger(DeliverySearchCommand.class);
	
	private static final String[] FIELDS = new String[] {"id", "name"};

	public DeliverySearchCommand() {
		super("searchDelivery", FIELDS);
	}
	
	@Override
	public String execute(RuntimeContext ctx) throws Exception {
		LOGGER.traceEntry(ctx.getParams().toString());
		validateParams(ctx);
		return format(ctx, ctx.getWch().deliverySearch(extractUrlParams(ctx)));
	}

	
	@Override
	public String getHelp() {
		return "searech delivery data";
	}
	
	

}