package edu.caltech.ipac.firefly.core;

import com.google.gwt.core.client.EntryPoint;

public class FireflyEntryPoint implements EntryPoint {
    public void onModuleLoad() {
        // firefly entry point... should add gwt init code here.
        loaded();
    };

    public static native void loaded() /*-{
        if ($wnd.ffgwt && $wnd.ffgwt.onLoaded) {
            $wnd.ffgwt.onLoaded();
        }
    }-*/;

}


