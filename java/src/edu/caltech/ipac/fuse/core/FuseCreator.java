package edu.caltech.ipac.fuse.core;

import edu.caltech.ipac.firefly.commands.CatalogSearchCmd;
import edu.caltech.ipac.firefly.commands.OverviewHelpCmd;
import edu.caltech.ipac.firefly.commands.ShowPreferencesCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.DefaultCreator;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.LoginManagerImpl;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.fuse.commands.GeneralSearchCmd;

import java.util.HashMap;
import java.util.Map;

public class FuseCreator extends DefaultCreator {


    public FuseCreator() {
        setAppDesc("fuse");
        PopoutWidget.setViewType(PopoutWidget.ViewType.GRID,true);
        AllPlots.getInstance().setDefaultExpandUseType(PopoutWidget.ExpandUseType.ALL);
        AllPlots.getInstance().setDefaultTiledTitle("");
    }

    public LayoutManager makeLayoutManager() {
        return new FuseLayoutManager();
    }

    public LoginManager makeLoginManager() {
        return new LoginManagerImpl();
    }

    public Map makeCommandTable() {

// a Map<String, GeneralCommand> of commands, keyed by command_name
        HashMap<String, GeneralCommand> commands = new HashMap<String, GeneralCommand>();

        addCommand(commands, new ShowPreferencesCmd());
        addCommand(commands, new CatalogSearchCmd());
        addCommand(commands, new OverviewHelpCmd());
        addCommand(commands, new GeneralSearchCmd());
        Application.getInstance().getWidgetFactory().addCreator(
                    getAppDesc() + "-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX, new FuseSearchDescResolver());

        return commands;
    }

    private void addCommand(HashMap<String, GeneralCommand> maps, GeneralCommand c) {
        maps.put(c.getName(), c);
    }


}
