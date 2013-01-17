package edu.caltech.ipac.firefly.util.event;

import java.util.EventListener;

/**
 * User: roby
 * Date: Dec 14, 2007
 * Time: 12:36:29 PM
 */
public interface WebEventListener<DataType> extends EventListener {
    public void eventNotify(WebEvent<DataType> ev);
}