/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task.rpc;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.DataEntry.WebPlotResultAry;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.CreatorResults;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.OverlayPlotView;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotInitializer;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.task.TaskUtils;

import java.util.ArrayList;
import java.util.List;

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

                WebPlotResultAry resultEntry= (WebPlotResultAry)result.getResult(WebPlotResult.RESULT_ARY);
                WebPlotResult resultAry[]= resultEntry.getArray();
                CreatorResults cr;
                List<OverlayPlotView> overlayPlotViews= mpw.getPlotView().getOverlayPlotViewList();
                if (resultAry.length>1 && resultAry.length-1==overlayPlotViews.size()) {
                    for(int i=1; (i<resultAry.length); i++) {
                        if (resultAry[i].isSuccess()) {
                            //================
                            //================
                            cr= (CreatorResults)resultAry[i].getResult(WebPlotResult.PLOT_CREATE);
                            WebPlotInitializer wpInit= cr.getInitializers()[0];
                            WebPlot overlayPlot= new WebPlot(wpInit,true);
                            OverlayPlotView opv= overlayPlotViews.get(i-1);
                            opv.setMaskPlot(overlayPlot);
                        }
                    }
                }
                cr= (CreatorResults)resultAry[0].getResult(WebPlotResult.PLOT_CREATE);
                WebPlotInitializer wpInit= cr.getInitializers()[0];
                WebPlot flipPlot= new WebPlot(wpInit,false);
                TaskUtils.copyImportantAttributes(oldPlot,flipPlot);

                WebPlotView pv= mpw.getPlotView();
                WebPlot oldPlot= pv.getPrimaryPlot();
                int idx= pv.indexOf(oldPlot);
                pv.removePlot(oldPlot,true);

                flipPlot.setPlotDesc(oldPlot.getPlotDesc());
                pv.addPlot(flipPlot,idx,false);
                pv.setPrimaryPlot(flipPlot);
                mpw.postPlotTask(flipPlot, null);
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
        if (mpw != null && mpw.getCurrentPlot() != null) {
            PlotServiceAsync pserv= PlotService.App.getInstance();
            pserv.flipImageOnY(getPlotStateAry(mpw), passAlong);
        }
    }

    public static PlotState[] getPlotStateAry(MiniPlotWidget mpw) {
        List<PlotState> stateList= new ArrayList<PlotState>();
        stateList.add(mpw.getCurrentPlot().getPlotState());

        List<WebPlot> overlayPlots= mpw.getPlotView().getOverlayPlotList();
        if (overlayPlots!=null && overlayPlots.size()>0) {
            for (WebPlot p : overlayPlots) {
                stateList.add(p.getPlotState());
            }
        }
        return stateList.toArray(new PlotState[stateList.size()]);
    }
}
