/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.server.visualize;
/**
 * User: roby
 * Date: 5/4/15
 * Time: 3:40 PM
 */


import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.FitsRead;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ImagePlot;

import java.io.File;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class PlotStateUtil {


    public static PlotState create(WebPlotRequest req) {
        PlotState state= new PlotState();
        state.setWebPlotRequest(req, Band.NO_BAND);
        state.setMultiImageAction(PlotState.MultiImageAction.USE_ALL);
        return state;
    }

    public static PlotState create(Map<Band,WebPlotRequest> reqMap) {

        PlotState state= new PlotState();
        state.setThreeColor(true);
        state.setMultiImageAction(PlotState.MultiImageAction.USE_FIRST);

        WebPlotRequest redReq= reqMap.get(Band.RED);
        WebPlotRequest blueReq= reqMap.get(Band.GREEN);
        WebPlotRequest greenReq= reqMap.get(Band.BLUE);
        if (redReq!=null) state.setWebPlotRequest(redReq, Band.RED);
        if (greenReq!=null) state.setWebPlotRequest(greenReq, Band.GREEN);
        if (blueReq!=null) state.setWebPlotRequest(blueReq, Band.BLUE);

        return state;
    }

    public static PlotState create(WebPlotRequest req[], PlotState initializerState) {
//        PlotState state= new PlotState(initializerState.isThreeColor());

        PlotState state= new PlotState();
        boolean threeC= initializerState.isThreeColor();
        state.setThreeColor(threeC);
        state.setMultiImageAction(threeC ? PlotState.MultiImageAction.USE_FIRST : PlotState.MultiImageAction.USE_ALL);

        state.setContextString(CtxControl.makeAndCachePlotCtx().getKey());
        initState(state, req, initializerState);
        CtxControl.getPlotCtx(state.getContextString()).setPlotState(state);
        for(Band band : initializerState.getBands()) {
            state.setOriginalFitsFileStr(initializerState.getOriginalFitsFileStr(band), band);
        }
        return state;
    }

    public static void initRequestFromState(WebPlotRequest req, PlotState oldState, Band band) {
        req.setInitialRangeValues(oldState.getRangeValues(band));
        req.setInitialColorTable(oldState.getColorTableId());
        req.setInitialZoomLevel(oldState.getZoomLevel());
    }


    private static void initState(PlotState state, WebPlotRequest req[], PlotState initializerState) {
        Band bands[]= initializerState.getBands();
        Assert.argTst(req.length == bands.length,
                      "there must be the same number of WebPlotRequest as there are bands in the initializerState");
        for(int i= 0; (i<bands.length); i++) {
            state.setWebPlotRequest(req[i],bands[i]);
            state.setRangeValues(initializerState.getRangeValues(bands[i]), bands[i]);
        }
        state.setNewPlot(false);
        state.setColorTableId(initializerState.getColorTableId());
        state.setZoomLevel(initializerState.getZoomLevel());
    }

    static void setPixelAccessInfo(ImagePlot plot, PlotState state, ActiveFitsReadGroup frGroup) {
        if (plot.isThreeColor()) {
            if (plot.isColorBandInUse(Band.RED, frGroup)) {
                setPixelAccessInfoBand(plot,frGroup,state, Band.RED);
            }
            if (plot.isColorBandInUse(Band.GREEN, frGroup)) {
                setPixelAccessInfoBand(plot,frGroup,state, Band.GREEN);
            }
            if (plot.isColorBandInUse(Band.BLUE, frGroup)) {
                setPixelAccessInfoBand(plot,frGroup,state, Band.BLUE);
            }
        }
        else {
            setPixelAccessInfoBand(plot,frGroup,state, Band.NO_BAND);
        }
    }

    static void setPixelAccessInfoBand(ImagePlot plot, ActiveFitsReadGroup frGroup, PlotState state, Band band) {
        FitsRead resultFr= plot.getHistogramOps(band,frGroup).getFitsRead();
        ImageHeader ih= resultFr.getImageHeader();
        state.setFitsHeader(ih.makeMiniHeader(), band);
    }

    public static File getWorkingFitsFile(PlotState state, Band band) {
        return ServerContext.convertToFile(state.getWorkingFitsFileStr(band));
    }

    public static void setWorkingFitsFile(PlotState state, File f, Band band) {
        state.setWorkingFitsFileStr(ServerContext.replaceWithPrefix(f), band);
    }

    public static File getOriginalFile(PlotState state, Band band) {
        return ServerContext.convertToFile(state.getOriginalFitsFileStr(band));
    }

    public static void setOriginalFitsFile(PlotState state, File f, Band band) {
        state.setOriginalFitsFileStr(ServerContext.replaceWithPrefix(f), band);
    }
}
