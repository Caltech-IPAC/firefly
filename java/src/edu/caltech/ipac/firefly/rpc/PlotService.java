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

/**
 * User: roby
 * Date: Feb 25, 2008
 * Time: 1:22:01 PM
 */
public interface PlotService extends RemoteService {

    public enum FileType {FILE,URL}
    public static final boolean OVERRIDE_NETWORK_MODE= false;

//    public PlotCreationResult[] getWebPlotBatch(WebPlotRequest request[]);

    public WebPlotResult getWebPlot(WebPlotRequest request);

    public WebPlotResult getWebPlot(WebPlotRequest redRequest,
                                    WebPlotRequest greenRequest,
                                    WebPlotRequest blueRequest);


    public String[] getFileFlux(FileAndHeaderInfo fileAndHeader[],ImagePt inIpt) ;
    public WebPlotResult getFlux(PlotState state, ImagePt inIpt);


    public WebPlotResult getTableData(WebPlotRequest state);

    public WebPlotResult addColorBand(PlotState      state,
                                      WebPlotRequest bandRequest,
                                      Band band);

    public WebPlotResult deleteColorBand(PlotState state, Band band);


    public boolean deletePlot(String ctxStr);
    public WebPlotResult setZoomLevel(PlotState state, float level, boolean isFullScreen);
    public WebPlotResult getFitsHeaderInfo(PlotState state);
    public WebPlotResult recomputeStretch(PlotState state,
                                          StretchData[] stretchData);
    public WebPlotResult crop(PlotState state,
                              ImagePt corner1,
                              ImagePt corner2);

    public WebPlotResult rotateNorth(PlotState state, boolean north);
    public WebPlotResult rotateToAngle(PlotState state, boolean rotate, double angle);

    public WebPlotResult flipImageOnY(PlotState state);

    public WebPlotResult changeColor(PlotState state, int colorTableId);

    //DB Code
    public WebPlotResult getAreaStatistics(PlotState state,ImagePt pt1, ImagePt pt2, ImagePt pt3, ImagePt pt4);


    public WebPlotResult getColorHistogram(PlotState state,
                                           Band band,
                                           int  width,
                                           int  height);

    public WebPlotResult getImagePng(PlotState state, ArrayList<StaticDrawInfo> drawInfoList);

    public WebPlotResult getDS9Region(String key);
    public WebPlotResult saveDS9RegionFile(String regionData);

    public WebPlotResult checkPlotProgress(String progressKey);

    public boolean addSavedRequest(String saveKey, WebPlotRequest request);
    public WebPlotResult getAllSavedRequest(String saveKey);



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
