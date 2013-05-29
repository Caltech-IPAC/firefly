package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 2/15/11
 * Time: 1:53 PM
 */


import edu.caltech.ipac.client.net.FailedRequestException;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.InsertBandInitializer;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.ImageDataGroup;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.PlotView;
import edu.caltech.ipac.visualize.plot.RangeValues;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.visualize.Band.BLUE;
import static edu.caltech.ipac.firefly.visualize.Band.GREEN;
import static edu.caltech.ipac.firefly.visualize.Band.NO_BAND;
import static edu.caltech.ipac.firefly.visualize.Band.RED;

/**
 * @author Trey Roby
 */
public class WebPlotFactory {

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    static {
        VisContext.init();
    }

    public static WebPlotInitializer[] createNew(String workingCtxStr,
                                                 WebPlotRequest redRequest,
                                                 WebPlotRequest greenRequest,
                                                 WebPlotRequest blueRequest) throws FailedRequestException, GeomException {

        LinkedHashMap<Band, WebPlotRequest> requestMap = new LinkedHashMap<Band, WebPlotRequest>(5);

        if (redRequest != null) requestMap.put(RED, redRequest);
        if (greenRequest != null) requestMap.put(GREEN, greenRequest);
        if (blueRequest != null) requestMap.put(BLUE, blueRequest);

        return create(workingCtxStr, requestMap, PlotState.MultiImageAction.USE_FIRST, null, true);
    }

    public static WebPlotInitializer[] createNew(String workingCtxStr, WebPlotRequest request) throws FailedRequestException, GeomException {
        Map<Band, WebPlotRequest> requestMap = new LinkedHashMap<Band, WebPlotRequest>(2);
        requestMap.put(NO_BAND, request);
        return create(workingCtxStr, requestMap, PlotState.MultiImageAction.USE_ALL, null, false);
    }

    public static WebPlotInitializer[] recreate(PlotState state) throws FailedRequestException, GeomException {
        Map<Band, WebPlotRequest> requestMap = new LinkedHashMap<Band, WebPlotRequest>(5);
        for (Band band : state.getBands()) requestMap.put(band, state.getWebPlotRequest(band));
        VisContext.purgeOtherPlots(state);
        for(WebPlotRequest req : requestMap.values()) {
            req.setZoomType(ZoomType.STANDARD);
            req.setInitialZoomLevel(state.getZoomLevel());
        }
        WebPlotInitializer wpAry[] = create(null, requestMap, null, state, state.isThreeColor());
        Assert.argTst(wpAry.length == 1, "in this case you should never have more than one result");
        return wpAry;
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static InsertBandInitializer insertBand(ImagePlot plot,
                                                    PlotState state,
                                                    FileData fd,
                                                    Band band) throws FailedRequestException, GeomException {

        InsertBandInitializer retval;

        try {
            VisContext.purgeOtherPlots(state);
            if (VisContext.getPlotCtx(state.getContextString()) == null) {
                throw new FailedRequestException("PlotClientCtx not found, ctxStr=" +
                                                         state.getContextString());
            }

            FileReadInfo frInfo[] = WebPlotReader.readOneFits(null, fd, band, null);

            ModFileWriter modWriter = ImagePlotCreator.createBand(state, plot, frInfo[0]);

            WebFitsData wfData = ImagePlotCreator.makeWebFitsData(plot, band, frInfo[0].getOriginalFile());
            PlotServUtils.setPixelAccessInfo(plot, state);

            initState(state, frInfo[0], band, null);


            PlotImages images = createImages(state, plot, true, false);

            if (modWriter != null) {
                if (modWriter.getCreatesOnlyOneImage()) state.setImageIdx(0, band);
                modWriter.go(state);
            }

            PlotServUtils.createThumbnail(plot, images, true,state.getThumbnailSize());

            retval = new InsertBandInitializer(state, images, band, wfData, frInfo[0].getDataDesc());

        } catch (FitsException e) {
            PlotServUtils.statsLog("Fits Read Failed", e.getMessage());
            throw new FailedRequestException("Fits read failed: " + fd.getFile(), null, e);
        } catch (IOException e) {
            PlotServUtils.statsLog("Fits Read Failed", e.getMessage());
            throw new FailedRequestException("Fits read failed: " + fd.getFile(), null, e);
        } catch (OutOfMemoryError e) {
            PlotServUtils.statsLog("Fits Read Failed", e.getMessage());
            System.gc();
            throw new FailedRequestException("Out of memory: " + fd.getFile(), e.toString(), e);
        }
        return retval;
    }



    private static WebPlotInitializer[] create(String workingCtxStr,
                                               Map<Band, WebPlotRequest> requestMap,
                                               PlotState.MultiImageAction multiAction,
                                               PlotState state,
                                               boolean threeColor) throws FailedRequestException, GeomException {


        long start = System.currentTimeMillis();
        WebPlotInitializer retval[];
        if (requestMap.size() == 0) {
            throw new FailedRequestException("Could not create plot", "All WebPlotRequest are null");
        }
        WebPlotRequest firstR = requestMap.values().iterator().next();
        ImagePlotInfo pInfo[];


        try {
            // ------------ find the files to read
            long findStart = System.currentTimeMillis();
            Map<Band, FileData> fileDataMap = findFiles(requestMap);
            long findElapse = System.currentTimeMillis() - findStart;
            VisContext.shouldContinue(workingCtxStr);

            // ------------ read the FITS files
            long readStart = System.currentTimeMillis();
            String addDateTitleStr = firstR.getPlotDescAppend();
            Map<Band, FileReadInfo[]> readInfoMap = WebPlotReader.readFiles(workingCtxStr, fileDataMap, firstR, addDateTitleStr);
            PlotServUtils.updateProgress(firstR.getProgressKey(), PlotServUtils.CREATING_MSG);
            purgeFailedBands(readInfoMap, requestMap);
            long readElapse = System.currentTimeMillis() - readStart;
            VisContext.shouldContinue(workingCtxStr);

            // ------------ make the ImagePlot(s)
            ZoomChoice zoomChoice = makeZoomChoice(requestMap, readInfoMap);
            if (state == null) {
                pInfo = makeNewPlots(workingCtxStr, readInfoMap, requestMap, zoomChoice, multiAction, threeColor);
                VisContext.shouldContinue(workingCtxStr);
            } else {
                pInfo = new ImagePlotInfo[1];
                pInfo[0] = recreatePlot(state, readInfoMap, zoomChoice);
            }


            // ------------ Iterate through results, Prepare the return objects, including PlotState if it is null
            retval = new WebPlotInitializer[pInfo.length];
            for (int i = 0; (i < pInfo.length); i++) {
                ImagePlotInfo pi = pInfo[i];
                if (pInfo.length>3) {
                    PlotServUtils.updateProgress(pi.getState().getPrimaryWebPlotRequest(),
                                                 PlotServUtils.PROCESSING_MSG+": "+ (i+1)+" of "+pInfo.length);
                }
                else {
                    PlotServUtils.updateProgress(pi.getState().getPrimaryWebPlotRequest(),
                                                 PlotServUtils.PROCESSING_MSG);
                }

                for (Map.Entry<Band, ModFileWriter> entry : pi.getFileWriterMap().entrySet()) {
                    ModFileWriter mfw = entry.getValue();
                    if (mfw != null) {
                        if (mfw.getCreatesOnlyOneImage()) pi.getState().setImageIdx(0, entry.getKey());
                        mfw.go(pi.getState());
                    }
                }

                retval[i] = makePlotResults(pi, (i<3||i>pInfo.length-3), zoomChoice);
            }

            long elapse = System.currentTimeMillis() - start;
            logSuccess(pInfo[0].getState(), elapse, findElapse, readElapse, false, null, true);
        } catch (FailedRequestException e) {
            throw new FailedRequestException("Could not create plot. " + e.getMessage(), e.getDetailMessage());
        } catch (FitsException e) {
            throw new FailedRequestException("Could not create plot. Invalid FITS File format.", e.getMessage());
        } catch (Exception e) {
            throw new FailedRequestException("Could not create plot.", e.getMessage(), e);
        }
        return retval;

    }

    private static Map<Band, FileData> findFiles(Map<Band, WebPlotRequest> requestMap) throws Exception {

        Map<Band, FileData> fitsFiles = new LinkedHashMap<Band, FileData>();

        PlotServUtils.updateProgress(firstRequest(requestMap), PlotServUtils.STARTING_READ_MSG);

        for (Map.Entry<Band, WebPlotRequest> entry : requestMap.entrySet()) {
            Band band = entry.getKey();
            WebPlotRequest request = entry.getValue();
            FileRetriever retrieve = FileRetrieverFactory.getRetriever(request);
            if (retrieve != null) {
                FileData fileData;
                try {
                    fileData = retrieve.getFile(request);
                    fitsFiles.put(band, fileData);
                } catch (Exception e) {
                    if (requestMap.size() > 1 && request.isContinueOnFail()) {
                        _log.error("Failed to find file for band: " + band.toString() +
                                           ", WebPlotRequest.isContinueOnFail() is true so attempting to continue");
                    } else {
                        throw e;
                    }
                }
            } else {
                _log.error("failed to find FileRetriever should only be FILE, URL, ALL_SKY, or SERVICE, for band " + band.toString());
            }
        }

        if (fitsFiles.size() > 0) {
            WebPlotReader.validateAccess(fitsFiles);
        } else {
            _log.error("could not find any fits files from request");
        }
        PlotServUtils.updateProgress(firstRequest(requestMap), PlotServUtils.ENDING_READ_MSG);

        return fitsFiles;
    }

    private static WebPlotRequest firstRequest(Map<Band, WebPlotRequest> requestMap) {
        WebPlotRequest r = requestMap.values().iterator().next();
        return r;
    }

    /**
     * @param plot    the image plot
     * @param state   current state
     * @param request request for the insert
     * @param band    which color band
     * @return the insertion initializer
     * @throws FailedRequestException
     * @throws GeomException
     * @throws SecurityException
     */
    public static InsertBandInitializer addBand(ImagePlot plot,
                                                PlotState state,
                                                WebPlotRequest request,
                                                Band band) throws FailedRequestException, GeomException, SecurityException {
        long start = System.currentTimeMillis();
        InsertBandInitializer retval = null;

        PlotClientCtx ctx = VisContext.getPlotCtx(state.getContextString());

        state.setWebPlotRequest(request, band);
        File file;
        String desc = null;
        FileData fd = null;

        long findStart = System.currentTimeMillis();
        FileRetriever retrieve = FileRetrieverFactory.getRetriever(request);
        if (retrieve != null) {
            fd = retrieve.getFile(request);
            file = fd.getFile();
        } else {
            file = null;
            _log.error("failed to find FileRetriever should only be FILE, URL, ALL_SKY, or SERVICE, for band " + band.toString());
        }
        long findElapse = System.currentTimeMillis() - findStart;
        VisContext.shouldContinue(ctx, null);


        if (file != null) {
            long insertStart = System.currentTimeMillis();
            retval = insertBand(plot, state, fd, band);
            long insertElapse = System.currentTimeMillis() - insertStart;
            long elapse = System.currentTimeMillis() - start;

            logSuccess(state, elapse, findElapse, insertElapse, true, band, false);
            state.setNewPlot(false);
        } else {
            _log.error("could not find any fits files from request");
        }

        return retval;
    }

    static ImagePlotInfo recreatePlot(PlotState state,
                                      Map<Band, FileReadInfo[]> readInfoMap,
                                      ZoomChoice zoomChoice) throws FailedRequestException,
                                                                    IOException,
                                                                    FitsException,
                                                                    GeomException {
        ImagePlotInfo retval = ImagePlotCreator.makeOneImagePerBand(null, state, readInfoMap, zoomChoice);
        return retval;
    }

    /**
     * This method will create the determine how many plots to make and create the plot state for each plot.  It is
     * only called when the plot is being created for the first time.  Any recreates do not go though this method.
     *
     * @param readInfoMap the map of band to all the fits images read and
     * @param requestMap  the map of band to all the plot request
     * @param zoomChoice  how do determine zoom
     * @param multiAction enum that gives direction on how to take the readInfoMap make plots out of them
     * @param threeColor  should be be making a three color plot
     * @return an array of ImagePlotInfo, one for each web plot that will be created on the client
     * @throws FailedRequestException configuration error
     * @throws IOException            error reading the file
     * @throws FitsException          error creating the fits data
     * @throws GeomException          on geom error
     */
    private static ImagePlotInfo[] makeNewPlots(String workingCtxStr,
                                                Map<Band, FileReadInfo[]> readInfoMap,
                                                Map<Band, WebPlotRequest> requestMap,
                                                ZoomChoice zoomChoice,
                                                PlotState.MultiImageAction multiAction,
                                                boolean threeColor) throws FailedRequestException,
                                                                           IOException,
                                                                           FitsException,
                                                                           GeomException {

        ImagePlotInfo plotInfo[] = new ImagePlotInfo[1];
        PlotState state;


        switch (multiAction) {
            case GUESS:
                plotInfo = makeNewPlots(workingCtxStr, readInfoMap, requestMap, zoomChoice, getActionGuess(threeColor), threeColor);
                break;
            case USE_FIRST:
                if (threeColor) state = make3ColorState(requestMap, readInfoMap, multiAction);
                else state = makeState(requestMap.get(NO_BAND), readInfoMap.get(NO_BAND)[0], multiAction);
                VisContext.purgeOtherPlots(state);
                for (Band band : requestMap.keySet()) {
                    state.setOriginalImageIdx(0, band);
                    state.setImageIdx(0, band);
                }
                plotInfo[0] = ImagePlotCreator.makeOneImagePerBand(workingCtxStr, state, readInfoMap, zoomChoice);
                break;
            case USE_ALL:
                if (!readInfoMap.containsKey(NO_BAND)) {
                    throw new FailedRequestException("Cannot create plot", "Cannot yet use the USE_ALL action with three color");
                }
                // if (threeColor ||readInfoMap.containsKey(NO_BAND)) { } // todo figure our if I need this case
                PlotState stateAry[] = makePlotStateArray(requestMap.get(NO_BAND), readInfoMap.get(NO_BAND), multiAction);
                for (int i = 0; (i < stateAry.length); i++) {
                    stateAry[i].setOriginalImageIdx(i, NO_BAND);
                    stateAry[i].setImageIdx(i, NO_BAND);
                }
                VisContext.purgeOtherPlots(stateAry[0]);
                plotInfo = ImagePlotCreator.makeAllNoBand(workingCtxStr, stateAry, readInfoMap.get(NO_BAND), zoomChoice);
                break;
            case MAKE_THREE_COLOR:
                if (threeColor && readInfoMap.containsKey(NO_BAND)) { // this handles the case of one file with multiple images becoming three color
                    // revamp plotDataMap, get the first the FileReadInfo objects, assign to bands, throw rest away
                    FileReadInfo[] readAry = readInfoMap.get(NO_BAND);
                    readInfoMap.clear();
                    int i = 0;
                    for (Band band : new Band[]{RED, GREEN, BLUE}) {
                        if (i >= readAry.length) break;
                        readInfoMap.put(band, new FileReadInfo[]{readAry[i]});
                        i++;
                    }
                }
                state = make3ColorState(requestMap, readInfoMap, multiAction);
                VisContext.purgeOtherPlots(state);
                plotInfo[0] = ImagePlotCreator.makeOneImagePerBand(workingCtxStr, state, readInfoMap, zoomChoice);
                break;
            default:
                throw new FailedRequestException("Plot creation failed", "unknown multiAction, don't know how to create plot");

        }

        return plotInfo;
    }

    private static PlotState.MultiImageAction getActionGuess(boolean threeColor) {
        return threeColor ?
               PlotState.MultiImageAction.USE_FIRST :
               PlotState.MultiImageAction.USE_ALL;
    }

    private static PlotState[] makePlotStateArray(WebPlotRequest request,
                                                  FileReadInfo info[],
                                                  PlotState.MultiImageAction multiAction) {
        PlotState stateAry[] = new PlotState[info.length];
        for (int i = 0; (i < stateAry.length); i++) {
            stateAry[i] = new PlotState(request);
            stateAry[i].setMultiImageAction(multiAction);
            initState(stateAry[i], info[i], NO_BAND, request);
        }
        return stateAry;
    }

    private static void purgeFailedBands(Map<Band, FileReadInfo[]> readInfoMap, Map<Band, WebPlotRequest> requestMap) {
        if (requestMap.size() > 1) {
            List<Band> pList = new ArrayList<Band>(3);
            for (Band band : requestMap.keySet()) {
                if (!readInfoMap.containsKey(band)) pList.add(band);
            }
            for (Band band : pList) requestMap.remove(band);
        }
    }

    private static PlotState make3ColorState(Map<Band, WebPlotRequest> requestMap,
                                             Map<Band, FileReadInfo[]> readInfoMap,
                                             PlotState.MultiImageAction multiAction) {
        PlotState state = new PlotState(requestMap.get(RED),
                                        requestMap.get(GREEN),
                                        requestMap.get(BLUE));
        state.setMultiImageAction(multiAction);
        for (Map.Entry<Band, FileReadInfo[]> entry : readInfoMap.entrySet()) {
            Band band = entry.getKey();
            FileReadInfo fi = entry.getValue()[0];
            initState(state, fi, band, requestMap.get(band));
        }
        RangeValues rv = state.getPrimaryRangeValues();
        if (rv != null) {
            for (Band band : state.getBands()) {
                state.setRangeValues(rv, band);
            }
        }
        return state;
    }

    private static PlotState makeState(WebPlotRequest request,
                                       FileReadInfo readInfo,
                                       PlotState.MultiImageAction multiAction) {
        PlotState state = new PlotState(request);
        state.setMultiImageAction(multiAction);
        initState(state, readInfo, NO_BAND, request);
        return state;
    }

    private static void initState(PlotState state,
                                  FileReadInfo fi,
                                  Band band,
                                  WebPlotRequest req) {
        if (state.isBandUsed(band)) {
            if (state.getContextString() == null) {
                String ctxStr = PlotServUtils.makePlotCtx();
                state.setContextString(ctxStr);
                _log.info("creating context for new plot: " + ctxStr);
            }
            state.setOriginalImageIdx(fi.getOriginalImageIdx(), band);
            VisContext.setOriginalFitsFile(state, fi.getOriginalFile(), band);
            VisContext.setWorkingFitsFile(state, fi.getWorkingFile(), band);
            state.setUploadFileName(fi.getUploadedName(),band);
            checkFileNames(state, fi.getOriginalFile(), band);
            state.setOriginalImageIdx(fi.getOriginalImageIdx(), band);
            state.setImageIdx(fi.getOriginalImageIdx(), band);
            if (WebPlotReader.isRotation(req)) {
                if (req.getRotateNorth()) {
                    state.setRotateType(PlotState.RotateType.NORTH);
                } else {
                    state.setRotateType(PlotState.RotateType.ANGLE);
                    state.setRotationAngle(req.getRotationAngle());
                }
            }
        }
    }

    private static void checkFileNames(PlotState state, File original, Band band) throws IllegalArgumentException {
        if (original != null && original.canRead() && state.getOriginalFitsFileStr(band) == null) {
            if (!VisContext.isFileInPath(original)) {
                String s = "Cannot read file - Configuration may not be setup correctly, file not in path: " +
                        original.getPath();
                _log.warn(s, "check property: " + VisContext.VIS_SEARCH_PATH);
                throw new IllegalArgumentException(s);
            }
        }
    }

    private static WebPlotInitializer makePlotResults(ImagePlotInfo pInfo,
                                                      boolean       makeFiles,
                                                      ZoomChoice zoomChoice) throws FitsException,
                                                                                                         IOException {
        PlotState state = pInfo.getState();

        PlotImages images = createImages(state, pInfo.getPlot(), makeFiles,
                                         zoomChoice.getZoomType() == ZoomType.FULL_SCREEN);

        WebPlotInitializer wpInit = makeWebPlotInitializer(state, images, pInfo);

        initPlotCtx(pInfo.getPlot(), state, images);

        return wpInit;
    }

    private static WebPlotInitializer makeWebPlotInitializer(PlotState state,
                                                             PlotImages images,
                                                             ImagePlotInfo pInfo) {
        // need a WebFits Data each band: normal is 1, 3 color is three
        WebFitsData wfDataAry[] = new WebFitsData[3];

        for (Map.Entry<Band, WebFitsData> entry : pInfo.getWebFitsDataMap().entrySet()) {
            wfDataAry[entry.getKey().getIdx()] = entry.getValue();
        }

        ImagePlot plot = pInfo.getPlot();
        ImageDataGroup imageData = plot.getImageData();
        return new WebPlotInitializer(state,
                                      images,
                                      plot.getCoordinatesOfPlot(),
                                      plot.getProjection(),
                                      imageData.getImageWidth(),
                                      imageData.getImageHeight(),
                                      plot.getFitsRead().getImageScaleFactor(),
                                      wfDataAry,
                                      plot.getPlotDesc(),
                                      pInfo.getDataDesc());
    }

    private static void initPlotCtx(ImagePlot plot,
                                    PlotState state,
                                    PlotImages images) throws FitsException,
                                                              IOException {
        PlotClientCtx ctx = VisContext.getPlotCtx(state.getContextString());
        ctx.setImages(images);
        ctx.setPlotState(state);
        ctx.setPlot(plot);
        ctx.extractColorInfo();
        ctx.addZoomLevel(plot.getPlotGroup().getZoomFact());
        PlotServUtils.setPixelAccessInfo(plot, state);
        VisContext.putPlotCtx(ctx);
        PlotServUtils.createThumbnail(plot, images, true, state.getThumbnailSize());
        state.setNewPlot(false);
    }

    private static PlotImages createImages(PlotState state,
                                           ImagePlot plot,
                                           boolean makeFiles,
                                           boolean fullScreen) throws IOException {
        // get, update and set the unique integer to build the file base name
        if (plot.getPlotView() == null) new PlotView().addPlot(plot);
//        Cache cache= UserCache.getInstance();
//        Integer cntObj= (Integer)cache.get(UNIQUE_CNT);
//        int uniqueCnt= (cntObj==null) ? 0 : cntObj;
//        String base= FileUtil.getBase(fitsFile) + "-"+imageIdx +"-" + (uniqueCnt++);
//        cache.put(UNIQUE_CNT,uniqueCnt);
        String base = PlotServUtils.makeTileBase(state);

        return PlotServUtils.writeImageTiles(VisContext.getVisSessionDir(),
                                             base, plot, fullScreen,
                                             makeFiles ? 2 : 0);

    }

    private static ZoomChoice makeZoomChoice(Map<Band, WebPlotRequest> requestMap, Map<Band, FileReadInfo[]> readInfoMap) {
        Band band = readInfoMap.entrySet().iterator().next().getKey();
        WebPlotRequest request = requestMap.get(band);
        FileReadInfo readInfo = readInfoMap.get(band)[0];
        boolean smartZoom;

        long length = readInfo.getOriginalFile() != null ? readInfo.getOriginalFile().length() : 0;
        if (length < 500 * FileUtil.K) {
            smartZoom = (request.isSmartZoom() || request.getZoomType() == ZoomType.SMART_FOR_SMALL_FILE);
        } else {
            smartZoom = request.isSmartZoom();
        }

        float zoomLevel = request.getInitialZoomLevel();

        return new ZoomChoice(smartZoom,
                              request.hasMaxZoomLevel(),
                              request.getZoomType(),
                              zoomLevel,
                              request.getZoomToWidth(),
                              request.getZoomToHeight(),
                              request.getZoomArcsecPerScreenPix());
    }

    private static void logSuccess(PlotState state,
                                   long elapse,
                                   long findElapse,
                                   long readElapse,
                                   boolean bandAdded,
                                   Band newBand,
                                   boolean newPlot) {
        String threeDesc = state.isThreeColor() ? "three color " : "";
        String majType = newPlot ? threeDesc + "create plot  " : threeDesc + "recreate plot";
        String minType = "file   ";
        String time3String = bandAdded ? ", Insert-" : ", Read-";
        long totSize = 0;

        List<String> out = new ArrayList<String>(8);
        String more = String.format("%s%9s%s%9s",
                                    ", Find-", UTCTimeUtil.getHMSFromMills(findElapse),
                                    time3String, UTCTimeUtil.getHMSFromMills(readElapse));
        out.add(majType + " - " + minType + ": Total: " + UTCTimeUtil.getHMSFromMills(elapse) + more);

        out.add("Context String: " + state.getContextString());
        if (bandAdded) {
            String bStr = newBand.toString() + " - ";
            File f = VisContext.getWorkingFitsFile(state, newBand);
            out.add("band: " + newBand + " added");
            if (!PlotServUtils.isBlank(state, newBand)) {
                out.add(bStr + "filename: " + f.getPath());
                out.add(bStr + "size:     " + FileUtil.getSizeAsString(f.length()));
                totSize = f.length();
            } else {
                out.add(bStr + "Blank Image");
            }
        } else {
            for (Band band : state.getBands()) {
                String bStr = state.isThreeColor() ? StringUtils.pad(5, band.toString()) + " - " : "";
                File f = VisContext.getWorkingFitsFile(state, band);
                if (!PlotServUtils.isBlank(state, band)) {
                    out.add(bStr + "filename: " + f.getPath());
                    out.add(bStr + "size:     " + FileUtil.getSizeAsString(f.length()));
                    totSize += f.length();
                } else {
                    out.add(bStr + "Blank Image");
                }
            }
        }
        out.add("PlotState Summary: " + state.toPrettyString());

        String statDetails = String.format("%6s%s", FileUtil.getSizeAsString(totSize), more);
        _log.info(out.toArray(new String[out.size()]));
        PlotServUtils.statsLog("create", "total-MB", (double) totSize / StringUtils.MEG, "Details", statDetails);
    }
}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
