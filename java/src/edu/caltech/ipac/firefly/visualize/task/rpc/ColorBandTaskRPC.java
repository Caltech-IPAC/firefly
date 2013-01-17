package edu.caltech.ipac.firefly.visualize.task.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.task.ColorBandTaskHelper;
/**
 * User: roby
 * Date: Apr 28, 2009
 * Time: 4:25:00 PM
 */


/**
 * @author Trey Roby
*/
public class ColorBandTaskRPC extends ServerTask<WebPlotResult> {


    private final ColorBandTaskHelper helper;

    public static ColorBandTaskRPC addBand(WebPlot plot,
                                           WebPlotRequest request,
                                           Band band,
                                           AsyncCallback<WebPlot> notify,
                                           MiniPlotWidget mpw) {
        ColorBandTaskRPC task= new ColorBandTaskRPC(plot,request, band, notify, mpw, ColorBandTaskHelper.Op.ADD);
        task.start();
        return task;
    }

    public static ColorBandTaskRPC removeBand(WebPlot plot,
                                              Band band,
                                              MiniPlotWidget mpw) {
        ColorBandTaskRPC task= new ColorBandTaskRPC(plot,null, band, null, mpw, ColorBandTaskHelper.Op.REMOVE);
        task.start();
        return task;
    }



    ColorBandTaskRPC(WebPlot plot,
                     WebPlotRequest request,
                     Band band,
                     AsyncCallback<WebPlot> notify,
                     MiniPlotWidget mpw,
                     ColorBandTaskHelper.Op op) {
        super.setMaskingDelaySec(1);
        helper= new ColorBandTaskHelper(plot,request,band,notify,mpw,op);
    }


    @Override
    public void onFailure(Throwable e) { helper.handleFailure(e); }


    @Override
    public void onSuccess(WebPlotResult result) { helper.handleSuccess(result); }


    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        PlotServiceAsync pserv= PlotService.App.getInstance();
        if (helper.getOp()== ColorBandTaskHelper.Op.ADD) {
            pserv.addColorBand(helper.getPlotState(),helper.getRequest(),helper.getBand(),passAlong);
        }
        else {
            pserv.deleteColorBand(helper.getPlotState(),helper.getBand(),passAlong);
        }
    }

    /**
     * change the default cancel behavior so server can clean up
     */
    public void cancel() {
        if (!isFinish()) {
            helper.cancel();
            unMask();
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
