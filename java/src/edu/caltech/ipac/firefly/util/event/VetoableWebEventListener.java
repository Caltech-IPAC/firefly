package edu.caltech.ipac.firefly.util.event;

import java.util.EventListener;

/**
 * User: roby
 * Date: Dec 14, 2007
 * Time: 12:36:29 PM
 */
public interface VetoableWebEventListener extends EventListener {
    public void vetoableEventNotify(WebEvent ev) throws VetoWebEventException;
}