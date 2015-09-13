/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task;

/**
 * User: roby
 * Date: Sep 23, 2009
 * Time: 11:39:34 AM
 */


import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotGroup;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Trey Roby
 */
public class ZoomTask extends ServerTask<WebPlotResult> {
    private final float zLevel;
    private final WebPlotGroup group;
    private final boolean isFullScreen;
    private List<WebPlot> overlayPlots;


    public static ZoomTask zoom(WebPlotGroup group, float zLevel, boolean isFullScreen, List<WebPlot> overlayPlots) {
        ZoomTask zt= new ZoomTask(group,zLevel, isFullScreen, overlayPlots);
        zt.start();
        return zt;
    }

    private ZoomTask(WebPlotGroup group, float zLevel, boolean isFullScreen, List<WebPlot> overlayPlots ) {
        super((Widget)null, "zooming...", false);
        this.group= group;
        this.zLevel = zLevel;
        this.isFullScreen= isFullScreen;
        this.overlayPlots= overlayPlots;
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        PlotImages images;
        if (result.isSuccess()) {
            DataEntry.WebPlotResultAry resultEntry= (DataEntry.WebPlotResultAry)result.getResult(WebPlotResult.RESULT_ARY);
            WebPlotResult resultAry[]= resultEntry.getArray();
            if (resultAry[0].isSuccess()) {
                group.getBasePlot().setPlotState((PlotState)resultAry[0].getResult(WebPlotResult.PLOT_STATE));
                images= (PlotImages)resultAry[0].getResult(WebPlotResult.PLOT_IMAGES);
                group.postZoom(images);
                if (group.getPlotView()!=null) {
                    group.getPlotView().getMiniPlotWidget().forcePlotPrefUpdate();
                }
                if (resultAry.length>1 && resultAry.length-1==overlayPlots.size()) {
                    for(int i=1; (i<resultAry.length); i++) {
                        if (resultAry[i].isSuccess()) {
                            WebPlot p= overlayPlots.get(i-1);
                            p.setPlotState((PlotState)resultAry[i].getResult(WebPlotResult.PLOT_STATE));
                            images= (PlotImages)resultAry[i].getResult(WebPlotResult.PLOT_IMAGES);
                            p.refreshWidget(images);
                        }
                    }
                }
            }
            else {
                Window.alert("Could not zoom: " + resultAry[0].getUserFailReason());
            }
        }
        else {
            Window.alert("Could not zoom: " + result.getUserFailReason());
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        PlotService.App.getInstance().setZoomLevel(getPlotStateAry(), zLevel, isFullScreen, passAlong);
    }

    public PlotState[] getPlotStateAry() {
        List<PlotState> stateList= new ArrayList<PlotState>();
        stateList.add(group.getBasePlot().getPlotState());
        if (overlayPlots!=null && overlayPlots.size()>0) {
            for (WebPlot p : overlayPlots) {
                stateList.add(p.getPlotState());
            }
        }
        return stateList.toArray(new PlotState[stateList.size()]);
    }
}

