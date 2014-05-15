package edu.caltech.ipac.frontpage.core;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import edu.caltech.ipac.firefly.commands.OverviewHelpCmd;
import edu.caltech.ipac.firefly.core.AlertManager;
import edu.caltech.ipac.firefly.core.Creator;
import edu.caltech.ipac.firefly.core.DefaultRequestHandler;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.LoginManagerImpl;
import edu.caltech.ipac.firefly.core.LoginToolbar;
import edu.caltech.ipac.firefly.core.RequestHandler;
import edu.caltech.ipac.firefly.core.layout.BaseRegion;
import edu.caltech.ipac.firefly.core.layout.EmptyLayoutManager;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.task.CoreTask;
import edu.caltech.ipac.firefly.ui.LinkButtonFactory;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;

import java.util.HashMap;
import java.util.Map;

public class FrontpageEmbededCreator implements Creator {


    public FrontpageEmbededCreator() { }





    public LayoutManager makeLayoutManager() {
        EmptyLayoutManager lm = new EmptyLayoutManager();
        BaseRegion alertRegion = new BaseRegion(LayoutManager.ALERTS_REGION){
            @Override
            protected void adjust(SimplePanel main, SimplePanel holder) {
                main.setHeight("");
                holder.setHeight("");
            }
        };
        lm.addRegion(alertRegion);
        return lm;
    }

    public boolean isApplication() { return FrontpageUtils.isFrontpage(); }

    public String getLoadingDiv() { return null; }
    public String getAppDesc() { return null; }
    public Toolbar getToolBar() { return null; }
    public Image getMissionIcon() { return null; }



    public RequestHandler makeCommandHandler() { return new DefaultRequestHandler(); }

    public LoginManager makeLoginManager() {
        return new LoginManagerImpl() {
            @Override
            protected LoginToolbar makeToolbar() {
                return new LoginToolbar(new LinkButtonFactory("linkMouseOffColor","linkMouseOnColor","linkMouseOffColor"),false);
            }
        };
    }


    public String getAppName() { return "frontpage"; }

    public Map makeCommandTable() {
        HashMap<String, GeneralCommand> commands = new HashMap<String, GeneralCommand>();
        addCommand(commands, new OverviewHelpCmd());
        return commands;

    }
    private void addCommand(HashMap<String, GeneralCommand> maps, GeneralCommand c) {
        maps.put(c.getName(), c);
    }

    public AlertManager makeAlertManager() { return null;}      // not sure if alertmanager is needed.  defer to ToolbarPanel to instantiate.

    public ServerTask[] getCreatorInitTask() { return new ServerTask[] {new CoreTask.LoadProperties(true)}; }

}