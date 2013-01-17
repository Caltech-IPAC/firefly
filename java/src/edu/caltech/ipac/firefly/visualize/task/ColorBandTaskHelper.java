package edu.caltech.ipac.firefly.visualize.task;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.InsertBandInitializer;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotImages;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
/**
 * User: roby
 * Date: Apr 28, 2009
 * Time: 4:25:00 PM
 */


/**
 * @author Trey Roby
*/
public class ColorBandTaskHelper {

    public enum Op {ADD, REMOVE}


    private final WebPlotRequest _request;
    private final Band _band;
    private final MiniPlotWidget _mpw;
    private final AsyncCallback<WebPlot> _notify;
    private final WebPlot _plot;
    private final Op _op;
    private boolean _continueOnSuccess= true;



    public ColorBandTaskHelper(WebPlot plot,
                               WebPlotRequest request,
                               Band band,
                               AsyncCallback<WebPlot> notify,
                               MiniPlotWidget mpw,
                               Op op) {
        _request= request;
        _band= band;
        _notify= notify;
        _mpw= mpw;
        _plot= plot;
        _op= op;
    }

    public Op getOp() { return _op; }
    public PlotState getPlotState() { return _plot.getPlotState(); }
    public Band getBand() { return _band; }
    public WebPlotRequest getRequest() { return _request; }

    public void handleFailure(Throwable e) {
        String extra= "";
        if (e.getCause()!=null) {
            extra= e.getCause().toString();

        }
        _mpw.processError(null,"Server Error", "Plot Failed: Server Error: "+  extra,null);
        if (_notify!=null) _notify.onFailure(null);
    }


    public void handleSuccess(WebPlotResult result) {
        try {
            if (result.isSuccess()) {
                if (_op== Op.ADD) addSuccess(result);
                else             removeSuccess(result);
                _mpw.forcePlotPrefUpdate();
            }
            else {
                showFailure(result);
            }
        } catch (Exception e) {
            _mpw.processError(_plot,result.getBriefFailReason(), "WebPlot exception: "+e, e);
            GWT.log("WebPlot exception: " + e, e);
        }
    }


    private void addSuccess(WebPlotResult result) {
        InsertBandInitializer init= (InsertBandInitializer)result.getResult(WebPlotResult.INSERT_BAND_INIT);
        if (_continueOnSuccess) {
            _plot.setPlotState(init.getPlotState());
            _plot.refreshWidget(init.getImages());
            _plot.setFitsData(init.getFitsData(), init.getBand());
            if (_notify!=null) _notify.onSuccess(_plot);
        }
    }


    private void removeSuccess(WebPlotResult result) {
        _plot.setPlotState((PlotState) result.getResult(WebPlotResult.PLOT_STATE));
        _plot.refreshWidget((PlotImages) result.getResult(WebPlotResult.PLOT_IMAGES));
    }

    private void showFailure(WebPlotResult result) {
        String title= _request.getTitle();
        _mpw.processError(null,result.getBriefFailReason(),
                          title + " Color band change Failed- "+ result.getUserFailReason(),null);
        if (_notify!=null) _notify.onFailure(null);
    }

    public void cancel() {
        _continueOnSuccess= false;
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
