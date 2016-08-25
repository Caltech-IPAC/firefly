/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.util.download.URLDownload;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Created by ejoliet on 8/23/16.
 */
public class LightCurveProcessorTest {


    private static PeriodogramAPIRequestTest req;
    private static File rawTable;


    @BeforeClass
    public static void setUp() {
        req = new PeriodogramAPIRequestTest();
        try {
            rawTable = File.createTempFile("phasefolded-temp-", ".tbl", new File("."));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetPeriodogram() {

        boolean deleteOnExit = true;
        LightCurveHandler t = new IrsaLightCurveHandler() {
            @Override
            protected URL buildUrl(PeriodogramAPIRequest req) throws MalformedURLException {
                return new URL(req.getResultTable());
            }

            @Override
            protected File makeResultTempFile(RESULT_TABLES_IDX resultTable) throws IOException {
                File tmpFile = File.createTempFile("period-test-", ".tbl", new File("."));
                if (deleteOnExit) {
                    tmpFile.deleteOnExit();
                }
                return tmpFile;
            }

            @Override
            protected File makeApiResultTempFile() throws IOException {
                File tmpFile = File.createTempFile("votable-test-", ".xml", new File("."));
                if (deleteOnExit) {
                    tmpFile.deleteOnExit();
                }
                return tmpFile;
            }
        };

        File p = t.getPeriodogramTable(req);

        try {
            DataGroup inDataGroup = IpacTableReader.readIpacTable(p, "periodogram");
            List<DataObject> dgjList = inDataGroup.values();
            DataType[] inColumns = inDataGroup.getDataDefinitions();
            Assert.assertTrue(inColumns.length + " is not 2", inColumns.length == 2);
            Assert.assertTrue("expected " + dgjList.size(), dgjList.size() == 390);
        } catch (IpacTableException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testGetPeaks() {

        boolean deleteOnExit = true;
        LightCurveHandler t = new IrsaLightCurveHandler() {
            @Override
            protected URL buildUrl(PeriodogramAPIRequest req) throws MalformedURLException {
                return new URL(req.getResultTable());
            }

            @Override
            protected File makeResultTempFile(RESULT_TABLES_IDX resultTable) throws IOException {
                File tmpFile = File.createTempFile("peaks-test-", ".tbl", new File("."));
                if (deleteOnExit) {
                    tmpFile.deleteOnExit();
                }
                return tmpFile;
            }

            @Override
            protected File makeApiResultTempFile() throws IOException {
                File tmpFile = File.createTempFile("votable-test-", ".xml", new File("."));
                if (deleteOnExit) {
                    tmpFile.deleteOnExit();
                }
                return tmpFile;
            }
        };

        File peaks = t.getPeaksTable(req);

        try {
            DataGroup inPeaksDataGroup = IpacTableReader.readIpacTable(peaks, "peaks");
            DataType[] inColumns = inPeaksDataGroup.getDataDefinitions();
            Assert.assertTrue(inColumns.length + " is not 5", inColumns.length == 5);
            List<DataObject> dgjList = inPeaksDataGroup.values();
            inColumns = inPeaksDataGroup.getDataDefinitions();
            Assert.assertTrue("expected " + dgjList.size(), dgjList.size() == req.getNumberPeaks());
        } catch (IpacTableException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testPhaseFoldedCurve() {

        boolean deleteOnExit = true;
        if(deleteOnExit){
            rawTable.deleteOnExit();
        }
        IrsaLightCurveHandler t = new IrsaLightCurveHandler() {

            private File getUnchangedTable(File tbl){
                return tbl;
            }
            @Override
            public File toPhaseFoldedTable(File tbl, float period) {
                //return same table for now, once implemented, plug the right code here
                return getUnchangedTable(tbl);
            }
        };

        DataGroup inDataGroup = null;
        try {
            inDataGroup = IpacTableReader.readIpacTable(new File(req.getLcSource()), "lc_raw");
        } catch (IpacTableException e) {
            e.printStackTrace();
        }
        List<DataObject> dgjListOrigin = inDataGroup.values();
        DataType[] inColumns = inDataGroup.getDataDefinitions();

        File p = t.toPhaseFoldedTable(new File(req.getLcSource()), req.getPeriod());

        try {
            inDataGroup = IpacTableReader.readIpacTable(p, "phasefolded");
            List<DataObject> dgjList = inDataGroup.values();
            DataType[] inColumnsPhaseFolded = inDataGroup.getDataDefinitions();
            //should be one more extra column (Phase)
            Assert.assertTrue(inColumns.length + " is not correct", inColumns.length == inColumnsPhaseFolded.length); //-1 when phasefold is introduced, test it
            Assert.assertTrue("expected " + dgjList.size(), dgjList.size() == dgjListOrigin.size());
        } catch (IpacTableException e) {
            e.printStackTrace();
        }

    }

    /**
     * Class that will support a request test
     */
    static class PeriodogramAPIRequestTest extends PeriodogramAPIRequest {

        @Override
        public float getPeriod() {
            return 0.5f;
        }

        @Override
        public String getLcSource() {
            try {
                URL demo = new URL("http://web.ipac.caltech.edu/staff/ejoliet/demo/AllWISE-MEP-m82-2targets-10arsecs.tbl");
                URLConnection uc = URLDownload.makeConnection(demo);
                URLDownload.getDataToFile(uc, rawTable);
                return rawTable.getAbsolutePath();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (FailedRequestException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public int getNumberPeaks() {
            return 50;
        }

        /**
         * @return the built url api
         */
        @Override
        public String getUrl() {
            //As for the test, we return the result table
            return getResultTable();
        }

        @Override
        public String getResultTable() {
            return "http://web.ipac.caltech.edu/staff/ejoliet/demo/vo-nexsci-result-sample.xml";
        }
    }
}
