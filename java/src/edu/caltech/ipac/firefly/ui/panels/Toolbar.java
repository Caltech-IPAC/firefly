package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.SearchCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.layout.BaseRegion;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.PropertyChangeEvent;
import edu.caltech.ipac.firefly.util.PropertyChangeListener;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

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

    private RootPanel body;
    private CollapsiblePanel dpanel;
    private WebEventManager eventManager = new WebEventManager();
    private TTabBar leftToolbar;
    private TTabBar centerToolbar;
    private TTabBar rightToolbar;
    private BaseRegion content = new BaseRegion(LayoutManager.DROPDOWN_REGION);
    private BaseRegion footer = new BaseRegion(LayoutManager.FOOTER_REGION);
    private Map<String, TabHolder> tabs = new HashMap<String, TabHolder>();
    private DockPanel mainPanel = new DockPanel();
    private HorizontalPanel headerBar = new HorizontalPanel();
    //    private Image close= new Image(iconCreator.getCloseExpandedMode());
    private BackButton close = new BackButton("Close");
    private SimplePanel titleBar = new SimplePanel();
    private HorizontalPanel headerButtons = new HorizontalPanel();
    private boolean showFooter = true;
    private boolean buttonClicked = false;
    private boolean isFramework = true;
    private Object owner= null;
    private boolean isCloseOnSubmit = true;
    private int toolbarTopSizeDelta= 50;
    private boolean closeButtonEnabled= true;


    public Toolbar() {

        DockPanel wrapper = new DockPanel();

        leftToolbar = new TTabBar();
        leftToolbar.setStylePrimaryName("DropDownToolBar");
        rightToolbar = new TTabBar();
        rightToolbar.setStylePrimaryName("DropDownToolBar");
        rightToolbar.addStyleName("right");
        centerToolbar = new TTabBar();
        centerToolbar.setStylePrimaryName("DropDownToolBar");
        centerToolbar.addStyleName("center");

        headerBar.add(close);
        GwtUtil.setHidden(close, true);
        close.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                close();
            }
        });
        GwtUtil.setStyle(close, "marginLeft", "20px");
        headerBar.setWidth("100%");
        headerBar.add(GwtUtil.getFiller(10, 1));
        headerBar.setStyleName("header");
        GwtUtil.setStyles(headerBar, "paddingLeft", "0px", "paddingTop", "5px");
        headerBar.add(headerButtons);
        headerBar.add(GwtUtil.getFiller(30, 1));
        headerBar.add(titleBar);
        titleBar.setStyleName("title-bar");
        headerBar.setCellHorizontalAlignment(titleBar, HasHorizontalAlignment.ALIGN_LEFT);
        headerBar.setCellWidth(titleBar, "100%");
//        GwtUtil.setStyles(DOM.getParent(headerButtons.getElement()), "minWidth", "200px");

        dpanel = new CollapsiblePanel(null);
        dpanel.setStylePrimaryName("DropDownToolBar");
        dpanel.collapse();
        dpanel.setSize("100%", "100%");
        GwtUtil.setStyle(dpanel, "marginTop", "2px");

        SimplePanel sep = new SimplePanel();
        DockPanel tbar = new DockPanel();
        tbar.add(leftToolbar, DockPanel.WEST);
        tbar.add(sep, DockPanel.WEST);
        tbar.add(centerToolbar, DockPanel.CENTER);
        tbar.add(rightToolbar, DockPanel.EAST);
        tbar.setCellWidth(rightToolbar, "10px");
        tbar.setCellWidth(leftToolbar, "10px");
        tbar.setCellHorizontalAlignment(centerToolbar, DockPanel.ALIGN_LEFT);
        tbar.setCellWidth(sep, "20px");
        tbar.setWidth("100%");

        wrapper.add(tbar, DockPanel.NORTH);
        wrapper.add(dpanel, DockPanel.CENTER);

        mainPanel.add(headerBar, DockPanel.NORTH);
//        mainPanel.setCellWidth(headerBar, "100%");
        content.setAlign(BaseRegion.ALIGN_MIDDLE);
//        dpanel.setContent(mainPanel);
        mainPanel.setCellHeight(headerBar, "1px");

        initWidget(wrapper);

        EventHandler eventHandler = new EventHandler();

        leftToolbar.addBeforeSelectionHandler(eventHandler);
        centerToolbar.addBeforeSelectionHandler(eventHandler);
        rightToolbar.addBeforeSelectionHandler(eventHandler);
        leftToolbar.addSelectionHandler(eventHandler);
        centerToolbar.addSelectionHandler(eventHandler);
        rightToolbar.addSelectionHandler(eventHandler);

        setAnimationEnabled(true);

        WebEventManager.getAppEvManager().addListener(Name.SEARCH_RESULT_END,
                                                      new WebEventListener() {
                                                          public void eventNotify(WebEvent ev) {
                                                              if (isCloseOnSubmit) {
                                                                  close();
                                                              }
                                                          }
                                                      });

        WebEventManager.getAppEvManager().addListener(Name.REGION_REMOVED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                Region source = (Region) ev.getSource();
                if (LayoutManager.DROPDOWN_REGION.equals(source.getId())) {
                    setOwner(null);
                }

            }
        });

        WebEventManager.getAppEvManager().addListener(Name.REGION_SHOW, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                Region source = (Region) ev.getSource();
                if (LayoutManager.DROPDOWN_REGION.equals(source.getId())) {
                    if (!buttonClicked) {
                        deselectAll();
                    }
                    if (isFramework) {
                        Request req = Application.getInstance().getRequestHandler().getCurrentRequest();
                        setTitle(req.getShortDesc());
                        if (!buttonClicked) {
                            String name = req.getCmdName();
                            if (!select(name, false)) {
                                if (SearchPanel.getInstance().getCommandIds().contains(name)) {
                                    select(SearchCmd.COMMAND_NAME, false);
                                }
                            }
                            setOwner(req);
                        }
                    }
                    layoutContent(showFooter);
                    open();
                }
            }
        });

        WebEventManager.getAppEvManager().addListener(Name.WINDOW_RESIZE, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                if (!dpanel.isCollapsed()) {
                    ensureSize();
                }
            }
        });
    }

    public boolean isCloseOnSubmit() {
        return isCloseOnSubmit;
    }

    public void setCloseOnSubmit(boolean closeOnSubmit) {
        isCloseOnSubmit = closeOnSubmit;
    }

    @Deprecated
    /** trey should go and switch over to setHeaderButtons() **/
    public Panel getHeaderButtons() {
        return headerButtons;
    }

    public void setHeaderButtons(Widget w) {
        headerButtons.clear();
        headerButtons.add(w);
    }

    public void setTitle(String title) {
        Label l = new Label(title);
        GwtUtil.setStyles(l, "fontSize", "13pt", "paddingTop", "7px");
        if (GwtUtil.isHidden(close.getElement())) {
            GwtUtil.setStyle(l, "paddingLeft", "60px");
        }
        setTitle(l);
    }

    public void setCloseText(String text) {
        close.setDesc(text);
    }

    public void setTitle(Widget title) {
        titleBar.setWidget(title);
    }

    public Region getContentRegion() {
        return content;
    }

    public Region getFooterRegion() {
        return footer;
    }

    public WebEventManager getEventManager() {
        return eventManager;
    }

    public void setAnimationEnabled(boolean enabled) {
        dpanel.setAnimationEnabled(enabled);
    }

    public void setOwner(Object owner) {
       this.owner= owner;
    }

    public Object getOwner() {
        return this.owner;
    }

    public Widget getWidget() {
        return this;
    }

    public void setContent(Widget content) {
        setContent(content, true, null,null);
    }

    public void setContent(Widget content, boolean showFooter) {
        setContent(content,showFooter,null,null);
    }

    public void setContent(Widget content, boolean showFooter, Object owner, String cmdName) {
        this.showFooter = showFooter;
        isFramework = false;
        Widget oc = this.content.getContent();
        if (oc != null) {
            oc.removeStyleName("shadow");
        }
        closeButtonEnabled= true;
        this.content.setDisplay(content);
        setOwner(owner);
        this.showFooter = true;
        isFramework = true;
        if (cmdName!=null) {
            if (!select(cmdName, false)) {
                if (SearchPanel.getInstance().getCommandIds().contains(cmdName)) {
                    select(SearchCmd.COMMAND_NAME, false);
                }
            }
        }
    }

    private void layoutContent(boolean showFooter) {
//        clearHeaderBar();
        mainPanel.add(content.getDisplay(), DockPanel.CENTER);
        mainPanel.add(footer.getDisplay(), DockPanel.SOUTH);
        footer.getDisplay().setStyleName("footer");
        footer.getDisplay().setVisible(showFooter);
        mainPanel.setCellHeight(content.getDisplay(), "100%");
        dpanel.setContent(mainPanel);
        GwtUtil.setStyles(mainPanel.getParent(), "width", "100%");
        if (content.getContent() != null) {
            if (showFooter) {
                content.getContent().addStyleName("shadow");
            } else {
                content.getContent().removeStyleName("shadow");
            }
        }
        updateCloseVisibility();

    }

    private void updateCloseVisibility() {
        // hide close button when default button is selected without results.
        if (isDefaultTabSelected()) {
            GwtUtil.setHidden(close, getShouldHideCloseOnDefaultTab());
        } else {
            GwtUtil.setHidden(close, false);
        }
        close.setVisible(closeButtonEnabled);
    }

    public void setCloseButtonEnabled(boolean enabled) {
        closeButtonEnabled= enabled;
        close.setVisible(closeButtonEnabled);
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
                updateCloseVisibility();
                return success;
            }
        }
        return false;
    }


    public boolean isOpen() {
        return !dpanel.isCollapsed();
    }

    public void open() {
        open(true);
    }

    public void open(boolean doAnimate) {
        setAnimationEnabled(doAnimate);
        dpanel.expand();
        WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.DROPDOWN_OPEN));
        setAnimationEnabled(true);
    }

    public void close() {
        close(true);
    }

    public void close(boolean doAnimate) {
        closeButtonEnabled= true;
        setAnimationEnabled(doAnimate);
        owner= null;
        dpanel.collapse();
        deselectAll();
        clearHeaderBar();
        Widget oc = content.getContent();
        if (oc != null) {
            oc.removeStyleName("shadow");
        }
        content.clear();
        WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.DROPDOWN_CLOSE));
        if (body != null) {
            GwtUtil.setStyle(body, "overflow", "visible");
            body.setHeight("100%");
        }
        if (getShouldExpandDefault()) expandDefault();

        setAnimationEnabled(true);
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
    //==================================================================
    //------------------ End tweak methods
    //==================================================================

    private void clearHeaderBar() {
        titleBar.clear();
        headerButtons.clear();
        close.setDesc("Close");
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
        addButton(button, Align.CENTER);
    }

    public void addButton(Button button, int idx) {
        addButton(button, Align.CENTER, idx);
    }

    public void addButton(Button button, Align align) {
        addButton(button, align, -1);
    }

    public void addButton(Button button, Align align, int idx) {
        TTabBar tb = align == Align.LEFT ? leftToolbar : align == Align.CENTER ? centerToolbar : rightToolbar;
        idx = idx < 0 || idx > tb.getTabCount() ? tb.getTabCount() : idx;
        tb.insertTab(button.asWidget(), idx);
        TabBar.Tab t = tb.getTab(idx);
        tabs.put(button.getName(), new TabHolder(t, button, tb));
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

    public Dimension getAvailContentSize() {
        Dimension dim = getDropDownSize();
        if (GwtUtil.isOnDisplay(footer.getDisplay())) {
            int h = dim.getHeight();
            int w = dim.getWidth();

            h = h - 76 - GwtUtil.getElementHeight(footer.getDisplay());
            w = w - 66;
            dim = new Dimension(w, h);
        }
        return dim;

    }

    public Dimension getDropDownSize() {
        int top = dpanel.getAbsoluteTop() + GwtUtil.getElementHeight(headerBar);
        int h = Window.getClientHeight() - top - 15;
        int w = Window.getClientWidth() - 10;

        int minH = Application.getInstance().getLayoutManager().getMinHeight();
        return new Dimension(w, Math.max(minH, h));
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

    public void setToolbarTopSizeDelta(int delta) {
        toolbarTopSizeDelta= delta;
    }

    private void ensureSize() {
        Dimension dim = getDropDownSize();
        mainPanel.setSize("100%", dim.getHeight() + 5 + "px");

        if (content.getContent() instanceof RequiresResize) {
            dim = getAvailContentSize();
            content.getContent().setPixelSize(dim.getWidth(), dim.getHeight()-20);
            ((RequiresResize) content.getContent()).onResize();
        }

        body = RootPanel.get(Application.getInstance().getCreator().getLoadingDiv());
//        GwtUtil.setStyle(body, "overflow", "hidden");
        int minH = Application.getInstance().getLayoutManager().getMinHeight();
        body.setHeight(Math.max(minH, this.getOffsetHeight() + toolbarTopSizeDelta)+ "px");

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
                clearHeaderBar();
            }
        }

        public void onSelection(SelectionEvent<Integer> ise) {
            TabBar tb = (TabBar) ise.getSource();
            int idx = tb.getSelectedTab();
            if (idx == cSelIdx) {
                tb.selectTab(-1, false);
                close();
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

        public void setUseDropdown(boolean useDropdown);
    }

    public static class CmdButton extends Composite implements Button {
        Command command;
        String name;
        boolean useDropdown = false;
        HTML html;


        public CmdButton(String name, String label, String desc, Command cmd) {
            this.name = name;
            String htmlstr = label == null ? name : label;
            html = new HTML(htmlstr);
            if (desc != null) {
                html.setTitle(desc);
            }
            this.command = cmd;
            initWidget(html);
            html.setWordWrap(false);
            if (command instanceof GeneralCommand) {
                addListeners();
                setButtonEnabled(((GeneralCommand)command).isEnabled());
            }
        }

        public CmdButton(String name, Widget w, Command cmd) {
            this.name = name;
            this.command = cmd;
            initWidget(w);
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

    public static class SearchButton extends RequestButton {

        public SearchButton(String name, String searchId) {
            super(name, searchId);
        }

        public SearchButton(String name, String searchId, String label, String desc) {
            super(name, searchId, label, desc);
            getRequest().setIsSearchResult(true);
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