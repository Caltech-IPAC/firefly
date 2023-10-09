/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.data.FileInfo;
import edu.caltech.ipac.firefly.data.RelatedData;
import edu.caltech.ipac.firefly.server.Counters;
import edu.caltech.ipac.firefly.server.util.Logger;
import edu.caltech.ipac.firefly.server.visualize.ImagePlotBuilder.MultiImageAction;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.BandState;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebFitsData;
import edu.caltech.ipac.firefly.visualize.WebPlotHeaderInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.FileUtil;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.UTCTimeUtil;
import edu.caltech.ipac.util.download.FailedRequestException;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static edu.caltech.ipac.firefly.server.visualize.ImagePlotBuilder.reqToMultiAction;
import static edu.caltech.ipac.firefly.server.visualize.ProgressStat.PType;
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

    public static WebPlotFactoryRet createNew(WebPlotRequest rRequest,
                                              WebPlotRequest gRequest,
                                              WebPlotRequest bRequest) throws FailedRequestException {

        LinkedHashMap<Band, WebPlotRequest> rMap = new LinkedHashMap<>(); // use linkedHashMap to keep order
        if (rRequest != null) rMap.put(RED, rRequest);
        if (gRequest != null) rMap.put(GREEN, gRequest);
        if (bRequest != null) rMap.put(BLUE, bRequest);
        return create(rMap, MultiImageAction.USE_FIRST, null, true);
    }

    public static WebPlotFactoryRet createNew(WebPlotRequest request) throws FailedRequestException {
        return create(Collections.singletonMap(NO_BAND, request), reqToMultiAction(request), null, false);
    }
    public static void recreate(PlotState state) throws FailedRequestException {
        Map<Band, WebPlotRequest> requestMap = new LinkedHashMap<>();
        for (Band band : state.getBands()) requestMap.put(band, state.getWebPlotRequest(band));
        MultiImageAction a= state.isThreeColor() ? MultiImageAction.USE_FIRST : MultiImageAction.USE_IDX;
        create(requestMap, a, state, state.isThreeColor());
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static WebPlotFactoryRet create(Map<Band, WebPlotRequest> requestMap,
                                            MultiImageAction multiAction,
                                            PlotState state,
                                            boolean threeColor) throws FailedRequestException {


        if (requestMap.size() == 0) throw new FailedRequestException("Could not create plot", "All WebPlotRequest are null");
        WebPlotRequest saveRequest = requestMap.values().iterator().next();
        boolean useHeader= !threeColor;

        try {
            long start = System.currentTimeMillis();
            ImagePlotBuilder.Results results= ImagePlotBuilder.build(requestMap, multiAction, state, threeColor);

            // ------------ Iterate through results, Prepare the return objects, including PlotState if it is null
            ImagePlotCreator.PlotInfo[] pInfo= results.plotInfoAry();

            if (threeColor) writeAnyModifiedFiles(pInfo);
            WebPlotHeaderInitializer wpHeader= useHeader ? makeWpHeaderInit(pInfo[0]) : null;
            WebPlotInitializer[] wpInit= Arrays.stream(pInfo)
                    .map( pi -> makeWebPlotInitializer(pi,useHeader))
                    .toList()
                    .toArray(new WebPlotInitializer[0]);
            cleanupAnyCachedHDUs(pInfo);
            long elapse = System.currentTimeMillis() - start;
            logSuccess(pInfo[0].state(), elapse, results.findElapse(), results.readElapse());
            return new WebPlotFactoryRet(wpInit,wpHeader);
        } catch (Exception e) {
            PlotServUtils.updateProgress(saveRequest, PType.FAIL, "Failed");
            throw makeException(e);
        }
    }

    private static FailedRequestException makeException(Exception e) {
        if (e instanceof FailedRequestException) return (FailedRequestException)e;
        else if (e instanceof FitsException) return new FailedRequestException(e.getMessage(), e.getMessage(), e);
        else return new FailedRequestException("Could not create plot.", e.getMessage(), e);
    }

    /**
     * If the file was geom for a 3 color plot
     */
    private static void writeAnyModifiedFiles(ImagePlotCreator.PlotInfo[] pInfo) {
        for(var pi : pInfo) {
            PlotState state= pi.state();
            for (Map.Entry<Band, ModFileWriter> entry : pi.fileWriterMap().entrySet()) {
                ModFileWriter mfw = entry.getValue();
                if (mfw != null) {
                    pi.state().setImageIdx(0, entry.getKey());
                    mfw.writeFile(state);
                }
            }
        }
    }

    private static void cleanupAnyCachedHDUs(ImagePlotCreator.PlotInfo[] pInfo) {
        var cleanList= new ArrayList<File>();
        for(ImagePlotCreator.PlotInfo pi : pInfo) {
            PlotState state= pi.state();
            for(Band b : state.getBands()) {
                if (state.getWorkingFitsFileStr(b)!=null) cleanList.add(PlotStateUtil.getWorkingFitsFile(state,b));
            }
        }
        cleanList.stream().distinct().forEach(FitsCacher::clearCachedHDU);
    }

    private static WebPlotHeaderInitializer makeWpHeaderInit(ImagePlotCreator.PlotInfo pInfo) {
        PlotState s= pInfo.state();
        FitsRead[] frAry= pInfo.fitsReadGroup().getFitsReadAry();
        FileInfo fi= pInfo.fileInfo();
        Map<String,String> attributes= null;
        if (fi!=null) {
            attributes= fi.getAttributeMap();
            attributes.remove(FileInfo.INTERNAL_NAME);
        }
        Header[] zeroHeaderAry= new Header[frAry.length];
        for(int k=0; k<zeroHeaderAry.length; k++) {
            zeroHeaderAry[k]= frAry[k]!=null ? frAry[k].getZeroHeader() : null;
        }
        return new WebPlotHeaderInitializer(s.getOriginalFitsFileStr(NO_BAND),
                s.getWorkingFitsFileStr(NO_BAND), s.getUploadFileName(NO_BAND),
                s.getRangeValues(), pInfo.dataDesc(), s.isMultiImageFile(),
                false, s.getPrimaryRequest(),zeroHeaderAry, attributes);
    }


    private static WebPlotInitializer makeWebPlotInitializer(ImagePlotCreator.PlotInfo pInfo,
                                                             boolean clearRedundantHeaderData) {

        PlotState state = pInfo.state();
        // need a WebFits Data each band: normal is 1, 3 color is three
        WebFitsData[] wfDataAry = new WebFitsData[3];

        for (Map.Entry<Band, WebFitsData> entry : pInfo.webFitsDataMap().entrySet()) {
            wfDataAry[entry.getKey().getIdx()] = entry.getValue();
        }
        FitsRead[] frAry= pInfo.fitsReadGroup().getFitsReadAry();
        Header[] headerAry= new Header[frAry.length];
        Header[] zeroHeaderAry= new Header[frAry.length];
        for(int i=0; i<headerAry.length; i++) {
            headerAry[i]= frAry[i]!=null ? frAry[i].getHeader() : null;
            zeroHeaderAry[i]= frAry[i]!=null ? frAry[i].getZeroHeader() : null;
        }

        FitsRead fr= pInfo.fitsReadGroup().getRefFitsRead();


        int dataWidth= fr.getImageDataWidth();
        int dataHeight= fr.getImageDataHeight();
        CoordinateSys imageCoordSys= fr.getImageCoordinateSystem();


        var desc= PlotServUtils.makePlotDesc(state,pInfo.fitsReadGroup(),pInfo.dataDesc(),state.isMultiImageFile());
        List<RelatedData> rdList= pInfo.relatedData();
        int noBandIdx= NO_BAND.getIdx();
        if (!state.isThreeColor()) {
            if (frAry[noBandIdx].isCube()) {
                int cubePlane = frAry[noBandIdx].getPlaneNumber();
                state.setCubePlaneNumber(cubePlane,NO_BAND);
                if (cubePlane>0) {  // have a cube
                    headerAry = null;
                    rdList = null;
                    dataWidth = -1;
                    dataHeight = -1;
                    imageCoordSys = null;
                    wfDataAry= null;
                    desc= null;
                }
            }
        }

        if (clearRedundantHeaderData) {
            state= state.makeCopy();
            BandState[] bsAry= state.getBandStateAry();
            for(int i=0; i<3; i++) {
                if (bsAry[i]!=null) {
                    bsAry[i].setWebPlotRequest(null);
                    bsAry[i].setUploadedFileName(null);
                    bsAry[i].setWorkingFitsFileStr(null);
                    bsAry[i].setOriginalFitsFileStr(null);
                    bsAry[i].setRangeValues(null);
                    bsAry[i].setMultiImageFile(false);
                }
            }
            zeroHeaderAry= null;
        }

        return new WebPlotInitializer(state,
                                      imageCoordSys,
                                      headerAry,
                                      zeroHeaderAry,
                                      dataWidth, dataHeight,
                                      wfDataAry,
                                      desc,
                                      clearRedundantHeaderData ? null : pInfo.dataDesc(),
                                      rdList);
    }


    private static void logSuccess(PlotState state, long elapse, long findElapse, long readElapse) {
        String threeDesc = state.isThreeColor() ? "three color " : "";
        String majType = threeDesc + "create plot  ";
        String minType = "file   ";
        String time3String = ", Read-";
        long totSize = 0;

        List<String> out = new ArrayList<>(8);
        String more = String.format("%s%9s%s%9s",
                                    ", Find-", UTCTimeUtil.getHMSFromMills(findElapse),
                                    time3String, UTCTimeUtil.getHMSFromMills(readElapse));
        out.add(majType + " - " + minType + ": Total: " + UTCTimeUtil.getHMSFromMills(elapse) + more +
                ", Ctx:"+state.getContextString());

        for (Band band : state.getBands()) {
            String bStr = state.isThreeColor() ? StringUtils.pad(5, band.toString()) + " - " : "";
            File f = PlotStateUtil.getWorkingFitsFile(state, band);
            if (f!=null) {
                String sizeStr= FileUtil.getSizeAsString(f.length());
                out.add(bStr + "filename "+"("+sizeStr+ ")" +": " + f.getPath());
                totSize += f.length();
            }
        }
        out.add("PlotState Summary: " + state.toPrettyString());

        String statDetails = String.format("%6s%s", FileUtil.getSizeAsString(totSize), more);
        _log.info(out.toArray(new String[0]));
        PlotServUtils.statsLog("create", "total-MB", (double) totSize / StringUtils.MEG, "Details", statDetails);
        Counters.getInstance().incrementKB(Counters.Category.Visualization, "Total Read", totSize/ StringUtils.K);
    }

    public record WebPlotFactoryRet(WebPlotInitializer[] wpInit, WebPlotHeaderInitializer wpHeader) { }
}

