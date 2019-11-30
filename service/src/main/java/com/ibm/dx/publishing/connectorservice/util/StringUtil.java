/*
 * Copyright IBM Corp. 2016
 */
package com.ibm.dx.publishing.connectorservice.util;


public class StringUtil {

    @SuppressWarnings("unused")
    private static final String COPYRIGHT = "Copyright IBM Corp. 2016";

    private StringUtil() {
    }

	public static String truncateString(String str, int len) {
	    if (str == null) {
	        return null;
	    }
	    else {
	        return (str.length() < len) ? str : str.substring(0, (len-1)) + "...";
	    }
	}

	public static String truncateObject(Object o, int len) {
        if (o == null) {
            return null;
        }
        else {
            return truncateString(o.toString(), len);
        }
    }
}
