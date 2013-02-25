package edu.caltech.ipac.firefly.visualize.task.rpc;
/**
 * User: roby
 * Date: 10/4/11
 * Time: 4:52 PM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
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
        super(null,null, true);
        super.setMaskingDelaySec(1);
        this.resultCB= resultCB;
        this.fileKey= fileKey;
    }

    @Override
    public void onSuccess(WebPlotResult result) {
        if (result.isSuccess()) {
            String data= result.getStringResult(WebPlotResult.REGION_DATA);
            String errors= result.getStringResult(WebPlotResult.REGION_ERRORS);
            resultCB.onSuccess(new RegionData(data,errors));
        }
        else {
            PopupUtil.showError("Server Error", "Could not parse Region File: " + result.getUserFailReason());
        }
    }

    @Override
    public void doTask(AsyncCallback<WebPlotResult> passAlong) {
        PlotServiceAsync pserv= PlotService.App.getInstance();
        pserv.getDS9Region(fileKey,passAlong);
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
