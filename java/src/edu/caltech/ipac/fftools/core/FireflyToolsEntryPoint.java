package edu.caltech.ipac.fftools.core;
/**
 * User: roby
 * Date: 11/18/11
 * Time: 2:15 PM
 */


import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Window;
import edu.caltech.ipac.firefly.commands.ImageSelectDropDownCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.NetworkMode;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.util.BrowserUtil;

/**
 * @author Trey Roby
 */
public class FireflyToolsEntryPoint implements EntryPoint {

    private static final boolean USE_CORS_IF_POSSIBLE= false;

    public void onModuleLoad() {
        FFToolEnv.loadJS();
        boolean alone= isStandAloneApp();
        Application.setCreator(alone ? new FFToolsStandaloneCreator() : new FireflyToolsEmbededCreator());
        final Application app= Application.getInstance();
        boolean useCORS= BrowserUtil.getSupportsCORS() && USE_CORS_IF_POSSIBLE;
        app.setNetworkMode(alone ||  useCORS ? NetworkMode.RPC : NetworkMode.JSONP);
//        app.setNetworkMode(alone ? NetworkMode.RPC : NetworkMode.JSONP);
        FFToolEnv.setApiMode(!alone);

        Request home = null;
        if (alone) {
            home = new Request(ImageSelectDropDownCmd.COMMAND_NAME, "FFTools Start Cmd", true, false);
        }
        else {
            Window.addResizeHandler(new ResizeHandler() {
                public void onResize(ResizeEvent event) {
                    app.resize();
                }
            });
        }
        app.start(home, new AppReady());
    }

    public class AppReady implements Application.ApplicationReady {
        public void ready() {
            FFToolEnv.postInitialization();
            if (isStandAloneApp()) {
                Application.getInstance().hideDefaultLoadingDiv();
            }
        }
    }

    public static native boolean isStandAloneApp() /*-{
        if ("fireflyToolsApp" in $wnd) {
            return $wnd.fireflyToolsApp;
        }
        else {
            return false;
        }
    }-*/;

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
