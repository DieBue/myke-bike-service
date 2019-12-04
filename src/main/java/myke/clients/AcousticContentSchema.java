package myke.clients;

public class AcousticContentSchema {
	public static final class Search {
		public static final String FIELD_QUERY = "q";
		public static final String FIELD_FILTER = "fq";
		public static final String FIELD_FORMAT = "fl";
		public static final String FIELD_SEED = "ts";
		public static final String VALUE_FORMAT_DOCUMENT = "document:[json]";
	}

	public static final class SearchResult {
		public static final String PROP_DOCUMENTS = "documents";
		public static final String PROP_DOCUMENT = "document";
	}

	public static final class Content {
		public static final String PROP_ID = "id";
		public static final String PROP_ELEMENTS = "elements";
		public static final String PROP_VALUE = "value";
		public static final String PROP_SELECTION = "selection";
		public static final String PROP_LATITUDE = "latitude";
		public static final String PROP_LONGITUDE = "longitude";
		
		public static final String ELEMENT_NAME = "bikeName";
		public static final String ELEMENT_LOCATION = "location";
		public static final String ELEMENT_OWNER = "owner";
		public static final String ELEMENT_BOOKING_DATE = "bookingDate";
		public static final String ELEMENT_STATUS = "status";
	}

}
