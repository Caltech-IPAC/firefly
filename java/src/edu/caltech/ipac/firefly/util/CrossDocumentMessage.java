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
