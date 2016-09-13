package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.server.query.lc.PhaseFoldedLightCurve;
import edu.caltech.ipac.firefly.server.util.ipactable.DataGroupReader;
import edu.caltech.ipac.util.DataGroup;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by ymei on 8/25/16.
 */
public class PhaseFoldedLightCurveTest {

    private static final String TEST_ROOT = "test"+ File.separatorChar;
    //private static String inFileName = "/hydra/cm/firefly/src/firefly/test/edu/caltech/ipac/firefly/server/query/AllWISE-MEP-m82-2targets-10arsecs-oneTarget.tbl";
    private static final String inFileName = "edu/caltech/ipac/firefly/server/query/AllWISE-MEP-m82-2targets-10arsecs-oneTarget.tbl";
    private static String inFileFullName =  TEST_ROOT + inFileName;

    private static final float period = 0.140630f;
    private static final String timeColName = "mjd";
    private static final String phaseColName = "phase";
    private static final double sumExpected = 21.4209;
    private static final double delta = 0.001;

    @Test
    public void testPhaseFoldedLC() {

        try {
            File inFile = new File(inFileFullName);

            //Get a datagroup from the IPAC table file:
            DataGroup dataGroup = DataGroupReader.readAnyFormat(inFile);

            //Add the new phase column:
            PhaseFoldedLightCurve pflc = new PhaseFoldedLightCurve();
            pflc.addPhaseCol(dataGroup, period, timeColName);

            //Check sum:
            float sum = 0;
            for (int i = 0; i < dataGroup.size(); i++) {
                    sum += (double)dataGroup.get(i).getDataElement(phaseColName);
            }
            Assert.assertEquals(sum, sumExpected, delta);

        } catch (IpacTableException e) {
            e.printStackTrace();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }
}
