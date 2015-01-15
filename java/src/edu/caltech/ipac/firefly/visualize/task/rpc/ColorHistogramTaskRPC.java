/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task.rpc;
/**
 * User: roby
 * Date: 10/4/11
 * Time: 4:52 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.WebHistogramOps;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;

/**
* @author Trey Roby
*/
public class ColorHistogramTaskRPC extends ServerTask<WebPlotResult> {

    private final WebPlot plot;
    private final WebHistogramOps hOps;
    private final AsyncCallback<WebPlotResult> imageUrlCB;
    private final int width;
    private final int height;

    public static void computeColorHistogram(WebHistogramOps hOps,
                                             WebPlot plot,
                                             int width,
                                             int height,
                                             AsyncCallback<WebPlotResult> imageUrlCB) {
        new ColorHistogramTaskRPC(hOps, plot, width, height, imageUrlCB).start();
    }

    private ColorHistogramTaskRPC(WebHistogramOps hOps,
                                  WebPlot plot,
                                  int width,
                                  int height,
                                  AsyncCallback<WebPlotResult> imageUrlCB) {
        super((Widget)null,null, true);
        this.plot= plot;
        this.hOps= hOps;
        this.width= width;
        this.height= height;
        this.imageUrlCB= imageUrlCB;
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        if (result.isSuccess()) {
            hOps.setDataHistogram(((DataEntry.IntArray)result.getResult(WebPlotResult.DATA_HISTOGRAM)).getArray());
            hOps.setMeanDataAry(((DataEntry.DoubleArray)result.getResult(WebPlotResult.DATA_BIN_MEAN_ARRAY)).getArray());
            imageUrlCB.onSuccess(result);
        }
        else {
            PopupUtil.showError("Server Error", "Could retrieve histogram data: " + result.getUserFailReason());
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        PlotServiceAsync pserv= PlotService.App.getInstance();
        pserv.getColorHistogram(plot.getPlotState(), hOps.getBand(),
                                width, height,
                                passAlong);
    }
}

