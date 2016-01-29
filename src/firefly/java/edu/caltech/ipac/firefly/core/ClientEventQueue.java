/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import com.google.gwt.core.client.js.JsExport;
import com.google.gwt.core.client.js.JsType;
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
@JsExport
@JsType
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
                WebEvent<String> ev= new WebEvent<String>(ClientEventQueue.class,name, String.valueOf(data));
                WebEventManager.getAppEvManager().fireEvent(ev);
                GwtUtil.getClientLogger().log(Level.INFO, "Event: Name:" + name.getName() + ", Data: " + sEvent.getData());
            }
        } catch (Exception e) {
            GwtUtil.getClientLogger().log(Level.WARNING, "Exception interpreting incoming message: " + msg, e);
        }
    }

    public static void onError(String error) {
        GwtUtil.getClientLogger().log(Level.INFO, "onError: " + error);
    }

    public static void onClose(String reason) {
        GwtUtil.getClientLogger().log(Level.INFO, "onClose");
    }

    public static void sendEvent(ServerEvent sevt) {
        sendMessage(sevt.toJsonString());
    }

    public static native void sendMessage(String msg) /*-{
        try {
            $wnd.firefly.ClientEventQueue.send(msg);
        } catch(ex) {
            console.log("Exception sending message: " + msg);
        }
    }-*/;

    private native void closeConnection() /*-{
        $wnd.firefly.ClientEventQueue.close();
    }-*/;

    public static native void start(String baseurl) /*-{

        var nRetries = 0;
        function doWSConnect() {
            if (!$wnd.firefly) {
                $wnd.firefly = {};
            }
            var l = window.location;
            if (baseurl == null) {
                var proto = (l.protocol === "https:") ? "wss://" : "ws://";
                var port = (l.port != 80 && l.port != 443) ? ":" + l.port : ""
                var pathname = l.pathname.substring(0, l.pathname.lastIndexOf('/'));
                baseurl = proto + l.hostname + port + "/" + pathname;
            } else {
                baseurl = baseurl.replace("https:", "wss:").replace("http:", "ws:");
            }
            var queryString = l.hash ? "?" + decodeURIComponent(l.hash.substring(1)) : "";

            console.log("Connecting to " + baseurl + "/sticky/firefly/events" + queryString);

            $wnd.firefly.ClientEventQueue = new WebSocket(baseurl + "/sticky/firefly/events" + queryString);
            var pingIt = null;

            $wnd.firefly.ClientEventQueue.onopen = function(){
                @edu.caltech.ipac.firefly.core.ClientEventQueue::onOpen();

                nRetries = 0;
                if (pingIt) clearInterval(pingIt);
                pingIt = setInterval(function () {$wnd.firefly.ClientEventQueue.send("")}, 5000);
            };

            $wnd.firefly.ClientEventQueue.onerror = function(event) {
                console.log("onerror:lsdfjdsklfj");
            };

            $wnd.firefly.ClientEventQueue.onclose = function(event) {
                console.log("onclose:" + event);
                @edu.caltech.ipac.firefly.core.ClientEventQueue::onClose(Ljava/lang/String;)(event.data);
            }

            $wnd.firefly.ClientEventQueue.onmessage = function(event){
                @edu.caltech.ipac.firefly.core.ClientEventQueue::onMessage(Ljava/lang/String;)(event.data);
            };
        }

        doWSConnect();
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
}
