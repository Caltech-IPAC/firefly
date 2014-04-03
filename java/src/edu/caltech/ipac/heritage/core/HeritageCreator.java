package edu.caltech.ipac.heritage.core;

import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.commands.FitsInputCmd;
import edu.caltech.ipac.firefly.commands.HistoryTagsCmd;
import edu.caltech.ipac.firefly.commands.IrsaCatalogDropDownCmd;
import edu.caltech.ipac.firefly.commands.OverviewHelpCmd;
import edu.caltech.ipac.firefly.commands.SearchCmd;
import edu.caltech.ipac.firefly.commands.ShowPreferencesCmd;
import edu.caltech.ipac.firefly.commands.TagCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.DefaultCreator;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.LoginManagerImpl;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.heritage.commands.AbstractSearchCmd;
import edu.caltech.ipac.heritage.commands.HeritageHomeCmd;
import edu.caltech.ipac.heritage.commands.IpacTableViewerCmd;
import edu.caltech.ipac.heritage.commands.SearchByCampaignCmd;
import edu.caltech.ipac.heritage.commands.SearchByDateCmd;
import edu.caltech.ipac.heritage.commands.SearchByNaifIDCmd;
import edu.caltech.ipac.heritage.commands.SearchByObserverCmd;
import edu.caltech.ipac.heritage.commands.SearchByPositionCmd;
import edu.caltech.ipac.heritage.commands.SearchByProgramCmd;
import edu.caltech.ipac.heritage.commands.SearchByRequestIDCmd;
import edu.caltech.ipac.heritage.commands.SearchIrsEnhancedCmd;
import edu.caltech.ipac.heritage.commands.SearchMOSCmd;

import java.util.HashMap;
import java.util.Map;

public class HeritageCreator extends DefaultCreator {

    public HeritageCreator() {
        setAppDesc("SHA");
        PopoutWidget.setViewType(PopoutWidget.ViewType.GRID,true);
        AllPlots.getInstance().setDefaultExpandUseType(PopoutWidget.ExpandUseType.ALL);
        AllPlots.getInstance().setDefaultTiledTitle("");
    }

    public Image getMissionIcon() {
        Image spitzerLogo = new Image("images/spitzer_mission_icon.jpg");
        return spitzerLogo;
    }

    public LayoutManager makeLayoutManager() {
        return new HeritageLayoutManager();
    }

    public LoginManager makeLoginManager() {
        return new LoginManagerImpl();
    }

    public Map makeCommandTable() {    // a Map<String, GeneralCommand> of commands, keyed by command_name

//        FinderChartCmd fcCmd = new FinderChartCmd();
//        XYPlotCmd xyPlotCmd = new XYPlotCmd();
//        XYCmd xyCmd = new XYCmd();

        HashMap<String, GeneralCommand> commands = new HashMap<String, GeneralCommand>();

        FitsInputCmd fitsInputCmd = new FitsInputCmd("Read FITS File", "Read a FITS file", true);
//        addCommand(commands, new IrsaCatalogCmd());
        addCommand(commands, new IrsaCatalogDropDownCmd());
        addCommand(commands, fitsInputCmd);
        addCommand(commands, new HistoryTagsCmd());
        addCommand(commands, new ShowPreferencesCmd());
        addCommand(commands, new SearchByCampaignCmd());
        addCommand(commands, new SearchByDateCmd());
        addCommand(commands, new SearchByNaifIDCmd());
        addCommand(commands, new SearchByObserverCmd());
        addCommand(commands, new SearchByPositionCmd());
        addCommand(commands, new SearchByProgramCmd());
        addCommand(commands, new SearchByRequestIDCmd());
        addCommand(commands, new SearchIrsEnhancedCmd());
        addCommand(commands, new SearchMOSCmd());
        addCommand(commands, new AbstractSearchCmd());
        addCommand(commands, new SearchCmd());
        addCommand(commands, new IpacTableViewerCmd("Ipac Table Viewer", "IPAC table viewer", true));
        addCommand(commands, new HeritageHomeCmd());
//        addCommand(commands, Application.getInstance().getBackgroundManager().makeCommand());
//        addCommand(commands, new AboutCmd());
        addCommand(commands, new OverviewHelpCmd());
        TagCmd tagCmd = new TagCmd();
        tagCmd.setTagReslover(new HeritageTagResolver());
        addCommand(commands,
                tagCmd);
        addCommand(commands, new TagCmd.TagItCmd());

        Application.getInstance().getWidgetFactory().addCreator(
                    getAppDesc() + "-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX, new HeritageSearchDescResolver());

        return commands;
    }

    private void addCommand(HashMap<String, GeneralCommand> maps, GeneralCommand c) {
        maps.put(c.getName(), c);
    }


}
