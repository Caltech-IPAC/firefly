package edu.caltech.ipac.firefly.fftools;
/**
 * User: roby
 * Date: 4/8/13
 * Time: 10:49 AM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Timer;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.util.CrossDocumentMessage;
import edu.caltech.ipac.firefly.util.WebUtil;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.task.VisTask;
import org.omg.CosNaming.NamingContextExtPackage.StringNameHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class AppMessenger {

    private boolean ENABLE_XDOC_MESSAGING= false;

    private Map<String,JavaScriptObject> loadedWindows= new HashMap<String, JavaScriptObject>(11);
    private static final int TIMEOUT= 120000;
    private CrossDocumentMessage  xOrMsg= new CrossDocumentMessage(FFToolEnv.getHost(GWT.getModuleBaseURL()));
    private FallbackLoader fallbackLoader= null;

    public AppMessenger() {
    }


    public void sendTableToApp(TableServerRequest request, String winName) {
        if (loadedWindows.containsKey(winName) && ENABLE_XDOC_MESSAGING) {
            // todo
        }
        else {
            String url= request.getParam("source");
            url= FFToolEnv.modifyURLToFull(url);
            request.setParam("source", url);
            showTableExternal(request, winName);
        }

    }

    public void sendPlotToApp(WebPlotRequest wpr, String winName) {
        if (loadedWindows.containsKey(winName) && ENABLE_XDOC_MESSAGING) {
            xOrMsg.setListener(new MyMessageListener(wpr,winName));
            fallbackLoader= new FallbackLoader(wpr,winName);
            JavaScriptObject target= loadedWindows.get(winName);
            fallbackLoader.schedule(TIMEOUT);
            xOrMsg.sendAlive(target);
        }
        else {
            findURLAndMakeFull(wpr);
            plotExternal(wpr, winName);
        }
    }

    public void sendPlotsToApp(List<WebPlotRequest> wprList, String winName) {
        String saveKey= "reqGroup-"+System.currentTimeMillis()+"";
        for(int i= 1; (i<wprList.size()); i++) {
            WebPlotRequest req= wprList.get(i);
            findURLAndMakeFull(req);
            VisTask.getInstance().addSavedRequest(saveKey,req);
        }
        WebPlotRequest fReq= wprList.get(0);
        fReq.setParam(WebPlotRequest.MULTI_PLOT_KEY, saveKey);
        findURLAndMakeFull(fReq);
        plotExternal(fReq,winName);
    }

    public void send3ColorPlotToApp(WebPlotRequest red,
                                    WebPlotRequest green,
                                    WebPlotRequest blue,
                                    String winName) {
        String saveKey= "reqGroup-3color-"+System.currentTimeMillis()+"";
        if (red!=null) {
            red.setParam(WebPlotRequest.THREE_COLOR_HINT,WebPlotRequest.RED_HINT);
            VisTask.getInstance().addSavedRequest(saveKey,red);
        }
        if (green!=null) {
            green.setParam(WebPlotRequest.THREE_COLOR_HINT,WebPlotRequest.GREEN_HINT);
            VisTask.getInstance().addSavedRequest(saveKey,blue);
        }
        if (blue!=null) {
            blue.setParam(WebPlotRequest.THREE_COLOR_HINT,WebPlotRequest.BLUE_HINT);
            VisTask.getInstance().addSavedRequest(saveKey,green);
        }
        ServerRequest serverRequest= new ServerRequest();
        serverRequest.setParam(WebPlotRequest.THREE_COLOR_PLOT_KEY, saveKey);
        plotExternal(serverRequest,winName);
    }

    public void showTableExternal(ServerRequest serverRequest, String target) {
        sendExternal(serverRequest, "FFToolsExtCatalogCmd", target);
    }


    public void plotExternal(ServerRequest serverRequest, String target) {
        sendExternal(serverRequest, "FFToolsImageCmd", target);
    }


    public void sendExternal(ServerRequest serverRequest, String idKey, String target) {
        String baseUrl= getViewerURL();
        List<Param> pList= new ArrayList<Param>(5);
        pList.add(new Param(Request.ID_KEY, idKey));
        pList.add(new Param(CommonParams.DO_PLOT, "true"));
        for(Param p : serverRequest.getParams()) {
            if (p.getName()!=Request.ID_KEY) pList.add(p);
        }

        baseUrl= WebUtil.encodeUrl(baseUrl, WebUtil.ParamType.POUND, pList);
        if (target==null) target= "_blank";
//        Window.open(url,target, "");
        JavaScriptObject windowId= openWindow(baseUrl,target, "");
        loadedWindows.put(target,windowId);
    }




    private String getViewerURL() {
        if (GWT.isProdMode()) {
            return FFToolEnv.getHost(GWT.getModuleBaseURL()) + "/fftools/app.html";
        }
        else {
            return FFToolEnv.getHost(GWT.getModuleBaseURL()) + "/fftools/app.html?gwt.codesvr=127.0.0.1:9997";
        }
    }

    public static native JavaScriptObject openWindow(String url, String name, String features) /*-{
        return $wnd.open(url,name,features);
    }-*/;



    private static void findURLAndMakeFull(WebPlotRequest wpr) {
        if (wpr.containsParam(WebPlotRequest.URL)) {
            String url= wpr.getURL();
            url= FFToolEnv.modifyURLToFull(url);
            wpr.setURL(url);
        }
    }


    private class FallbackLoader extends Timer {

        private WebPlotRequest wpr;
        private String target;

        private FallbackLoader(WebPlotRequest wpr, String target) {
            this.wpr = wpr;
            this.target = target;
        }

        @Override
        public void run() {
            findURLAndMakeFull(wpr);
            plotExternal(wpr,target);
        }
    }

    private class MyMessageListener implements CrossDocumentMessage.MessageListener {
        private WebPlotRequest wpr;
        private String winName;

        private MyMessageListener(WebPlotRequest wpr, String winName) {
            this.wpr = wpr;
            this.winName = winName;
        }

        public void message(String msg) {
            if (msg.equals(CrossDocumentMessage.ALIVE)) {
                fallbackLoader.cancel();
                JavaScriptObject target= loadedWindows.get(winName);
                xOrMsg.postMessage(target,wpr.toString());
            }
        }
    }


}

