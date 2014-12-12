package edu.caltech.ipac.client;

import java.util.EventListener;

/**
 * User: roby
 * Date: Dec 14, 2007
 * Time: 12:36:29 PM
 */
public interface ClientEventListener extends EventListener {
    public void eventNotify(ClientEvent ev);
}