/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.BandInfo;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.BandState;
import edu.caltech.ipac.firefly.visualize.ClientFitsHeader;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.FileAndHeaderInfo;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebPlotHeaderInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.RegionFactory;
import edu.caltech.ipac.util.RegionParser;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.draw.AreaStatisticsUtil;
import edu.caltech.ipac.visualize.draw.Metric;
import edu.caltech.ipac.visualize.draw.Metrics;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.CropFile;
import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.HistogramOps;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.PixelValue;
import edu.caltech.ipac.visualize.plot.PixelValueException;
import edu.caltech.ipac.visualize.plot.PlotGroup;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.Fits;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static edu.caltech.ipac.firefly.visualize.Band.NO_BAND;


/**
 * @author Trey Roby
 * Date: Aug 7, 2008
 */
public class VisServerOps {

    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static final Counters counters = Counters.getInstance();


    static {
        VisContext.init();
        VisContext.initCounters();
    }

    /**
     * create a new 3 color plot
     * note - createPlot does a free resources
     *
     * @return PlotCreationResult the results
     */
    public static WebPlotResult create3ColorPlot(WebPlotRequest redR, WebPlotRequest greenR, WebPlotRequest blueR) {
        try {
            WebPlotFactory.WebPlotFactoryRet wpRet = WebPlotFactory.createNew(redR, greenR, blueR);
            WebPlotRequest req= wpRet.getWpInit()[0].getPlotState().getPrimaryRequest();
            WebPlotResult retval = makeNewPlotResult(wpRet.getWpInit(),wpRet.getWpHeader(), req.getProgressKey());
            CtxControl.deletePlotCtx(CtxControl.getPlotCtx(null));
            counters.incrementVis("New 3 Color Plots");
            return retval;
        } catch (Exception e) {
            return createError("on createPlot", null, new WebPlotRequest[]{redR, greenR, blueR}, e);
        }
    }


    /**
     * create a group of new plots
     *
     * @return PlotCreationResult the results
     */
    public static WebPlotResult[] createPlotGroup(List<WebPlotRequest> rList, String progressKey) {
        final List<WebPlotResult> resultList = new ArrayList<>(rList.size());

        List<String> keyList = new ArrayList<>(rList.size());
        for (WebPlotRequest wpr : rList) {
            if (wpr.getProgressKey() != null) keyList.add(wpr.getProgressKey());
        }
        PlotServUtils.updatePlotCreateProgress(new ProgressStat(keyList, progressKey));

        ExecutorService executor = Executors.newFixedThreadPool(rList.size());
        boolean allCompleted = false;
        try {
            for (WebPlotRequest r : rList) {
                executor.execute(() -> resultList.add(createPlot(r)));
            }
            executor.shutdown();
            allCompleted = executor.awaitTermination(500, TimeUnit.SECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow();
            if (!allCompleted) {
                _log.info("ExecutorService thread pool was shut down before all plots could complete, after 500 seconds");
            }
        }

        return resultList.toArray(new WebPlotResult[0]);
    }


    /**
     * create a new plot
     * note - createPlot does a free resources
     *
     * @return PlotCreationResult the results
     */
    public static WebPlotResult createPlot(WebPlotRequest request) {

        try {
            WebPlotFactory.WebPlotFactoryRet wpRet= WebPlotFactory.createNew(request);
            WebPlotResult retval = makeNewPlotResult(wpRet.getWpInit(), wpRet.getWpHeader(), request.getProgressKey());
            CtxControl.deletePlotCtx(CtxControl.getPlotCtx(null));
            counters.incrementVis("New Plots");
            return retval;
        } catch (Exception e) {
            return createError("on createPlot", null, new WebPlotRequest[]{request}, e);
        }
    }

    static final boolean USE_DIRECT_FLUX_IF_POSSIBLE = true;

    private static boolean isDirectFluxAccessAvailable(PlotState state) {
        //todo: make this test more sophisticated
        if (!USE_DIRECT_FLUX_IF_POSSIBLE) return false;

        for(Band b : state.getBands()) {
            if (state.getWorkingFitsFileStr(b).endsWith("gz")) {
                return false;
            }

            if (state.isTileCompress(b)) {
                return false;
            }
        }

        return true;
    }

    public static double[] getZAxisAry(PlotState state, ImagePt pt, int hduNum, int ptSize, boolean relatedHDUs) {
        try {
            PlotServUtils.statsLog("z-axis drilldown");
            File fitsFile= ServerContext.convertToFile(state.getWorkingFitsFileStr(NO_BAND));
            return FitsReadUtil.getZAxisAryFromCube(pt, fitsFile, hduNum, ptSize);
        } catch (Exception e) {
            return new double[] {};
        }
    }

    public static double[] getLineDataAry(PlotState state, ImagePt pt, ImagePt pt2, int plane, int hduNum, int drillSize) {
        try {
            PlotServUtils.statsLog("line data");
            File fitsFile= ServerContext.convertToFile(state.getWorkingFitsFileStr(NO_BAND));
            return FitsReadUtil.getLineDataAryFromFile(pt, pt2, plane, fitsFile, hduNum, drillSize);
        } catch (Exception e) {
            return new double[] {};
        }
    }

    public static double[] getPointDataAry(PlotState state, ImagePt[] ptAry, int plane, int hduNum, int drillSize) {
        try {
            PlotServUtils.statsLog("line data");
            File fitsFile= ServerContext.convertToFile(state.getWorkingFitsFileStr(NO_BAND));
            return FitsReadUtil.getPointDataAryFromFile(ptAry, plane, fitsFile, hduNum, drillSize);
        } catch (Exception e) {
            return new double[] {};
        }
    }

    public static String[] getFlux(PlotState[] stateAry, ImagePt ipt) {
        PlotState state= stateAry[0];
        FileAndHeaderInfo[] fahAry;
        List<String> fluxList= new ArrayList<>();


        // 1. handle primary plot
        if (isDirectFluxAccessAvailable(state)) {
            List<FileAndHeaderInfo> list = new ArrayList<>();
            for(Band b : state.getBands()) {
                list.add(state.getFileAndHeaderInfo(b));
            }
            fahAry = list.toArray(new FileAndHeaderInfo[0]);
            String[] res = VisServerOps.getFileFlux(fahAry, ipt);
            fluxList.addAll(Arrays.asList(res));
        }
        else {
            for(Band b : state.getBands()) {
                try {
                    fluxList.add(getFluxValueInMemory(state, b, ipt)+"");
                } catch (IOException e) {
                    fluxList.add("NaN");
                }
            }
        }

        // 2. handle overlays
        if (stateAry.length>1) {
            for(int i=1; (i<stateAry.length);i++) {
                if (isDirectFluxAccessAvailable(stateAry[i])) {
                    FileAndHeaderInfo[] fah= new FileAndHeaderInfo[] {stateAry[i].getFileAndHeaderInfo(Band.NO_BAND)};
                    String[] res = VisServerOps.getFileFlux(fah, ipt);
                    fluxList.add(res[0]);
                }
                else {
                    try {
                        fluxList.add(getFluxValueInMemory(stateAry[i],Band.NO_BAND, ipt)+"");
                    } catch (IOException e) {
                        fluxList.add("NaN");
                    }
                }
            }
        }

        // 3. return all the gathered fluxes
        return fluxList.toArray(new String[0]);
    }
    public static String[] getFileFlux(FileAndHeaderInfo[] fileAndHeader, ImagePt ipt) {
        try {
            String[] retval = new String[fileAndHeader.length];
            int i = 0;
            for (FileAndHeaderInfo fap : fileAndHeader) {
                File f = ServerContext.convertToFile(fap.getfileName());
                retval[i++] = getFluxFromFitsFile(f, fap.getHeader(), ipt) + "";
            }
            return retval;
        } catch (IOException e) {
            return new String[]{PlotState.NO_CONTEXT};
        }
    }


    private static double getFluxValueInMemory(PlotState state,
                                               Band band,
                                               ImagePt ipt) throws IOException {
        if (state == null) throw new IllegalArgumentException("state must not be null");
        double retval;
        if (!CtxControl.isImagePlotAvailable(state.getContextString())) {  // work directly on the file
            FileAndHeaderInfo fap = state.getFileAndHeaderInfo(band);
            File f = ServerContext.convertToFile(fap.getfileName());
            retval = getFluxFromFitsFile(f, fap.getHeader(), ipt);
        } else {
            try {  // use the in memory plot object
                ActiveCallCtx ctx = CtxControl.prepare(state);
                ImagePlot plot = ctx.getPlot();
                retval = plot.getFlux(ctx.getFitsReadGroup(), band, plot.getImageWorkSpaceCoords(ipt));
                CtxControl.updateCachedPlot(ctx);
            } catch (FailedRequestException e) {
                throw new IOException(e);
            } catch (PixelValueException e) {
                retval = Double.NaN;
            }
        }
        return retval;
    }


    public static WebPlotResult changeColor(PlotState state, int colorTableId) {
        try {
            ActiveCallCtx ctx = CtxControl.prepare(state);
            PlotServUtils.statsLog("color", "new_color_id", colorTableId, "old_color_id", state.getColorTableId());
            ImagePlot plot = ctx.getPlot();
            plot.getImageData().setColorTableId(colorTableId);
            state.setColorTableId(colorTableId);
            PlotImages images = reviseImageFile(state, ctx, plot, ctx.getFitsReadGroup());
            WebPlotResult retval = new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.PLOT_IMAGES, images);
            retval.putResult(WebPlotResult.PLOT_STATE, state);
            counters.incrementVis("Color change");
            PlotServUtils.createThumbnail(plot, ctx.getFitsReadGroup(), images, false, state.getThumbnailSize());
            CtxControl.updateCachedPlot(ctx);
            return retval;
        } catch (Exception e) {
            return createError("on changeColor", state, e);
        }
    }


    public static float[] getFloatDataArray(PlotState state, Band band) {
        try {
            long start = System.currentTimeMillis();
            ActiveCallCtx ctx = CtxControl.prepare(state);
            ActiveFitsReadGroup frGroup= ctx.getFitsReadGroup();
            FitsRead fr= frGroup.getFitsRead(band);
            float [] float1d= fr.getRawFloatAry();
            float [] flip1d= DirectStretchUtils.flipFloatArray(float1d,fr.getNaxis1(), fr.getNaxis2());
            long elapse = System.currentTimeMillis() - start;
            PlotServUtils.statsLog("floatAry",
                    "total-MB", ((float)(flip1d.length*4)) / StringUtils.MEG,
                    "Type", state.isThreeColor() ? "3 Color: "+band.toString() : "Standard",
                    "Time", UTCTimeUtil.getHMSFromMills(elapse));
            return flip1d;
        } catch (Exception e) {
            return new float[] {};
        }
    }



    public static byte[] getByteStretchArray(PlotState state, int tileSize, boolean mask, long maskBits) {
        try {
            long start = System.currentTimeMillis();
            ActiveCallCtx ctx = CtxControl.prepare(state);
            ActiveFitsReadGroup frGroup= ctx.getFitsReadGroup();
            byte [] byte1d= DirectStretchUtils.getStretchData(state,frGroup,tileSize,mask, maskBits);
            long elapse = System.currentTimeMillis() - start;
            PlotServUtils.statsLog("byteAry",
                    "total-MB", (float)byte1d.length / StringUtils.MEG,
                    "Type", state.isThreeColor() ? "3 Color" : "Standard",
                    "Time", UTCTimeUtil.getHMSFromMills(elapse));
            return byte1d;
        } catch (Exception e) {
            return new byte[] {};
        }

    }

    public static WebPlotResult recomputeStretch(PlotState state, StretchData[] stretchData) {
        try {
            ActiveCallCtx ctx = CtxControl.prepare(state);
            PlotServUtils.statsLog("stretch");
            PlotImages images = null;
            WebPlotResult retval = new WebPlotResult(ctx.getKey());
            ImagePlot plot = ctx.getPlot();
            if (stretchData.length == 1 && stretchData[0].getBand() == NO_BAND) {
                plot.getHistogramOps(Band.NO_BAND, ctx.getFitsReadGroup()).recomputeStretch(stretchData[0].getRangeValues(), state.getColorTableId());
                state.setRangeValues(stretchData[0].getRangeValues(), Band.NO_BAND);
                retval.putResult(WebPlotResult.PLOT_STATE, state);
                images = reviseImageFile(state, ctx, plot, ctx.getFitsReadGroup());
                // update range values in state, in case there are computed values (ex. default asinh_q)
                state.setRangeValues(stretchData[0].getRangeValues(), Band.NO_BAND);
                retval.putResult(WebPlotResult.PLOT_IMAGES, images);
            } else if (plot.isThreeColor()) {
                for (StretchData sd : stretchData) {
                    HistogramOps ops = plot.getHistogramOps(sd.getBand(), ctx.getFitsReadGroup());
                    state.setBandVisible(sd.getBand(), sd.isBandVisible());
                    if (sd.isBandVisible() && ops != null) {
                        ops.recomputeStretch(sd.getRangeValues());
                        state.setRangeValues(sd.getRangeValues(), sd.getBand());
                        state.setBandVisible(sd.getBand(), sd.isBandVisible());
                    }
                }
                images = reviseImageFile(state, ctx, plot, ctx.getFitsReadGroup());
                // update range values in state, in case there are computed values (ex. default asinh_q)
                for (StretchData sd : stretchData) {
                    if (sd.isBandVisible()) {
                        state.setRangeValues(sd.getRangeValues(), sd.getBand());
                    }
                }
                retval.putResult(WebPlotResult.PLOT_IMAGES, images);
                retval.putResult(WebPlotResult.PLOT_STATE, state);

            } else {
                FailedRequestException fe = new FailedRequestException(
                        "Some Context wrong, isThreeColor()==true && only band passed is NO_BAND");
                retval = createError("on recomputeStretch", state, fe);
            }
            if (images != null) {
                PlotServUtils.createThumbnail(plot, ctx.getFitsReadGroup(), images, false, state.getThumbnailSize());
            }
            CtxControl.updateCachedPlot(ctx);
            counters.incrementVis("Stretch change");
            return retval;
        } catch (Exception e) {
            return createError("on recomputeStretch", state, e);
        }
    }



    public static WebPlotResult crop(PlotState[] stateAry, ImagePt c1, ImagePt c2, boolean cropMultiAll) {
        WebPlotResult[] resultAry = new WebPlotResult[stateAry.length];
        boolean success = true;
        for (int i = 0; (i < stateAry.length); i++) {
            resultAry[i] = crop(stateAry[i], c1, c2, cropMultiAll);
            if (success) success = resultAry[i].isSuccess();
        }
        WebPlotResult result = new WebPlotResult(stateAry[0].getContextString());
        result.putResult(WebPlotResult.RESULT_ARY, resultAry);
        return success ? result : resultAry[0];
    }


    public static WebPlotResult crop(PlotState state, ImagePt c1, ImagePt c2, boolean cropMultiAll) {
        try {

            Band[] bands = state.getBands();
            BandState[] bandStateAry = state.getBandStateAry();
            WebPlotRequest[] cropRequest = new WebPlotRequest[bands.length];

            for (int i = 0; (i < bands.length); i++) {

                File workingFilsFile = PlotStateUtil.getWorkingFitsFile(state, bands[i]);
                String fName = workingFilsFile.getName();
                String multiStr= state.isMultiImageFile(bands[i]) ? "-multi" : "-"+state.getImageIdx(bands[i]);
                File cropFile = File.createTempFile(FileUtil.getBase(fName) + multiStr + "-crop",
                        "." + FileUtil.FITS,
                        ServerContext.getVisSessionDir());

                Fits cropFits;


                boolean saveCropFits = true;
                if (state.isMultiImageFile(bands[i])) {
                    if (cropMultiAll) {
                        File originalFile = PlotStateUtil.getOriginalFile(state, bands[i]);
                        CropFile.crop_extensions(originalFile.getPath(), cropFile.getPath(),
                                (int) c1.getX(), (int) c1.getY(),
                                (int) c2.getX(), (int) c2.getY());
                        cropFits = new Fits(cropFile);
                        saveCropFits = false;
                    } else {
                        Fits fits = new Fits(PlotStateUtil.getWorkingFitsFile(state, bands[i]));
                        cropFits = CropFile.do_crop(fits, state.getImageIdx(bands[i]) + 1,
                                (int) c1.getX(), (int) c1.getY(),
                                (int) c2.getX(), (int) c2.getY());
                    }
                } else {

                    Fits fits = new Fits(PlotStateUtil.getWorkingFitsFile(state, bands[i]));

                    String multiImageExtValue = bandStateAry[i].getWebPlotRequest().getMultiImageExts();
                    int multiImageExt = multiImageExtValue!=null?Integer.parseInt(multiImageExtValue):0;

                    if ( multiImageExt>0){
                        cropFits = CropFile.do_crop(fits, multiImageExt, (int) c1.getX(), (int) c1.getY(),
                                (int) c2.getX(), (int) c2.getY());
                    }
                    else {
                        cropFits = CropFile.do_crop(fits, (int) c1.getX(), (int) c1.getY(),
                                (int) c2.getX(), (int) c2.getY());
                    }
                }

                FitsRead[] fr= FitsCacher.loadFits(cropFits, cropFile).getFitReadAry();


                if (saveCropFits) {
                    BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(cropFile), 4096);
                    FitsReadUtil.writeFitsFile(stream, fr, cropFits);
                    FileUtil.silentClose(stream);
                }


                String fReq = ServerContext.replaceWithPrefix(cropFile);
                cropRequest[i] = WebPlotRequest.makeFilePlotRequest(fReq, state.getZoomLevel());
                cropRequest[i].setTitle(state.isThreeColor() ?
                        "Cropped Plot (" + bands[i].toString() + ")" :
                        "Cropped Plot");
                cropRequest[i].setThumbnailSize(state.getThumbnailSize());
                PlotStateUtil.initRequestFromState(cropRequest[i], state, bands[i]);
            }


            WebPlotFactory.WebPlotFactoryRet wpRet = (state.isThreeColor() && cropRequest.length == 3) ?
                    WebPlotFactory.createNew(cropRequest[0], cropRequest[1], cropRequest[2]) :
                    WebPlotFactory.createNew(cropRequest[0]);

            int imageIdx = 0;
            for (WebPlotInitializer wpInit : wpRet.getWpInit()) {
                PlotState cropState = wpInit.getPlotState();
                cropState.addOperation(PlotState.Operation.CROP);
                cropState.setWorkingFitsFileStr(cropRequest[0].getFileName(), bands[0]);
                for (int i = 0; (i < bands.length); i++) {
                    cropState.setWorkingFitsFileStr(cropRequest[i].getFileName(), bands[i]);
                    if (!cropMultiAll) {
                        cropState.setOriginalImageIdx(state.getOriginalImageIdx(bands[i]), bands[i]);
                    }
                    cropState.setImageIdx(imageIdx, bands[i]);
                }
                imageIdx++;
            }


            WebPlotResult cropResult = makeNewPlotResult(wpRet.getWpInit(), wpRet.getWpHeader(), null);
            CtxControl.updateCachedPlot(cropResult.getContextStr());

            counters.incrementVis("Crop");
            PlotServUtils.statsLog("crop");


            return cropResult;
        } catch (Exception e) {
            return createError("on crop", state, e);
        }
    }


    // create an ellipse containing the information of ellipse center and length of major and minor axis
    private static Ellipse2D.Double makeRotatedEllipse(ImagePt pt1, ImagePt pt2, ImagePt pt3) {
        double a = Math.sqrt(Math.pow((pt1.getX() - pt2.getX()), 2) + Math.pow((pt1.getY() - pt2.getY()), 2));
        double b = Math.sqrt(Math.pow((pt2.getX() - pt3.getX()), 2) + Math.pow((pt2.getY() - pt3.getY()), 2));
        double x_c = (pt1.getX() + pt3.getX())/2;
        double y_c = (pt1.getY() + pt3.getY())/2;
        return new Ellipse2D.Double(x_c - a/2, y_c - b/2, a, b);
    }

    public static WebPlotResult getAreaStatistics(PlotState state, ImagePt pt1, ImagePt pt2, ImagePt pt3, ImagePt pt4, String areaShape, double rotateAngle) {


        try {
            HashMap<Band, HashMap<Metrics, Metric>> metricsMap = new HashMap<>();
            WebPlotResult retValue = new WebPlotResult();
            ActiveCallCtx ctx = CtxControl.prepare(state);
            ImagePlot plot = ctx.getPlot();

            for (Band band : state.getBands()) {
                // modeled after AreaStatisticsUtil.java lines:654 - 721
                Shape shape;

                if (areaShape.equals("circle")) {
                    double x_1 = Math.min(pt1.getX(), pt3.getX());
                    double x_2 = Math.max(pt1.getX(), pt3.getX());
                    double y_1 = Math.min(pt1.getY(), pt3.getY());
                    double y_2 = Math.max(pt1.getY(), pt3.getY());

                    Ellipse2D.Double ellipseArea;

                    if (rotateAngle != 0.0) {
                        ellipseArea = makeRotatedEllipse(pt1, pt2, pt3);
                    } else {
                        ellipseArea = new Ellipse2D.Double(x_1, y_1, (x_2 - x_1), (y_2 - y_1));
                    }
                    shape = ellipseArea;

                } else { // rectangle
                    GeneralPath genPath = new GeneralPath();
                    genPath.moveTo((float) pt1.getX(), (float) pt1.getY());

                    genPath.lineTo((float) pt4.getX(), (float) pt4.getY());
                    genPath.lineTo((float) pt3.getX(), (float) pt3.getY());
                    genPath.lineTo((float) pt2.getX(), (float) pt2.getY());
                    genPath.lineTo((float) pt1.getX(), (float) pt1.getY());

                    shape = genPath;
                }

                Rectangle2D boundingBox = shape.getBounds2D();

                double minX = boundingBox.getMinX();
                double maxX = boundingBox.getMaxX();
                double minY = boundingBox.getMinY();
                double maxY = boundingBox.getMaxY();

                PlotGroup.ImageLocation imageLocation = plot.getPlotGroup().getImageLocation(plot);
                minX = Math.max(imageLocation.getMinX(), minX);
                maxX = Math.min(imageLocation.getMaxX() - 1, maxX);
                minY = Math.max(imageLocation.getMinY(), minY);
                maxY = Math.min(imageLocation.getMaxY() - 1, maxY);

                Rectangle2D.Double newBoundingBox = new Rectangle2D.Double(minX, minY, (maxX - minX), (maxY - minY));
                //what to do about selected band?
                HashMap<Metrics, Metric> metrics = AreaStatisticsUtil.getStatisticMetrics(plot, ctx.getFitsReadGroup(),
                                                                                          band, shape, newBoundingBox,
                                                                                          rotateAngle);
               metricsMap.put(band, metrics);
            }

            BandInfo bandInfo = new BandInfo(metricsMap);

            retValue.putResult(WebPlotResult.BAND_INFO, bandInfo);

            counters.incrementVis("Area Stat");
            return retValue;

        } catch (Exception e) {
            return createError("on getStats", state, e);
        }
    }



    public static WebPlotResult setZoomLevel(PlotState[] stateAry, float level, boolean fullScreen) {
        WebPlotResult[] resultAry = new WebPlotResult[stateAry.length];
        boolean success = true;
        for (int i = 0; (i < stateAry.length); i++) {
            resultAry[i] = setZoomLevel(stateAry[i], level, fullScreen);
            if (success) success = resultAry[i].isSuccess();
        }
        WebPlotResult result = new WebPlotResult(stateAry[0].getContextString());
        result.putResult(WebPlotResult.RESULT_ARY, resultAry);
        return result;
    }


    public static WebPlotResult setZoomLevel(PlotState state, float level, boolean fullScreen) {
        WebPlotResult retval;

        try {
            String ctxStr = state.getContextString();
            if (!CtxControl.isCtxAvailable(ctxStr) || fullScreen ) {
                retval = setZoomLevelFull(state, level, fullScreen);
            } else {
                retval = setZoomLevelFast(state, level);
            }
            counters.incrementVis("Zoom");
        } catch (Exception e) {
            retval = createError("on setZoomLevel", state, e);
        }
        return retval;
    }

    private static WebPlotResult setZoomLevelFast(PlotState state, float level) {


        PlotClientCtx ctx= CtxControl.getPlotCtx(state.getContextString());
        PlotImages originalImages = ctx.getImages();
        float oldLevel = state.getZoomLevel();
        float scale = level / oldLevel;
        int targetWidth = Math.round(originalImages.getScreenWidth() * scale);
        int targetHeight = Math.round(originalImages.getScreenHeight() * scale);


        WebPlotResult retval;
        String details = "Fast: From " + PlotServUtils.convertZoomToString(oldLevel) +
                " to " + PlotServUtils.convertZoomToString(level);
        PlotServUtils.statsLog("zoom", details);

        PlotImages.ThumbURL thumb = ctx.getImages().getThumbnail();
        state.setZoomLevel(level);
        PlotImages images = redefineTiles(state, level, targetWidth, targetHeight);
        ctx.setImages(images);
        images.setThumbnail(thumb);
        retval = new WebPlotResult(state.getContextString());
        retval.putResult(WebPlotResult.PLOT_IMAGES, images);
        retval.putResult(WebPlotResult.PLOT_STATE, state);
        ctx.setPlotState(state);
        ImagePlot plot = ctx.getCachedPlot();
        if (plot != null) plot.setZoomTo(level);
        return retval;
    }

    private static WebPlotResult setZoomLevelFull(PlotState state,
                                                  float level,
                                                  boolean fullScreen) throws FailedRequestException, IOException{
        WebPlotResult retval;
        ActiveCallCtx ctx = CtxControl.prepare(state);
        ImagePlot plot = ctx.getPlot();
        String details = "From " + PlotServUtils.convertZoomToString(state.getZoomLevel()) +
                " to " + PlotServUtils.convertZoomToString(level);
        PlotServUtils.statsLog("zoom", details);
        PlotImages.ThumbURL thumb = ctx.getImages().getThumbnail();
        plot.getPlotGroup().setZoomTo(level);
        state.setZoomLevel(level);
        PlotImages images = reviseImageFile(state, ctx, plot, ctx.getFitsReadGroup(), fullScreen);
        images.setThumbnail(thumb);
        retval = new WebPlotResult(ctx.getKey());
        retval.putResult(WebPlotResult.PLOT_IMAGES, images);
        retval.putResult(WebPlotResult.PLOT_STATE, state);
        PlotServUtils.createThumbnail(plot, ctx.getFitsReadGroup(), images, false, state.getThumbnailSize());
        CtxControl.updateCachedPlot(ctx);
        return retval;
    }


    public static WebPlotResult getColorHistogram(PlotState state, Band band) {
        try {
            ActiveCallCtx ctx = CtxControl.prepare(state);
            ImagePlot plot = ctx.getPlot();
            HistogramOps hOps = plot.getHistogramOps(band, ctx.getFitsReadGroup());
            Histogram hist = hOps.getDataHistogram();

            int[] dHist = hist.getHistogramArray();
            byte[] dHistColors = hOps.getDataHistogramColors(hist, state.getRangeValues(band));
            double [] meanDataAry = new double[dHist.length];
            for (int i = 0; i < meanDataAry.length; i++) meanDataAry[i] = hOps.getMeanValueFromBin(hist, i);

            WebPlotResult retval = new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.DATA_HISTOGRAM, dHist);
            retval.putResult(WebPlotResult.DATA_BIN_MEAN_ARRAY, meanDataAry);
            retval.putResult(WebPlotResult.DATA_BIN_COLOR_IDX, dHistColors);
            counters.incrementVis("getColorHistogram");
            return retval;

        } catch (Exception e) {
            return createError("on getColorHistogram", state, e);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }


    public static WebPlotResult saveDS9RegionFile(String regionData) {
        try {
            File f = File.createTempFile("regionDownload-", ".reg", ServerContext.getVisSessionDir());
            List<String> regOutList = StringUtils.parseStringList(regionData);
            RegionParser rp = new RegionParser();
            rp.saveFile(f, regOutList, "Region file generated by IRSA");
            String retFile = ServerContext.replaceWithPrefix(f);
            WebPlotResult retval = new WebPlotResult();
            retval.putResult(WebPlotResult.REGION_FILE_NAME, retFile);
            counters.incrementVis("Region save");
            return retval;
        } catch (Exception e) {
            return createError("on getImagePng", null, e);
        }
    }

    public static WebPlotResult getDS9Region(String fileKey) {


        try {
            Cache sessionCache = UserCache.getInstance();
            File regFile = ServerContext.convertToFile(fileKey);
            if (regFile == null || !regFile.canRead()) {
                UploadFileInfo tmp = (UploadFileInfo) (sessionCache.get(new StringKey(fileKey)));
                regFile = tmp.getFile();
            }
            RegionParser parser = new RegionParser();
            RegionFactory.ParseRet r = parser.processFile(regFile);
            WebPlotResult retval = new WebPlotResult();
            List<String> rAsStrList = toStringList(r.getRegionList());


            retval.putResult(WebPlotResult.REGION_DATA, StringUtils.combineStringList(rAsStrList));
            retval.putResult(WebPlotResult.REGION_ERRORS, StringUtils.combineStringList(r.getMsgList()));

            UploadFileInfo fi = (UploadFileInfo) sessionCache.get(new StringKey(fileKey));
            String title;
            if (fi != null) {
                title = fi.getFileName();
            } else {
                title = fileKey.startsWith("UPLOAD") ? "Region file" : regFile.getName();
            }
            retval.putResult(WebPlotResult.TITLE, title);
            PlotServUtils.statsLog("ds9Region", fileKey);
            counters.incrementVis("Region read");
            return retval;
        } catch (Exception e) {
            return createError("on getDSRegion", null, e);
        }
    }

    public static WebPlotResult getRelocatableRegions(String fileKey) {
        List<String> rAsStrList =  new ArrayList<>();
        List<String> msgList =  new ArrayList<>();
        WebPlotResult retval = new WebPlotResult();

        try {
            Cache sessionCache = UserCache.getInstance();
            File fpFile = ServerContext.convertToFile(fileKey);

            if (fpFile == null || !fpFile.canRead()) {
                UploadFileInfo tmp = (UploadFileInfo) (sessionCache.get(new StringKey(fileKey)));
                fpFile = tmp.getFile();
            }


            InputStream in = new FileInputStream(fpFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String tmpLine;

            while ((tmpLine = br.readLine()) != null) {
                tmpLine = tmpLine.trim();
                if (!tmpLine.startsWith("#")) rAsStrList.add(tmpLine);
            }
            if (rAsStrList.size() == 0) {
                msgList.add("no region is defined in the footprint file");
            }
        } catch (Exception e) {
                retval = createError("on getRelocatableRegion", null, e);
        }

        retval.putResult(WebPlotResult.REGION_DATA, StringUtils.combineStringList(rAsStrList));
        retval.putResult(WebPlotResult.REGION_ERRORS, StringUtils.combineStringList(msgList));
        return retval;
    }

    public static WebPlotResult getFootprintRegion(String fpInfo) {

        List<String> rAsStrList =  new ArrayList<>();
        List<String> msgList =  new ArrayList<>();
        WebPlotResult retval = new WebPlotResult();
        String fileName;

        if ((fileName = VisContext.getFootprint(fpInfo)) != null) {
            int idx = fpInfo.indexOf('_');

            String tag = idx >= 0 ? fpInfo.substring(idx + 1) : fpInfo;

            try {
                InputStream in = VisServerOps.class.getClassLoader().getResourceAsStream(fileName);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String tmpLine;

                while ((tmpLine = br.readLine()) != null) {
                    tmpLine = tmpLine.trim();
                    if (!tmpLine.startsWith("#") && ((tmpLine.contains("tag={" + tag)) ||
                            (!tmpLine.contains("tag"))))
                        rAsStrList.add(tmpLine);
                }
                if (rAsStrList.size() == 0) {
                    msgList.add("no region is defined in the footprint file");
                }
            } catch (Exception e) {
                retval = createError("on getFootprintRegion", null, e);
            }
        } else {
            msgList.add("no footprint description file is found");
        }
        retval.putResult(WebPlotResult.REGION_DATA, StringUtils.combineStringList(rAsStrList));
        retval.putResult(WebPlotResult.REGION_ERRORS, StringUtils.combineStringList(msgList));
        return retval;
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private static List<String> toStringList(List<Region> rList) {
        List<String> retval = new ArrayList<>(rList.size());
        for (Region r : rList) retval.add(r.serialize());
        return retval;
    }

    private static PlotImages reviseImageFile(PlotState state, ActiveCallCtx ctx,
                                              ImagePlot plot, ActiveFitsReadGroup frGroup) throws IOException {
        return reviseImageFile(state, ctx, plot, frGroup, false);
    }


    private static PlotImages reviseImageFile(PlotState state, ActiveCallCtx ctx, ImagePlot plot, ActiveFitsReadGroup frGroup,
                                              boolean fullScreen) throws IOException {

        String base = PlotServUtils.makeTileBase(state);

        File dir = ServerContext.getVisSessionDir();
        int tileCnt = fullScreen ? 1 : 2;
        PlotImages images = PlotServUtils.writeImageTiles(dir, base, plot, frGroup, fullScreen, tileCnt);
        ctx.setImages(images);
        return images;
    }


    private static PlotImages redefineTiles(PlotState state, float zFactor, int screenWidth, int screenHeight) {
        return PlotServUtils.defineTiles(
                        ServerContext.getVisSessionDir(),
                        PlotServUtils.makeTileBase(state),
                        zFactor, screenWidth, screenHeight);
    }


    private static double getFluxFromFitsFile(File f,
                                              ClientFitsHeader clientFitsHeader,
                                              ImagePt ipt) throws IOException {
        RandomAccessFile fitsFile = null;
        double val;

        try {
            if (f.canRead()) {
                fitsFile = new RandomAccessFile(f, "r");
                if (clientFitsHeader == null) {
                    throw new IOException("Can't read file, ClientFitsHeader is null");
                }
                val = PixelValue.pixelVal(fitsFile, (int) ipt.getX(), (int) ipt.getY(), clientFitsHeader);
            } else {
                throw new IOException("Can't read file or it does not exist");

            }
        } catch (PixelValueException e) {
            val = Double.NaN;
        } finally {
            FileUtil.silentClose(fitsFile);
        }
        return val;
    }

    private static WebPlotResult createError(String logMsg, PlotState state, Exception e) {
        return createError(logMsg, state, null, e);
    }

    private static WebPlotResult createError(String logMsg, PlotState state, WebPlotRequest[] reqAry, Exception e) {
        WebPlotResult retval;
        boolean userAbort = false;
        String progressKey = "";
        String plotId= null;
        if (reqAry != null && reqAry.length>0) {
            plotId= reqAry[0].getPlotId();
            for (WebPlotRequest wpr : reqAry) {
                if (wpr != null) {
                    progressKey = wpr.getProgressKey();
                    break;
                }
            }
        }

        if (e instanceof FailedRequestException) {
            FailedRequestException fe = (FailedRequestException) e;
            retval = WebPlotResult.makeFail(fe.getUserMessage(), fe.getUserMessage(), fe.getDetailMessage(), progressKey, plotId);
            userAbort = VisContext.PLOT_ABORTED.equals(fe.getDetailMessage());
        } else if (e instanceof SecurityException) {
            retval = WebPlotResult.makeFail("No Access", "You do not have access to this data,", e.getMessage(), progressKey, plotId);
        } else {
            retval = WebPlotResult.makeFail("Server Error, Please Report", e.getMessage(), null, progressKey, plotId);
        }
        List<String> messages = new ArrayList<>(8);
        messages.add(logMsg);
        if (state != null) {
            messages.add("Context String: " + state.getContextString());
            try {
                if (state.isThreeColor()) {
                    for (Band band : state.getBands()) {
                        messages.add("Fits Filename (" + band.toString() + "): " + PlotStateUtil.getWorkingFitsFile(state, band));
                    }

                } else {
                    messages.add("Fits Filename: " + PlotStateUtil.getWorkingFitsFile(state, NO_BAND));

                }
            } catch (Exception ignore) {
                // if anything goes wrong here we have to recover, this is only for logging
            }
            CtxControl.freeCtxResources(state.getContextString());
        }
        if (reqAry != null) {
            for (WebPlotRequest req : reqAry) {
                if (req != null) messages.add("Request: " + req.prettyString());
            }
        }

        if (userAbort) {
            _log.info(logMsg + ": " + VisContext.PLOT_ABORTED);
        }
        else {
            if (e.getMessage().toLowerCase().contains("area not covered")) {
                messages.add(e.getMessage());
                _log.info( messages.toArray(new String[0]));
            }
            else {
                _log.warn(e, messages.toArray(new String[0]));
            }
        }


        return retval;
    }



    private static WebPlotResult makeNewPlotResult(WebPlotInitializer[] wpInit, WebPlotHeaderInitializer wpHeader, String requestKey) {
        PlotState state = wpInit[0].getPlotState();
        WebPlotResult retval = new WebPlotResult(state.getContextString());
        if (requestKey!=null) retval.setRequestKey(requestKey);
        retval.putResult(WebPlotResult.PLOT_CREATE, new CreatorResults(wpHeader, wpInit));
        return retval;
    }

}
