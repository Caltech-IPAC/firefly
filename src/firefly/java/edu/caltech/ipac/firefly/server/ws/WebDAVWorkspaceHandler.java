package edu.caltech.ipac.firefly.server.ws;

import edu.caltech.ipac.firefly.server.WorkspaceManager;
import edu.caltech.ipac.util.AppProperties;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * API manager/handler for webdav client
 * Class implementation of manager passed as property¬
 * Created by ejoliet on 6/15/17.
 */
public class WebDAVWorkspaceHandler implements WorkspaceHandler {



    /**
     * map userKey and ws,
     * TODO we don't want to create a new Wsmanager for every user access, do we?
     */
    Map<String, WorkspaceManager> wsPool;

    public WebDAVWorkspaceHandler() {
        wsPool = new HashMap<>();
    }

    public WorkspaceManager withCredentials(WsCredentials cred) {

        synchronized (cred) {
            String id = cred.getWsId();
            String protocol = AppProperties.getProperty("workspace.protocol", "webdav");
            String managerClass = AppProperties.getProperty("workspace.protocol."+protocol.toLowerCase(), "edu.caltech.ipac.firefly.server.WebDAVWorkspaceManager");

            //TODO The problem is how do i know that the cookkie/session is authtenticated at this point?
            /*if (wsPool.containsKey(id)) {
                return wsPool.get(id);
            }*/
            WorkspaceManager ws = null;//new WebDAVWorkspaceManager(cred.getWsId(), cred.getCookies());
            try {
                Class clazz = Class.forName(managerClass);
                Constructor<?> ctor = clazz.getConstructor(WsCredentials.class);
                ws = (WorkspaceManager) ctor.newInstance(new Object[]{cred});
                wsPool.put(cred.getWsId(), ws);
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

            return ws;
        }
    }
}
