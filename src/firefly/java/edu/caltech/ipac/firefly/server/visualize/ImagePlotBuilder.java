/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.FileRetriever;
import edu.caltech.ipac.firefly.server.visualize.imageretrieve.ImageFileRetrieverFactory;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.BandState;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.RangeValues;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;
import nom.tam.fits.FitsException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    public enum MultiImageAction { GUESS,      // Default, guess between load first, and use all, depending on three color params
        USE_FIRST,   // only valid option if loading a three color with multiple Request
        USE_IDX,   // use a specific image from the fits read Array
        USE_EXTS,  // use a list of specific image extension from fits read Array
        USE_ALL} // only valid in non three color, make a array of WebPlots

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================


    public static SimpleResults create(WebPlotRequest wpr) throws FailedRequestException, GeomException {
        List<SimpleResults> retList= createList(wpr);
        return (retList.size()>0) ? retList.get(0) : null;
    }

    public static SimpleResults create3Color(WebPlotRequest redRequest,
                                         WebPlotRequest greenRequest,
                                         WebPlotRequest blueRequest) throws FailedRequestException {
        try {
            SimpleResults retval= null;
            LinkedHashMap<Band, WebPlotRequest> requestMap = new LinkedHashMap<>(5);

            if (redRequest != null) requestMap.put(RED, redRequest);
            if (greenRequest != null) requestMap.put(GREEN, greenRequest);
            if (blueRequest != null) requestMap.put(BLUE, blueRequest);
            Results allPlots= build(requestMap, MultiImageAction.USE_FIRST, null, true);
            ImagePlotCreator.PlotInfo[] piAry= allPlots.plotInfoAry();
            if (piAry!=null && piAry.length>0)  retval= new SimpleResults(piAry[0].plot(), piAry[0].fitsReadGroup());
            return retval;
        } catch (Exception e) {
            throw makeException(e);
        }
    }


    private static List<SimpleResults> createList(WebPlotRequest wpr) throws FailedRequestException {
        try {
            wpr.setProgressKey(null); // this just makes sure in update progress caching does not happen
            List<SimpleResults> retList= new ArrayList<>(10);
            Map<Band, WebPlotRequest> requestMap = new LinkedHashMap<>(2);
            requestMap.put(NO_BAND, wpr);
            Results allPlots= build(requestMap, MultiImageAction.USE_FIRST, null, false);
            for(ImagePlotCreator.PlotInfo pi : allPlots.plotInfoAry())  retList.add(new SimpleResults(pi.plot(),pi.fitsReadGroup()));
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
        PlotServUtils.updatePlotCreateProgress( firstR, ProgressStat.PType.CREATING, PlotServUtils.CREATING_MSG);
        purgeFailedBands(readInfoMap, requestMap);
        long readElapse = System.currentTimeMillis() - readStart;

        // ------------ make the ImagePlot(s)
        ImagePlotCreator.PlotInfo[] pInfo;
        if (state == null) {
            pInfo = makeNewPlots(readInfoMap, requestMap, multiAction, threeColor);
        } else {
            pInfo = new ImagePlotCreator.PlotInfo[] {recreatePlot(state, readInfoMap)};
        }

        return new Results(pInfo,findElapse,readElapse);
    }




//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    static private ImagePlotCreator.PlotInfo recreatePlot(PlotState state, Map<Band, WebPlotReader.FileReadInfo[]> readInfoMap)
            throws FailedRequestException, IOException, FitsException, GeomException {
        return ImagePlotCreator.makeOneImagePerBand(state, readInfoMap);
    }



    private static Map<Band, FileInfo> findFiles(Map<Band, WebPlotRequest> requestMap) throws Exception {

        Map<Band, FileInfo> fitsFiles = new LinkedHashMap<>();

        PlotServUtils.updatePlotCreateProgress( firstRequest(requestMap), ProgressStat.PType.READING,
                                                PlotServUtils.STARTING_READ_MSG);

        for (var entry : requestMap.entrySet()) {
            Band band = entry.getKey();
            WebPlotRequest request = entry.getValue();
            FileRetriever retrieve = ImageFileRetrieverFactory.getRetriever(request);
            if (retrieve != null) {
                FileInfo fileData;
                try {
                    fileData = retrieve.getFile(request);
                    fitsFiles.put(band, fileData);
                } catch (Exception e) {
                    throw e;
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
     * @param multiAction enum that gives direction on how to take the readInfoMap make plots out of them
     * @param threeColor  should be be making a three color plot
     * @return an array of PlotInfo, one for each web plot that will be created on the client
     * @throws FailedRequestException configuration error
     * @throws IOException            error reading the file
     * @throws FitsException          error creating the fits data
     * @throws GeomException          on geom error
     */
    private static ImagePlotCreator.PlotInfo[] makeNewPlots(Map<Band, WebPlotReader.FileReadInfo[]> readInfoMap,
                                                            Map<Band, WebPlotRequest> requestMap,
                                                            MultiImageAction multiAction,
                                                            boolean threeColor) throws FailedRequestException,
                                                                           IOException,
                                                                           FitsException,
                                                                           GeomException {

        ImagePlotCreator.PlotInfo[] plotInfo = new ImagePlotCreator.PlotInfo[1];
        PlotState state;


        switch (multiAction) {
            case GUESS:
                plotInfo = makeNewPlots(readInfoMap, requestMap, getActionGuess(threeColor), threeColor);
                break;
            case USE_FIRST:
                if (threeColor) state = make3ColorState(requestMap, readInfoMap);
                else            state = makeState(requestMap.get(NO_BAND), readInfoMap.get(NO_BAND)[0]);
                for (Band band : requestMap.keySet()) {
                    state.setOriginalImageIdx(0, band);
                    state.setImageIdx(0, band);
                }
                plotInfo[0] = ImagePlotCreator.makeOneImagePerBand(state, readInfoMap);
                break;
            case USE_IDX:
                WebPlotRequest r= requestMap.get(NO_BAND);
                int idx= r.getMultiImageIdx();
                state = makeState(requestMap.get(NO_BAND), readInfoMap.get(NO_BAND)[idx]);
                state.setOriginalImageIdx(idx, NO_BAND);
                state.setImageIdx(idx, NO_BAND);
                state.setMultiImageFile(true,Band.NO_BAND);
                plotInfo[0] = ImagePlotCreator.makeOneImagePerBand(state, readInfoMap);
                break;
            case USE_EXTS:
                if (!readInfoMap.containsKey(NO_BAND) || threeColor) {
                    throw new FailedRequestException("Cannot create plot",
                            "Cannot yet use the MultiImageAction.USE_EXT action with three color");
                }
                WebPlotRequest rs = requestMap.get(NO_BAND);

                ArrayList<Integer> infoList = getFileReadList(rs.getMultiImageExts(), readInfoMap.get(NO_BAND));

               PlotState[] stateArys = makeNoBandMultiImagePlotStateOnList(rs, readInfoMap.get(NO_BAND), infoList);
                plotInfo = new ImagePlotCreator.PlotInfo[stateArys.length];
                for (int i = 0; i < stateArys.length; i++) {
                    plotInfo[i] = ImagePlotCreator.makeOneImagePerBand(stateArys[i], readInfoMap);
                }
                break;
            case USE_ALL:
                if (!readInfoMap.containsKey(NO_BAND) || threeColor) {
                    throw new FailedRequestException("Cannot create plot",
                                                     "Cannot yet use the MultiImageAction.USE_ALL action with three color");
                }
                PlotState[] stateAry = makeNoBandMultiImagePlotState(requestMap.get(NO_BAND), readInfoMap.get(NO_BAND));
                plotInfo = ImagePlotCreator.makeAllNoBand(stateAry, readInfoMap.get(NO_BAND));
                break;
            default:
                throw new FailedRequestException("Plot creation failed", "unknown multiAction, don't know how to create plot");

        }

        return plotInfo;
    }

    private static ArrayList<Integer> getFileReadList(String extList, WebPlotReader.FileReadInfo[] frInfo) {
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

    private static PlotState makeState(WebPlotRequest request, WebPlotReader.FileReadInfo readInfo) {
        PlotState state = PlotStateUtil.create(request);
        initState(state, readInfo, NO_BAND);
        return state;
    }



    private static PlotState make3ColorState(Map<Band, WebPlotRequest> requestMap,
                                             Map<Band, WebPlotReader.FileReadInfo[]> readInfoMap) {

        PlotState state = PlotStateUtil.create(requestMap);
        for (Map.Entry<Band, WebPlotReader.FileReadInfo[]> entry : readInfoMap.entrySet()) {
            Band band = entry.getKey();
            WebPlotReader.FileReadInfo fi = entry.getValue()[0];
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

    private static PlotState[] makeNoBandMultiImagePlotState(WebPlotRequest request, WebPlotReader.FileReadInfo[] info) {
        ArrayList<Integer> infoList = new ArrayList<>();
        for (int i = 0; i < info.length; i++) {
            infoList.add(i);
        }
        return makeNoBandMultiImagePlotStateOnList(request, info, infoList);
    }

    private static PlotState[] makeNoBandMultiImagePlotStateOnList(WebPlotRequest request,
                                                                   WebPlotReader.FileReadInfo[] info,
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


    private static void initState(PlotState state, WebPlotReader.FileReadInfo fi, Band band ) {
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


    private static MultiImageAction getActionGuess(boolean threeColor) {
        return threeColor ? MultiImageAction.USE_FIRST : MultiImageAction.USE_ALL;
    }


    private static void purgeFailedBands(Map<Band, WebPlotReader.FileReadInfo[]> readInfoMap, Map<Band, WebPlotRequest> requestMap) {
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

    record Results(ImagePlotCreator.PlotInfo[] plotInfoAry, long findElapse, long readElapse) { }


    public record SimpleResults(ImagePlot plot, ActiveFitsReadGroup frGroup) {
        public ImagePlot getPlot() { return plot; }
        public ActiveFitsReadGroup getFrGroup() { return frGroup; }
    }
}
