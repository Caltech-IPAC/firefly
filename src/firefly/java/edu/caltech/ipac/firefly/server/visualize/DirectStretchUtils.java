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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Trey Roby
 */
public class DirectStretchUtils {

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

    public static byte [] getStretchData(PlotState state,  ActiveFitsReadGroup frGroup, int tileSize, boolean mask, long maskBits)
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
        int idx;
        int bPos= 0;

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
            byte1d= new byte[totWidth*totHeight * state.getBands().length];


            RangeValues rv= state.getRangeValues();
            RangeValues[] rvAry= new RangeValues[] {
                    state.getRangeValues(Band.RED),
                    state.getRangeValues(Band.GREEN),
                    state.getRangeValues(Band.BLUE)
            };
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

            for (ImageData imageData : imageDataAry) {
                tmpByte3CAry= imageData.getSaved3CStretch();
                for(int bandIdx=0; (bandIdx<3);bandIdx++) {
                    if (float1dAry[bandIdx]!=null) {
                        System.arraycopy(tmpByte3CAry[bandIdx],0,byte1d,bPos,tmpByte3CAry[bandIdx].length);
                        bPos+=tmpByte3CAry[bandIdx].length;
                    }
                }
            }

        }
        else if (mask) {
            float [] float1d= fr.getRawFloatAry();
            final float [] flip1d= flipFloatArray(float1d,totWidth,totHeight);
            byte1d= new byte[flip1d.length];

            List<ImageMask> masksList=  new ArrayList<ImageMask>();
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
                    imageDataAry[idx]= new ImageData( maskAry,
                            state.getRangeValues(), tileSize*i,tileSize*j, width, height);
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
                            state.getColorTableId(),state.getRangeValues(), tileSize*i,tileSize*j, width, height);
                    ImageData im= imageDataAry[idx];

                    executor.execute(() -> im.stretch8bitAndSave(flip1d,fr.getHeader(),fr.getHistogram()));
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
        if (!normalTermination) executor.shutdownNow();
        return byte1d;
    }


}
