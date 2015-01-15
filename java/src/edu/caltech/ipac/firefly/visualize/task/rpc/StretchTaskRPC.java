/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task.rpc;
/**
 * User: roby
 * Date: 10/4/11
 * Time: 5:00 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;

/**
* @author Trey Roby
*/
public class StretchTaskRPC extends ServerTask<WebPlotResult> {

    private WebPlot plot;
    private StretchData[] stretchData;

    public static void stretch(WebPlot plot, StretchData[] stretchData) {
        new StretchTaskRPC(plot,stretchData).start();
    }

    public StretchTaskRPC(WebPlot plot, StretchData[] stretchData) {
        super(plot.getPlotView().getMaskWidget(), "stretching...", true);
        super.setMaskingDelaySec(1);
        this.stretchData= stretchData;
        this.plot= plot;
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        if (result.isSuccess()) {
            plot.setPlotState((PlotState)result.getResult(WebPlotResult.PLOT_STATE));
            plot.refreshWidget((PlotImages)result.getResult(WebPlotResult.PLOT_IMAGES));
            plot.getPlotView().getMiniPlotWidget().forcePlotPrefUpdate();
        }
        else {
            PopupUtil.showError("Server Error", "Changing Stretch Failed: " + result.getUserFailReason());
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        PlotServiceAsync pserv= PlotService.App.getInstance();
        pserv.recomputeStretch(plot.getPlotState(), stretchData, passAlong);
    }
}

