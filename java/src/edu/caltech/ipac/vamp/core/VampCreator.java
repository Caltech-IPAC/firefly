package edu.caltech.ipac.vamp.core;

import edu.caltech.ipac.firefly.commands.OverviewHelpCmd;
import edu.caltech.ipac.firefly.commands.SearchCmd;
import edu.caltech.ipac.firefly.commands.ShowPreferencesCmd;
import edu.caltech.ipac.firefly.core.DefaultCreator;
import edu.caltech.ipac.firefly.core.DynRequestHandler;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.LoginManagerImpl;
import edu.caltech.ipac.firefly.core.RequestHandler;
import edu.caltech.ipac.vamp.commands.AvmSearchCmd;

import java.util.HashMap;
import java.util.Map;

public class VampCreator extends DefaultCreator {

    public VampCreator() {
    }

    public RequestHandler makeCommandHandler() {
        return new DynRequestHandler();
    }


    public LoginManager makeLoginManager() {
        return new LoginManagerImpl();
    }

    public Map makeCommandTable() {    // a Map<String, GeneralCommand> of commands, keyed by command_name

//        FinderChartCmd fcCmd = new FinderChartCmd();
//        XYPlotCmd xyPlotCmd = new XYPlotCmd();
//        XYCmd xyCmd = new XYCmd();

        HashMap<String, GeneralCommand> commands = new HashMap<String, GeneralCommand>();

//        addCommand(commands, new IrsaCatalogCmd());
//        addCommand(commands, new HistoryTagsCmd());//TODO - readd tag stuff
        addCommand(commands, new ShowPreferencesCmd());
        addCommand(commands, new SearchCmd());
        addCommand(commands, new AvmSearchCmd());
//        addCommand(commands, Application.getInstance().getBackgroundManager().makeCommand());
//        addCommand(commands, new AboutCmd());
        addCommand(commands, new OverviewHelpCmd());

//        TagCmd tagCmd = new TagCmd();
//        addCommand(commands, tagCmd);
//        addCommand(commands, new TagCmd.TagItCmd());

        return commands;
    }

    private void addCommand(HashMap<String, GeneralCommand> maps, GeneralCommand c) {
        maps.put(c.getName(), c);
    }


}
