package edu.caltech.ipac.firefly.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.MenuItemAttrib;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.IconMenuItem;
import edu.caltech.ipac.firefly.ui.TwoComponentMenuItem;
import edu.caltech.ipac.firefly.ui.background.BackgroundManager;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.util.PropConst;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.firefly.util.WebProp;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.util.action.ActionConst;

import java.util.Map;

/**
 * Date: Oct 30, 2007
 *
 * @author loi
 * @version $Id: MenuGenerator.java,v 1.48 2012/11/21 21:12:43 roby Exp $
 */
@Deprecated
public class MenuGenerator {

    private static MenuGenerator defaultInstance= null;

    private Map<String, GeneralCommand> commandTable;
    private final boolean iconOnly;



    private MenuGenerator(Map<String, GeneralCommand> commandTable, boolean iconOnly) {
        this.commandTable= commandTable;
        this.iconOnly= iconOnly;
    }

    public static MenuGenerator getDefaultInstance() {
        if (defaultInstance==null) {
            defaultInstance= new MenuGenerator(Application.getInstance().getCommandTable(), false);
        }
        return defaultInstance;
    }
    public static MenuGenerator create(Map<String, GeneralCommand> commandTable) {
        return new MenuGenerator(commandTable,false);
    }

    public static MenuGenerator create(Map<String, GeneralCommand> commandTable, boolean iconOnly) {
        return new MenuGenerator(commandTable, iconOnly);
    }

//====================================================================
//  creating toolbar using http request
//====================================================================

    /**
     *  Create a ToolBar from the given property name.
     * @param menuProp  a property name to lookup menu.
     * @return a ToolBar
     */
//    public MenuBar makeToolBarFromProp(String menuProp) {
//
//        final MenuBar toolbar = new MenuBar();
//        toolbar.setAutoOpen(true);
//        toolbar.setAnimationEnabled(true);
//
//
//        ResourceServices.App.getInstance().getMenu(menuProp, new AsyncCallback(){
//            public void onFailure(Throwable throwable) {
//                final DialogBox popup = new DialogBox(false, true);
//                popup.setHTML("Failed while creating menu: <br>" + throwable.getMessage());
////                popup.setSize("600px", "400px");
//
//                Button ok = new Button("OK");
//                ok.addClickListener(new ClickListener() {
//                  public void onClick(Widget sender) {
//                    popup.hide();
//                  }
//                });
//                popup.add(ok);
//                popup.show();
//            }
//
//            public void onSuccess(Object o) {
//                makeMenu(toolbar, (MenuItemAttrib)o);
//            }
//        });
//
//        return toolbar;
//    }

    //=============****************
    public MenuBar makeToolBarFromProp(String menuProp) {
       return  makeToolBarFromProp(menuProp,null, false,false,false);
    }

    public MenuBar makeToolBarFromProp(String menuProp, boolean vertical, boolean ignoreStrays) {
        return  makeToolBarFromProp(menuProp,null, vertical,false,ignoreStrays);
    }


    public MenuBar makeToolBarFromProp(String menuProp,
                                       MenuBar toolbar,
                                       boolean vertical,
                                       boolean forDialog,
                                       boolean ignoreStrays) {

        if (toolbar==null) toolbar = new FireflyMenuBar(vertical);
        if (vertical) toolbar.addStyleName("veritcal-toolbar");
        toolbar.addStyleName(menuProp);
        toolbar.setAnimationEnabled(true);
        MenuItemAttrib mia= getMenuItemAttrib(menuProp);
        makeMenu(toolbar, false, mia,forDialog, ignoreStrays);
        return toolbar;
    }

    public Toolbar createToolbarFromProp(String menuProp,
                                         Toolbar toolbar) {

        if (toolbar==null) toolbar = new Toolbar();
        MenuItemAttrib mia= getMenuItemAttrib(menuProp);
        if (mia.getPreferWidth() > 0) {
            toolbar.setDefaultWidth(mia.getPreferWidth() + "px");
        }

        // this toolbar only support 1 level deep
        MenuItemAttrib[] children = mia.getChildren();
        if (children != null) {
            for (MenuItemAttrib item : children) {
                if (!item.isSeparator()) {
                    String name = item.getName();
                    String label = item.getLabel();
                    String desc = item.getDesc();
                    String shortDesc = item.getShortDesc();
                    Toolbar.Button b= null;
                    if (item.getToolBarButtonType()== MenuItemAttrib.ToolbarButtonType.COMMAND) {
                        GeneralCommand cmd= commandTable.get(item.getName());
                        if (cmd!=null) b= new Toolbar.CmdButton(name, label, shortDesc, cmd);
                    }
                    else {
                        b= new Toolbar.RequestButton(name, name, label, shortDesc);
                    }
                    if (b!=null) toolbar.addButton(b, Toolbar.Align.LEFT);
                }
            }
        }
        BackgroundManager bMan = Application.getInstance().getBackgroundManager();
        toolbar.addButton(bMan.getButton(), Toolbar.Align.RIGHT, "150px");
        return toolbar;
    }


    public MenuItemAttrib getMenuItemAttrib(String commandName) {

        MenuItemAttrib mia = new MenuItemAttrib(commandName.equals("-"));
        if (!mia.isSeparator()) {
            setUIAttributes(mia, commandName);
            String[] items = WebProp.getItems(commandName);
            if (items != null) {
                for(String s : items) {
                    mia.addMenuItem(getMenuItemAttrib(s));
                }
            }
        }
        return mia;
    }

    private void setUIAttributes(MenuItemAttrib uia, String commandName) {
        WebAppProperties prop= Application.getInstance().getProperties();

        uia.setName(WebProp.getName(commandName, commandName, null));
        uia.setLabel(WebProp.getTitle(commandName));
        uia.setShortDesc(prop.getProperty(commandName + "." + PropConst.SHORT_DESCRIPTION));
        uia.setIcon(prop.getProperty(commandName + ".Icon"));
        uia.setPreferWidth(prop.getIntProperty(commandName + "." + ActionConst.PREFER_WIDTH, -1));

        String bt= prop.getProperty(commandName + ".ToolbarButtonType","NONE");
        MenuItemAttrib.ToolbarButtonType buttonType;
        try {
            buttonType= Enum.valueOf(MenuItemAttrib.ToolbarButtonType.class,bt);
        } catch (Exception e) {
            buttonType= MenuItemAttrib.ToolbarButtonType.NONE;
        }
        uia.setToolBarButtonType(buttonType);

    }


//====================================================================
//  private and protected methods
//====================================================================

    protected MenuBar makeMenu(MenuBar menuBar,
                               boolean isInsideVeritcalMenu,
                               MenuItemAttrib menuData,
                               boolean forDialog,
                               boolean ignoreStrays ) {

        if (menuBar == null) {
            menuBar = new FireflyMenuBar(true);
            isInsideVeritcalMenu= true;
            menuBar.setAnimationEnabled(true);
//            if (forDialog) menuBar.addStyleName("onTopDialogPulldown");
            if (Application.getInstance().getDefZIndex()>0) {
//                GwtUtil.setStyle(menuBar, "zIndex", Application.getInstance().getDefZIndex()+"");
                menuBar.addStyleName("onTopDialogPulldown");
            }
        }
        MenuItemAttrib[] children = menuData.getChildren();

        boolean first= true;
        for (MenuItemAttrib itemData : children) {
            if (itemData.hasChildren()) {
                MenuBar subBar= makeMenu(null, isInsideVeritcalMenu, itemData,forDialog,ignoreStrays );


                MenuBarCmd cmd;
                GeneralCommand gc= commandTable.get(itemData.getName());
                if (gc!=null && gc instanceof MenuBarCmd) {
                    cmd= (MenuBarCmd)gc;
                }
                else {
                    cmd= new MenuBarCmd(itemData.getName());
                }

                MenuItem barItem= makeMenuItem(itemData.getName(),first,
                                               isInsideVeritcalMenu,cmd,
                                               ignoreStrays );
                new MenuItemConnect(barItem,gc);
                barItem.setSubMenu(subBar);
                menuBar.addItem(barItem);
//                uio= subBar;


//                menuBar.addItem(itemData.getLabel(), subBar);
            } else {
                if (itemData.isSeparator()) {
                    menuBar.addSeparator();
                }
                else {
                    UIObject uio= makeMenuItem(itemData.getName(),first,isInsideVeritcalMenu,ignoreStrays );
                    if (uio!=null) menuBar.addItem((MenuItem)uio);
                }
            }
            first=false;
        }
        return menuBar;
    }


    private String getTip(Command command) {
        String tip= null;
        if (command instanceof GeneralCommand) {
            GeneralCommand fireflyCommand =
                                          (GeneralCommand)command;
            tip= fireflyCommand.getShortDesc();
        }
        return tip;
    }


    private MenuItem makeMenuItem(String name,
                                  boolean first,
                                  boolean isInsideVeritcalMenu,
                                  boolean        ignoreStrays ) {


        return makeMenuItem(name,
                            first,
                            isInsideVeritcalMenu,
                            commandTable.get(name),
                            ignoreStrays );
    }


    private MenuItem makeMenuItem(String name,
                                  boolean first,
                                  boolean isInsideVeritcalMenu,
                                  GeneralCommand cmd,
                                  boolean        ignoreStrays) {
        final MenuItem mi;
        final GeneralCommand command;
        if (cmd==null) {
            command = commandTable.get(name);
        }
        else {
            command = cmd;
        }
        String tip= getTip(command);
        Command trigger = command;
        if (command instanceof RequestCmd) {
            trigger = new Command(){
                public void execute() {
                    Request req = new Request(command.getName(), command.getShortDesc(), true, false);
                    req.setIsDrilldownRoot(true);
                    Application.getInstance().processRequest(req);
                }
            };
        }

        if (command !=null && (!StringUtils.isEmpty(command.getIcon()) || command.hasIcon() || iconOnly)) {
            if (((!iconOnly && !command.isIconOnlyHint()) || isInsideVeritcalMenu) &&
                !StringUtils.isEmpty(command.getLabel())) {
                if (trigger instanceof MenuBarCmd) trigger= null;
//                Image image= cmd.canCreateIcons() ? cmd.createCmdImage() : new Image(command.getIcon());
                Image image= cmd.createImage();
                mi= new TwoComponentMenuItem(command.getLabel(), image, trigger);

            }
            else {
                if (trigger instanceof MenuBarCmd) trigger= null;
                mi= new IconMenuItem(command.createImage(), trigger,first);
            }
        }
        else if (command !=null) {
            if (trigger instanceof MenuBarCmd) trigger= null;
            mi= new MenuItem(command.getLabel(), trigger);
        }
        else if (command ==null && ignoreStrays) {
            mi= null;
        }
        else {
            mi= new MenuItem(name,(Command)null);
        }
        if (tip!=null && mi!=null) mi.setTitle(tip);

        if (cmd!=null && !(cmd instanceof MenuBarCmd)) {
            new MenuItemConnect(mi,cmd);
            mi.addStyleName(command.getName());
        }

        if (cmd!=null && cmd.isImportant()) {
//            mi.addStyleName("standout-background");
        }


        return mi;
    }



    public static class MenuBarCmd extends GeneralCommand{
        public MenuBarCmd(String name) {
            super(name);
        }
        protected void doExecute() { }
    }







    public static class MenuItemConnect implements PropertyChangeListener {
        private final MenuItem mi;
        private final GeneralCommand cmd;

        MenuItemConnect(MenuItem mi, GeneralCommand cmd) {
            this.mi= mi;
            this.cmd= cmd;
            cmd.addPropertyChangeListener(this);
            setMenuItemEnabled(mi, cmd.isEnabled());
        }


        private void setMenuItemEnabled(MenuItem mi, boolean enabled) {
            if (enabled) {
                mi.removeStyleDependentName("disabled");
            }
            else {
                mi.addStyleDependentName("disabled");
            }
        }

        private void setMenuItemAttention(MenuItem mi, boolean attention) {
            if (attention) {
                mi.addStyleDependentName("attention");
            }
            else {
                mi.removeStyleDependentName("attention");
            }
        }

        private void setMenuItemHidden(MenuItem mi, boolean hidden) {
            GwtUtil.setHidden(mi.getElement(),hidden);
        }

        public void propertyChange(PropertyChangeEvent ev) {
            if (ev.getPropertyName().equals(GeneralCommand.PROP_ENABLED)) {
                setMenuItemEnabled(mi, cmd.isEnabled());
            }
            if (ev.getPropertyName().equals(GeneralCommand.PROP_ATTENTION)) {
                setMenuItemAttention(mi, cmd.isAttention());
            }
            else if (ev.getPropertyName().equals(GeneralCommand.PROP_HIDDEN)) {
                mi.setVisible(!cmd.isHidden());
            }
            else if (ev.getPropertyName().equals(GeneralCommand.PROP_TITLE)) {
                mi.setText((String)ev.getNewValue());

            }
            else if (ev.getPropertyName().equals(GeneralCommand.PROP_ICON)) {
                    if (mi instanceof IconMenuItem) {
                        String url= (String)ev.getNewValue();
                        if (!url.startsWith("http")) url= GWT.getModuleBaseURL() +url;
                        ((IconMenuItem)mi).setImage(url);
                    }
                    else if (mi instanceof TwoComponentMenuItem) {
                        String url= (String)ev.getNewValue();
                        if (!url.startsWith("http")) url= GWT.getModuleBaseURL() +url;
                        ((TwoComponentMenuItem)mi).setImage(new Image(url));
                    }

            }
            else if (ev.getPropertyName().equals(GeneralCommand.ICON_PRIOPERTY)) {
                    if (mi instanceof IconMenuItem) {
                        ((IconMenuItem)mi).setImage(cmd.createImage());
                    }
                    else if (mi instanceof TwoComponentMenuItem) {
                        ((TwoComponentMenuItem)mi).setImage(cmd.createImage());
                    }
            }
        }
    }


    public static class FireflyMenuBar extends MenuBar {
        FireflyMenuBar(boolean vertical) {
            super(vertical);
        }

        public MenuItem getSelectedItem() {
            return super.getSelectedItem();
        }


        public void onAttach() {
            super.onAttach();
            Widget p;
            for(p= getParent(); (p!=null && (!(p instanceof PopupPanel)));p=p.getParent() ) {  }
            if (Application.getInstance().getDefZIndex()>0 && p!=null && p instanceof PopupPanel) {
                GwtUtil.setStyle(p, "zIndex", Application.getInstance().getDefZIndex()+"");
            }

        }

    }


}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
* OR FOR ANY PURPOSE WHATSOEVER, FOR THE SOFTWARE AND RELATED MATERIALS,
* HOWEVER USED.
*
* IN NO EVENT SHALL CALTECH, ITS JET PROPULSION LABORATORY, OR NASA BE LIABLE
* FOR ANY DAMAGES AND/OR COSTS, INCLUDING, BUT NOT LIMITED TO, INCIDENTAL
* OR CONSEQUENTIAL DAMAGES OF ANY KIND, INCLUDING ECONOMIC DAMAGE OR INJURY TO
* PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER CALTECH, JPL, OR NASA BE
* ADVISED, HAVE REASON TO KNOW, OR, IN FACT, SHALL KNOW OF THE POSSIBILITY.
*
* RECIPIENT BEARS ALL RISK RELATING TO QUALITY AND PERFORMANCE OF THE SOFTWARE
* AND ANY RELATED MATERIALS, AND AGREES TO INDEMNIFY CALTECH AND NASA FOR
* ALL THIRD-PARTY CLAIMS RESULTING FROM THE ACTIONS OF RECIPIENT IN THE USE
* OF THE SOFTWARE.
*/