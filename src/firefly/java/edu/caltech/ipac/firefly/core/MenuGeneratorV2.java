/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.MenuItemAttrib;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.BadgeButton;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.background.BackgroundManager;
import edu.caltech.ipac.firefly.ui.panels.Toolbar;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.firefly.util.WebProp;
import edu.caltech.ipac.util.action.ActionConst;

import java.util.Map;

/**
 * Date: Oct 30, 2007
 *
 * @author loi
 * @version $Id: MenuGenerator.java,v 1.48 2012/11/21 21:12:43 roby Exp $
 */
public class MenuGeneratorV2 {

    private static MenuGeneratorV2 defaultInstance= null;
    private Map<String, GeneralCommand> commandTable;
    private final boolean iconOnly;
    private final MouseOver over;



    private MenuGeneratorV2(Map<String, GeneralCommand> commandTable, MouseOver over, boolean iconOnly) {
        this.commandTable= commandTable;
        this.iconOnly= iconOnly;
        this.over= over;
    }

    public static MenuGeneratorV2 getDefaultInstance() {
        if (defaultInstance==null) {
            defaultInstance= new MenuGeneratorV2(Application.getInstance().getCommandTable(), null, false);
        }
        return defaultInstance;
    }

    public static MenuGeneratorV2 create(Map<String, GeneralCommand> commandTable) {
        return new MenuGeneratorV2(commandTable,null, false);
    }

    public static MenuGeneratorV2 create(Map<String, GeneralCommand> commandTable, MouseOver over) {
        return new MenuGeneratorV2(commandTable,over, false);
    }

    public static MenuGeneratorV2 create(Map<String, GeneralCommand> commandTable, MouseOver over, boolean iconOnly) {
        return new MenuGeneratorV2(commandTable, over, iconOnly);
    }

//====================================================================
//  creating toolbar using http request
//====================================================================


    public Toolbar populateApplicationToolbar(String menuProp, Toolbar toolbar) {

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



    public FlowPanel makeMenuToolBarFromProp(String menuProp, boolean forDialog) {
        FlowPanel toolbar = new FlowPanel();
        MenuItemAttrib mia= getMenuItemAttrib(menuProp);
        makeMenuBar(toolbar, mia, true,forDialog);
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

        uia.setName(WebProp.getName(commandName, commandName));
        uia.setLabel(WebProp.getTitle(commandName));
        uia.setShortDesc(prop.getProperty(commandName + "." + ActionConst.SHORT_DESCRIPTION));
        uia.setIcon(prop.getProperty(commandName + ".Icon"));

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

    protected FlowPanel makeMenuBar(FlowPanel menuBar,
                                    MenuItemAttrib menuData,
                                    boolean ignoreStrays,
                                    boolean forDialog) {

        MenuItemAttrib[] children = menuData.getChildren();
        GwtUtil.setStyle(menuBar, "whiteSpace", "nowrap");


        for (MenuItemAttrib itemData : children) {
            GeneralCommand gc= commandTable.get(itemData.getName());
            if (itemData.hasChildren()) {
                MenuBarCmd cmd;
                if (gc!=null && gc instanceof MenuBarCmd)  cmd= (MenuBarCmd)gc;
                else                                       cmd= new MenuBarCmd(itemData.getName());

                BadgeButton barItem= makeMenuItem(itemData.getName(), cmd, true, ignoreStrays);
                new MenuItemConnect(barItem,cmd);
                Widget content= makeDropDownContent(itemData, ignoreStrays);
                GwtUtil.setStyle(barItem.getWidget(), "display", "inline-block");
                menuBar.add(barItem.getWidget());
                new PullDown(barItem.getWidget(),content);
            } else {
                if (itemData.isSeparator()) {
                    HTML sep= new HTML("");
                    sep.setStyleName("firefly-horizontal-separator-v2");
                    menuBar.add(sep);
                }
                else {
                    BadgeButton menuItem= makeMenuItem(itemData.getName(), gc, false, ignoreStrays);
                    if (menuItem!=null) {
                        GwtUtil.setStyle(menuItem.getWidget(),"display", "inline-block");
                        menuBar.add(menuItem.getWidget());
                    }
                }
            }
        }
        return menuBar;
    }


    private Widget makeDropDownContent(MenuItemAttrib parentItem, boolean ignoreStrays) {
        FlowPanel fp= new FlowPanel();
        if (!parentItem.hasChildren()) return null;
        for (MenuItemAttrib itemData : parentItem.getChildren()) {
            GeneralCommand gc= commandTable.get(itemData.getName());
            if (!itemData.hasChildren()) {
                if (itemData.isSeparator()) {
                    Label l= new Label("");
                    l.setSize("5px", "5px");
                    fp.add(l);
                }
                else {
                    BadgeButton menuItem= makeMenuItem(itemData.getName(), gc, false, ignoreStrays);
                    if (menuItem!=null) {
                        fp.add(menuItem.getWidget());
                    }
                }
            }
        }
        GwtUtil.setPadding(fp, 8,8,8,8);
        return fp;
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


    private BadgeButton makeMenuItem(String name, final GeneralCommand cmd, boolean isMenu, boolean ignoreStrays) {
        final MenuItem mi;
        final GeneralCommand command = (cmd==null) ? commandTable.get(name) : cmd;
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
        if (command==null) {
            if (ignoreStrays) return null;
            else /*todo: return something*/ return null;
        }

        BadgeButton button;
        if (command.hasIcon()) {
            Image image= cmd.createImage();
            if (image!=null) {
                button= new BadgeButton(image, command.getName(),true);
            }
            else {
                button= new BadgeButton(command.getLabel(),command.getName());
            }
        }
        else {
            button= new BadgeButton(command.getLabel(), command.getName());
        }


        if (!isMenu) {
            button.addClickHandler( new ClickHandler() {
                public void onClick(ClickEvent event) {
                    PullDown.hide();
                    cmd.execute();
                } });
        }

        if (over!=null) {
            Widget itemW= button.getWidget();
            itemW.addDomHandler(new MouseOverHandler() {
                public void onMouseOver(MouseOverEvent event) {
                    over.in(command);
                }
            }, MouseOverEvent.getType());

            itemW.addDomHandler(new MouseOutHandler() {
                public void onMouseOut(MouseOutEvent event) {
                    over.out(command);
                }
            }, MouseOutEvent.getType());
        }

        if (tip!=null) button.setTitle(tip);

        if (!(command instanceof MenuBarCmd)) {
            new MenuItemConnect(button,cmd);
        }


        return button;
    }



    public static class MenuBarCmd extends GeneralCommand{
        public MenuBarCmd(String name) {
            super(name);
        }
        protected void doExecute() { }
    }


    public static class MenuItemConnect implements PropertyChangeListener {
        private final BadgeButton button;
        private final GeneralCommand cmd;
        private final boolean horizontal;

        MenuItemConnect(BadgeButton button, GeneralCommand cmd, boolean horizontal) {
            this.button = button;
            this.cmd= cmd;
            this.horizontal= horizontal;
            cmd.addPropertyChangeListener(this);
            button.setEnabled(cmd.isEnabled());
            button.setBadgeCount(cmd.getBadgeCount());
        }

        MenuItemConnect(BadgeButton mi, GeneralCommand cmd) {
            this(mi,cmd,true);
        }

        public void propertyChange(PropertyChangeEvent ev) {
            if (ev.getPropertyName().equals(GeneralCommand.PROP_ENABLED)) {
                button.setEnabled(cmd.isEnabled());
            }
            if (ev.getPropertyName().equals(GeneralCommand.PROP_ATTENTION)) {
                button.setAttention(cmd.isAttention());
            }
            else if (ev.getPropertyName().equals(GeneralCommand.PROP_HIDDEN)) {
                button.getWidget().setVisible(!cmd.isHidden());
                if (!cmd.isHidden() && horizontal) {
                    GwtUtil.setStyle(button.getWidget(), "disy", "inline-block");
                }
            }
            else if (ev.getPropertyName().equals(GeneralCommand.PROP_TITLE)) {
                button.setText((String) ev.getNewValue());

            }
            else if (ev.getPropertyName().equals(GeneralCommand.PROP_ICON)) {
                String url= (String)ev.getNewValue();
                if (!url.startsWith("http")) url= GWT.getModuleBaseURL() +url;
                button.setIcon(new Image(url));

            }
            else if (ev.getPropertyName().equals(GeneralCommand.BADGE_COUNT)) {
                button.setBadgeCount(cmd.getBadgeCount());
            }
            else if (ev.getPropertyName().equals(GeneralCommand.ICON_PRIOPERTY)) {
                button.setIcon(cmd.createImage());
            }
        }
    }



    public static class PullDown {
        private static PullDown active= null;
        private Widget controlWidget;
        private Widget content;
        private static PopupPanel pulldown= null;

        public PullDown(Widget        controlWidget,
                        Widget        content) {
            this.controlWidget= controlWidget;
            this.content= content;
            init();
        }

        private void init() {
            controlWidget.addDomHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    changeState();
                }
            }, ClickEvent.getType());

            if (pulldown==null) {
                pulldown= new PopupPanel();
                pulldown.setAutoHideEnabled(true);
                pulldown.setAutoHideOnHistoryEventsEnabled(true);
                pulldown.setStyleName("firefly-MenuItem-v2-dropDown");
                pulldown.addStyleName("onTopDialogPulldown");
                pulldown.setAnimationEnabled(false);
//                GwtUtil.setStyle(pulldown, "minWidth", "240px");
                pulldown.addCloseHandler(new CloseHandler<PopupPanel>() {
                    public void onClose(CloseEvent<PopupPanel> event) {
                        if (active!=null) active.disableHighlight();
//                        active= null;
                    }
                });
                Window.addResizeHandler(new ResizeHandler() {
                    public void onResize(ResizeEvent event) {
                        if (pulldown.isShowing() && active!=null) {
                            pulldown.hide();
                        }
                    }
                });
            }

        }

        public Widget getWidget() {
            return pulldown;
        }


        private void changeState() {
            if (active==null) {
                show();
            }
            else if (active==this) {
                hide();
            }
            else {
                show();
            }
        }

        public static void hide() {
            active= null;
            pulldown.hide();
        }

        private void show() {
            active= this;
            pulldown.setWidget(content);
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                public void execute() {
                    showFixed();
                }
            });
        }

        private void showFixed() {
            int cw= Window.getClientWidth();
            int width= cw-20;
            int xPos= controlWidget.getAbsoluteLeft()-10;
            positionAndDisplay(xPos, width);
        }


        private void positionAndDisplay(int xPos, int width) {
//            pulldown.setWidth(width+"px");
            int y= controlWidget.getAbsoluteTop() + controlWidget.getOffsetHeight();

            pulldown.setPopupPosition(xPos, y);

            pulldown.show();
            enableHighlight();
        }

        private void enableHighlight() {
            controlWidget.addStyleName("firefly-MenuItem-v2-Active");

        }
        private void disableHighlight() {
            controlWidget.removeStyleName("firefly-MenuItem-v2-Active");

        }
    }


    public static interface MouseOver {
        public void in(GeneralCommand cmd);
        public void out(GeneralCommand cmd);
    }


}
