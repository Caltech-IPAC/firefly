/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task.rpc;

import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.BandInfo;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.ui.FitsHeaderDialog;


/**
 * User: balandra
 * Date: Sep 16, 2009
 * Time: 11:19:42 AM
 */
public class FitsHeaderTaskRPC extends ServerTask<WebPlotResult> {

    private final PlotState        state;
    private final MiniPlotWidget   mpw;
    private final FitsHeaderDialog dialog;

    public static void getFitsHeaderInfo(PlotState state,
                                         String message,
                                         MiniPlotWidget mpw,
                                         FitsHeaderDialog dialog) {
        new FitsHeaderTaskRPC(state,message, mpw, dialog).start();
    }

    FitsHeaderTaskRPC(PlotState state,
                      String message,
                      MiniPlotWidget mpw,
                      FitsHeaderDialog dialog) {
        super(mpw.getPanelToMask(), message, true);
        this.state= state;
        this.mpw= mpw;
        this.dialog = dialog;
    }

    public void onFailure(Throwable e) {
        String extra= "";
        if (e.getCause()!=null) {
            extra= e.getCause().toString();

        }
        PopupUtil.showError("Fits Header Failed: Server Error:", extra);
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        mpw.hideMouseReadout();
        try {
            if (result.isSuccess()) {
                BandInfo info = (BandInfo)result.getResult(WebPlotResult.BAND_INFO);
                dialog.createContents(info);

            }
            else {
                PopupUtil.showError("Fits Header Failed:", result.getDetailFailReason());
            }
        } catch (Exception e) {
            PopupUtil.showError("exception:", e.toString());
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        PlotServiceAsync pserv= PlotService.App.getInstance();
        pserv.getFitsHeaderInfo(state, passAlong);

    }
}

