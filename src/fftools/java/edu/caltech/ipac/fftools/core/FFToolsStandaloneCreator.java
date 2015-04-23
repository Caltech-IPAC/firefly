/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.fftools.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Image;
import edu.caltech.ipac.firefly.commands.AnyDataSetCmd;
import edu.caltech.ipac.firefly.commands.ImageSelectCmd;
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
import edu.caltech.ipac.firefly.core.task.CoreTask;
import edu.caltech.ipac.firefly.data.fuse.ConverterStore;
import edu.caltech.ipac.firefly.data.fuse.DatasetInfoConverter;
import edu.caltech.ipac.firefly.task.DataSetInfoFactory;
import edu.caltech.ipac.firefly.ui.ServerTask;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.ui.panels.ToolbarDropdown;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.ui.ImageSelectDropDown;

import java.util.HashMap;
import java.util.Map;

public class FFToolsStandaloneCreator extends DefaultCreator {

    public static final String APPLICATION_MENU_PROP = "AppMenu";
    private static final boolean SUPPORT_LOGIN= false;
    private static final String CATALOG_NAME= IrsaCatalogDropDownCmd.COMMAND_NAME;
//    private TabPlotWidgetFactory factory= new TabPlotWidgetFactory();
    private final static String FIREFLY_LOGO= GWT.getModuleBaseURL()+  "images/fftools-logo-offset-small-75x75.png";
    private StandaloneUI aloneUI;
    ImageSelectDropDownCmd isddCmd;
    private final int bannerOffset;
    private final String footerHtmlFile;
    private final String defaultCmdName;

    public FFToolsStandaloneCreator(DataSetInfoFactory factory, int bannerOffset, String footerHtmlFile, String defaultCmdName) {
        if (factory!=null) Application.setDataSetFactory(factory);
        this.bannerOffset= bannerOffset;
        this.footerHtmlFile= footerHtmlFile;
        this.defaultCmdName= defaultCmdName;
    }

    public boolean isApplication() { return true; }

    public Image getMissionIcon() {
        return new Image(FIREFLY_LOGO);
    }

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
//                Toolbar.RequestButton catalog = new Toolbar.RequestButton(CATALOG_NAME, IrsaCatalogDropDownCmd.COMMAND_NAME,
//                                                                          "Catalogs", "Search and load IRSA catalog");
//                toolbar.addButton(catalog, 0);
                ImageSelectCmd cmd= (ImageSelectCmd)AllPlots.getInstance().getCommand(ImageSelectCmd.CommandName);
                cmd.setUseDropdownCmd(isddCmd);


                DatasetInfoConverter ddConv= ConverterStore.get(ConverterStore.DYNAMIC);
                ImageSelectPanelDynPlotter plotter= new ImageSelectPanelDynPlotter(ddConv.getPlotData());
                ImageSelectDropDown dropDown= new ImageSelectDropDown(null,true,plotter);
                isddCmd.addImageSelectDropDown(ddConv,dropDown);
                isddCmd.setDatasetInfoConverterForCreation(dropDown);
            }
        });
        return toolbar;
    }



    public Map makeCommandTable() {
        Application.getInstance().getProperties().setProperty("XYCharts.enableXYCharts", false+"");

        aloneUI= new StandaloneUI();
        if (defaultCmdName!=null) aloneUI.setDefaultCmdName(defaultCmdName);


        isddCmd= new ImageSelectDropDownCmd();

        HashMap<String, GeneralCommand > commands = new HashMap<String, GeneralCommand>();
//        addCommand(commands, catalogDropDownCmd);
        addCommand(commands, new OverviewHelpCmd());
        addCommand(commands, new AnyDataSetCmd());
//        addCommand(commands, new ExternalCatalogSearchCmd());
        commands.put(FFToolsImageCmd.COMMAND, new FFToolsImageCmd(aloneUI));
        commands.put(FFToolsPushReceiveCmd.COMMAND, new FFToolsPushReceiveCmd(aloneUI));
        commands.put(FFToolsExtCatalogCmd.COMMAND, new FFToolsExtCatalogCmd(aloneUI));
        commands.put(ImageSelectDropDownCmd.COMMAND_NAME, isddCmd);
        commands.put(IrsaCatalogDropDownCmd.COMMAND_NAME , new IrsaCatalogDropDownCmd(true));
//        commands.put(ImageSelectDropDownDynCmd.COMMAND_NAME, new ImageSelectDropDownDynCmd(aloneUI));

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


    public LayoutManager makeLayoutManager() { return new FFToolsStandaloneLayoutManager(bannerOffset, footerHtmlFile); }

    public String getLoadingDiv() { return "application"; }

    private class MyToolbarDropdown extends ToolbarDropdown {
        @Override
        protected boolean getShouldExpandDefault() {
//            return aloneUI.hasOnlyPlotResults() || aloneUI.isInitialStart();
            return aloneUI.hasOnlyPlotResults() || !aloneUI.hasResults();
        }

        @Override
        protected boolean getShouldHideCloseOnDefaultTab() {
            return !aloneUI.hasResults();
        }
    }

    private class StandaloneToolBar extends Toolbar {

        private StandaloneToolBar() {
            setDropdown(new MyToolbarDropdown());
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
        protected boolean isDefaultTabSelected() {
            String cmd= getSelectedCommand();

            return ((cmd==null || cmd.equals(CATALOG_NAME) || cmd.equals(ImageSelectDropDownCmd.COMMAND_NAME)));
        }
    }


    @Override
    public ServerTask[] getCreatorInitTask() {
        return new ServerTask[] {new CoreTask.LoadProperties(), new CoreTask.LoadJS()};
    }
}