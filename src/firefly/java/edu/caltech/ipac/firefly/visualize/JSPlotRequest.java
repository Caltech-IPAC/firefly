/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * User: roby
 * Date: Jun 18, 2010
 * Time: 3:27:03 PM
 */


/**
 * @author Trey Roby
 */
public class JSPlotRequest extends JavaScriptObject {

    protected JSPlotRequest() {}

    public final native String getParam(String param) /*-{
          if (param in this) {
              return this[param];
          }
          else {
              return null;
          }
    }-*/;

    public final native boolean containsKey(String param) /*-{
        return param in this;
    }-*/;


}

