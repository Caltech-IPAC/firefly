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
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.MenuItemAttrib;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.PropConst;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.WebAppProperties;
import edu.caltech.ipac.firefly.util.WebProp;

import java.util.Map;

/**
 * Date: Oct 30, 2007
 *
 * @author loi
 * @version $Id: MenuGenerator.java,v 1.48 2012/11/21 21:12:43 roby Exp $
 */
public class MenuGeneratorV2 {

    private Map<String, GeneralCommand> commandTable;
    private final boolean iconOnly;
    private final MouseOver over;



    private MenuGeneratorV2(Map<String, GeneralCommand> commandTable, MouseOver over, boolean iconOnly) {
        this.commandTable= commandTable;
        this.iconOnly= iconOnly;
        this.over= over;
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

    public FlowPanel makeToolBarFromProp(String menuProp, boolean forDialog) {
        FlowPanel toolbar = new FlowPanel();
        MenuItemAttrib mia= getMenuItemAttrib(menuProp);
        makeMenuBar(toolbar, mia, true);
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
                                    boolean ignoreStrays) {

        MenuItemAttrib[] children = menuData.getChildren();
        GwtUtil.setStyle(menuBar,"whiteSpace", "nowrap");


        for (MenuItemAttrib itemData : children) {
            GeneralCommand gc= commandTable.get(itemData.getName());
            if (itemData.hasChildren()) {
                MenuBarCmd cmd;
                if (gc!=null && gc instanceof MenuBarCmd)  cmd= (MenuBarCmd)gc;
                else                                       cmd= new MenuBarCmd(itemData.getName());

                MenuGenItem barItem= makeMenuItem(itemData.getName(), gc, true, ignoreStrays);
                new MenuItemConnect(barItem,cmd);
                Widget content= makeDropDownContent(itemData, ignoreStrays);
                GwtUtil.setStyle(barItem.getWidget(), "display", "inline-block");
                menuBar.add(barItem.getWidget());
                new PullDown(barItem.getWidget(),content);
            } else {
                if (itemData.isSeparator()) {
                    Label l= new Label("");
                    l.setSize("5px", "5px");
                    GwtUtil.setStyle(l, "display", "inline-block");
                   menuBar.add(l);
                }
                else {
                    MenuGenItem menuItem= makeMenuItem(itemData.getName(), gc, false, ignoreStrays);
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
                    MenuGenItem menuItem= makeMenuItem(itemData.getName(), gc, false, ignoreStrays);
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


    private MenuGenItem makeMenuItem(String name, final GeneralCommand cmd, boolean isMenu, boolean ignoreStrays) {
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

        MenuGenItem item;
        if (command.hasIcon()) {
            Image image= cmd.createImage();
            if (image!=null) {
                item= new MenuGenItem(image, command.getName());
            }
            else {
                item= new MenuGenItem(command.getLabel(),command.getName());
            }

        }
        else {
            item= new MenuGenItem(command.getLabel(), command.getName());
        }

        Widget itemW= item.getWidget();

        if (!isMenu) {
            itemW.addDomHandler( new ClickHandler() {
                public void onClick(ClickEvent event) {
                    PullDown.hide();
                    cmd.execute();
                } }, ClickEvent.getType());
        }

        if (over!=null) {
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

        if (tip!=null) itemW.setTitle(tip);

        if (!(command instanceof MenuBarCmd)) {
            new MenuItemConnect(item,cmd);
        }


        return item;
    }



    public static class MenuBarCmd extends GeneralCommand{
        public MenuBarCmd(String name) {
            super(name);
        }
        protected void doExecute() { }
    }


    public static class MenuGenItem {
        private final SimplePanel sp= new SimplePanel();

        private MenuGenItem(String styleName) {
            sp.setStyleName("firefly-v2-MenuItem");
            sp.addStyleName(styleName);
        }

        public MenuGenItem(Image image, String styleName) {
            this(styleName);
            setIcon(image);
        }
        public MenuGenItem(String text, String styleName) {
            this(styleName);
            setText(text);
        }

        public void setIcon(Image image) {
            sp.setWidget(image);
        }
        public void setText(String text) {
            Label l= new Label(text);
            l.setStyleName("menuItemText");
            sp.setWidget(l);
        }
        public Widget getWidget() { return sp; }
    }



    public static class MenuItemConnect implements PropertyChangeListener {
        private final MenuGenItem mi;
        private final GeneralCommand cmd;
        private final boolean horizontal;

        MenuItemConnect(MenuGenItem mi, GeneralCommand cmd, boolean horizontal) {
            this.mi= mi;
            this.cmd= cmd;
            this.horizontal= horizontal;
            cmd.addPropertyChangeListener(this);
            setMenuItemEnabled(cmd.isEnabled());
        }

        MenuItemConnect(MenuGenItem mi, GeneralCommand cmd) {
            this(mi,cmd,true);
        }


        private void setMenuItemEnabled(boolean enabled) {
            if (enabled) {
                mi.getWidget().removeStyleName("firefly-MenuItem-v2-disabled");
            }
            else {
                mi.getWidget().addStyleName("firefly-MenuItem-v2-disabled");
            }
        }

        private void setMenuItemAttention(boolean attention) {
            if (attention) {
                mi.getWidget().addStyleName("firefly-MenuItem-v2-attention");
            }
            else {
                mi.getWidget().removeStyleName("firefly-MenuItem-v2-attention"); }
        }

        private void setMenuItemHidden(boolean hidden) {
            GwtUtil.setHidden(mi.getWidget().getElement(),hidden);
        }

        public void propertyChange(PropertyChangeEvent ev) {
            if (ev.getPropertyName().equals(GeneralCommand.PROP_ENABLED)) {
                setMenuItemEnabled(cmd.isEnabled());
            }
            if (ev.getPropertyName().equals(GeneralCommand.PROP_ATTENTION)) {
                setMenuItemAttention(cmd.isAttention());
            }
            else if (ev.getPropertyName().equals(GeneralCommand.PROP_HIDDEN)) {
                mi.getWidget().setVisible(!cmd.isHidden());
                if (!cmd.isHidden() && horizontal) {
                    GwtUtil.setStyle(mi.getWidget(), "display", "inline-block");
                }
            }
            else if (ev.getPropertyName().equals(GeneralCommand.PROP_TITLE)) {
                mi.setText((String)ev.getNewValue());

            }
            else if (ev.getPropertyName().equals(GeneralCommand.PROP_ICON)) {
                String url= (String)ev.getNewValue();
                if (!url.startsWith("http")) url= GWT.getModuleBaseURL() +url;
                mi.setIcon(new Image(url));

            }
            else if (ev.getPropertyName().equals(GeneralCommand.ICON_PRIOPERTY)) {
                mi.setIcon(cmd.createImage());
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