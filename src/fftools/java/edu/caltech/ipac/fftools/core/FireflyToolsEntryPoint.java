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
import edu.caltech.ipac.firefly.core.Creator;
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
    private Application.EventMode eventMode = Application.EventMode.WebSocket;
    private String appHelpName = null;


    public void setAppHelpName(String appHelpName) {
        this.appHelpName = appHelpName;
    }

    public void setEventMode(Application.EventMode eventMode) {
        this.eventMode = eventMode;
    }

    public void onModuleLoad() {
        start(IrsaPlusLsstDataSetsFactory.getInstance(),
              2,"generic_footer_minimal.html",
              ImageSelectDropDownCmd.COMMAND_NAME);
    }

    public void start(DataSetInfoFactory factory, int bannerOffset, String footerHtmlFile, String defCommandName) {

        Application.setEventMode(eventMode);  // -- uncomment for testing only, not ready  for production
        boolean useCORSForXS= BrowserUtil.getSupportsCORS() && USE_CORS_IF_POSSIBLE;
        Request home;

        FFToolsDisplayMode displayMode= FFToolsDisplayMode.valueOf(getDisplayMode());
        if (displayMode == FFToolsDisplayMode.embedded) {
            FFToolEnv.loadJS();
            Application.setCreator(new FireflyToolsEmbededCreator());
            Application.getInstance().setNetworkMode(useCORSForXS ? NetworkMode.RPC : NetworkMode.JSONP);
            FFToolEnv.setApiMode(true);
            home = null;
            Window.addResizeHandler(new ResizeHandler() {
                public void onResize(ResizeEvent event) {
                    Application.getInstance().resize();
                }
            });
        } else {
            Creator creator;
            if (displayMode == FFToolsDisplayMode.full) {
                creator= new FFToolsStandaloneCreator(displayMode, factory, bannerOffset,
                                                     footerHtmlFile, defCommandName);
                home = new Request(ImageSelectDropDownCmd.COMMAND_NAME, "Images", true, false);
            } else {
                creator= new FFToolsStandaloneCreator(displayMode, factory);
                home = new Request(FFToolsPushReceiveCmd.COMMAND);
            }
            Application.setCreator(creator);
            Application.getInstance().setNetworkMode(NetworkMode.RPC);
            FFToolEnv.setApiMode(false);
        }
        Application.getInstance().start(home, new AppReady());
    }

    public class AppReady implements Application.ApplicationReady {
        public void ready() {
            FFToolEnv.postInitialization();
            String displayMode = getDisplayMode();
            if (displayMode.equals("minimal") || displayMode.equals("full")) {
                Application.getInstance().hideDefaultLoadingDiv();
                if (appHelpName != null) {
                    Application.getInstance().getHelpManager().setAppHelpName(appHelpName);
                }
            } else {
                Application.getInstance().getHelpManager().setAppHelpName("fftools-api");
            }
        }
    }

    /**
     * Display mode can be one of 'full', 'minimal', or 'embedded'
     * @return  returns 'embedded' is one is not present.
     */
    public static native String getDisplayMode() /*-{
        if ("fireflyToolsMode" in $wnd) {
            return $wnd.fireflyToolsMode;
        }
        else {
            return "embedded";
        }
    }-*/;

}

