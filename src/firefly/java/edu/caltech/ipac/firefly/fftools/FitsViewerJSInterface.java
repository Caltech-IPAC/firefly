/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.fftools;
/**
 * User: roby
 * Date: 12/7/11
 * Time: 1:33 PM
 *
 * 6/26/15
 * User LZ
 * Add ZP, MP parameters for asinh stretch
 */


import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.JscriptRequest;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupContainerForStandAlone;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.creator.CoverageCreator;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.creator.drawing.DrawingLayerProvider;
import edu.caltech.ipac.firefly.ui.creator.eventworker.ActiveTargetCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.DrawingLayerCreator;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.table.NewTableEventHandler;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePreview;
import edu.caltech.ipac.firefly.visualize.*;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.RangeValues;

import java.util.*;
import java.util.logging.Level;


/**
 * @author Trey Roby
 */
public class FitsViewerJSInterface {

    private static final String EXPANDED_KEY= "ExpandedKey";
    private static MiniPlotWidget _mpw= null;
    private static final Map<String,MiniPlotWidget> mpwMap= new HashMap<String,MiniPlotWidget>(17);
    private static final Map<MiniPlotWidget,ArrayList<AsyncCallback<MiniPlotWidget>>> mpwCallbacks = new HashMap<MiniPlotWidget,ArrayList<AsyncCallback<MiniPlotWidget>>>();
//    private static final Map<String,TabPane> tpMap= new HashMap<String,TabPane>(17);
    private static boolean _closeButtonClosesWindow= false;
    private static boolean autoOverlayEnabled= false;
    private static FloatingBackgroundManager _floatingBM= null;

//============================================================================================
//------- Methods take the JSPlotRequest, called from javascript, converts then calls others -
//============================================================================================

    public static void plotImageToDiv(String div, JscriptRequest jspr) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr,FFToolEnv.isAdvertise());
        if (div!=null)  plotNowToTarget(div, wpr, null);
    }

    public static void plotGroupedImageToDiv(String div, JscriptRequest jspr, String group) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr, FFToolEnv.isAdvertise());
        if (div!=null)  plotNowToTarget(div, wpr, group);
        else  PopupUtil.showError("Plot Error", "You must specify this \"PlotToDiv\" parameter");
    }


    public static void plotOneFileGroup(JsArray<JscriptRequest> jsprAry, String group) {
        List<WebPlotRequest> reqList= new ArrayList<WebPlotRequest>(jsprAry.length());
        for(int i=0; i<jsprAry.length(); i++) {
            JscriptRequest jspr= jsprAry.get(i);
            WebPlotRequest wpr= RequestConverter.convertToRequest(jspr, false);
            reqList.add(wpr);
        }
        plotFileGroupToTarget(reqList,group);
    }



    public static void plotAsExpanded(JscriptRequest jspr,boolean fullControl) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr, FFToolEnv.isAdvertise());
        plotNowAsExpanded(wpr,fullControl);
    }


    //--------------- Begin Deprecated methods --------------------------

    @Deprecated
    public static void plotImage(JscriptRequest jspr) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr,FFToolEnv.isAdvertise());
        if (wpr.getPlotToDiv()!=null) {
            plotNowToDiv(wpr, null);
        }
        else {
            if (_mpw!=null) plotImageNow(wpr);
        }
    }

    @Deprecated
    public static void plotGroupedImage(JscriptRequest jspr, String group) {
        WebPlotRequest wpr= RequestConverter.convertToRequest(jspr, FFToolEnv.isAdvertise());
        if (wpr.getPlotToDiv()!=null) {
            plotNowToDiv(wpr, group);
        }
        else {
            PopupUtil.showError("Plot Error", "You must specify this \"PlotToDiv\" parameter");
        }
    }


    //--------------- End Deprecated methods --------------------------



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

        if (!paramMap.containsKey(CommonParams.CATALOGS_AS_OVERLAYS)) {
            paramMap.put(CommonParams.CATALOGS_AS_OVERLAYS, "false");
        }

        if (paramMap.containsKey(WebPlotRequest.OVERLAY_POSITION)) {
            enableAutoOverlays();
            if (paramMap.containsKey("EVENT_WORKER_ID")) {
                String s= paramMap.get("EVENT_WORKER_ID");
                paramMap.put("EVENT_WORKER_ID",s+","+"target");
            }
            else {
                paramMap.put("EVENT_WORKER_ID", "target");
            }
        }



        TablePreview covPrev= factory.createObserverUI(WidgetFactory.COVERAGE_VIEW, paramMap);
        covPrev.bind(FFToolEnv.getHub());

        SimplePanel panel= makeCenter();
        panel.add(covPrev.getDisplay());
        FFToolEnv.addToPanel(div, panel, "Coverage");
    }

    public static void addDataSourceCoveragePlot(JscriptRequest jspr, String div) {
        Map<String,String> paramMap= jspr.asMap();
        WidgetFactory factory= Application.getInstance().getWidgetFactory();
        if (!paramMap.containsKey(CommonParams.ENABLE_DETAILS)) {
            paramMap.put(CommonParams.ENABLE_DETAILS, "false");
        }

        TablePreview covPrev= factory.createObserverUI(WidgetFactory.DATA_SOURCE_COVERAGE_VIEW, paramMap);
        covPrev.bind(FFToolEnv.getHub());


        SimplePanel panel= makeCenter();
        panel.add(covPrev.getDisplay());
        FFToolEnv.addToPanel(div, panel, "Coverage");
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

    public static void overlayRegionData(JsArrayString regionData, final String regionLayerId, final String title, final String plotId) {
        final String regText = "["+regionData.join(StringUtils.STRING_SPLIT_TOKEN)+"]";
        final String[] plotIdArr = (plotId == null) ? null : new String[]{plotId};

        // load region data now if we have at least one loaded MiniPlotWidget with matching plot id
        for(MiniPlotWidget mpw : AllPlots.getInstance().getAll()) {
            if (plotId== null || plotId.equals(mpw.getPlotId())) {
                RegionLoader.loadRegion(title, regText, null, regionLayerId, plotIdArr);
                break;
            }
        }
        // load region data later if we have loading MiniPlotWidgets
        for (MiniPlotWidget mpw : mpwMap.values()) {
            if (mpw.getPlotId() == null) {
                ArrayList<AsyncCallback<MiniPlotWidget>> callbacks = mpwCallbacks.get(mpw);
                if (callbacks==null) {
                    callbacks = new ArrayList<AsyncCallback<MiniPlotWidget>>();
                    mpwCallbacks.put(mpw, callbacks);
                }
                callbacks.add(new AsyncCallback<MiniPlotWidget>() {
                    @Override
                    public void onFailure(Throwable caught) {
                    }

                    @Override
                    public void onSuccess(MiniPlotWidget result) {
                        if (plotId == null || plotId.equals(result.getPlotId())) {
                            RegionLoader.loadRegion(title, regText, null, regionLayerId, plotIdArr);
                        }
                    }
                });
            }
        }
    }

    public static void removeRegionData(JsArrayString regionData, String regionLayerId) {
        String regText = "["+regionData.join(StringUtils.STRING_SPLIT_TOKEN)+"]";
        RegionLoader.removeFromRegion(regText, regionLayerId);
    }


    public static void addDataViewer(JscriptRequest jspr, String div) {
        Map<String,String> paramMap= jspr.asMap();
        WidgetFactory factory= Application.getInstance().getWidgetFactory();

        if (!paramMap.containsKey(CoverageCreator.QUERY_ID)) {
            paramMap.put(CoverageCreator.QUERY_ID, CommonParams.ALL_TABLES);
        }
        TablePreview covPrev= factory.createObserverUI(WidgetFactory.DATA_VIEW,paramMap);
        covPrev.bind(FFToolEnv.getHub());

        SimplePanel panel= makeCenter();
        panel.add(covPrev.getDisplay());
        FFToolEnv.addToPanel(div, panel, "FITS Image");
    }

    //LZ 6/11/15 add this method
    public static String serializeRangeValues(String stretchType,
                                              double lowerValue,
                                              double upperValue, double drValue,double zpValue, double wpValue, double gammaValue,
                                              String algorithm) {
        RangeValues rv= RangeValues.create(stretchType,lowerValue,upperValue,drValue,zpValue, wpValue, gammaValue, algorithm);
        return rv.serialize();
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


//    public static void makeTabPanelForCatalogLoading(String div,String tpName) {
//        if (_floatingBM==null) _floatingBM= new FloatingBackgroundManager(FloatingBackgroundManager.Position.TOP_RIGHT);
//        TabPane tp = new TabPane();
//        tp.setSize("100%", "100%");
//        tp.setTabPaneName(tpName);
//        tpMap.put(tpName,tp);
//        RootPanel root= FFToolEnv.getRootPanel(div);
//        root.add(tp);
//        new NewTableEventHandler(FFToolEnv.getHub(), tp);
//    }


    public static void makeTabPanel(String div,String tpName) {
        TabPane tp = new TabPane();
        FFToolEnv.addToPanel(div, tp, "Tabs");
        tp.setSize("100%", "100%");
        tp.setTabPaneName(tpName);
        FFToolEnv.putPanel(tpName, tp);
    }

    public static void enableTabPanelCatalogLoading(String tpName) {
        Widget w= FFToolEnv.getPanelByID(tpName);
        if (w instanceof TabPane) {
            if (_floatingBM==null) _floatingBM= new FloatingBackgroundManager(FloatingBackgroundManager.Position.TOP_RIGHT);
            new NewTableEventHandler(FFToolEnv.getHub(), (TabPane)w);
        }

    }








//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private static void plotImageNow(final WebPlotRequest request) {
        setWH(_mpw,request);
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
                setWH(mpw,wpr);
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


    private static void enableAutoOverlays() {
        if (!autoOverlayEnabled) {
            autoOverlayEnabled=true;
            Map<String,String> params= new HashMap<String, String>(5);
            params.put(EventWorker.ID, "target");
            params.put(CommonParams.TARGET_TYPE, ActiveTargetCreator.TargetType.PlotFixedTarget.toString());
            EventWorker targetLayer= new WidgetFactory().createEventWorker(CommonParams.ACTIVE_TARGET, params);
            targetLayer.bind(FFToolEnv.getHub());
        }

    }



    private static MiniPlotWidget makeMPW(String groupName, boolean fullControl) {
        final MiniPlotWidget mpw= new MiniPlotWidget(groupName, new PopupContainerForStandAlone(fullControl));
        GwtUtil.setStyles(mpw, "fontFamily", "tahoma,arial,helvetica,sans-serif",
                "fontSize", "11px" );
        mpw.setRemoveOldPlot(true);
        mpw.setMinSize(50, 50);
        mpw.setAutoTearDown(false);
        mpw.setLockImage(false);

        if (autoOverlayEnabled) {
            mpw.getOps(new MiniPlotWidget.OpsAsync() {
                public void ops(final PlotWidgetOps widgetOps) {
                    FFToolEnv.getHub().getDataConnectionDisplay().addPlotView(mpw.getPlotView(), Arrays.asList("target"));
                }
            });
        }
        return mpw;
    }


    private static void plotNowToDiv(WebPlotRequest wpr, String groupName) {
        final String div= wpr.getPlotToDiv();
        plotNowToTarget(div, wpr, groupName);
    }


    private static void plotFileGroupToTarget(final List<WebPlotRequest> wprList, final String groupName) {


        List<MiniPlotWidget> mpwList= new ArrayList<MiniPlotWidget>(wprList.size());
        for(WebPlotRequest wpr : wprList) {
            String div= wpr.getPlotToDiv();
            if (wpr.containsParam(WebPlotRequest.OVERLAY_POSITION)) {
                enableAutoOverlays();
            }
            MiniPlotWidget mpw;
            if (mpwMap.containsKey(div)) {
                mpw= mpwMap.get(div);
            }
            else {
                SimplePanel panel= makeCenter();
                FFToolEnv.addToPanel(div,panel,"FITS Image");
                mpw= createInDiv(panel, div, groupName, false);
            }
            mpwList.add(mpw);
            setWH(mpw,wpr);
        }
        PlotWidgetOps.plotOneFileGroup(null, wprList, mpwList, false, new AsyncCallback<WebPlot>() {
            public void onFailure(Throwable caught) {

            }

            public void onSuccess(WebPlot result) {

            }
        });

    }



    public static void plotNowToTarget(final String target, final WebPlotRequest wpr, final String groupName) {

        if (target!=null) {
            wpr.setPlotId(target);
            if (wpr.containsParam(WebPlotRequest.OVERLAY_POSITION)) {
                enableAutoOverlays();
            }
            if (mpwMap.containsKey(target)) {
                final MiniPlotWidget mpw= mpwMap.get(target);
                plot(mpw, groupName, wpr);
            }
            else {
                SimplePanel panel= makeCenter();
                FFToolEnv.addToPanel(target,panel,"FITS Image");
                createAndPlot(panel, target, groupName, wpr,false);
            }
        }

    }


    private static MiniPlotWidget createInDiv(SimplePanel panel,
                                              String target,
                                              String groupName,
                                              boolean enableCatalogs) {
        MiniPlotWidget mpw= makeMPW(groupName);
        mpw.addStyleName("standard-border");
        mpw.setCatalogButtonEnable(enableCatalogs);
        panel.setWidget(mpw);
        panel.addStyleName("fits-input-cmd-main-widget");
        mpwMap.put(target, mpw);
        return mpw;
    }


    private static void createAndPlot(SimplePanel panel,
                                      String target,
                                      String groupName,
                                      WebPlotRequest wpr,
                                      boolean enableCatalogs) {
        MiniPlotWidget mpw= createInDiv(panel, target, groupName, enableCatalogs);
        plot(mpw, groupName, wpr);
    }

    private static void setWH(MiniPlotWidget mpw, WebPlotRequest wpr) {
        int w= mpw.getOffsetWidth();
        int h= mpw.getOffsetHeight();
        if (w>5 && wpr.getZoomToWidth()==0) {
            wpr.setZoomToWidth(w-6);
        }
        if (h>5 && wpr.getZoomToHeight()==0) {
            wpr.setZoomToHeight(h);
        }
    }

    private static void plot(final MiniPlotWidget mpw, final String groupName, final WebPlotRequest wpr) {
        setWH(mpw,wpr);
        mpw.getOps(new MiniPlotWidget.OpsAsync() {
            public void ops(PlotWidgetOps widgetOps) {
                if (!StringUtils.isEmpty(groupName)) mpw.getGroup().setLockRelated(true);
                widgetOps.plot(wpr, false, new WebPlotCallback(mpw, !StringUtils.isEmpty(groupName)));
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

            // complete callbacks waiting on MiniPlotWidget to load
            ArrayList<AsyncCallback<MiniPlotWidget>> callbacks = mpwCallbacks.get(mpw);
            if (callbacks != null) {
                mpwCallbacks.remove(mpw);
                for (AsyncCallback<MiniPlotWidget> c : callbacks) {
                    try {
                        c.onSuccess(mpw);
                    } catch (Exception e) {
                        GwtUtil.getClientLogger().log(Level.INFO, "unable to complete a callback on plot id "+mpw.getPlotId()+" "+e.getMessage());
                    }
                }
            }
        }
        public void onFailure(Throwable caught) {
            mpwCallbacks.remove(mpw);
        }
    }



}

