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
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;

/**
* @author Trey Roby
*/
public class LoadDS9RegionTask extends ServerTask<WebPlotResult> {

    private final AsyncCallback<RegionData> resultCB;
    private final String fileKey;

    public static void loadDS9Region(String fileKey,
                                     AsyncCallback<RegionData> resultCB )  {
        new LoadDS9RegionTask(fileKey, resultCB).start();
    }

    private LoadDS9RegionTask(String fileKey,
                              AsyncCallback<RegionData> resultCB) {
        super((Widget)null,null, true);
        super.setMaskingDelaySec(1);
        this.resultCB= resultCB;
        this.fileKey= fileKey;
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        if (result.isSuccess()) {
            String data= result.getStringResult(WebPlotResult.REGION_DATA);
            String errors= result.getStringResult(WebPlotResult.REGION_ERRORS);
            String title= result.getStringResult(WebPlotResult.TITLE);
            resultCB.onSuccess(new RegionData(title,data,errors));
        }
        else {
            String details= result.getUserFailReason()!=null ? ": "+ result.getUserFailReason() : "";
            PopupUtil.showError("Server Error", "Could not parse Region file" + details);
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        PlotServiceAsync pserv= PlotService.App.getInstance();
        pserv.getDS9Region(fileKey,passAlong);
    }
}

