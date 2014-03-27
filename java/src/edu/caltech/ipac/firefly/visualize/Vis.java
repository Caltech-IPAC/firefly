package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 6/16/11
 * Time: 3:24 PM
 */


import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;

/**
 * @author Trey Roby
 */
public class Vis {

    public static boolean initialized= false;
    public static void init(final InitComplete ic) { init(null,ic); }

    /**
     * Will force a asynchronous init to be call, then isInit() will return true.
     * @param mpw the MiniPlotWidget to init
     * @param ic callback handler to call the init is complete
     */
    public static void init(final MiniPlotWidget mpw, final InitComplete ic) {
        GWT.runAsync(new GwtUtil.DefAsync() {
            public void onSuccess() {
                if (initialized) {
                    if (mpw != null) {
                        mpw.initMPWAsync(ic);
                    }
                    else {
                        ic.done();
                    }
                }
                else {
                    initialized= true;
                    AllPlots.getInstance().initAllPlots();
                    if (mpw != null) mpw.initMPWAsync(ic);
                    else ic.done();
                }
            }
        });
    }


    public interface InitComplete {
        public void done();
    }

    public static boolean isInitialized() { return initialized; }
    public static void assertInitialized() { assert initialized; }


    public static ServerTask getInitAsServerTask() {
       return new ServerTask() {
           @Override
           public void onSuccess(Object result) { }

           @Override
           public void doTask(final AsyncCallback passAlong) {
               Vis.init(new InitComplete() {
                   public void done() {
                       passAlong.onSuccess("ok");
                   }
               });
           }
       };
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
