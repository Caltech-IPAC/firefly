/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import edu.caltech.ipac.firefly.data.HasSizeOf;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.StopWatch;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Creates a histogram of an image
 *
 * @author Booth Hartley
 * Edit history
 * LZ 6/15/15
 *         - Renamed and rewrote the getTblArray method and commented out the eq_tbl and deq_dtbl
 *         - Only leave the bitpix = -32 and clean up the rest
 */


public class Histogram implements HasSizeOf {
    private static final int HISTSIZ2 = 4096;  /* full size of hist array */
    private static final int HISTSIZ = 2048;     /* half size of hist array */
    private static final boolean debug= false;

    private int[] hist;
    private double histMin;
    private double histBinsize;
    private final double irafMin;
    private final double irafMax;
    private final double largeBinPercent;

    private final static ExecutorService exeService= Executors.newWorkStealingPool();


    public Histogram(float[] float1dArray, double datamin, double datamax) {

//        StopWatch.getInstance().start("minMax");

        if (Double.isNaN(datamin) || Double.isNaN(datamax)) {
            MinMax minMax = findMinMax(float1dArray);
            datamin = minMax.min;
            datamax = minMax.max;
        }


//        StopWatch.getInstance().stop("minMax");
//        Logger.briefInfo("minMax="+t);
//        StopWatch.getInstance().printLog("minMax", StopWatch.Unit.SECONDS);

        double histDatamax;
        double histDatamin;

        histMin = datamin;
        double histMax = datamax;
        boolean doing_redo = false;

        StopWatch.getInstance().start("histogram");
        int count=0;
        while (true) {
            count++;

            boolean redo_flag = false;
            histBinsize =getHistBinSize(histMax);
            HistEntry histEntry= makeHistogramEntry(float1dArray,histMin,histBinsize);
            hist= histEntry.hist;
            histDatamin = histEntry.histDatamin;
            histDatamax = histEntry.histDatamax;

            printDebugInfo(histMax, histEntry.underflowCount, histEntry.overflowCount);
            datamin = histDatamin;
            datamax = histDatamax;

	        /* redo if more than 1% of pixels fell off histogram */
            if (histEntry.underflowCount > float1dArray.length * .01) redo_flag = true;
            if (histEntry.overflowCount > float1dArray.length * .01) redo_flag = true;

            /* check if we got a good spread */

            if (!redo_flag && !doing_redo) { /* don't bother checking if we already want a redo */
	           /* see what happens if we lop off top and bottom 0.05% of hist */
                int lowLimit = getLowLimit();
                int histMaxIndex = getHighSumIndex(lowLimit) + 1;
                int histMinIndex = getLowSumIndex(lowLimit);
                if (histMaxIndex==-1 || histMinIndex==-1) {
                    break;
                }

                if ((histMaxIndex - histMinIndex) < HISTSIZ) {
                    histMax = (histMaxIndex * histBinsize) + histMin;
                    histMin = (histMinIndex * histBinsize) + histMin;
                    redo_flag = true;   /* we can spread it out by factor of 2 */
                }
            } else {
                if (!doing_redo) {
                    histMax = datamax;
                    histMin = datamin;
                }
            }


            if ( !doing_redo  &&  redo_flag ) {
                if (debug) System.out.println("rebuilding histogram . . ");
                doing_redo = true;
            } else
                break;

        }

        irafMin = datamin;
        irafMax = datamax;
        largeBinPercent= computeLargeBinPercent(float1dArray.length);
//        StopWatch.getInstance().stop("histogram");
//        t= StopWatch.getInstance().getTracker("histogram").getElapsedTime(StopWatch.Unit.SECONDS);
//        Logger.briefInfo("histogram="+t+ ", count=" +count);
    }

    private static HistEntry makeHistogramEntry(float[] float1dArray, double histMin, double histBinsize) {

        var hist = new int[HISTSIZ2 + 1];
        var overflowCount = 0;
        var underflowCount = 0;
        double histDatamax = -Double.MAX_VALUE;
        double histDatamin = Double.MAX_VALUE;
        try {
            var len = float1dArray.length< 10000 ? 1 : 4;
            var taskList = new ArrayList<Callable<Void>>();
            var partSize = float1dArray.length / 4;
            var phistList = new ArrayList<PartialHistogram>();


            for (int i = 0; i < len; i++) {
                var stop = i < len - 1 ? (i+1) * partSize : float1dArray.length;
                var pHist = new PartialHistogram(float1dArray, i * partSize, stop, histMin, histBinsize);
                phistList.add(pHist);
                taskList.add(pHist::makeHistPartial);
            }

            // call in threads
            if (taskList.size() == 1) {
                taskList.getFirst().call();
            } else {
                var results = exeService.invokeAll(taskList);
                if (!results.stream().filter(Future::isCancelled).toList().isEmpty()) {
                    throw new InterruptedException("Not all threads completed");
                }
            }

            // assemble results
            for (PartialHistogram pHist : phistList) {
                overflowCount += pHist.overflowCount;
                underflowCount += pHist.underflowCount;
                for (int i = 0; i < HISTSIZ2; i++) {
                    hist[i] += pHist.partialHist[i];
                }
                if (pHist.histDatamin < histDatamin) histDatamin = pHist.histDatamin;
                if (pHist.histDatamax > histDatamax) histDatamax = pHist.histDatamax;
            }
        } catch (Exception e) {
            Logger.warn(e, "Histgram Entry failed");
        }
        return new HistEntry(hist,overflowCount,underflowCount, histDatamin, histDatamax);
    }


    private record HistEntry(int[] hist, int overflowCount, int underflowCount, double histDatamin, double histDatamax) {};
    private record MinMax(double min, double max) {};

    private MinMax findMinMax(float[] float1dArray) {
        double datamin= Double.MAX_VALUE;
        double datamax= -Double.MAX_VALUE;
        try {
            var taskList = new ArrayList<Callable<Void>>();
            var len = float1dArray.length< 10000 ? 1 : 4;
            var partSize = float1dArray.length / 4;
            var pMinMaxList = new ArrayList<PartialMinMax>();

            for (int i = 0; i < len; i++) {
                var stop = i < len - 1 ? (i+1) * partSize : float1dArray.length;
                var pMinMax = new PartialMinMax(float1dArray, i * partSize, stop);
                pMinMaxList.add(pMinMax );
                taskList.add(pMinMax::findPartialMinMax);
            }
            // call in threads
            if (taskList.size() == 1) {
                taskList.getFirst().call();
            } else {
                var results = exeService.invokeAll(taskList);
                if (!results.stream().filter(Future::isCancelled).toList().isEmpty()) {
                    throw new InterruptedException("Not all threads completed");
                }
            }

            // assemble results
            for (PartialMinMax pMinMax : pMinMaxList) {
                if (pMinMax.datamin < datamin) datamin = pMinMax.datamin;
                if (pMinMax.datamin > datamax) datamax = pMinMax.datamax;
            }
        } catch (Exception e) {
            Logger.warn(e, "Histgram Entry failed");
        }
        return new MinMax(datamin,datamax);

    }

    private static class PartialHistogram {
        int []  partialHist = new int[HISTSIZ2 + 1];
        int underflowCount = 0;
        int overflowCount = 0;
        float[] float1dArray;
        int start;
        int stop;
        double histMin;
        double histBinsize;
        double histDatamax = -Double.MAX_VALUE;
        double histDatamin = Double.MAX_VALUE;

        PartialHistogram(float[] float1dArray, int start, int stop, double histMin, double histBinsize) {
            this.float1dArray = float1dArray;
            this.start = start;
            this.stop = stop;
            this.histMin = histMin;
            this.histBinsize = histBinsize;
        }

         Void makeHistPartial() {
            for (int k = start; k < stop; k++) {
                if (!Double.isNaN(float1dArray[k])) {
                    int i = (int) ((float1dArray[k] - histMin) / histBinsize);
                    if (i<0) {
                        underflowCount++;
                    }
                    else if (i>HISTSIZ2) {
                        overflowCount++;
                    }
                    else {
                        partialHist[i] ++;
                    }
                    if (float1dArray[k] < histDatamin) histDatamin = float1dArray[k];
                    if (float1dArray[k] > histDatamax) histDatamax = float1dArray[k];
                }
            }
            return null;
        }
    }

    private static class PartialMinMax {
        float[] float1dArray;
        int start;
        int stop;
        double datamin= Double.MAX_VALUE;
        double datamax= -Double.MAX_VALUE;

        PartialMinMax(float[] float1dArray, int start, int stop) {
            this.float1dArray = float1dArray;
            this.start = start;
            this.stop = stop;
        }

        Void findPartialMinMax() {
            float v;
            for (int k = start; k < stop; k++) {
                v= float1dArray[k];
                if (v < datamin) datamin = v;
                if (v > datamax) datamax = v;
            }
            return null;
        }
    }


    private double getHistBinSize(double histMax){
        double  hbinsiz = (histMax - histMin) / HISTSIZ2;
        if (hbinsiz == 0.0) hbinsiz = 1.0;
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
        for (int i = 0; i < HISTSIZ2; i++) goodpix += hist[i];
        return (int) (goodpix * 0.0005);
    }

    private void printDebugInfo(double hist_max, int underFlowCount, int overFlowCount) {
        if (debug) {
            System.out.println("histMin = " + histMin);
            System.out.println("hist_max = " + hist_max);
            System.out.println("histBinsize = " + histBinsize);
            System.out.println("underFlowCount = " + underFlowCount + "  overFlowCount = " + overFlowCount);
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
        if (debug) {
            System.out.println("goodpix = " + goodpix
                    + "   goal = " + goal
                    + "   i = " + i
                    + "   histBinsize = " + histBinsize);
        }
        if (round_up)
            return ((i + 1.0) * histBinsize + histMin);
        else
            return ((i) * histBinsize + histMin);
    }

    public double computeLargeBinPercent(int dataLen) {
       int cnt=0;

       int histMax=0;
        for (int j : hist) {
            if (histMax < j) histMax = j;
        }
        int marker= (int)(histMax*.4);


        for (int j : hist) {
            if (j > marker) cnt++;
        }
       return (float)cnt/(float)hist.length;
    }

    /**
     * @return A pointer to the histogram array
     */
    public int[] getHistogramArray() {
        int[] retHist= new int[hist.length];
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
        return (bin * histBinsize + histMin);
    }

    /**
     * Generate the mean bin data array
     * @param bscale the bscale from the fits header
     * @param bzero the bzero from the fits header
     * @return an array the same length as the histograme
     */
    public double[] getMeanBinDataAry(double bscale, double bzero) {
        double[] meanDataAry = new double[hist.length];
        for (int i = 0; i < meanDataAry.length; i++) meanDataAry[i] = getDNfromBin(i) * bscale + bzero;
        return meanDataAry ;
    }

    /**
     * @param dn The DN value
     * @return The histogram index corresponding to the DN value
     */
    public int getBinfromDN(double dn) {
        int bin = (int) ((dn - histMin) / histBinsize);
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
        int bin = (int) ((dn - histMin) / histBinsize);
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
        int bin = (int) ((dn - histMin) / histBinsize);
        if (bin >= HISTSIZ2)
            bin = HISTSIZ2 - 1;
        if (bin < 0)
            bin = 0;
        return bin;
    }

    /**
     *
     * @return  tbl An int array [256] to be filled with the histogram equalized values
     */
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
                tbl[tblindex++] = (hist_index * histBinsize + histMin);
                next_goal += goodpix_255;
            } else {
                accum += hist[hist_index++];
            }
        }
        while (tblindex < 255)
            tbl[tblindex++] = hist_index * histBinsize + histMin;
        tbl[255] = Double.MAX_VALUE;
        return tbl;
    }

    public long getSizeOf() { return hist.length*4L + 40L; }

    public double getLargeBinPercent() { return largeBinPercent; }
}
