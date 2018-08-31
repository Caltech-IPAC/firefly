package edu.caltech.ipac.firefly.visualize.ui;

import jsinterop.annotations.JsType;
import com.google.gwt.json.client.JSONObject;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
public class ReactUIWrapper {

    @JsType public interface ReactJavaInterface {
        void createHistogram(JSONObject params, String div);
    }
}
