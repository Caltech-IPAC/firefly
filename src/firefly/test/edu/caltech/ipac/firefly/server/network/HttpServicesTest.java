package edu.caltech.ipac.firefly.server.network;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.server.RequestAgent;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.visualize.plot.CircleTest;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.logging.log4j.Level;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import static edu.caltech.ipac.firefly.server.network.HttpServices.sanitizeHeader;
import static edu.caltech.ipac.firefly.server.network.HttpServices.sanitizeHeaders;
import static edu.caltech.ipac.firefly.server.security.SsoAdapter.SSO_FRAMEWORK_NAME;
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

    @BeforeClass
    public static void init() {
        AppProperties.setProperty(SSO_FRAMEWORK_NAME, "josso");
        AppProperties.setProperty("sso.req.auth.hosts", "localhost,httpbin.org,acme.org,.ipac.caltech.edu");
        AppProperties.setProperty("sso.send.user.id", "true");
    }

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
//        input.setFile("samplePng", FileLoader.resolveFile(CircleTest.class, "imageDataWithMaskTest.png"));  // a small png file to test with.
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
            assertNotNull(method.getRequestHeader("X-User-Id"));
            assertTrue(HttpServices.isRedirected(method));
            assertEquals("redirect to www.acme.org", method.getResponseHeader("location").getValue(), "http://www.acme.org");
            return HttpServices.Status.ok();
        }));
    }

    @Test
    public void testGetWithAuth(){
        ByteArrayOutputStream results = new ByteArrayOutputStream();
        HttpServices.Status status = HttpServices.getData(input.setRequestUrl(GET_URL), HttpServices.defaultHandler(results));
        validateResults(status, results);
    }


    @Test
    public void testGetWithAuthRedirected(){
        HttpServiceInput nInput = input.setRequestUrl(REDIRECT_URL)
                .setParam("url", "https://irsa.ipac.caltech.edu/docs/help_desk.html")
                .setParam("status_code", "301");

        HttpServices.Status status = HttpServices.getData(nInput, 3, method -> {
            assertNotNull(method.getRequestHeader("X-User-Id"));
            return HttpServices.Status.ok();
        });
        assertEquals(200, status.getStatusCode());    // 200 OK
    }

    @Test
    @Ignore    // This is tested using mockbin.  It should not run normally.
    public void testFetchMaxFollow() {
        HttpServiceInput nInput = input.setRequestUrl("https://mockbin.org/bin/c8bc6283-9129-4aef-8768-1488a85cae09");
//                "https://mockbin.org/bin/8066cc72-aff6-4443-8812-f4983bcd43c8"    // setup to redirect to another bin

        HttpServices.Status status = HttpServices.getData(nInput, 1, method -> {
            fail("Should not get here");
            return HttpServices.Status.ok();
        });
        assertEquals(421, status.getStatusCode());    // 421 ERR_TOO_MANY_REDIRECTS

    }

    @Test
    public void testSanitizeHeader() {
        Header[] headers = new Header[]{
                new Header("Authorization", "Bearer abc-123"),
                new Header("AUTHORIZATION", "basic abc-123"),
                new Header("remove-new-line", "there is\r and new line\n"),
                new Header("ignore", "what ever the value is"),
        };
        assertEquals("Bearer [redacted]", sanitizeHeader(headers[0].getName(), headers[0].getValue()) );
        assertEquals("basic [redacted]", sanitizeHeader(headers[1].getName(), headers[1].getValue()) );
        assertEquals("there is and new line", sanitizeHeader(headers[2].getName(), headers[2].getValue()));
        assertEquals("what ever the value is", sanitizeHeader(headers[3].getName(), headers[3].getValue()));

        assertEquals("Authorization: Bearer [redacted], AUTHORIZATION: basic [redacted], remove-new-line: there is and new line, ignore: what ever the value is", sanitizeHeaders(headers));
    }

    @Category({TestCategory.Perf.class})
    @Test
    public void perfVsCurl() throws DataAccessException {
        /*
          Performance PRIOR to any changes
            curl fetch time: 3.862747 SECONDS
            HttpServices fetch time: 7.7860 SECONDS

          FIREFLY-1682: read/write byte array instead of single byte with buffered stream
          (3 tests in a loop)
            curl fetch time:             4.386379 SECONDS
            HttpServices fetch time:     3.7160 SECONDS

            curl fetch time:             3.914665 SECONDS
            HttpServices fetch time:     4.0240 SECONDS

            curl fetch time:             3.831989 SECONDS
            HttpServices fetch time:     3.6930 SECONDS

        */
        Logger.setLogLevel(Level.TRACE);

        String testUrl = "https://irsatest.ipac.caltech.edu/irsa-euclid/mer-catalog/search?POS=CIRCLE+267+65+0.5";
        String[] curlCmd = new String[] {
                "curl",
                "-o", "a.vot",
                "-s",
                "-w", "%{time_starttransfer} %{time_total}",
                testUrl
        };

        for(int i = 0; i<3; i++ ) {
            Logger.getLogger().debug("CURL:");
            String[] waitTotal = execCmd(curlCmd).split(" ");
            float fetchTime = Float.parseFloat(waitTotal[1]) - Float.parseFloat(waitTotal[0]);
            Logger.getLogger().debug("curl fetch time: %.6f SECONDS".formatted(fetchTime));

            Logger.getLogger().debug("HttpServices:");
            HttpServices.getData(new HttpServiceInput().setRequestUrl(testUrl), (method) -> {
                File a = new File("b.vot");
//            a.deleteOnExit();
                HttpServices.OutputStreamHandler h = Util.Try.it(() -> new HttpServices.OutputStreamHandler(a)).get();
                StopWatch.getInstance().start("HttpServices fetch time");
                h.handleResponse(method);
                StopWatch.getInstance().printLog("HttpServices fetch time");
                return HttpServices.Status.ok();
            });
        }
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
