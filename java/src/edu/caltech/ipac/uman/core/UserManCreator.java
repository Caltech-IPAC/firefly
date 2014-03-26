package edu.caltech.ipac.uman.core;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.OverviewHelpCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.DefaultCreator;
import edu.caltech.ipac.firefly.core.DefaultRequestHandler;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.LoginManagerImpl;
import edu.caltech.ipac.firefly.core.RequestHandler;
import edu.caltech.ipac.firefly.core.layout.AbstractLayoutManager;
import edu.caltech.ipac.firefly.core.layout.BaseRegion;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.core.layout.ResizableLayoutManager;
import edu.caltech.ipac.firefly.data.Version;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.panels.SearchPanel;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.uman.commands.AccessCmd;
import edu.caltech.ipac.uman.commands.ChangeEmailCmd;
import edu.caltech.ipac.uman.commands.ChangePasswordCmd;
import edu.caltech.ipac.uman.commands.ProfileCmd;
import edu.caltech.ipac.uman.commands.RegistrationCmd;
import edu.caltech.ipac.uman.commands.RolesCmd;
import edu.caltech.ipac.uman.data.UmanConst;

import java.util.HashMap;
import java.util.Map;

/**
 * Date: 3/9/12
 *
 * @author loi
 * @version $Id: UserManCreator.java,v 1.9 2012/11/19 22:05:43 loi Exp $
 */
public class UserManCreator extends DefaultCreator {

    public UserManCreator() {
    }

    public LoginManager makeLoginManager() {
        return new LoginManagerImpl();
    }

    @Override
    public RequestHandler makeCommandHandler() {
        DefaultRequestHandler handler = (DefaultRequestHandler) super.makeCommandHandler();
        handler.setDoRecordHistory(false);
        return handler;
    }

    public Map makeCommandTable() {    // a Map<String, GeneralCommand> of commands, keyed by command_name

        HashMap<String, GeneralCommand> commands = new HashMap<String, GeneralCommand>();
        addCommand(commands, new RegistrationCmd());
        addCommand(commands, new ProfileCmd());
        addCommand(commands, new ChangeEmailCmd());
        addCommand(commands, new ChangePasswordCmd());
        return commands;
    }

    protected void addCommand(HashMap<String, GeneralCommand> maps, GeneralCommand c) {
        maps.put(c.getName(), c);
    }

    @Override
    public LayoutManager makeLayoutManager() {
        return new UmanLayoutManager(800, 600);
    }

    @Override
    public Toolbar getToolBar() {
        return null;
    }

    @Override
    public String getAppDesc() {
        return "Account";
    }

    class UmanLayoutManager extends AbstractLayoutManager {
        private DockPanel mainPanel = new DockPanel();

        protected UmanLayoutManager(int minWidth, int minHeight) {
            super(minWidth, minHeight);
        }


        @Override
        public Widget getDisplay() {
            return mainPanel;
        }

        @Override
        public void layout(String rootId) {
            init();


            Region loginRegion = Application.getInstance().getLoginManager().makeLoginRegion();

            VerticalPanel lp = new VerticalPanel();
            lp.add(loginRegion.getDisplay());
            lp.setCellHeight(loginRegion.getDisplay(), "20px");
            lp.setStyleName("user-info");
            RootPanel.get("user-info").add(lp);

            Widget north, center;
            center = getResult().getDisplay();
            north = getForm().getDisplay();

            Widget footer = getFooter().getDisplay();
            if (north != null) {
                mainPanel.add(north, DockPanel.NORTH);
            }

            if (center != null) {
                mainPanel.add(center, DockPanel.CENTER);
                GwtUtil.setStyle(center, "padding", "0 10px");
                center.setSize("","");
            }

            if (footer != null) {
                mainPanel.add(footer, DockPanel.SOUTH);
                GwtUtil.setStyle(footer, "padding", "0 10px");
            }

            if (rootId != null) {
                RootPanel root = RootPanel.get(rootId);
                if (root == null) {
                    throw new RuntimeException("Application is not setup correctly; unable to find " + rootId);
                }
                root.add(mainPanel);
                root.setHeight("100%");
            } else {
                RootPanel.get().add(mainPanel);
            }
            mainPanel.setSize("100%", "100%");

            SearchPanel.getInstance().addStyleName("shadow");

            resize();
        }

        @Override
        protected Region makeFooter() {
            final Label vers = new Label();
            GwtUtil.setStyle(vers, "color", "#BBB");
            BaseRegion r = new BaseRegion(FOOTER_REGION, vers);
            Application.getInstance().findVersion(new AsyncCallback<Version>() {
                        public void onFailure(Throwable throwable) {
                            vers.setText("Version: unknown");
                        }
                        public void onSuccess(Version version) {
                            vers.setText("Version: " + version.toString());
                        }
                    });
            return r;
        }
    }
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
