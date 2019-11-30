package com.ibm.dx.test.mocks;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Convenience class to provide access to input streams for files located in the
 * test/resources/test/publishing/mocks/verticles/helpers.
 * 
 * @author buehlerd
 *
 */
public class MockData {

    private static Logger LOGGER = LogManager.getLogger();

    public static final MockData INSTANCE = new MockData();

    private MockData() {
    }

    public InputStream get(String name) {
        LOGGER.entry(name);
        InputStream result = null;
        try {
            result = MockData.class.getResourceAsStream(name);
        }
        catch (Exception e) {
            LOGGER.catching(e);
        }
        LOGGER.traceExit(result);
        return result;
    }

    /**
     * Throws a java.io.FileNotFoundException if resource cannot be found
     */
    public String getAsString(String name) throws IOException {
        StringWriter writer = new StringWriter();
        InputStream is = get(name);
        if (is == null) {
            throw new FileNotFoundException(name);
        }
        IOUtils.copy(is, writer, "UTF-8");
        return writer.toString();
    }
    
}
