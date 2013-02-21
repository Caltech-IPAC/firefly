package edu.caltech.ipac.firefly.visualize.task;
/**
 * User: roby
 * Date: 2/7/12
 * Time: 9:19 AM
 */


import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.rpc.PlotService;
import edu.caltech.ipac.firefly.rpc.PlotServiceAsync;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.FileAndHeaderInfo;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotState;
import edu.caltech.ipac.firefly.visualize.StretchData;
import edu.caltech.ipac.firefly.visualize.WebHistogramOps;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotGroup;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotResult;
import edu.caltech.ipac.firefly.visualize.task.rpc.AreaStatisticsTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.ColorBandTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.ColorHistogramTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.ColorTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.CropTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.FitsHeaderTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.FlipTask;
import edu.caltech.ipac.firefly.visualize.task.rpc.LoadDS9RegionTask;
import edu.caltech.ipac.firefly.visualize.task.rpc.PlotFileTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.RegionData;
import edu.caltech.ipac.firefly.visualize.task.rpc.RotateTaskRPC;
import edu.caltech.ipac.firefly.visualize.task.rpc.StretchTaskRPC;
import edu.caltech.ipac.firefly.visualize.ui.FitsHeaderDialog;
import edu.caltech.ipac.visualize.plot.ImagePt;

/**
 * @author Trey Roby
 */
public class VisTask {

    private static VisTask instance= null;

    private static PlotServiceAsync pserv= PlotService.App.getInstance();


    public static VisTask getInstance() {
        if (instance==null)  instance= new VisTask();
        return instance;
    }

    public VisTask() { }

    public PlotFileTask plot(WebPlotRequest request1,
                             WebPlotRequest request2,
                             WebPlotRequest request3,
                             boolean threeColor,
                             String message,
                             boolean removeOldPlot,
                             boolean addToHistory,
                             AsyncCallback<WebPlot> notify,
                             MiniPlotWidget mpw) {
        return PlotFileTaskRPC.plot(request1, request2, request3, threeColor,
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

    public void crop(WebPlot plot, String message, String newTitle, ImagePt pt1, ImagePt pt2, MiniPlotWidget mpw) {
        CropTaskRPC.crop(plot, message, newTitle, pt1, pt2, mpw);
    }

    public void rotateNorth(WebPlot plot, boolean rotateNorth, MiniPlotWidget mpw) {
        RotateTaskRPC.rotateNorth(plot,rotateNorth,mpw);
    }

    public void rotate(WebPlot plot, boolean rotate, double angle, MiniPlotWidget mpw) {
        RotateTaskRPC.rotate(plot, rotate, angle, mpw);
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
