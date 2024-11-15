/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.util;

import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.util.FileLoader;
import org.apache.logging.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static edu.caltech.ipac.util.FormatUtil.Format.*;
import static org.junit.Assert.assertEquals;

/**
 * Date: 8/5/22
 *
 * @author loi
 * @version : $
 */
public class FormatUtilTest extends ConfigTest {

    @BeforeClass
    public static void setUp() {
        if (false) Logger.setLogLevel(Level.TRACE);			// set condition to 'true' to print debugging info
    }

    @Test
    public void detect() throws IOException {
        File tfile = FileLoader.resolveFile("FileUpload-samples/VOTable/binary/binary_gaia.xml");
        assertEquals(tfile.getName(), VO_TABLE, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("FileUpload-samples/VOTable/binary2/binary2_gaia.xml");
        assertEquals(tfile.getName(), VO_TABLE, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("FileUpload-samples/VOTable/fits/starlinkVOfits.xml");
        assertEquals(tfile.getName(), VO_TABLE, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/firefly/server/query/lc/periodogramResult_212027909_filteredBJDLargeThan3270.voTbl");
        assertEquals(tfile.getName(), VO_TABLE, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("FileUpload-samples/fits/imagetable/hpacs1342250905_00hps3d_00.fits");
        assertEquals(tfile.getName(), FITS, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("FileUpload-samples/fits/moc/nicmos.fits");
        assertEquals(tfile.getName(), FITS, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("FileUpload-samples/fits/multiimage/n8t801pxq_cal.fits");
        assertEquals(tfile.getName(), FITS, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/firefly/server/query/ptf-lc.tbl");
        assertEquals(tfile.getName(), IPACTABLE, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/firefly/server/util/tables/IpacTableTest.tbl");
        assertEquals(tfile.getName(), IPACTABLE, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("region-test-files/test5_global.reg");
        assertEquals(tfile.getName(), REGION, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("region-test-files/footprint_upload/gaia.reg");
        assertEquals(tfile.getName(), REGION, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/firefly/server/util/tables/IpacTableTest.json");
        assertEquals(tfile.getName(), JSON, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/visualize/plot/projection/f3Header.json");
        assertEquals(tfile.getName(), JSON, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/table/cars.csv");
        assertEquals(tfile.getName(), CSV, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("FileUpload-samples/TSV/gaia_result.tsv");
        assertEquals(tfile.getName(), TSV, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/table/iris.parquet");
        assertEquals(tfile.getName(), PARQUET, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/table/table_1mil.zip.parquet");
        assertEquals(tfile.getName(), PARQUET, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/visualize/plot/f3_0_0.png");
        assertEquals(tfile.getName(), PNG, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("stripe82-testfits/calexp-005882-i1-0406.fits.gz");
        assertEquals(tfile.getName(), GZIP, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("FileUpload-samples/ErrorSamples/genindex.html");
        assertEquals(tfile.getName(), HTML, FormatUtil.detect(tfile));
    }

    @Test
    public void compressedFits() throws IOException {
        File tfile = FileLoader.resolveFile("fits-tile-compression/lossless.fits");
        assertEquals(tfile.getName(), FITS, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("fits-tile-compression/lossy.fits");
        assertEquals(tfile.getName(), FITS, FormatUtil.detect(tfile));
    }

    @Test
    public void unknownExt() throws IOException {
        // this is a directory
        File tfile = FileLoader.resolveFile("multi-ext-fits/with-wavelength");
        assertEquals(tfile.getName(), UNKNOWN, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("stripe82-testfits/put-data-here.txt");
        assertEquals(tfile.getName(), UNKNOWN, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/visualize/plot/projection/ProjectionTestInfo.txt");
        assertEquals(tfile.getName(), UNKNOWN, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/table/cars.ul");
        assertEquals(tfile.getName(), CSV, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/table/iris.csv");
        assertEquals(tfile.getName(), PARQUET, FormatUtil.detect(tfile));
    }

    @Test
    public void mimeType() {
        File tfile = FileLoader.resolveFile("FileUpload-samples/VOTable/binary/binary_gaia.xml");
        assertEquals(tfile.getName(), "text/xml", FormatUtil.getMimeType(tfile).mime());

        tfile = FileLoader.resolveFile("FileUpload-samples/VOTable/fits/starlinkVOfits.xml");
        assertEquals(tfile.getName(), "text/xml", FormatUtil.getMimeType(tfile).mime());

        tfile = FileLoader.resolveFile("edu/caltech/ipac/firefly/server/query/ptf-lc.tbl");
        assertEquals(tfile.getName(), "text/plain", FormatUtil.getMimeType(tfile).mime());
    }
}
