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
