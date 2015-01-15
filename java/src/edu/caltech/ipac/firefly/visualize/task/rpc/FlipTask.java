/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task.rpc;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.task.TaskUtils;
/**
 * User: roby
 * Date: Apr 28, 2009
 * Time: 4:49:37 PM
 */


/**
 * @author Trey Roby
*/
public class FlipTask extends ServerTask<WebPlotResult> {

    private final MiniPlotWidget mpw;
    private final WebPlot oldPlot;

    public static void flipY(MiniPlotWidget mpw) {
        new FlipTask(mpw).start();
    }


    public FlipTask(MiniPlotWidget mpw) {
        super(mpw.getPanelToMask(), "Flipping on Y Axis", true);
        this.mpw= mpw;
        this.oldPlot= mpw.getCurrentPlot();
    }


    public void onFailure(Throwable e) {
        String extra= "";
        if (e.getCause()!=null) {
            extra= e.getCause().toString();

        }
        Window.alert("Flip Failed: Server Error: " + extra);
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        mpw.hideMouseReadout();
        try {
            if (result.isSuccess()) {
                CreatorResults cr= (CreatorResults)result.getResult(WebPlotResult.PLOT_CREATE);
                WebPlotInitializer wpInit= cr.getInitializers()[0];
                WebPlot rotPlot= new WebPlot(wpInit);
                TaskUtils.copyImportantAttributes(oldPlot,rotPlot);

                WebPlotView pv= mpw.getPlotView();
                WebPlot oldPlot= pv.getPrimaryPlot();
                int idx= pv.indexOf(oldPlot);
                pv.removePlot(oldPlot,true);

                rotPlot.setPlotDesc(oldPlot.getPlotDesc());
                pv.addPlot(rotPlot,idx,false);
                pv.setPrimaryPlot(rotPlot);
                mpw.postPlotTask(rotPlot, null);
                mpw.forcePlotPrefUpdate();
            }
            else {
                PopupUtil.showError("Flip ", "Could not Flip: " + result.getUserFailReason());
            }
        } catch (Exception e) {
            PopupUtil.showError("Flip", "Could not Flip: " + e.toString());
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        PlotServiceAsync pserv= PlotService.App.getInstance();
        if (mpw != null && mpw.getCurrentPlot() != null) {
            pserv.flipImageOnY(mpw.getCurrentPlot().getPlotState(), passAlong);
        }
    }
}
