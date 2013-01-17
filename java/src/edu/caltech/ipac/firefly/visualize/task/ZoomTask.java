package edu.caltech.ipac.firefly.visualize.task;

/**
 * User: roby
 * Date: Sep 23, 2009
 * Time: 11:39:34 AM
 */


import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
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
        super(null, "zooming...", false);
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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
