package edu.caltech.ipac.firefly.server.query;
import edu.caltech.ipac.astro.IpacTableException;

import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by zhang on 10/29/15.
 */
public class HistogramProcessorTest {




    @Test
    public void testFixedBinSize() throws DataAccessException {

        //create five bin histData
        double[] histData = {
                0.00000,
                2.70000,
                2.50000,
                6.00000,
                6.20000,
                7.00000,
                4.05000,
                7.10000,
                7.30000,
                7.40000,
                10.0000,
                12.0000,
                9.20000,
                8.50000,
                12.000,
        };
        double max=12.0;
        double min=0.0;
        int nBin=6;


        int[] expectedNumPointsInBin={1, 2, 1, 6, 2,  3};
        double[] expectedBinMin={0.0, 2, 4,  6.0, 8.0, 10.0};
        double[] expectedBinMax={2, 4, 6,  8, 10, 12};
        DataGroup expectedDG = createDataGroup(expectedNumPointsInBin, expectedBinMin, expectedBinMax);

        HistogramProcessor hp = new HistogramProcessor();
        hp.setBinNumber(6);
        DataGroup outDG = hp.createHistogramTable(histData);

        validateResult(expectedDG, outDG);


    }

    private void validateResult(DataGroup expected, DataGroup calculated){
        for (int i=0; i<expected.size(); i++ ){
            Object[] expectedDJ = expected.get(i).getData();
            Object[] calculatedGJ = calculated.get(i).getData();
            for (int j=0; j<calculatedGJ.length; j++){
                if ( calculatedGJ[j] instanceof Integer){
                     Assert.assertEquals(calculatedGJ[j], expectedDJ [j]);
                }
                if ( calculatedGJ[j] instanceof Double){
                    Assert.assertEquals( ((Double) calculatedGJ[j]).doubleValue(), ( (Double) expectedDJ [j]).doubleValue(), 0.0000005);
                }
            }

        }
    }

    private DataGroup createDataGroup( int[] numPointInBin, double[] binMin, double[] binMax){
        DataType[] columns = new DataType[]{
                                                   new DataType("numInBin", Integer.class),
                                                   new DataType("binMin", Double.class),
                                                   new DataType("binMax",  Double.class)
        };


        DataGroup expectedDG =  new DataGroup("histogramTable", columns);

        for (int i=0; i<numPointInBin.length; i++){
            DataObject row = new DataObject(expectedDG);
            row.setDataElement(columns[0], numPointInBin[i]);
            row.setDataElement(columns[1], binMin[i]);
            row.setDataElement(columns[2], binMax[i]);
            expectedDG.add(row);
        }
        return expectedDG;
    }
    @Test
    /**
     * Compare with the result from https://github.com/fedhere/fedsastroutils/blob/master/bayesianblocks_fbb.py
     * The testing data generated according to https://jakevdp.github.io/blog/2012/09/12/dynamic-programming-in-python/
     * np.random.seed(0)
     * x = np.concatenate([stats.cauchy(-5, 1.8).rvs(50),
     *stats.cauchy(-4, 0.8).rvs(2000),
     * stats.cauchy(-1, 0.3).rvs(500),
     * tats.cauchy(2, 0.8).rvs(100),
     * stats.cauchy(4, 1.5).rvs(50)])
     * truncate values to a reasonable range
     * x = x[(x > -15) & (x < 15)]
     *
     *
     */


    public void testVariableBinSize() {

        double[] histData = {
                                    -4.72178176984,
                                    -3.55616908059,
                                    -4.39781898076,
                                    -4.74449599349,
                                    -5.44019536274,
                                    -4.11191090606,
                                    -5.35752960839,
                                    0.0884446941929,
                                    10.6992317554,
                                    -5.6902534341,
                                    -2.65330342232,
                                    -4.83615278107,
                                    -4.60924679225,
                                    2.55993713501,
                                    -12.9313853116,
                                    -11.4108874709,
                                    -1.89838487253,
                                    -2.84964940465,
                                    -0.840009841168,
                                    -2.53623493211,
                                    -5.21889870322,
                                    -2.81679404789,
                                    -9.61928698036,
                                    -4.15352973806,
                                    -8.72288323453,
                                    5.25057274847,
                                    -4.87625626103,
                                    -5.4944796303,
                                    -6.64247343623,
                                    -2.90271589399,
                                    -5.24954458354,
                                    -4.60694047432,
                                    -4.30275147637,
                                    -4.33854100681,
                                    -4.3073097066,
                                    5.07931462821,
                                    -3.84319332307,
                                    -5.85041712794,
                                    -5.36079452838,
                                    -3.71257991572,
                                    -14.3997518146,
                                    -3.9600150285,
                                    -3.93060853728,
                                    -7.314796098,
                                    -9.19835191646,
                                    -6.17891461465,
                                    -5.82152399229,
                                    2.17934050528,
                                    1.8437460783,
                                    -0.409371435745,
                                    0.961096913729,
                                    0.558880966133,
                                    2.41751009574,
                                    1.216376634,
                                    1.91501216543,
                                    1.17147755347,
                                    0.53358085331,
                                    -0.213895409664,
                                    2.42786612233,
                                    0.274413309187,
                                    0.873658992513,
                                    1.64999847704,
                                    3.26933569984,
                                    -0.540641523301,
                                    3.43319794841,
                                    -0.56886536703,
                                    12.7976938682,
                                    1.92095615358,
                                    12.938348639,
                                    2.27346752992,
                                    2.74777459197,
                                    -4.46528068306,
                                    1.34997941924,
                                    -0.0169296033283,
                                    1.40381208642,
                                    -0.0444058397939,
                                    1.48516551134,
                                    1.77915257735,
                                    -1.91583868155,
                                    2.55281048869,
                                    2.1698739551,
                                    1.27384341802,
                                    2.05853284439,
                                    -0.631575625635,
                                    2.19458072379,
                                    5.54218704611,
                                    1.487244971,
                                    2.46437579347,
                                    0.179588110528,
                                    2.64641582105,
                                    1.37706217561,
                                    0.766904320312,
                                    2.22294757957,
                                    -10.6474458703,
                                    3.3425028369,
                                    2.50003093954,
                                    1.29474073934,
                                    2.72883586101,
                                    8.70296960768,
                                    1.19370793683,
                                    2.19514205819,
                                    2.2379970513,
                                    2.18477313388,
                                    1.05180800955,
                                    7.3496178203,
                                    1.86587568955,
                                    3.52724266326,
                                    2.57923684223,
                                    1.40886588859,
                                    3.20791714674,
                                    1.73031992056,
                                    4.04121000958,
                                    2.20881858454,
                                    4.05320305394,
                                    2.55303135337,
                                    2.68437055372,
                                    2.00332855403,
                                    7.76163609186,
                                    2.38877383261,
                                    1.80489091508,
                                    2.2778191009,
                                    -11.2515288836,
                                    1.42479157102,
                                    2.44037185625,
                                    1.37976873663,
                                    2.31098689107,
                                    1.81792677505,
                                    0.235208971973,
                                    1.4121441847,
                                    2.17872859329,
                                    2.23480201916,
                                    2.1902703381,
                                    2.41780591976,
                                    2.41430139985,
                                    1.82491814871,
                                    4.37418897735,
                                    1.64650957172,
                                    1.83659371532,
                                    4.26493321688,
                                    3.14740964902,
                                    2.59630042875,
                                    -0.456188373802,
                                    5.09490140894,
                                    2.63779611466,
                                    1.04342734695,
                                    7.41105857398,
                                    1.32141781413,
                                    4.56981816489,
                                    0.340365745189,
                                    6.89892828756,
                                    6.16768235812,
                                    4.33084311976,
                                    3.54977848596,
                                    -2.79408234439,
                                    5.07140885735,
                                    3.77950722087,
                                    5.25731199901,
                                    7.36097196203,
                                    7.08154172439,
                                    3.29402731637,
                                    5.32234884997,
                                    1.49302155609,
                                    4.09927725055,
                                    -4.7014232285,
                                    1.93537971933,
                                    5.98088142159,
                                    2.23166392031,
                                    3.20791149633,
                                    10.5255958615,
                                    5.12192258157,
                                    -10.9462130026,
                                    1.36433896086,
                                    4.60196583012,
                                    4.37124430934,
                                    2.38132124837,
                                    11.1542165195,
                                    4.5612409942,
                                    4.16862056825,
                                    4.43533028925,
                                    5.3234502435,
                                    2.99394455227,
                                    3.5033354299,
                                    2.06425032659,
                                    1.73501354726,
                                    12.4956765666,
                                    5.40461549911,
                                    3.95502472437,
                                    2.27046621065,
                                    2.54050701184,
                                    -4.1366608831,
                                    3.68649685616
        };

        Arrays.sort(histData);
        double max=histData[histData.length-1];
        double min=histData[0];
        int nbin=6;
        double delta =( max -min)/100*nbin;
        //double[] expectedBins={ -14.3997518146, -6.0146658712, 1.1825927451, 2.7383052264, 5.4734012726, 12.938348639};
        boolean showEmptyBin=false;

        int[] expectedNumPointInBin = {12, 59, 68, 36, 16};


        double[] expectedBinMin = {

                -14.3997518146,
                -6.014665871295,
                1.18259274515,
                2.73830522649,
                5.47340127261
                 };


        double[] expectedBinMax = {

                -6.01466587129,
                1.18259274515,
                2.73830522649,
                5.47340127261,
                14.57863466624
        };


        try {

            HistogramProcessor hp = new HistogramProcessor();
            DataGroup outDG = hp.createHistogramTable(histData);

            DataGroup expectedDG = createDataGroup(expectedNumPointInBin, expectedBinMin, expectedBinMax );
            validateResult(expectedDG, outDG);

        }
        catch (Exception ex){
            ex.printStackTrace();
        }
            ;


    }
    public static void main(String args[]) throws IpacTableException, IOException, DataAccessException {
        HistogramProcessorTest myTest = new HistogramProcessorTest();

        myTest.testFixedBinSize();
        myTest.testVariableBinSize();



    }

}
