/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.servlets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import edu.caltech.ipac.firefly.ConfigTest;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static edu.caltech.ipac.firefly.server.servlets.AnyFileDownload.DOWNLOAD_TOKEN;
import static edu.caltech.ipac.firefly.server.servlets.AnyFileDownload.RETURN_PARAM;
import static edu.caltech.ipac.firefly.server.servlets.AnyFileDownload.STREAM_READ_TIMEOUT;
import static edu.caltech.ipac.firefly.server.servlets.AnyFileDownload.STREAM_URL;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the streamURL mode of servlet/Download against a remote server running in this JVM.
 */
public class AnyFileDownloadTest extends ConfigTest {

    private HttpServer upstream;
    private String baseUrl;

    @Before
    public void startUpstream() throws IOException {
        upstream = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        upstream.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        upstream.start();
        baseUrl = "http://127.0.0.1:" + upstream.getAddress().getPort();
    }

    @After
    public void stopUpstream() {
        upstream.stop(0);
    }

    @Test
    public void passesBytesThroughUnchanged() throws Exception {
        byte[] gz = new byte[200_000];
        new Random(1).nextBytes(gz);
        upstream.createContext("/cat.vot.gz", ex -> {
            ex.getResponseHeaders().set("Content-Type", "application/gzip");
            ex.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"cat.vot.gz\"");
            ex.sendResponseHeaders(200, gz.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(gz); }
        });

        FakeResponse res = new FakeResponse(new ByteArrayOutputStream());
        new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/cat.vot.gz", RETURN_PARAM, "job-7", DOWNLOAD_TOKEN, "tok1"), res.proxy);

        assertEquals(200, res.status);
        assertArrayEquals(gz, res.body());
        assertEquals("application/gzip", res.headers.get("Content-Type"));
        assertEquals(String.valueOf(gz.length), res.headers.get("Content-Length"));
        assertNull("not decoded, so no encoding to pass on", res.headers.get("Content-Encoding"));
        assertTrue(res.headers.get("Content-Disposition").startsWith("attachment; filename=\"cat.vot.gz\""));
        assertEquals("nosniff", res.headers.get("X-Content-Type-Options"));
        assertEquals("ffdl_tok1", res.cookies.get(0).getName());
    }

    @Test
    public void startsSendingBeforeTheRemoteFileEnds() throws Exception {
        CountDownLatch finishUpstream = new CountDownLatch(1);
        upstream.createContext("/slow", ex -> {
            ex.sendResponseHeaders(200, 0);                 // chunked: no length known up front
            OutputStream os = ex.getResponseBody();
            os.write(new byte[64 * 1024]);
            os.flush();
            await(finishUpstream);
            os.write(new byte[64 * 1024]);
            os.close();
        });

        CountDownLatch firstPartArrived = new CountDownLatch(1);
        FakeResponse res = new FakeResponse(new ByteArrayOutputStream() {
            @Override public synchronized void write(byte[] b, int off, int len) {
                super.write(b, off, len);
                if (size() >= 64 * 1024) firstPartArrived.countDown();
            }
        });
        Thread t = new Thread(() -> runQuietly(() -> new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/slow"), res.proxy)));
        t.start();

        assertTrue("the first 64 KB should reach the browser while the remote server is still sending",
                firstPartArrived.await(10, TimeUnit.SECONDS));
        finishUpstream.countDown();
        t.join(10_000);
        assertEquals(128 * 1024, res.body().length);
    }

    @Test
    public void remoteErrorBecomesAReadableMessage() throws Exception {
        upstream.createContext("/gone", ex -> {
            ex.sendResponseHeaders(404, -1);
            ex.close();
        });

        FakeResponse res = new FakeResponse(new ByteArrayOutputStream());
        new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/gone", DOWNLOAD_TOKEN, "tok2"), res.proxy);

        assertEquals(502, res.status);
        String text = new String(res.body(), StandardCharsets.UTF_8);
        assertTrue(text, text.contains(baseUrl + "/gone") && text.contains("404"));
        assertNull(res.headers.get("Content-Disposition"));
        assertTrue("no download-started cookie on failure", res.cookies.isEmpty());
    }

    @Test
    public void remoteFileCutShortBeforeSendingBecomesAReadableMessage() throws Exception {
        upstream.createContext("/cut0", cutShort(1000, new byte[0]));

        FakeResponse res = new FakeResponse(new ByteArrayOutputStream());
        new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/cut0", DOWNLOAD_TOKEN, "tok3"), res.proxy);

        assertEquals(502, res.status);
        String text = new String(res.body(), StandardCharsets.UTF_8);
        assertTrue(text, text.contains("0 of 1000 bytes"));
        assertTrue("no download-started cookie on failure", res.cookies.isEmpty());
    }

    @Test
    public void remoteFileCutShortAfterSendingDropsTheConnection() throws Exception {
        upstream.createContext("/cut", cutShort(200_000, new byte[100_000]));

        FakeResponse res = new FakeResponse(new ByteArrayOutputStream());
        try {
            new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/cut"), res.proxy);
            assertTrue("the servlet should throw so Tomcat drops the connection", false);
        } catch (IOException e) {
            assertTrue(e.getCause().getMessage(), e.getCause().getMessage().contains("100000 of 200000 bytes"));
        }
    }

    @Test
    public void remoteServerGoingSilentAfterARedirectEndsTheDownload() throws Exception {
        CountDownLatch silence = new CountDownLatch(1);
        upstream.createContext("/stall", stallsAfter(new byte[64 * 1024], silence));
        upstream.createContext("/moved", ex -> {          // TAP result URLs often redirect; the timeout must carry over
            ex.getResponseHeaders().set("Location", baseUrl + "/stall");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });

        FakeResponse res = new FakeResponse(new ByteArrayOutputStream());
        System.setProperty(STREAM_READ_TIMEOUT, "1");
        try {
            new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/moved"), res.proxy);
            assertTrue("the servlet should throw so Tomcat drops the connection", false);
        } catch (IOException e) {
            assertTrue(String.valueOf(e.getCause()), e.getCause() instanceof SocketTimeoutException);
        } finally {
            System.clearProperty(STREAM_READ_TIMEOUT);
            silence.countDown();
        }
        assertEquals(64 * 1024, res.body().length);
    }

    @Test
    public void remoteServerSilentBeforeTheFirstChunkBecomesAReadableMessage() throws Exception {
        CountDownLatch silence = new CountDownLatch(1);
        upstream.createContext("/silent", stallsAfter(new byte[0], silence));

        FakeResponse res = new FakeResponse(new ByteArrayOutputStream());
        System.setProperty(STREAM_READ_TIMEOUT, "1");
        try {
            new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/silent", DOWNLOAD_TOKEN, "tok4"), res.proxy);
        } finally {
            System.clearProperty(STREAM_READ_TIMEOUT);
            silence.countDown();
        }

        assertEquals(502, res.status);
        String text = new String(res.body(), StandardCharsets.UTF_8);
        assertTrue(text, text.contains("no response body"));
        assertTrue("no download-started cookie on failure", res.cookies.isEmpty());
    }

    @Test
    public void rejectsNonHttpUrls() throws Exception {
        FakeResponse res = new FakeResponse(new ByteArrayOutputStream());
        new AnyFileDownload().processRequest(request(STREAM_URL, "file:///etc/passwd"), res.proxy);
        assertEquals(400, res.status);
    }

    @Test
    public void namesTheFileFromReturnAndContentType() throws Exception {
        upstream.createContext("/async/42/results/result", text("application/x-votable+xml", "<VOTABLE/>"));

        FakeResponse res = new FakeResponse(new ByteArrayOutputStream());
        new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/async/42/results/result", RETURN_PARAM, "my query"), res.proxy);

        assertTrue(res.headers.get("Content-Disposition"), res.headers.get("Content-Disposition").startsWith("attachment; filename=\"my query.xml\""));

        FakeResponse noReturn = new FakeResponse(new ByteArrayOutputStream());
        new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/async/42/results/result"), noReturn.proxy);
        assertTrue(noReturn.headers.get("Content-Disposition"), noReturn.headers.get("Content-Disposition").startsWith("attachment; filename=\"result.xml\""));

        upstream.createContext("/data/cat.vot", text("application/x-votable+xml", "<VOTABLE/>"));
        FakeResponse urlName = new FakeResponse(new ByteArrayOutputStream());
        new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/data/cat.vot"), urlName.proxy);
        assertTrue(urlName.headers.get("Content-Disposition"), urlName.headers.get("Content-Disposition").startsWith("attachment; filename=\"cat.vot\""));

        upstream.createContext("/async/43/results/result", text("text/xml; charset=utf-8", "<VOTABLE/>"));
        FakeResponse textXml = new FakeResponse(new ByteArrayOutputStream());
        new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/async/43/results/result", RETURN_PARAM, "mast query"), textXml.proxy);
        assertTrue(textXml.headers.get("Content-Disposition"), textXml.headers.get("Content-Disposition").startsWith("attachment; filename=\"mast query.xml\""));

        upstream.createContext("/data/other.vot", text("text/xml", "<VOTABLE/>"));
        FakeResponse urlExtFirst = new FakeResponse(new ByteArrayOutputStream());
        new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/data/other.vot", RETURN_PARAM, "q"), urlExtFirst.proxy);
        assertTrue(urlExtFirst.headers.get("Content-Disposition"), urlExtFirst.headers.get("Content-Disposition").startsWith("attachment; filename=\"q.vot\""));
    }

    @Test
    public void browserCancelStopsReadingTheRemoteFile() throws Exception {
        AtomicLong sent = new AtomicLong();
        upstream.createContext("/big", throttled(20_000_000, sent));      // ~16 s to send it all

        OutputStream failsAfterFirstWrite = new OutputStream() {
            int writes;
            @Override public void write(int b) throws IOException { write(new byte[]{(byte) b}, 0, 1); }
            @Override public void write(byte[] b, int off, int len) throws IOException {
                if (++writes > 1) throw new IOException("Connection reset by peer");
            }
        };
        new AnyFileDownload().processRequest(request(STREAM_URL, baseUrl + "/big"), new FakeResponse(failsAfterFirstWrite).proxy);

        assertTrue("the remote server sent " + sent.get() + " bytes", sent.get() < 20_000_000);
    }

    @Test
    public void headRequestDoesNotReadTheBody() throws Exception {
        AtomicLong sent = new AtomicLong();
        upstream.createContext("/big", throttled(20_000_000, sent));

        FakeResponse res = new FakeResponse(new ByteArrayOutputStream());
        new AnyFileDownload().doHead(request(STREAM_URL, baseUrl + "/big"), res.proxy);

        assertTrue("the remote server sent " + sent.get() + " bytes", sent.get() < 20_000_000);
        assertEquals("20000000", res.headers.get("Content-Length"));
        assertEquals(0, res.body().length);
    }

//====================================================================
//  helpers
//====================================================================

    private static HttpHandler text(String type, String body) {
        return ex -> {
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", type);
            ex.sendResponseHeaders(200, b.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(b); }
        };
    }

    /** Promises {@code declared} bytes but sends only {@code body}, then drops the connection. */
    private static HttpHandler cutShort(long declared, byte[] body) {
        return ex -> {
            ex.sendResponseHeaders(200, declared);
            OutputStream os = ex.getResponseBody();
            os.write(body);
            os.flush();
            os.close();     // throws "insufficient bytes written to stream", and the server closes the connection
        };
    }

    /** Sends the headers alone (chunked), 300 ms later {@code body}, then nothing, with the connection open, until {@code silence} ends. */
    private static HttpHandler stallsAfter(byte[] body, CountDownLatch silence) {
        return ex -> {
            ex.sendResponseHeaders(200, 0);
            try (OutputStream os = ex.getResponseBody()) {
                Thread.sleep(300);
                if (body.length > 0) {
                    os.write(body);
                    os.flush();
                }
                await(silence);
            } catch (IOException | InterruptedException ignored) {}
        };
    }

    /** Sends {@code total} bytes, 64 KB every 50 ms, counting each chunk before writing it, until the client hangs up. */
    private static HttpHandler throttled(long total, AtomicLong sent) {
        return (HttpExchange ex) -> {
            ex.sendResponseHeaders(200, total);
            byte[] chunk = new byte[64 * 1024];
            try (OutputStream os = ex.getResponseBody()) {
                while (sent.get() < total) {
                    int n = (int) Math.min(chunk.length, total - sent.get());
                    sent.addAndGet(n);      // counted before the write, so the count is complete before Firefly can read the last bytes
                    os.write(chunk, 0, n);
                    os.flush();
                    Thread.sleep(50);
                }
            } catch (IOException | InterruptedException ignored) {}
        };
    }

    private static HttpServletRequest request(String... keyVals) {
        Map<String, String[]> params = new HashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) params.put(keyVals[i], new String[]{keyVals[i + 1]});
        return (HttpServletRequest) Proxy.newProxyInstance(AnyFileDownloadTest.class.getClassLoader(),
                new Class[]{HttpServletRequest.class},
                (proxy, m, args) -> switch (m.getName()) {
                    case "getParameterMap" -> params;
                    case "getParameter" -> params.containsKey(args[0]) ? params.get(args[0])[0] : null;
                    default -> null;
                });
    }

    private static void await(CountDownLatch latch) {
        try { latch.await(10, TimeUnit.SECONDS); } catch (InterruptedException ignored) {}
    }

    private interface ThrowingRunnable { void run() throws Exception; }
    private static void runQuietly(ThrowingRunnable r) {
        try { r.run(); } catch (Exception e) { throw new RuntimeException(e); }
    }

    /** Records what the servlet sets on the response; the body goes to the given stream. */
    private static class FakeResponse implements java.lang.reflect.InvocationHandler {
        int status = 200;
        final Map<String, String> headers = new HashMap<>();
        final List<Cookie> cookies = new ArrayList<>();
        private final OutputStream out;
        private boolean committed;

        final HttpServletResponse proxy = (HttpServletResponse) Proxy.newProxyInstance(
                AnyFileDownloadTest.class.getClassLoader(), new Class[]{HttpServletResponse.class}, this);

        FakeResponse(OutputStream out) { this.out = out; }

        byte[] body() { return out instanceof ByteArrayOutputStream b ? b.toByteArray() : new byte[0]; }

        @Override
        public Object invoke(Object proxy, java.lang.reflect.Method m, Object[] args) throws Throwable {
            switch (m.getName()) {
                case "setStatus" -> status = (int) args[0];
                case "setContentType" -> headers.put("Content-Type", (String) args[0]);
                case "setContentLength", "setContentLengthLong" -> headers.put("Content-Length", String.valueOf(args[0]));
                case "setHeader", "addHeader" -> headers.put((String) args[0], (String) args[1]);
                case "addCookie" -> cookies.add((Cookie) args[0]);
                case "isCommitted" -> { return committed; }
                case "reset" -> {
                    if (committed) throw new IllegalStateException("committed");
                    status = 200; headers.clear(); cookies.clear();
                    if (out instanceof ByteArrayOutputStream b) b.reset();
                }
                case "getOutputStream" -> { return servletStream(); }
                default -> {
                    if (m.getReturnType() == boolean.class) return false;
                    if (m.getReturnType() == int.class) return 0;
                }
            }
            return null;
        }

        private ServletOutputStream servletStream() {
            return new ServletOutputStream() {
                @Override public boolean isReady() { return true; }
                @Override public void setWriteListener(WriteListener l) {}
                @Override public void write(int b) throws IOException { write(new byte[]{(byte) b}, 0, 1); }
                @Override public void write(byte[] b, int off, int len) throws IOException {
                    committed = true;
                    out.write(b, off, len);
                }
            };
        }
    }
}
