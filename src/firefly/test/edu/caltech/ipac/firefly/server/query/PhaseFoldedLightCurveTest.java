package edu.caltech.ipac.firefly.server.query;

import edu.caltech.ipac.astro.IpacTableException;
import edu.caltech.ipac.firefly.server.query.lc.PhaseFoldedLightCurve;
import edu.caltech.ipac.firefly.util.FileLoader;
import edu.caltech.ipac.util.DataGroup;
import nom.tam.fits.FitsException;
import org.junit.Assert;
import org.junit.Test;


/**
 * Created by ymei on 8/25/16.
 * 10/19/16
 *  DM-8028
 *    Use teh UnitTestUtility to load file
 */
public class PhaseFoldedLightCurveTest {

    private static final float period = 0.140630f;
    private static final String timeColName = "mjd";
    private static final String phaseColName = "phase";
    private static final double sumExpected = 21.4209;
    private static final double delta = 0.001;
    private String fileName = "/AllWISE-MEP-m82-2targets-10arsecs-oneTarget.tbl";

    @Test
    public void testPhaseFoldedLC() throws FitsException, ClassNotFoundException {

        try {

            DataGroup dataGroup = FileLoader.loadIpacTable( PhaseFoldedLightCurveTest.class, fileName );

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
        }
    }
}
