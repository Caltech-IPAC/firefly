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
