package edu.caltech.ipac.firefly.fftools;
/**
 * User: roby
 * Date: 6/22/12
 * Time: 3:31 PM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.ui.RootPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.JSLoad;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.util.StringUtils;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Trey Roby
 */
public class FFToolEnv {


    private static final String HUB = "FFTOOLS_Hub";
    private static final String ADVERTISE = "FireflyTools.Advertise";
    private static final FireflyCss _ffCss = CssData.Creator.getInstance().getFireflyCss();
    private static String _rootPath = null;
    private static boolean _scriptLoaded = false;
    private static boolean _initComplete = false;
    private static boolean apiMode= false;
    private static Logger  logger= GwtUtil.getClientLogger();




    public static void loadJS() {
        String js= GWT.getModuleBaseURL() + "js/fftools/fireflyJSTools.js";
        new JSLoad(new JSLoad.Loaded(){
            public void allLoaded() {
                _scriptLoaded = true;
                initPlotting();
                initTable();
                initPlot();
                notifyLoaded();
            }
        },js);
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
        EventHub hub = (EventHub) Application.getInstance().getAppData(HUB);
        if (hub == null) {
            hub = new EventHub();
            Application.getInstance().setAppData(HUB, hub);
        }
        return hub;
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

    public static RootPanel getRootPanel(String div) {
        RootPanel rp = (div == null) ? RootPanel.get() : RootPanel.get(div);
        rp.addStyleName(_ffCss.globalSettings());
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


    /**
     * Modified a relative root path to a full root path.  The root is the source of the hosting web site by default.
     * If setRootPath has been called then the root the what was set.
     *
     * @param url a url relative or full, the relative is modified to full
     * @return a full path URL
     */
    public static String modifyURLToFull(String url) {
        String retURL = url;
        if (!StringUtils.isEmpty(url)) {
            if (!isFull(url)) {
                if (_rootPath == null) {
                    String docUrl = Document.get().getURL();
                    int lastSlash = docUrl.lastIndexOf("/");
                    if (lastSlash > -1) {
                        String rootURL = docUrl.substring(0, lastSlash + 1);
                        retURL = rootURL + url;
                    } else {
                        retURL = docUrl + "/" + url;
                    }
                } else {
                    retURL = _rootPath + "/" + url;
                }
            }
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


//============================================================================================
//------- JSNI code from here on -------------------------------------------------------------
//============================================================================================



    public static native boolean isDebug() /*-{
        return $wnd.firefly.debug;
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



    private static native void initPlotting() /*-{

        // these I would like to make private like below
        $wnd.firefly.plotImage=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotImage(Ledu/caltech/ipac/firefly/data/JscriptRequest;));
        $wnd.firefly.plotGroupedImage=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotGroupedImage(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));
        $wnd.firefly.plotAsExpanded=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotAsExpanded(Ledu/caltech/ipac/firefly/data/JscriptRequest;Z));
        $wnd.firefly.plotExternal=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotExternal(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));
        $wnd.firefly.plotExternalMulti=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotExternalMulti(Lcom/google/gwt/core/client/JsArray;Ljava/lang/String;));

        // these could be moved to a util object under firefly
        $wnd.firefly.serializeRangeValues=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::serializeRangeValues(Ljava/lang/String;DDLjava/lang/String;));
        $wnd.firefly.setRootPath=
                $entry(@edu.caltech.ipac.firefly.fftools.FFToolEnv::setRootPath(Ljava/lang/String;));

        // these will stay public
        $wnd.firefly.addCoveragePlot=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::addCoveragePlot(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));
        $wnd.firefly.addDataViewer=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::addDataViewer(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));

        $wnd.firefly.addDataSourceCoveragePlot=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::addDataSourceCoveragePlot(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));
        $wnd.firefly.addDrawingLayer=
                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::addDrawingLayer(Ledu/caltech/ipac/firefly/data/JscriptRequest;));


        // I am going towards the addPrivate approach, this works with the closure in the new firefly object
        $wnd.firefly.addPrivate("plotGroupedImageToDiv",
                                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotGroupedImageToDiv(Ljava/lang/String;Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;)));
        $wnd.firefly.addPrivate("plotImageToDiv",
                                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::plotImageToDiv(Ljava/lang/String;Ledu/caltech/ipac/firefly/data/JscriptRequest;)));
        $wnd.firefly.addPrivate("setCloseButtonClosesWindow",
                                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::setCloseButtonClosesWindow(Z)));
        $wnd.firefly.addPrivate("makeTabPanel",
                                $entry(@edu.caltech.ipac.firefly.fftools.FitsViewerJSInterface::makeTabPanel(Ljava/lang/String;Ljava/lang/String;)));
    }-*/;

    private static native void initPlot() /*-{
        $wnd.firefly.showPlot=
                $entry(@edu.caltech.ipac.firefly.fftools.XYPlotJSInterface::plotTable(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));
    }-*/;

    private static native void initTable() /*-{
        $wnd.firefly.showTable=
                $entry(@edu.caltech.ipac.firefly.fftools.TableJSInterface::showTable(Ledu/caltech/ipac/firefly/data/JscriptRequest;Ljava/lang/String;));

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
