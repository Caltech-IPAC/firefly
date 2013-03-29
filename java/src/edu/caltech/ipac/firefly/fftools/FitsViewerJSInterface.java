package edu.caltech.ipac.firefly.fftools;
/**
 * User: roby
 * Date: 12/7/11
 * Time: 1:33 PM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.JscriptRequest;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerParams;
import edu.caltech.ipac.firefly.ui.PopupContainerForStandAlone;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.CoverageCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.creator.drawing.DrawingLayerProvider;
import edu.caltech.ipac.firefly.ui.creator.eventworker.DrawingLayerCreator;
import edu.caltech.ipac.firefly.ui.table.NewTableEventHandler;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.util.WebUtil;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Trey Roby
 */
public class FitsViewerJSInterface {

    private static final String EXPANDED_KEY= "ExpandedKey";
    private static MiniPlotWidget _mpw= null;
    private static final Map<String,MiniPlotWidget> mpwMap= new HashMap<String,MiniPlotWidget>(17);
    private static final Map<String,TabPane> tpMap= new HashMap<String,TabPane>(17);
    private static boolean _closeButtonClosesWindow= false;
    private static FloatingBackgroundManager _floatingBM= null;

//============================================================================================
//------- Methods take the JSPlotRequest, called from javascript, converts then calls others -
//============================================================================================

    public static void plotImage(JscriptRequest jspr) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr,FFToolEnv.isAdvertise());
        if (wpr.getPlotToDiv()!=null) {
            plotNowToDiv(wpr, null);
        }
        else {
            if (_mpw!=null) plotImageNow(wpr);
        }
    }

    public static void plotImageToDiv(String div, JscriptRequest jspr) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr,FFToolEnv.isAdvertise());
        if (div!=null) {
            plotNowToTarget(div, wpr, null);
        }
    }


    public static void plotGroupedImageToDiv(String div, JscriptRequest jspr, String group) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr,FFToolEnv.isAdvertise());
        if (div!=null) {
            plotNowToTarget(div, wpr, group);
        }
        else {
            PopupUtil.showError("Plot Error", "You must specify this \"PlotToDiv\" parameter");
        }
    }

    public static void plotGroupedImage(JscriptRequest jspr, String group) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr,FFToolEnv.isAdvertise());
        if (wpr.getPlotToDiv()!=null) {
            plotNowToDiv(wpr, group);
        }
        else {
            PopupUtil.showError("Plot Error", "You must specify this \"PlotToDiv\" parameter");
        }
    }

    public static void plotAsExpanded(JscriptRequest jspr,boolean fullControl) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr,FFToolEnv.isAdvertise());
        plotNowAsExpanded(wpr,fullControl);
    }


    /**
     *
     * @param jspr the request from java script
     */
    public static void plotExternal(JscriptRequest jspr, String target) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr,FFToolEnv.isAdvertise());
        findURLAndMakeFull(wpr);
        String url= getHost(GWT.getModuleBaseURL()) + "/fftools/app.html"; // TODO: need to fixed this
        List<Param> pList= new ArrayList(5);
        pList.add(new Param(Request.ID_KEY, "FFToolsImageCmd"));
        pList.add(new Param(CommonParams.DO_PLOT, "true"));
        for(Param p : wpr.getParams()) {
            if (p.getName()!=Request.ID_KEY) pList.add(p);
        }

        url= WebUtil.encodeUrl(url, WebUtil.ParamType.POUND, pList);
        if (target==null) target= "_blank";
        Window.open(url,target, "");
    }


    public static void plotExternal_OLD_VERSION(JscriptRequest jspr, String target) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr,FFToolEnv.isAdvertise());
        findURLAndMakeFull(wpr);
//        String url= getHost(GWT.getModuleBaseURL()) + "/applications/resultViewer/servlet/ShowResult";
        String url= getHost(GWT.getModuleBaseURL()) + "/fftools/sticky/FireFly_Standalone";
        List<Param> pList= new ArrayList(5);
        pList.add(new Param(ServerParams.COMMAND, ServerParams.PLOT_EXTERNAL));
        pList.add(new Param(ServerParams.REQUEST, wpr.toString()));

        url= WebUtil.encodeUrl(url, pList);
        if (target==null) target= "_blank";
        Window.open(url,target, "");
    }




    private static String getHost(String url) {
        String retval= null;
        if (url!=null && url.length()>8) {
            int lastSlash= url.indexOf("/",9);
            if (lastSlash>-1) {
                retval=  url.substring(0,lastSlash);
            }
        }
        return retval;
    }

    public static void addCoveragePlot(JscriptRequest jspr, String div) {
        Map<String,String> paramMap= jspr.asMap();
        WidgetFactory factory= Application.getInstance().getWidgetFactory();
        if (paramMap.containsKey(CommonParams.CENTER_COLUMNS) || paramMap.containsKey(CommonParams.CORNER_COLUMNS)) {
            if (!paramMap.containsKey(CommonParams.CENTER_COLUMNS)) {
                paramMap.put(CommonParams.CENTER_COLUMNS, "ra,dec");
            }
        }
        else {
            paramMap.put(CommonParams.ENABLE_DEFAULT_COLUMNS, "true");
        }

        TablePreview covPrev= factory.createObserverUI(WidgetFactory.COVERAGE_VIEW,paramMap);
        covPrev.bind(FFToolEnv.getHub());

        RootPanel root= FFToolEnv.getRootPanel(div);
        if (root!=null) {
            SimplePanel panel= makeCenter();
            root.add(panel);
            panel.add(covPrev.getDisplay());
        }
        else {
            FFToolEnv.logDebugMsg("Could not find div: " + div + ", CoveragePlot was not added");
        }

    }

    public static void addDataSourceCoveragePlot(JscriptRequest jspr, String div) {
        Map<String,String> paramMap= jspr.asMap();
        WidgetFactory factory= Application.getInstance().getWidgetFactory();
        if (!paramMap.containsKey(CommonParams.ENABLE_DETAILS)) {
            paramMap.put(CommonParams.ENABLE_DETAILS,"false");
        }

        TablePreview covPrev= factory.createObserverUI(WidgetFactory.DATA_SOURCE_COVERAGE_VIEW,paramMap);
        covPrev.bind(FFToolEnv.getHub());

        RootPanel root= FFToolEnv.getRootPanel(div);
        if (root!=null) {
            SimplePanel panel= makeCenter();
            root.add(panel);
            panel.add(covPrev.getDisplay());
        }
        else {
            FFToolEnv.logDebugMsg("Could not find div: " + div + ", CoveragePlot was not added");
        }
    }


    public static void addDrawingLayer(JscriptRequest jspr) {
        Map<String,String> paramMap= jspr.asMap();
        paramMap.put("searchProcessorId", TableJSInterface.SEARCH_PROC_ID);

        if (paramMap.containsKey(CommonParams.CENTER_COLUMNS) || paramMap.containsKey(CommonParams.CORNER_COLUMNS)) {
            if (!paramMap.containsKey(CommonParams.CENTER_COLUMNS)) {
                paramMap.put(CommonParams.CENTER_COLUMNS, "ra,dec");
            }
        }
        else {
            paramMap.put(CommonParams.ENABLE_DEFAULT_COLUMNS, "true");
        }


        WidgetFactory factory= Application.getInstance().getWidgetFactory();
        DrawingLayerProvider dl= (DrawingLayerProvider)factory.createEventWorker(DrawingLayerCreator.DATASET_VIS_QUERY,paramMap);
        FFToolEnv.getHub().bind(dl);
        dl.bind(FFToolEnv.getHub());

        if (paramMap.containsKey(TableJSInterface.TBL_SOURCE)) {
            Map<String,String> activateParams= new HashMap<String, String>(3);
            String url= paramMap.get(TableJSInterface.TBL_SOURCE);
            if (url!=null) {
                url= FFToolEnv.modifyURLToFull(url);
                activateParams.put(TableJSInterface.TBL_SOURCE, url);
                dl.activate(null,paramMap);
            }
        }

    }




    public static void addDataViewer(JscriptRequest jspr, String div) {
        Map<String,String> paramMap= jspr.asMap();
        WidgetFactory factory= Application.getInstance().getWidgetFactory();

        if (!paramMap.containsKey(CoverageCreator.QUERY_ID)) {
            paramMap.put(CoverageCreator.QUERY_ID, CommonParams.ALL_TABLES);
        }
        TablePreview covPrev= factory.createObserverUI(WidgetFactory.DATA_VIEW,paramMap);
        covPrev.bind(FFToolEnv.getHub());

        RootPanel root= FFToolEnv.getRootPanel(div);
        if (root!=null) {
            SimplePanel panel= makeCenter();
            root.add(panel);
            panel.add(covPrev.getDisplay());
        }
        else {
            FFToolEnv.logDebugMsg("Could not find div: " + div + ", DataViewer was not added");
        }
    }

    public static String serializeRangeValues(String stretchType,
                                              double lowerValue,
                                              double upperValue,
                                              String algorithm) {
        RangeValues rv= RangeValues.create(stretchType,lowerValue,upperValue,algorithm);
        return rv.serialize();
    }

    public static void setCloseButtonClosesWindow(boolean closeWindow) {
        _closeButtonClosesWindow= closeWindow;
    }

    public static void makeTabPanel(String div,String tpName) {
        if (_floatingBM==null) _floatingBM= new FloatingBackgroundManager(FloatingBackgroundManager.Position.TOP_RIGHT);
        TabPane tp = new TabPane();
        tp.setSize("100%", "100%");
        tp.setTabPaneName(tpName);
        tpMap.put(tpName,tp);
        RootPanel root= FFToolEnv.getRootPanel(div);
        root.add(tp);
        new NewTableEventHandler(FFToolEnv.getHub(), tp);
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static void plotImageNow(final WebPlotRequest request) {
        _mpw.getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(PlotWidgetOps widgetOps) {
                widgetOps.plot(request);
            }
        });
    }


    private static void plotNowAsExpanded(final WebPlotRequest wpr, boolean fullControl) {

        if (!mpwMap.containsKey(EXPANDED_KEY)) {

            RootPanel root= RootPanel.get();
            if (root!=null) {
                SimplePanel panel= new SimplePanel();
                root.add(panel);
                panel.setSize("1px", "1px");
                final MiniPlotWidget mpw= makeMPW(null,fullControl);
                mpw.supportTabs();
                mpw.setCatalogButtonEnable(true);
                panel.setWidget(mpw);
                if (_floatingBM==null && !fullControl) {
                    _floatingBM= new FloatingBackgroundManager(FloatingBackgroundManager.Position.UNDER_TOOLBAR);
                }
                mpwMap.put(EXPANDED_KEY, mpw);
                mpw.getOps(new MiniPlotWidget.OpsAsync() {
                    public void ops(final PlotWidgetOps widgetOps) {
                        widgetOps.plotExpanded(wpr, false, new WebPlotCallback(mpw,false));
                    }
                });
            }
        }
        else {
            final MiniPlotWidget mpw= mpwMap.get(EXPANDED_KEY);
            mpw.getOps(new MiniPlotWidget.OpsAsync() {
                public void ops(PlotWidgetOps widgetOps) {
                    widgetOps.plotExpanded(wpr, false, new WebPlotCallback(mpw,false));
                }
            });

        }

        MiniPlotWidget mpw= mpwMap.get(EXPANDED_KEY);
        if (mpw.getPopoutContainer() instanceof PopupContainerForStandAlone) {
            ((PopupContainerForStandAlone)mpw.getPopoutContainer()).setCloseBrowserWindow(_closeButtonClosesWindow);
        }

    }

    private static MiniPlotWidget makeMPW(String groupName) {
        return makeMPW(groupName,false);
    }

    private static MiniPlotWidget makeMPW(String groupName, boolean fullControl) {
        MiniPlotWidget mpw= new MiniPlotWidget(groupName, new PopupContainerForStandAlone(fullControl));
        mpw.setRemoveOldPlot(true);
        mpw.setMinSize(50, 50);
        mpw.setAutoTearDown(false);
        mpw.setLockImage(false);
        return mpw;
    }


    private static void plotNowToDiv(WebPlotRequest wpr, String groupName) {
        final String div= wpr.getPlotToDiv();
        plotNowToTarget(div, wpr, groupName);
    }

    private static void plotNowToTarget(final String target, final WebPlotRequest wpr, final String groupName) {

        if (target!=null) {
            if (tpMap.containsKey(target)) {
                TabPane tp = tpMap.get(target);
                if (!mpwMap.containsKey(target)) {
                    SimplePanel panel= makeCenter();
                    tp.addTab(panel, "Plot", "Fits Viewer", false, true);
                    createAndPlot(panel, target, groupName, wpr,true);
                }
                else {
                    final MiniPlotWidget mpw= mpwMap.get(target);
                    plot(mpw,groupName,wpr);
                }

            }
            else if (!mpwMap.containsKey(target)) {
                RootPanel root= FFToolEnv.getRootPanel(target);
                if (root!=null) {
                    SimplePanel panel= makeCenter();
                    root.add(panel);
                    createAndPlot(panel, target, groupName, wpr,false);
                }
            }
            else {
                final MiniPlotWidget mpw= mpwMap.get(target);
                plot(mpw,groupName,wpr);
            }
        }

    }

    private static void createAndPlot(SimplePanel panel,
                                      String target,
                                      String groupName,
                                      WebPlotRequest wpr,
                                      boolean enableCatalogs) {
        MiniPlotWidget mpw= makeMPW(groupName);
        mpw.addStyleName("standard-border");
        mpw.setCatalogButtonEnable(enableCatalogs);
        panel.setWidget(mpw);
        panel.addStyleName("fits-input-cmd-main-widget");
        mpwMap.put(target, mpw);
        plot(mpw, groupName, wpr);
    }

    private static void plot(final MiniPlotWidget mpw, final String groupName, final WebPlotRequest wpr) {
        mpw.getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(PlotWidgetOps widgetOps) {
                if (!StringUtils.isEmpty(groupName))  mpw.getGroup().setLockRelated(true);
                widgetOps.plot(wpr, false, new WebPlotCallback(mpw,!StringUtils.isEmpty(groupName)) );
            }
        });
    }

    protected static SimplePanel makeCenter() {
        final SimplePanel center = new SimplePanel();
        center.setSize("100%", "100%");
        return center;
    }

    private static class WebPlotCallback implements AsyncCallback<WebPlot> {
        private final boolean lock;
        private final MiniPlotWidget mpw;
        public WebPlotCallback(MiniPlotWidget mpw, boolean lock) {
            this.lock= lock;
            this.mpw= mpw;
        }

        public void onSuccess(WebPlot plot) {
            if (lock) mpw.getGroup().setLockRelated(lock);
            FFToolEnv.getHub().getCatalogDisplay().addPlotView(plot.getPlotView());
        }
        public void onFailure(Throwable caught) { }
    }


    private static void findURLAndMakeFull(WebPlotRequest wpr) {
        if (wpr.containsParam(WebPlotRequest.URL)) {
            String url= wpr.getURL();
            url= FFToolEnv.modifyURLToFull(url);
            wpr.setURL(url);
        }

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
