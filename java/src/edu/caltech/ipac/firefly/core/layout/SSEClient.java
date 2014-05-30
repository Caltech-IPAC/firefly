package edu.caltech.ipac.firefly.core.layout;
/**
 * User: roby
 * Date: 5/23/14
 * Time: 11:37 AM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Timer;
import edu.caltech.ipac.firefly.core.background.BackgroundReport;
import edu.caltech.ipac.firefly.ui.GwtUtil;
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

    public SSEClient() {
        createListener();
    }


    public void start() {
        activateComet();
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
        reactivateTimer.schedule(5000);
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
                GwtUtil.getClientLogger().log(Level.INFO, "onHeartbeat");
            }

            public void onRefresh() {
                GwtUtil.getClientLogger().log(Level.INFO, "onRefresh");
            }

            public void onMessage(List<? extends Serializable> messages) {
                String data= messages.get(0).toString();
                GwtUtil.getClientLogger().log(Level.INFO, "onMessage: " + data);
                String sAry[]= data.split("=====BEGIN:");
                if (sAry.length>1) {
                    try {
                        BackgroundReport report= BackgroundReport.parse(sAry[1]);
                        GwtUtil.getClientLogger().log(Level.INFO, "Parsed Report:" + sAry[1]);
                    } catch (Exception e) {
                        GwtUtil.getClientLogger().log(Level.INFO, "Failed to parse report:"+sAry[1]);
                    }
                }
            }
        };
    }


    private void activateComet() {
        client= new CometClient(COMET_URL,listener);
        client.start();

    }


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================



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
