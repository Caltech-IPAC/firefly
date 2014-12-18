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

