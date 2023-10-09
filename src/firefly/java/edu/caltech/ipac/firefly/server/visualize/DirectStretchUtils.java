package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.HasSizeOf;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ImageMask;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.ImageStretch;
import edu.caltech.ipac.visualize.plot.plotdata.RGBIntensity;
import nom.tam.fits.Header;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static edu.caltech.ipac.firefly.visualize.Band.BLUE;
import static edu.caltech.ipac.firefly.visualize.Band.GREEN;
import static edu.caltech.ipac.firefly.visualize.Band.RED;

/**
 * @author Trey Roby
 * Date: 10/1/20
 */
public class DirectStretchUtils {

    private final static ExecutorService exeService= Executors.newWorkStealingPool();
    public enum CompressType {FULL, HALF, HALF_FULL, QUARTER_HALF, QUARTER_HALF_FULL}

    public static StretchDataInfo getStretchData(PlotState state, ActiveFitsReadGroup frGroup, int tileSize, CompressType ct)
            throws Exception {
        return state.isThreeColor() ?
                getStretch3C(state,frGroup,tileSize,ct) :
                getStretchStandard(state,frGroup.getFitsRead(state.firstBand()),tileSize,ct);
    }

    public static StretchDataInfo getStretchDataMask(PlotState state, ActiveFitsReadGroup frGroup, int tileSize, long maskBits)
            throws Exception {
        FitsRead fr= frGroup.getFitsRead(state.firstBand());
        float [] float1d= fr.getRawFloatAry();
        StretchVars sv= getStretchVars(fr,tileSize, CompressType.FULL);
        float [] flip1d= flipFloatArray(float1d,sv.totWidth,sv.totHeight);
        List<ImageMask> maskList=  new ArrayList<>();

        for(int j= 0; (j<31); j++) {
            if (((maskBits>>j) & 1) != 0) maskList.add(new ImageMask(j, Color.RED));
        }
        var sTileList= doTileStretch(sv,tileSize, StretchMaskTile::new,
                (stdef, strContainer) -> () -> strContainer.stretch(stdef, maskList,  flip1d,fr.getNaxis1()) );

        byte[] byte1d= combineArray(flip1d.length, sTileList.stream().map( st -> st.result).toList());
        return new StretchDataInfo(byte1d, null, null, getRangeValuesToUse(state));
    }

    private static StretchDataInfo getStretchStandard(PlotState state, FitsRead fr, int tileSize, CompressType ct)
            throws Exception {
        StretchVars sv= getStretchVars(fr,tileSize, ct);
        float [] float1d= fr.getRawFloatAry();
        float [] flip1d= flipFloatArray(float1d,sv.totWidth,sv.totHeight);
        RangeValues rv= state.getRangeValues();

        var sTileList = doTileStretch(sv,tileSize, StretchStandardTile::new,
                (stdef, strContainer) -> () -> strContainer.stretch(stdef, rv, flip1d,fr.getHeader(),fr.getHistogram()) );
        return buildStandardResult(sTileList,rv,sv.totWidth,sv.totHeight,ct);
    }

    private static StretchDataInfo getStretch3C(PlotState state, ActiveFitsReadGroup frGroup, int tileSize, CompressType ct)
            throws Exception {
        FitsRead fr= frGroup.getFitsRead(state.firstBand());
        StretchVars sv= getStretchVars(fr,tileSize, ct);
        RangeValues[] rvAry= getRangeValuesToUse(state);
        int bPos= 0;
        int bPosHalf=0;
        int bPosQuarter=0;
        Band[] bands= state.getBands();
        ThreeCComponents tComp= get3CComponents(frGroup,sv.totWidth,sv.totHeight,state);
        RGBIntensity rgbI= get3CRGBIntensity(state.getRangeValues(),frGroup,bands);
         new ArrayList<Stretch3CTile>(sv.tileLen);

        var sTileList =doTileStretch(sv,tileSize, Stretch3CTile::new,
                (stdef,strContainer) -> () -> strContainer.stretch(stdef, rvAry, tComp.float1dAry,tComp.imHeadAry,tComp.histAry,rgbI) );
        int bLen= state.getBands().length;
        byte[] byte1d= new byte[sv.totWidth*sv.totHeight * bLen];
        byte[] byte1dHalf=  useHalf(ct) ? new byte[dRoundUp(sv.totWidth,2) * dRoundUp(sv.totHeight,2) * bLen] : null;
        byte[] byte1dQuarter= useQuarter(ct) ? new byte[dRoundUp(sv.totWidth,4) * dRoundUp(sv.totHeight,4) * bLen] : null;

        for (Stretch3CTile stretchTile : sTileList) {
            byte[][] tmpByte3CAry= stretchTile.result;
            for(int bandIdx=0; (bandIdx<3);bandIdx++) {
                if (tComp.float1dAry[bandIdx]!=null) {
                    System.arraycopy(tmpByte3CAry[bandIdx],0,byte1d,bPos,tmpByte3CAry[bandIdx].length);
                    bPos+=tmpByte3CAry[bandIdx].length;
                    if (useHalf(ct)) {
                        byte[] hDecimatedAry= stretchTile.resultHalf[bandIdx];
                        System.arraycopy(hDecimatedAry, 0, byte1dHalf, bPosHalf, hDecimatedAry.length);
                        bPosHalf += hDecimatedAry.length;
                    }
                    if (useQuarter(ct)) {
                        byte[] qDecimatedAry= stretchTile.resultQuarter[bandIdx];
                        System.arraycopy(qDecimatedAry, 0, byte1dQuarter, bPosQuarter , qDecimatedAry.length);
                        bPosQuarter += qDecimatedAry.length;
                    }
                }
            }
        }
        return new StretchDataInfo(byte1d, byte1dHalf, byte1dQuarter, rvAry);
    }

    private static <T> List<T> doTileStretch(StretchVars sv, int tileSize, Callable<T> stretchContainerFactory,
                                            SetupStretchTask<T> setupTileStretch) throws Exception {
        var stretchResultList= new ArrayList<T>(300);
        var taskList= new ArrayList<Callable<Void>>(sv.xPanels*sv.yPanels);
        for(int i= 0; i<sv.xPanels; i++) {
            for(int j= 0; j<sv.yPanels; j++) {
                int width= (i<sv.xPanels-1) ? tileSize : ((sv.totWidth-1) % tileSize + 1);
                int height= (j<sv.yPanels-1) ? tileSize : ((sv.totHeight-1) % tileSize + 1);
                T stretchContainer= stretchContainerFactory.call();
                stretchResultList.add(stretchContainer);
                StretchTileDef tileDef= new StretchTileDef(tileSize*i, tileSize*j, width, height,sv.ct);
                taskList.add(setupTileStretch.makeTask(tileDef, stretchContainer));
            }
        }
        invokeList(taskList);
        return stretchResultList;
    }

    private static StretchDataInfo buildStandardResult(List<StretchStandardTile> sTileList, RangeValues rv,
                                                      int totWidth, int totHeight, CompressType ct) throws Exception {
        var taskList= new ArrayList<Callable<Void>>();
        byte[] byte1d= useFull(ct ) ? new byte[totWidth*totHeight] : null;
        byte[] byte1dQuarter=  useQuarter(ct) ? new byte[dRoundUp(totWidth,4) * dRoundUp(totHeight,4)]:null;
        byte[] byte1dHalf= useHalf(ct) ? new byte[dRoundUp(totWidth,2) * dRoundUp(totHeight,2)]:null;
        if (useFull(ct)) {
            taskList.add(() -> combineArray(byte1d, sTileList.stream().map( st -> st.result).toList()));
        }
        if (useQuarter(ct)) {
            taskList.add(() -> combineArray(byte1dQuarter, sTileList.stream().map( st -> st.resultQuarter).toList()));
        }
        if (useHalf(ct)) {
            taskList.add(() -> combineArray(byte1dHalf, sTileList.stream().map( st -> st.resultHalf).toList()));
        }
        invokeList(taskList);
        return new StretchDataInfo(byte1d, byte1dHalf, byte1dQuarter, new RangeValues[] {rv});
    }

    private static void invokeList(List<Callable<Void>> taskList) throws Exception {
        if (taskList.size()==1) {
            taskList.get(0).call();
        }
        else {
            var results= exeService.invokeAll(taskList);
            if (results.stream().filter(Future::isCancelled).toList().size()>0) {
                throw new InterruptedException("Not all tiles completed");
            }
        }
    }

    private static boolean useHalf(CompressType ct) {
        return ct==CompressType.HALF || ct==CompressType.HALF_FULL || ct==CompressType.QUARTER_HALF || ct==CompressType.QUARTER_HALF_FULL;
    }
    private static boolean useQuarter(CompressType ct) {
        return ct==CompressType.QUARTER_HALF || ct==CompressType.QUARTER_HALF_FULL;
    }
    private static boolean useFull(CompressType ct) {
        return ct==CompressType.FULL || ct==CompressType.HALF_FULL || ct==CompressType.QUARTER_HALF_FULL;
    }

    private static float [] flipFloatArray(float [] float1d, int naxis1, int naxis2) {
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

    private static StretchVars getStretchVars(FitsRead fr, int tileSize, CompressType ct) {
        int totWidth= fr.getNaxis1();
        int totHeight= fr.getNaxis2();
        int xPanels= totWidth / tileSize;
        int yPanels= totHeight / tileSize;
        if (totWidth % tileSize > 0) xPanels++;
        if (totHeight % tileSize > 0) yPanels++;
        int tileLen= xPanels * yPanels;
        return new StretchVars(totWidth,totHeight,xPanels,yPanels,tileLen,ct);
    }

    private static ThreeCComponents get3CComponents(ActiveFitsReadGroup frGroup, int totWidth, int totHeight, PlotState state) {
        int idx;
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
        return new ThreeCComponents(float1dAry,imHeadAry,histAry);
    }

    private static RGBIntensity get3CRGBIntensity(RangeValues rv, ActiveFitsReadGroup frGroup, Band[] bands) {
        RGBIntensity rgbIntensity = new RGBIntensity();
        boolean useIntensity= false;
        if (rv.rgbPreserveHue() && bands.length==3) {
            FitsRead [] fitsReadAry= new FitsRead[] {
                    frGroup.getFitsRead(RED), frGroup.getFitsRead(GREEN), frGroup.getFitsRead(BLUE), };
            for(int i=0; (i<3); i++) rgbIntensity.addRangeValues(fitsReadAry, i, rv);
            useIntensity= true;
        }
        return useIntensity ? rgbIntensity : null;
    }

    private static RangeValues[] getRangeValuesToUse(PlotState state) {
        RangeValues rv= state.getRangeValues();
        if (!state.isThreeColor()) return new RangeValues[] {rv};
        if (rv.rgbPreserveHue()) return new RangeValues[] {rv,rv,rv};
        return new RangeValues[] { state.getRangeValues(RED), state.getRangeValues(GREEN), state.getRangeValues(BLUE) };
    }

    private static Void combineArray(byte[] target, List<byte[]> aList) {
        int pos= 0;
        for (var dAry : aList) {
            System.arraycopy(dAry, 0, target, pos , dAry.length);
            pos += dAry.length;
        }
        return null;
    }

    private static byte[] combineArray(int length, List<byte[]> aList) {
        byte[] target= new byte[length];
        combineArray(target,aList);
        return target;
    }

    private static int dRoundUp(int v, int factor) { return v % factor == 0 ? v/factor : v/factor +1; }

    private static byte[] makeDecimated(byte[] in, int factor, int width, int height) {
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

    private static byte averageCells(byte[] in,int width,int rowIdx, int colIdx, int factor) {
        int sum= 0;
        int cnt= 0;
        int inIdx;
        int factHalf= factor/2;
        int startColIdx, endColIdx;
        int startRowIdx, endRowIdx;
        if (colIdx-factHalf<0) {
            startColIdx= colIdx;
            endColIdx= colIdx+factor;
        }
        else {
            startColIdx= colIdx-factHalf;
            endColIdx= colIdx+factHalf;
        }
        if (rowIdx-factHalf<0) {
            startRowIdx= rowIdx;
            endRowIdx= rowIdx+factor;
        }
        else {
            startRowIdx= rowIdx-factHalf;
            endRowIdx= rowIdx+factHalf;
        }
        for(int j=startColIdx; (j<endColIdx); j++)  {
            for(int i=startRowIdx; (i<endRowIdx); i++)  {
                inIdx= j * width + i;
                if (inIdx>=0  && inIdx<in.length) {
                    sum+= Byte.toUnsignedInt(in[inIdx]);
                    cnt++;
                }
            }
        }
        return (byte) (sum/cnt);
    }

    private interface SetupStretchTask<T> { Callable<Void> makeTask(StretchTileDef stdef, T stretchContainer); }
    private record ThreeCComponents(float[][] float1dAry, ImageHeader[] imHeadAry, Histogram[] histAry) {}
    private record StretchVars(int totWidth, int totHeight, int xPanels, int yPanels, int tileLen, CompressType ct) {}
    private record StretchTileDef(int x, int y, int width, int height, CompressType ct) {}

    public static class StretchDataInfo implements Serializable, HasSizeOf {
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

        public byte[] findMostCompressAry(CompressType ct) {
            return switch (ct) {
                case FULL -> byte1d;
                case QUARTER_HALF_FULL, QUARTER_HALF -> byte1dQuarter;
                case HALF, HALF_FULL -> byte1dHalf;
            };
        }

        public static String getMostCompressedDescription(CompressType ct) {
            return switch (ct) {
                case FULL -> "Full";
                case QUARTER_HALF_FULL, QUARTER_HALF -> "Quarter";
                case HALF, HALF_FULL -> "Half";
            };
        }

        public boolean isRangeValuesMatching(PlotState state) {
            if (!state.isThreeColor()) {
                return rvAry.length==1 && rvAry[0].toString().equals(state.getRangeValues().toString());
            }
            for (Band band : new Band[]{RED, GREEN, BLUE}) {
                if (state.isBandUsed(band)) {
                    int idx= band.getIdx();
                    if (rvAry[idx]==null || !rvAry[idx].toString().equals(state.getRangeValues(band).toString())) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * create a version of the object with only the full byte array and optionally the half if the
         * CompressType is only using the quarter
         * @return a version of StretchDataInfo without all the data we will not use again
         */
        public StretchDataInfo copyParts(CompressType ct) {
            boolean keepHalf= ct== CompressType.QUARTER_HALF_FULL || ct== CompressType.QUARTER_HALF;
            return new StretchDataInfo(byte1d, keepHalf?byte1dHalf:null, null, rvAry);
        }

        @Override
        public long getSizeOf() {
            long sum= rvAry.length * 80L;
            if (byte1d!=null) sum+=byte1d.length;
            if (byte1dHalf!=null) sum+=byte1dHalf.length;
            if (byte1dQuarter!=null) sum+=byte1dQuarter.length;
            return sum;
        }
    }

    private static class Stretch3CTile {
        byte[][] result;
        byte[][] resultHalf;
        byte[][] resultQuarter;

        Void stretch(StretchTileDef stdef, RangeValues[] rvAry, float [][] float1dAry,
                               ImageHeader [] imHeadAry, Histogram[] histAry, RGBIntensity rgbIntensity) {
            byte[][] pixelDataAry= new byte[3][];
            for(int i=0;i<3; i++) pixelDataAry[i]= new byte[stdef.width * stdef.height];
            int lastPixel = stdef.x + stdef.width -1;
            int lastLine = stdef.y + stdef.height -1;
            ImageStretch.stretchPixels3Color(rvAry, float1dAry, pixelDataAry, imHeadAry, histAry,
                    rgbIntensity, stdef.x, lastPixel, stdef.y, lastLine );
            this.result =  pixelDataAry;
            if (useHalf(stdef.ct))   {
                this.resultHalf = new byte[3][];
                for(int i=0;i<3; i++) {
                    this.resultHalf[i]= makeDecimated(pixelDataAry[i], 2, stdef.width, stdef.height);
                }
            }
            if (useQuarter(stdef.ct)) {
                this.resultQuarter = new byte[3][];
                for(int i=0;i<3; i++) {
                    this.resultQuarter[i]= makeDecimated(pixelDataAry[i], 4, stdef.width, stdef.height);
                }
            }
            return null;
        }
    }

    private static class StretchStandardTile {
        byte[] result;
        byte[] resultHalf;
        byte[] resultQuarter;

        Void stretch(StretchTileDef stdef, RangeValues rv, float [] float1d, Header header, Histogram histogram) {
            final ImageHeader imHead= new ImageHeader(header) ;
            byte [] byteAry= new byte[stdef.width * stdef.height];
            int lastPixel = stdef.x + stdef.width -1;
            int lastLine = stdef.y + stdef.height -1;
            ImageStretch.stretchPixels8Bit(rv, float1d, byteAry, imHead,  histogram, stdef.x, lastPixel, stdef.y, lastLine );
            if (useHalf(stdef.ct))   this.resultHalf = makeDecimated(byteAry, 2, stdef.width, stdef.height);
            if (useQuarter(stdef.ct)) this.resultQuarter = makeDecimated(byteAry, 4, stdef.width, stdef.height);
            this.result =byteAry;
            return null;
        }
    }

    private static class StretchMaskTile {
        byte[] result;

        Void stretch(StretchTileDef stdef, List<ImageMask> maskList, final float [] float1d, final int naxis1) {
            byte [] byteAry= new byte[stdef.width * stdef.height];
            int[] pixelhist = new int[256];
            int lastPixel = stdef.x + stdef.width -1;
            int lastLine = stdef.y + stdef.height -1;
            ImageMask[] iMasks= maskList.toArray(new ImageMask[0]);
            ImageStretch.stretchPixelsForMask(stdef.x, lastPixel, stdef.y, lastLine, naxis1,
                    (byte) 255, float1d, byteAry, pixelhist, iMasks);
            this.result = byteAry;
            return null;
        }
    }
}
