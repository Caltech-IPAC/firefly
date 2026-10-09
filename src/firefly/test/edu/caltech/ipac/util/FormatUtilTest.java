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
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPOutputStream;

import static edu.caltech.ipac.util.FormatUtil.Format.CSV;
import static edu.caltech.ipac.util.FormatUtil.Format.FITS;
import static edu.caltech.ipac.util.FormatUtil.Format.GZIP;
import static edu.caltech.ipac.util.FormatUtil.Format.HTML;
import static edu.caltech.ipac.util.FormatUtil.Format.IPACTABLE;
import static edu.caltech.ipac.util.FormatUtil.Format.JSON;
import static edu.caltech.ipac.util.FormatUtil.Format.PARQUET;
import static edu.caltech.ipac.util.FormatUtil.Format.PNG;
import static edu.caltech.ipac.util.FormatUtil.Format.REGION;
import static edu.caltech.ipac.util.FormatUtil.Format.TEXT;
import static edu.caltech.ipac.util.FormatUtil.Format.TSV;
import static edu.caltech.ipac.util.FormatUtil.Format.UNKNOWN;
import static edu.caltech.ipac.util.FormatUtil.Format.VO_TABLE;
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
        assertEquals(tfile.getName(), FITS, FormatUtil.detect(tfile));

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
        assertEquals(tfile.getName(), TEXT, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/visualize/plot/projection/ProjectionTestInfo.txt");
        assertEquals(tfile.getName(), TEXT, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/table/cars.ul");
        assertEquals(tfile.getName(), CSV, FormatUtil.detect(tfile));

        tfile = FileLoader.resolveFile("edu/caltech/ipac/table/iris.csv");
        assertEquals(tfile.getName(), PARQUET, FormatUtil.detect(tfile));
    }

    @Test
    public void csvSingleColumn() throws IOException {
        String content = """
                ssObjectId_user
                5977535780727431144
                4350915375550808373
                5977535780727431144""".stripIndent();
        Path tmp = Files.createTempFile("test", ".cat");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, content);
        assertEquals("singleColumnCsv", CSV, FormatUtil.detect(tmp.toFile()));
    }

    @Test
    public void csvMissingData() throws IOException {
        String content = """
                ssObjectId_user, bad_column_with_missing_data_at_row1
                5977535780727431144
                4350915375550808373, 2
                5977535780727431144, 3""".stripIndent();
        Path tmp = Files.createTempFile("test", ".cat");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, content);
        assertEquals("csvMissingData", UNKNOWN, FormatUtil.detect(tmp.toFile()));
    }

    @Test
    public void csvMissingHeader() throws IOException {
        String content = """
                ssObjectId_user
                5977535780727431144, 2
                4350915375550808373, 3
                5977535780727431144, 4""".stripIndent();
        Path tmp = Files.createTempFile("test", ".cat");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, content);
        assertEquals("csvMissingHeader", UNKNOWN, FormatUtil.detect(tmp.toFile()));
    }

    @Test
    public void tsvWith2Cols() throws IOException {
        String content = """
                ssObjectId_user\trow_num
                5977535780727431144\t1
                4350915375550808373\t2
                5977535780727431144\t3""".stripIndent();
        Path tmp = Files.createTempFile("test", ".cat");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, content);
        assertEquals("tsvWith2Cols", TSV, FormatUtil.detect(tmp.toFile()));
    }

    @Test
    public void falsePositive() throws IOException {
        String content = """
                ssObjectId_user\trow_num
                5977535780727431144\t1\t2
                4350915375550808373\t2
                5977535780727431144\t3""".stripIndent();
        Path tmp = Files.createTempFile("test", ".cat");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, content);
        // badly formatted TSV that we returned as a single column CSV
        // this is unavoidable because you may have a valid one-column CSV file with a tab(s) in it
        assertEquals("falsePositive", CSV, FormatUtil.detect(tmp.toFile()));
    }

    @Test
    public void badMixedDelimiters() throws IOException {
        String content = """
                ssObjectId_user\trow_num
                5977535780727431144, 1
                4350915375550808373\t2
                5977535780727431144\t3""".stripIndent();
        Path tmp = Files.createTempFile("test", ".cat");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, content);
        assertEquals("badMixedDelimiters", UNKNOWN, FormatUtil.detect(tmp.toFile()));
    }

    @Test
    public void mimeType() throws IOException {
        File tfile = FileLoader.resolveFile("FileUpload-samples/VOTable/binary/binary_gaia.xml");
        assertEquals(tfile.getName(), VO_TABLE.mime(), FormatUtil.getMimeType(tfile).mime());

        tfile = FileLoader.resolveFile("FileUpload-samples/VOTable/fits/starlinkVOfits.xml");
        assertEquals(tfile.getName(), VO_TABLE.mime(), FormatUtil.getMimeType(tfile).mime());

        tfile = FileLoader.resolveFile("edu/caltech/ipac/firefly/server/query/ptf-lc.tbl");
        assertEquals(tfile.getName(), UNKNOWN.mime(), FormatUtil.getMimeType(tfile).mime());

        tfile = FileLoader.resolveFile("stripe82-testfits/calexp-i-0-366,0.fits.gz");
        String mtype = FormatUtil.getMimeType(tfile).mime();
        assertEquals(tfile.getName(), GZIP.mime(), mtype);
        assertEquals(tfile.getName(), FITS, FormatUtil.detect(tfile));

        // gzipped FITS must be detected by content, not by a "fit" in the file name (e.g. temp files for urls with a query string)
        Path tmp = Files.createTempFile("req_", ".ul");
        tmp.toFile().deleteOnExit();
        Files.copy(tfile.toPath(), tmp, StandardCopyOption.REPLACE_EXISTING);
        assertEquals("gzipped fits named .ul", FITS, FormatUtil.detect(tmp.toFile()));
    }

    @Test
    public void extensionDoesNotDecideFormat() throws IOException {
        // a .fits name must not decide the format; on linux /etc/mime.types maps .fits to image/fits
        Path tmp = Files.createTempFile("test", ".fits");
        tmp.toFile().deleteOnExit();
        Files.writeString(tmp, "a,b,c\n1,2,3\n4,5,6\n");
        assertEquals("csv named .fits", CSV, FormatUtil.detect(tmp.toFile()));
    }

    @Test
    public void voTableWithWrongExtension() throws IOException {
        File tfile = FileLoader.resolveFile("FileUpload-samples/VOTable/binary/binary_gaia.xml");
        for (String ext : new String[] {".csv", ".json"}) {
            Path tmp = Files.createTempFile("test", ext);
            tmp.toFile().deleteOnExit();
            Files.copy(tfile.toPath(), tmp, StandardCopyOption.REPLACE_EXISTING);
            assertEquals("votable named " + ext, VO_TABLE.mime(), FormatUtil.getMimeType(tmp.toFile()).mime());
            assertEquals("votable named " + ext, VO_TABLE, FormatUtil.detect(tmp.toFile()));
        }
    }

    @Test
    public void gzipNotFitsNamedFits() throws IOException {
        // the name says fits but the decompressed content does not
        Path tmp = Files.createTempFile("test", ".fits.gz");
        tmp.toFile().deleteOnExit();
        try (OutputStream out = new GZIPOutputStream(Files.newOutputStream(tmp))) {
            out.write("a,b,c\n1,2,3\n".getBytes());
        }
        assertEquals("gzipped csv named .fits.gz", GZIP, FormatUtil.detect(tmp.toFile()));
    }
}
