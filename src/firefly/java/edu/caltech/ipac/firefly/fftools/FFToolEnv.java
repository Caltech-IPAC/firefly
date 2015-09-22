/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.fftools;
/**
 * User: roby
 * Date: 6/22/12
 * Time: 3:31 PM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.JSLoad;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.visualize.Ext;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Trey Roby
 */
public class FFToolEnv {

    private static final HashMap<String,Widget> allPanelMap= new HashMap<String, Widget>(31);

    //private static final String HUB = "FFTOOLS_Hub";
    private static final String ADVERTISE = "FireflyTools.Advertise";
    private static final FireflyCss _ffCss = CssData.Creator.getInstance().getFireflyCss();
    private static String _rootPath = null;
    private static boolean _scriptLoaded = false;
    private static boolean _initComplete = false;
    private static boolean apiMode= false;
    private static Logger  logger= GwtUtil.getClientLogger();




    public static void loadJS() {
        String js= GWT.getModuleBaseURL() + "js/fftools/fireflyJSTools.js";
        String fireflyJS= GWT.getModuleBaseURL() + "fflib.js";

        new JSLoad(new JSLoad.Loaded(){
            public void allLoaded() {
                _scriptLoaded = true;
                monitorForExternalPushChannel();
                initFitsView();
                initTable();
                initPlot();
                notifyLoaded();
            }
        },js,fireflyJS);
    }

    public static void postInitialization() {
        _initComplete= true;
        notifyLoaded();
    }

    public static boolean isAPIMode() { return apiMode;   }
    public static void setApiMode(boolean apiMode) {
        FFToolEnv.apiMode = apiMode;
    }


    public static void notifyLoaded() {
        if (_scriptLoaded && _initComplete) {
            try {
                if (!doOnFireflyLoaded()) {
                    FFToolEnv.logDebugMsg("DEBUG: Init Method Missing",
                                          "Could not find init method on web page: onFireflyLoaded()");
                }

            } catch (Throwable e) {
                FFToolEnv.logDebugMsg("DEBUG: Init Method failed",
                                      "init method, onFireflyLoaded(), threw an exception and did not initialize correctly: " +
                                              e.toString());
            }
        }

    }




    public static boolean isAdvertise() {
        boolean retval = Application.getInstance().getProperties().getBooleanProperty(ADVERTISE, false);
        if (isAdvertiseDefined()) {
            retval = isAdvertise();
        }
        return retval;
    }



    public static EventHub getHub() {
        return Application.getInstance().getEventHub();
//        EventHub hub = (EventHub) Application.getInstance().getAppData(HUB);
//        if (hub == null) {
//            hub = new EventHub();
//            Application.getInstance().setAppData(HUB, hub);
//        }
//        return hub;
    }


    public static void logParamParseError(String source, String param, String value, String toType) {
        logDebugMsg(source + " could not parse param, " + param +
                            ", with value " + value + ", to type " + toType);
    }

    public static void logDebugMsg(String msg) {
        logDebugMsg(null, msg);
    }

    public static void logDebugMsg(String title, String msg) {
        if (FFToolEnv.isDebug()) {
            if (title == null) title = "FFTools";
//            PopupUtil.showError(title, msg);
            logger.log(Level.INFO, title+": "+msg);
        }
    }

    public static void addToPanel(String id, Widget w, String title) {
        Widget container= getPanelByID(id);
        if (container instanceof TabPane) {
            TabPane<Widget> tp= (TabPane)container;
            tp.addTab(w,title);
        }
        else if (container instanceof Panel){
            ((Panel)container).add(w);
        }
    }


    public static boolean isTabPanel(String id) {
        Widget w= getPanelByID(id);
        return  (w instanceof TabPane);
    }


    public static Widget getPanelByID(String id) {
        Widget retval= null;
        if (allPanelMap.containsKey(id)) {
            retval= allPanelMap.get(id);
        }
        else {
            if (id!=null) {
                retval= getRootPanel(id);
                if (retval==null) {
                    FFToolEnv.logDebugMsg("Could not find div: " + id + ", using root of document");
                    retval= getRootPanel(null);
                }
            }
        }
        return retval;
    }


    public static void putPanel(String id, Widget panel) {
        allPanelMap.put(id,panel);
    }


    public static RootPanel getRootPanel(String div) {
        RootPanel rp = (div == null) ? RootPanel.get() : RootPanel.get(div);
        if (rp != null) {
            rp.addStyleName(_ffCss.globalSettings());
        }
        return rp;
    }

    /**
     * set the root path so all URL with out a root path are modified to be full with this path
     *
     * @param rootPath the root path of the url
     */
    public static void setRootPath(String rootPath) {
        if (rootPath != null) {
            if (isFull(rootPath)) {
                _rootPath = rootPath;
            } else {
                String docUrl = Document.get().getURL();
                String sAry[] = docUrl.split("/");
                if (sAry.length >= 3) {
                    _rootPath = sAry[0] + "//" + sAry[2] + "/" + rootPath;
                }
            }

        } else {
            _rootPath = null;
        }

    }

    public static String getRootPath() { return _rootPath; }


    /**
     * Modified a relative root path to a full root path.  The root is the source of the hosting web site by default.
     * If setRootPath has been called then the root the what was set.
     *
     * @param url a url relative or full, the relative is modified to full
     * @return a full path URL
     */
    public static String modifyURLToFull(String url) {
        return modifyURLToFull(url,_rootPath);
    }


    public static String modifyURLToFull(String url, String rootPath) {
        return modifyURLToFull(url,rootPath,null);
    }


    public static String modifyURLToFullAlways(String url) {
        return modifyURLToFull(url,_rootPath,"/");
    }

    public static String modifyURLToFull(String url, String rootPathOp1, String rootPathFallback) {
        String rootPath= rootPathOp1!=null ? rootPathOp1 : rootPathFallback;
        String retURL = url;
        if (!StringUtils.isEmpty(url)) {
            if (!isFull(url)) {
                if (rootPath == null) {
                    String docUrl = Document.get().getURL();
                    int lastSlash = docUrl.lastIndexOf("/");
                    if (lastSlash > -1) {
                        String rootURL = docUrl.substring(0, lastSlash + 1);
                        retURL = rootURL + url;
                    } else {
                        retURL = docUrl + "/" + url;
                    }
                } else {
                    retURL = rootPath.endsWith("/") ? rootPath + url : rootPath + "/" + url;
                }
            }
        }
        else {
            retURL= rootPath;
        }
        return retURL;
    }



    private static boolean isFull(String url) {
        url = url.toLowerCase();
        return url.startsWith("http") ||
                url.startsWith("https") ||
                url.startsWith("/") ||
                url.startsWith("file");
    }


    public static String getHost(String url) {
        String retval= null;
        if (url!=null && url.length()>8) {
            int lastSlash= url.indexOf("/",9);
            if (lastSlash>-1) {
                retval=  url.substring(0,lastSlash);
            }
        }
        return retval;
    }

    private static void setHelpTarget(String helpTarget) {
        Application.getInstance().getHelpManager().setAppHelpName(helpTarget);
    }

    private static void showHelp(String helpID) {
        Application.getInstance().getHelpManager().showHelpAt(helpID);
    }

//============================================================================================
//------- JSNI code from here on -------------------------------------------------------------
//============================================================================================



    public static native boolean isDebug() /*-{
        return $wnd.firefly.debug;
    }-*/;





    public static void monitorForExternalPushChannel() {
        if (!Application.getInstance().getCreator().isApplication()) {
            Ext.ExtensionInterface exI= Ext.makeExtensionInterfaceWithListener(new Object(), getStoreCBForJs());
        }
    }

    private static String pushChannel= null;

    public static void initExternalPush() {
        Ext.ExtensionInterface exI= Ext.makeExtensionInterface();
        String channel= exI.getRemoteChannel();
        GwtUtil.getClientLogger().log(Level.INFO, "channel:" + channel);
        if (channel!=null && !ComparisonUtil.equals(channel,pushChannel)) {
            pushChannel= channel;
            GwtUtil.getClientLogger().log(Level.INFO, "here:" + channel);
            PushReceiver.ExternalPlotController plotController= new ApiPlotController();
            new PushReceiver(plotController);
        }
    }


    public static void storeCB(Object o) {
        initExternalPush();
    }

    public static native JavaScriptObject getStoreCBForJs() /*-{
        return $entry(@edu.caltech.ipac.firefly.fftools.FFToolEnv::storeCB(*));
    }-*/;




    private static native boolean isInternalAdvertise() /*-{
        if ("advertise" in $wnd.firefly) { return $wnd.firefly.advertise;
        }
        else {
            return true;
        }
    }-*/;


    private static native boolean isAdvertiseDefined() /*-{
        return ("advertise" in $wnd.firefly);
    }-*/;

    private static native boolean doOnFireflyLoaded() /*-{
        if ("onFireflyLoaded" in $wnd) {
            $wnd.onFireflyLoaded();
            return true;
        }
        else {
            return false;
        }

    }-*/;



    private static native void initFitsView() /*-{


        //---------Begin deprecated ------------------------------
        $wnd.firefly.plotImage=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotImage(Ledu/caltech/ipac/firefly/data/JscriptRequest;));
        $wnd.firefly.plotGroupedImage=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotGroupedImage(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));
        //---------End deprecated ------------------------------



        // these I would like to make private like below
        $wnd.firefly.plotAsExpanded=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotAsExpanded(Ledu/caltech/ipac/firefly/data/JscriptRequest;Z));
        $wnd.firefly.plotExternal=
                $entry(@edu.caltech.ipac.firefly.fftools.ExtViewer::plot(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));
        $wnd.firefly.plotExternalMulti=
                $entry(@edu.caltech.ipac.firefly.fftools.ExtViewer::plotMulti(Lcom/google/gwt/core/client/JsArray;Ljava/lang/String;));
        $wnd.firefly.showTableExternal=
                $entry(@edu.caltech.ipac.firefly.fftools.ExtViewer::showTable(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));

        // these could be moved to a util object under firefly
        $wnd.firefly.serializeRangeValues=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::serializeRangeValues(Ljava/lang/String;DDLjava/lang/String;));
        $wnd.firefly.setRootPath=
                $entry(@edu.caltech.ipac.firefly.fftools.FFToolEnv::setRootPath(Ljava/lang/String;));
        $wnd.firefly.setHelpTarget=
                $entry(@edu.caltech.ipac.firefly.fftools.FFToolEnv::setHelpTarget(Ljava/lang/String;));
        $wnd.firefly.showHelp=
                $entry(@edu.caltech.ipac.firefly.fftools.FFToolEnv::showHelp(Ljava/lang/String;));

        // these will stay public
        $wnd.firefly.addCoveragePlot=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::addCoveragePlot(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));
        $wnd.firefly.addDataViewer=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::addDataViewer(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));

        $wnd.firefly.addDataSourceCoveragePlot=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::addDataSourceCoveragePlot(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));
        $wnd.firefly.addDrawingLayer=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::addDrawingLayer(Ledu/caltech/ipac/firefly/data/JscriptRequest;));
        $wnd.firefly.setLockExpandMenuBarVisible=
                $entry(@edu.caltech.ipac.firefly.ui.PopoutWidget::setMenuBarPermLockVisible(Z));

        $wnd.firefly.overlayRegionData=
                        $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::overlayRegionData(Lcom/google/gwt/core/client/JsArrayString;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;));
        $wnd.firefly.removeRegionData=
                        $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::removeRegionData(Lcom/google/gwt/core/client/JsArrayString;Ljava/lang/String;));



        // I am going towards the addPrivate approach, this works with the closure in the new firefly object
        $wnd.firefly.addPrivate("plotGroupedImageToDiv",
                                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotGroupedImageToDiv(Ljava/lang/String;Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;)));
        $wnd.firefly.addPrivate("plotImageToDiv",
                                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotImageToDiv(Ljava/lang/String;Ledu/caltech/ipac/firefly/data/JscriptRequest;)));
        $wnd.firefly.addPrivate("plotOneFileGroup",
                                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotOneFileGroup(Lcom/google/gwt/core/client/JsArray;Ljava/lang/String;)));
        $wnd.firefly.addPrivate("setCloseButtonClosesWindow",
                                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::setCloseButtonClosesWindow(Z)));
        $wnd.firefly.addPrivate("makeTabPanel",
                                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::makeTabPanel(Ljava/lang/String;Ljava/lang/String;)));
    }-*/;

    private static native void initPlot() /*-{
        $wnd.firefly.showPlot=
                $entry(@edu.caltech.ipac.firefly.fftools.XYPlotJSInterface::plotTable(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));
        $wnd.firefly.addXYPlot=
                $entry(@edu.caltech.ipac.firefly.fftools.XYPlotJSInterface::addXYPlot(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));
        $wnd.firefly.showHistogram=
                $entry(@edu.caltech.ipac.firefly.fftools.HistogramJSInterface::plotHistogram(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));

    }-*/;

    private static native void initTable() /*-{
        $wnd.firefly.showTable=
                $entry(@edu.caltech.ipac.firefly.fftools.TableJSInterface::showTable(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;Ljava/lang/String;));

        // the external table stuff pretty experimental
        var extTable= new Object();
        $wnd.firefly.extTable= extTable;

        extTable.bind=
                $entry(@edu.caltech.ipac.firefly.fftools.ExtTableJSInterface::bindExtTable(Ljava/lang/String;ZZ));
        extTable.addMeta=
                $entry(@edu.caltech.ipac.firefly.fftools.ExtTableJSInterface::addExtTableMeta(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;));
        extTable.fireEvent=
                $entry(@edu.caltech.ipac.firefly.fftools.ExtTableJSInterface::fireExtTableEvent(Ljava/lang/String;Ljava/lang/String;Ledu/caltech/ipac/firefly/data/JscriptRequest;));
    }-*/;

    public static class ApiPlotController implements PushReceiver.ExternalPlotController {

        public ApiPlotController() {
        }

        public void update(WebPlotRequest wpr) {
            FitsViewerJSInterface.plotNowToTarget(wpr.getPlotId(), wpr, null);
        }

        public void addXYPlot(Map<String, String> params) { XYPlotJSInterface.plotTable(params, null); }
    }
}

