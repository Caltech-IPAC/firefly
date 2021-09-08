/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.FileRetriever;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.ImageFileRetrieverFactory;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;
import nom.tam.fits.FitsException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
        List<SimpleResults> retList= createList(wpr);
        return (retList.size()>0) ? retList.get(0) : null;
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
            ImagePlotInfo[] piAry= allPlots.getPlotInfoAry();
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


    private static List<SimpleResults> createList(WebPlotRequest wpr)
            throws FailedRequestException {
        wpr.setProgressKey(null); // this just makes sure in update progress caching does not happen
        List<SimpleResults> retList= new ArrayList<>(10);

        try {
            Map<Band, WebPlotRequest> requestMap = new LinkedHashMap<>(2);
            requestMap.put(NO_BAND, wpr);
            Results allPlots= build(requestMap, PlotState.MultiImageAction.USE_FIRST, null, false);
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

        ImagePlotInfo[] pInfo;
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

// Unused code commented out 3/5/2019 - keep around for awhile
//    static Results buildFromFile(WebPlotRequest request,
//                                 FileInfo fileData,
//                                 FitsRead fitsRead,
//                                 int imageIdx,
//                                 PlotState state) throws Exception {
//
//
//        ImagePlotInfo pInfo[];
//        // ------------ read the FITS files
//        long readStart = System.currentTimeMillis();
//        PlotServUtils.updatePlotCreateProgress(request, ProgressStat.PType.CREATING, PlotServUtils.CREATING_MSG);
//        long readElapse = System.currentTimeMillis() - readStart;
//
//
//        Map<Band, FileReadInfo[]> readInfoMap = WebPlotReader.processFitsRead(fileData,request,fitsRead,imageIdx);
//
//        Map<Band,WebPlotRequest> requestMap= new HashMap<>(1);
//        requestMap.put(Band.NO_BAND,request);
//
//        // ------------ make the ImagePlot(s)
//        ZoomChoice zoomChoice = makeZoomChoice(requestMap, readInfoMap);
//        if (state == null) {
//            pInfo = makeNewPlots(readInfoMap, requestMap, zoomChoice, PlotState.MultiImageAction.USE_FIRST, false);
//        } else {
//            pInfo = new ImagePlotInfo[1];
//            pInfo[0] = recreatePlot(state, readInfoMap, zoomChoice);
//        }
//
//        return new Results(pInfo,zoomChoice, 0,readElapse);
//    }





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

        ImagePlotInfo[] plotInfo = new ImagePlotInfo[1];
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
                state = makeState(requestMap.get(NO_BAND), readInfoMap.get(NO_BAND)[idx], multiAction);
                state.setOriginalImageIdx(idx, NO_BAND);
                state.setImageIdx(idx, NO_BAND);
                state.setMultiImageFile(true,Band.NO_BAND);
                plotInfo[0] = ImagePlotCreator.makeOneImagePerBand(state, readInfoMap, zoomChoice);
                break;
            case USE_EXTS:
                if (!readInfoMap.containsKey(NO_BAND) || threeColor) {
                    throw new FailedRequestException("Cannot create plot",
                            "Cannot yet use the MultiImageAction.USE_ALL action with three color");
                }
                WebPlotRequest rs = requestMap.get(NO_BAND);

                ArrayList<Integer> infoList = getFileReadList(rs.getMultiImageExts(), readInfoMap.get(NO_BAND));

                PlotState[] stateArys = makeNoBandMultiImagePlotStateOnList(rs, readInfoMap.get(NO_BAND),
                                                                        PlotState.MultiImageAction.USE_EXTS,
                                                                        infoList);
                plotInfo = new ImagePlotInfo[stateArys.length];
                for (int i = 0; i < stateArys.length; i++) {
                    plotInfo[i] = ImagePlotCreator.makeOneImagePerBand(stateArys[i], readInfoMap, zoomChoice);
                }

                break;
            case USE_ALL:
                if (!readInfoMap.containsKey(NO_BAND) || threeColor) {
                    throw new FailedRequestException("Cannot create plot",
                                                     "Cannot yet use the MultiImageAction.USE_ALL action with three color");
                }
                PlotState[] stateAry = makeNoBandMultiImagePlotState(requestMap.get(NO_BAND), readInfoMap.get(NO_BAND));
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

    private static ArrayList<Integer> getFileReadList(String extList, FileReadInfo[] frInfo) {
        List<String> idxs = new ArrayList<>(Arrays.asList(extList.split(",")));
        List<Integer> idxsInt = new ArrayList<>();

        int maxIdx = -1;
        for (String idxStr : idxs) {
            int idx = Integer.parseInt(idxStr);
            idxsInt.add(idx);
            if (idx > maxIdx) {
                maxIdx = idx;
            }
        }

        ArrayList<Integer> infoList = new ArrayList<>();

        for (int i = 0; i < frInfo.length; i++) {
            int extNo = frInfo[i].getFitsRead().getHduNumber();
            if (idxsInt.contains(extNo) || (idxsInt.contains(-1) && extNo==0)) {
                infoList.add(i);
            } else if(extNo > maxIdx) {
                break;
            }
        }

        return infoList;
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

    private static PlotState[] makeNoBandMultiImagePlotState(WebPlotRequest request, FileReadInfo[] info) {
        ArrayList<Integer> infoList = new ArrayList<>();
        for (int i = 0; i < info.length; i++) {
            infoList.add(i);
        }
        return makeNoBandMultiImagePlotStateOnList(request, info, PlotState.MultiImageAction.USE_ALL, infoList);
    }

    private static PlotState[] makeNoBandMultiImagePlotStateOnList(WebPlotRequest request,
                                                                   FileReadInfo[] info,
                                                                   PlotState.MultiImageAction action,
                                                                   ArrayList<Integer> infoList ) {
        PlotState[] stateAry = new PlotState[infoList.size()];
        for (int i = 0; (i < stateAry.length); i++) {
            int n = infoList.get(i);

            stateAry[i] = PlotStateUtil.create(request);
            stateAry[i].setMultiImageAction(action);
            initState(stateAry[i], info[n], NO_BAND, request);
            stateAry[i].setOriginalImageIdx(n, NO_BAND);
            stateAry[i].setImageIdx(n, NO_BAND);
        }
        initMultiImageInfoOnList(stateAry,info, infoList);
        return stateAry;
    }

    private static void initMultiImageInfoOnList(PlotState[] stateAry,  FileReadInfo[] infoAry, ArrayList<Integer>infoList) {
        if (stateAry.length!=infoList.size() && stateAry.length>1) return;
        boolean multiImageFile= stateAry.length>1;

        boolean cube= false;
        for (Integer v : infoList) {
            cube = infoAry[v].getFitsRead().getPlaneNumber() > 1;
            if (cube) break;
        }

        int cubePlane= 1;
        int cubeCnt= 0;
        for (int i = 0; (i < stateAry.length); i++) {
            stateAry[i].setMultiImageFile(multiImageFile, NO_BAND);
            if (cube) {
                if (infoAry[infoList.get(i)].getFitsRead().getPlaneNumber()==1) {
                    cubePlane=1;
                    cubeCnt++;
                }
                stateAry[i].setCubeCnt(cubeCnt,Band.NO_BAND);
                stateAry[i].setCubePlaneNumber(cubePlane,Band.NO_BAND);
                cubePlane++;
            }
        }
    }

    private static void initState(PlotState state,
                          FileReadInfo fi,
                          Band band,
                          WebPlotRequest req) {
        if (state.isBandUsed(band)) {
            if (state.getContextString() == null) {
                state.setContextString(CtxControl.makeCachedCtx());
            }
            state.setTileCompress(fi.getFitsRead().isTileCompress(), band);
            state.setOriginalImageIdx(fi.getOriginalImageIdx(), band);
            PlotStateUtil.setOriginalFitsFile(state, fi.getOriginalFile(), band);
            PlotStateUtil.setWorkingFitsFile(state, fi.getWorkingFile(), band);
            state.setUploadFileName(fi.getUploadedName(),band);
            checkFileNames(state, fi.getOriginalFile(), band);
            state.setOriginalImageIdx(fi.getOriginalImageIdx(), band);
            state.setImageIdx(fi.getOriginalImageIdx(), band);
            if (WebPlotPipeline.isRotation(req)) {
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
        return threeColor ? PlotState.MultiImageAction.USE_FIRST : PlotState.MultiImageAction.USE_ALL;
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

        return new ZoomChoice(false,
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
        private final ImagePlotInfo[] plotInfoAry;
        private final long findElapse;
        private final long readElapse;
        private final ZoomChoice zoomChoice;

        private Results(ImagePlotInfo[] plotInfoAry, ZoomChoice zoomChoice, long findElapse, long readElapse) {
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
