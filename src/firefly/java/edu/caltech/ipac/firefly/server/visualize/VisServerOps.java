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
import edu.caltech.ipac.firefly.server.visualize.DirectStretchUtils.CompressType;
import edu.caltech.ipac.firefly.server.visualize.DirectStretchUtils.StretchDataInfo;
import edu.caltech.ipac.firefly.server.visualize.WebPlotFactory.WebPlotFactoryRet;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.BandState;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotHeaderInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.RegionParser;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.cache.Cache;
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.draw.AreaStatisticsUtil;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.CropFile;
import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.PixelValue;
import edu.caltech.ipac.visualize.plot.plotdata.FitsExtract;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.Fits;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
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
    }

    /**
     * create a new 3 color plot
     * @return PlotCreationResult the results
     */
    public static WebPlotResult create3ColorPlot(WebPlotRequest redR, WebPlotRequest greenR, WebPlotRequest blueR) {
        try {
            counters.incrementVis("New 3 Color Plots");
            WebPlotFactoryRet wpRet = WebPlotFactory.createNew(redR, greenR, blueR);
            WebPlotRequest req= wpRet.wpInit()[0].plotState().getPrimaryRequest();
            return makeNewPlotResult(wpRet.wpInit(),wpRet.wpHeader(), req.getProgressKey());
        } catch (Exception e) {
            return createError("on createPlot", null, new WebPlotRequest[]{redR, greenR, blueR}, e);
        }
    }


    /**
     * create a group of new plots
     * @return PlotCreationResult the results
     */
    public static List<WebPlotResult> createPlotGroup(List<WebPlotRequest> rList, String progressKey) {

        List<String> keyList= rList.stream().map(WebPlotRequest::getProgressKey).filter(Objects::nonNull).toList();
        PlotServUtils.updateProgress(new ProgressStat(keyList, progressKey));

        ExecutorService executor = Executors.newFixedThreadPool(rList.size());
        boolean allCompleted = false;
        try {
            final List<WebPlotResult> resultList = new ArrayList<>();
            rList.forEach(r -> executor.execute(() -> resultList.add(createPlot(r))));
            executor.shutdown();
            allCompleted = executor.awaitTermination(500, TimeUnit.SECONDS);
            return resultList;
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        } finally {
            executor.shutdownNow();
            if (!allCompleted) {
                _log.warn("ExecutorService thread pool was shut down before all plots could complete, after 500 seconds");
            }
        }
    }

    /**
     * create a new plot
     * @return PlotCreationResult the results
     */
    public static WebPlotResult createPlot(WebPlotRequest request) {
        try {
            counters.incrementVis("New Plots");
            WebPlotFactoryRet wpRet= WebPlotFactory.createNew(request);
            return makeNewPlotResult(wpRet.wpInit(), wpRet.wpHeader(), request.getProgressKey());
        } catch (Exception e) {
            return createError("on createPlot", null, new WebPlotRequest[]{request}, e);
        }
    }

    private interface Extractor { List<Number> getData(File fitsFile) throws Exception; }

    private static List<Number> getDataAry(String desc, PlotState state, Extractor extractor) {
        try {
            CtxControl.confirmFiles(state);
            PlotServUtils.statsLog(desc);
            File fitsFile= ServerContext.convertToFile(state.getWorkingFitsFileStr(NO_BAND));
            return extractor.getData(fitsFile);
        } catch (Exception e) {
            return Collections.emptyList();
        }

    }
    public static List<Number> getZAxisAry(PlotState state, ImagePt pt, int hduNum,
                                           int ptSize, FitsExtract.CombineType ct) {
        return getDataAry("z-axis drilldown", state,
                (f) -> FitsExtract.getZAxisAryFromCube(pt, f, hduNum, ptSize, ct));
    }

    public static List<Number> getLineDataAry(PlotState state, ImagePt pt, ImagePt pt2, int plane, int hduNum,
                                              int drillSize, FitsExtract.CombineType ct) {
        return getDataAry("point data", state,
                (f) -> FitsExtract.getLineDataAryFromFile(pt, pt2, plane, f, hduNum, hduNum, drillSize, ct));
    }

    public static List<Number> getPointDataAry(PlotState state, ImagePt[] ptAry, int plane, int hduNum,
                                               int ptSizeX, int ptSizeY, FitsExtract.CombineType ct) {
        return getDataAry("line data", state,
                (f) -> FitsExtract.getPointDataAryFromFile(ptAry, plane, f, hduNum, hduNum, ptSizeX, ptSizeY, ct));
    }

    public static List<PixelValue.Result> getFlux(PlotState[] stateAry, ImagePt ipt) {
        PlotState primState= stateAry[0];

        // 1. handle primary plot
        var faHList = Arrays.stream(primState.getBands()).map(primState::getFileAndHeaderInfo).toList();

        try {
            CtxControl.confirmFiles(stateAry[0]);
        } catch (FailedRequestException e) {
            return faHList.stream().map( f -> PixelValue.Result.makeUnavailable()).toList();
        }

        var baseList= getFileFlux(faHList, ipt);
        if (stateAry.length==1) return baseList;

        // 2. if there are overlays - handle them
        List<PixelValue.Result> fluxList= new ArrayList<>(baseList);
        for(int i=1; (i<stateAry.length);i++) {
            var faHOverlayList= Collections.singletonList(stateAry[i].getFileAndHeaderInfo(Band.NO_BAND));
            fluxList.add(getFileFlux(faHOverlayList, ipt).get(0));
        }
        return fluxList;
    }

    private static List<PixelValue.Result> getFileFlux(List<BandState.FileAndHeaderInfo> fileAndHeader, ImagePt ipt) {
        return fileAndHeader.stream()
                .map (fap -> PixelValue.getPixelValue(ServerContext.convertToFile(fap.fileName()), ipt, fap.header()))
                .toList();
    }

    public static byte[] getByteStretchArray(PlotState state, int tileSize, boolean mask, long maskBits, CompressType ct) {
        DirectStretchUtils.StretchDataInfo data;
        try {
            ActiveFitsReadGroup frGroup= CtxControl.prepare(state);
            Cache memCache= CacheManager.getCache(Cache.TYPE_VIS_SHARED_MEM);
            CacheKey stretchDataKey= new StringKey(state.getContextString()+"byte-data");
            data= (StretchDataInfo)memCache.get(stretchDataKey);
            String fromCache= "";
            if (data!=null && data.isRangeValuesMatching(state) && data.findMostCompressAry(ct)!=null) {
                if (ct==CompressType.FULL || ct==CompressType.HALF) memCache.put(stretchDataKey, null); // this the two types then this is the last time we need this data
                fromCache= " (from Cache)";
            }
            else {
                data= !mask ? DirectStretchUtils.getStretchData(state,frGroup,tileSize,ct) :
                              DirectStretchUtils.getStretchDataMask(state,frGroup,tileSize,maskBits);
                if (ct!= CompressType.FULL) memCache.put(stretchDataKey, data.copyParts(ct));
            }
            counters.incrementVis("Byte Data: " + StretchDataInfo.getMostCompressedDescription(ct));
            PlotServUtils.statsLog("byteAry",
                    "total-MB", (float)data.findMostCompressAry(ct).length / StringUtils.MEG,
                    "Type", (state.isThreeColor() ? "3 Color" : "Standard") +" - "+ ct + fromCache);
            CtxControl.refreshCache(state);
            return data.findMostCompressAry(ct);
        } catch (Exception e) {
            return new byte[] {};
        }
    }

    public static WebPlotResult crop(PlotState[] stateAry, ImagePt c1, ImagePt c2, boolean cropMultiAll) {
        List<WebPlotResult> resultsList= Arrays.stream(stateAry).map((s) -> crop(s, c1, c2, cropMultiAll)).toList();
        boolean success= resultsList.stream().filter(r -> !r.success()).toList().isEmpty();
        WebPlotResult result = WebPlotResult.make(WebPlotResult.RESULT_ARY, resultsList.toArray(new WebPlotResult[0]));
        return success ? result : resultsList.get(0);
    }


    public static WebPlotResult crop(PlotState state, ImagePt c1, ImagePt c2, boolean cropMultiAll) {
        try {

            Band[] bands = state.getBands();
            BandState[] bandStateAry = state.getBandStateAry();
            WebPlotRequest[] cropRequest = new WebPlotRequest[bands.length];

            for (int i = 0; (i < bands.length); i++) {

                File workingFitsFile = PlotStateUtil.getWorkingFitsFile(state, bands[i]);
                String fName = workingFitsFile.getName();
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


                if (saveCropFits) FitsReadUtil.writeFitsFile(cropFile,fr,cropFits);



                String fReq = ServerContext.replaceWithPrefix(cropFile);
                cropRequest[i] = WebPlotRequest.makeFilePlotRequest(fReq, 1);
                cropRequest[i].setTitle(state.isThreeColor() ?
                        "Cropped Plot (" + bands[i].toString() + ")" :
                        "Cropped Plot");
                cropRequest[i].setThumbnailSize(state.getPrimaryRequest().getThumbnailSize());
                PlotStateUtil.initRequestFromState(cropRequest[i], state, bands[i]);
            }


            WebPlotFactoryRet wpRet = (state.isThreeColor() && cropRequest.length == 3) ?
                    WebPlotFactory.createNew(cropRequest[0], cropRequest[1], cropRequest[2]) :
                    WebPlotFactory.createNew(cropRequest[0]);

            int imageIdx = 0;
            for (WebPlotInitializer wpInit : wpRet.wpInit()) {
                PlotState cropState = wpInit.plotState();
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


            WebPlotResult cropResult = makeNewPlotResult(wpRet.wpInit(), wpRet.wpHeader(), null);

            counters.incrementVis("Crop");
            PlotServUtils.statsLog("crop");


            return cropResult;
        } catch (Exception e) {
            return createError("on crop", state, e);
        }
    }

    public static WebPlotResult getAreaStatistics(PlotState state, ImagePt pt1, ImagePt pt2, ImagePt pt3, ImagePt pt4, String areaShape, double rotateAngle) {
        try {
            counters.incrementVis("Area Stat");
            HashMap<Band, HashMap<AreaStatisticsUtil.Metrics, AreaStatisticsUtil.Metric>> metricsMap = new HashMap<>();
            ActiveFitsReadGroup frGroup= CtxControl.prepare(state);
            for (Band b: state.getBands()) {
                metricsMap.put(b,
                        AreaStatisticsUtil.getAreaStatistics(frGroup, areaShape,rotateAngle,b,pt1,pt2,pt3,pt4));
            }
            return WebPlotResult.make(WebPlotResult.BAND_INFO, new BandInfo(metricsMap));
        } catch (Exception e) {
            return createError("on getStats", state, e);
        }
    }

    public static WebPlotResult getColorHistogram(PlotState state, Band band) {
        try {
            counters.incrementVis("getColorHistogram");
            FitsRead fr= CtxControl.prepare(state).getFitsRead(band);
            Histogram hist = fr.getHistogram();
            return WebPlotResult.make(
                    WebPlotResult.DATA_HISTOGRAM, hist.getHistogramArray(),
                    WebPlotResult.DATA_BIN_MEAN_ARRAY, hist.getMeanBinDataAry(fr.getBscale(),fr.getBzero()),
                    WebPlotResult.DATA_BIN_COLOR_IDX, fr.getHistColors(hist, state.getRangeValues(band)) );
        } catch (Throwable e) {
            return createError("on getColorHistogram", state, e);
        }
    }


    public static WebPlotResult saveDS9RegionFile(String regionData) {
        try {
            counters.incrementVis("Region save");
            File f = File.createTempFile("regionDownload-", ".reg", ServerContext.getVisSessionDir());
            List<String> regOutList = StringUtils.parseStringList(regionData);
            new RegionParser().saveFile(f, regOutList, "Region file generated by Firefly");
            return WebPlotResult.make(WebPlotResult.REGION_FILE_NAME, ServerContext.replaceWithPrefix(f));
        } catch (Exception e) {
            return createError("on getImagePng", e);
        }
    }

    private static File getRegFile(String fileKey) {
        File regFile = ServerContext.convertToFile(fileKey);
        if (regFile != null && regFile.canRead()) return regFile;
        UploadFileInfo tmp = (UploadFileInfo) (UserCache.getInstance().get(new StringKey(fileKey)));
        return tmp !=null ? tmp.getFile() : new File("");
    }

    private static String getRegTitle(String fileKey) {
        File f= getRegFile(fileKey);
        UploadFileInfo fi = (UploadFileInfo) UserCache.getInstance().get(new StringKey(fileKey));
        return (fi!=null) ? fi.getFileName() : fileKey.startsWith("UPLOAD") ? "Region file" : f.getName();
    }

    public static WebPlotResult getDS9Region(String fileKey) {
        try {
            counters.incrementVis("Region read");
            PlotServUtils.statsLog("ds9Region", fileKey);
            var r = new RegionParser().processFile(getRegFile(fileKey));
            var rAsStrList = r.regionList().stream().map(Region::serialize).toList();
            return WebPlotResult.make(
                    WebPlotResult.TITLE, getRegTitle(fileKey),
                    WebPlotResult.REGION_DATA, StringUtils.combineStringList(rAsStrList),
                    WebPlotResult.REGION_ERRORS, StringUtils.combineStringList(r.msgList()) );
        } catch (Exception e) {
            return createError("on getDSRegion", e);
        }
    }

    public static WebPlotResult getRelocatableRegions(String fileKey) {
        File fpFile = getRegFile(fileKey);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fpFile)))) {
            List<String> rAsStrList =  new ArrayList<>();
            List<String> msgList =  new ArrayList<>();
            String tmpLine;

            while ((tmpLine = br.readLine()) != null) {
                tmpLine = tmpLine.trim();
                if (!tmpLine.startsWith("#")) rAsStrList.add(tmpLine);
            }
            if (rAsStrList.size() == 0) msgList.add("no region is defined in the footprint file");

            return WebPlotResult.make(
                    WebPlotResult.REGION_DATA, StringUtils.combineStringList(rAsStrList),
                    WebPlotResult.REGION_ERRORS, StringUtils.combineStringList(msgList));
        } catch (Exception e) {
            return createError("on getRelocatableRegion", e);
        }
    }

    public static WebPlotResult getFootprintRegion(String fpInfo) {
        List<String> rAsStrList= new ArrayList<>();
        List<String> msgList= new ArrayList<>();
        String fileName= VisContext.getFootprint(fpInfo);

        if (fileName != null) {
            int idx = fpInfo.indexOf('_');
            String tag = idx >= 0 ? fpInfo.substring(idx + 1) : fpInfo;
            try (InputStream in = VisServerOps.class.getClassLoader().getResourceAsStream(fileName)) {
                if (in==null) throw new IOException("InputStream is null");
                try ( BufferedReader br = new BufferedReader( new InputStreamReader(in)) ) {
                    String tmpLine;
                    while ((tmpLine = br.readLine()) != null) {
                        tmpLine = tmpLine.trim();
                        if (!tmpLine.startsWith("#") && ((tmpLine.contains("tag={" + tag)) || (!tmpLine.contains("tag"))))
                            rAsStrList.add(tmpLine);
                    }
                    if (rAsStrList.size() == 0) msgList.add("no region is defined in the footprint file");
                } catch (Exception e) {
                    return createError("on getFootprintRegion", e);
                }
            } catch (NullPointerException | IOException e) {
                return createError("Could not find footprint filename: "+fileName, e);
            }
        } else {
            msgList.add("no footprint description file is found");
        }
        return WebPlotResult.make(
                WebPlotResult.REGION_DATA, StringUtils.combineStringList(rAsStrList),
                WebPlotResult.REGION_ERRORS, StringUtils.combineStringList(msgList) );
    }


    private static WebPlotResult createError(String logMsg, Exception e) { return createError(logMsg, null, null, e); }

    private static WebPlotResult createError(String logMsg, PlotState state, Throwable e) {
        return createError(logMsg, state, null, e);
    }

    private static WebPlotResult createError(String logMsg, PlotState state, WebPlotRequest[] reqAry, Throwable e) {
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

        if (e instanceof FailedRequestException fe) {
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
            } catch (Exception ignore) {} // if anything goes wrong here we have to recover, this is only for logging
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
        WebPlotResult retval = new WebPlotResult(requestKey);
        retval.putResult(WebPlotResult.PLOT_CREATE, new CreatorResults(wpHeader, wpInit));
        return retval;
    }
}
