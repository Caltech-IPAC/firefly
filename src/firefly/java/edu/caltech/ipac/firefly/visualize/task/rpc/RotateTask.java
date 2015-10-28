/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
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
public class RotateTask extends ServerTask<WebPlotResult> {

    private final WebPlot _oldPlot;
    private final MiniPlotWidget _mpw;
    private final PlotState.RotateType _rotateType;
    private final double _angle;
    private final float _newZoomLevel;

    public static void rotateNorth(boolean rotateNorth,
                                   float newZoomLevel,
                                   MiniPlotWidget mpw) {
        new RotateTask(mpw.getCurrentPlot(),
                       rotateNorth ? PlotState.RotateType.NORTH : PlotState.RotateType.UNROTATE,
                       newZoomLevel, Double.NaN, mpw).start();
    }


    public static void rotate(WebPlot plot,
                              boolean rotate,
                              double  angle,
                              float newZoomLevel,
                              MiniPlotWidget mpw) {
        new RotateTask(plot, rotate ? PlotState.RotateType.ANGLE : PlotState.RotateType.UNROTATE,
                      newZoomLevel, angle, mpw).start();
    }



    RotateTask(WebPlot plot,
               PlotState.RotateType rotateType,
               float newZoomLevel,
               double angle,
               MiniPlotWidget mpw) {
        super(mpw.getPanelToMask(), makeMessage(rotateType, angle), true);
        _oldPlot= plot;
        _mpw= mpw;
        _angle= angle;
        _newZoomLevel= newZoomLevel;
        _rotateType= rotateType;
    }


    public void onFailure(Throwable e) {
        String extra= "";
        if (e.getCause()!=null) {
            extra= e.getCause().toString();
        }
        Window.alert("Rotate Failed: Server Error: " + extra);
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        _mpw.hideMouseReadout();
        try {
            if (result.isSuccess()) {
                WebPlotResultAry resultEntry= (WebPlotResultAry)result.getResult(WebPlotResult.RESULT_ARY);
                WebPlotResult resultAry[]= resultEntry.getArray();
                CreatorResults cr;
                List<OverlayPlotView> overlayPlotViews= _mpw.getPlotView().getOverlayPlotViewList();
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
                WebPlot rotPlot= new WebPlot(wpInit,false);
                TaskUtils.copyImportantAttributes(_oldPlot, rotPlot);

                WebPlotView pv= _mpw.getPlotView();
                WebPlot oldPlot= pv.getPrimaryPlot();
                int idx= pv.indexOf(oldPlot);
                pv.removePlot(oldPlot,true);

                rotPlot.setPlotDesc(_oldPlot.getPlotDesc());
                pv.addPlot(rotPlot,idx,false);
                pv.setPrimaryPlot(rotPlot);
                _mpw.postPlotTask(rotPlot, null);
                _mpw.forcePlotPrefUpdate();
            }
            else {
                PopupUtil.showError("Rotate", "Could not rotate: " + result.getUserFailReason());
            }
        } catch (Exception e) {
            PopupUtil.showError("Rotate", "Could not rotate: " + e.toString());
            GWT.log("Could not rotate", e);
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        _mpw.prePlotTask();
        PlotServiceAsync pserv= PlotService.App.getInstance();
        switch (_rotateType) {
            case NORTH:    pserv.rotateNorth(getPlotStateAry(_oldPlot, _oldPlot.getPlotView()),true,_newZoomLevel,passAlong);
                           break;
            case ANGLE:    pserv.rotateToAngle(getPlotStateAry(_oldPlot,_oldPlot.getPlotView()), true, _angle,
                                                               _newZoomLevel, passAlong);
                           break;
            case UNROTATE: pserv.rotateToAngle(getPlotStateAry(_oldPlot,_oldPlot.getPlotView()), false,
                                                                Double.NaN, -1, passAlong);
                           break;
        }
    }

    public static String makeMessage(PlotState.RotateType rotateType, double angle) {
        final NumberFormat nf = NumberFormat.getFormat("#.#");
        String retval;
        switch (rotateType) {
            case NORTH:    retval= "Rotating North...";
                break;
            case ANGLE:    retval=  "Rotating to " + nf.format(angle) + " degrees ...";
                break;
            case UNROTATE: retval= "Returning to original rotation...";
                break;
            default:       retval= "undefined";
                break;
        }
        return retval;
    }

    public static PlotState[] getPlotStateAry(WebPlot plot,WebPlotView pv) {
        List<PlotState> stateList= new ArrayList<PlotState>();
        stateList.add(plot.getPlotState());

        List<WebPlot> overlayPlots= pv.getOverlayPlotList();
        if (overlayPlots!=null && overlayPlots.size()>0) {
            for (WebPlot p : overlayPlots) {
                stateList.add(p.getPlotState());
            }
        }
        return stateList.toArray(new PlotState[stateList.size()]);
    }
}
