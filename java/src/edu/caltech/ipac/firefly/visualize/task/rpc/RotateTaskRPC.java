/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.task.RotateTaskHelper;
/**
 * User: roby
 * Date: Apr 28, 2009
 * Time: 4:49:37 PM
 */


/**
 * @author Trey Roby
*/
public class RotateTaskRPC extends ServerTask<WebPlotResult> {

    private final RotateTaskHelper helper;

    public static void rotateNorth(WebPlot plot,
                                   boolean rotateNorth,
                                   float newZoomLevel,
                                   MiniPlotWidget mpw) {
        new RotateTaskRPC(plot, rotateNorth ? PlotState.RotateType.NORTH : PlotState.RotateType.UNROTATE,
                       newZoomLevel, Double.NaN, mpw).start();
    }


    public static void rotate(WebPlot plot,
                              boolean rotate,
                              double  angle,
                              float newZoomLevel,
                              MiniPlotWidget mpw) {
        new RotateTaskRPC(plot, rotate ? PlotState.RotateType.ANGLE : PlotState.RotateType.UNROTATE,
                      newZoomLevel, angle, mpw).start();
    }



    RotateTaskRPC(WebPlot plot,
                  PlotState.RotateType rotateType,
                  float newZoomLevel,
                  double angle,
                  MiniPlotWidget mpw) {
        super(mpw.getPanelToMask(), RotateTaskHelper.makeMessage(rotateType,angle), true);
        helper= new RotateTaskHelper(plot,rotateType,newZoomLevel, angle,mpw);
    }


    public void onFailure(Throwable e) {
        helper.handleFailure(e);
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        helper.handleSuccess(result);
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        helper.getMiniPlotWidget().prePlotTask();
        PlotServiceAsync pserv= PlotService.App.getInstance();
        switch (helper.getType()) {
            case NORTH:    pserv.rotateNorth(helper.getPlotState(),true,helper.getNewZoomLevel(),passAlong);
                           break;
            case ANGLE:    pserv.rotateToAngle(helper.getPlotState(),
                                               true,
                                               helper.getAngle(),
                                               helper.getNewZoomLevel(),
                                               passAlong);
                           break;
            case UNROTATE: pserv.rotateToAngle(helper.getPlotState(), false, Double.NaN, -1, passAlong);
                           break;
        }
    }

}
