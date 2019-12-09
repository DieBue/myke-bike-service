package myke.clients;

/**
 * Maps the Acoustic Content JSON Schema to Java Strings
 * @author DieterBuehler
 *
 */
public class AcousticContentSchema {
	/**
	 * Search API
	 */
	public static final class Search {
		public static final String PARAM_QUERY = "q";
		public static final String PARAM_FILTER = "fq";
		public static final String PARAM_FORMAT = "fl";
		public static final String PARAM_ROWS = "rows";
		public static final String PARAM_SEED = "ts";
		public static final String VALUE_FORMAT_DOCUMENT = "document:[json]";
	}

	/**
	 * Search result schema constants
	 */
	public static final class SearchResult {
		public static final String PROP_DOCUMENTS = "documents";
		public static final String PROP_DOCUMENT = "document";
		public static final String PROP_NUM_FOUND = "numFound";
	}

	/**
	 * Content schema constants
	 */
	public static final class Content {
		public static final String PROP_ID = "id";
		public static final String PROP_ELEMENTS = "elements";
		public static final String PROP_VALUE = "value";
		public static final String PROP_SELECTION = "selection";
		public static final String PROP_LATITUDE = "latitude";
		public static final String PROP_LONGITUDE = "longitude";
		public static final String PROP_TAGS = "tags";
		
		public static final String ELEMENT_NAME = "bikeName";
		public static final String ELEMENT_LOCATION = "location";
		public static final String ELEMENT_OWNER = "owner";
		public static final String ELEMENT_BOOKING_DATE = "bookingDate";
		public static final String ELEMENT_STATUS = "status";
	}

}
