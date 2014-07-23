package edu.caltech.ipac.fuse.core;

import edu.caltech.ipac.firefly.commands.OverviewHelpCmd;
import edu.caltech.ipac.firefly.commands.ShowPreferencesCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.DefaultCreator;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.fuse.commands.FuseDataSetCmd;
import edu.caltech.ipac.fuse.commands.FuseInventoryCmd;
import edu.caltech.ipac.fuse.commands.FuseQuickSearchCmd;
import edu.caltech.ipac.fuse.commands.TaskManagerCmd;
import edu.caltech.ipac.fuse.commands.TestLayoutCmd;

import java.util.HashMap;
import java.util.Map;

public class FuseCreator extends DefaultCreator {

    public static final String APP_DESC = "fuse";

    public FuseCreator() {
        setAppDesc(APP_DESC);
        PopoutWidget.setViewType(PopoutWidget.ViewType.GRID,true);
        AllPlots.getInstance().setDefaultExpandUseType(PopoutWidget.ExpandUseType.ALL);
        AllPlots.getInstance().setDefaultTiledTitle("");
    }

    public LayoutManager makeLayoutManager() {
        return new FuseLayoutManager();
    }

    public LoginManager makeLoginManager() {
//        return new LoginManagerImpl();
        return null;
    }

    public Map makeCommandTable() {

// a Map<String, GeneralCommand> of commands, keyed by command_name
        HashMap<String, GeneralCommand> commands = new HashMap<String, GeneralCommand>();

        addCommand(commands, new ShowPreferencesCmd());
        addCommand(commands, new OverviewHelpCmd());
        addCommand(commands, new FuseDataSetCmd());
        addCommand(commands, new FuseQuickSearchCmd());
        addCommand(commands, new FuseInventoryCmd());
        addCommand(commands, new TaskManagerCmd());
        commands.put("TriView", new TestLayoutCmd(FuseLayoutManager.VType.TRIVIEW));
        commands.put("TabImgView", new TestLayoutCmd(FuseLayoutManager.VType.IMAGE_TABLE));
        commands.put("TabPlotView", new TestLayoutCmd(FuseLayoutManager.VType.XYPLOT_TABLE));

//        addCommand(commands, new GeneralSearchCmd());
        Application.getInstance().getWidgetFactory().addCreator(
                FuseSearchDescResolver.ID, new FuseSearchDescResolver());

        return commands;
    }

    private void addCommand(HashMap<String, GeneralCommand> maps, GeneralCommand c) {
        maps.put(c.getName(), c);
    }


}
