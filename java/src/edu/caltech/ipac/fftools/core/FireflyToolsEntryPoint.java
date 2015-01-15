/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
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
import edu.caltech.ipac.firefly.task.DataSetInfoFactory;
import edu.caltech.ipac.firefly.task.IrsaPlusLsstDataSetsFactory;
import edu.caltech.ipac.firefly.util.BrowserUtil;

/**
 * @author Trey Roby
 */
public class FireflyToolsEntryPoint implements EntryPoint {

    private static final boolean USE_CORS_IF_POSSIBLE= true;

    public void onModuleLoad() {
        start(IrsaPlusLsstDataSetsFactory.getInstance(),2,"generic_footer_minimal.html");
    }

    public void start(DataSetInfoFactory factory, int bannerOffset, String footerHtmlFile) {
        FFToolEnv.loadJS();
        boolean alone= isStandAloneApp();
        Application.setCreator(alone ? new FFToolsStandaloneCreator(factory,bannerOffset, footerHtmlFile) : new FireflyToolsEmbededCreator());
        final Application app= Application.getInstance();
        boolean useCORSForXS= BrowserUtil.getSupportsCORS() && USE_CORS_IF_POSSIBLE;
        app.setNetworkMode(alone ||  useCORSForXS ? NetworkMode.RPC : NetworkMode.JSONP);
        FFToolEnv.setApiMode(!alone);

        Request home = null;
        if (alone) {
            home = new Request(ImageSelectDropDownCmd.COMMAND_NAME, "Add/Modify Image...", true, false);
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
            else {
                Application.getInstance().getHelpManager().setAppHelpName("fftools-api");
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

