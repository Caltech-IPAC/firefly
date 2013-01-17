package edu.caltech.ipac.firefly.ui.panels;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.GeneralCommand;
import edu.caltech.ipac.firefly.core.Preferences;
import edu.caltech.ipac.firefly.data.MenuItemAttrib;
import edu.caltech.ipac.firefly.data.Param;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author tatianag
 *         $Id: SearchPanel.java,v 1.34 2012/08/09 01:09:28 loi Exp $
 */
public class SearchPanel extends Composite {

    private static String LAST_SEARCH_PREF = "LastSearch";

    private SimplePanel formPanel = new SimplePanel();
    private List<LinksPanel.ListItem> items = new ArrayList<LinksPanel.ListItem>();
    private List<String> ids = new ArrayList<String>();
    private LinksPanel.ListItem current = null;
    private List<Param> addtlUrlParams = null;
    private List<LinksPanel.ULList> lists = new ArrayList<LinksPanel.ULList>();
//    private Label title = new Label("init");

    static SearchPanel instance = null;
    private VerticalPanel navPanel;

    private SearchPanel() {

        navPanel = new VerticalPanel();

        formPanel.addStyleName("content-panel");
        setFormAreaMinWidth("500px");

        //li
        DockPanel mainPanel = new DockPanel();
        mainPanel.addStyleName("component-background");
        mainPanel.add(navPanel, DockPanel.WEST);
        mainPanel.add(formPanel, DockPanel.CENTER);

        Element el = (Element) navPanel.getElement().getParentElement();
        if (el != null) {
            DOM.setStyleAttribute(el, "background", "#E5E5E5");
            DOM.setStyleAttribute(el, "padding", "10px 10px 10px 30px");
            DOM.setStyleAttribute(el, "borderRight", "2px outset #e8e8e8");
        } else {
            navPanel.addStyleName("search-panel-list");
        }

//        mainPanel.setWidth("100%");
        mainPanel.setCellWidth(navPanel, "200px");
        mainPanel.setCellHeight(navPanel, "100%");

        initWidget(mainPanel);
    }

    public void setFormAreaMinWidth(String minWidth) {
        GwtUtil.setStyle(formPanel, "minWidth", minWidth);
    }

    private void layout() {

        formPanel.clear();
        navPanel.clear();

        if (lists.size()==1 && lists.get(0).getWidgetCount()==1) {
            navPanel.setVisible(false);
            if (navPanel.getParent() instanceof DockPanel) {
                DockPanel dp = (DockPanel)navPanel.getParent();
                dp.remove(navPanel);
            }
        }
        else {
            for (int i = 0; i < lists.size(); i++) {
                if (i != 0) {
                    SimplePanel spacer = new SimplePanel();
                    spacer.setPixelSize(1,10);
                    navPanel.add(spacer);
                }
                LinksPanel.ULList l = lists.get(i);
                if (l != null) {
                    navPanel.add(l);
                }
            }
        }        
    }

    public static SearchPanel getInstance() {
        if (instance == null) {
            instance = new SearchPanel();
            WebEventManager.getAppEvManager().addListener(Name.REQUEST_COMMAND_LAYOUT, new WebEventListener() {

                public void eventNotify(WebEvent ev) {
                    if (ev.getData() instanceof String) {
                        instance.selectLink((String) ev.getData());
                    }
                }
            });
        }
        return instance;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    public void setFormArea(Widget w) {
        if (w == null) {
            formPanel.clear();
        } else {
            formPanel.setWidget(w);
        }
    }

    public void setApplicationContext(String searchPanelTitle, List<String> commandNames) {
        setApplicationContext(searchPanelTitle, commandNames, null);
    }

    public void setApplicationContext(String searchPanelTitle, List<String> commandNames, List<Param> addtlUrlParams) {
        MenuItemAttrib menu = new MenuItemAttrib("main", searchPanelTitle, "", "", null);
        for (String s : commandNames) {
            menu.addMenuItem(new MenuItemAttrib(s, s, s, s, null));
        }
        setApplicationContext(addtlUrlParams, menu);
    }

    public void setApplicationContext(List<Param> addtlUrlParams, MenuItemAttrib... menu) {
        lists.clear();
        items.clear();
        ids.clear();
        current = null;
        this.addtlUrlParams = addtlUrlParams;

        for (MenuItemAttrib item : menu) {
            if (item.hasChildren()) {
                addItems(item);
            }
        }
        // clear form region (we don't always want a default search command to show when application context switches)
        formPanel.clear();
        layout();
    }

    private void addItems(MenuItemAttrib menu) {
        Map<String, GeneralCommand> commandTable = Application.getInstance().getCommandTable();
        LinksPanel.ULList list = null;
        for (MenuItemAttrib item : menu.getChildren()) {
            if (item.hasChildren()) {
                addItems(item);
            } else {
                GeneralCommand cmd = commandTable.get(item.getName());
                if (cmd != null) {
                    if (list == null) {
                        list = new LinksPanel.ULList(menu.getLabel());
                        final LinksPanel.ULList list1 = list;
                        list.addClickHandler(new ClickHandler() {
                            public void onClick(ClickEvent ev) {
                                processCommandRequest(list1.getLastClicked().getId());
                            }
                        });
                    }
                    LinksPanel.ListItem li = new LinksPanel.ListItem(item.getName(), cmd.getLabel());
                    DOM.setStyleAttribute(li.getElement(), "margin", "10px");
                    items.add(li);
                    ids.add(item.getName());
                    list.add(li);
                }
            }
        }
        if (list != null) {
            lists.add(list);
        }
    }

    public void processDefaultCommand() {
        String selectCmd = Preferences.get(Application.getInstance().getAppDesc() + "." + SearchPanel.LAST_SEARCH_PREF);
        if (selectCmd == null || !ids.contains(selectCmd)) {
            selectCmd = ids.size() > 0 ? ids.get(0) : null;
        }
        if (selectCmd != null) processCommandRequest(selectCmd);
    }

    public void processCommandRequest(String cmdName) {
        GeneralCommand cmd = Application.getInstance().getCommandTable().get(cmdName);
//        assert (cmd != null && cmd instanceof RequestCmd);
        Request req = new Request(cmd.getName(), cmd.getShortDesc(), true, false);
        req.setIsDrilldownRoot(true);

        if (addtlUrlParams != null) {
            for (Param p : addtlUrlParams) {
                req.setParam(p.getName(), p.getValue());
            }
        }

        Application.getInstance().processRequest(req);
    }

    public void selectLink(String cmdName) {
        int idx = 0;
        for (String s : ids) {
            if (s.equals(cmdName)) {
                selectLink(idx);
                Preferences.set(Application.getInstance().getAppDesc() + "." + SearchPanel.LAST_SEARCH_PREF, cmdName, true);
            }
            idx++;
        }
    }

    /**
     * return list of cmd
     * @return
     */
    public List<String> getCommandIds() {
        return ids;
    }

    public void selectLink(int idx) {
        selectLink(items.get(idx));
    }

    private void selectLink(LinksPanel.ListItem li) {

        for (LinksPanel.ListItem l : items) {
            if (li == l) {
                if (current != null) current.setCurrent(false);
                current = l;
                current.setCurrent(true);

//                current.setStyleName("used-link");
                current.addStyleName("highlight-text");
                formPanel.setVisible(true);
                return;
            }
        }
    }


    private Widget makeOpenWidget(String title) {
        final Label tLabel = new Label(title == null ? "Search Panel" : title);
        //tLabel.addStyleName("collapsible-panel-deflabel-standoutsize");
        tLabel.addStyleName("title-font-family");
        tLabel.addStyleName("title-color");
        tLabel.addStyleName("title-label");
        return tLabel;
    }

    private Widget makeClosedWidget() {
        FlowPanel fp = new FlowPanel();
        final Label tLabel = new Label("Search Again");
        //tLabel.addStyleName("collapsible-panel-deflabel-standoutsize");

        tLabel.addMouseOverHandler(new MouseOverHandler() {
            public void onMouseOver(MouseOverEvent event) {
                tLabel.addStyleName("marked-text");
            }
        });

        tLabel.addMouseOutHandler(new MouseOutHandler() {
            public void onMouseOut(MouseOutEvent event) {
                tLabel.removeStyleName("marked-text");
            }
        });

        String s = " - " +
                "<span style= \"font-size:80%;font-weight:normal;\">" +
                "Click here to refine your search or do another search" +
                "</span>";
        HTML html = new HTML(s);

        DOM.setStyleAttribute(tLabel.getElement(), "display", "inline");
        DOM.setStyleAttribute(html.getElement(), "display", "inline");

        fp.add(tLabel);
        fp.add(html);
        fp.addStyleName("title-font-family");
        fp.addStyleName("title-label");
        fp.addStyleName("title-color");

        return fp;
    }
}