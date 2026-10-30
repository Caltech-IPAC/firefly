package edu.caltech.ipac.firefly.server.network;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.*;
import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.core.Util;
import edu.caltech.ipac.firefly.server.query.DataAccessException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.util.serialization.Serializer;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.Property;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPOutputStream;

import static edu.caltech.ipac.firefly.server.network.HttpServices.*;
import static org.junit.Assert.*;

/**
 * Tests HttpServices against a server running in this JVM, which echoes the request back as JSON
 * the way httpbin.org does.
 */
public class HttpServicesTest extends ConfigTest {

    private static final String PARAM1_VAL = "a b&c=d/param1_val";       // needs encoding

    private HttpServer server;
    private String baseUrl;
    private HttpServiceInput input;

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/echo", HttpServicesTest::echo);
        server.createContext("/gzip", HttpServicesTest::gzip);
        server.createContext("/redirect-to", HttpServicesTest::redirectTo);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        input = new HttpServiceInput()
                .setCookie("cookie1", "cookie1_val").setCookie("cookie2", "cookie2_val")
                .setParam("param1", PARAM1_VAL).setParam("param2", "param2_val")
                .setHeader("Header1", "header1_val").setHeader("Header2", "header2_val");
    }

    @After
    public void tearDown() {
        server.stop(0);
        input = null;
    }

//====================================================================
//  Test cases
//====================================================================

    @Test
    public void testGetData(){

        ByteArrayOutputStream results = new ByteArrayOutputStream();
        HttpServices.Status status = HttpServices.getData(input.setRequestUrl(baseUrl + "/echo"), results);
        validateResults(status, results);
        assertEquals("GET", toJson(results).path("method").asText());
    }

    @Test
    public void testPostData(){

        ByteArrayOutputStream results = new ByteArrayOutputStream();
        HttpServices.Status status = HttpServices.postData(input.setRequestUrl(baseUrl + "/echo"), results);
        validateResults(status, results);
        assertEquals("POST", toJson(results).path("method").asText());
    }

    @Test
    public void testPostMultiPartData() throws IOException {

        File sampleFile = File.createTempFile("HttpServicesTest", ".txt");
        sampleFile.deleteOnExit();
        Files.writeString(sampleFile.toPath(), "sample file content");
        input.setFile("sampleFile", sampleFile);

        ByteArrayOutputStream results = new ByteArrayOutputStream();
        HttpServices.Status status = HttpServices.postData(input.setRequestUrl(baseUrl + "/echo"), results);

        assertFalse("Has error", status.isError());
        JsonNode json = toJson(results);
        assertTrue("should be a multipart request", json.path("contentType").asText().startsWith("multipart/form-data"));
        String body = json.path("body").asText();
        assertTrue("param1 should be sent as a part", body.contains("name=\"param1\""));
        assertTrue("sampleFile should have been uploaded as well", body.contains("name=\"sampleFile\""));
        assertTrue("sampleFile content should be uploaded", body.contains("sample file content"));
    }

    /**
     * Using the low-level api to execute a 'put' method.
     */
    @Test
    public void testExecMethod(){

        ByteArrayOutputStream results = new ByteArrayOutputStream();
        PutMethod put = new PutMethod(baseUrl + "/echo");
        try {
            HttpServices.Status status = HttpServices.executeMethod(put, input, results);
            validateResults(status, results);
            assertEquals("PUT", toJson(results).path("method").asText());
        } catch (IOException e) {
            fail("Encounter IO exception during a put request");
        }
    }

    @Test
    public void testGzipData(){

        ByteArrayOutputStream results = new ByteArrayOutputStream();
        HttpServices.Status status = HttpServices.getData(input.setRequestUrl(baseUrl + "/gzip"), results);

        assertFalse("Has error", status.isError());

        assertTrue("Returned content should be gzipped", toJson(results).path("gzipped").asBoolean());
    }

    @Test
    public void testRedirectData(){
        input.setParam("url", baseUrl + "/echo");  // redirect back to echo
        ByteArrayOutputStream results = new ByteArrayOutputStream();
        HttpServices.Status status = HttpServices.getData(input.setRequestUrl(baseUrl + "/redirect-to"), results);

        assertFalse("Has error", status.isError());

        assertEquals("url should be redirected to /echo now", "/echo", toJson(results).path("path").asText());
    }

    @Test
    public void testFollowRedirect(){
        HttpServiceInput nInput = input.setRequestUrl(baseUrl + "/redirect-to")
                                        .setParam("url", baseUrl + "/echo")
                                        .setParam("status_code", "301");

        HttpServices.Status status = HttpServices.getData(nInput, new ByteArrayOutputStream());
        assertFalse(status.isRedirected());

        HttpServices.getData(nInput.setFollowRedirect(false), (method -> {
            assertTrue(HttpServices.isRedirected(method));
            assertEquals("redirect to /echo", baseUrl + "/echo", method.getResponseHeader("location").getValue());
            return HttpServices.Status.ok();
        }));
    }

    @Test
    public void testGetWithAuth(){
        input.setHeader("Authorization", "Bearer abc-123");
        ByteArrayOutputStream results = new ByteArrayOutputStream();
        HttpServices.Status status = HttpServices.getData(input.setRequestUrl(baseUrl + "/echo"), HttpServices.defaultHandler(results));
        validateResults(status, results);
        assertEquals("Bearer abc-123", getHeader(toJson(results), "Authorization"));
    }

    @Test
    public void testGetWithAuthRedirected(){
        AtomicReference<String> authAtRedirect = new AtomicReference<>();
        server.createContext("/moved", ex -> {
            authAtRedirect.set(ex.getRequestHeaders().getFirst("Authorization"));
            ex.getResponseHeaders().set("Location", baseUrl + "/echo");
            ex.sendResponseHeaders(301, -1);
            ex.close();
        });
        HttpServiceInput nInput = input.setRequestUrl(baseUrl + "/moved").setHeader("Authorization", "Bearer abc-123");

        ByteArrayOutputStream results = new ByteArrayOutputStream();
        HttpServices.Status status = HttpServices.getData(nInput, 3, HttpServices.defaultHandler(results));

        assertEquals(200, status.getStatusCode());    // 200 OK
        assertEquals("Bearer abc-123", authAtRedirect.get());
        // only credentials from the SsoAdapter are re-applied for the new url; explicit ones are not forwarded
        assertEquals("", getHeader(toJson(results), "Authorization"));
    }

    @Test
    public void testFetchMaxFollow() {
        server.createContext("/loop", ex -> {
            ex.getResponseHeaders().set("Location", baseUrl + "/loop");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        HttpServiceInput nInput = input.setRequestUrl(baseUrl + "/loop");

        HttpServices.Status status = HttpServices.getData(nInput, 1, method -> {
            fail("Should not get here");
            return HttpServices.Status.ok();
        });
        assertEquals(421, status.getStatusCode());    // 421 ERR_TOO_MANY_REDIRECTS
    }

    @Test
    public void testFailedRequestLogIsRedacted() {
        server.createContext("/fail", ex -> {
            ex.sendResponseHeaders(500, -1);
            ex.close();
        });
        List<String> logs = captureLogs(() -> HttpServices.getData(
                input.setRequestUrl(baseUrl + "/fail").setHeader("Authorization", "Bearer secret-token-123"),
                new ByteArrayOutputStream()));

        String allLogs = String.join("\n", logs);
        assertTrue("failed request should be logged with its curl command", allLogs.contains("CURL CMD"));
        assertTrue(allLogs.contains("Authorization: Bearer [redacted]"));
        assertFalse("token should not be logged", allLogs.contains("secret-token-123"));
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

        assertEquals("[redacted]", sanitizeHeader("Authorization", "abc.def.ghi"));           // no scheme
        assertEquals("Basic [redacted]", sanitizeHeader("Proxy-Authorization", "Basic dXNlcjpwYXNz"));
        assertEquals("[redacted]", sanitizeHeader("X-Api-Key", "secret-key"));
        assertEquals("text/html", sanitizeHeader("Content-Type", "text/html"));
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
//  test server handlers
//====================================================================

    /** Respond with the request as JSON: method, path, args, form, headers, contentType, and body. */
    private static void echo(HttpExchange ex) throws IOException {
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");

        Map<String, String> headers = new LinkedHashMap<>();
        ex.getRequestHeaders().forEach((k, v) -> headers.put(k.toLowerCase(), String.join(", ", v)));

        Map<String, Object> json = new LinkedHashMap<>();
        json.put("method", ex.getRequestMethod());
        json.put("path", ex.getRequestURI().getPath());
        json.put("args", parseQuery(ex.getRequestURI().getRawQuery()));
        json.put("form", contentType != null && contentType.startsWith("application/x-www-form-urlencoded") ? parseQuery(body) : Map.of());
        json.put("headers", headers);
        json.put("contentType", contentType);
        json.put("body", body);
        sendJson(ex, Serializer.toJsonBytes(json), false);
    }

    private static void gzip(HttpExchange ex) throws IOException {
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(compressed)) {
            gz.write(Serializer.toJsonBytes(Map.of("gzipped", true)));
        }
        sendJson(ex, compressed.toByteArray(), true);
    }

    /** Redirect to the 'url' query param, with 'status_code' if given, otherwise 302. */
    private static void redirectTo(HttpExchange ex) throws IOException {
        Map<String, String> args = parseQuery(ex.getRequestURI().getRawQuery());
        String statusCode = args.get("status_code");
        ex.getResponseHeaders().set("Location", args.get("url"));
        ex.sendResponseHeaders(statusCode == null ? 302 : Integer.parseInt(statusCode), -1);
        ex.close();
    }

    private static void sendJson(HttpExchange ex, byte[] content, boolean gzipped) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        if (gzipped) ex.getResponseHeaders().set("Content-Encoding", "gzip");
        ex.sendResponseHeaders(200, content.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(content); }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (query == null || query.isEmpty()) return params;
        for (String pair : query.split("&")) {
            String[] keyVal = pair.split("=", 2);
            params.put(URLDecoder.decode(keyVal[0], StandardCharsets.UTF_8),
                       keyVal.length > 1 ? URLDecoder.decode(keyVal[1], StandardCharsets.UTF_8) : "");
        }
        return params;
    }

//====================================================================
//  private
//====================================================================

    /** Run the action and return the messages HttpServices logged while it ran. */
    private static List<String> captureLogs(Runnable action) {
        List<String> logs = new ArrayList<>();
        AbstractAppender appender = new AbstractAppender("HttpServicesTest", null, null, true, Property.EMPTY_ARRAY) {
            public void append(LogEvent event) { logs.add(event.getMessage().getFormattedMessage()); }
        };
        appender.start();
        String loggerName = HttpServices.class.getName();
        var logger = (org.apache.logging.log4j.core.Logger) LogManager.getLogger(loggerName);
        Level origLevel = logger.getLevel();
        Configurator.setLevel(loggerName, Level.INFO);       // logging is OFF during tests
        logger.addAppender(appender);
        try {
            action.run();
        } finally {
            logger.removeAppender(appender);
            appender.stop();
            Configurator.setLevel(loggerName, origLevel);
        }
        return logs;
    }

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

        JsonNode json = toJson(results);
        assertEquals("cookie2 value should be cookie2_val", "cookie2_val", getCookie(json, "cookie2"));
        assertEquals("header1 value should be header1_val", "header1_val", getHeader(json, "Header1"));
        assertEquals("param1 value should be " + PARAM1_VAL, PARAM1_VAL, getParam(json, "param1"));
    }

    private static JsonNode toJson(ByteArrayOutputStream results) {
        return Serializer.fromJson(results.toByteArray(), JsonNode.class);
    }

    private static String getParam(JsonNode json, String key) {
        JsonNode val = json.path("args").path(key);
        return (val.isMissingNode() ? json.path("form").path(key) : val).asText();
    }

    private static String getHeader(JsonNode json, String key) {
        return json.path("headers").path(key.toLowerCase()).asText();
    }

    private static String getCookie(JsonNode json, String key) {
        HashMap<String,String> cookies = new HashMap<>();
        Arrays.stream(getHeader(json, "Cookie").split(";")).map((s) -> s.trim().split("="))
                .forEach((c) -> cookies.put(c[0], c.length>1 ? c[1] : null));
        return cookies.get(key);
    }

}
