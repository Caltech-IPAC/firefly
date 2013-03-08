package edu.caltech.ipac.fftools.core;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.CatalogSearchCmd;
import edu.caltech.ipac.firefly.commands.FFToolsAppCmd;
import edu.caltech.ipac.firefly.commands.IrsaCatalogDropDownCmd;
import edu.caltech.ipac.firefly.commands.OverviewHelpCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.Creator;
import edu.caltech.ipac.firefly.core.DefaultRequestHandler;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.LoginManager;
import edu.caltech.ipac.firefly.core.MenuGenerator;
import edu.caltech.ipac.firefly.core.RequestHandler;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.Vis;

import java.util.HashMap;
import java.util.Map;

public class FFToolsStandaloneCreator implements Creator {

    public static final String APPLICATION_MENU_PROP = "AppMenu";
    private Toolbar.RequestButton catalog= null;

    public FFToolsStandaloneCreator() {
    }






    public boolean isApplication() { return true; }



    public Toolbar getToolBar() {
        // todo

        final Toolbar toolbar = new Toolbar();
        toolbar.setToolbarTopSizeDelta(47);
        GwtUtil.setStyles(toolbar, "zIndex", "10", "position", "absolute");
        toolbar.setVisible(true);
        toolbar.setWidth("100%");
        AllPlots.getInstance().setToolBarIsPopup(false);

        Vis.init(new Vis.InitComplete() {
            public void done() {
                Map<String, GeneralCommand> map= Application.getInstance().getCommandTable();
                map.putAll(AllPlots.getInstance().getCommandMap());
                MenuGenerator gen = MenuGenerator.create(map,false);
                gen.createToolbarFromProp(APPLICATION_MENU_PROP, toolbar);
                setupAddtlButtons(toolbar);
                Widget visToolBar= AllPlots.getInstance().getMenuBarInline();
                FFToolsStandaloneLayoutManager lm=
                        (FFToolsStandaloneLayoutManager)Application.getInstance().getLayoutManager();
//                lm.getMenuLines().insert(visToolBar,0);
                lm.getMenuLines().clear();
                lm.getMenuLines().add(visToolBar);
                lm.getMenuLines().add(Application.getInstance().getToolBar().getWidget());

                Application.getInstance().getToolBar().getWidget().addStyleName("tool-bar-widget");
                visToolBar.addStyleName("vis-tool-bar-widget");


                Region helpReg= lm.getRegion(LayoutManager.VIS_MENU_HELP_REGION);
                helpReg.setDisplay(AllPlots.getInstance().getMenuBarInlineStatusLine());
//                lm.getMenuLines().add(lm.getRegion(LayoutManager.VIS_MENU_HELP_REGION).getContent());
//                lm.getSouth().add(lm.getRegion(LayoutManager.VIS_MENU_HELP_REGION).getDisplay());
//                lm.getSouth().add(AllPlots.getInstance().getMenuBarInlineStatusLine());
            }
        });
        return toolbar;
    }

    private void setupAddtlButtons(final Toolbar toolbar) {

        catalog = new Toolbar.RequestButton("Catalogs", IrsaCatalogDropDownCmd.COMMAND_NAME);
        WebEventManager.getAppEvManager().addListener(Name.SEARCH_RESULT_END, new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                toolbar.addButton(catalog, 0);
            }
        });
        WebEventManager.getAppEvManager().addListener(Name.SEARCH_RESULT_START, new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                toolbar.removeButton(catalog.getName());
            }
        });
    }


    public Map makeCommandTable() {
        // todo

        HashMap<String, GeneralCommand> commands = new HashMap<String, GeneralCommand>();
        addCommand(commands, new IrsaCatalogDropDownCmd());
        addCommand(commands, new OverviewHelpCmd());
        commands.put(FFToolsAppCmd.COMMAND, new FFToolsAppCmd());
        commands.put(CatalogSearchCmd.COMMAND_NAME, new CatalogSearchCmd());

        return commands;
    }


    private void addCommand(HashMap<String, GeneralCommand> maps, GeneralCommand c) {
        maps.put(c.getName(), c);
    }



    public RequestHandler makeCommandHandler() { return new DefaultRequestHandler(); }
    public LoginManager makeLoginManager() { return null; }
    public String getAppDesc() { return "IRSA general FITS/Catalog Viewer"; }
    public String getAppName() { return "IRSA Viewer"; }



    public LayoutManager makeLayoutManager() { return new FFToolsStandaloneLayoutManager(); }

    public String getLoadingDiv() { return "application"; }



}