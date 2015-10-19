/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.util.SUTDebug;

import java.util.Arrays;

/**
 * Creates a histogram of an image
 *
 * @author Booth Hartley
 *
 * Edit history
 * LZ 6/15/15
 *         - Renamed and rewroted the getTblArray method and commented out the eq_tbl and deq_dtbl
 *         - Only leave the bitpix = -32 and clean up the rest
 */


public class Histogram {
    private static int HISTSIZ2 = 4096;  /* full size of hist array */
    private static int HISTSIZ = 2048;     /* half size of hist array */

    private int[] hist;
    private double histMin;
    private double histBinsiz;
    private double irafMin;
    private double irafMax;



    public Histogram(float[] float1dArray, double datamin, double datamax) {

	   /*
        If the datamin or datamax is NaN, adjust them
		 */
        if (Double.isNaN(datamin) || Double.isNaN(datamax)) {
            datamax = -Double.MAX_VALUE;
            datamin = Double.MAX_VALUE;
            for (int k = 0; k < float1dArray.length; k++) {

                if (!Double.isNaN(float1dArray[k])) {
                    if (float1dArray[k] < datamin)
                        datamin = float1dArray[k];
                    if (float1dArray[k] > datamax)
                        datamax = float1dArray[k];
                }
            }
        }

        hist = new int[HISTSIZ2 + 1];

        double histDatamax = -Double.MAX_VALUE;
        double histDatamin = Double.MAX_VALUE;

        histMin = datamin;
        double histMax = datamax;
        boolean doing_redo = false;

        while (true) {

            boolean redo_flag = false;
            histBinsiz =getHistBinSize(histMax );
             //reintialize the hist to 0
            Arrays.fill(hist, 0);
            int underflowCount = 0;
            int overflowCount = 0;
            for (int k = 0; k < float1dArray.length; k++)
            {
                if (!Double.isNaN(float1dArray[k]))
                {
                   int i = (int) ((float1dArray[k] - histMin) / histBinsiz);
                      if (i<0)
                    {
                        //redo_flag = true;   /* hist_min was bad */
                        underflowCount++;
                    }
                    else if (i>HISTSIZ2)
                    {
                        //redo_flag = true;   /* hist_max was bad */
                        overflowCount++;
                    }
                    else
                    {
                        hist[i] ++;
                    }
                    if (float1dArray[k] < histDatamin)
                        histDatamin = float1dArray[k];
                    if (float1dArray[k] > histDatamax)
                        histDatamax = float1dArray[k];
                }
            }


            printeDebugInfo(histMax, underflowCount, overflowCount);
            datamin = histDatamin;
            datamax = histDatamax;

	        /* redo if more than 1% of pixels fell off histogram */
            if (underflowCount > float1dArray.length / .01)
                redo_flag = true;
            if (overflowCount > float1dArray.length / .01)
                redo_flag = true;

            /* check if we got a good spread */

            if (!redo_flag && !doing_redo) { /* don't bother checking if we already want a redo */

	           /* see what happens if we lop off top and bottom 0.05% of hist */

                int lowLimit = getLowLimit();

                int histMaxIndex = getHighSumIndex(lowLimit) + 1;

                int histMinIndex = getLowSumIndex(lowLimit);

                if (histMaxIndex==-1 || histMinIndex==-1){
                    System.out.println("the index can not be negative");
                    break;
                }

                if ((histMaxIndex - histMinIndex) < HISTSIZ) {

                    histMax = (histMaxIndex * histBinsiz) + histMin;
                    histMin = (histMinIndex * histBinsiz) + histMin;
                    redo_flag = true;   /* we can spread it out by factor of 2 */
                }
            } else {

                if (!doing_redo) {

                    histMax = datamax;
                    histMin = datamin;
                }
            }


            if (SUTDebug.isDebug())
                System.out.println("done");

            if ( !doing_redo  &&  redo_flag ) {

                if (SUTDebug.isDebug())
                    System.out.println("rebuilding histogram . . ");
                doing_redo = true;
            } else
                break;
        }

        irafMin = datamin;
        irafMax = datamax;


    }

    private double  getHistBinSize(double histMax){
        double  hbinsiz = (histMax - histMin) / HISTSIZ2;
        if (hbinsiz == 0.0)
            hbinsiz = 1.0;
        return hbinsiz;
    }
    private int getLowSumIndex(int lowLimit)  {

        int lowSum = 0;
        for (int i = 0; i < HISTSIZ2; i++) {

            lowSum += hist[i];
            if (lowSum > lowLimit) {
                return i;
            }

        }

       return -1;

    }


    private int getHighSumIndex(int lowLimit) {
        int highSum = 0;


        for (int i = HISTSIZ2; i >= 0; i--) {
            highSum += hist[i];
            if (highSum > lowLimit) {
                return i;
            }
        }
       return -1;
    }

    private int getLowLimit() {
        int goodpix = 0;
        for (int i = 0; i < HISTSIZ2; i++)
            goodpix += hist[i];

        return (int) (goodpix * 0.0005);
    }

    private void printeDebugInfo(double hist_max, int underFlowCount, int overFlowCount) {
        if (SUTDebug.isDebug()) {
            System.out.println("histMin = " + histMin);
            System.out.println("hist_max = " + hist_max);
            System.out.println("histBinsiz = " + histBinsiz);


            System.out.println("underFlowCount = " + underFlowCount +
                    "  overFlowCount = " + overFlowCount);


        }
    }



    /**
     * get_sigma
     * set DN corresponding to a sigma on the histogram
     * Code stolen from Montage mJPEG.c from Serge Monkewitz
     *
     * @param sigma_value The sigma on the histogram
     * @param round_up    Use the upper edge of the bin (versus the lower edge)
     * @return The DN value in the image corresponding to the sigma
     */
    public double get_sigma(double sigma_value, boolean round_up) {
        double lev16 = get_pct(16., round_up);
        double lev50 = get_pct(50., round_up);
        double lev84 = get_pct(84., round_up);
        double sigma = (lev84 - lev16) / 2;
        return (lev50 + sigma_value * sigma);
    }


    /**
     * @param ra_value The percentile on the histogram (99.0 signifies 99%)
     * @param round_up Use the upper edge of the bin (versus the lower edge)
     * @return The DN value in the image corresponding to the percentile
     */
    public double get_pct(double ra_value, boolean round_up) {
        int sum, goal, i;
        int goodpix;

        if (ra_value == 0.0)
            return irafMin;
        if (ra_value == 100.0)
            return irafMax;

        goodpix = 0;
        for (i = 0; i < HISTSIZ2; i++)
            goodpix += hist[i];
        sum = 0;
        goal = (int) (goodpix * (ra_value) / 100);
        i = -1;
        do {
            i++;
            sum = sum + hist[i];
        } while (sum < goal);
        if (SUTDebug.isDebug()) {
            System.out.println("goodpix = " + goodpix
                    + "   goal = " + goal
                    + "   i = " + i
                    + "   histBinsiz = " + histBinsiz);
        }
        if (round_up)
            return ((i + 1.0) * histBinsiz + histMin);
        else
            return ((i) * histBinsiz + histMin);
    }

    /**
     * @return A pointer to the histogram array
     */
    public int[] getHistogramArray() {
        int retHist[] = new int[hist.length];
        System.arraycopy(hist, 0, retHist, 0, hist.length);
        return retHist;
    }


    /**
     * @return The minimum DN value in the image
     */
    public double getDNMin() {
        return irafMin;
    }

    /**
     * @return The maximum DN value in the image
     */
    public double getDNMax() {
        return irafMax;
    }

    /**
     * @param bin The bin index in the histogram
     * @return The DN value in the image corresponding to the specified bin
     */
    public double getDNfromBin(int bin) {
        return (bin * histBinsiz + histMin);
    }

    /**
     * @param dn The DN value
     * @return The histogram index corresponding to the DN value
     */
    public int getBinfromDN(double dn) {
        int bin = (int) ((dn - histMin) / histBinsiz);
        if (bin >= HISTSIZ2)
            bin = HISTSIZ2 - 1;
        if (bin < 0)
            bin = 0;
        return bin;
    }

    /**
     * @param pct      The percentile on the histogram (99.0 signifies 99%)
     * @param round_up Use the upper edge of the bin (versus the lower edge)
     * @return The histogram index corresponding to the percentile
     */
    public int getBINfromPercentile(double pct, boolean round_up) {
        double dn = get_pct(pct, round_up);
        int bin = (int) ((dn - histMin) / histBinsiz);
        if (bin >= HISTSIZ2)
            bin = HISTSIZ2 - 1;
        if (bin < 0)
            bin = 0;
        return bin;
    }

    /**
     * @param sigma    The sigma multiplier (-2 signifies 2 sigma below the mean)
     * @param round_up Use the upper edge of the bin (versus the lower edge)
     * @return The histogram index corresponding to the percentile
     */
    public int getBINfromSigma(double sigma, boolean round_up) {
        double dn = get_sigma(sigma, round_up);
        int bin = (int) ((dn - histMin) / histBinsiz);
        if (bin >= HISTSIZ2)
            bin = HISTSIZ2 - 1;
        if (bin < 0)
            bin = 0;
        return bin;
    }


    public double[] getTblArray() {
        double[] tbl = new double[256];

        int goodpix = 0;
        for (int hist_index = 0; hist_index < HISTSIZ2; hist_index++)
            goodpix += hist[hist_index];
        double goodpix_255 = goodpix / 255.0;

        int tblindex = 0;
        tbl[tblindex++] = histMin;
        double next_goal = goodpix_255;
        int hist_index = 0;
        int accum = 0;
        while (hist_index < HISTSIZ2 && tblindex < 255) {

            if (accum >= next_goal) {
                tbl[tblindex++] = (hist_index * histBinsiz + histMin);
                next_goal += goodpix_255;
            } else {
                accum += hist[hist_index++];
            }
        }
        while (tblindex < 255)
            tbl[tblindex++] = hist_index * histBinsiz + histMin;
        tbl[255] = Double.MAX_VALUE;
        return tbl;
    }

}



