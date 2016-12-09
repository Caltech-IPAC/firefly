/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.astro.IpacTableReader;
import edu.caltech.ipac.firefly.ConfigTest;
import edu.caltech.ipac.firefly.server.query.lc.IrsaLightCurveHandler;
import edu.caltech.ipac.firefly.server.query.lc.LightCurveHandler;
import edu.caltech.ipac.firefly.server.query.lc.PeriodogramAPIRequest;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Test IRSA LC API
 */
public class LightCurveProcessorTest extends ConfigTest {


    private static PeriodogramAPIRequestTest req;


    @BeforeClass
    public static void setUp() {
        req = new PeriodogramAPIRequestTest();
    }

    @Test
    public void testGetPeriodogram() {

        boolean deleteOnExit = true;
        LightCurveHandler t = new IrsaLightCurveHandler() {

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
            Assert.assertTrue(inColumns.length + " is not 3", inColumns.length > 1);
            Assert.assertTrue("expected " + dgjList.size(), dgjList.size() > 0);
        } catch (IpacTableException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testGetPeaks() {

        boolean deleteOnExit = true;
        LightCurveHandler t = new IrsaLightCurveHandler() {

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

        File rlc = FileLoader.resolveFile(LightCurveProcessorTest.class, "/AllWISE-MEP-m82-2targets-10arsecs-oneTarget.tbl");
        boolean deleteOnExit = true;
        IrsaLightCurveHandler t = new IrsaLightCurveHandler() {
            @Override
            protected File createPhaseFoldedTempFile() throws IOException {
                File tmpFile = File.createTempFile("phase-folded-test-", ".tbl", new File("."));
                if (deleteOnExit) {
                    tmpFile.deleteOnExit();
                }
                return tmpFile;
            }
        };

        DataGroup inDataGroup = null;
        try {
            inDataGroup = IpacTableReader.readIpacTable(rlc, "lc_raw");
        } catch (IpacTableException e) {
            e.printStackTrace();
        }
        List<DataObject> dgjListOrigin = inDataGroup.values();
        DataType[] inColumns = inDataGroup.getDataDefinitions();

        File p = t.toPhaseFoldedTable(rlc, req.getPeriod(), req.getTimeColName());

        try {
            inDataGroup = IpacTableReader.readIpacTable(p, "phasefolded");
            List<DataObject> dgjList = inDataGroup.values();
            DataType[] inColumnsPhaseFolded = inDataGroup.getDataDefinitions();
            //should be one more extra column (Phase)
            Assert.assertTrue(inColumns.length + " is not correct", inColumns.length == inColumnsPhaseFolded.length - 1);
            Assert.assertTrue("expected " + dgjList.size(), dgjList.size() == dgjListOrigin.size());
            double period = req.getPeriod();
            double tzero = (Double) dgjListOrigin.get(0).getDataElement("mjd");

            int iTest = 3;
            double mjdTest = (Double) dgjListOrigin.get(iTest).getDataElement("mjd");

            double phaseTested = (Double) inDataGroup.get(iTest).getDataElement("phase");
            double phaseExpected = (mjdTest - tzero) / period - Math.floor((mjdTest - tzero) / period);

            Assert.assertEquals(phaseExpected, phaseTested, 0.0001);
        } catch (IpacTableException e) {
            e.printStackTrace();
        }

    }

    /**
     * Class that will support a request test
     */
    static class PeriodogramAPIRequestTest extends PeriodogramAPIRequest {

        private int n_peaks = 52;
        // Fake values passed from client,
        // see test properties irsa.gator.service.periodogram.keys
        private final String[] reqValues = new String[]{"x=mjd", "y=w1mpro_ep", "peaks=" + n_peaks, "alg=ls"};

        @Override
        public String getParam(String param) {
            int i = 0;
            for (String k : reqValues) {
                if (k.startsWith(param)) {
                    return reqValues[i].split("=")[1];
                }
                i++;
            }
            return "";
        }

        @Override
        public float getPeriod() {
            return 1.345f;
        }

        @Override
        public String getLcSource() {
            return "http://web.ipac.caltech.edu/staff/ejoliet/demo/AllWISE-MEP-m82-2targets-10arsecs.tbl";
        }

        @Override
        public String getDataColName() {
            return "w1mpro_ep";
        }

        @Override
        public int getNumberPeaks() {
            return n_peaks;
        }

        @Override
        public String getTimeColName() {
            return "mjd";
        }

        @Override
        public String getResultTable() {
            return "http://web.ipac.caltech.edu/staff/ejoliet/demo/vo-nexsci-result-sample-old.xml";
        }
    }

    /**
     * Could be useful to define algorithm with default parameter and use them by mapping an enum from the request to ease the URL API building
     * Example of classes below
     */

    class LombScargle implements Periodogram {


        @Override
        public AlgorithmDefinition getAlgoDef() {
            return AlgorithmDefinition.LS;
        }

        @Override
        public int getNPeaks() {
            return 50;
        }

        @Override
        public Period getPeriod() {
            return new PeriodSample();
        }

        @Override
        public double[] getAlgoValues() {
            return new double[0];
        }

        @Override
        public StepMethod getStepMethod(StepMethod.STEPMETHOD_NAME sName) {
            return new FixedPeriodMethod(0.1f);
        }
    }

    class FixedPeriodMethod implements StepMethod {

        private final float val;

        FixedPeriodMethod(float step) {
            this.val = step;
        }

        @Override
        public String getName() {
            return STEPMETHOD_NAME.FIXED_PERIOD.name();
        }

        @Override
        public float getValue() {
            return val;
        }
    }

    protected class PeriodSample implements Period {

        @Override
        public float getMin() {
            return 0;
        }

        @Override
        public float getMax() {
            return 10;
        }

        @Override
        public float getPeakValue() {
            return 2;
        }
    }
}

