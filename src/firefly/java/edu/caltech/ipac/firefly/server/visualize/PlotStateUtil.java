/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.server.visualize;

import edu.caltech.ipac.firefly.server.ServerContext;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;

import java.io.File;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class PlotStateUtil {


    public static PlotState create(WebPlotRequest req) {
        PlotState state= new PlotState();
        state.setWebPlotRequest(req, Band.NO_BAND);
        return state;
    }

    public static PlotState create(Map<Band,WebPlotRequest> reqMap) {
        PlotState state= new PlotState();
        state.setThreeColor(true);
        WebPlotRequest redReq= reqMap.get(Band.RED);
        WebPlotRequest greenReq= reqMap.get(Band.GREEN);
        WebPlotRequest blueReq= reqMap.get(Band.BLUE);
        if (redReq!=null) state.setWebPlotRequest(redReq, Band.RED);
        if (greenReq!=null) state.setWebPlotRequest(greenReq, Band.GREEN);
        if (blueReq!=null) state.setWebPlotRequest(blueReq, Band.BLUE);
        return state;
    }

    public static void initRequestFromState(WebPlotRequest req, PlotState oldState, Band band) {
        req.setInitialRangeValues(oldState.getRangeValues(band));
        WebPlotRequest oldReq= oldState.getPrimaryRequest();
        if (oldReq.isPlotAsMask()) {
            req.setPlotAsMask(true);
            req.setMaskBits(req.getMaskBits());
            req.setMaskColors(req.getMaskColors().toArray(new String[3]));
        }
    }

    public static File getWorkingFitsFile(PlotState state) { return getWorkingFitsFile(state, Band.NO_BAND); }

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
