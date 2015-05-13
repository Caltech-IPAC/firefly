/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;
/**
 * User: roby
 * Date: 5/23/14
 * Time: 11:37 AM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.Constants;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.ServerSentEventNames;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import net.zschech.gwt.comet.client.CometClient;
import net.zschech.gwt.comet.client.CometListener;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Trey Roby
 */
public class SSEClient {

    private final long widowID= System.currentTimeMillis();
    private String COMET_URL = GWT.getModuleBaseURL() + "sticky/FireFly_events?winId="+widowID;
    private CometClient client;
    CometListener listener;
    private Timer reactivateTimer= null;
    private long lastReceivedEvent= System.currentTimeMillis();
    private long MAX_EVENT_INTERVAL= 90*1000;

    private static SSEClient instance= null;

    private SSEClient() {
        createListener();
        activateComet();
    }


    public static void start() {
        if (instance==null) {
            instance= new SSEClient();
        }
    }

    private void delayedReactivate() {
        if (reactivateTimer==null) {
            reactivateTimer= new Timer() {
                @Override
                public void run() {
                    GwtUtil.getClientLogger().log(Level.INFO, "reactivate");
                    activateComet();
                }
            };
        }
        reactivateTimer.cancel();
        if (client!=null) {
            client.stop();
        }
        GwtUtil.getClientLogger().log(Level.INFO, "polling all MonitorItems");
        Application.getInstance().getBackgroundMonitor().pollAll();
        reactivateTimer.schedule(10000); // 10 sec
    }


    private void createListener() {

        listener= new CometListener() {

            public void onConnected(int heartbeat) {
                GwtUtil.getClientLogger().log(Level.INFO, "heartbeat: "+heartbeat);
            }

            public void onDisconnected() {
                GwtUtil.getClientLogger().log(Level.INFO, "onDisconnected");
                delayedReactivate();
            }

            public void onError(Throwable e, boolean connected) {
                GwtUtil.getClientLogger().log(Level.INFO, "onError: connected: "+connected + " Message: " + e.getMessage());
                delayedReactivate();
            }

            public void onHeartbeat() {
                if (lastReceivedEvent+MAX_EVENT_INTERVAL<System.currentTimeMillis())  {
                    GwtUtil.getClientLogger().log(Level.INFO, "onHeartbeat: no event heartbeat, restarting");
                    delayedReactivate();
                }
            }

            public void onRefresh() {
                GwtUtil.getClientLogger().log(Level.INFO, "onRefresh");
            }

            public void onMessage(List<? extends Serializable> messages) {
                for (Object m : messages) evaluateMessage(m.toString());
            }
        };
    }




    private void activateComet() {
        lastReceivedEvent= System.currentTimeMillis();
        client= new CometClient(COMET_URL,listener);
        client.start();

    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void evaluateMessage(String message) {
        if (message.endsWith(Name.EVT_CONN_EST.getName())) {
           // do nothing
            GwtUtil.getClientLogger().log(Level.INFO, "Heartbeat");
        }
        else {
            String sAry[]= message.split(Constants.SSE_SPLIT_TOKEN);
            if (sAry.length>0) {
                String data= sAry.length>1? sAry[1] : null;
                Name n= ServerSentEventNames.getEvName(sAry[0]);
                if (n!=null) {
                    WebEvent<String> ev= new WebEvent<String>(this,n,data);
                    WebEventManager.getAppEvManager().fireEvent(ev);
                }
                GwtUtil.getClientLogger().log(Level.INFO, "Event: Name:" + n.getName()+ ", Data: "+data);
            }
            else {
                GwtUtil.getClientLogger().log(Level.INFO, "Failed to evaluate: " +message);
            }
        }
        lastReceivedEvent= System.currentTimeMillis();
    }



}

