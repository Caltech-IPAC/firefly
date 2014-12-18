package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.visualize.task.PlotGroupTask;
import edu.caltech.ipac.firefly.visualize.task.VisTask;
import edu.caltech.ipac.firefly.visualize.ui.FitsHeaderDialog;
import edu.caltech.ipac.visualize.plot.ImagePt;

import java.util.List;
/**
 * User: roby
 * Date: Jul 23, 2010
 * Time: 2:44:16 PM
 */


/**
 * @author Trey Roby
 */
public class PlotWidgetOps {

    private final MiniPlotWidget _mpw;
    private VisTask task= VisTask.getInstance();


    public PlotWidgetOps(MiniPlotWidget mpw) { _mpw= mpw; }

    public PlotWidgetGroup getGroup() { return _mpw.getGroup(); }
    public WebPlotView getPlotView()  { return _mpw.getPlotView(); }

    public void showImageSelectDialog() { _mpw.showImageSelectDialog(); }
    public boolean isPlotShowing()         { return _mpw.isPlotShowing(); }
    public WebPlot getCurrentPlot()         { return _mpw.getCurrentPlot(); }

    public MiniPlotWidget getMPW() { return _mpw;}

    public void addColorBand(WebPlotRequest request, Band band, AsyncCallback<WebPlot> notify) {
            task.addColorBand(_mpw.getPlotView().getPrimaryPlot(), request, band, notify, _mpw);
    }

    public void removeColorBand(Band band) {
            task.removeColorBand(_mpw.getPlotView().getPrimaryPlot(), band, _mpw);
    }

    public void flipImage() {
        task.flipY(_mpw);
    }

    public void getAreaStatistics(final ImagePt pt1, final ImagePt pt2,final ImagePt pt3, final ImagePt pt4)  {
            task.getAreaStatistics(_mpw.getPlotView().getPrimaryPlot().getPlotState(),
                                                 "Getting Area Statistics...", pt1, pt2, pt3, pt4,
                                                 _mpw);
    }

    public void getFitsHeaderInfo(FitsHeaderDialog dialog)  {
            task.getFitsHeaderInfo(_mpw.getPlotView().getPrimaryPlot().getPlotState(),
                                                    "Getting Fits File Header Info...",
                                                    _mpw,
                                                    dialog);

    }

    public void restoreDefaults() {
        DefaultRequestInfo def= _mpw.getDefaultsPlotRequest();
        if (def!=null) {
            if (def.isThreeColor()) {
                plot3Internal(def.getRequest(Band.RED), def.getRequest(Band.GREEN), def.getRequest(Band.BLUE),
                              false, false, false, true, null);
            }
            else {
                plotInternal(def.getRequest(Band.NO_BAND), false, false, false, true, null);
            }
        }
    }


    public void plotExpanded(WebPlotRequest request,
                             boolean canCollapse,
                             AsyncCallback<WebPlot> notify) {
        plotInternal(request, false, true, true, canCollapse, notify);
    }

    public void plot3Expanded(WebPlotRequest red,
                              WebPlotRequest green,
                              WebPlotRequest blue,
                              boolean canCollapse,
                              AsyncCallback<WebPlot> notify) {
        plot3Internal(red,green,blue, false, true, true, canCollapse, notify);
    }


    public void plot(WebPlotRequest request) { plotInternal(request, false, true,
                                                            AllPlots.getInstance().isExpanded(),
                                                            true, null); }


    public void plot(WebPlotRequest request,
                     boolean addToHistory,
                     AsyncCallback<WebPlot> notify) {
        plotInternal(request,addToHistory,true,AllPlots.getInstance().isExpanded(), true,notify);
    }
    public void plot(WebPlotRequest request,
                     boolean addToHistory,
                     boolean expanded,
                     AsyncCallback<WebPlot> notify) {
        plotInternal(request,addToHistory,true,expanded, true,notify);
    }




    public static void plotGroup(final Element maskElement,
                                 final List<WebPlotRequest> requestList,
                                 final List<MiniPlotWidget> mpwList,
                                 final boolean plotExpanded,
                                 final AsyncCallback<WebPlot> notify) {

        Vis.init(new Vis.InitComplete() {
            public void done() {
                if (plotExpanded) AllPlots.getInstance().forceExpand(mpwList.get(0));
                for(int i=0; (i<requestList.size()); i++) {
                    MiniPlotWidget mpw= mpwList.get(i);
                    WebPlotRequest r= requestList.get(i);
                    mpw.setDefaultPlotRequest(new DefaultRequestInfo(r));
                    mpw.setStartingExpanded(plotExpanded);
                    mpw.setCanCollapse(true);
                    mpw.initMPW();
                    if (plotExpanded) {
                        if (mpw.getPlotView()!=null) mpw.getPlotView().clearAllPlots();
                        r.setZoomType(ZoomType.FULL_SCREEN);
                        r.setZoomToWidth(200);
                        r.setZoomToHeight(200);
                        mpw.prepare(r, null, null, false, true);

                    }
                    else {
                        List<WebPlotRequest> rl= mpw.prepare(r,null,null,false,true);
                        requestList.set(i,rl.get(0));
                    }
                }
                PlotGroupTask.plot(maskElement,requestList,mpwList,notify);
            }
        });
    }



    private void plotInternal(final WebPlotRequest request,
                              final boolean addToHistory,
                              final boolean enableMods,
                              final boolean plotExpanded,
                              final boolean canCollapse,
                              final AsyncCallback<WebPlot> notify) {
        _mpw.setDefaultPlotRequest(new DefaultRequestInfo(request));
        _mpw.setStartingExpanded(plotExpanded);
        _mpw.setCanCollapse(canCollapse);
        if (plotExpanded) {
            Vis.init(_mpw,new Vis.InitComplete() {
                public void done() {
                    DeferredCommand.addCommand(new Command() {
                        public void execute() {
                            doExpand(request, addToHistory, enableMods, notify);
                        }
                    });
                }
            });
        }
        else {
            _mpw.initAndPlot(request,null,null,false,addToHistory,enableMods, notify);
        }
    }


    public void doExpand(WebPlotRequest request,
                         boolean addToHistory,
                         boolean enableMods,
                         AsyncCallback<WebPlot> notify
                              ) {
        if (_mpw.getPlotView()!=null) _mpw.getPlotView().clearAllPlots();
        AllPlots.getInstance().forceExpand(_mpw);
        request.setZoomType(ZoomType.FULL_SCREEN);
        request.setZoomToWidth(200);
        request.setZoomToHeight(200);
        _mpw.initAndPlot(request, null, null, false, addToHistory, enableMods, notify);
    }

    public void plot3Internal(final WebPlotRequest red,
                              final WebPlotRequest green,
                              final WebPlotRequest blue,
                              final boolean addToHistory,
                              final boolean enableMods,
                              final boolean plotExpanded,
                              final boolean canCollapse,
                              final AsyncCallback<WebPlot> notify) {
        _mpw.setDefaultPlotRequest(new DefaultRequestInfo(red,green,blue));
        _mpw.setCanCollapse(canCollapse);
        if (plotExpanded) {
            red.setZoomType(ZoomType.FULL_SCREEN);
            red.setZoomToWidth(Window.getClientWidth());
            red.setZoomToHeight(Window.getClientHeight()-125);
            green.setZoomType(ZoomType.FULL_SCREEN);
            green.setZoomToWidth(Window.getClientWidth());
            green.setZoomToHeight(Window.getClientHeight()-125);
            blue.setZoomType(ZoomType.FULL_SCREEN);
            blue.setZoomToWidth(Window.getClientWidth());
            blue.setZoomToHeight(Window.getClientHeight()-125);
        }

        if (plotExpanded) {
            Vis.init(_mpw,new Vis.InitComplete() {
                public void done() {
                    DeferredCommand.addCommand(new Command() {
                        public void execute() {
                            doExpand3Color(red, green, blue, addToHistory, enableMods, notify);
                        }
                    });
                }
            });
        }
        else {
            _mpw.initAndPlot(red,green,blue,true, addToHistory,enableMods, notify);
        }
    }




    private void doExpand3Color(WebPlotRequest red,
                                WebPlotRequest green,
                                WebPlotRequest blue,
                                final boolean addToHistory,
                                final boolean enableMods,
                                final AsyncCallback<WebPlot> notify) {
        if (_mpw.getPlotView()!=null) _mpw.getPlotView().clearAllPlots();
        AllPlots.getInstance().forceExpand(_mpw);
        _mpw.initAndPlot(red, green, blue, true, addToHistory, enableMods, notify);
    }

    public void plot3Color(WebPlotRequest request,
                           Band band,
                           boolean addToHistory,
                           boolean expanded,
                           AsyncCallback<WebPlot> notify) {
        plot3Internal(band==Band.RED   ? request : null,
                      band==Band.GREEN ? request : null,
                      band==Band.BLUE  ? request : null,
                      addToHistory,true, expanded, true,notify);
    }

    public void plot3Color(WebPlotRequest red,
                           WebPlotRequest green,
                           WebPlotRequest blue,
                           boolean addToHistory,
                           AsyncCallback<WebPlot> notify) {

        plot3Internal(red, green, blue, addToHistory, true, false, true, notify);
    }

    public void removeCurrentPlot() {
        if (_mpw.getPlotView()!=null) {
            WebPlot p= _mpw.getPlotView().getPrimaryPlot();
            if (p!=null) removePlot(p);
        }
    }


    public void removePlot(WebPlot plot) {
        if (_mpw.getPlotView()!=null) {
            _mpw.getPlotView().removePlot(plot,true);
        }
    }
}

