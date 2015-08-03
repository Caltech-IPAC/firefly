/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.ui;
/**
 * User: roby
 * Date: 6/11/15
 * Time: 2:14 PM
 */


import com.google.gwt.core.client.js.JsType;

/**
 * @author Trey Roby
 */
@JsType
public interface JsExampleDialog {

    void showDialog();


    public static class Builder {
        public static native JsExampleDialog makeDialog() /*-{
            if ($wnd.firefly && $wnd.firefly.gwt) {
                return new $wnd.firefly.gwt.ExampleDialog();
            }
            else {
                return null;
            }
        }-*/;
    }
}
