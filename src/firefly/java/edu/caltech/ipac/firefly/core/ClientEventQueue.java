/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Timer;
import edu.caltech.ipac.firefly.core.background.BackgroundStatus;
import edu.caltech.ipac.firefly.data.ServerEvent;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.io.Serializable;
import java.util.logging.Level;

/**
 * Date: 4/17/15
 *
 * @author loi
 * @version $Id: $
 */
public class ClientEventQueue {
    private static Timer reactivateTimer= null;
    private static int retries = 0;

    public static void onOpen() {
        retries = 0;
        GwtUtil.getClientLogger().log(Level.INFO, "event manager started");
    }

    public static void onMessage(String msg) {
        try {
            GwtUtil.getClientLogger().log(Level.INFO, "onMessage: " + msg);

            ServerEvent sEvent = parseJsonEvent(msg);
            Name name = sEvent == null ? null : sEvent.getName();
            Serializable data = sEvent.getData();

            if (name == null) {
                GwtUtil.getClientLogger().log(Level.INFO, "Failed to evaluate: " + msg);
            }
            if (name.equals(Name.EVT_CONN_EST)) {
                JSONObject dataJ = JSONParser.parseStrict(sEvent.getData().toString()).isObject();
                String sEventInfo = dataJ.get("connID").isString().stringValue() + "_" + dataJ.get("channel").isString().stringValue();
                Cookies.setCookie("seinfo", sEventInfo);
                GwtUtil.getClientLogger().log(Level.INFO, "Websocket connection established: " + sEventInfo );
            } else if (data instanceof BackgroundStatus) {
                WebEvent<String> ev= new WebEvent<String>(ClientEventQueue.class,name,((BackgroundStatus)data).serialize());
                WebEventManager.getAppEvManager().fireEvent(ev);
                GwtUtil.getClientLogger().log(Level.INFO, "Event: Name:" + name.getName() + ", Data: " + ev.getData());
            } else {
                WebEvent<String> ev= new WebEvent<String>(ClientEventQueue.class,name,sEvent.getData().toString());
                WebEventManager.getAppEvManager().fireEvent(ev);
                GwtUtil.getClientLogger().log(Level.INFO, "Event: Name:" + name.getName() + ", Data: " + sEvent.getData());
            }
        } catch (Exception e) {
            GwtUtil.getClientLogger().log(Level.WARNING, "Exception interpreting incoming message: " + msg, e);
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
        try {
            $wnd.firefly.ClientEventManager.send(msg);
        } catch(ex) {
            console.log("Exception sending message: " + msg);
        }
    }-*/;

    public static native void start() /*-{

        if (!$wnd.firefly) {
            $wnd.firefly = {};
        }

        var l = window.location;
        var proto = (l.protocol === "https:") ? "wss://" : "ws://";
        var port = (l.port != 80 && l.port != 443) ? ":" + l.port : ""
        var pathname = l.pathname.substring(0, l.pathname.lastIndexOf('/'));
        var baseurl = proto + l.hostname + port + "/" + pathname;
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
        };
    }-*/;

    private native void closeConnection() /*-{
        $wnd.firefly.ClientEventManager.close();
    }-*/;

//====================================================================
//
//====================================================================

    private static ServerEvent parseJsonEvent(String msg) {
        try {
            JSONObject eventJ = JSONParser.parseStrict(msg).isObject();
            Name name = new Name(eventJ.get("name").isString().stringValue(), "");
            ServerEvent.Scope scope = ServerEvent.Scope.valueOf(eventJ.get("scope").isString().stringValue());
            ServerEvent.DataType dataType = eventJ.get("dataType") == null ? ServerEvent.DataType.STRING :
                    ServerEvent.DataType.valueOf(eventJ.get("dataType").isString().stringValue());
            Serializable data;
            String from =  eventJ.get("from") == null ? null : eventJ.get("from").toString();
            if (dataType == ServerEvent.DataType.BG_STATUS) {
                data = BackgroundStatus.parse(eventJ.get("data").isString().stringValue());
            } else if (dataType == ServerEvent.DataType.JSON) {
                data = eventJ.get("data").isObject().toString();
            } else {
                data = eventJ.get("data").isString().stringValue();
            }
            ServerEvent sEvent = new ServerEvent(name, scope, dataType, data);
            sEvent.setFrom(from);
            return sEvent;

        } catch (Exception e) {
            GwtUtil.getClientLogger().log(Level.WARNING, "Unable to parse json message into ServerEvent: " + msg, e);
            return null;
        }
    }

    private static void delayedReactivate() {
        if (retries < 10) {
            if (reactivateTimer==null) {
                reactivateTimer= new Timer() {
                    @Override
                    public void run() {
                        GwtUtil.getClientLogger().log(Level.INFO, "Attempting to reconnect with server events...");
                        start();
                        retries++;
                    }
                };
            }

            reactivateTimer.schedule(retries * 1000); // 10 sec
        }
    }

}
