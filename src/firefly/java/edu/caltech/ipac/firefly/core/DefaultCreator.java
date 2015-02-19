/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.commands.IrsaCatalogDropDownCmd;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.ResizableLayoutManager;
import edu.caltech.ipac.firefly.core.task.CoreTask;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.Map;

/**
 * Date: Nov 1, 2007
 *
 * @author loi
 * @version $Id: DefaultCreator.java,v 1.24 2012/08/09 01:09:27 loi Exp $
 */
public class DefaultCreator implements Creator {
    public static final String APPLICATION_MENU_PROP = "AppMenu";
    private String appDesc = "";
    private String appName = null;
    private Toolbar.RequestButton catalog= null;

    public Toolbar getToolBar() {

        Toolbar toolbar = new Toolbar();
        toolbar.setVisible(true);
        toolbar.setDefaultWidth("75px");

        MenuGeneratorV2.getDefaultInstance().populateApplicationToolbar(APPLICATION_MENU_PROP, toolbar);
        setupAddtlButtons(toolbar);
        return toolbar;
    }

    protected void setupAddtlButtons(final Toolbar toolbar) {

        boolean showCatalog = Application.getInstance().getProperties().getBooleanProperty("Catalog.button.show", true);

        if (showCatalog) {
            catalog = new Toolbar.RequestButton("Catalogs", IrsaCatalogDropDownCmd.COMMAND_NAME);
            WebEventManager.getAppEvManager().addListener(Name.SEARCH_RESULT_END, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    toolbar.addButton(catalog, 0);
                }
            });
            WebEventManager.getAppEvManager().addListener(Name.SEARCH_RESULT_START, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    toolbar.removeButton(catalog.getName());
                }
            });
        }
    }

    public Map makeCommandTable()     // a Map<String, GeneralCommand> of commands, keyed by command_name
    {
        // no default behavior.. return nothing.
        return null;
    }

    public RequestHandler makeCommandHandler() {
        return new DefaultRequestHandler();
    }

    public LoginManager makeLoginManager() {
        // no default Login Manager
        return null;
    }

    public LayoutManager makeLayoutManager() {
        return new ResizableLayoutManager();
    }

    public String getLoadingDiv() {
        return "application";
    }

    public boolean isApplication() { return true; }

    public String getAppDesc() {
        return appDesc;
    }

    public void setAppDesc(String desc) {
        appDesc = desc;
    }

    public String getAppName() {
        if (appName == null) {
            appName = GwtUtil.getGwtProperty("app-name");
            appName = appName == null ? appDesc : appName;
        }
        return appName;
    }

    public AlertManager makeAlertManager() { return makeDefaultAlertManager(); }

    public ServerTask[] getCreatorInitTask() { return getDefaultCreatorInitTask(); }

    public Image getMissionIcon() {
        return null;
    }

    public static AlertManager makeDefaultAlertManager() {
        return new AlertManager();
    }

    public static ServerTask[] getDefaultCreatorInitTask() {
        return new ServerTask[] {new CoreTask.LoadProperties()};
    }
}
