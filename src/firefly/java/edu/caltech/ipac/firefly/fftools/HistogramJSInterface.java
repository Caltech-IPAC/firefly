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

        for (String p : jspr.keySet()) {
            if (p.equals("source")) {
                String url =  FFToolEnv.modifyURLToFull(jspr.getParam("source"));
                jsonObj.put("source", new JSONString(url));
            } else if (p.equals("data")) {
                jsonObj.put("data", new JSONString(jspr.getParam("data")));
            } else {
                jsonObj.put(p, new JSONString(jspr.getParam(p)));
            }
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
