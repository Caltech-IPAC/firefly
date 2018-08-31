/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.util.event;

import jsinterop.annotations.JsType;

/**
 * User: roby
 * Date: Dec 14, 2007
 * Time: 12:36:29 PM
 */

@JsType
public interface WebEventListener<DataType> {
    public void eventNotify(WebEvent<DataType> ev);
}