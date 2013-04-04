package edu.caltech.ipac.firefly.visualize.task.rpc;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.BandInfo;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.ui.StatisticsDialog;
import edu.caltech.ipac.visualize.plot.ImagePt;

/**.
 * User: balandra
 * Date: Aug 27, 2009
 * Time: 10:20:24 AM
 */
public class AreaStatisticsTaskRPC extends ServerTask<WebPlotResult>{

    private final PlotState _request;
    private final MiniPlotWidget _mpw;
    private final ImagePt _pt1;
    private final ImagePt _pt2;
    private final ImagePt _pt3;
    private final ImagePt _pt4;

    public static void getAreaStatistics(PlotState request,
                            String message,
                            ImagePt pt1,
                            ImagePt pt2,
                            ImagePt pt3,
                            ImagePt pt4,
                            MiniPlotWidget mpw) {
        new AreaStatisticsTaskRPC(request,message, pt1, pt2, pt3, pt4, mpw).start();
    }

    AreaStatisticsTaskRPC(PlotState request,
                          String message,
                          ImagePt pt1,
                          ImagePt pt2,
                          ImagePt pt3,
                          ImagePt pt4,
                          MiniPlotWidget mpw) {
        super(mpw.getPanelToMask(), message, true);
        _request= request;
        _mpw= mpw;
        _pt1= pt1;
        _pt2= pt2;
        _pt3= pt3;
        _pt4= pt4;
    }

    public void onFailure(Throwable e) {
        String extra= "";
        if (e.getCause()!=null) {
            extra= e.getCause().toString();

        }
        PopupUtil.showError("Area Statistics Failed: Server Error:", extra);

    }

    @Override
    public void onSuccess(WebPlotResult result) {
        _mpw.hideMouseReadout();
        try {
            if (result.isSuccess()) {
                BandInfo info= (BandInfo)result.getResult(WebPlotResult.BAND_INFO);
                StatisticsDialog.showStats(_mpw, info);
            }
            else {
                PopupUtil.showError("Area Statistics Failed:", result.getDetailFailReason());
            }
        } catch (JavaScriptException jse){
            PopupUtil.showError("Invalid Area Selection:", "Please select an area within the FITS Image.");
        } catch (NullPointerException e) {
            PopupUtil.showError("Invalid Area Selection:", "Please select an area within the FITS Image.");
        } catch (Exception e) {
            PopupUtil.showError("Exception:", e.toString());
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {

        PlotServiceAsync pserv= PlotService.App.getInstance();
        pserv.getAreaStatistics(_request,_pt1, _pt2,_pt3, _pt4, passAlong);

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
