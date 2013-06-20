package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 6/16/11
 * Time: 3:24 PM
 */


import com.google.gwt.core.client.GWT;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.visualize.draw.Drawer;

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
                if (initialized && Drawer.isAllLoaded()) {
                    if (mpw != null) {
                        mpw.initAsync(ic);
                    }
                    else {
                        ic.done();
                    }
                }
                else {
                    Drawer.loadJS(new Drawer.CompleteNotifier() {
                        public void done() {
                            initialized= true;
                            AllPlots.getInstance().init();
                            if (mpw != null) mpw.initAsync(ic);
                            else ic.done();
                        }
                    });
                }
            }
        });
    }


    public interface InitComplete {
        public void done();
    }

    public static boolean isInitialized() { return initialized && Drawer.isAllLoaded(); }
    public static void assertInitialized() { assert initialized && Drawer.isAllLoaded(); }
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
