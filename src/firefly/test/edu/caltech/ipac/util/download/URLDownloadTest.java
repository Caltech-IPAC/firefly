/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util.download;

import edu.caltech.ipac.firefly.ConfigTest;
import org.junit.Assert;
import org.junit.Test;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.util.download.URLDownload.codeFromException;
import static edu.caltech.ipac.util.download.URLDownload.firstParamValUsingKeyList;
import static edu.caltech.ipac.util.download.URLDownload.getQueryParams;
import static edu.caltech.ipac.util.download.URLDownload.getSuggestedFileName;
import static edu.caltech.ipac.util.download.URLDownload.makeContentDisposition;

public class URLDownloadTest extends ConfigTest {

    @Test
    public void makeHeader() {
        Assert.assertEquals("attachment; filename=\"table.csv\"; filename*=UTF-8''table.csv",
                makeContentDisposition("table.csv"));

        // characters that break an unquoted header value
        Assert.assertEquals("attachment; filename=\"a, b; c.csv\"; filename*=UTF-8''a%2C%20b%3B%20c.csv",
                makeContentDisposition("a, b; c.csv"));

        // quote, path separators, control chars and non-ascii
        Assert.assertEquals("attachment; filename=\"_x_y_z__.fits\"; filename*=UTF-8''%22x_y_z_%C3%A9.fits",
                makeContentDisposition("\"x/y\\z\né.fits"));
        Assert.assertTrue(makeContentDisposition("é*.fits").endsWith("filename*=UTF-8''%C3%A9%2A.fits"));
    }

    @Test
    public void parseHeader() {
        Assert.assertEquals("table.csv", getSuggestedFileName("attachment; filename=table.csv"));
        Assert.assertEquals("table.csv", getSuggestedFileName("attachment; filename=\"table.csv\""));
        Assert.assertEquals("a__b.csv", getSuggestedFileName("attachment; filename=\"a; b.csv\""));
        Assert.assertEquals("table.csv", getSuggestedFileName("inline; filename=table.csv; size=100"));
        Assert.assertEquals("t_1.csv", getSuggestedFileName("attachment; filename=\"fallback.csv\"; filename*=UTF-8''t%201.csv"));
        Assert.assertNull(getSuggestedFileName("attachment"));
    }

    @Test
    public void roundTrip() {
        for (String name : new String[] {"table.csv", "my table, v2.tbl", "image;1.fits"}) {
            String expected = URLDownload.sanitizeFilename(name);
            Assert.assertEquals(expected, getSuggestedFileName(makeContentDisposition(name)));
        }
    }

    @Test
    public void queryParams() throws Exception {
        Assert.assertTrue("null url", getQueryParams(null).isEmpty());
        Assert.assertTrue("no query", getQueryParams(url("https://abc.com/a/x.fits")).isEmpty());

        var p = getQueryParams(url("https://abc.com/a?x=1&y=2&x=3&flag&&=5&eq=a=b&sig=abc==&empty="));
        Assert.assertEquals(List.of("1", "3"), p.get("x"));      // repeated keys keep their order
        Assert.assertEquals(List.of("2"), p.get("y"));
        Assert.assertEquals("key with no value", List.of("true"), p.get("flag"));
        Assert.assertEquals("value containing =", List.of("a=b"), p.get("eq"));
        Assert.assertEquals("trailing = kept", List.of("abc=="), p.get("sig"));
        Assert.assertEquals("explicit empty value", List.of(""), p.get("empty"));
        Assert.assertEquals("empty segment and empty key are skipped", 6, p.size());
    }

    @Test
    public void firstParamVal() {
        Map<String, List<String>> p = Map.of("b", List.of("b1", "b2"), "c", List.of("c1"), "empty", List.of());
        Assert.assertEquals("first key that has a value wins", "b1", firstParamValUsingKeyList(p, "a", "b", "c"));
        Assert.assertEquals("key order decides, not map order", "c1", firstParamValUsingKeyList(p, "c", "b"));
        Assert.assertEquals("empty value list is skipped", "c1", firstParamValUsingKeyList(p, "empty", "c"));
        Assert.assertNull("no match", firstParamValUsingKeyList(p, "x", "y"));
        Assert.assertNull("no keys", firstParamValUsingKeyList(p));
        Assert.assertNull("null params", firstParamValUsingKeyList(null, "b"));
    }

    @Test
    public void suggestedFileNameFromConnection() throws Exception {
        Assert.assertNull("null connection", getSuggestedFileName((URLConnection) null));
        Assert.assertNull("no header", getSuggestedFileName(conn(null)));
        Assert.assertEquals("table.csv", getSuggestedFileName(conn("attachment; filename=\"table.csv\"")));
    }

    @Test
    public void codeFromExceptionMapping() {
        Assert.assertEquals(495, codeFromException(new SSLException("x")));
        Assert.assertEquals(HttpURLConnection.HTTP_CLIENT_TIMEOUT, codeFromException(new SocketTimeoutException()));
        Assert.assertEquals(HttpURLConnection.HTTP_BAD_GATEWAY, codeFromException(new UnknownHostException()));
        Assert.assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, codeFromException(new IOException()));
        Assert.assertEquals(HttpURLConnection.HTTP_INTERNAL_ERROR, codeFromException(new RuntimeException()));
    }

    private static URL url(String s) throws Exception { return new URI(s).toURL(); }

    /** a connection that only answers the Content-Disposition header, no network */
    private static URLConnection conn(String disposition) throws Exception {
        return new URLConnection(url("https://abc.com/a")) {
            @Override public void connect() {}
            @Override public String getHeaderField(String name) {
                return "content-disposition".equalsIgnoreCase(name) ? disposition : null;
            }
        };
    }
}
