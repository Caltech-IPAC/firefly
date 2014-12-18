package edu.caltech.ipac.firefly.visualize.task.rpc;
/**
 * User: roby
 * Date: 10/4/11
 * Time: 4:59 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;

/**
* @author Trey Roby
*/
public class ColorTaskRPC extends ServerTask<WebPlotResult> {

    private final WebPlot plot;
    private final int colorIdx;

    public static void changeColor(WebPlot plot, int colorIdx) {
        new ColorTaskRPC(plot,colorIdx).start();
    }

    public ColorTaskRPC(WebPlot plot, int colorIdx) {
        super(plot.getPlotView().getMaskWidget(), "changing color...", true);
        super.setMaskingDelaySec(1);
        this.plot= plot;
        this.colorIdx= colorIdx;
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        if (result.isSuccess()) {
            plot.setPlotState((PlotState)result.getResult(WebPlotResult.PLOT_STATE));
            plot.refreshWidget((PlotImages)result.getResult(WebPlotResult.PLOT_IMAGES));
            plot.getPlotView().getMiniPlotWidget().forcePlotPrefUpdate();
        }
        else {
            PopupUtil.showError("Server Error", "Could not change color: " + result.getUserFailReason());
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        PlotServiceAsync pserv= PlotService.App.getInstance();
        pserv.changeColor(plot.getPlotState(), colorIdx, passAlong);
    }
}

