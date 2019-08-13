/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.BandState;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.firefly.visualize.WebPlotHeaderInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImageDataGroup;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.plotdata.GeomException;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.visualize.Band.*;

/**
 * @author Trey Roby
 */
public class WebPlotFactory {

    private static final Logger.LoggerImpl _log = Logger.getLogger();

    static {
        VisContext.init();
    }

    public static WebPlotFactoryRet createNew(WebPlotRequest rRequest,
                                                 WebPlotRequest gRequest,
                                                 WebPlotRequest bRequest) throws FailedRequestException, GeomException {

        LinkedHashMap<Band, WebPlotRequest> rMap = new LinkedHashMap<>();

        if (rRequest != null) rMap.put(RED, rRequest);
        if (gRequest != null) rMap.put(GREEN, gRequest);
        if (bRequest != null) rMap.put(BLUE, bRequest);

        return create(rMap, PlotState.MultiImageAction.USE_FIRST, null, true);
    }

// Unused code commented out 3/5/2019 - keep around for awhile
//    public static WebPlotInitializer[] createNewGroup(List<WebPlotRequest> wprList) throws Exception {
//
//
//        FileRetriever retrieve = ImageFileRetrieverFactory.getRetriever(wprList.get(0));
//        FileInfo fileData = retrieve.getFile(wprList.get(0));
//
//        FitsDataEval fitsDataInfo = FitsCacher.readFits(fileData, wprList.get(0), false, false);
//        FitsRead[] frAry= fitsDataInfo.getFitReadAry();
//        List<ImagePlotBuilder.Results> resultsList= new ArrayList<>(wprList.size());
//        int length= Math.min(wprList.size(), frAry.length);
//        WebPlotInitializer retval[]= new WebPlotInitializer[length];
//        for(int i= 0; (i<length); i++) {
//            WebPlotRequest request= wprList.get(i);
//            ImagePlotBuilder.Results r= ImagePlotBuilder.buildFromFile(request, fileData,frAry[i],
//                                                                       request.getMultiImageIdx(),null);
//            resultsList.add(r);
//        }
//        for(int i= 0; (i<resultsList.size()); i++) {
//            ImagePlotBuilder.Results r= resultsList.get(i);
//            ImagePlotInfo pi = r.getPlotInfoAry()[0];
//            PlotServUtils.updatePlotCreateProgress(pi.getState().getWebPlotRequest(), ProgressStat.PType.CREATING,
//                                         PlotServUtils.PROCESSING_MSG+": "+ (i+1)+" of "+resultsList.size());
//
//            for (Map.Entry<Band, ModFileWriter> entry : pi.getFileWriterMap().entrySet()) {
//                ModFileWriter mfw = entry.getValue();
//                if (mfw != null) {
//                    if (mfw.getCreatesOnlyOneImage()) pi.getState().setImageIdx(0, entry.getKey());
//                    mfw.writeFile(pi.getState());
//                }
//            }
//
//            retval[i] = makePlotResults(pi, true, r.getZoomChoice());
//        }
//        return retval;
//    }


    public static WebPlotFactoryRet createNew(WebPlotRequest request) throws FailedRequestException, GeomException {
        Map<Band, WebPlotRequest> requestMap = new LinkedHashMap<>(2);
        requestMap.put(NO_BAND, request);
        PlotState.MultiImageAction multiAction= PlotState.MultiImageAction.USE_ALL;
        if (request.containsParam(WebPlotRequest.MULTI_IMAGE_IDX)) {
            multiAction= PlotState.MultiImageAction.USE_IDX;
        } else if (request.containsParam(WebPlotRequest.MULTI_IMAGE_EXTS)) {
            multiAction = PlotState.MultiImageAction.USE_EXTS;
        }
        return create(requestMap, multiAction, null, false);
    }

    public static WebPlotFactoryRet recreate(PlotState state) throws FailedRequestException, GeomException {
        Map<Band, WebPlotRequest> requestMap = new LinkedHashMap<Band, WebPlotRequest>(5);
        for (Band band : state.getBands()) requestMap.put(band, state.getWebPlotRequest(band));
        for(WebPlotRequest req : requestMap.values()) {
            req.setZoomType(ZoomType.LEVEL);
            req.setInitialZoomLevel(state.getZoomLevel());
        }
        WebPlotFactoryRet retval = create(requestMap, null, state, state.isThreeColor());
        Assert.argTst(retval.wpInit.length == 1, "in this case you should never have more than one result");
        return retval;
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


    private static WebPlotFactoryRet create(Map<Band, WebPlotRequest> requestMap,
                                               PlotState.MultiImageAction multiAction,
                                               PlotState state,
                                               boolean threeColor) throws FailedRequestException, GeomException {


        long start = System.currentTimeMillis();
        WebPlotRequest saveRequest = requestMap.values().iterator().next();
        WebPlotInitializer[] wpInit;
        WebPlotHeaderInitializer wpHeader= null;
        if (requestMap.size() == 0) {
            throw new FailedRequestException("Could not create plot", "All WebPlotRequest are null");
        }


        try {

            ImagePlotBuilder.Results allPlots= ImagePlotBuilder.build(requestMap, multiAction, state, threeColor);

            // ------------ Iterate through results, Prepare the return objects, including PlotState if it is null
            ImagePlotInfo[] pInfo= allPlots.getPlotInfoAry();
            wpInit = new WebPlotInitializer[pInfo.length];
            for (int i = 0; (i < pInfo.length); i++) {
                ImagePlotInfo pi = pInfo[i];
                if (i==0) saveRequest= pi.getState().getWebPlotRequest();

                boolean notify= pInfo.length<5 || i % ((pInfo.length/10)+1)==0 || i==pInfo.length-1;
                if (notify) {
                    if (pInfo.length>3) {
                        if (i==pInfo.length-1) {
                            PlotServUtils.updatePlotCreateProgress(pi.getState().getWebPlotRequest(),
                                    ProgressStat.PType.CREATING,
                                    PlotServUtils.PROCESSING_COMPLETED_MSG);
                        }
                        else {
                            PlotServUtils.updatePlotCreateProgress(pi.getState().getWebPlotRequest(), ProgressStat.PType.CREATING,
                                    PlotServUtils.PROCESSING_MSG+": "+ (i+1)+" of "+pInfo.length);
                        }
                    }
                    else {
                        PlotServUtils.updatePlotCreateProgress(pi.getState().getWebPlotRequest(),ProgressStat.PType.CREATING,
                                PlotServUtils.PROCESSING_MSG);
                    }
                }
                for (Map.Entry<Band, ModFileWriter> entry : pi.getFileWriterMap().entrySet()) {
                    ModFileWriter mfw = entry.getValue();
                    if (mfw != null) {
                        if (mfw.getCreatesOnlyOneImage()) pi.getState().setImageIdx(0, entry.getKey());
                        mfw.writeFile(pi.getState());
                    }
                }

                if (i==0 && !threeColor) {
                    PlotState s= pInfo[i].getState();

                    FitsRead[] frAry= pInfo[0].getFrGroup().getFitsReadAry();
                    Header[] zeroHeaderAry= new Header[frAry.length];
                    for(int k=0; k<zeroHeaderAry.length; k++) {
                        zeroHeaderAry[k]= frAry[k]!=null ? frAry[k].getZeroHeader() : null;
                    }
                    wpHeader= new WebPlotHeaderInitializer(s.getOriginalFitsFileStr(NO_BAND),
                            s.getWorkingFitsFileStr(NO_BAND), s.getUploadFileName(NO_BAND),
                            pInfo[i].getDataDesc(), false, s.getPrimaryRequest(),zeroHeaderAry);
                }
                wpInit[i] = makePlotResults(pi, (i < 2 || i > pInfo.length - 2), allPlots.getZoomChoice(), !threeColor);
            }

//            if (saveRequest!=null) {
//                PlotServUtils.updatePlotCreateProgress(saveRequest, ProgressStat.PType.SUCCESS, "Success");
//            }

            long elapse = System.currentTimeMillis() - start;
            logSuccess(pInfo[0].getState(), elapse, allPlots.getFindElapse(), allPlots.getReadElapse(), false, null, true);
        } catch (FailedRequestException e) {
            updateProgressIsFailure(saveRequest);
            throw e;
        } catch (FitsException e) {
            updateProgressIsFailure(saveRequest);
            throw new FailedRequestException(e.getMessage(), e.getMessage(), e);
        } catch (Exception e) {
            updateProgressIsFailure(saveRequest);
            throw new FailedRequestException("Could not create plot", e.getMessage(), e);
        }
        return new WebPlotFactoryRet(wpInit,wpHeader);
    }

    private static void updateProgressIsFailure(WebPlotRequest wpr) {
        if (wpr!=null) PlotServUtils.updatePlotCreateProgress(wpr, ProgressStat.PType.FAIL, "Failed");
    }


    private static WebPlotInitializer makePlotResults(ImagePlotInfo pInfo, boolean makeFiles,
                                                      ZoomChoice zoomChoice, boolean clearHeaderData)
                                               throws FitsException, IOException {
        PlotState state = pInfo.getState();

        for(Band b : state.getBands()) { //clearing out hdu cuts fits memory usage in half
            File f= ServerContext.convertToFile(state.getWorkingFitsFileStr(b));
            if (f!=null) FitsCacher.clearCachedHDU(f);
        }

        boolean fullScreen= zoomChoice.getZoomType() == ZoomType.FULL_SCREEN;
        PlotImages images = createImages(state, pInfo.getPlot(), pInfo.getFrGroup(),makeFiles, fullScreen);

        WebPlotInitializer wpInit = makeWebPlotInitializer(state, images, pInfo, clearHeaderData);

        CtxControl.initPlotCtx(state, pInfo.getPlot(), pInfo.getFrGroup(), images);

        return wpInit;
    }

    private static WebPlotInitializer makeWebPlotInitializer(PlotState state, PlotImages images,
                                                             ImagePlotInfo pInfo,
                                                             boolean clearHeaderData) {
        // need a WebFits Data each band: normal is 1, 3 color is three
        WebFitsData wfDataAry[] = new WebFitsData[3];

        for (Map.Entry<Band, WebFitsData> entry : pInfo.getWebFitsDataMap().entrySet()) {
            wfDataAry[entry.getKey().getIdx()] = entry.getValue();
        }



        ImagePlot plot = pInfo.getPlot();
        FitsRead[] frAry= pInfo.getFrGroup().getFitsReadAry();
        Header[] headerAry= new Header[frAry.length];
        Header[] zeroHeaderAry= new Header[frAry.length];
        for(int i=0; i<headerAry.length; i++) {
            headerAry[i]= frAry[i]!=null ? frAry[i].getHeader() : null;
            zeroHeaderAry[i]= frAry[i]!=null ? frAry[i].getZeroHeader() : null;
        }
        ImageDataGroup imageData = plot.getImageData();

        int dataWidth= imageData.getImageWidth();
        int dataHeight= imageData.getImageHeight();
        CoordinateSys imageCoordSys= plot.getCoordinatesOfPlot();



        List<RelatedData> rdList= pInfo.getRelatedData();
        if (!state.isThreeColor()) {
            int cubePlane = headerAry[NO_BAND.getIdx()].getIntValue("SPOT_PL", -1);
            if (cubePlane>=0) {
                state.setCubePlaneNumber(cubePlane,NO_BAND);
                if (cubePlane>0) {  // have a cube
                    headerAry = null;
                    rdList = null;
                    dataWidth = -1;
                    dataHeight = -1;
                    imageCoordSys = null;
                }
            }
        }

        if (clearHeaderData) {
            state= state.makeCopy();
            BandState bsAry[]= state.getBandStateAry();
            for(int i=0; i<3; i++) {
                if (bsAry[i]!=null) {
                    bsAry[i].setWebPlotRequest(null);
                    bsAry[i].setUploadedFileName(null);
                    bsAry[i].setWorkingFitsFileStr(null);
                    bsAry[i].setOriginalFitsFileStr(null);
                }
            }
            zeroHeaderAry= null;
        }


        return new WebPlotInitializer(state,
                                      images,
                                      imageCoordSys,
                                      headerAry,
                                      zeroHeaderAry,
                                      dataWidth, dataHeight,
                                      wfDataAry,
                                      plot.getPlotDesc(),
                                      clearHeaderData ? null : pInfo.getDataDesc(),
                                      rdList);
    }


    private static PlotImages createImages(PlotState state,
                                           ImagePlot plot,
                                           ActiveFitsReadGroup frGroup,
                                           boolean makeFiles,
                                           boolean fullScreen) throws IOException {
        String base = PlotServUtils.makeTileBase(state);

        return PlotServUtils.writeImageTiles(ServerContext.getVisSessionDir(),
                                             base, plot, frGroup, fullScreen,
                                             makeFiles ? 2 : 0);

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

        List<String> out = new ArrayList<>(8);
        String more = String.format("%s%9s%s%9s",
                                    ", Find-", UTCTimeUtil.getHMSFromMills(findElapse),
                                    time3String, UTCTimeUtil.getHMSFromMills(readElapse));
        out.add(majType + " - " + minType + ": Total: " + UTCTimeUtil.getHMSFromMills(elapse) + more +
                ", Ctx:"+state.getContextString());

        if (bandAdded) {
            String bStr = newBand.toString() + " - ";
            File f = PlotStateUtil.getWorkingFitsFile(state, newBand);
            out.add("band: " + newBand + " added");
            if (!PlotServUtils.isBlank(state, newBand)) {
                String sizeStr= FileUtil.getSizeAsString(f.length());
                out.add(bStr + "filename "+"("+sizeStr+ ")" +": " + f.getPath());
                totSize = f.length();
            } else {
                out.add(bStr + "Blank Image");
            }
        } else {
            for (Band band : state.getBands()) {
                String bStr = state.isThreeColor() ? StringUtils.pad(5, band.toString()) + " - " : "";
                File f = PlotStateUtil.getWorkingFitsFile(state, band);
                if (!PlotServUtils.isBlank(state, band)) {
                    if (f!=null) {
                        String sizeStr= FileUtil.getSizeAsString(f.length());
                        out.add(bStr + "filename "+"("+sizeStr+ ")" +": " + f.getPath());
                        totSize += f.length();
                    }
                } else {
                    out.add(bStr + "Blank Image");
                }
            }
        }
        out.add("PlotState Summary: " + state.toPrettyString());

        String statDetails = String.format("%6s%s", FileUtil.getSizeAsString(totSize), more);
        _log.info(out.toArray(new String[out.size()]));
        PlotServUtils.statsLog("create", "total-MB", (double) totSize / StringUtils.MEG, "Details", statDetails);
        Counters.getInstance().incrementKB(Counters.Category.Visualization, "Total Read", totSize/ StringUtils.K);
    }


    public static class WebPlotFactoryRet {
        private WebPlotInitializer[] wpInit;
        private WebPlotHeaderInitializer wpHeader;

        public WebPlotFactoryRet(WebPlotInitializer[] wpInit, WebPlotHeaderInitializer wpHeader) {
            this.wpInit = wpInit;
            this.wpHeader = wpHeader;
        }

        public WebPlotInitializer[] getWpInit() { return wpInit; }

        public WebPlotHeaderInitializer getWpHeader() { return wpHeader; }
    }

}

