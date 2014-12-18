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

