package edu.caltech.ipac.firefly.server.ws;

import edu.caltech.ipac.firefly.server.WorkspaceManager;

/**
 * API for WS handler that will handle every ws manager per user
 * Created by ejoliet on 6/15/17.
 */
public interface WorkspaceHandler {

    /**
     * Return ws manager using credentials {@link WsCredentials}
     *
     * @param cred {@link WsCredentials}
     * @return
     */
    WorkspaceManager withCredentials(WsCredentials cred);

}
