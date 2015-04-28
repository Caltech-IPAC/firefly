package edu.caltech.ipac.firefly.visualize.ui;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayNumber;
import com.google.gwt.core.client.js.JsType;

/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 *
 * @author tatianag
 */
public class ReactUIWrapper {

    @JsType public interface ReactJavaInterface {
        void createHistogram(JsArray<JsArrayNumber> data, String div);
    }
}
