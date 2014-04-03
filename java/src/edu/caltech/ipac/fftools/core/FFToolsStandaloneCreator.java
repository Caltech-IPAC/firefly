package edu.caltech.ipac.fftools.core;

import edu.caltech.ipac.firefly.commands.ImageSelectDropDownCmd;
import edu.caltech.ipac.firefly.commands.IrsaCatalogDropDownCmd;
import edu.caltech.ipac.firefly.commands.OverviewHelpCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.DefaultCreator;
import edu.caltech.ipac.firefly.core.DefaultRequestHandler;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.LoginManagerImpl;
import edu.caltech.ipac.firefly.core.MenuGeneratorV2;
import edu.caltech.ipac.firefly.core.RequestHandler;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.Vis;

import java.util.HashMap;
import java.util.Map;

public class FFToolsStandaloneCreator extends DefaultCreator {

    public static final String APPLICATION_MENU_PROP = "AppMenu";
    private static final boolean SUPPORT_LOGIN= false;
    private static final String CATALOG_NAME= IrsaCatalogDropDownCmd.COMMAND_NAME;
    private TabPlotWidgetFactory factory= new TabPlotWidgetFactory();
    private StandaloneUI aloneUI;
    IrsaCatalogDropDownCmd catalogDropDownCmd;

    public FFToolsStandaloneCreator() { }

    public boolean isApplication() { return true; }


    public StandaloneUI getStandaloneUI() { return aloneUI; }

    public Toolbar getToolBar() {
        // todo

        final Toolbar toolbar = new StandaloneToolBar();
        toolbar.setVisible(true);

        Vis.init(new Vis.InitComplete() {
            public void done() {
                Map<String, GeneralCommand> map = Application.getInstance().getCommandTable();
                map.putAll(AllPlots.getInstance().getCommandMap());
                MenuGeneratorV2.create(map).populateApplicationToolbar(APPLICATION_MENU_PROP, toolbar);
                Toolbar.RequestButton catalog = new Toolbar.RequestButton(CATALOG_NAME, IrsaCatalogDropDownCmd.COMMAND_NAME,
                                                                          "Catalogs", "Search and load IRSA catalog");
                toolbar.addButton(catalog, 0);
            }
        });
        return toolbar;
    }



    public Map makeCommandTable() {
        Application.getInstance().getProperties().setProperty("XYCharts.enableXYCharts", false+"");


        aloneUI= new StandaloneUI(factory);
        factory.setStandAloneUI(aloneUI);

        catalogDropDownCmd= new IrsaCatalogDropDownCmd() {
            @Override
            protected void catalogDropSearching() {
                aloneUI.initStartComplete();
            }

            @Override
            protected void doExecute() {
                super.doExecute();
            }
        };



        ImageSelectDropDownCmd isddCmd= new ImageSelectDropDownCmd() {

            @Override
            protected void doExecute() {
//                aloneUI.eventOpenImage();
                super.doExecute();
            }
        };
        isddCmd.setPlotWidgetFactory(factory);

        HashMap<String, GeneralCommand> commands = new HashMap<String, GeneralCommand>();
        addCommand(commands, catalogDropDownCmd);
        addCommand(commands, new OverviewHelpCmd());
        commands.put(FFToolsImageCmd.COMMAND, new FFToolsImageCmd(factory, aloneUI));
        commands.put(FFToolsExtCatalogCmd.COMMAND, new FFToolsExtCatalogCmd(aloneUI));
        commands.put(ImageSelectDropDownCmd.COMMAND_NAME, isddCmd);

        return commands;
    }


    private void addCommand(HashMap<String, GeneralCommand> maps, GeneralCommand c) {
        maps.put(c.getName(), c);
    }



    public RequestHandler makeCommandHandler() { return new DefaultRequestHandler(); }

    public LoginManager makeLoginManager() {
        LoginManagerImpl lm= null;
        if (SUPPORT_LOGIN) {
            lm= new LoginManagerImpl();
        }
        return lm;
    }

    public String getAppDesc() { return "Firefly Tools FITS/Catalog Viewer"; }
    public String getAppName() {
        return "fftools";
    }


    public LayoutManager makeLayoutManager() { return new FFToolsStandaloneLayoutManager(); }

    public String getLoadingDiv() { return "application"; }

    private class StandaloneToolBar extends Toolbar {
        @Override
        protected boolean getShouldExpandDefault() {
            return aloneUI.hasOnlyPlotResults() || aloneUI.isInitialStart();
        }

        @Override
        protected void expandDefault() {

            if (aloneUI.hasOnlyPlotResults()) {
                aloneUI.expandImage();
            }
            else if (aloneUI.isInitialStart()) {
                this.select(ImageSelectDropDownCmd.COMMAND_NAME);
            }
        }

        @Override
        protected boolean getShouldHideCloseOnDefaultTab() {
            return !aloneUI.hasResults();
        }


        @Override
        protected boolean isDefaultTabSelected() {
            String cmd= getSelectedCommand();

            return ((cmd==null || cmd.equals(CATALOG_NAME) || cmd.equals(ImageSelectDropDownCmd.COMMAND_NAME)));
        }
    }

    public ServerTask[] getCreatorInitTask() { return DefaultCreator.getDefaultCreatorInitTask(); }
}