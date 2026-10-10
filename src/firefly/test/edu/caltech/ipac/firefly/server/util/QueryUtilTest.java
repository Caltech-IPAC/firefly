/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.util;

import edu.caltech.ipac.firefly.ConfigTest;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.Assert.assertTrue;

public class QueryUtilTest extends ConfigTest {

    @Test
    public void tmpFileForUrl() throws IOException {
        File dir = Files.createTempDirectory("queryUtilTest").toFile();
        dir.deleteOnExit();

        assertTmpExt(dir, "http://h/a/x.fits.gz", ".fits.gz");
        assertTmpExt(dir, "http://h/a/x.csv", ".csv");
        // a query string makes the path extension unreliable
        assertTmpExt(dir, "http://h/a/x.fits.gz?token=1", ".ul");
        assertTmpExt(dir, "http://h/cgi/get.py?file=a.csv", ".ul");
        assertTmpExt(dir, "http://h/a/noext", ".ul");
    }

    private static void assertTmpExt(File dir, String url, String ext) {
        File f = QueryUtil.tmpFileForUrl(url, "req_", dir);
        f.deleteOnExit();
        assertTrue(url + " -> " + f.getName(), f.getName().endsWith(ext));
    }
}
