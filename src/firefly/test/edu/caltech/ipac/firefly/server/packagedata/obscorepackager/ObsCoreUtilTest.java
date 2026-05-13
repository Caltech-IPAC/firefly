/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.packagedata.obscorepackager;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.table.MappedData;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Date: 1/13/26
 *
 * @author kartik
 * @version : $
 */
public class ObsCoreUtilTest extends ConfigTest {

    @Test
    public void testTestSem() {
        Assert.assertTrue(ObsCoreUtil.testSem("http://example.org#cutout", "#cutout"));
        Assert.assertTrue(ObsCoreUtil.testSem("http://example.org#CUTOUT", "#cutout"));
        Assert.assertFalse(ObsCoreUtil.testSem("http://example.org#this", "#cutout"));
        Assert.assertFalse(ObsCoreUtil.testSem(null, "#cutout"));
    }

    @Test
    public void testGetExtFromURL() {
        Assert.assertEquals("fits", ObsCoreUtil.getExtFromURL("image/fits"));
        Assert.assertEquals("json", ObsCoreUtil.getExtFromURL("application/json; charset=utf-8"));
        Assert.assertEquals("pdf", ObsCoreUtil.getExtFromURL("http://example.org/a.pdf"));
        Assert.assertEquals("unknown", ObsCoreUtil.getExtFromURL("application/octet-stream"));
        Assert.assertEquals("unknown", ObsCoreUtil.getExtFromURL(null));
    }

    @Test
    public void testGetPositionColValsUsesExplicitValues() {
        String pos = "{"
                + "\"centerColNames\":{\"lonCol\":\"ra\",\"latCol\":\"dec\"},"
                + "\"centerColValues\":{\"ra\":\"10.5\",\"dec\":\"-20.25\"}"
                + "}";

        List<String> vals = ObsCoreUtil.getPositionColVals(pos, 0, null);
        Assert.assertEquals("10.5", vals.get(0));
        Assert.assertEquals("-20.25", vals.get(1));
    }

    @Test
    public void testGetPositionColValsFallsBackToMappedData() {
        String pos = "{"
                + "\"centerColNames\":{\"lonCol\":\"ra_col\",\"latCol\":\"dec_col\"},"
                + "\"centerColValues\":{\"ra\":\"null\",\"dec\":\"null\"}"
                + "}";

        MappedData md = new MappedData();
        md.put(0, "ra_col", "123.4");
        md.put(0, "dec_col", "-55.6");

        List<String> vals = ObsCoreUtil.getPositionColVals(pos, 0, md);
        Assert.assertEquals("123.4", vals.get(0));
        Assert.assertEquals("-55.6", vals.get(1));
    }

    @Test
    public void testMakeUniquePrefix() {
        Map<String, Integer> m = new HashMap<>();
        Assert.assertEquals("abc", ObsCoreUtil.makeUniquePrefix("abc", m));
        Assert.assertEquals("abc-1", ObsCoreUtil.makeUniquePrefix("abc", m));
        Assert.assertEquals("abc-2", ObsCoreUtil.makeUniquePrefix("abc", m));
        Assert.assertEquals("xyz", ObsCoreUtil.makeUniquePrefix("xyz", m));
    }

    @Test
    public void testMakeValidString() {
        Assert.assertEquals("HelloWorld", ObsCoreUtil.makeValidString("Hello World"));
        Assert.assertEquals("ab.c-d", ObsCoreUtil.makeValidString("a b.c-d"));
        Assert.assertEquals("a_b_c", ObsCoreUtil.makeValidString("a@b#c"));
    }

    @Test
    public void testGetFileNamePrefixUsesTemplateCols() throws Exception {
        MappedData md = new MappedData();
        md.put(0, "c1", "Hello World");
        md.put(0, "c2", "X/Y");

        String[] templateCols = new String[]{"c1", "c2"};
        List<String> dummyRaDec = List.of("10", "-10");

        String prefix = ObsCoreUtil.getFileNamePrefix(
                0, templateCols, md, new URL("http://example.org/a.fits"), dummyRaDec);

        Assert.assertEquals("HelloWorld-X_Y", prefix);
    }

    @Test
    public void testGetFileNamePrefixFallsBackToObsTitle() throws Exception {
        MappedData md = new MappedData();
        md.put(0, "obs_title", "Obs Core Title");
        List<String> dummyRaDec = List.of("10", "-10");

        String prefix = ObsCoreUtil.getFileNamePrefix(
                0, null, md, new URL("http://example.org/a.fits"), dummyRaDec);

        Assert.assertEquals("ObsCoreTitle", prefix);
    }

    @Test
    public void testGetFileNamePrefixFallsBackToManualTemplate() throws Exception {
        MappedData md = new MappedData();
        md.put(0, "obs_collection", "Spitzer");
        md.put(0, "instrument_name", "IRAC");
        md.put(0, "obs_id", "SPITZER_I1_00012345");
        List<String> dummyRaDec = List.of("10", "-10");

        String prefix = ObsCoreUtil.getFileNamePrefix(
                0, null, md, new URL("http://example.org/a.fits"), dummyRaDec);

        Assert.assertEquals("Spitzer-IRAC-SPITZER_I1_00012345", prefix);
    }

    @Test
    public void testGetFileNamePrefixFallsBackToRaDecOrIndex() throws Exception {
        MappedData md = new MappedData();
        List<String> dummyRaDec = List.of("10", "-10");

        String prefix1 = ObsCoreUtil.getFileNamePrefix(
                0, null, md, new URL("http://example.org/a.fits"), dummyRaDec);
        Assert.assertEquals("10_-10", prefix1); //test using ra/dec

        String prefix2 = ObsCoreUtil.getFileNamePrefix(
                5, null, md, new URL("http://example.org/a.fits"), Arrays.asList(null, null));
        Assert.assertEquals("file_5", prefix2); //test final fallback to filename
    }
}
