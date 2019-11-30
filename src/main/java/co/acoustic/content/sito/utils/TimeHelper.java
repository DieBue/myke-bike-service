package co.acoustic.content.sito.utils;

import javax.xml.bind.DatatypeConverter;

public class TimeHelper {
	public static final long parse(String time) {
		return DatatypeConverter.parseDateTime(time).getTimeInMillis();
	}
}
