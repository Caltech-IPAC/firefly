/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.task;
/**
 * User: roby
 * Date: 2/7/12
 * Time: 9:19 AM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.data.DataEntry;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.FileAndHeaderInfo;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.VisUtil;
import edu.caltech.ipac.firefly.visualize.WebHistogramOps;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotGroup;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.task.rpc.AreaStatisticsTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.ColorBandTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.ColorHistogramTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.ColorTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.FitsHeaderTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.FlipTask;
import edu.caltech.ipac.firefly.visualize.task.rpc.LoadDS9RegionTask;
import edu.caltech.ipac.firefly.visualize.task.rpc.RegionData;
import edu.caltech.ipac.firefly.visualize.task.rpc.RotateTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.StretchTaskRPC;
import edu.caltech.ipac.firefly.visualize.ui.FitsHeaderDialog;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.ImagePt;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Trey Roby
 */
public class VisTask {

    private static VisTask instance= null;

    private final PlotServiceAsync pserv;


    public static VisTask getInstance() {
        if (instance==null)  instance= new VisTask();
        return instance;
    }

    public VisTask() {
        pserv= PlotService.App.getInstance();
    }

    public PlotFileTask plot(WebPlotRequest request1,
                             WebPlotRequest request2,
                             WebPlotRequest request3,
                             boolean threeColor,
                             String message,
                             boolean removeOldPlot,
                             boolean addToHistory,
                             AsyncCallback<WebPlot> notify,
                             MiniPlotWidget mpw) {
        return PlotFileTask.plot(request1, request2, request3, threeColor,
                                 message, removeOldPlot, addToHistory, notify, mpw);
    }
    public void getFlux(FileAndHeaderInfo fileAndHeader[],
                        ImagePt inIpt,
                        final AsyncCallback<String[]> async) {
        pserv.getFileFlux(fileAndHeader,  inIpt, async);
    }


    public ZoomTask zoom(WebPlotGroup group, float zlevel, boolean isFullScreen) {
        return ZoomTask.zoom(group, zlevel, isFullScreen);
    }


    public void stretch(WebPlot plot, StretchData[] stretchData) {
        StretchTaskRPC.stretch(plot, stretchData);
    }

    public void addColorBand(WebPlot plot, WebPlotRequest request, Band band, AsyncCallback<WebPlot> notify, MiniPlotWidget mpw) {
        ColorBandTaskRPC.addBand(plot, request, band, notify, mpw);
    }

    public void removeColorBand(WebPlot plot, Band band, MiniPlotWidget mpw) {
        ColorBandTaskRPC.removeBand(plot, band, mpw);
    }

    public void changeColor(WebPlot plot, int colorIdx) {
        ColorTaskRPC.changeColor(plot, colorIdx);
    }

    public void crop(MiniPlotWidget mpw,
                     ImagePt pt1,
                     ImagePt pt2,
                     boolean cropMultiAll) {

        String desc= mpw.getCurrentPlot().getPlotDesc();
        String t= StringUtils.isEmpty(desc) ? mpw.getTitle() : desc;
        if (cropMultiAll) t= mpw.getTitle();
        String newTitle= t.startsWith(CropTask.CROPPED) ? t : CropTask.CROPPED + t;
        CropTask.crop(mpw, "Cropping...", newTitle, pt1, pt2, cropMultiAll);
    }

    public void rotateNorth(WebPlot plot, boolean rotateNorth, float newZoomLevel, MiniPlotWidget mpw) {
        RotateTaskRPC.rotateNorth(plot,rotateNorth,newZoomLevel, mpw);
    }

    public void rotate(WebPlot plot, boolean rotate, double angle, float newZoomLevel, MiniPlotWidget mpw) {
        RotateTaskRPC.rotate(plot, rotate, angle, newZoomLevel, mpw);
    }

    public void flipY(MiniPlotWidget mpw) {
        FlipTask.flipY(mpw);
    }

    public void computeColorHistogram(WebHistogramOps hOps, WebPlot plot, int width, int height, AsyncCallback<WebPlotResult> imageUrlCB) {
        ColorHistogramTaskRPC.computeColorHistogram(hOps, plot, width, height, imageUrlCB);
    }

    public void getFitsHeaderInfo(PlotState state, String message, MiniPlotWidget mpw, FitsHeaderDialog dialog) {
        FitsHeaderTaskRPC.getFitsHeaderInfo(state, message, mpw, dialog);
    }

    public void deletePlot(WebPlot plot) {
        deletePlot(plot.getPlotState().getContextString());
    }

    public void getDS9Region(String key, AsyncCallback<RegionData> async) {
        LoadDS9RegionTask.loadDS9Region(key,async);
    }



    public void deletePlot(String ctxStr) {
        pserv.deletePlot(ctxStr,
                         new AsyncCallback<Boolean>() {
                             public void onSuccess(Boolean result) { }
                             public void onFailure(Throwable caught) { }
                         });
    }

    public void addSavedRequest(String saveKey, WebPlotRequest request) {
        pserv.addSavedRequest(saveKey, request,
                              new AsyncCallback<Boolean>() {
                                  public void onSuccess(Boolean result) { }
                                  public void onFailure(Throwable caught) { }
                              });
    }

    public void getAllSavedRequest(String saveKey, final AsyncCallback<List<WebPlotRequest>> async) {
        pserv.getAllSavedRequest(saveKey,
                                 new AsyncCallback<WebPlotResult>() {
                                     public void onSuccess(WebPlotResult result) {
                                         DataEntry.StringArray sAry= (DataEntry.StringArray)result.getResult(WebPlotResult.REQUEST_LIST);
                                         String[] resultAry= sAry.getArray();
                                         List<WebPlotRequest> reqList= new ArrayList<WebPlotRequest>(resultAry.length);
                                         WebPlotRequest request;
                                         for(String s : resultAry) {
                                             request= WebPlotRequest.parse(s);
                                             if (request!=null) {
                                                 reqList.add(request);
                                             }
                                         }
                                         async.onSuccess(reqList);
                                     }

                                     public void onFailure(Throwable caught) {
                                         async.onFailure(caught);
                                     }
                                 });
    }

    public void getAreaStatistics(PlotState request,
                                  String message,
                                  ImagePt pt1,
                                  ImagePt pt2,
                                  ImagePt pt3,
                                  ImagePt pt4,
                                  MiniPlotWidget mpw) {
        AreaStatisticsTaskRPC.getAreaStatistics(request, message, pt1, pt2, pt3, pt4, mpw);
    }

    public void checkPlotProgress(String key,AsyncCallback<WebPlotResult> asyncCallback) {
        pserv.checkPlotProgress(key,asyncCallback);
    }
}

