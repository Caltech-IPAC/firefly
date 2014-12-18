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

