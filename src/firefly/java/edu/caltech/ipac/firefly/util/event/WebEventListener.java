/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util.event;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;

/**
 * User: roby
 * Date: Dec 14, 2007
 * Time: 12:36:29 PM
 */
@JsExport
@JsType
public interface WebEventListener<DataType> {
    public void eventNotify(WebEvent<DataType> ev);
}