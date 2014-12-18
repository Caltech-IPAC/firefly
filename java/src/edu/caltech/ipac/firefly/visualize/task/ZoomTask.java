package edu.caltech.ipac.firefly.visualize.task;

/**
 * User: roby
 * Date: Sep 23, 2009
 * Time: 11:39:34 AM
 */


import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotGroup;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;

/**
 * @author Trey Roby
 */
public class ZoomTask extends ServerTask<WebPlotResult> {
    private final float zLevel;
    private final WebPlotGroup group;
    private final boolean isFullScreen;


    public static ZoomTask zoom(WebPlotGroup group, float zLevel, boolean isFullScreen) {
        ZoomTask zt= new ZoomTask(group,zLevel, isFullScreen);
        zt.start();
        return zt;
    }

    private ZoomTask(WebPlotGroup group, float zLevel, boolean isFullScreen) {
        super((Widget)null, "zooming...", false);
        this.group= group;
        this.zLevel = zLevel;
        this.isFullScreen= isFullScreen;
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        if (result.isSuccess()) {
            group.getBasePlot().setPlotState((PlotState)result.getResult(WebPlotResult.PLOT_STATE));
            PlotImages images= (PlotImages)result.getResult(WebPlotResult.PLOT_IMAGES);
            group.postZoom(images);
            if (group.getPlotView()!=null) {
                group.getPlotView().getMiniPlotWidget().forcePlotPrefUpdate();
            }
        }
        else {
            Window.alert("Could not zoom: " + result.getUserFailReason());
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        PlotService.App.getInstance().setZoomLevel(getPlotState(), zLevel, isFullScreen, passAlong);
    }

    public PlotState getPlotState() { return group.getBasePlot().getPlotState(); }
}

