package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.StringUtils;

import java.util.HashMap;
import java.util.Map;


/**
 * Date: Jun 20, 2008
 *
 * @author loi
 * @version $Id: Toolbar.java,v 1.39 2012/05/16 01:39:04 loi Exp $
 */
public class Toolbar extends Composite {
    public static enum Align {LEFT, RIGHT, CENTER}
    private static IconCreator iconCreator = IconCreator.Creator.getInstance();

    private ToolbarDropdown dropdown = new ToolbarDropdown();

    private WebEventManager eventManager = new WebEventManager();
    private TTabBar leftToolbar;
    private TTabBar centerToolbar;
    private TTabBar rightToolbar;
    private Map<String, TabHolder> tabs = new HashMap<String, TabHolder>();
    private boolean buttonClicked = false;
    private String defaultWidth = "75px";

    public Toolbar() {

//        DockPanel wrapper = new DockPanel();

        leftToolbar = new TTabBar();
        leftToolbar.setStylePrimaryName("DropDownToolBar");
        rightToolbar = new TTabBar();
        rightToolbar.setStylePrimaryName("DropDownToolBar");
        rightToolbar.addStyleName("right");
        centerToolbar = new TTabBar();
        centerToolbar.setStylePrimaryName("DropDownToolBar");
        centerToolbar.addStyleName("center");

        HTMLPanel tbar = new HTMLPanel("<div style='white-space:nowrap'>\n" +
                                "    <div id='leftBar' style='display:inline-block'></div>\n" +
                                "    <div id='centerBar' style='display:inline-block;margin-left:20px'></div>\n" +
                                "    <div id='rightBar' style='display:inline-block;position:absolute;right:0'></div>\n" +
                                "</div>");

        tbar.add(leftToolbar, "leftBar");
        tbar.add(centerToolbar, "centerBar");
        tbar.add(rightToolbar, "rightBar");

        initWidget(tbar);

        EventHandler eventHandler = new EventHandler();

        leftToolbar.addBeforeSelectionHandler(eventHandler);
        centerToolbar.addBeforeSelectionHandler(eventHandler);
        rightToolbar.addBeforeSelectionHandler(eventHandler);
        leftToolbar.addSelectionHandler(eventHandler);
        centerToolbar.addSelectionHandler(eventHandler);
        rightToolbar.addSelectionHandler(eventHandler);

    }

    public void setDropdown(ToolbarDropdown  dropdown) {
        this.dropdown= dropdown;
    }

    public ToolbarDropdown getDropdown() {
        return dropdown;
    }

    public String getDefaultWidth() {
        return defaultWidth;
    }

    public void setDefaultWidth(String defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    public WebEventManager getEventManager() {
        return eventManager;
    }

    public Widget getWidget() {
        return this;
    }

    public void deselectAll() {
        leftToolbar.selectTab(-1, false);
        centerToolbar.selectTab(-1, false);
        rightToolbar.selectTab(-1, false);
    }

    public boolean select(String name) {
        return select(name, true);
    }

    public boolean select(String name, boolean fireEvent) {
        TabHolder th = tabs.get(name);
        if (th != null) {
            int idx = indexOf(th.tabBar, th.tab);
            if (idx >= 0) {
                boolean success= th.tabBar.selectTab(idx, fireEvent);
                dropdown.updateCloseVisibility();
                return success;
            }
        }
        return false;
    }

    //==================================================================
    //------------------ Protected methods that can be overridden to
    //------------------ tweak the behavior of the Toolbar
    //==================================================================


    protected boolean isDefaultTabSelected() { return leftToolbar.getSelectedTab() <= 0; }

    protected boolean getShouldHideCloseOnDefaultTab() {
        return !Application.getInstance().hasSearchResult();
    }

    protected boolean getShouldExpandDefault() {
        return !Application.getInstance().hasSearchResult();
    }

    protected void expandDefault() {
        if (leftToolbar.getTabCount() > 0) {
            leftToolbar.selectTab(0);
        }
    }

    public void setButtonVisible(String name, boolean isVisible) {
        TabHolder th = tabs.get(name);
        if (th != null) {
            int idx = indexOf(th.tabBar, th.tab);
            Widget w = th.tabBar.getTabWrapper(idx);
            if (w != null) {
                w.setVisible(isVisible);
            }
        }
    }
    
    public Button getButton(String name) {
        TabHolder th = tabs.get(name);
        if (th != null) {
            return th.button;
        }
        return null;
    }
    
    public void addButton(Button button) {
        addButton(button, Align.CENTER, null);
    }

    public void addButton(Button button, int idx) {
        addButton(button, Align.CENTER, idx, null);
    }

    public void addButton(Button button, Align align) {
        addButton(button, align, -1, null);
    }

    public void addButton(Button button, Align align, String width) {
        addButton(button, align, -1, width);
    }

    public void addButton(Button button, Align align, int idx, String width) {
        TTabBar tb = align == Align.LEFT ? leftToolbar : align == Align.CENTER ? centerToolbar : rightToolbar;
        idx = idx < 0 || idx > tb.getTabCount() ? tb.getTabCount() : idx;
        tb.insertTab(button.asWidget(), idx);
        TabBar.Tab t = tb.getTab(idx);
        tabs.put(button.getName(), new TabHolder(t, button, tb));
        width = StringUtils.isEmpty(width) ? defaultWidth : width;
        GwtUtil.setStyle(button.asWidget(), "minWidth", width);
    }

    public void removeButton(String name) {
        TabHolder th = tabs.get(name);
        if (th != null) {
            int idx = indexOf(th.tabBar, th.tab);
            if (idx >= 0) {
                th.tabBar.removeTab(idx);
                tabs.remove(name);
            }
        }
    }

//====================================================================
//
//====================================================================

    public String getSelectedCommand() {
        String retval= null;
        for (Map.Entry<String,TabHolder> entry : tabs.entrySet()) {
            TabHolder th= entry.getValue();
            int idx= th.tabBar.getSelectedTab();
            if (idx>-1) {
                TabBar.Tab testTab= th.tabBar.getTab(idx);
                if (testTab==th.tab)  retval= entry.getKey();
            }
        }
        return retval;
    }
    
    private int indexOf(TabBar bar, TabBar.Tab tab) {
        if (bar != null) {
            for (int i = 0; i < bar.getTabCount(); i++) {
                if (bar.getTab(i).equals(tab)) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    private TabHolder findTab(int idx, TabBar tb) {
        TabBar.Tab tab = tb.getTab(idx);
        if (tab != null) {
            for (TabHolder th : tabs.values()) {
                if (th.tab.equals(tab)) {
                    return th;
                }
            }
        }
        return null;
    }

//====================================================================
//
//====================================================================

    class EventHandler implements SelectionHandler<Integer>, BeforeSelectionHandler<Integer> {
        private int cSelIdx = -1;

        public void onBeforeSelection(BeforeSelectionEvent<Integer> bse) {
            buttonClicked = true;
            TabBar tb = (TabBar) bse.getSource();
            cSelIdx = tb.getSelectedTab();
            int selIdx = bse.getItem();
            TabHolder th = findTab(selIdx, tb);
            if (!th.button.isUseDropdown()) {
                th.button.activate();
                bse.cancel();
            } else {
                dropdown.clearHeaderBar();
            }
        }

        public void onSelection(SelectionEvent<Integer> ise) {
            TabBar tb = (TabBar) ise.getSource();
            int idx = tb.getSelectedTab();
            if (idx == cSelIdx) {
                tb.selectTab(-1, false);
                dropdown.close();
            } else {
                if (!tb.equals(leftToolbar)) {
                    leftToolbar.selectTab(-1, false);
                }
                if (!tb.equals(centerToolbar)) {
                    centerToolbar.selectTab(-1, false);
                }
                if (!tb.equals(rightToolbar)) {
                    rightToolbar.selectTab(-1, false);
                }

                TabHolder th = findTab(idx, tb);
                if (th != null) {
                    th.button.activate();
                }
            }
            buttonClicked = false;
        }
    }

    static class TabHolder {
        public TTabBar.Tab tab;
        public Button button;
        public TTabBar tabBar;

        TabHolder(TabBar.Tab tab, Button button, TTabBar tabBar) {
            this.tab = tab;
            this.button = button;
            this.tabBar = tabBar;
        }
    }
    
    static class TTabBar extends TabBar {
        TTabBar() {
            setWidth("1px");
        }

        ComplexPanel asPanel() {
            return (ComplexPanel) getWidget();
        }
        
        Widget getTabWrapper(int idx) {
            if (idx >= 0 && idx < this.getTabCount()) {
                return asPanel().getWidget(idx + 1);
            }
            return null;
        }
        
    }

//====================================================================
//
//====================================================================

    public interface Button extends IsWidget {
        String getName();
        void activate();
        boolean isUseDropdown();
        void setUseDropdown(boolean useDropdown);
        void setText(String s);
        void setIconLeft(Widget w);
        void setIconRight(Widget w);

    }

    public static class CmdButton extends Composite implements Button {
        Command command;
        String name;
        boolean useDropdown = false;
        HorizontalPanel container;
        HTML html;
        SimplePanel iconHolderLeft = new SimplePanel();
        SimplePanel iconHolderRight = new SimplePanel();

        public CmdButton(String name, String label, String desc, Command cmd) {
            this(name, null, cmd, label, desc);
        }

        public CmdButton(String name, Widget icon, Command cmd, String label, String desc) {
            this.name = name;
            this.command = cmd;
            this.name = name;
            String htmlstr = label == null ? name : label;
            html = new HTML(htmlstr);
            if (desc != null) {
                html.setTitle(desc);
            }
            this.command = cmd;
            html.setWordWrap(false);
            if (command instanceof GeneralCommand) {
                addListeners();
                setButtonEnabled(((GeneralCommand)command).isEnabled());
            }

            GwtUtil.setStyles(iconHolderLeft, "padding", "none", "marginRight", "3px");
            GwtUtil.setStyle(html, "padding", "6px 0");
            container = GwtUtil.makeHoriPanel(null, null, iconHolderLeft, html, iconHolderRight);
            container.setCellVerticalAlignment(iconHolderLeft, VerticalPanel.ALIGN_MIDDLE);
            container.setCellVerticalAlignment(iconHolderRight, VerticalPanel.ALIGN_MIDDLE);
            setIconLeft(icon);
            setIconRight(null);
            GwtUtil.setStyle(container, "margin", "0px auto");
            initWidget(new SimplePanel(container));
        }

        protected void setCommand(Command cmd) {
            command = cmd;
        }

        public String getName() {
            return name;
        }

        public void activate() {
            command.execute();
        }

        public boolean isUseDropdown() {
            return useDropdown;
        }

        public void setUseDropdown(boolean useDropdown) {
            this.useDropdown = useDropdown;
        }

        public void setText(String s) {
            html.setText(s);
        }

        public void setIconLeft(Widget w) {
            iconHolderLeft.setWidget(w);
            iconHolderLeft.setVisible(w != null);
            if (w != null) {
                w.setSize("20px", "20px");
                GwtUtil.setStyles(w, "verticalAlign", "middle", "margin", "0");
            }
        }

        public void setIconRight(Widget w) {
            iconHolderRight.setWidget(w);
            iconHolderRight.setVisible(w != null);
            if (w != null) {
                w.setSize("20px", "20px");
                GwtUtil.setStyles(w, "verticalAlign", "middle", "margin", "0");
            }
        }

        public Widget getIcon() {
            return iconHolderLeft.getWidget();
        }


        private void setButtonEnabled(boolean enabled) {
            GwtUtil.setStyle(html, "color", enabled?"black":"gray");

        }

        private void addListeners() {
            if (command instanceof GeneralCommand) {
                final GeneralCommand c= (GeneralCommand)command;
                c.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent ev) {
                        if (ev.getPropertyName().equals(GeneralCommand.PROP_ENABLED)) {
                            setButtonEnabled(c.isEnabled());
                        }
                    }
                });
            }


        }

    }

    public static class RequestButton extends CmdButton {
        Request request;

        public RequestButton(String name, String searchId) {
            this(name, searchId, null, null);
        }

        public RequestButton(String name, String searchId, String label, String desc) {
            super(name, label, desc, null);
            request = new Request(searchId, label, true, false);
            request.setIsSearchResult(false);
            Command cmd = new Command() {
                public void execute() {
                    Application.getInstance().processRequest(request);
                }
            };
            setCommand(cmd);
            setUseDropdown(true);
        }

        Request getRequest() {
            return request;
        }
    }
}

//====================================================================
//
//====================================================================
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