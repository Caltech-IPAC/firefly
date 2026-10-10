/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util.download;

import org.junit.Test;

import java.util.List;

import static edu.caltech.ipac.util.download.UriRef.CloudEnvironment.AWS;
import static edu.caltech.ipac.util.download.UriRef.CloudEnvironment.GCP;
import static edu.caltech.ipac.util.download.UriRef.CloudEnvironment.ON_PREM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RetrieveUtilTest {

    private static final UriRef S3 = UriRef.make("s3://bucket/a/x.fits");
    private static final UriRef GCS = UriRef.make("https://storage.googleapis.com/bucket/a/x.fits");
    private static final UriRef URL = UriRef.make("https://abc.com/a/x.fits");
    private static final UriRef URL2 = UriRef.make("https://def.com/a/x.fits");

    @Test
    public void makeUniqueFileNameExtension() {
        assertExt("https://abc.com/a/x.fits", ".fits");
        assertExt("https://abc.com/a/x.fits.gz", ".fits.gz");
        assertExt("https://abc.com/a/x.xyz", ".ul");          // not in the known extension list
        assertExt("s3://bucket/a/x.fits", ".fits");           // s3 refs have no query
        // a query string makes the path extension unreliable
        assertExt("https://abc.com/a/x.fits?sig=abc", ".ul");
        assertExt("https://abc.com/cgi/get?file=a.fits", ".ul");
    }

    @Test
    public void makeUniqueFileNameIsUnique() {
        String a1 = name("https://abc.com/a/x.fits?a=1", "user1");
        assertEquals("same input, same name", a1, name("https://abc.com/a/x.fits?a=1", "user1"));
        assertNotEquals("different query", a1, name("https://abc.com/a/x.fits?a=2", "user1"));
        assertNotEquals("different user", a1, name("https://abc.com/a/x.fits?a=1", "user2"));
    }

    @Test
    public void makeUniqueFileNameChars() {
        // "=" and "," cause problems in the download servlet
        String n = name("https://abc.com/cgi/get?file=a.fits&pos=10,20", null);
        assertFalse(n, n.matches(".*[=,?:/\\\\ ].*"));
    }

    @Test
    public void makeUniqueFileNameNull() {
        assertEquals("no-network-resource.empty", RetrieveUtil.makeUniqueFileName(null, null, null));
    }

    @Test
    public void optimalUriTrivial() {
        assertNull("empty list", RetrieveUtil.getOptimalUri(List.of(), AWS));
        assertSame("single entry is used whatever the env", GCS, RetrieveUtil.getOptimalUri(List.of(GCS), AWS));
        assertSame("same type, first wins", URL, RetrieveUtil.getOptimalUri(List.of(URL, URL2), AWS));
    }

    @Test
    public void optimalUriMatchesCloud() {
        List<UriRef> all = List.of(URL, S3, GCS);
        assertSame("aws prefers s3", S3, RetrieveUtil.getOptimalUri(all, AWS));
        assertSame("gcp prefers gcs", GCS, RetrieveUtil.getOptimalUri(all, GCP));
        assertSame("on prem prefers url", URL, RetrieveUtil.getOptimalUri(all, ON_PREM));
        assertSame("no env prefers url", URL, RetrieveUtil.getOptimalUri(all, null));
    }

    @Test
    public void optimalUriFallsBackToUrl() {
        // the env's cloud is not in the list, so use the url
        assertSame("gcp, no gcs", URL, RetrieveUtil.getOptimalUri(List.of(S3, URL), GCP));
        assertSame("aws, no s3", URL, RetrieveUtil.getOptimalUri(List.of(GCS, URL), AWS));
        assertSame("first url of that type", URL, RetrieveUtil.getOptimalUri(List.of(S3, URL, URL2), ON_PREM));
    }

    @Test
    public void optimalUriCloudsOnly() {
        List<UriRef> clouds = List.of(GCS, S3);
        assertSame("aws", S3, RetrieveUtil.getOptimalUri(clouds, AWS));
        assertSame("gcp", GCS, RetrieveUtil.getOptimalUri(clouds, GCP));
        // no match and no url: falls back to s3, then gcs, regardless of list order
        assertSame("on prem guesses s3", S3, RetrieveUtil.getOptimalUri(clouds, ON_PREM));
    }

    private static String name(String uri, String user) {
        return RetrieveUtil.makeUniqueFileName(UriRef.make(uri), user, null);
    }

    private static void assertExt(String uri, String ext) {
        String n = name(uri, null);
        assertTrue(uri + " -> " + n, n.endsWith(ext));
    }
}
