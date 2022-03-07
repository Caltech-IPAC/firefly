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
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.BandState;
import edu.caltech.ipac.firefly.visualize.ClientFitsHeader;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.FileAndHeaderInfo;
import edu.caltech.ipac.firefly.visualize.PlotState;
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
import edu.caltech.ipac.util.cache.CacheKey;
import edu.caltech.ipac.util.cache.CacheManager;
import edu.caltech.ipac.util.cache.StringKey;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.draw.AreaStatisticsUtil;
import edu.caltech.ipac.visualize.draw.Metric;
import edu.caltech.ipac.visualize.draw.Metrics;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.CropFile;
import edu.caltech.ipac.visualize.plot.Histogram;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.PixelValue;
import edu.caltech.ipac.visualize.plot.PixelValueException;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.FitsReadUtil;
import nom.tam.fits.Fits;

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
            WebPlotRequest req= wpRet.getWpInit()[0].plotState().getPrimaryRequest();
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
            CtxControl.deletePlotCtx(CtxControl.getPlotCtx(null));
            counters.incrementVis("New Plots");
            return makeNewPlotResult(wpRet.getWpInit(), wpRet.getWpHeader(), request.getProgressKey());
        } catch (Exception e) {
            return createError("on createPlot", null, new WebPlotRequest[]{request}, e);
        }
    }

    static final boolean USE_DIRECT_FLUX_IF_POSSIBLE = true;

    private static boolean isDirectFluxAccessAvailable(PlotState state) {
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

    public static double[] getZAxisAry(PlotState state, ImagePt pt, int hduNum, int ptSize) {
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
        if (!CtxControl.isCtxAvailable(state.getContextString())) {  // work directly on the file
            FileAndHeaderInfo fap = state.getFileAndHeaderInfo(band);
            File f = ServerContext.convertToFile(fap.getfileName());
            retval = getFluxFromFitsFile(f, fap.getHeader(), ipt);
        } else {
            try {  // use the in memory plot object
                ActiveCallCtx ctx = CtxControl.prepare(state);
                retval= ctx.getFitsReadGroup().getFitsRead(band).getFlux(ipt);
            } catch (FailedRequestException e) {
                throw new IOException(e);
            } catch (PixelValueException e) {
                retval = Double.NaN;
            }
        }
        return retval;
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
                    "Type", state.isThreeColor() ? "3 Color: "+band : "Standard",
                    "Time", UTCTimeUtil.getHMSFromMills(elapse));
            return flip1d;
        } catch (Exception e) {
            return new float[] {};
        }
    }



    public static byte[] getByteStretchArray(PlotState state, int tileSize, boolean mask, long maskBits, CompressType ct) {
        DirectStretchUtils.StretchDataInfo data;
        try {
            long start = System.currentTimeMillis();
            ActiveCallCtx ctx = CtxControl.prepare(state);
            ActiveFitsReadGroup frGroup= ctx.getFitsReadGroup();
            Cache memCache= CacheManager.getCache(Cache.TYPE_VIS_SHARED_MEM);
            CacheKey stretchDataKey= new StringKey(ctx.getKey()+"byte-data");
            data= (DirectStretchUtils.StretchDataInfo)memCache.get(stretchDataKey);
            String fromCache= "";
            if (data!=null && data.isRangeValuesMatching(state) && data.findMostCompressAry(ct)!=null) {
                if (ct==CompressType.FULL || ct==CompressType.HALF) memCache.put(stretchDataKey, null); // this the two types then this is the last time we need this data
                fromCache= " (from Cache)";
            }
            else {
                data= DirectStretchUtils.getStretchData(state,frGroup,tileSize,mask, maskBits,ct);
                if (ct!= CompressType.FULL) memCache.put(stretchDataKey, data.copyParts(ct));
            }
            PlotServUtils.statsLog("byteAry",
                    "total-MB", (float)data.findMostCompressAry(ct).length / StringUtils.MEG,
                    "Type", (state.isThreeColor() ? "3 Color" : "Standard") +" - "+ ct + fromCache,
                    "Time", UTCTimeUtil.getHMSFromMills(System.currentTimeMillis() - start));
            return data.findMostCompressAry(ct);
        } catch (Exception e) {
            return new byte[] {};
        }
    }



    public static WebPlotResult crop(PlotState[] stateAry, ImagePt c1, ImagePt c2, boolean cropMultiAll) {
        WebPlotResult[] resultAry = new WebPlotResult[stateAry.length];
        boolean success = true;
        for (int i = 0; (i < stateAry.length); i++) {
            resultAry[i] = crop(stateAry[i], c1, c2, cropMultiAll);
            if (success) success = resultAry[i].success();
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


            WebPlotResult cropResult = makeNewPlotResult(wpRet.getWpInit(), wpRet.getWpHeader(), null);

            counters.incrementVis("Crop");
            PlotServUtils.statsLog("crop");


            return cropResult;
        } catch (Exception e) {
            return createError("on crop", state, e);
        }
    }

    public static WebPlotResult getAreaStatistics(PlotState state, ImagePt pt1, ImagePt pt2, ImagePt pt3, ImagePt pt4, String areaShape, double rotateAngle) {
        try {
            HashMap<Band, HashMap<Metrics, Metric>> metricsMap = new HashMap<>();
            ActiveCallCtx ctx = CtxControl.prepare(state);
            ActiveFitsReadGroup frGroup= ctx.getFitsReadGroup();

            for (Band band : state.getBands()) {
                HashMap<Metrics, Metric> metrics =
                        AreaStatisticsUtil.getAreaStatistics(frGroup, areaShape,rotateAngle,band,pt1,pt2,pt3,pt4);
                metricsMap.put(band, metrics);
            }

            WebPlotResult retValue = new WebPlotResult();
            retValue.putResult(WebPlotResult.BAND_INFO, new BandInfo(metricsMap));

            counters.incrementVis("Area Stat");
            return retValue;
        } catch (Exception e) {
            return createError("on getStats", state, e);
        }
    }

    public static WebPlotResult getColorHistogram(PlotState state, Band band) {
        try {
            ActiveCallCtx ctx = CtxControl.prepare(state);
            FitsRead fr= ctx.getFitsReadGroup().getFitsRead(band);
            Histogram hist = fr.getHistogram();
            int[] dHist = hist.getHistogramArray();
            byte[] dHistColors = fr.getHistColors(hist, state.getRangeValues(band));
            double [] meanDataAry = new double[dHist.length];
            for (int i = 0; i < meanDataAry.length; i++) meanDataAry[i] = hist.getDNfromBin(i) * fr.getBscale() + fr.getBzero();

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
        PlotState state = wpInit[0].plotState();
        WebPlotResult retval = new WebPlotResult((requestKey!=null) ? requestKey : state.getContextString());
        retval.putResult(WebPlotResult.PLOT_CREATE, new CreatorResults(wpHeader, wpInit));
        return retval;
    }
}
