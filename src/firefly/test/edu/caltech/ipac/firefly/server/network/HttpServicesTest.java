package edu.caltech.ipac.firefly.server.network;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.server.db.DbMonitor;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.query.SearchManager;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.table.DataGroupPart;
import edu.caltech.ipac.visualize.plot.CircleTest;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.logging.log4j.Level;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static edu.caltech.ipac.firefly.TestUtil.logMemUsage;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.*;

public class HttpServicesTest extends ConfigTest {

	private static String TEST_HOST_URL = "https://httpbin.org/";
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

		setupServerContext(null);
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
		HttpServices.Status status = HttpServices.getData(input.setRequestUrl(GET_URL), results);
		validateResults(status, results);
	}

	@Test
	public void testPostData(){

		ByteArrayOutputStream results = new ByteArrayOutputStream();
		HttpServices.Status status = HttpServices.postData(input.setRequestUrl(POST_URL), results);
		validateResults(status, results);
	}

	@Test
	public void testPostMultiPartData(){

		// resolveFile is not implemented right.. it depends on classes residing under firefly... which may not be true.
//		input.setFile("samplePng", FileLoader.resolveFile(CircleTest.class, "imageDataWithMaskTest.png"));  // a small png file to test with.
		String relPath = CircleTest.class.getCanonicalName().replaceAll("\\.", "/").replace(CircleTest.class.getSimpleName(), "");
		File samplePng = new File("..", FileLoader.TEST_DATA_ROOT + relPath + "imageDataWithMaskTest.png");
		input.setFile("samplePng", samplePng);

		ByteArrayOutputStream results = new ByteArrayOutputStream();
		HttpServices.Status status = HttpServices.postData(input.setRequestUrl(POST_URL), results);

		validateResults(status, results);

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
			HttpServices.Status status = HttpServices.executeMethod(put, input, results);
			validateResults(status, results);
		} catch (IOException e) {
			fail("Encounter IO exception during a put request");
		}
	}

	@Test
	public void testGzipData(){

		ByteArrayOutputStream results = new ByteArrayOutputStream();
		HttpServices.Status status = HttpServices.getData(input.setRequestUrl(GZIP_URL), results);

		assertFalse("Has error", status.isError());

		assertEquals("Returned content should be gzipped", "true", getProp(results.toString(), "gzipped"));
	}

	@Test
	public void testRedirectData(){
		input.setParam("url", GET_URL);  // redirect back to get
		ByteArrayOutputStream results = new ByteArrayOutputStream();
		HttpServices.Status status = HttpServices.getData(input.setRequestUrl(REDIRECT_URL), results);

		assertFalse("Has error", status.isError());

		assertEquals("url should be redirected to /get now", GET_URL, getProp(results.toString(), "url"));
	}

	@Test
	public void testFollowRedirect(){
		HttpServiceInput nInput = input.setRequestUrl(REDIRECT_URL)
										.setParam("url", "http://www.acme.org")
										.setParam("status_code", "301");

		HttpServices.Status status = HttpServices.getData(nInput, new ByteArrayOutputStream());
		assertFalse(status.isRedirected());

		HttpServices.getData(nInput.setFollowRedirect(false), (method -> {
			assertTrue(HttpServices.isRedirected(method));
			assertEquals("redirect to www.acme.org", method.getResponseHeader("location").getValue(), "http://www.acme.org");
			return HttpServices.Status.ok();
		}));
	}

	@Test
	public void testGetWithAuth(){
		ByteArrayOutputStream results = new ByteArrayOutputStream();
		HttpServices.Status status = HttpServices.getWithAuth(input.setRequestUrl(GET_URL), HttpServices.defaultHandler(results));
		validateResults(status, results);
	}


	@Test
	public void testGetWithAuthRedirected(){
		HttpServiceInput nInput = input.setRequestUrl(REDIRECT_URL)
				.setParam("url", "https://irsa.ipac.caltech.edu/docs/help_desk.html")
				.setParam("status_code", "301");

		HttpServices.Status status = HttpServices.getWithAuth(nInput, 3, method -> HttpServices.Status.ok());
		assertEquals(200, status.getStatusCode());	// 200 OK
	}

	@Test
	@Ignore	// This is tested using mockbin.  It should not run normally.
	public void testFetchMaxFollow(){
		HttpServiceInput nInput = input.setRequestUrl("https://mockbin.org/bin/c8bc6283-9129-4aef-8768-1488a85cae09");
//				"https://mockbin.org/bin/8066cc72-aff6-4443-8812-f4983bcd43c8"	// setup to redirect to another bin

		HttpServices.Status status = HttpServices.getWithAuth(nInput, 1, method -> {
			fail("Should not get here");
			return HttpServices.Status.ok();
		});
		assertEquals(421, status.getStatusCode());	// 421 ERR_TOO_MANY_REDIRECTS

	}

	@Category({TestCategory.Perf.class})
	@Test
	public void perfVsCurl() throws DataAccessException {
        /*
        */
		Logger.setLogLevel(Level.TRACE);

		String testUrl = "https://irsatest.ipac.caltech.edu/irsa-euclid/mer-catalog/search?POS=CIRCLE+267+65+0.5";
		String[] curlCmd = new String[] {
				"curl",
				"-o", "a.vot",
				"-s",
				"-w", "\\nWait Time: %{time_starttransfer}\\nTotal time: %{time_total}\\n",
				testUrl
		};

		Logger.getLogger().debug("CURL:");
		String curlResults = execCmd(curlCmd);
		Logger.getLogger().debug(curlResults);

		Logger.getLogger().debug("HttpServices:");
		StopWatch.getInstance().start("Total Time");
		StopWatch.getInstance().start("Wait Time");
		HttpServices.getData(new HttpServiceInput().setRequestUrl(testUrl), (method) -> {
			StopWatch.getInstance().printLog("Wait Time");
			File a = new File("b.vot");
//			a.deleteOnExit();
			HttpServices.OutputStreamHandler h = Util.Try.it(() -> new HttpServices.OutputStreamHandler(a)).get();
			StopWatch.getInstance().start("Fetch Time");
			h.handleResponse(method);
			StopWatch.getInstance().printLog("Fetch Time");
			return HttpServices.Status.ok();
		});
		StopWatch.getInstance().printLog("Total Time");
	}

//====================================================================
//  private
//====================================================================

	private static String execCmd(String[] cmd) {
		StringBuilder out = new StringBuilder();
		try {
			Process process = Runtime.getRuntime().exec(cmd);
			try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
				reader.lines().forEach(line -> out.append(line).append("\n"));
			}
			process.waitFor();
		} catch (Exception ignored) {}
		return out.toString();
	}

	private static void validateResults(HttpServices.Status status, ByteArrayOutputStream results) {
		assertFalse("Has error", status.isError());

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
