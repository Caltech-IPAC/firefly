/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 10/14/15
 * Time: 9:14 AM
 */


import com.google.gwt.core.client.js.JsProperty;
import com.google.gwt.core.client.js.JsType;

/**
 * @author Trey Roby
 */
@JsType
public interface BandCI {

    @JsProperty int value();
    @JsProperty String key();

}
