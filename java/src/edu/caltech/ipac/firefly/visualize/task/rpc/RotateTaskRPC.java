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
                              MiniPlotWidget mpw) {
        new RotateTaskRPC(plot, rotate ? PlotState.RotateType.ANGLE : PlotState.RotateType.UNROTATE,
                      -1, angle, mpw).start();
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
            case ANGLE:    pserv.rotateToAngle(helper.getPlotState(), true, helper.getAngle(), passAlong);
                           break;
            case UNROTATE: pserv.rotateToAngle(helper.getPlotState(), false, Double.NaN, passAlong);
                           break;
        }
    }

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

