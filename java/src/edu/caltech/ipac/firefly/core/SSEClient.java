package edu.caltech.ipac.firefly.core;
/**
 * User: roby
 * Date: 5/23/14
 * Time: 11:37 AM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.event.Name;
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
        GwtUtil.getClientLogger().log(Level.INFO, "onMessage: " + message);
        if (message.endsWith(Name.HEART_BEAT.getName())) {
           // do nothing
        }
        else {
            String sAry[]= message.split("=====BEGIN:");
            if (sAry.length>1) {
                try {
                    BackgroundStatus bgStat= BackgroundStatus.parse(sAry[1]);
                    if (bgStat!=null)  Application.getInstance().getBackgroundMonitor().setStatus(bgStat);
                } catch (Exception e) {
                    GwtUtil.getClientLogger().log(Level.WARNING, "Failed to parse bgStat:"+sAry[1],e);
                }
            }
        }
        lastReceivedEvent= System.currentTimeMillis();
    }



}

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
 * OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS, 
 * HOWEVER USED.
 * 
 * IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE 
 * FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL 
 * OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO 
 * PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE 
 * ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
 * 
 * RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE 
 * AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR 
 * ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE 
 * OF THE SOFTWARE. 
 */
