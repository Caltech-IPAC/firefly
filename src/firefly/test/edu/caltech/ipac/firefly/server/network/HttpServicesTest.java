package edu.caltech.ipac.firefly.server.network;

import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.visualize.plot.CircleTest;
import org.apache.commons.httpclient.methods.PutMethod;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class HttpServicesTest {

	private static String TEST_HOST_URL = "http://httpbin.org/";
	private static String GET_URL = TEST_HOST_URL + "get";
	private static String POST_URL = TEST_HOST_URL + "post";
	private static String GZIP_URL = TEST_HOST_URL + "gzip";
	private static String REDIRECT_URL = TEST_HOST_URL + "redirect-to";
	private static String PUT_URL = TEST_HOST_URL + "put";

	private HttpServiceInput input;

	@Before
	public void setUp() {
		input = new HttpServiceInput()
				.setCookie("cookie1", "cookie1_val").setCookie("cookie2", "cookie2_val")
				.setParam("param1", TEST_HOST_URL+"param1_val").setParam("param2", TEST_HOST_URL+"param2_val")
				.setHeader("Header1", "header1_val").setHeader("Header2", "header2_val");
	}

	@After
	public void tearDown() {
		input = null;
	}

//====================================================================
//  Test cases
//====================================================================

	@Test
	public void testGetData(){

		ByteArrayOutputStream results = new ByteArrayOutputStream();
		int statusCode = HttpServices.getData(GET_URL, results, input);
		validateResults(statusCode, results);
	}

	@Test
	public void testPostData(){

		ByteArrayOutputStream results = new ByteArrayOutputStream();
		int statusCode = HttpServices.postData(POST_URL, results, input);
		validateResults(statusCode, results);
	}

	@Test
	public void testPostMultiPartData(){

		// resolveFile is not implemented right.. it depends on classes residing under firefly... which may not be true.
//		input.setFile("samplePng", FileLoader.resolveFile(CircleTest.class, "imageDataWithMaskTest.png"));  // a small png file to test with.
		String relPath = CircleTest.class.getCanonicalName().replaceAll("\\.", "/").replace(CircleTest.class.getSimpleName(), "");
		File samplePng = new File("..", FileLoader.TEST_DATA_ROOT + relPath + "imageDataWithMaskTest.png");
		input.setFile("samplePng", samplePng);

		ByteArrayOutputStream results = new ByteArrayOutputStream();
		int statusCode = HttpServices.postData(POST_URL, results, input);

		validateResults(statusCode, results);

		assertNotNull("samplePng should have been uploaded as well", getProp(results.toString(), "samplePng"));
	}

	/**
	 * Using the low-level api to execute a 'put' method.
	 */
	@Test
	public void testExecMethod(){

		ByteArrayOutputStream results = new ByteArrayOutputStream();
		PutMethod put = new PutMethod(PUT_URL);
		try {
			int statusCode = HttpServices.executeMethod(put, input, results);
			validateResults(statusCode, results);
		} catch (IOException e) {
			fail("Encounter IO exception during a put request");
		}
	}

	@Test
	public void testGzipData(){

		ByteArrayOutputStream results = new ByteArrayOutputStream();
		int statusCode = HttpServices.getData(GZIP_URL, results, input);

		assertEquals("Returned status code should be 200", 200, statusCode);

		assertEquals("Returned content should be gzipped", "true", getProp(results.toString(), "gzipped"));
	}

	@Test
	public void testRedirectData(){
		input.setParam("url", GET_URL);  // redirect back to get
		ByteArrayOutputStream results = new ByteArrayOutputStream();
		int statusCode = HttpServices.getData(REDIRECT_URL, results, input);

		assertEquals("Returned status code should be 200", 200, statusCode);

		assertEquals("url should be redirected to /get now", GET_URL, getProp(results.toString(), "url"));
	}

//====================================================================
//  private
//====================================================================

	private static void validateResults(int statusCode, ByteArrayOutputStream results) {
		assertEquals("Returned status code should be 200", 200, statusCode);

		try {
			JSONObject json = (JSONObject) new JSONParser().parse(results.toString());

			assertEquals("cookie2 value should be cookie2_val", "cookie2_val", getCookie(json, "cookie2"));
			assertEquals("header1 value should be header1_val", "header1_val", getHeader(json, "Header1"));
			assertEquals("param1 value should be param1_val", TEST_HOST_URL+"param1_val", getParam(json, "param1"));

		} catch (ParseException e) {
			fail("Returned json is not parsable");
		}
	}

	private static String getParam(JSONObject json, String key) {
		try {
			JSONObject args = (JSONObject) json.get("args");
			String val = (String) args.get(key);
			if (val == null) {
				JSONObject form = (JSONObject) json.get("form");
				val = (String) form.get(key);
			}
			return String.valueOf(val);
		} catch (Exception e) {
			return "";
		}
	}

	private static String getHeader(JSONObject json, String key) {
		try {
			JSONObject headers = (JSONObject) json.get("headers");
			return String.valueOf(headers.get(key));
		} catch (Exception e) {
			return "";
		}
	}

	private static String getProp(String jsonStr, String key) {
		try {
			JSONObject json = (JSONObject) new JSONParser().parse(jsonStr.toString());
			return String.valueOf(json.get(key));
		} catch (ParseException e) {
			return "";
		}
	}

	private static String getCookie(JSONObject json, String key) {
		try {
			String cookieStr = getHeader(json, "Cookie");
			HashMap<String,String> cookies = new HashMap<>();
			Arrays.stream(cookieStr.split(";")).map((s) -> s.trim().split("="))
					.forEach((c) -> cookies.put(c[0], c.length>1 ? c[1] : null));
			return cookies.get(key);
		} catch (Exception e) {
			return "";
		}
	}

}
