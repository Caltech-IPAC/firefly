package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.HasCloseHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.IndexedPanel;
import com.google.gwt.user.client.ui.LayoutPanel;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.HelpManager;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.Component;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.MaskPane;
import edu.caltech.ipac.firefly.ui.StatefulWidget;
import edu.caltech.ipac.firefly.ui.VisibleListener;
import edu.caltech.ipac.firefly.util.Browser;
import edu.caltech.ipac.firefly.util.BrowserUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Date: Aug 4, 2009
 *
 * @author loi
 * @version $Id: TabPane.java,v 1.45 2012/04/10 21:12:17 roby Exp $
 */
public class TabPane<T extends Widget> extends Composite
        implements HasSelectionHandlers<Integer>,
                   HasBeforeSelectionHandlers<Integer>,
                   HasCloseHandlers<TabPane>,
                   HasWidgets,
                   RequiresResize,
                   IndexedPanel,
                   StatefulWidget {

    public static final Name TAB_ADDED = new Name("TabPane.tabAdded",
                                                    "When a new tab is added.");

    public static final Name TAB_REMOVED = new Name("TabPane.tabRemoved",
                                                    "When a tab is removed.");

    public static final Name TAB_SELECTED = new Name("TabPane.tabSelected",
                                                    "When a tab is selected.");

    private TabLayoutPanelPlus tabPanel;
    private List<Tab<T>> tabs;
//    private List<Tab<T>> visibleTabs;
    private String stateId = "TPE";
    private WebEventManager eventManager = new WebEventManager();
    private boolean forceIE6Layout = BrowserUtil.isBrowser(Browser.IE,6);
    private Tab curSelectedTab;
    private String tabPaneName;
    private Widget helpIcon;
    private AbsolutePanel wrapper;


    public HandlerRegistration addCloseHandler(CloseHandler<TabPane> handler) {
        return addHandler(handler, CloseEvent.getType());
    }

    class SelHandler implements BeforeSelectionHandler<Integer>, SelectionHandler<Integer> {
        private Tab prevTab;

        public void onBeforeSelection(BeforeSelectionEvent<Integer> ev) {
            int idx = tabPanel.getSelectedIndex();
            if (idx >= 0) {
                prevTab = getVisibleTab(idx);
            }
        }
        public void onSelection(SelectionEvent<Integer> ev) {
            Tab t = (Tab) tabPanel.getWidget(ev.getSelectedItem());
            onTabSelected(t, prevTab);
        }
    }

    public void setHelpId(String id) {
        if (helpIcon != null) {
            wrapper.remove(helpIcon);
        }
        if (!StringUtils.isEmpty(id)) {
            helpIcon = HelpManager.makeHelpIcon(id);
            wrapper.add(helpIcon);
        } else {
            helpIcon = null;
        }
    }

    public void forceLayout() {
        tabPanel.forceLayout();
    }

    void onTabSelected(Tab tab, Tab prevTab) {

        if (tab == null) return;
        if (prevTab != null) {
            if (TabPane.this.forceIE6Layout) tabPanel.forceLayout();
            else                             prevTab.onResize();
        }
        if (prevTab != null && prevTab.getContent() instanceof VisibleListener) {
            // workaround for AR8930
            prevTab.getContent().setVisible(false);
            ((VisibleListener)prevTab.getContent()).onHide();
        }
        if (tab.getContent() instanceof VisibleListener) {
            // workaround for AR8930
            tab.getContent().setVisible(true);
            ((VisibleListener)tab.getContent()).onShow();
        }
        curSelectedTab = TabPane.this.getSelectedTab();

        eventManager.fireEvent(new WebEvent<Tab>(this, TAB_SELECTED, curSelectedTab));
    }

    public TabPane() {
        tabs = new ArrayList<Tab<T>>();
//        visibleTabs = new ArrayList<Tab<T>>();
        tabPanel = new TabLayoutPanelPlus(20, Style.Unit.PX);
        tabPanel.setStyleName("firefly-TabPane");

        wrapper = new AbsolutePanel();
        wrapper.add(tabPanel);
        tabPanel.setSize("100%", "100%");
        initWidget(wrapper);

        SelHandler selHandler = new SelHandler();

        tabPanel.addBeforeSelectionHandler(selHandler);
        tabPanel.addSelectionHandler(selHandler);

        WebEventManager.getAppEvManager().addListener(Name.WINDOW_RESIZE,
                new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        ensureHelpPos();
                    }
                });

    }

    void ensureHelpPos() {
        if (helpIcon != null && GwtUtil.isOnDisplay(helpIcon)) {
            wrapper.setWidgetPosition(helpIcon, wrapper.getOffsetWidth()-19, 3);
        }
    }

    public String getTabPaneName() {
        if (StringUtils.isEmpty(tabPaneName)) {
            return "";
        } else {
            return tabPaneName;
        }
    }

    public void setTabPaneName(String tabPaneName) {
        this.tabPaneName = tabPaneName;
    }

    public void onResize() {
        tabPanel.onResize();
        tabPanel.adjustTabWidth();
        ensureHelpPos();
    }

    

    @Override
    protected void onLoad() {
        int selidx = tabPanel.getSelectedIndex();
        if (selidx >= 0) {
            onTabSelected(getVisibleTab(selidx), null);
        }
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand(){
                    public void execute() {
                        ensureHelpPos();
                    }
                });
    }

    public Tab<T> addTab(Tab<T> tabItem) {
        return addTab(-1, tabItem, true);
    }

    public Tab<T> addTab(int index, Tab<T> tabItem, boolean isVisible) {

        if (tabs.contains(tabItem)) {
            pickAltName(tabItem);
        }

//        if (index < 0 || index > this.getWidgetCount()) {
//            tabPanel.add(tabItem, tabItem.makeLabel());
//        } else {
//            tabPanel.insert(tabItem, tabItem.makeLabel(), index);
//        }
        index = index < 0 || index > tabs.size() ? tabs.size() : index;
        tabs.add(index, tabItem);
        if (isVisible) {
            showTab(tabItem);
        }
        eventManager.fireEvent(new WebEvent<Tab>(this, TAB_ADDED, tabItem));
        return tabItem;
    }

    public Tab<T> addTab(T tabContent, String name) {
        return addTab(tabContent, name, null, false);
    }

    public Tab<T> addTab(T tabContent, String name, String tooltips, boolean removable) {
        return addTab(-1, tabContent, name, tooltips, removable, true);
    }

    public Tab<T> addTab(T tabContent, String name, String tooltips, boolean removable, boolean visible) {
        return addTab(-1, tabContent, name, tooltips, removable, visible);
    }

    public Tab<T> addTab(int index, T tabContent, String name, String tooltips, boolean removable, boolean visible) {
        Tab<T> t = new Tab<T>(this, tabContent, removable, name, tooltips);
        return addTab(index, t, visible);
    }

    private void pickAltName(Tab<T> tabItem) {
        String name = tabItem.getName();
        int i = tabs.size();
        do {
            tabItem.setName(name + "-" + --i);
        } while ( !(tabs.contains(tabItem) || i == 0));
        tabItem.setName(name + "-" + ++i);
        if (tabItem.content instanceof TablePanel) {
            TablePanel tp = (TablePanel) tabItem.content;
            if (name.equals(tp.getShortDesc())) {
                tp.setShortDesc(tabItem.getName());
            }
        }
    }

    public WebEventManager getEventManager() {
        return eventManager;
    }

    public Tab<T> getSelectedTab() {
        int i = tabPanel.getSelectedIndex();
        if (i < 0 || i >= tabPanel.getWidgetCount()) {
            return null;
        } else {
            return (Tab<T>) tabPanel.getWidget(i);
        }
    }

    public int getSelectedIndex() {
        return tabPanel.getSelectedIndex();
    }

    public boolean isSelectedTab(Tab tab) {
        Tab t = getSelectedTab() ;
        if (t != null && t.equals(tab)) {
            return true;
        }
        return false;
    }

    public Tab<T> getTab(String name) {
        if (name != null) {
            for(Tab<T> t : tabs) {
                if (t.getName().equals(name)) {
                    return t;
                }
            }
        }
        return null;
    }

    public Tab<T> getVisibleTab(String name) {
        Tab<T> t = getTab(name);
        if (isTabVisible(t)) {
            return t;
        }
        return null;
    }

    public Tab<T> getVisibleTab(int index) {
        if (index < 0 || index > tabPanel.getWidgetCount()) return null;
        Tab<T> t = (Tab<T>)tabPanel.getWidget(index);
        return t;
    }

    public void hideTab(final Tab<T> t) {
        try {

//            final Tab<T> selTab = getSelectedTab();
            if (t != null && isTabVisible(t)) {
                if (tabPanel.remove(t)) {
                    // NOTE: if we do not re-select the current active tab, the widget ui fail to render correctly.
//                    DeferredCommand.addCommand(new Command() {
//                        public void execute() {
                            if (curSelectedTab == null || curSelectedTab == t) {
                                if (tabPanel.getWidgetCount() > 0) {
                                    tabPanel.selectTab(0);
                                }
                            } else {
                                TabPane.this.selectTab(curSelectedTab);
                            }
                            tabPanel.adjustTabWidth();
//                        }
//                    });
                }
            }

        } catch (Exception e) {
            GWT.log(e.getMessage(), e);
        }
    }

    public void showTab(final Tab<T> tab) {
        if (tab == null || isTabVisible(tab)) return;
        try {

            int idx = tabs.indexOf(tab);
            for(int i = 0; i < tabPanel.getWidgetCount(); i++) {
                Tab t = getVisibleTab(i);
                if (tabs.indexOf(t) > idx) {
                    tabPanel.insert(tab, tab.makeLabel(), i);
                    return;
                }
            }
            tabPanel.add(tab, tab.makeLabel());
        } finally {
            // NOTE: if we do not re-select the current active tab, the widget ui fail to render correctly.
//            DeferredCommand.addCommand(new Command() {
//                public void execute() {
                    if (curSelectedTab == null) {
                        if (tabPanel.getWidgetCount() > 0) {
                            tabPanel.selectTab(0);
                        }
                    } else {
                        TabPane.this.selectTab(curSelectedTab);
                    }
                    tabPanel.adjustTabWidth();
//                }
//            });
        }
    }

    public boolean isTabVisible(Tab<T> tab) {
        return tabPanel.getWidgetIndex(tab) >= 0;
    }

    public void removeTab(String name) {
        removeTab(getTab(name));
    }

    public void removeTab(Tab<T> tab) {
        if (tab != null) {
            tabs.remove(tab);
            hideTab(tab);
            eventManager.fireEvent(new WebEvent<Tab>(this, TAB_REMOVED, tab));
            if (tab.getContent() instanceof Component) {
                ((Component)tab.getContent()).onHide();
            }
        }
    }

    /**
     * this method allow you to remove the tab based on the Tab object or its content.
     * @param w the widget inside the tab to remove
     */
    public void removeTab(Widget w) {
        if (w == null) return;

        if (tabs.contains(w)) {
            removeTab((Tab<T>)w);
        } else {
            for(Tab<T> tab : tabs) {
                if (tab.getContent() != null && w.equals(tab.getContent())) {
                    removeTab(tab);
                    break;
                }
            }
        }
    }


    public TabLayoutPanel getInternalTabPanel() {
        return tabPanel;
    }


    public HandlerRegistration addSelectionHandler(SelectionHandler<Integer> h) {
        return tabPanel.addSelectionHandler(h);
    }

    public HandlerRegistration addBeforeSelectionHandler(BeforeSelectionHandler<Integer> h) {
        return tabPanel.addBeforeSelectionHandler(h);
    }

    public void add(Widget w) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public Iterator<Widget> iterator() {
        return tabPanel.iterator();
    }

    public boolean remove(Widget w) {
        throw new UnsupportedOperationException();
    }

    public Widget getWidget(int index) {
        return tabPanel.getWidget(index);
    }

    public int getWidgetCount() {
        return tabPanel.getWidgetCount();
    }

    public int getWidgetIndex(Widget child) {
        return tabPanel.getWidgetIndex(child);
    }

    public boolean remove(int index) {
        throw new UnsupportedOperationException();
    }

    public void selectTab(Tab tab) {
        curSelectedTab = tab;
        selectTab(getWidgetIndex(tab));
    }

    public void selectTab(int i) {
        if (i >=0 && i < tabPanel.getWidgetCount()) {
            tabPanel.selectTab(i);
        }
    }
//====================================================================
//  end TabPanel's interface impl
//====================================================================

//====================================================================
//  implements StatefulWidget
//====================================================================
    public String getStateId() {
        return stateId;
    }

    public void setStateId(String id) {
        stateId = id;
    }

    public void recordCurrentState(Request req) {
        Tab tab = getSelectedTab();
        if (tab != null) {
            req.setParam(getStateId() + "_selTab", tab.getName());
//            if (tab.getContent() instanceof StatefulWidget) {
//                ((StatefulWidget)tab.getContent()).recordCurrentState(req);
//            }
        }
    }

    public void moveToRequestState(Request req, AsyncCallback callback) {
        String tabname = req.getParam(getStateId() + "_selTab");

        if (!StringUtils.isEmpty(tabname)) {
            selectTab(getTab(tabname));
        }
        callback.onSuccess(null);
    }

    public boolean isActive() {
        return GwtUtil.isOnDisplay(this);
    }
//====================================================================


    class TabLayoutPanelPlus extends TabLayoutPanel {
        Element tabBar;

        public TabLayoutPanelPlus(int barHeight, Style.Unit unit) {
            super(barHeight, unit);
        }

        public void forceLayout() {  ((LayoutPanel)getWidget()).forceLayout(); }

        @Override
        protected void onDetach() {
            super.onDetach();
            CloseEvent.fire(TabPane.this, TabPane.this, true);
        }



        public void adjustTabWidth() {
            // adjust width of tab if necessary

            int count = getWidgetCount();
            int w = getOffsetWidth();
            int tabBarWidth = 0;

            if (count == 0 || w == 0) return;

            ArrayList<Tab> tabs = new ArrayList<Tab>(count);

            for(int i = 0; i < this.getWidgetCount(); i++) {
                Tab t = (Tab) getWidget(i);
                tabs.add(t);
                tabBarWidth += this.getTabWidget(i).getOffsetWidth()+30;
            }
            int extra = w * tabBarWidth==0 ? 0 : tabBarWidth - w;

            Collections.sort(tabs, new Comparator<Tab>(){
                                    public int compare(Tab tab, Tab tab1) {
                                        return tab.getLabelSize() > tab1.getLabelSize() ? 1 :
                                               tab.getLabelSize() == tab1.getLabelSize() ? 0 : -1;
                                    }
                                });

            if (extra < 0) {
                // there is room to expand the tabs
                int extraChars = Math.abs(extra) /8;
                if (extraChars > 4) {
                    boolean done = false;
                    while (!done) {
                        done = true;
                        for (int i = 0; i < tabs.size(); i++) {
                            Tab t = tabs.get(i);
                            if (t.getName().length() > t.getLabelSize()) {
                                t.setLabelSize(t.getLabelSize() + 1);
                                extraChars--;
                                done = extraChars <= 0;
                            }
                        }
                    }
                    for(int i = 0; i < this.getWidgetCount(); i++) {
                        Tab t = (Tab) getWidget(i);
                        t.updateLabel();
                    }
                }
            } else if (extra > 0) {
                // need to shrink the tabs
                int extraChars = extra / 8;
                if (extraChars > 4) {
                    boolean done = false;
                    int mx = 0;
                    while (!done) {
                        done = true;
                        for (int i = tabs.size() - 1; i >= 0; i--) {
                            Tab t = tabs.get(i);
                            if (tabs.size() == 1) {
                                t.setLabelSize(t.getLabelSize() - extraChars);
                                done = true;
                                break;
                            } else {
                                if (tabs.get(i).getLabelSize() >= mx) {
                                    int x = i == 0 ? 1 : tabs.get(i).getLabelSize() - tabs.get(i-1).getLabelSize() + 1;
                                    x = Math.min(x, extraChars);
                                    tabs.get(i).setLabelSize(tabs.get(i).getLabelSize() - x);
                                    mx = tabs.get(i).getLabelSize();
                                    extraChars -= x;
                                    done = extraChars <= 0;
                                }
                            }
                        }
                    }
                    for(int i = 0; i < this.getWidgetCount(); i++) {
                        Tab t = (Tab) getWidget(i);
                        t.updateLabel();
                    }
                }
            }
        }
    }


    public static class Tab<T extends Widget> extends LayoutPanel {
        private TabPane tabPane;
        private String name;
        private String labelStr;  // this can be html
        private String shrinkableLabelStr; // this should not be html
        private T content;
        private MaskPane maskPane;
        private boolean isRemovable;
        private String tooltips;
        private HTML label;
        private transient int labelSize;
//        private String w;
//        private String h;

//        public Tab(TabPane tabPane, T content, String name) {
//            this(tabPane, content, false, name, null);
//        }

        public Tab(TabPane tabPane, T content, boolean removable, String name, String tooltips) {
            this.tabPane = tabPane;
            this.tooltips = StringUtils.isEmpty(tooltips) ? name : tooltips;
            isRemovable = removable;
            setName(name);
            setContent(content);
//            setSize("100%", "100%");
            setStyleName("TabItem");
//            w = DOM.getStyleAttribute(content.getElement(), "width");
//            h = DOM.getStyleAttribute(content.getElement(), "height");
        }

        public void setContent(T content) {
            clear();
            this.content = content;
            add(content);
        }

        public void setSize(String w, String h) {
            tabPane.setSize(w, h);
            tabPane.forceLayout();
        }

        int getLabelSize() {
            return labelSize;
        }

        void setLabelSize(int labelSize) {
            if (labelSize <= 1) {
                this.labelSize = 1;
            } else if (labelSize >= getName().length()) {
                this.labelSize = getName().length();
            } else {
                this.labelSize = labelSize;
            }
        }

        void setName(String name) {
            this.name = name == null ? "" : name;
            labelStr= this.name;
            shrinkableLabelStr= this.name;
            setLabelSize(shrinkableLabelStr.length());
        }

        public String getName() {
            return name;
        }


        public void setToolTips(String toolTips) {
            this.tooltips= toolTips;
            this.label.setTitle(toolTips);
        }

        public void setLabelString(String labelStr) {
            setLabelString(labelStr,labelStr);
        }

        /**
         * Takes two strings to use as the label. The first can be html can represents the full uncompress version.
         * The second should <i>not</i> be html and is the one that can be shrunk for smaller labels.
         * @param labelStr the main label string can be html
         * @param shrinkableLabelStr the version of the label to be used when the label is shrunk, should not be html
         */
        public void setLabelString(String labelStr, String shrinkableLabelStr) {
            this.labelStr = labelStr;
            this.shrinkableLabelStr = shrinkableLabelStr;
            if (labelStr!=null && shrinkableLabelStr!=null)  {
                setLabel(this.labelStr);
                setLabelSize(this.shrinkableLabelStr.length());
            }
        }

        void setLabel(String label) {
            this.label.setHTML(label);
        }


        public String getPaneName() {
            return tabPane.getTabPaneName();
        }

        public T getContent() {
            return content;
        }

        public boolean isRemovable() {
            return isRemovable;
        }

        /**
         * you cannot change this after it is rendered.
         * @param removable
         */
        protected void setRemovable(boolean removable) {
            isRemovable = removable;
        }

        public void unmask() {
            if (maskPane != null) {
                maskPane.hide();
            }
        }

        public void mask(String msg) {
            if (maskPane == null) {
                maskPane = GwtUtil.mask(msg, this);
            } else {
                maskPane.showWhenUncovered();
            }
        }

        public void setBgColor(String color) {
            if (!StringUtils.isEmpty(color)) {
                DOM.setStyleAttribute(getElement(), "backgroundColor", color);
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Tab) {
                return ((Tab)obj).getName().equals(getName());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        void updateLabel() {
            if (StringUtils.shrink(shrinkableLabelStr, getLabelSize()).equals(shrinkableLabelStr)) {
                setLabel(labelStr);
            }
            else {
                setLabel(StringUtils.shrink(shrinkableLabelStr, getLabelSize()));
            }
            if (getName().length() > getLabelSize() && !shrinkableLabelStr.equals(tooltips)) {
                label.setTitle("<" + shrinkableLabelStr + ">  " + tooltips);
            } else {
                label.setTitle(tooltips);
            }
        }

        protected Widget makeLabel() {
            label = new HTML(getName(), false);
            if (tooltips != null) {
                label.setTitle(tooltips);
            }
            if (isRemovable) {
                HorizontalPanel hp = new HorizontalPanel();
                IconCreator ic= IconCreator.Creator.getInstance();
                Image img= new Image(ic.getBlueDelete10x10());
                DOM.setStyleAttribute(img.getElement(), "cursor", "pointer");
                img.addClickHandler(new ClickHandler(){
                    public void onClick(ClickEvent event) {
                        tabPane.removeTab(getName());
                        tabPane.tabPanel.adjustTabWidth();
                    }
                });
                hp.add(label);
                hp.add(GwtUtil.getFiller(5,1));
                hp.add(img);
                hp.addStyleName("removable");
                return hp;

            } else {
                return label;
            }
        }

        @Override
        public void onResize() {
            super.onResize();    //To change body of overridden methods use File | Settings | File Templates.
        }
    }

}
