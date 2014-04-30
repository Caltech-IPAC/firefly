package edu.caltech.ipac.fuse.core;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.task.IrsaAllDataSetsTask;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.fuse.commands.GeneralSearchCmd;
import net.zschech.gwt.comet.client.CometClient;
import net.zschech.gwt.comet.client.CometListener;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;


/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class FuseEntryPoint implements EntryPoint {
    /**
     * This is the entry point method.
     */
    public void onModuleLoad() {
        Application.setCreator(new FuseCreator());
        Application.getInstance().runOnStartup(new IrsaAllDataSetsTask());
        Request home = new Request(GeneralSearchCmd.COMMAND_NAME, "General Search", true, false);
        Application.getInstance().start(home, new AppReady());
    }

    public class AppReady implements Application.ApplicationReady {
        public void ready() {
            Application.getInstance().hideDefaultLoadingDiv();
            // uncomment to see server send events in log
//            startComet();
        }
    }

    //=======================================================
    //--------------- Experimental -----------------
    //=======================================================
    private static void startComet() {
        CometListener listener= new CometListener() {

            public void onConnected(int heartbeat) {
                GwtUtil.getClientLogger().log(Level.INFO, "heartbeat: "+heartbeat);
            }

            public void onDisconnected() {
                GwtUtil.getClientLogger().log(Level.INFO, "onDisconnected");
            }

            public void onError(Throwable e, boolean connected) {
                GwtUtil.getClientLogger().log(Level.INFO, "onError: connected: "+connected + " Message: " + e.getMessage());
            }

            public void onHeartbeat() {
                GwtUtil.getClientLogger().log(Level.INFO, "onHeartbeat");
            }

            public void onRefresh() {
                GwtUtil.getClientLogger().log(Level.INFO, "onRefresh");
            }

            public void onMessage(List<? extends Serializable> messages) {
                GwtUtil.getClientLogger().log(Level.INFO, "onMessage: " + messages.get(0));
            }
        };
        String COMET_URL = GWT.getModuleBaseURL() + "sticky/FireFly_events";

        CometClient client= new CometClient(COMET_URL,listener);
        client.start();

    }
    //=======================================================
    //--------------- End Experimental -----------------
    //=======================================================
}
