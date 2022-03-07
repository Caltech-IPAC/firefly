package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 10/1/20
 * Time: 8:58 AM
 */


import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.ImageData;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ImageMask;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.RGBIntensity;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static edu.caltech.ipac.firefly.visualize.Band.BLUE;
import static edu.caltech.ipac.firefly.visualize.Band.GREEN;
import static edu.caltech.ipac.firefly.visualize.Band.RED;

/**
 * @author Trey Roby
 */
public class DirectStretchUtils {

    public enum CompressType {FULL, HALF, HALF_FULL, QUARTER_HALF, QUARTER_HALF_FULL}

    public static boolean useHalf(CompressType ct) {
        return ct==CompressType.HALF || ct==CompressType.HALF_FULL || ct==CompressType.QUARTER_HALF || ct==CompressType.QUARTER_HALF_FULL;
    }
    public static boolean useQuarter(CompressType ct) {
        return ct==CompressType.QUARTER_HALF || ct==CompressType.QUARTER_HALF_FULL;
    }
    public static boolean useFull(CompressType ct) {
        return ct==CompressType.FULL || ct==CompressType.HALF_FULL || ct==CompressType.QUARTER_HALF_FULL;
    }

    public static float [] flipFloatArray(float [] float1d, int naxis1, int naxis2) {
        float [] flipped= new float[float1d.length];
        int idx=0;
        for (int y= naxis2-1; y>=0; y--) {
            for (int x= 0; x<naxis1; x++) {
                flipped[idx]= float1d[y*naxis1+x];
                idx++;
            }
        }
        return flipped;
    }

    public static StretchDataInfo getStretchData(PlotState state, ActiveFitsReadGroup frGroup, int tileSize,
                                                 boolean mask, long maskBits, CompressType ct)
            throws InterruptedException {

        FitsRead fr= frGroup.getFitsRead(state.firstBand());
        int totWidth= fr.getNaxis1();
        int totHeight= fr.getNaxis2();


        int xPanels= totWidth / tileSize;
        int yPanels= totHeight / tileSize;
        if (totWidth % tileSize > 0) xPanels++;
        if (totHeight % tileSize > 0) yPanels++;
        ImageData[] imageDataAry= new ImageData[xPanels * yPanels];

        byte [] byte1d;
        byte [] byte1dHalf= null;
        byte [] byte1dQuarter= null;
        int idx;
        int bPos= 0;
        int bPosHalf=0;
        int bPosQuarter=0;

        RangeValues rv= state.getRangeValues();
        RangeValues[] rvAry=
                !state.isThreeColor() ?
                        new RangeValues[] {rv} :
                        new RangeValues[] {
                                state.getRangeValues(Band.RED),
                                state.getRangeValues(Band.GREEN),
                                state.getRangeValues(Band.BLUE)
                        };

        int coreCnt= ServerContext.getParallelProcessingCoreCnt();
        ExecutorService executor = Executors.newFixedThreadPool(coreCnt);
        boolean normalTermination;

        if (state.isThreeColor()) {
            float[][] float1dAry= new float[3][];
            ImageHeader[] imHeadAry= new ImageHeader[3];
            Histogram[] histAry= new Histogram[3];
            Band[] bands= state.getBands();

            for(Band band : bands) {
                FitsRead bandFr= frGroup.getFitsRead(band);
                idx= band.getIdx();
                float1dAry[idx] = flipFloatArray(bandFr.getRawFloatAry(),totWidth,totHeight);
                imHeadAry[idx]= new ImageHeader(bandFr.getHeader());
                histAry[idx]= bandFr.getHistogram();
            }
            int bLen= state.getBands().length;


            RGBIntensity rgbIntensity = new RGBIntensity();
            boolean useIntensity= false;
            if (rv.rgbPreserveHue() && bands.length==3) {
                FitsRead [] fitsReadAry= new FitsRead[] {
                        frGroup.getFitsRead(Band.RED),
                        frGroup.getFitsRead(Band.GREEN),
                        frGroup.getFitsRead(Band.BLUE),
                };
                for(int i=0; (i<3); i++) rgbIntensity.addRangeValues(fitsReadAry, i, rv);
                useIntensity= true;
            }

            byte [][] tmpByte3CAry;
            for(int i= 0; i<xPanels; i++) {
                for(int j= 0; j<yPanels; j++) {
                    int width= (i<xPanels-1) ? tileSize : ((totWidth-1) % tileSize + 1);
                    int height= (j<yPanels-1) ? tileSize : ((totHeight-1) % tileSize + 1);
                    idx= (i*yPanels) +j;
                    imageDataAry[idx]= new ImageData(ImageData.ImageType.TYPE_24_BIT,
                            0,state.getRangeValues(), tileSize*i,tileSize*j, width, height);
                    if (!rv.rgbPreserveHue()) imageDataAry[idx].setRangeValuesAry(rvAry);
                    imageDataAry[idx].stretch3ColorAndSave(float1dAry,imHeadAry,histAry,fr.getHeader(), useIntensity?rgbIntensity:null);
                }
            }
            executor.shutdown();
            normalTermination= executor.awaitTermination(600, TimeUnit.SECONDS);

            byte1d= new byte[totWidth*totHeight * bLen];
            byte1dHalf=  useHalf(ct) ? new byte[dRoundUp(totWidth,2) * dRoundUp(totHeight,2) * bLen] : null;
            byte1dQuarter= useQuarter(ct) ? new byte[dRoundUp(totWidth,4) * dRoundUp(totHeight,4) * bLen] : null;

            for (ImageData imageData : imageDataAry) {
                tmpByte3CAry= imageData.getSaved3CStretch();
                for(int bandIdx=0; (bandIdx<3);bandIdx++) {
                    if (float1dAry[bandIdx]!=null) {
                        System.arraycopy(tmpByte3CAry[bandIdx],0,byte1d,bPos,tmpByte3CAry[bandIdx].length);
                        bPos+=tmpByte3CAry[bandIdx].length;
                        if (useHalf(ct)) {
                            byte[] hDecimatedAry= makeDecimated(tmpByte3CAry[bandIdx],2, imageData.getWidth(), imageData.getHeight());
                            System.arraycopy(hDecimatedAry, 0, byte1dHalf, bPosHalf, hDecimatedAry.length);
                            bPosHalf += hDecimatedAry.length;
                        }
                        if (useQuarter(ct)) {
                            byte[] qDecimatedAry= makeDecimated(tmpByte3CAry[bandIdx],4, imageData.getWidth(), imageData.getHeight());
                            System.arraycopy(qDecimatedAry, 0, byte1dQuarter, bPosQuarter , qDecimatedAry.length);
                            bPosQuarter += qDecimatedAry.length;
                        }
                    }
                }
            }

        }
        else if (mask) {
            float [] float1d= fr.getRawFloatAry();
            final float [] flip1d= flipFloatArray(float1d,totWidth,totHeight);
            byte1d= new byte[flip1d.length];

            List<ImageMask> masksList=  new ArrayList<>();
            for(int j= 0; (j<31); j++) {
                if (((maskBits>>j) & 1) != 0) {
                    masksList.add(new ImageMask(j,Color.RED));
                }
            }
            ImageMask[] maskAry= masksList.toArray(new ImageMask[0]);


            for(int i= 0; i<xPanels; i++) {
                for(int j= 0; j<yPanels; j++) {
                    int width= (i<xPanels-1) ? tileSize : ((totWidth-1) % tileSize + 1);
                    int height= (j<yPanels-1) ? tileSize : ((totHeight-1) % tileSize + 1);
                    idx= (i*yPanels) +j;
                    imageDataAry[idx]= new ImageData( maskAry, rv, tileSize*i,tileSize*j, width, height);
                    ImageData im= imageDataAry[idx];

                    executor.execute(() -> im.stretchMaskAndSave(flip1d,fr.getNaxis1()));
                }
            }
            executor.shutdown();
            normalTermination= executor.awaitTermination(600, TimeUnit.SECONDS);
            for (ImageData imageData : imageDataAry) {
                byte[] tmpByteAry = imageData.getSavedStandardStretch();
                System.arraycopy(tmpByteAry, 0, byte1d, bPos, tmpByteAry.length);
                bPos += tmpByteAry.length;
            }

        }
        else {
            float [] float1d= fr.getRawFloatAry();
            final float [] flip1d= flipFloatArray(float1d,totWidth,totHeight);
            byte1d= new byte[flip1d.length];
            for(int i= 0; i<xPanels; i++) {
                for(int j= 0; j<yPanels; j++) {
                    int width= (i<xPanels-1) ? tileSize : ((totWidth-1) % tileSize + 1);
                    int height= (j<yPanels-1) ? tileSize : ((totHeight-1) % tileSize + 1);
                    idx= (i*yPanels) +j;
                    imageDataAry[idx]= new ImageData(ImageData.ImageType.TYPE_8_BIT,
                            state.getColorTableId(),rv, tileSize*i,tileSize*j, width, height);
                    ImageData im= imageDataAry[idx];

                    executor.execute(() -> im.stretch8bitAndSave(flip1d,fr.getHeader(),fr.getHistogram()));
                }
            }
            executor.shutdown();
            normalTermination= executor.awaitTermination(600, TimeUnit.SECONDS);


            byte1dHalf=  useHalf(ct) ? new byte[dRoundUp(totWidth,2) * dRoundUp(totHeight,2)] : null;
            byte1dQuarter= useQuarter(ct) ? new byte[dRoundUp(totWidth,4) * dRoundUp(totHeight,4)] : null;
            for (ImageData imageData : imageDataAry) {
                byte[] tmpByteAry = imageData.getSavedStandardStretch();
                System.arraycopy(tmpByteAry, 0, byte1d, bPos, tmpByteAry.length);
                bPos += tmpByteAry.length;
                if (useHalf(ct)) {
                    byte[] hDecimatedAry= makeDecimated(tmpByteAry,2, imageData.getWidth(), imageData.getHeight());
                    System.arraycopy(hDecimatedAry, 0, byte1dHalf, bPosHalf, hDecimatedAry.length);
                    bPosHalf += hDecimatedAry.length;
                }
                if (useQuarter(ct)) {
                    byte[] qDecimatedAry= makeDecimated(tmpByteAry,4, imageData.getWidth(), imageData.getHeight());
                    System.arraycopy(qDecimatedAry, 0, byte1dQuarter, bPosQuarter , qDecimatedAry.length);
                    bPosQuarter += qDecimatedAry.length;
                }
            }
        }
        if (!normalTermination) executor.shutdownNow();
        return new StretchDataInfo(useFull(ct) ? byte1d : null, byte1dHalf, byte1dQuarter, rvAry);
    }



    public static class StretchDataInfo implements Serializable {
        private final byte [] byte1d;
        private final byte [] byte1dHalf;
        private final byte [] byte1dQuarter;
        private final RangeValues[] rvAry;

        public StretchDataInfo(byte[] byte1d, byte[] byte1dHalf, byte[] byte1dQuarter, RangeValues[] rvAry) {
            this.byte1d = byte1d;
            this.byte1dHalf = byte1dHalf;
            this.byte1dQuarter = byte1dQuarter;
            this.rvAry= rvAry;
        }

        public byte[] getByte1d() { return byte1d; }
        public byte[] getByte1dHalf() { return byte1dHalf; }
        public byte[] getByte1dQuarter() { return byte1dQuarter; }


        public byte[] findMostCompressAry(CompressType ct) {
            return switch (ct) {
                case FULL -> byte1d;
                case QUARTER_HALF_FULL, QUARTER_HALF -> byte1dQuarter;
                case HALF, HALF_FULL -> byte1dHalf;
            };
        }

        public boolean isRangeValuesMatching(PlotState state) {
            if (state.isThreeColor()) {
                for (Band band : new Band[]{RED, GREEN, BLUE}) {
                    if (state.isBandUsed(band)) {
                        int idx= band.getIdx();
                        if (rvAry[idx]==null && !rvAry[idx].toString().equals(state.getRangeValues(band).toString())) {
                            return false;
                        }
                    }
                }
                return true;
            }
            else {
                return rvAry.length==1 && rvAry[0].toString().equals(state.getRangeValues().toString());
            }

        }

        /**
         * create a version of the object withh only the full byte array and optionally the half is the
         * CompressType is only useing the quarter
         * @return a version of StretchDataInfo without all the data we will not use again
         */
        public StretchDataInfo copyParts(CompressType ct) {
            boolean keepHalf= ct== CompressType.QUARTER_HALF_FULL || ct== CompressType.QUARTER_HALF;
            return new StretchDataInfo(byte1d, keepHalf?byte1dHalf:null, null, rvAry);
        }

    }

    private static int dRoundUp(int v, int factor) {
        return v % factor == 0 ? v/factor : v/factor +1;
    }

    static private byte[] makeDecimated(byte[] in, int factor, int width, int height) {
        int outW= dRoundUp(width,factor);
        int outH= dRoundUp(height,factor);
        int outLen= (outW * outH);
        byte[] out= new byte[outLen];
        int outIdx;
        for(int j= 0; (j<height); j+=factor) {
            for (int i = 0; (i < width); i += factor) {
                outIdx= (j/factor) * outW + (i/factor);
                out[outIdx]= averageCells(in,width,i,j,factor);
            }
        }
        return out;
    }

    static byte averageCells(byte[] in,int width,int rowIdx, int colIdx, int factor) {
        int sum= 0;
        int cnt= 0;
        int inIdx;
        for(int j=colIdx; (j<colIdx+factor); j++)  {
            for(int i=rowIdx; (i<rowIdx+factor); i++)  {
                inIdx= j * width + i;
                if (inIdx<in.length) {
                    sum+= Byte.toUnsignedInt(in[inIdx]);
                    cnt++;
                }
            }
        }
        return (byte) (sum/cnt);
    }
}
