package myke;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

/**
 * Utility class to simplify HTTP unit testing
 */
public class HttpUnitTestHelper {
    private static Logger LOGGER = LogManager.getLogger();

    public static JsonObject getUrlAsJsonObject(String url, int expectedReturnCode) throws TestException, IOException, SAXException {
        LOGGER.entry(url, expectedReturnCode);
        WebResponse res = getResponse(url, expectedReturnCode);
        String str = (res != null) ? res.getText() : null;
        return LOGGER.traceExit((str != null) ? new JsonObject(str) : null);
    }

    public static WebResponse getResponse(String url, int expectedReturnCode) throws TestException, IOException, SAXException {
		LOGGER.entry(url, expectedReturnCode);

		WebResponse result = null;
		WebConversation conversation = new WebConversation();
		conversation.setExceptionsThrownOnErrorStatus(false); // this is crucial to be able to read error response bodies

		WebRequest request = new GetMethodWebRequest(url);
		try {
			result = conversation.getResponse(request);
			assertResponseCode(url, expectedReturnCode, result);
		} catch (HttpException e) { // NOSONAR
			// this path is necessary as for 40x and 50x error codes, an http exception will
			// be thrown
			assertResponseCode(url, expectedReturnCode, e.getResponseCode());
		}
		return LOGGER.traceExit(result);
	}
    


    /**
     * Executes an HTTP PUT request against the given url using the input JsonObject as payload.
     * The return code of that request will be validated against the expectedReturnCode parameter.
     *
     * @param input
     * @param url
     * @param expectedReturnCode
     * @return
     * @throws IOException
     * @throws TestException
     * @throws SAXException
     */
    public static String putJson(JsonObject input, String url, int expectedReturnCode) throws IOException, SAXException, TestException {
        final String jsonString = Json.encode(input);
        final InputStream is = new ByteArrayInputStream(jsonString.getBytes("UTF-8"));
        return putJson(is, url, expectedReturnCode);
    }
    

    /**
     * Executes an HTTP PUT request against the given url using the input stream as payload. 
     * The return code of that request will be validated against the expectedReturnCode parameter. 
     * 
     * @param is
     * @param url
     * @param expectedReturnCode
     * @return
     * @throws IOException
     * @throws SAXException
     * @throws TestException
     */
    public static String putJson(InputStream is, String url, int expectedReturnCode) throws IOException, SAXException, TestException {
        LOGGER.entry(is, url, expectedReturnCode);

        final WebRequest req = new PutMethodWebRequest(url, is, "application/json");
        final WebConversation wc = new WebConversation();

        String result;
        try {
            WebResponse response = wc.sendRequest(req);

            // make sure the return code matches the expectations
            assertResponseCode(url, expectedReturnCode, response);
            result = response.getText();
        
        } catch (HttpException e) { //NOSONAR
            assertResponseCode(url, expectedReturnCode, e.getResponseCode());
            result = e.getResponseMessage();
        }

        return LOGGER.traceExit(result);
    }

    /**
     * Checks if the response code matches the expectations.
     *
     * @param url the url that was requested
     * @param expected the expected response code
     * @param actual the actual response code
     *
     * @throws TestException if the response code does not match the expectations
     */
    private static void assertResponseCode(final String url, final int expected, final int actual) throws TestException {
        LOGGER.entry(url, expected, actual);
        if(expected != actual){
            throw new TestException("Unexpected return code for url " + url + ". Expected " + expected + ", was " + actual + ".");
        } else {
            // everything is fine
        }

        LOGGER.traceExit();
    }

    private static void assertResponseCode(final String url, final int expected, final WebResponse response) throws TestException {
        LOGGER.entry(url, expected, response);
        if (response != null) {
            int actual = response.getResponseCode();
            if(expected != actual){
                try {
                    String msg = response.getText();
                    throw new TestException(msg);
                } catch (IOException e) {
                    throw new TestException(e);
                }
            } 
        }
        else {
            throw new TestException("No response");
        }

        LOGGER.traceExit();
    }

}
