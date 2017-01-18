/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.FileRetriever;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.ImageFileRetrieverFactory;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
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

import static edu.caltech.ipac.firefly.visualize.Band.*;

/**
 * @author Trey Roby
 */
public class ImagePlotBuilder {

    private static final Logger.LoggerImpl _log = Logger.getLogger();


//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public static SimpleResults create(WebPlotRequest wpr) throws FailedRequestException, GeomException {
        List<SimpleResults> retList= createList(wpr, PlotState.MultiImageAction.USE_FIRST);
        return (retList!=null && retList.size()>0) ? retList.get(0) : null;
    }

    public static SimpleResults create3Color(WebPlotRequest redRequest,
                                         WebPlotRequest greenRequest,
                                         WebPlotRequest blueRequest) throws FailedRequestException, GeomException {
        SimpleResults retval= null;
        LinkedHashMap<Band, WebPlotRequest> requestMap = new LinkedHashMap<>(5);

        if (redRequest != null) requestMap.put(RED, redRequest);
        if (greenRequest != null) requestMap.put(GREEN, greenRequest);
        if (blueRequest != null) requestMap.put(BLUE, blueRequest);

        try {
            Results allPlots= build(requestMap, PlotState.MultiImageAction.USE_FIRST,
                                                     null, true);
            ImagePlotInfo piAry[]= allPlots.getPlotInfoAry();
            if (piAry!=null && piAry.length>0)  retval= new SimpleResults(piAry[0].getPlot(), piAry[0].getFrGroup());
        } catch (FailedRequestException e) {
            throw new FailedRequestException("Could not create plot. " + e.getMessage(), e.getDetailMessage());
        } catch (FitsException e) {
            throw new FailedRequestException("Could not create plot. Invalid FITS File format.", e.getMessage());
        } catch (Exception e) {
            throw new FailedRequestException("Could not create plot.", e.getMessage(), e);
        }
        return retval;

    }


    private static List<SimpleResults> createList(WebPlotRequest wpr, PlotState.MultiImageAction multiAction)
            throws FailedRequestException, GeomException {
        wpr.setProgressKey(null); // this just makes sure in update progress caching does not happen
        List<SimpleResults> retList= new ArrayList<>(10);

        try {
            Map<Band, WebPlotRequest> requestMap = new LinkedHashMap<>(2);
            requestMap.put(NO_BAND, wpr);
            Results allPlots= build(requestMap, multiAction, null, false);
            for(ImagePlotInfo pi : allPlots.getPlotInfoAry())  retList.add(new SimpleResults(pi.getPlot(),pi.getFrGroup()));
        } catch (FailedRequestException e) {
            throw new FailedRequestException("Could not create plot. " + e.getMessage(), e.getDetailMessage());
        } catch (FitsException e) {
            throw new FailedRequestException("Could not create plot. Invalid FITS File format.", e.getMessage());
        } catch (Exception e) {
            throw new FailedRequestException("Could not create plot.", e.getMessage(), e);
        }
        return retList;
    }

    static Results build(Map<Band, WebPlotRequest> requestMap,
                         PlotState.MultiImageAction multiAction,
                         PlotState state,
                         boolean threeColor) throws Exception {

        ImagePlotInfo pInfo[];
        WebPlotRequest firstR = requestMap.values().iterator().next();
        // ------------ find the files to read
        long findStart = System.currentTimeMillis();
        Map<Band, FileInfo> fileDataMap = findFiles(requestMap);
        long findElapse = System.currentTimeMillis() - findStart;

        // ------------ read the FITS files
        long readStart = System.currentTimeMillis();
        Map<Band, FileReadInfo[]> readInfoMap = WebPlotReader.readFiles(fileDataMap, firstR);
        PlotServUtils.updatePlotCreateProgress( firstR, ProgressStat.PType.CREATING, PlotServUtils.CREATING_MSG);
        purgeFailedBands(readInfoMap, requestMap);
        long readElapse = System.currentTimeMillis() - readStart;

        // ------------ make the ImagePlot(s)
        ZoomChoice zoomChoice = makeZoomChoice(requestMap, readInfoMap);
        if (state == null) {
            pInfo = makeNewPlots(readInfoMap, requestMap, zoomChoice, multiAction, threeColor);
        } else {
            pInfo = new ImagePlotInfo[1];
            pInfo[0] = recreatePlot(state, readInfoMap, zoomChoice);
        }

        return new Results(pInfo,zoomChoice, findElapse,readElapse);
    }

    static Results buildFromFile(WebPlotRequest request,
                                 FileInfo fileData,
                                 FitsRead fitsRead,
                                 int imageIdx,
                                 PlotState state) throws Exception {


        ImagePlotInfo pInfo[];
        // ------------ read the FITS files
        long readStart = System.currentTimeMillis();
        PlotServUtils.updatePlotCreateProgress(request, ProgressStat.PType.CREATING, PlotServUtils.CREATING_MSG);
        long readElapse = System.currentTimeMillis() - readStart;


        Map<Band, FileReadInfo[]> readInfoMap = WebPlotReader.processFitsRead(fileData,request,fitsRead,imageIdx);

        Map<Band,WebPlotRequest> requestMap= new HashMap<>(1);
        requestMap.put(Band.NO_BAND,request);

        // ------------ make the ImagePlot(s)
        ZoomChoice zoomChoice = makeZoomChoice(requestMap, readInfoMap);
        if (state == null) {
            pInfo = makeNewPlots(readInfoMap, requestMap, zoomChoice, PlotState.MultiImageAction.USE_FIRST, false);
        } else {
            pInfo = new ImagePlotInfo[1];
            pInfo[0] = recreatePlot(state, readInfoMap, zoomChoice);
        }

        return new Results(pInfo,zoomChoice, 0,readElapse);
    }





//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    static private ImagePlotInfo recreatePlot(PlotState state,
                                              Map<Band, FileReadInfo[]> readInfoMap,
                                              ZoomChoice zoomChoice) throws FailedRequestException,
                                                                            IOException,
                                                                            FitsException,
                                                                            GeomException {
        return ImagePlotCreator.makeOneImagePerBand(state, readInfoMap, zoomChoice);
    }



    private static Map<Band, FileInfo> findFiles(Map<Band, WebPlotRequest> requestMap) throws Exception {

        Map<Band, FileInfo> fitsFiles = new LinkedHashMap<>();

        PlotServUtils.updatePlotCreateProgress( firstRequest(requestMap), ProgressStat.PType.READING,
                                                PlotServUtils.STARTING_READ_MSG);

        for (Map.Entry<Band, WebPlotRequest> entry : requestMap.entrySet()) {
            Band band = entry.getKey();
            WebPlotRequest request = entry.getValue();
            FileRetriever retrieve = ImageFileRetrieverFactory.getRetriever(request);
            if (retrieve != null) {
                FileInfo fileData;
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
        PlotServUtils.updatePlotCreateProgress(firstRequest(requestMap), ProgressStat.PType.READING,
                                               PlotServUtils.ENDING_READ_MSG);

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
    private static ImagePlotInfo[] makeNewPlots(Map<Band, FileReadInfo[]> readInfoMap,
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
                plotInfo = makeNewPlots(readInfoMap, requestMap, zoomChoice,
                                        getActionGuess(threeColor), threeColor);
                break;
            case USE_FIRST:
                if (threeColor) state = make3ColorState(requestMap, readInfoMap, multiAction);
                else            state = makeState(requestMap.get(NO_BAND), readInfoMap.get(NO_BAND)[0], multiAction);
                for (Band band : requestMap.keySet()) {
                    state.setOriginalImageIdx(0, band);
                    state.setImageIdx(0, band);
                }
                plotInfo[0] = ImagePlotCreator.makeOneImagePerBand(state, readInfoMap, zoomChoice);
                break;
            case USE_IDX:
                WebPlotRequest r= requestMap.get(NO_BAND);
                int idx= r.getMultiImageIdx();
                state = makeState(requestMap.get(NO_BAND), readInfoMap.get(NO_BAND)[0], multiAction);
                state.setOriginalImageIdx(idx, NO_BAND);
                state.setImageIdx(idx, NO_BAND);
                state.setMultiImageFile(true,Band.NO_BAND);
                //todo: here
                plotInfo[0] = ImagePlotCreator.makeOneImagePerBand(state, readInfoMap, zoomChoice);
                break;
            case USE_ALL:
                if (!readInfoMap.containsKey(NO_BAND) || threeColor) {
                    throw new FailedRequestException("Cannot create plot",
                                                     "Cannot yet use the MultiImageAction.USE_ALL action with three color");
                }
                PlotState stateAry[] = makeNoBandMultiImagePlotState(requestMap.get(NO_BAND), readInfoMap.get(NO_BAND));
                plotInfo = ImagePlotCreator.makeAllNoBand(stateAry, readInfoMap.get(NO_BAND), zoomChoice);
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
                plotInfo[0] = ImagePlotCreator.makeOneImagePerBand(state, readInfoMap, zoomChoice);
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
        RangeValues rv = state.getRangeValues();
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
                state.setContextString(CtxControl.makeCachedCtx());
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
            if (!ServerContext.isFileInPath(original)) {
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
            List<Band> pList = new ArrayList<>(3);
            for (Band band : requestMap.keySet()) {
                if (!readInfoMap.containsKey(band)) pList.add(band);
            }
            for (Band band : pList) requestMap.remove(band);
        }
    }




    private static WebPlotRequest firstRequest(Map<Band, WebPlotRequest> requestMap) {
        return requestMap.values().iterator().next();
    }

    private static ZoomChoice makeZoomChoice(Map<Band, WebPlotRequest> requestMap,
                                             Map<Band, FileReadInfo[]> readInfoMap) {
        Band band = readInfoMap.entrySet().iterator().next().getKey();
        WebPlotRequest request = requestMap.get(band);

        return new ZoomChoice(request.hasMaxZoomLevel(),
                              request.getZoomType(),
                              request.getInitialZoomLevel(),
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

        ImagePlotInfo[] getPlotInfoAry() { return plotInfoAry; }
        long getFindElapse() { return findElapse; }
        long getReadElapse() { return readElapse; }
        ZoomChoice getZoomChoice() { return zoomChoice; }
    }


    public static class SimpleResults {
        private final ImagePlot plot;
        private final ActiveFitsReadGroup frGroup;

        SimpleResults(ImagePlot plot, ActiveFitsReadGroup frGroup) {
            this.plot = plot;
            this.frGroup = frGroup;
        }

        public ImagePlot getPlot() { return plot; }

        public ActiveFitsReadGroup getFrGroup() { return frGroup; }
    }
}

