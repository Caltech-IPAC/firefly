package edu.caltech.ipac.firefly.server.ws;

import edu.caltech.ipac.firefly.core.ResourceNotFoundException;
import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.util.AppProperties;

/**
 * Factories of ws handlers
 * Created by ejoliet on 6/15/17.
 */
public class WorkspaceFactory {

    static String protocolProp = AppProperties.getProperty("workspace.protocol.irsa", "webdav");

    /**
     * @return Handler based on protocol read from properties workspace.protocol.irsa
     * @throws WsException
     */
    public static WorkspaceHandler getWorkspaceHandler() {
        try {
            return getWorkspaceHandler(get(protocolProp));
        } catch (WsException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static WorkspaceHandler getWorkspaceHandler(WorkspaceManager.PROTOCOL p) {

        switch (p) {
            case LOCAL:
                return new LocalWorkspaceHandler();
            case WEBDAV:
                return new WebDAVWorkspaceHandler();
            case VOSPACE:
                throw new ResourceNotFoundException(p.name() + " not implemented yet ");
        }
        throw new ResourceNotFoundException(p.name() + " not found ");
    }

    private static WorkspaceManager.PROTOCOL get(String name) throws WsException {
        for (WorkspaceManager.PROTOCOL p : WorkspaceManager.PROTOCOL.values()) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        throw new WsException("Invalid protocol name loaded from properties: " + name);
    }

}
