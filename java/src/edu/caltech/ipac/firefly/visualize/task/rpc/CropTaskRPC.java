package edu.caltech.ipac.firefly.visualize.task.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.task.CropTaskHelper;
import edu.caltech.ipac.visualize.plot.ImagePt;
/**
 * User: roby
 * Date: Apr 28, 2009
 * Time: 4:49:37 PM
 */


/**
 * @author Trey Roby
*/
public class CropTaskRPC extends ServerTask<WebPlotResult> {
    private final CropTaskHelper _helper;

    public static void crop(WebPlot plot,
                            String message,
                            String newTitle,
                            ImagePt pt1,
                            ImagePt pt2,
                            MiniPlotWidget mpw) {
        new CropTaskRPC(plot,message, newTitle, pt1, pt2, mpw).start();
    }

    CropTaskRPC(WebPlot plot,
                String message,
                String newTitle,
                ImagePt pt1,
                ImagePt pt2,
                MiniPlotWidget mpw) {
        super(mpw.getLayoutPanel(), message, true);
        super.setMaskingDelaySec(1);
        _helper= new CropTaskHelper(plot,newTitle,pt1,pt2,mpw);
    }

    public void onFailure(Throwable e) {
        _helper.handleFailure(e);
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        _helper.handleSuccess(result);
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        _helper.getMiniPlotWidget().prePlotTask();
        PlotServiceAsync pserv= PlotService.App.getInstance();
        pserv.crop(_helper.getPlotState(),_helper.getPt1(),_helper.getPt2(),passAlong);
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
