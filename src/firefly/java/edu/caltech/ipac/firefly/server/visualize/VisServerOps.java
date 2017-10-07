/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.BandInfo;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.data.table.RawDataSet;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.cache.UserCache;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.util.QueryUtil;
import edu.caltech.ipac.firefly.server.util.multipart.UploadFileInfo;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.ClientFitsHeader;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.FileAndHeaderInfo;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.RequestType;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.RegionFactory;
import edu.caltech.ipac.util.RegionParser;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.dd.RegParseException;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.dd.RegionFileElement;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.draw.AreaStatisticsUtil;
import edu.caltech.ipac.visualize.draw.ColorDisplay;
import edu.caltech.ipac.visualize.draw.HistogramDisplay;
import edu.caltech.ipac.visualize.draw.Metric;
import edu.caltech.ipac.visualize.draw.Metrics;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.CropFile;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.HistogramOps;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.PixelValue;
import edu.caltech.ipac.visualize.plot.PixelValueException;
import edu.caltech.ipac.visualize.plot.PlotGroup;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.util.Cursor;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.image.IndexColorModel;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static edu.caltech.ipac.firefly.visualize.Band.NO_BAND;
import static edu.caltech.ipac.visualize.draw.AreaStatisticsUtil.WhichReadout.LEFT;
import static edu.caltech.ipac.visualize.draw.AreaStatisticsUtil.WhichReadout.RIGHT;


/**
 * @author Trey Roby
 * Date: Aug 7, 2008
 */
public class VisServerOps {

    private static final Logger.LoggerImpl _log = Logger.getLogger();
    private static Counters counters = Counters.getInstance();


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
            WebPlotInitializer wpInit[] = WebPlotFactory.createNew(redR, greenR, blueR);
            WebPlotRequest req= wpInit[0].getPlotState().getPrimaryRequest();
            WebPlotResult retval = makeNewPlotResult(wpInit,req.getProgressKey());
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

        return resultList.toArray(new WebPlotResult[resultList.size()]);
    }

    /**
     * create a new plot
     * note - createPlot does a free resources
     *
     * @return PlotCreationResult the results
     */
    public static WebPlotResult[] createOneFileGroup(List<WebPlotRequest> rList) {

        List<WebPlotResult> resultList = new ArrayList<>(rList.size());
        try {
            WebPlotInitializer wpInitAry[] = WebPlotFactory.createNewGroup(rList);
            for (WebPlotInitializer wpInit : wpInitAry) {
                WebPlotRequest req= wpInitAry[0].getPlotState().getPrimaryRequest();
                resultList.add(makeNewPlotResult(new WebPlotInitializer[]{wpInit}, req.getProgressKey()));
                CtxControl.deletePlotCtx(CtxControl.getPlotCtx(null));
                counters.incrementVis("New Plots");
            }
        } catch (Exception e) {
            for (int i = 0; (i < resultList.size()); i++) {
                resultList.add(createError("on createPlot", null, new WebPlotRequest[]{rList.get(i)}, e));
            }
        }
        return resultList.toArray(new WebPlotResult[resultList.size()]);
    }


    /**
     * create a new plot
     * note - createPlot does a free resources
     *
     * @return PlotCreationResult the results
     */
    public static WebPlotResult createPlot(WebPlotRequest request) {

        try {
            WebPlotInitializer wpInit[] = WebPlotFactory.createNew(request);
            WebPlotResult retval = makeNewPlotResult(wpInit, request.getProgressKey());
            CtxControl.deletePlotCtx(CtxControl.getPlotCtx(null));
            counters.incrementVis("New Plots");
            return retval;
        } catch (Exception e) {
            return createError("on createPlot", null, new WebPlotRequest[]{request}, e);
        }
    }

    public static WebPlotResult checkPlotProgress(String progressKey) {
        Cache cache = UserCache.getInstance();
        ProgressStat stat = (ProgressStat) cache.get(new StringKey(progressKey));
        WebPlotResult retval;
        PlotServUtils.ProgressMessage progressMsg = PlotServUtils.getPlotProgressMessage(stat);

        if (progressMsg != null) {
            retval = new WebPlotResult(null);
            retval.putResult(WebPlotResult.STRING, new DataEntry.Str(progressMsg.message));
        } else {
            retval = WebPlotResult.makeFail("Not found", null, null);
        }
        return retval;
    }


    public static boolean deletePlot(String ctxStr) {
        CtxControl.getPlotCtx(ctxStr);
        return true;
    }

    public static double[] getBeta(PlotState state) {
        double[] resultsAry= new double[] {Double.NaN,Double.NaN,Double.NaN};
        try {
            ActiveCallCtx ctx = CtxControl.prepare(state);
            FitsRead frAry[]= ctx.getFitsReadGroup().getFitsReadAry();
            for(int i= 0; (i<frAry.length);i++) {
                if (frAry[i]!=null) resultsAry[i]= frAry[i].getDefaultBeta();
            }
            return resultsAry;

        } catch (Exception e) {
            return resultsAry;
        }
    }


    private static boolean isDirectFluxAccessAvailable(PlotState state) {
        //todo: make this test more sophisticated

        for(Band b : state.getBands()) {
            if (state.getWorkingFitsFileStr(b).endsWith("gz")) {
                return false;
            }
        }
        return true;


    }

    public static String[] getFlux(PlotState stateAry[], ImagePt ipt) {
        PlotState state= stateAry[0];
        FileAndHeaderInfo fahAry[];
        List<String> fluxList= new ArrayList<>();


        // 1. handle primary plot
        if (isDirectFluxAccessAvailable(state)) {
            List<FileAndHeaderInfo> list = new ArrayList<>();
            for(Band b : state.getBands()) {
                list.add(state.getFileAndHeaderInfo(b));
            }
            fahAry = list.toArray(new FileAndHeaderInfo[list.size()]);
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
                    FileAndHeaderInfo fah[]= new FileAndHeaderInfo[] {stateAry[i].getFileAndHeaderInfo(Band.NO_BAND)};
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
        return fluxList.toArray(new String[fluxList.size()]);
    }

    public static String[] getFileFlux(FileAndHeaderInfo fileAndHeader[], ImagePt ipt) {
        try {
            String retval[] = new String[fileAndHeader.length];
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

    @Deprecated
    public static WebPlotResult getFluxOld(PlotState state, ImagePt ipt) {
        try {
            Band bands[] = state.getBands();
            double fluxes[] = new double[bands.length];
            for (int i = 0; (i < bands.length); i++) {
                fluxes[i] = getFluxValueInMemory(state, bands[i], ipt);
            }
            WebPlotResult retval = new WebPlotResult(state.getContextString());
            retval.putResult(WebPlotResult.FLUX_VALUE, new DataEntry.DoubleArray(fluxes));
            return retval;
        } catch (IOException e) {
            return createError("on getFlux", state, e);
        }
    }

    public static WebPlotResult deleteColorBand(PlotState state, Band band) {
        try {
            ActiveCallCtx ctx = CtxControl.prepare(state);
            ImagePlot plot = ctx.getPlot();
            plot.removeThreeColorBand(band, ctx.getFitsReadGroup());
            state.clearBand(band);
            state.setWebPlotRequest(null, band);
            PlotImages images = reviseImageFile(state, ctx, plot, ctx.getFitsReadGroup());
            WebPlotResult retval = new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.PLOT_IMAGES, images);
            retval.putResult(WebPlotResult.PLOT_STATE, state);
            CtxControl.updateCachedPlot(ctx);
            return retval;
        } catch (Exception e) {
            return createError("on deleteColorBand", state, e);
        }
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


    public static WebPlotResult recomputeStretch(PlotState state, StretchData[] stretchData) {
        return recomputeStretch(state, stretchData, true);
    }

    public static WebPlotResult recomputeStretch(PlotState state,
                                                 StretchData[] stretchData,
                                                 boolean recreateImages) {
        try {
            ActiveCallCtx ctx = CtxControl.prepare(state);
            PlotServUtils.statsLog("stretch");
            PlotImages images = null;
            WebPlotResult retval = new WebPlotResult(ctx.getKey());
            ImagePlot plot = ctx.getPlot();
            if (stretchData.length == 1 && stretchData[0].getBand() == NO_BAND) {
                plot.getHistogramOps(Band.NO_BAND, ctx.getFitsReadGroup()).recomputeStretch(stretchData[0].getRangeValues());
                state.setRangeValues(stretchData[0].getRangeValues(), Band.NO_BAND);
                retval.putResult(WebPlotResult.PLOT_STATE, state);
                images = reviseImageFile(state, ctx, plot, ctx.getFitsReadGroup());
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
                if (recreateImages) {
                    images = reviseImageFile(state, ctx, plot, ctx.getFitsReadGroup());
                    retval.putResult(WebPlotResult.PLOT_IMAGES, images);
                }
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



    public static WebPlotResult crop(PlotState stateAry[], ImagePt c1, ImagePt c2, boolean cropMultiAll) {
        WebPlotResult resultAry[] = new WebPlotResult[stateAry.length];
        boolean success = true;
        for (int i = 0; (i < stateAry.length); i++) {
            resultAry[i] = crop(stateAry[i], c1, c2, cropMultiAll);
            if (success) success = resultAry[i].isSuccess();
        }
        WebPlotResult result = new WebPlotResult(stateAry[0].getContextString());
        result.putResult(WebPlotResult.RESULT_ARY, new DataEntry.WebPlotResultAry(resultAry));
        return success ? result : resultAry[0];
    }


    public static WebPlotResult crop(PlotState state, ImagePt c1, ImagePt c2, boolean cropMultiAll) {
        try {

            Band bands[] = state.getBands();
            WebPlotRequest cropRequest[] = new WebPlotRequest[bands.length];

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
                    cropFits = CropFile.do_crop(fits, (int) c1.getX(), (int) c1.getY(),
                            (int) c2.getX(), (int) c2.getY());
                }

                FitsRead fr[] = FitsCacher.loadFits(cropFits, cropFile);


                if (saveCropFits) {
                    BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(cropFile), 4096);
                    FitsRead.writeFitsFile(stream, fr, cropFits);
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


            WebPlotInitializer wpInitAry[] = (state.isThreeColor() && cropRequest.length == 3) ?
                    WebPlotFactory.createNew(cropRequest[0], cropRequest[1], cropRequest[2]) :
                    WebPlotFactory.createNew(cropRequest[0]);

            int imageIdx = 0;
            for (WebPlotInitializer wpInit : wpInitAry) {
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


            WebPlotResult cropResult = makeNewPlotResult(wpInitAry, null);
            CtxControl.updateCachedPlot(cropResult.getContextStr());

            counters.incrementVis("Crop");
            PlotServUtils.statsLog("crop");


            return cropResult;
        } catch (Exception e) {
            return createError("on crop", state, e);
        }
    }



    private static WebPlotResult getFitsHeaderInfoFull(PlotState state) {


        try {
            HashMap<Band, RawDataSet> rawDataMap = new HashMap<>();
            HashMap<Band, String> stringMap = new HashMap<>();
            WebPlotResult retValue = new WebPlotResult();
            ActiveCallCtx ctx = CtxControl.prepare(state);
            ImagePlot plot = ctx.getPlot();

            for (Band band : state.getBands()) {
                FitsRead fr = plot.getHistogramOps(band, ctx.getFitsReadGroup()).getFitsRead();
                DataGroup dg = getFitsHeaders(fr.getHeader(), plot.getPlotDesc());
                RawDataSet rds = QueryUtil.getRawDataSet(dg);

                String string = String.valueOf(plot.getPixelScale());

                File f = PlotStateUtil.getOriginalFile(state, band);
                if (f == null) f = PlotStateUtil.getWorkingFitsFile(state, band);

                string += ";" + StringUtils.getSizeAsString(f.length(), true);

                rawDataMap.put(band, rds);
                stringMap.put(band, string);
            }
            BandInfo bandInfo = new BandInfo(rawDataMap, stringMap, null);

            retValue.putResult(WebPlotResult.BAND_INFO, bandInfo);
            counters.incrementVis("Fits header");
            return retValue;

        } catch (Exception e) {
            return createError("on getFitsInfo", state, e);
        }

    }

    private static DataGroup getFitsHeaders(Header headers, String name) {
        DataType comment = new DataType("Comments", String.class);
        DataType keyword = new DataType("Keyword", String.class);
        DataType value = new DataType("Value", String.class);
        DataType num = new DataType("#", Integer.class);
        comment.getFormatInfo().setWidth(30);
        value.getFormatInfo().setWidth(10);
        keyword.getFormatInfo().setWidth(10);
        num.getFormatInfo().setWidth(3);
        DataType[] types = new DataType[]{num, keyword, value, comment};
        DataGroup dg = new DataGroup("Headers - " + name, types);

        int i = 0;
        for (Cursor itr = headers.iterator(); itr.hasNext(); ) {
            HeaderCard hc = (HeaderCard) itr.next();
            if (hc.isKeyValuePair()) {
                DataObject row = new DataObject(dg);
                row.setDataElement(types[0], i++);
                row.setDataElement(types[1], hc.getKey());
                row.setDataElement(types[2], hc.getValue());
                row.setDataElement(types[3], hc.getComment());
                dg.add(row);
            }
        }
        return dg;
    }


    private static class UseFullException extends Exception {
    }


    public static WebPlotResult getFitsHeaderInfo(PlotState state) {
        WebPlotResult retValue = new WebPlotResult();

        try {
            HashMap<Band, RawDataSet> rawDataMap = new HashMap<>();
            for (Band band : state.getBands()) {
                File f = PlotStateUtil.getWorkingFitsFile(state, band);
                if (f == null) f = PlotStateUtil.getOriginalFile(state, band);
                Fits fits = new Fits(f);
                BasicHDU hdu[] = fits.read();
                Header header = hdu[0].getHeader();
                if (header.containsKey("EXTEND") && header.getBooleanValue("EXTEND")) {
                    throw new UseFullException();
                } else {
                    DataGroup dg = getFitsHeaders(header, "fits data");
                    RawDataSet rds = QueryUtil.getRawDataSet(dg);
                    rawDataMap.put(band, rds);
                }
            }
            BandInfo bandInfo = new BandInfo(rawDataMap, null, null);

            retValue.putResult(WebPlotResult.BAND_INFO, bandInfo);
            counters.incrementVis("Fits header");

        } catch (UseFullException e) {
            retValue = getFitsHeaderInfoFull(state);
        } catch (Exception e) {
            retValue = createError("on getFitsInfo", state, e);
        }

        return retValue;
    }



    /**
     * 03/24/16, LZ
     * DM-4494
     *
     * @param state plot state
     * @param f the file
     * @param tableID table id
     * @return  map of band and DataGroup
     */

    private static Map<String, DataGroup> getFitsHeaderExtend(PlotState state, File f, String tableID) throws FitsException {
        try {

            HashMap<String, DataGroup> dataMap = new HashMap<>();
            ActiveCallCtx ctx = CtxControl.prepare(state);
            ImagePlot plot = ctx.getPlot();
            for (Band band : state.getBands()) {
                FitsRead fr = plot.getHistogramOps(band, ctx.getFitsReadGroup()).getFitsRead();
                DataGroup dg = getFitsHeaders(fr.getHeader(), plot.getPlotDesc());
                dg.addAttribute("source", ServerContext.replaceWithPrefix(f));
                dg.addAttribute("fileSize", String.valueOf(f.length()));
                dg.addAttribute(TableServerRequest.TBL_ID, tableID + '-' + band.name());

                dataMap.put(band.name(), dg);
            }
            return dataMap;

        } catch (Exception e) {
            throw new FitsException("Can not getFits header");
        }
    }

    /**
     * 03/20/16, LZ
     * DM-4494
     *
     * @param state the state
     * @return map of band and data group
     */

    public static HashMap<String, DataGroup> getFitsHeader(PlotState state, String tableID) throws FitsException {

        HashMap<String, DataGroup> dataMap = new HashMap<>();
        try {
            for (Band band : state.getBands()) {
                File f = PlotStateUtil.getWorkingFitsFile(state, band);
                if (f == null) f = PlotStateUtil.getOriginalFile(state, band);
                Fits fits = new Fits(f);
                TableServerRequest  request = new TableServerRequest("fitsHeaderTale");
                request.setTblId(tableID);

                BasicHDU hdu[] = fits.read();
                Header header = hdu[0].getHeader();
                if (header.containsKey("EXTEND") && header.getBooleanValue("EXTEND")) {
                    dataMap = (HashMap<String, DataGroup>) getFitsHeaderExtend(state, f, tableID);
                } else {
                    DataGroup dg = getFitsHeaders(header, "fits data");
                    dg.addAttribute("source", ServerContext.replaceWithPrefix(f));
                    dg.addAttribute("fileSize", String.valueOf(f.length()));
                    dg.addAttribute(TableServerRequest.TBL_ID, tableID + '-' + band.name());
                    dataMap.put(band.name(), dg);
                }
            }

            ////LZcounters.incrementVis("Fits header");
            return dataMap;

        }
        catch (Exception e  ) {
            throw new FitsException("Can not getFits header");
        }

    }


    public static WebPlotResult getAreaStatistics(PlotState state, ImagePt pt1, ImagePt pt2, ImagePt pt3, ImagePt pt4) {


        try {
            HashMap<Band, HashMap<Metrics, Metric>> metricsMap = new HashMap<>();
            HashMap<Band, String> stringMap = new HashMap<>();
            WebPlotResult retValue = new WebPlotResult();
            ActiveCallCtx ctx = CtxControl.prepare(state);
            ImagePlot plot = ctx.getPlot();

            for (Band band : state.getBands()) {
                // modeled after AreaStatisticsUtil.java lines:654 - 721
                Shape shape;
                GeneralPath genPath = new GeneralPath();
                genPath.moveTo((float) pt1.getX(), (float) pt1.getY());

                genPath.lineTo((float) pt4.getX(), (float) pt4.getY());
                genPath.lineTo((float) pt3.getX(), (float) pt3.getY());
                genPath.lineTo((float) pt2.getX(), (float) pt2.getY());
                genPath.lineTo((float) pt1.getX(), (float) pt1.getY());

                shape = genPath;

                Rectangle2D boundingBox = shape.getBounds2D();

                double minX = boundingBox.getMinX();
                double maxX = boundingBox.getMaxX();
                double minY = boundingBox.getMinY();
                double maxY = boundingBox.getMaxY();

                // smallest x and y within the plot is 0
                // biggest x within the plot is plot.getImageDataWidth()-1
                // biggest y within the plot is plot.getImageDataHeight()-1
                // use getImageLocation method, it takes into account offsetX, offsetY
                PlotGroup.ImageLocation imageLocation = plot.getPlotGroup().getImageLocation(plot);
                if (minX >= imageLocation.getMaxX() - 1 || maxX <= imageLocation.getMinX() ||
                        minY >= imageLocation.getMaxY() - 1 || maxY <= imageLocation.getMinY()) {
                    throw new Exception("The area and the image do not overlap");
                }
                minX = Math.max(imageLocation.getMinX(), minX);
                maxX = Math.min(imageLocation.getMaxX() - 1, maxX);
                minY = Math.max(imageLocation.getMinY(), minY);
                maxY = Math.min(imageLocation.getMaxY() - 1, maxY);

                Rectangle2D.Double newBoundingBox = new Rectangle2D.Double(minX, minY, (maxX - minX), (maxY - minY));
                //what to do about selected band?
                HashMap<Metrics, Metric> metrics = AreaStatisticsUtil.getStatisticMetrics(plot, ctx.getFitsReadGroup(),
                        band, shape, newBoundingBox);

                String html;

                String token = "--;;--";

                Metric max = metrics.get(Metrics.MAX);
                ImageWorkSpacePt maxIp = max.getImageWorkSpacePt();
                html = AreaStatisticsUtil.formatPosHtml(LEFT, plot, maxIp);
                html = html + token + AreaStatisticsUtil.formatPosHtml(RIGHT, plot, maxIp);

                Metric min = metrics.get(Metrics.MIN);
                ImageWorkSpacePt minIp = min.getImageWorkSpacePt();
                html = html + token + AreaStatisticsUtil.formatPosHtml(LEFT, plot, minIp);
                html = html + token + AreaStatisticsUtil.formatPosHtml(RIGHT, plot, minIp);

                Metric centroid = metrics.get(Metrics.CENTROID);
                ImageWorkSpacePt centroidIp = centroid.getImageWorkSpacePt();
                html = html + token + AreaStatisticsUtil.formatPosHtml(LEFT, plot, centroidIp);
                html = html + token + AreaStatisticsUtil.formatPosHtml(RIGHT, plot, centroidIp);

                Metric fwCentroid = metrics.get(Metrics.FW_CENTROID);
                ImageWorkSpacePt fwCentroidIp = fwCentroid.getImageWorkSpacePt();
                html = html + token + AreaStatisticsUtil.formatPosHtml(LEFT, plot, fwCentroidIp);
                html = html + token + AreaStatisticsUtil.formatPosHtml(RIGHT, plot, fwCentroidIp);

                //Add Lon and Lat strings for WorldPt conversion on the client
                Pt pt;
                try {
                    pt = plot.getWorldCoords(maxIp);
                } catch (ProjectionException e) {
                    pt = maxIp;
                }
                html = html + token + String.valueOf(pt.serialize());
                try {
                    pt = plot.getWorldCoords(minIp);
                } catch (ProjectionException e) {
                    pt = minIp;
                }
                html = html + token + String.valueOf(pt.serialize());
                try {
                    pt = plot.getWorldCoords(centroidIp);
                } catch (ProjectionException e) {
                    pt = centroidIp;
                }
                html = html + token + String.valueOf(pt.serialize());
                try {
                    pt = plot.getWorldCoords(fwCentroidIp);
                } catch (ProjectionException e) {
                    pt = fwCentroidIp;
                }
                html = html + token + String.valueOf(pt.serialize());

                metricsMap.put(band, metrics);
                stringMap.put(band, html);

            }

            BandInfo bandInfo = new BandInfo(null, stringMap, metricsMap);

            retValue.putResult(WebPlotResult.BAND_INFO, bandInfo);

            counters.incrementVis("Area Stat");
            return retValue;

        } catch (Exception e) {
            return createError("on getStats", state, e);
        }
    }



    public static WebPlotResult setZoomLevel(PlotState stateAry[], float level, boolean fullScreen) {
        WebPlotResult resultAry[] = new WebPlotResult[stateAry.length];
        boolean success = true;
        for (int i = 0; (i < stateAry.length); i++) {
            resultAry[i] = setZoomLevel(stateAry[i], level, fullScreen);
            if (success) success = resultAry[i].isSuccess();
        }
        WebPlotResult result = new WebPlotResult(stateAry[0].getContextString());
        result.putResult(WebPlotResult.RESULT_ARY, new DataEntry.WebPlotResultAry(resultAry));
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
                                                  boolean fullScreen) throws FailedRequestException,
                                                                             IOException, FitsException {
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


    public static WebPlotResult getColorHistogram(PlotState state,
                                                  Band band,
                                                  int width,
                                                  int height) {
        WebPlotResult retval;
        int dHist[];
        byte dHistColors[];
        Color bgColor = new Color(181, 181, 181);
        HistogramOps hOps;

        try {
            ActiveCallCtx ctx = CtxControl.prepare(state);
            ImagePlot plot = ctx.getPlot();
            if (band == NO_BAND) {
                hOps = plot.getHistogramOps(NO_BAND, ctx.getFitsReadGroup());
                int id = plot.getImageData().getColorTableId();
                if (id == 0 || id == 1) bgColor = new Color(0xCC, 0xCC, 0x99);
            } else {
                hOps = plot.getHistogramOps(band, ctx.getFitsReadGroup());
            }
            Histogram hist = hOps.getDataHistogram();

            dHist = hist.getHistogramArray();
            dHistColors = hOps.getDataHistogramColors(hist, state.getRangeValues(band));

            double meanDataAry[] = new double[dHist.length];
            for (int i = 0; i < meanDataAry.length; i++) {
                meanDataAry[i] = hOps.getMeanValueFromBin(hist, i);
            }


            boolean three = plot.isThreeColor();
            IndexColorModel newColorModel = plot.getImageData().getColorModel();


            HistogramDisplay dataHD = new HistogramDisplay();
            dataHD.setScaleOn2ndValue(true);
            dataHD.setSize(width, height);
            dataHD.setHistogramArray(dHist, dHistColors, newColorModel);
            if (three) {
                dataHD.setColorBand(band);
            }
            dataHD.setBottomSize(4);

            ColorDisplay colorBarC = new ColorDisplay();
            colorBarC.setSize(width, 10);
            if (plot.isThreeColor()) {
                Color color;
                switch (band) {
                    case RED:
                        color = Color.red;
                        break;
                    case GREEN:
                        color = Color.green;
                        break;
                    case BLUE:
                        color = Color.blue;
                        break;
                    default:
                        color = null;
                        break;
                }
                colorBarC.setColor(color);
            } else {
                colorBarC.setColor(newColorModel);
            }


            String templateName = ctx.getImages().getTemplateName();
            String bandDesc = (band != Band.NO_BAND) ? band.toString() + "-" : "";
            String dataFname = templateName + "-dataHist-" + bandDesc + System.currentTimeMillis() + ".png";
            String cbarFname = templateName + "-colorbar-" + bandDesc + System.currentTimeMillis() + ".png";


            File dir = ServerContext.getVisSessionDir();


            File dataFile = PlotServUtils.createHistImage(dataHD, dataHD, dir, bgColor, dataFname);
            File cbarFile = PlotServUtils.createHistImage(colorBarC, colorBarC, dir, bgColor, cbarFname);

            retval = new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.DATA_HISTOGRAM, new DataEntry.IntArray(dHist));
            retval.putResult(WebPlotResult.DATA_BIN_MEAN_ARRAY,
                    new DataEntry.DoubleArray(meanDataAry));
            retval.putResult(WebPlotResult.DATA_HIST_IMAGE_URL,
                    new DataEntry.Str(ServerContext.replaceWithPrefix(dataFile)));
            retval.putResult(WebPlotResult.CBAR_IMAGE_URL,
                    new DataEntry.Str(ServerContext.replaceWithPrefix(cbarFile)));
            counters.incrementVis("Color change");

        } catch (Exception e) {
            retval = createError("on getColorHistogram", state, e);
        } catch (Throwable e) {
            retval = null;
            e.printStackTrace();
        }
        return retval;
    }


    public static WebPlotResult getImagePng(PlotState state, List<StaticDrawInfo> drawInfoList) {
        try {
            ActiveCallCtx ctx = CtxControl.prepare(state);
            String pngFile = PlotPngCreator.createImagePng(ctx.getPlot(), ctx.getFitsReadGroup(), drawInfoList);
            WebPlotResult retval = new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.IMAGE_FILE_NAME, new DataEntry.Str(pngFile));
            CtxControl.updateCachedPlot(ctx);
            return retval;
        } catch (Exception e) {
            return createError("on getImagePng", state, e);
        }
    }

    public static WebPlotResult getImagePngWithRegion(PlotState state,
                                                         String regionData,
                                                         boolean clientIsNorth,
                                                         int clientRotAngle,
                                                         boolean clientFlipY) {
        try {
            List<String> regOutList = StringUtils.parseStringList(regionData);
            List<Region> reglist= regOutList.stream().map(s -> {
                try {
                    List<RegionFileElement> l= RegionFactory.parsePart(s);
                    return (Region) l.get(0);
                } catch (RegParseException e) {
                    return null;
                }
            }).collect(Collectors.toList());


            //====================================================================
            //====================================================================
            //====================================================================
            // ------------ Another concept for doing the same thing
//            ImagePlotBuilder.SimpleResults plotR;
//
//            ActiveCallCtx ctx = CtxControl.prepare(state);
//            WebPlotResult retval = new WebPlotResult(ctx.getKey());
//
//            if (!state.isThreeColor()) {
//                File f = ServerContext.convertToFile(state.getWorkingFitsFileStr(Band.NO_BAND));
//                FitsRead fr;
//                if (clientFlipY) {
//                    fr= FitsCacher.readFits(f)[0];
//                    f= PlotServUtils.createFlipYFile(f, fr);
//                }
//                if (clientIsNorth) {
//                    fr= FitsCacher.readFits(f)[0];
//                    f= PlotServUtils.createRotateNorthFile(f, fr, CoordinateSys.EQ_J2000);
////                    FitsCacher.readFits(f)[0];
//                }
//                else if (clientRotAngle>0) {
//                    fr= FitsCacher.readFits(f)[0];
//                    f= PlotServUtils.createRotatedAngleFile(f, fr, clientRotAngle);
////                    FitsCacher.readFits(f)[0];
//                }
//                WebPlotRequest r= state.getWebPlotRequest(Band.NO_BAND);
//                r.setRequestType(RequestType.FILE);
//                r.setFileName(f.getPath());
//                r.setInitialColorTable(state.getColorTableId());
//                r.setInitialZoomLevel(state.getZoomLevel());
//                r.setZoomType(ZoomType.LEVEL);
//                plotR= ImagePlotBuilder.create(r);
//                String pngFile= PlotPngCreator.createImagePngWithRegions(plotR.getPlot(), plotR.getFrGroup(), reglist);
//
//                retval.putResult(WebPlotResult.IMAGE_FILE_NAME, new DataEntry.Str(pngFile));
//            }
            //====================================================================
            //====================================================================
            //====================================================================

            Map<Band,WebPlotRequest> rMap= new HashMap<>();

            for(Band b : state.getBands()) {
                WebPlotRequest r= state.getWebPlotRequest(b);
                r.setRequestType(RequestType.FILE);
                r.setFileName(state.getWorkingFitsFileStr(b));
                r.setInitialColorTable(state.getColorTableId());
                r.setInitialZoomLevel(state.getZoomLevel());
                r.setZoomType(ZoomType.LEVEL);

                if (clientFlipY) r.setFlipY(true);

                if (clientIsNorth) r.setRotateNorth(true);
                else if (clientRotAngle>0) {
                    r.setRotateFromNorth(true);
                    r.setRotate(true);
                    r.setRotationAngle(clientRotAngle);
                }
                r.setInitialRangeValues(state.getRangeValues(Band.NO_BAND));
                rMap.put(b,r);
            }


            ImagePlotBuilder.SimpleResults plotR;


            if (state.isThreeColor()) {
                plotR= ImagePlotBuilder.create3Color(rMap.get(Band.RED), rMap.get(Band.GREEN),rMap.get(Band.BLUE));
            }
            else {
                plotR= ImagePlotBuilder.create(rMap.get(Band.NO_BAND));
            }
            String pngFile= PlotPngCreator.createImagePngWithRegions(plotR.getPlot(), plotR.getFrGroup(), reglist);

            ActiveCallCtx ctx = CtxControl.prepare(state);
            WebPlotResult retval = new WebPlotResult(ctx.getKey());
            retval.putResult(WebPlotResult.IMAGE_FILE_NAME, new DataEntry.Str(pngFile));


            return retval;
        } catch (Exception e) {
            return createError("on getImagePng", state, e);
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
            retval.putResult(WebPlotResult.REGION_FILE_NAME, new DataEntry.Str(retFile));
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


            retval.putResult(WebPlotResult.REGION_DATA,
                    new DataEntry.Str(StringUtils.combineStringList(rAsStrList)));
            retval.putResult(WebPlotResult.REGION_ERRORS,
                    new DataEntry.Str(StringUtils.combineStringList(r.getMsgList())));

            UploadFileInfo fi = (UploadFileInfo) sessionCache.get(new StringKey(fileKey));
            String title;
            if (fi != null) {
                title = fi.getFileName();
            } else {
                title = fileKey.startsWith("UPLOAD") ? "Region file" : regFile.getName();
            }
            retval.putResult(WebPlotResult.TITLE, new DataEntry.Str(title));
            PlotServUtils.statsLog("ds9Region", fileKey);
            counters.incrementVis("Region read");
            return retval;
        } catch (Exception e) {
            return createError("on getDSRegion", null, e);
        }
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
        retval.putResult(WebPlotResult.REGION_DATA,
                new DataEntry.Str(StringUtils.combineStringList(rAsStrList)));
        retval.putResult(WebPlotResult.REGION_ERRORS,
                new DataEntry.Str(StringUtils.combineStringList(msgList)));
        return retval;
    }

    public static synchronized boolean addSavedRequest(String saveKey, WebPlotRequest request) {
        Cache cache = UserCache.getInstance();
        CacheKey key = new StringKey(saveKey);
        ArrayList<WebPlotRequest> reqList;

        if (cache.isCached(key)) {
            reqList = (ArrayList) cache.get(key);
            reqList.add(request);
            cache.put(key, reqList);
        } else {
            reqList = new ArrayList<>(10);
            reqList.add(request);
            cache.put(key, reqList);
        }
        return true;
    }

    public static WebPlotResult getAllSavedRequest(String saveKey) {
        Cache cache = UserCache.getInstance();
        CacheKey key = new StringKey(saveKey);

        WebPlotResult result;
        if (cache.isCached(key)) {
            ArrayList<WebPlotRequest> reqList = (ArrayList) cache.get(key);
            String[] sAry = new String[reqList.size()];
            for (int i = 0; (i < sAry.length); i++) {
                sAry[i] = reqList.get(i).toString();
            }
            result = new WebPlotResult();
            result.putResult(WebPlotResult.REQUEST_LIST, new DataEntry.StringArray(sAry));
        } else {
            result = WebPlotResult.makeFail("not request found", null, null);
        }
        return result;
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
        double val = 0.0;

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

    private static WebPlotResult createError(String logMsg, PlotState state, WebPlotRequest reqAry[], Exception e) {
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
            _log.warn(e, messages.toArray(new String[messages.size()]));
        }


        return retval;
    }



    private static WebPlotResult makeNewPlotResult(WebPlotInitializer wpInit[], String requestKey) {
        PlotState state = wpInit[0].getPlotState();
        WebPlotResult retval = new WebPlotResult(state.getContextString());
        if (requestKey!=null) retval.setRequestKey(requestKey);
        retval.putResult(WebPlotResult.PLOT_CREATE, new CreatorResults(wpInit));
        return retval;
    }

}

