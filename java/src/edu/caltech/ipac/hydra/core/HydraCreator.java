package edu.caltech.ipac.hydra.core;

import edu.caltech.ipac.firefly.commands.CatalogSearchCmd;
import edu.caltech.ipac.firefly.commands.DemoSearch2MassPosCmd;
import edu.caltech.ipac.firefly.commands.DynHomeCmd;
import edu.caltech.ipac.firefly.commands.FitsInputCmd;
import edu.caltech.ipac.firefly.commands.HistoryCmd;
import edu.caltech.ipac.firefly.commands.IrsaCatalogDropDownCmd;
import edu.caltech.ipac.firefly.commands.OverviewHelpCmd;
import edu.caltech.ipac.firefly.commands.SearchCmd;
import edu.caltech.ipac.firefly.commands.ShowPreferencesCmd;
import edu.caltech.ipac.firefly.commands.TagCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.DefaultCreator;
import edu.caltech.ipac.firefly.core.DynRequestHandler;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.LoginManagerImpl;
import edu.caltech.ipac.firefly.core.RequestHandler;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;

import java.util.HashMap;
import java.util.Map;

public class HydraCreator extends DefaultCreator {

    public HydraCreator() {
    }

    public RequestHandler makeCommandHandler() {
        return new DynRequestHandler();
    }


    public LayoutManager makeLayoutManager() {
        return new HydraLayoutManager();
    }

    public LoginManager makeLoginManager() {
        return new LoginManagerImpl();
    }

    public Map makeCommandTable() {    // a Map<String, GeneralCommand> of commands, keyed by command_name

        HashMap<String, GeneralCommand> commands = new HashMap<String, GeneralCommand>();
        addCommand(commands, new IrsaCatalogDropDownCmd());
        addCommand(commands, new HistoryCmd());
        addCommand(commands, new ShowPreferencesCmd());
        addCommand(commands, new SearchCmd());
        addCommand(commands, new CatalogSearchCmd());
        addCommand(commands, new DemoSearch2MassPosCmd());
        addCommand(commands, new DynHomeCmd());
//        addCommand(commands, new AboutCmd());
        addCommand(commands, new OverviewHelpCmd());

        TagCmd tagCmd = new TagCmd();
        addCommand(commands, tagCmd);
        addCommand(commands, new TagCmd.TagItCmd());

        FitsInputCmd fitsInputCmd = new FitsInputCmd("Read Fits File", "read a fits file", true);
        addCommand(commands, fitsInputCmd);

        Application.getInstance().getWidgetFactory().addCreator(
                    "wise-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX, new WiseSearchDescResolver());

        Application.getInstance().getWidgetFactory().addCreator(
                    "ptf-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX, new PtfSearchDescResolver());

        Application.getInstance().getWidgetFactory().addCreator(
                    "planck-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX, new PlanckSearchDescResolver());
        
        Application.getInstance().getWidgetFactory().addCreator(
                    "lcogt-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX, new LcogtSearchDescResolver());

        Application.getInstance().getWidgetFactory().addCreator(
                    "lsst-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX, new LsstSearchDescResolver());

        Application.getInstance().getWidgetFactory().addCreator(
                "resultViewer-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX, new ResultViewerDescResolver());
        
        Application.getInstance().getWidgetFactory().addCreator(
                "finderChart-" + WidgetFactory.SEARCH_DESC_RESOLVER_SUFFIX, new FinderChartDescResolver());


        return commands;
    }

    private void addCommand(HashMap<String, GeneralCommand> maps, GeneralCommand c) {
        maps.put(c.getName(), c);
    }

}
