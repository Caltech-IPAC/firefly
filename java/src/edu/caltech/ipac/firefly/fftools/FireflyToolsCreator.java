package edu.caltech.ipac.firefly.fftools;

import com.google.gwt.core.client.GWT;
import edu.caltech.ipac.firefly.core.Creator;
import edu.caltech.ipac.firefly.core.DefaultRequestHandler;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.RequestHandler;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.ui.JSLoad;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;

import java.util.Map;

public class FireflyToolsCreator implements Creator {

    private static boolean _scriptLoaded = false;
    private static boolean _initComplete = false;

    public FireflyToolsCreator() {
//        initFireflyObject();
        loadJS();
//        defineImageViewer();
//        defineExpandedViewer();
    }



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



    public LayoutManager makeLayoutManager() { return null; }

    public boolean isApplication() { return false; }



    public Toolbar getToolBar() { return null; }
    public Map makeCommandTable() { return null; }
    public RequestHandler makeCommandHandler() { return new DefaultRequestHandler(); }
    public LoginManager makeLoginManager() { return null; }
    public String getLoadingDiv() { return null; }
    public String getAppDesc() { return null; }
    public String getAppName() { return null; }

    public void postInitialization() {
        _initComplete= true;
        notifyLoaded();
    }

    public static void notifyLoaded() {
        if (_scriptLoaded && _initComplete) {
            try {
                if (!FireflyToolsCreator.doOnFireflyLoaded()) {
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


//============================================================================================
//------- JSNI code from here on -------------------------------------------------------------
//============================================================================================


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