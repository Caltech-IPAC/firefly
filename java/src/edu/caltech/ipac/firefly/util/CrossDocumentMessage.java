package edu.caltech.ipac.firefly.util;
/**
 * User: roby
 * Date: 4/8/13
 * Time: 11:10 AM
 */


import com.google.gwt.core.client.JavaScriptObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class CrossDocumentMessage {


    private static final Map<String,CrossDocumentMessage> allInstances= new HashMap<String, CrossDocumentMessage>(5);
    public static final String ALIVE = "Alive";
    public static final String ALIVE_CHECK = "AliveCheck";
    private String targetOrigin;
    private MessageListener listener;
    private String key= this.hashCode()+"";


    public CrossDocumentMessage(String targetOrigin) {
        this(targetOrigin, null);

    }

    public CrossDocumentMessage(String targetOrigin, MessageListener  listener) {
        this.targetOrigin = targetOrigin;
        this.listener = listener;
        allInstances.put(key,this);
        listen(targetOrigin,key);
    }


    public void setListener(MessageListener  listener) {
        this.listener= listener;
    }

    public void sendAlive(JavaScriptObject target) {
        postMessage(target, ALIVE_CHECK);
    }

    public void postMessage(JavaScriptObject target, String message)  {
        postMessage(target, message, targetOrigin);
    }

    public static void postMessage(JavaScriptObject target, String message, String targetOrigin)  {
        if (isSupported()) {
            doPost(target,message,targetOrigin);
        }
    }
    public void processMessage(JavaScriptObject source, String data, String origin) {
        if (origin.equals(targetOrigin)) {
            if (data.equals(ALIVE_CHECK)) {
                postMessage(source,ALIVE,origin);
            }
            else {
                if (listener!=null) listener.message(data);
            }
        }
    }


    public static void messageReceived(String key, JavaScriptObject source, String data, String origin) {
        if (allInstances.containsKey(key)) {
            allInstances.get(key).processMessage(source, data,origin);
        }
    }
//    public static void showDM(String msg, JavaScriptObject source) {
//        PopupUtil.showInfo(msg+ ":"+ source.toString());
//    }

    public static native void listen(String targetOrigin,
                                     String key)  /*-{
        var m=   $entry(@edu.caltech.ipac.firefly.util.CrossDocumentMessage::messageReceived(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;Ljava/lang/String;));

        $wnd.firefly.enableDocListening(m,key);
    }-*/;



    public static native void doPost(JavaScriptObject target,
                                     String msg,
                                     String targetOrigin)  /*-{

        $wnd.firefly.postMessage(target,msg,targetOrigin);
    }-*/;


    public static native boolean isSupported()  /*-{
        return "postMessage" in $wnd;
    }-*/;


    public interface MessageListener {

        public void message(String msg);
    }
}

