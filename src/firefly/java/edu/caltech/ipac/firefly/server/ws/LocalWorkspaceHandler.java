package edu.caltech.ipac.firefly.server.ws;

import edu.caltech.ipac.firefly.server.LocalFSWorkspace;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.util.AppProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * API WS for client
 * Created by ejoliet on 6/15/17.
 */
public class LocalWorkspaceHandler implements WorkspaceHandler {

    String p = AppProperties.getProperty("workspace.protocol.irsa.webdav", "edu.caltech.ipac.firefly.server.WebDAVWorkspaceManager");

    /**
     * map userKey and ws,
     * TODO we don't want to create a new Wsmanager for every user access, do we?
     */
    Map<String, WorkspaceManager> wsPool;

    public LocalWorkspaceHandler() {
        wsPool = new HashMap<>();
    }

    public WorkspaceManager withCredentials(WsCredentials cred) {

        synchronized (cred) {
            String id = cred.getWsId();

            if (wsPool.containsKey(id)) {
                return wsPool.get(id);
            }
            WorkspaceManager ws = new LocalFSWorkspace();
            wsPool.put(ws.getCredentials().getWsId(), ws);
            return ws;
        }
    }
}
