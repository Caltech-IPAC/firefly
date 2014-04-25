package edu.caltech.ipac.firefly.server.sse;

import java.util.EventListener;

/**
 * User: roby
 * Date: Dec 14, 2007
 * Time: 12:36:29 PM
 */
public interface ServerSentEventListener extends EventListener {
    public void eventNotify(ServerSentEvent ev);
}