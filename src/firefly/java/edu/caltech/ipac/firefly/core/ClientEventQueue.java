/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import com.google.gwt.user.client.Timer;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.Constants;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.ServerSentEventNames;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.logging.Level;

/**
 * Date: 4/17/15
 *
 * @author loi
 * @version $Id: $
 */
public class ClientEventQueue {
    private static Timer reactivateTimer= null;

    public static void onOpen() {
        GwtUtil.getClientLogger().log(Level.INFO, "event manager started");
    }

    public static void onMessage(String msg) {
        GwtUtil.getClientLogger().log(Level.INFO, "onMessage: " + msg);
            String sAry[]= msg.split(Constants.SSE_SPLIT_TOKEN);
            if (sAry.length>0) {
                String data= sAry.length>1? sAry[1] : null;
                Name n= ServerSentEventNames.getEvName(sAry[0]);
                if (n!=null) {
                    WebEvent<String> ev= new WebEvent<String>(ClientEventQueue.class,n,data);
                    WebEventManager.getAppEvManager().fireEvent(ev);
                    GwtUtil.getClientLogger().log(Level.INFO, "Event: Name:" + n.getName() + ", Data: " + data);
                }
            }
            else {
                GwtUtil.getClientLogger().log(Level.INFO, "Failed to evaluate: " + msg);
            }
    }

    public static void onError(String error) {
        GwtUtil.getClientLogger().log(Level.INFO, "onError: " + error);
        delayedReactivate();
    }

    public static void onClose() {
        GwtUtil.getClientLogger().log(Level.INFO, "onClose");
        delayedReactivate();
    }

    public native void sendMessage(String msg) /*-{
        $wnd.firefly.ClientEventManager.send(msg);
    }-*/;

    public static native void start() /*-{

        if (!$wnd.firefly) {
            $wnd.firefly = {};
        }

        var l = window.location;
        var baseurl = ((l.protocol === "https:") ? "wss://" : "ws://") + l.hostname + (((l.port != 80) && (l.port != 443)) ? ":" + l.port : "") + "/" + l.pathname.split('/')[1];
        $wnd.firefly.ClientEventManager = new WebSocket(baseurl + "/sticky/firefly/events");

        $wnd.firefly.ClientEventManager.onopen = function(){
            @edu.caltech.ipac.firefly.core.ClientEventQueue::onOpen();
        };
        $wnd.firefly.ClientEventManager.onerror = function (error) {
            @edu.caltech.ipac.firefly.core.ClientEventQueue::onError(Ljava/lang/String;)(error);
        };
        $wnd.firefly.ClientEventManager.onclose = function() {
            @edu.caltech.ipac.firefly.core.ClientEventQueue::onClose();
        }
        $wnd.firefly.ClientEventManager.onmessage = function(msg){
            @edu.caltech.ipac.firefly.core.ClientEventQueue::onMessage(Ljava/lang/String;)(msg.data);
            var chatlog = $doc.getElementById("chatlog");
            if (chatlog) {
                chatlog.textContent += msg.data + "\n";
            }
        };
    }-*/;

    private native void closeConnection() /*-{
        $wnd.firefly.ClientEventManager.close();
    }-*/;


    private static void delayedReactivate() {
        if (reactivateTimer==null) {
            reactivateTimer= new Timer() {
                @Override
                public void run() {
                    GwtUtil.getClientLogger().log(Level.INFO, "reactivate");
                    start();
                }
            };
        }
        reactivateTimer.cancel();
        reactivateTimer.schedule(10000); // 10 sec
    }

}
