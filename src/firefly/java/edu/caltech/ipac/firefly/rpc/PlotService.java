/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.rpc;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.NetworkMode;
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.FileAndHeaderInfo;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.draw.StaticDrawInfo;
import edu.caltech.ipac.visualize.plot.ImagePt;

import java.util.ArrayList;
import java.util.List;

/**
 * User: roby
 * Date: Feb 25, 2008
 * Time: 1:22:01 PM
 */
public interface PlotService extends RemoteService {

    enum FileType {FILE,URL}
    static final boolean OVERRIDE_NETWORK_MODE= false;

//    PlotCreationResult[] getWebPlotBatch(WebPlotRequest request[]);

    WebPlotResult getWebPlot(WebPlotRequest request);

    WebPlotResult[] getWebPlotGroup(List<WebPlotRequest> requestList, String progressKey);

    WebPlotResult[] getOneFileGroup(List<WebPlotRequest> requestList, String progressKey);

    WebPlotResult getWebPlot(WebPlotRequest redRequest,
                                    WebPlotRequest greenRequest,
                                    WebPlotRequest blueRequest);


    String[] getFileFlux(FileAndHeaderInfo fileAndHeader[],ImagePt inIpt) ;
    WebPlotResult getFlux(PlotState state, ImagePt inIpt);


    WebPlotResult getTableData(WebPlotRequest state);

    WebPlotResult addColorBand(PlotState      state,
                                      WebPlotRequest bandRequest,
                                      Band band);

    WebPlotResult deleteColorBand(PlotState state, Band band);


    boolean deletePlot(String ctxStr);
    WebPlotResult setZoomLevel(PlotState state[], float level, boolean isFullScreen);
    WebPlotResult getFitsHeaderInfo(PlotState state);
    WebPlotResult recomputeStretch(PlotState state, StretchData[] stretchData);

    WebPlotResult crop(PlotState stateAry[],
                       ImagePt corner1,
                       ImagePt corner2,
                       boolean cropMultiAll);

    WebPlotResult rotateNorth(PlotState state[], boolean north, float newZoomLevel);
    WebPlotResult rotateToAngle(PlotState state[], boolean rotate, double angle, float newZoomLevel);

    WebPlotResult flipImageOnY(PlotState state[]);

    WebPlotResult changeColor(PlotState state, int colorTableId);

    //DB Code
    WebPlotResult getAreaStatistics(PlotState state,ImagePt pt1, ImagePt pt2, ImagePt pt3, ImagePt pt4);


    WebPlotResult getColorHistogram(PlotState state,
                                           Band band,
                                           int  width,
                                           int  height);

    WebPlotResult getImagePng(PlotState state, ArrayList<StaticDrawInfo> drawInfoList);

    WebPlotResult getDS9Region(String key);
    WebPlotResult saveDS9RegionFile(String regionData);

    WebPlotResult checkPlotProgress(String progressKey);

    boolean addSavedRequest(String saveKey, WebPlotRequest request);
    WebPlotResult getAllSavedRequest(String saveKey);



    /**
     * Utility/Convenience class.
     * Use PlotService.App.getInstance() to access static instance of PlotServiceAsync
     */
    public static class App extends ServiceLocator<PlotServiceAsync> {
        private static final App locator = new App();

        private App() {
            super("sticky/FireFly_PlotService");
        }

        protected PlotServiceAsync createService() {
            PlotServiceAsync retval= null;
            NetworkMode mode= Application.getInstance().getNetworkMode();
            if (OVERRIDE_NETWORK_MODE) mode= NetworkMode.WORKER;
            switch (mode) {
                case RPC:    retval= (PlotServiceAsync) GWT.create(PlotService.class); break;
                case WORKER: retval= new PlotServiceJson(false); break;
                case JSONP:  retval= new PlotServiceJson(true); break;
                default : retval= null; break;
            }
            WebAssert.argTst(retval!=null, "PlotServiceAsync.createService: retval==null, This should not happen");
            return retval;
        }

        public static PlotServiceAsync getInstance() {
            return locator.getService();
        }
    }
    
}
