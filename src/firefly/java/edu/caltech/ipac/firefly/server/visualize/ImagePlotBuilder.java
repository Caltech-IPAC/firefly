/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 10/17/13
 * Time: 1:01 PM
 */


import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.GeomException;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.RangeValues;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
public class ImagePlotBuilder {

    private static final Logger.LoggerImpl _log = Logger.getLogger();


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public static ImagePlot create(WebPlotRequest wpr) throws FailedRequestException, GeomException {
        List<ImagePlot> retList= createList(wpr, PlotState.MultiImageAction.USE_FIRST);
        return (retList!=null && retList.size()>0) ? retList.get(0) : null;
    }

    public static List<ImagePlot> createList(WebPlotRequest wpr)
            throws FailedRequestException, GeomException {
        return createList(wpr, PlotState.MultiImageAction.USE_ALL);
    }

    public static ImagePlot create3Color(WebPlotRequest redRequest,
                                         WebPlotRequest greenRequest,
                                         WebPlotRequest blueRequest) throws FailedRequestException, GeomException {
        ImagePlot retval= null;
        LinkedHashMap<Band, WebPlotRequest> requestMap = new LinkedHashMap<Band, WebPlotRequest>(5);

        if (redRequest != null) requestMap.put(RED, redRequest);
        if (greenRequest != null) requestMap.put(GREEN, greenRequest);
        if (blueRequest != null) requestMap.put(BLUE, blueRequest);

        try {
            Results allPlots= build(null, requestMap, PlotState.MultiImageAction.USE_FIRST,
                                                     null, true);

            ImagePlotInfo piAry[]= allPlots.getPlotInfoAry();
            if (piAry!=null && piAry.length>0)  retval= piAry[0].getPlot();
        } catch (FailedRequestException e) {
            throw new FailedRequestException("Could not create plot. " + e.getMessage(), e.getDetailMessage());
        } catch (FitsException e) {
            throw new FailedRequestException("Could not create plot. Invalid FITS File format.", e.getMessage());
        } catch (Exception e) {
            throw new FailedRequestException("Could not create plot.", e.getMessage(), e);
        }
        return retval;

    }




    private static List<ImagePlot> createList(WebPlotRequest wpr, PlotState.MultiImageAction multiAction)
            throws FailedRequestException, GeomException {
        wpr.setProgressKey(null); // this just makes sure in update progress caching does not happen
        List<ImagePlot> retList= new ArrayList<ImagePlot>(10);

        try {
            Map<Band, WebPlotRequest> requestMap = new LinkedHashMap<Band, WebPlotRequest>(2);
            requestMap.put(NO_BAND, wpr);
            Results allPlots= build(null, requestMap, multiAction, null, false);
            for(ImagePlotInfo pi : allPlots.getPlotInfoAry())  retList.add(pi.getPlot());
        } catch (FailedRequestException e) {
            throw new FailedRequestException("Could not create plot. " + e.getMessage(), e.getDetailMessage());
        } catch (FitsException e) {
            throw new FailedRequestException("Could not create plot. Invalid FITS File format.", e.getMessage());
        } catch (Exception e) {
            throw new FailedRequestException("Could not create plot.", e.getMessage(), e);
        }
        return retList;
    }

    static Results build(String workingCtxStr,
                         Map<Band, WebPlotRequest> requestMap,
                         PlotState.MultiImageAction multiAction,
                         PlotState state,
                         boolean threeColor) throws Exception {

        ImagePlotInfo pInfo[];
        WebPlotRequest firstR = requestMap.values().iterator().next();
        // ------------ find the files to read
        long findStart = System.currentTimeMillis();
        Map<Band, FileData> fileDataMap = findFiles(requestMap);
        long findElapse = System.currentTimeMillis() - findStart;
        VisContext.shouldContinue(workingCtxStr);

        // ------------ read the FITS files
        long readStart = System.currentTimeMillis();
        WebPlotReader wpr= new WebPlotReader(workingCtxStr);
        Map<Band, FileReadInfo[]> readInfoMap = wpr.readFiles(fileDataMap, firstR);
        PlotServUtils.updateProgress( firstR.getProgressKey(), ProgressStat.PType.CREATING,
                                      PlotServUtils.CREATING_MSG);
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

        return new Results(pInfo,zoomChoice, findElapse,readElapse);
    }

    static Results buildFromFile(WebPlotRequest request,
                                 FileData fileData,
                                 FitsRead fitsRead,
                                 int imageIdx,
                                 PlotState state) throws Exception {


        ImagePlotInfo pInfo[];
        // ------------ read the FITS files
        long readStart = System.currentTimeMillis();
        PlotServUtils.updateProgress(request.getProgressKey(), ProgressStat.PType.CREATING,
                                     PlotServUtils.CREATING_MSG);
        long readElapse = System.currentTimeMillis() - readStart;
//        VisContext.shouldContinue(workingCtxStr);


        WebPlotReader wpr= new WebPlotReader(null);
        Map<Band, FileReadInfo[]> readInfoMap = wpr.processFitsRead(fileData,request,fitsRead,imageIdx);

        Map<Band,WebPlotRequest> requestMap= new HashMap<Band,WebPlotRequest>(1);
        requestMap.put(Band.NO_BAND,request);

        // ------------ make the ImagePlot(s)
        ZoomChoice zoomChoice = makeZoomChoice(requestMap, readInfoMap);
        if (state == null) {
            pInfo = makeNewPlots(null, readInfoMap, requestMap, zoomChoice, PlotState.MultiImageAction.USE_FIRST, false);
//            VisContext.shouldContinue(workingCtxStr);
        } else {
            pInfo = new ImagePlotInfo[1];
            pInfo[0] = recreatePlot(state, readInfoMap, zoomChoice);
        }

        return new Results(pInfo,zoomChoice, 0,readElapse);
    }





//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    static ImagePlotInfo recreatePlot(PlotState state,
                                      Map<Band, FileReadInfo[]> readInfoMap,
                                      ZoomChoice zoomChoice) throws FailedRequestException,
                                                                    IOException,
                                                                    FitsException,
                                                                    GeomException {
        ImagePlotInfo retval = ImagePlotCreator.makeOneImagePerBand(null, state, readInfoMap, zoomChoice);
        return retval;
    }



    private static Map<Band, FileData> findFiles(Map<Band, WebPlotRequest> requestMap) throws Exception {

        Map<Band, FileData> fitsFiles = new LinkedHashMap<Band, FileData>();

        PlotServUtils.updateProgress( firstRequest(requestMap), ProgressStat.PType.READING,
                                      PlotServUtils.STARTING_READ_MSG);

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
        PlotServUtils.updateProgress(firstRequest(requestMap), ProgressStat.PType.READING, PlotServUtils.ENDING_READ_MSG);

        return fitsFiles;
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
                plotInfo = makeNewPlots(workingCtxStr, readInfoMap, requestMap, zoomChoice,
                                        getActionGuess(threeColor), threeColor);
                break;
            case USE_FIRST:
                if (threeColor) state = make3ColorState(requestMap, readInfoMap, multiAction);
                else            state = makeState(requestMap.get(NO_BAND), readInfoMap.get(NO_BAND)[0], multiAction);
                VisContext.purgeOtherPlots(state);
                for (Band band : requestMap.keySet()) {
                    state.setOriginalImageIdx(0, band);
                    state.setImageIdx(0, band);
                }
                plotInfo[0] = ImagePlotCreator.makeOneImagePerBand(workingCtxStr, state, readInfoMap, zoomChoice);
                break;
            case USE_ALL:
                if (!readInfoMap.containsKey(NO_BAND) || threeColor) {
                    throw new FailedRequestException("Cannot create plot",
                                                     "Cannot yet use the MultiImageAction.USE_ALL action with three color");
                }
                PlotState stateAry[] = makeNoBandMultiImagePlotState(requestMap.get(NO_BAND), readInfoMap.get(NO_BAND));
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

    private static PlotState makeState(WebPlotRequest request,
                                       FileReadInfo readInfo,
                                       PlotState.MultiImageAction multiAction) {
        PlotState state = PlotStateUtil.create(request);
        state.setMultiImageAction(multiAction);
        initState(state, readInfo, NO_BAND, request);
        return state;
    }



    private static PlotState make3ColorState(Map<Band, WebPlotRequest> requestMap,
                                             Map<Band, FileReadInfo[]> readInfoMap,
                                             PlotState.MultiImageAction multiAction) {

        PlotState state = PlotStateUtil.create(requestMap);
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

    private static PlotState[] makeNoBandMultiImagePlotState(WebPlotRequest request,
                                                             FileReadInfo info[]) {
        PlotState stateAry[] = new PlotState[info.length];
        for (int i = 0; (i < stateAry.length); i++) {
            stateAry[i] = PlotStateUtil.create(request);
            stateAry[i].setMultiImageAction(PlotState.MultiImageAction.USE_ALL);
            initState(stateAry[i], info[i], NO_BAND, request);
            stateAry[i].setOriginalImageIdx(i, NO_BAND);
            stateAry[i].setImageIdx(i, NO_BAND);
        }
        initMultiImageInfo(stateAry,info);
        return stateAry;
    }

    private static void initMultiImageInfo(PlotState stateAry[],  FileReadInfo infoAry[]) {
        if (stateAry.length!=infoAry.length && stateAry.length>1) return;
        boolean multiImageFile= stateAry.length>1;

        boolean cube= false;
        for(FileReadInfo info : infoAry) {
            cube=info.getFitsRead().getPlaneNumber()>1;
            if (cube) break;
        }

        int cubePlane= 1;
        int cubeCnt= 0;
        for (int i = 0; (i < stateAry.length); i++) {
            stateAry[i].setOriginalImageIdx(i, NO_BAND);
            stateAry[i].setImageIdx(i, NO_BAND);
            stateAry[i].setMultiImageFile(multiImageFile, NO_BAND);


            if (cube) {
                if (infoAry[i].getFitsRead().getPlaneNumber()==1) {
                    cubePlane=1;
                    cubeCnt++;
                }
                stateAry[i].setCubeCnt(cubeCnt,Band.NO_BAND);
                stateAry[i].setCubePlaneNumber(cubePlane,Band.NO_BAND);
                cubePlane++;
            }


        }
    }


    static void initState(PlotState state,
                          FileReadInfo fi,
                          Band band,
                          WebPlotRequest req) {
        if (state.isBandUsed(band)) {
            if (state.getContextString() == null) {
                PlotClientCtx ctx= new PlotClientCtx();
                CtxControl.putPlotCtx(ctx);
                String ctxStr = ctx.getKey();
                state.setContextString(ctxStr);
            }
            state.setOriginalImageIdx(fi.getOriginalImageIdx(), band);
            PlotStateUtil.setOriginalFitsFile(state, fi.getOriginalFile(), band);
            PlotStateUtil.setWorkingFitsFile(state, fi.getWorkingFile(), band);
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
                _log.warn(s, "check property: " + ServerContext.VIS_SEARCH_PATH);
                throw new IllegalArgumentException(s);
            }
        }
    }


    private static PlotState.MultiImageAction getActionGuess(boolean threeColor) {
        return threeColor ?
               PlotState.MultiImageAction.USE_FIRST :
               PlotState.MultiImageAction.USE_ALL;
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




    private static WebPlotRequest firstRequest(Map<Band, WebPlotRequest> requestMap) {
        WebPlotRequest r = requestMap.values().iterator().next();
        return r;
    }

    private static ZoomChoice makeZoomChoice(Map<Band, WebPlotRequest> requestMap,
                                             Map<Band, FileReadInfo[]> readInfoMap) {
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

//======================================================================
//------------------ Inner Classes --------------------------------------
//======================================================================

     static class Results {
        private final ImagePlotInfo plotInfoAry[];
        private final long findElapse;
        private final long readElapse;
        private final ZoomChoice zoomChoice;

        private Results(ImagePlotInfo[] plotInfoAry,
                        ZoomChoice zoomChoice,
                        long findElapse,
                        long readElapse) {
            this.plotInfoAry = plotInfoAry;
            this.findElapse = findElapse;
            this.readElapse = readElapse;
            this.zoomChoice = zoomChoice;
        }

        public ImagePlotInfo[] getPlotInfoAry() { return plotInfoAry; }
        public long getFindElapse() { return findElapse; }
        public long getReadElapse() { return readElapse; }
        public ZoomChoice getZoomChoice() { return zoomChoice; }
    }


}

