package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.commands.SearchCmd;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.layout.BaseRegion;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;


/**
 * Date: Jun 20, 2008
 *
 * @author loi
 * @version $Id: Toolbar.java,v 1.39 2012/05/16 01:39:04 loi Exp $
 */
public class ToolbarDropdown extends Composite {

    private CollapsiblePanel dpanel;
    private WebEventManager eventManager = new WebEventManager();
    private BaseRegion content = new DropdownRegion();
    private BaseRegion footer = new BaseRegion(LayoutManager.FOOTER_REGION);
    private BaseRegion alertsRegion = new BaseRegion(LayoutManager.ALERTS_REGION);
    private DockPanel mainPanel = new DockPanel();
    private HorizontalPanel headerBar = new HorizontalPanel();
    //    private Image close= new Image(iconCreator.getCloseExpandedMode());
    private BackButton close = new BackButton("Close");
    private SimplePanel titleBar = new SimplePanel();
    private HorizontalPanel headerButtons = new HorizontalPanel();
    private boolean showFooter = true;
    private Object owner= null;
    private boolean isCloseOnSubmit = true;
    private boolean closeButtonEnabled= true;

    public ToolbarDropdown() {

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
        dpanel.setWidth("100%");

        alertsRegion.hide();
        mainPanel.add(alertsRegion.getDisplay(), DockPanel.NORTH);
        mainPanel.add(headerBar, DockPanel.NORTH);
        content.setAlign(BaseRegion.ALIGN_MIDDLE);
        mainPanel.setCellHeight(headerBar, "1px");

        initWidget(dpanel);
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
                    layout();
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

    public Region getAlertsRegion() {
        return alertsRegion;
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
        Widget oc = this.content.getContent();
        if (oc != null) {
            oc.removeStyleName("shadow");
        }
        closeButtonEnabled= true;
        this.content.setDisplay(content);
        setOwner(owner);
        this.showFooter = true;

        Toolbar toolbar = Application.getInstance().getToolBar();
        if (cmdName!=null && toolbar != null) {
            if (!toolbar.select(cmdName, false)) {
                if (SearchPanel.getInstance().getCommandIds().contains(cmdName)) {
                    toolbar.select(SearchCmd.COMMAND_NAME, false);
                }
            }
        }
    }

    public void layout() {
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

    public void updateCloseVisibility() {
        // hide close button when default button is selected without results.
        Toolbar toolbar = Application.getInstance().getToolBar();
        if (toolbar.isDefaultTabSelected()) {
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

    public boolean isOpen() {
        return !dpanel.isCollapsed();
    }

    public void open() {
        open(true);
    }

    public void open(boolean doAnimate) {
        dpanel.setVisible(true);
        setAnimationEnabled(doAnimate);
        dpanel.expand();
        WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.DROPDOWN_OPEN));
        setAnimationEnabled(true);
    }

    public void close() {
        close(true);
    }

    public void close(boolean doAnimate) {

        dpanel.setVisible(false);
        closeButtonEnabled= true;
        setAnimationEnabled(doAnimate);
        owner= null;
        dpanel.collapse();
        if (Application.getInstance().getToolBar() != null) {
            Application.getInstance().getToolBar().deselectAll();
        }
        clearHeaderBar();
        Widget oc = content.getContent();
        if (oc != null) {
            oc.removeStyleName("shadow");
        }
        content.clear();
        if (getShouldExpandDefault()) {
            Toolbar toolbar = Application.getInstance().getToolBar();
            if (toolbar != null) {
                toolbar.expandDefault();
            }
        }
        WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.DROPDOWN_CLOSE));
        setAnimationEnabled(true);
    }

    public void clearHeaderBar() {
        titleBar.clear();
        headerButtons.clear();
        close.setDesc("Close");
    }

    //==================================================================
    //------------------ Protected methods that can be overridden to
    //------------------ tweak the behavior of the Toolbar
    //==================================================================


    protected boolean getShouldHideCloseOnDefaultTab() {
        return !Application.getInstance().hasSearchResult();
    }

    protected boolean getShouldExpandDefault() {
        return !Application.getInstance().hasSearchResult();
    }

    //==================================================================
    //------------------ End tweak methods
    //==================================================================

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
        int top = dpanel.getAbsoluteTop();
        int h = Window.getClientHeight() - top;
        int w = Window.getClientWidth() - 10;

        int minH = Application.getInstance().getLayoutManager().getMinHeight();
        return new Dimension(w, Math.max(minH, h));
    }

//====================================================================
//
//====================================================================

    private void ensureSize() {
        Dimension dim = getDropDownSize();
        mainPanel.setSize("100%", dim.getHeight() - 10 + "px");

        if (content.getContent() instanceof RequiresResize) {
            dim = getAvailContentSize();
            content.getContent().setPixelSize(dim.getWidth(), dim.getHeight()-20);
            ((RequiresResize) content.getContent()).onResize();
        }

    }


    class DropdownRegion extends BaseRegion {

        public DropdownRegion() {
            super(LayoutManager.DROPDOWN_REGION);
        }

        public void setDisplay(Widget display) {
            super.setDisplay(display);
            Toolbar toolbar = Application.getInstance().getToolBar();
            Request req = Application.getInstance().getRequestHandler().getCurrentRequest();
            setOwner(req);
            setTitle(req.getShortDesc());
            String name = req.getCmdName();
            Application.getInstance().getToolBar().deselectAll();
            if (!toolbar.select(name, false)) {
                if (SearchPanel.getInstance().getCommandIds().contains(name)) {
                    toolbar.select(SearchCmd.COMMAND_NAME, false);
                }
            }
        }

    }
}

