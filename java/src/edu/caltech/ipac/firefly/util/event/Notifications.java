package edu.caltech.ipac.firefly.util.event;
/**
 * User: roby
 * Date: 10/3/12
 * Time: 1:40 PM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptException;
import edu.caltech.ipac.firefly.ui.JSLoad;

/**
 * @author Trey Roby
 */
public class Notifications {

    public static final boolean supported= isSupportedInternal();
    private static boolean scriptLoaded = false;
    private static boolean loadingStarted = false;

    private static boolean active = true;


    public static void loadJS(final InitComplete ic) {
        String js= GWT.getModuleBaseURL() + "js/fftools/FFNotifier.js";
        if (!scriptLoaded && !loadingStarted) {
            loadingStarted= true;
            new JSLoad(new JSLoad.Loaded(){
                public void allLoaded() {
                    scriptLoaded = true;
                    isSupportedInternal();
                    if (ic!=null) ic.done();
                }
            },js);
        }
    }

    public static void requestPermission() {
        if (active && supported)  {
            try {
                requestPermissionInternal();
                loadJS(null);
            } catch (JavaScriptException e) {
                e.printStackTrace();
            }
        }
    }

    public static void notify(final String title, final String msg) {
        notify(title,msg,null);
    }

    public static void notify(String title, final String msg, final UserClick click ) {
        final String notifyTitle= (title==null) ? "Background Manager" : title;
        if (active && supported) {
            try {
                if (scriptLoaded) {
                    notifyInternal(notifyTitle,msg);
                }
                else {
                    loadJS(new InitComplete() {
                        public void done() {
                            notifyInternal(notifyTitle,msg);
                        }
                    });
                }
            } catch (JavaScriptException e) {
                e.printStackTrace();
            }
        }
    }

    public static void itClosed() {
//        GwtUtil.showDebugMsg("Notification closed!!!!!!!!!!!!!!");
    }


    private static native boolean notifyInternal(String title, String msg) /*-{
        var func= $entry(@edu.caltech.ipac.firefly.util.event.Notifications::itClosed());
        var not= $wnd.makeFFNotifier(title,msg,func);
        not.send();
    }-*/;


    private static native boolean isSupportedInternal() /*-{
        if ($wnd.webkitNotifications) {
            return true;
        }
        else {
            return false;
        }
    }-*/;


    private static native void requestPermissionInternal() /*-{
        window.webkitNotifications.requestPermission();
    }-*/;


    private interface InitComplete {
        public void done();
    }

    private interface UserClick {
        public void click();
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
