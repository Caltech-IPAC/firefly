/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.ImagePlotCreator.PlotInfo;
import edu.caltech.ipac.firefly.server.visualize.WebPlotReader.FileReadInfo;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.FileRetriever;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.ImageFileRetrieverFactory;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.BandState;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;
import nom.tam.fits.FitsException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.server.visualize.ProgressStat.PType;
import static edu.caltech.ipac.firefly.visualize.Band.BLUE;
import static edu.caltech.ipac.firefly.visualize.Band.GREEN;
import static edu.caltech.ipac.firefly.visualize.Band.NO_BAND;
import static edu.caltech.ipac.firefly.visualize.Band.RED;

/**
 * @author Trey Roby
 */
public class ImagePlotBuilder {

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    public enum MultiImageAction {
        USE_FIRST,   // only valid option if loading a three color with multiple Request
        USE_IDX,   // use a specific image from the fits read Array
        USE_EXTS,  // use a list of specific image extension from fits read Array, is an extension is a cube it will read all planes
        USE_ALL; // use every image extension
        public static boolean support3C(MultiImageAction a) { return a==USE_FIRST; }
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public static PlotInfo create(WebPlotRequest wpr) throws FailedRequestException, GeomException {
        List<PlotInfo> retList= createList(wpr);
        return (retList.size()>0) ? retList.get(0) : null;
    }

    public static PlotInfo create3Color(WebPlotRequest redRequest,
                                         WebPlotRequest greenRequest,
                                         WebPlotRequest blueRequest) throws FailedRequestException {
        try {
            PlotInfo retval= null;
            LinkedHashMap<Band, WebPlotRequest> requestMap = new LinkedHashMap<>(5);

            if (redRequest != null) requestMap.put(RED, redRequest);
            if (greenRequest != null) requestMap.put(GREEN, greenRequest);
            if (blueRequest != null) requestMap.put(BLUE, blueRequest);
            Results allPlots= build(requestMap, MultiImageAction.USE_FIRST, null, true);
            PlotInfo[] piAry= allPlots.plotInfoAry();
            if (piAry!=null && piAry.length>0)  retval= piAry[0];
            return retval;
        } catch (Exception e) {
            throw makeException(e);
        }
    }


    private static List<PlotInfo> createList(WebPlotRequest wpr) throws FailedRequestException {
        try {
            wpr.setProgressKey(null); // this just makes sure update progress caching does not happen
            List<PlotInfo> retList= new ArrayList<>(10);
            Map<Band, WebPlotRequest> requestMap = new LinkedHashMap<>(2);
            requestMap.put(NO_BAND, wpr);
            Results allPlots= build(requestMap, MultiImageAction.USE_FIRST, null, false);
            Collections.addAll(retList, allPlots.plotInfoAry());
            return retList;
        } catch (Exception e) {
            throw makeException(e);
        }
    }

    private static FailedRequestException makeException(Exception e) {
        if (e instanceof FailedRequestException) {
            return new FailedRequestException("Could not create plot. " + e.getMessage(),
                    ((FailedRequestException)e).getDetailMessage());
        }
        else if (e instanceof FitsException) {
            return new FailedRequestException("Could not create plot. Invalid FITS File format.", e.getMessage());
        }
        else {
            return new FailedRequestException("Could not create plot.", e.getMessage(), e);
        }
    }

    static Results build(Map<Band, WebPlotRequest> requestMap,
                         MultiImageAction multiAction,
                         PlotState state,
                         boolean threeColor) throws Exception {

        // ------------ find (maybe download) the files to read
        long findStart = System.currentTimeMillis();
        Map<Band, FileInfo> fileDataMap = findFiles(requestMap);
        long findElapse = System.currentTimeMillis() - findStart;

        // ------------ read the FITS files
        long readStart = System.currentTimeMillis();
        WebPlotRequest firstR = requestMap.values().iterator().next();
        var readInfoMap = WebPlotReader.readFiles(fileDataMap, firstR);
        PlotServUtils.updateProgress( firstR, PType.CREATING, PlotServUtils.CREATING_MSG);
        purgeFailedBands(readInfoMap, requestMap);
        long readElapse = System.currentTimeMillis() - readStart;

        // ------------ make the fits plotting data
        PlotInfo[] pInfo= (state==null) ?
                makeNewPlots(readInfoMap, requestMap, multiAction, threeColor) :
                new PlotInfo[] {recreatePlot(state, readInfoMap)};

        return new Results(pInfo,findElapse,readElapse);
    }



    public static MultiImageAction reqToMultiAction(WebPlotRequest r) {
        if (r.containsParam(WebPlotRequest.MULTI_IMAGE_IDX)) return MultiImageAction.USE_IDX;
        if (r.containsParam(WebPlotRequest.MULTI_IMAGE_EXTS)) return MultiImageAction.USE_EXTS;
        return MultiImageAction.USE_ALL;
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    static private PlotInfo recreatePlot(PlotState state, Map<Band, FileReadInfo[]> readInfoMap)
            throws FailedRequestException, IOException, FitsException, GeomException {
        return state.isThreeColor() ?
                ImagePlotCreator.makeOneImagePerBand3Color(state, readInfoMap) :
                ImagePlotCreator.makeNoBand(state, readInfoMap.get(NO_BAND)[0]);
    }



    private static Map<Band, FileInfo> findFiles(Map<Band, WebPlotRequest> requestMap) throws Exception {

        Map<Band, FileInfo> fitsFiles = new LinkedHashMap<>();
        PlotServUtils.updateProgress( firstRequest(requestMap), PType.READING, PlotServUtils.STARTING_READ_MSG);

        for (var entry : requestMap.entrySet()) {
            Band band = entry.getKey();
            WebPlotRequest request = entry.getValue();
            FileRetriever retrieve = ImageFileRetrieverFactory.getRetriever(request);
            if (retrieve != null) {
                fitsFiles.put(band, retrieve.getFile(request));
            } else {
                _log.error("failed to find FileRetriever should only be FILE, URL, ALL_SKY, or SERVICE, for band " + band.toString());
            }
        }

        if (fitsFiles.size() > 0) {
            WebPlotReader.validateAccess(fitsFiles);
        } else {
            _log.error("could not find any fits files from request");
        }
        PlotServUtils.updateProgress(firstRequest(requestMap), PType.READING, PlotServUtils.ENDING_READ_MSG);

        return fitsFiles;
    }


    /**
     * This method will create and determine how many plots to make and create the plot state for each plot.  It is
     * only called when the plot is being created for the first time.  Any recreates do not go through this method.
     *
     * @param readInfoMap the map of band to all the fits images read and
     * @param requestMap  the map of band to all the plot request
     * @param multiAction enum that gives direction on how to take the readInfoMap make plots out of them
     * @param threeColor  should be making a three color plot
     * @return an array of PlotInfo, one for each web plot that will be created on the client
     * @throws FailedRequestException configuration error
     * @throws IOException            error reading the file
     * @throws FitsException          error creating the fits data
     * @throws GeomException          on geom error
     */
    private static PlotInfo[] makeNewPlots(Map<Band, FileReadInfo[]> readInfoMap,
                                           Map<Band, WebPlotRequest> requestMap,
                                           MultiImageAction multiAction,
                                           boolean threeColor)
            throws FailedRequestException, IOException, FitsException, GeomException {

        validateParams(multiAction,threeColor);
        FileReadInfo[] readAry= readInfoMap.get(NO_BAND); // use for non-three color
        WebPlotRequest req= requestMap.get(NO_BAND); // use for non-three color

        return switch (multiAction) {
            case USE_FIRST -> {
                PlotState state = threeColor ? make3ColorState(requestMap, readInfoMap) : makeState(req, readAry[0]);
                yield asArray(threeColor ?
                        ImagePlotCreator.makeOneImagePerBand3Color(state, readInfoMap) :
                        ImagePlotCreator.makeNoBand(state, readAry[0]));
            }
            case USE_IDX -> {
                int idx= req.getMultiImageIdx();
                var state= makeStateOnImageIdx( req, readAry[idx], idx);
                yield asArray(ImagePlotCreator.makeNoBandFromIdx(state, readAry));
            }
            case USE_EXTS -> {
                var infoList = getFileReadList( req.getMultiImageExts(), readAry);
                var stateArys = makeNoBandMultiImagePlotStateOnList( req, readAry, infoList);
                yield ImagePlotCreator.makeAllNoBandByState(stateArys,readAry);
            }
            case USE_ALL -> ImagePlotCreator.makeAllNoBand(makeNoBandMultiImagePlotState( req, readAry), readAry);
        };
    }

    private static void validateParams( MultiImageAction multiAction, boolean threeColor)
            throws FailedRequestException{
        if (threeColor && !MultiImageAction.support3C(multiAction)) {
            throw new FailedRequestException("Cannot create plot",
                    "Cannot yet use the " + multiAction + " action with three color");
        }
    }

    private static PlotInfo[] asArray(PlotInfo pi) { return new PlotInfo[] {pi}; }

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

        ArrayList<Integer> infoList = new ArrayList<>(frInfo.length);

        for (int i = 0; i < frInfo.length; i++) {
            int extNo = frInfo[i].fitsRead().getHduNumber();
            if (idxsInt.contains(extNo) || (idxsInt.contains(-1) && extNo==0)) {
                infoList.add(i);
            } else if(extNo > maxIdx) {
                break;
            }
        }

        return infoList;
    }

    private static PlotState makeState(WebPlotRequest request, FileReadInfo readInfo) {
        PlotState state = PlotStateUtil.create(request);
        initState(state, readInfo, NO_BAND);
        return state;
    }

    private static PlotState makeStateOnImageIdx(WebPlotRequest request, FileReadInfo readInfo, int idx) {
        PlotState state= makeState(request,readInfo);
        state.setOriginalImageIdx(idx, NO_BAND);
        state.setImageIdx(idx, NO_BAND);
        state.setMultiImageFile(true, NO_BAND);
        return state;
    }

    private static PlotState make3ColorState(Map<Band, WebPlotRequest> requestMap,
                                             Map<Band, FileReadInfo[]> readInfoMap) {

        PlotState state = PlotStateUtil.create(requestMap);
        for (Map.Entry<Band, FileReadInfo[]> entry : readInfoMap.entrySet()) {
            Band band = entry.getKey();
            FileReadInfo fi = entry.getValue()[0];
            initState(state, fi, band);
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
        return makeNoBandMultiImagePlotStateOnList(request, info, infoList);
    }

    private static PlotState[] makeNoBandMultiImagePlotStateOnList(WebPlotRequest request,
                                                                   FileReadInfo[] info,
                                                                   ArrayList<Integer> infoList ) {
        boolean cube= false;
        if (infoList.size()>1) {
            for (Integer v : infoList) {
                cube = info[v].fitsRead().getPlaneNumber() > 1;
                if (cube) break;
            }
        }

        PlotState[] stateAry = new PlotState[infoList.size()];
        int cubePlane= 1;
        int cubeCnt= 0;
        BandState bandS;
        for (int i = 0; (i < stateAry.length); i++) {
            int n = infoList.get(i);
            stateAry[i] = PlotStateUtil.create(request);
            bandS= stateAry[i].get(NO_BAND);
            initState(stateAry[i], info[n], NO_BAND);
            bandS.setOriginalImageIdx(n);
            bandS.setImageIdx(n);
            bandS.setMultiImageFile(stateAry.length>1);
            if (cube) {
                if (info[infoList.get(i)].fitsRead().getPlaneNumber()==1) {
                    cubePlane=1;
                    cubeCnt++;
                    if (i>0) {
                        stateAry[i-1].setCubeCnt(cubeCnt,Band.NO_BAND);
                        stateAry[i-1].setCubePlaneNumber(0,Band.NO_BAND);
                    }
                }
                bandS.setCubePlaneNumber(cubePlane);
                bandS.setCubeCnt(cubeCnt);
                cubePlane++;
            }
        }
        return stateAry;
    }


    private static void initState(PlotState state, FileReadInfo fi, Band band ) {
        if (state.isBandUsed(band)) {
            BandState bandS= state.get(NO_BAND);
            if (state.getContextString() == null) state.setContextString(CtxControl.makeCtxString());
            bandS.setTileCompress(fi.fitsRead().isTileCompress());
            bandS.setOriginalImageIdx(fi.originalImageIdx());
            PlotStateUtil.setOriginalFitsFile(state, fi.originalFile(), band);
            PlotStateUtil.setWorkingFitsFile(state, fi.workingFile(), band);
            bandS.setUploadedFileName(fi.uploadedName());
            bandS.setOriginalImageIdx(fi.originalImageIdx());
            bandS.setImageIdx(fi.originalImageIdx());
        }
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

//======================================================================
//------------------ Inner Classes --------------------------------------
//======================================================================

    record Results(PlotInfo[] plotInfoAry, long findElapse, long readElapse) { }


}
