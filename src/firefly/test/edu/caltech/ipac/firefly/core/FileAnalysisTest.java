/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import edu.caltech.ipac.TestCategory;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.table.DataGroup;
import org.apache.logging.log4j.Level;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author loi
 * @version $Id: IpacTableParser.java,v 1.18 2011/12/08 19:34:02 loi Exp $
 */
public class FileAnalysisTest extends ConfigTest {

    private static File voTable = FileLoader.resolveFile("LSSTFoorprintSources/combined_sources_and_footprints_5000.xml");      // 44 MB
    private static File ipacTable = FileLoader.resolveFile("edu/caltech/ipac/firefly/server/util/tables/50k.tbl");              // 21 MB
    private static File multiImage = FileLoader.resolveFile("FileUpload-samples/fits/multiimage/mips_IER_7735552_ier600_A24_P24_10s.cal.fits");              // 28 MB
    private static File fitsTables = FileLoader.resolveFile("FileUpload-samples/fits/imagetable/calexp-005882-i1-0406.fits");              // 29 MB
    private static File csvTable = FileLoader.resolveFile("FileUpload-samples/CSV/gaia_result.csv");        //112 KB

    @BeforeClass
    public static void setUp() {
        // needed when dealing with code running in a server's context, ie  SearchProcessor, RequestOwner, etc.
        setupServerContext(null);
    }

    @Test
    @Ignore
    public void testBrief() throws Exception {

//TODO This test doesn't make sense since the type Brief return Nomal in FileAnalysis.analyse(), Loi to check...Ignore for now.

        // brief reports only the bare minimum..  path, size, type and descriptions
        // The following tests is to ensure that all analyzers(fits, votable, ipactable, and csv/tsv) capture this information correctly
        FileAnalysisReport.ReportType reportType = FileAnalysisReport.ReportType.Brief;

        FileAnalysisReport report= FileAnalysis.analyze(voTable, reportType);
        assertEquals(FileAnalysisReport.ReportType.Brief, report.getType());
        assertEquals(voTable.getPath(), report.getFilePath());
        assertEquals(voTable.length(), report.getFileSize());
        assertEquals(1, report.getParts().size());
        assertEquals(FileAnalysisReport.Type.Table, report.getParts().get(0).getType());
        assertEquals("VOTable (271 cols x 5000 rows)", report.getParts().get(0).getDesc());

        report= FileAnalysis.analyze(ipacTable, reportType);
        assertEquals(FileAnalysisReport.ReportType.Brief, report.getType());
        assertEquals(ipacTable.getPath(), report.getFilePath());
        assertEquals(ipacTable.length(), report.getFileSize());
        assertEquals(1, report.getParts().size());
        assertEquals(FileAnalysisReport.Type.Table, report.getParts().get(0).getType());
        assertEquals("IPAC Table (25 cols x 49933 rows)", report.getParts().get(0).getDesc());

        report= FileAnalysis.analyze(fitsTables, reportType);
        assertEquals(FileAnalysisReport.ReportType.Brief, report.getType());
        assertEquals(fitsTables.getPath(), report.getFilePath());
        assertEquals(fitsTables.length(), report.getFileSize());
        assertEquals(1, report.getParts().size());
        assertEquals(FileAnalysisReport.Type.HeaderOnly, report.getParts().get(0).getType());
        assertEquals("Primary", report.getParts().get(0).getDesc());

        report= FileAnalysis.analyze(multiImage, reportType);
        assertEquals(FileAnalysisReport.ReportType.Brief, report.getType());
        assertEquals(multiImage.getPath(), report.getFilePath());
        assertEquals(multiImage.length(), report.getFileSize());
        assertEquals(1, report.getParts().size());
        assertEquals(FileAnalysisReport.Type.HeaderOnly, report.getParts().get(0).getType());
        assertEquals("Primary", report.getParts().get(0).getDesc());

        report= FileAnalysis.analyze(csvTable, reportType);
        assertEquals(FileAnalysisReport.ReportType.Brief, report.getType());
        assertEquals(csvTable.getPath(), report.getFilePath());
        assertEquals(csvTable.length(), report.getFileSize());
        assertEquals(1, report.getParts().size());
        assertEquals(FileAnalysisReport.Type.Table, report.getParts().get(0).getType());
        assertEquals("CSVFormat (6 cols x 1000 rows)", report.getParts().get(0).getDesc());
    }

    @Test
    public void testNormal() throws Exception {
        // normal reports on individual parts but without details
        FileAnalysisReport.ReportType reportType = FileAnalysisReport.ReportType.Normal;

        FileAnalysisReport report= FileAnalysis.analyze(voTable, reportType);
        assertEquals(FileAnalysisReport.ReportType.Normal, report.getType());
        assertEquals(voTable.getPath(), report.getFilePath());
        assertEquals(voTable.length(), report.getFileSize());
        assertEquals(1, report.getParts().size());
        assertEquals(FileAnalysisReport.Type.Table, report.getParts().get(0).getType());
        assertEquals("VOTable (271 cols x 5000 rows)", report.getParts().get(0).getDesc());

        File wNrows = FileLoader.resolveFile("/votable-samples/stilbinary.xml");                 // VOTable with nrows
        report= FileAnalysis.analyze(wNrows, reportType);
        assertEquals(FileAnalysisReport.Type.Table, report.getParts().get(0).getType());
        assertEquals("Main Information Table for NED objects within 1.000 arcmin of object MESSIER 031 (17 cols x 165 rows)", report.getParts().get(0).getDesc());        // first table

        report= FileAnalysis.analyze(ipacTable, reportType);
        assertEquals(FileAnalysisReport.ReportType.Normal, report.getType());
        assertEquals(ipacTable.getPath(), report.getFilePath());
        assertEquals(ipacTable.length(), report.getFileSize());
        assertEquals(1, report.getParts().size());
        assertEquals(FileAnalysisReport.Type.Table, report.getParts().get(0).getType());
        assertEquals("IPAC Table (25 cols x 49933 rows)", report.getParts().get(0).getDesc());

        report= FileAnalysis.analyze(csvTable, reportType);
        assertEquals(FileAnalysisReport.ReportType.Normal, report.getType());
        assertEquals(csvTable.getPath(), report.getFilePath());
        assertEquals(csvTable.length(), report.getFileSize());
        assertEquals(1, report.getParts().size());
        assertEquals(FileAnalysisReport.Type.Table, report.getParts().get(0).getType());
        assertEquals("CSV (6 cols x 1000 rows)", report.getParts().get(0).getDesc());

        // files with multiple parts
        // --------------------------------------------------------------------------------------------
        File multiVoTable = FileLoader.resolveFile("FileUpload-samples/VOTable/tabledata/multiTables_Ned.xml");    // VOTable with multiple parts
        report= FileAnalysis.analyze(multiVoTable, reportType);
        assertEquals(826, report.getParts().size());
        assertEquals(FileAnalysisReport.Type.Table, report.getParts().get(0).getType());
        assertEquals("Main Information Table for NED objects within 1.000 arcmin of object MESSIER 031 (17 cols x 165 rows)", report.getParts().get(0).getDesc());       // first table
        assertEquals("Table of all names in NED for MESSIER 031 (2 cols x 49 rows)", report.getParts().get(1).getDesc());                                                // second table
        assertEquals("Table of External Links for the  SSTSL2 J004244.22+411708.6 (3 cols x 12 rows)", report.getParts().get(825).getDesc());                            // last table

        // mixed images and table in a fits file
        report= FileAnalysis.analyze(fitsTables, reportType);
        assertEquals(FileAnalysisReport.ReportType.Normal, report.getType());
        assertEquals(fitsTables.getPath(), report.getFilePath());
        assertEquals(fitsTables.length(), report.getFileSize());
        assertEquals(11, report.getParts().size());
        assertEquals(FileAnalysisReport.Type.HeaderOnly, report.getParts().get(0).getType());
        assertNull(report.getParts().get(0).getDesc());
        assertNotNull(report.getParts().get(1).getDesc());

        assertEquals(FileAnalysisReport.Type.Table, report.getParts().get(4).getType());
        assertEquals(" 7 cols x 12 rows ", report.getParts().get(4).getDesc());

        assertEquals(FileAnalysisReport.Type.Table, report.getParts().get(8).getType());
        assertEquals(" 3 cols x 4 rows ", report.getParts().get(8).getDesc());

        assertEquals(FileAnalysisReport.Type.Table, report.getParts().get(10).getType());
        assertEquals(" 4 cols x 1 rows ", report.getParts().get(10).getDesc());

        // multiple images in a fits file
        report= FileAnalysis.analyze(multiImage, reportType);
        assertEquals(FileAnalysisReport.ReportType.Normal, report.getType());
        assertEquals(multiImage.getPath(), report.getFilePath());
        assertEquals(multiImage.length(), report.getFileSize());
        assertEquals(62, report.getParts().size());
        assertEquals(FileAnalysisReport.Type.HeaderOnly, report.getParts().get(0).getType());
        assertNull(report.getParts().get(0).getDesc());
        assertNotNull(report.getParts().get(1).getDesc());
        assertNotNull(report.getParts().get(61).getDesc());
    }

    @Test
    public void testDetails() throws Exception {
        // details report on all of the above plus additional information like fits/image/table headers plus columns, links, and groups information if it's a table.
        FileAnalysisReport.ReportType reportType = FileAnalysisReport.ReportType.Details;

        // a VO table with one table... has headers
        FileAnalysisReport report= FileAnalysis.analyze(voTable, reportType);
        DataGroup details = report.getParts().get(0).getDetails();
        // test headers
        assertEquals("true", details.getAttribute("contains_lsst_footprints"));
        assertEquals("true", details.getAttribute("contains_lsst_measurements"));
        // test column info
        assertEquals(271, details.size());       // number of columns
        assertEquals("coord_ra", details.getData("name", 2));       // coord_ra column info
        assertEquals("double", details.getData("type", 2));
        assertEquals("rad", details.getData("unit", 2));
        assertEquals("base_SdssCentroid_xErr", details.getData("name", 13));       // base_SdssCentroid_xErr column info
        assertEquals("float", details.getData("type", 13));
        assertEquals("pix", details.getData("unit", 13));

        // an IPAC Table
        report= FileAnalysis.analyze(ipacTable, reportType);
        details = report.getParts().get(0).getDetails();
        // test headers
        assertEquals("J2000", details.getAttribute("EQUINOX"));
        assertEquals("IPAC Infrared Science Archive (IRSA), Caltech/JPL", details.getAttribute("ORIGIN"));
        // test column info
        assertEquals(25, details.size());       // number of columns
        assertEquals("id", details.getData("name", 0));       // id column info
        assertEquals("int", details.getData("type", 0));
        assertEquals("number", details.getData("unit", 0));
        assertEquals("mv", details.getData("name", 24));       // mv column info
        assertEquals("double", details.getData("type", 13));
        assertEquals("mag", details.getData("unit", 13));

        // csv table
        report= FileAnalysis.analyze(csvTable, reportType);
        details = report.getParts().get(0).getDetails();
        // test column info  -- no meta or column info except for column's name
        assertEquals(6, details.size());       // number of columns
        assertEquals("source_id", details.getData("name", 0));
        assertEquals("pmra", details.getData("name", 3));
        assertEquals("parallax", details.getData("name", 5));

        // votable with multiple parts
        File multiVoTable = FileLoader.resolveFile("FileUpload-samples/VOTable/tabledata/multiTables_Ned.xml");    // VOTable with multiple parts
        report= FileAnalysis.analyze(multiVoTable, reportType);
        details = report.getParts().get(0).getDetails();        // first of 826 parts
        // test params
        assertEquals("J2000.0", details.getParam("Equinox").getStringValue());
        assertEquals("Equatorial", details.getParam("CoordSystem").getStringValue());
        // test column info
        assertEquals(17, details.size());       // number of columns
        assertEquals("Object Name", details.getData("name", 1));       // Object Name column info
        assertEquals("char", details.getData("type", 1));
        assertEquals(null, details.getData("unit", 1));
        assertEquals("RA(deg)", details.getData("name", 2));       // mv column info
        assertEquals("double", details.getData("type", 2));
        assertEquals("degrees", details.getData("unit", 2));

        // multiple images in a fits file
        report= FileAnalysis.analyze(multiImage, reportType);
        assertEquals(62,report.getParts().size());     // number of extensions
        details = report.getParts().get(0).getDetails();        // first of 62 parts
        // test extension headers
        assertEquals(185, details.size());       // number of headers
        assertEquals("SIMPLE", details.getData("key", 0));
        assertEquals("T", details.getData("value", 0));
        assertEquals("DATE", details.getData("key", 42));
        assertEquals("2003-11-02T20:07:39", details.getData("value", 42));
        assertEquals("DO_SGOUT", details.getData("key", 178));
        assertEquals("No", details.getData("value", 178));

    }


    @Category({TestCategory.Perf.class})
    @Test
    public void perfTest() throws Exception {

        Logger.setLogLevel(Level.TRACE, "edu.caltech");     // exclude numerous nom.tam warnings
        StopWatch.getInstance().start("perfTestMidSize");
        for(int i=0; i < 10; i++) {
            StopWatch.getInstance().start("voTable");
            FileAnalysis.analyze(voTable, FileAnalysisReport.ReportType.Details);
            StopWatch.getInstance().stop("voTable");

            StopWatch.getInstance().start("ipacTable");
            FileAnalysis.analyze(ipacTable, FileAnalysisReport.ReportType.Details);
            StopWatch.getInstance().stop("ipacTable");

            StopWatch.getInstance().start("fitsTables");
            FileAnalysis.analyze(fitsTables, FileAnalysisReport.ReportType.Details);
            StopWatch.getInstance().stop("fitsTables");

            StopWatch.getInstance().start("multiImage");
            FileAnalysis.analyze(multiImage, FileAnalysisReport.ReportType.Details);
            StopWatch.getInstance().stop("multiImage");

            StopWatch.getInstance().start("csvTable");
            FileAnalysis.analyze(csvTable, FileAnalysisReport.ReportType.Details);
            StopWatch.getInstance().stop("csvTable");
        }

        StopWatch.getInstance().printLog("voTable", StopWatch.Unit.SECONDS);
        StopWatch.getInstance().printLog("ipacTable", StopWatch.Unit.SECONDS);
        StopWatch.getInstance().printLog("fitsTables", StopWatch.Unit.SECONDS);
        StopWatch.getInstance().printLog("multiImage", StopWatch.Unit.SECONDS);
        StopWatch.getInstance().printLog("csvTable", StopWatch.Unit.SECONDS);

// it's slower after the changes, but it's still very fast.  It has additional info that may be used when needed.
//        0    [main] INFO  console  - voTable ran 10 times., Elapsed Time: 0.5600 SECONDS., Total time is 6.3520 SECONDS, Avg time is 0.6352 SECONDS.
//        1    [main] INFO  console  - ipacTable ran 10 times., Elapsed Time: 0.0010 SECONDS., Total time is 0.0080 SECONDS, Avg time is 0.0008 SECONDS.
//        1    [main] INFO  console  - fitsTables ran 10 times., Elapsed Time: 0.0020 SECONDS., Total time is 0.0680 SECONDS, Avg time is 0.0068 SECONDS.
//        1    [main] INFO  console  - multiImage ran 10 times., Elapsed Time: 0.0110 SECONDS., Total time is 0.1690 SECONDS, Avg time is 0.0169 SECONDS.
//        1    [main] INFO  console  - csvTable ran 10 times., Elapsed Time: 0.0010 SECONDS., Total time is 0.0110 SECONDS, Avg time is 0.0011 SECONDS.
    }


    @Category({TestCategory.Perf.class})
    @Test
    public void beforeRefactor() throws Exception {

//        StopWatch.getInstance().start("perfTestMidSize").enable().setLogger(Logger.getLogger("console"));
//        for(int i=0; i < 10; i++) {
//            StopWatch.getInstance().start("voTable");
//            AnyFileUpload.createAnalysisResult(new UploadFileInfo("voTable", voTable, voTable.getName(), "test"));
//            StopWatch.getInstance().stop("voTable");
//
//            StopWatch.getInstance().start("ipacTable");
//            AnyFileUpload.createAnalysisResult(new UploadFileInfo("ipacTable", ipacTable, ipacTable.getName(), "test"));
//            StopWatch.getInstance().stop("ipacTable");
//
//            StopWatch.getInstance().start("fitsTables");
//            AnyFileUpload.createAnalysisResult(new UploadFileInfo("fitsTables", fitsTables, fitsTables.getName(), "test"));
//            StopWatch.getInstance().stop("fitsTables");
//
//            StopWatch.getInstance().start("multiImage");
//            AnyFileUpload.createAnalysisResult(new UploadFileInfo("multiImage", multiImage, multiImage.getName(), "test"));
//            StopWatch.getInstance().stop("multiImage");
//
//            StopWatch.getInstance().start("csvTable");
//            AnyFileUpload.createAnalysisResult(new UploadFileInfo("csvTable", csvTable, csvTable.getName(), "test"));
//            StopWatch.getInstance().stop("csvTable");
//        }

//        StopWatch.getInstance().printLog("voTable", StopWatch.Unit.SECONDS);
//        StopWatch.getInstance().printLog("ipacTable", StopWatch.Unit.SECONDS);
//        StopWatch.getInstance().printLog("fitsTables", StopWatch.Unit.SECONDS);
//        StopWatch.getInstance().printLog("multiImage", StopWatch.Unit.SECONDS);
//        StopWatch.getInstance().printLog("csvTable", StopWatch.Unit.SECONDS);

//        0    [main] INFO  console  - voTable ran 10 times., Elapsed Time: 0.7180 SECONDS., Total time is 6.7820 SECONDS, Avg time is 0.6782 SECONDS.
//        1    [main] INFO  console  - ipacTable ran 10 times., Elapsed Time: 0.0010 SECONDS., Total time is 0.0020 SECONDS, Avg time is 0.0002 SECONDS.
//        1    [main] INFO  console  - fitsTables ran 10 times., Elapsed Time: 0.0020 SECONDS., Total time is 0.0620 SECONDS, Avg time is 0.0062 SECONDS.
//        1    [main] INFO  console  - multiImage ran 10 times., Elapsed Time: 0.0210 SECONDS., Total time is 0.4090 SECONDS, Avg time is 0.0409 SECONDS.
//        1    [main] INFO  console  - csvTable ran 10 times., Elapsed Time: 0.0000 SECONDS., Total time is 0.0010 SECONDS, Avg time is 0.0001 SECONDS.
    }

}
