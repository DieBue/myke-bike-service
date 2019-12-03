package myke.beans.bikes;

public final class BikeSchema {

	public final static class Bike {
		public static final String PROP_ID = "id";
		public static final String PROP_NAME = "name";
		public static final String PROP_LATITUDE = "latitude";
		public static final String PROP_LONGITUDE = "longitude";
		public static final String PROP_STATUS = "status";
		public static final String PROP_OWNER = "owner";

		public static final String VALUE_STATUS_FREE = "free";
		public static final String VALUE_STATUS_UNAVAILABLE = "unavailable";
		public static final String VALUE_STATUS_BOOKED = "booked";
		
	}
	
	public final static class BikeList {
		public static final String PROP_ITEMS = "items";
	}
}
