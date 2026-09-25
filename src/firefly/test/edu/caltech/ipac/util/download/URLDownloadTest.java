/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util.download;

import edu.caltech.ipac.firefly.ConfigTest;
import org.junit.Assert;
import org.junit.Test;

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
}
