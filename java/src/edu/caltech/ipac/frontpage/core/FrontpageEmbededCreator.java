package edu.caltech.ipac.frontpage.core;

import edu.caltech.ipac.firefly.commands.OverviewHelpCmd;
import edu.caltech.ipac.firefly.core.AlertManager;
import edu.caltech.ipac.firefly.core.Creator;
import edu.caltech.ipac.firefly.core.DefaultCreator;
import edu.caltech.ipac.firefly.core.DefaultRequestHandler;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.LoginManagerImpl;
import edu.caltech.ipac.firefly.core.LoginToolbar;
import edu.caltech.ipac.firefly.core.RequestHandler;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.ui.LinkButtonFactory;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;

import java.util.HashMap;
import java.util.Map;

public class FrontpageEmbededCreator implements Creator {


    public FrontpageEmbededCreator() {
    }





    public LayoutManager makeLayoutManager() { return null; }

    public boolean isApplication() { return FrontpageUtils.isFrontpage(); }



    public Toolbar getToolBar() { return null; }
    public RequestHandler makeCommandHandler() { return new DefaultRequestHandler(); }
    public LoginManager makeLoginManager() {
        return new LoginManagerImpl() {
            @Override
            protected LoginToolbar makeToolbar() {
                return new LoginToolbar(new LinkButtonFactory("linkMouseOffColor","linkMouseOnColor","linkMouseOffColor"));
            }
        };
    }
    public String getLoadingDiv() { return null; }
    public String getAppDesc() { return null; }


    public String getAppName() { return "frontpage"; }

    public Map makeCommandTable() {
        HashMap<String, GeneralCommand> commands = new HashMap<String, GeneralCommand>();
        addCommand(commands,new ComponentsCmd());
        addCommand(commands,new AppMenuBarCmd());
        addCommand(commands, new OverviewHelpCmd());
        return commands;

    }
    private void addCommand(HashMap<String, GeneralCommand> maps, GeneralCommand c) {
        maps.put(c.getName(), c);
    }

    public AlertManager makeAlertManager() { return isApplication() ? new AlertManager() : null; }

    public ServerTask[] getCreatorInitTask() { return DefaultCreator.getDefaultCreatorInitTask(); }
}