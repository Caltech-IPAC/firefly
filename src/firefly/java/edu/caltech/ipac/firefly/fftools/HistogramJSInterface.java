package edu.caltech.ipac.firefly.fftools;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import edu.caltech.ipac.firefly.data.JscriptRequest;
import edu.caltech.ipac.firefly.visualize.ui.ReactUIWrapper;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
public class HistogramJSInterface {
    public static void plotHistogram(JscriptRequest jspr, String div) {

        JSONObject jsonObj = new JSONObject();
        if (jspr.containsKey("descr")) {
            jsonObj.put("descr", new JSONString(jspr.getParam("descr")));
        }

        if (jspr.containsKey("source")) {
            String url =  FFToolEnv.modifyURLToFull(jspr.getParam("source"));
            jsonObj.put("source", new JSONString(url));
        } else if (jspr.containsKey("data")) {
            jsonObj.put("data", new JSONString(jspr.getParam("data")));
        }
        ReactUIWrapper.ReactJavaInterface reactInterface = getReactInterface();
        reactInterface.createHistogram(jsonObj, div);
    }

    public static native ReactUIWrapper.ReactJavaInterface getReactInterface() /*-{
        if ($wnd.firefly && $wnd.firefly.gwt) {
            return new $wnd.firefly.gwt.ReactJavaInterface();
        }
        else {
            return null;
        }
    }-*/;
}
