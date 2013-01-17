package edu.caltech.ipac.firefly.ui;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.table.NewTableEventHandler;
import edu.caltech.ipac.firefly.ui.table.PreviewTabPane;
import edu.caltech.ipac.firefly.ui.table.TabPane;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.ui.table.TablePreviewEventHub;
import edu.caltech.ipac.firefly.ui.table.TextView;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

/**
 * Date: Nov 29, 2010
 *
 * @author loi
 * @version $Id: TabPreviewCombo.java,v 1.7 2012/01/11 00:30:15 loi Exp $
 */
public class TabPreviewCombo extends ResizeComposite {
    private boolean isSwiching;

    public enum Layout {TOP, BOTTOM, LEFT, RIGHT, CENTER}

    private SplitLayoutPanel mainPanel;
    private TablePreviewEventHub eventHub = new TablePreviewEventHub();

    private TabPane<Widget> centerTab;
    private PreviewTabPane previews;

    private ShadowedPanel previewWrapper;
    private ShadowedPanel centerWrapper;
    private TitlePanel centerTitlePanel;
    private TitlePanel previewTitlePanel;


    public TabPreviewCombo() {
        this(Layout.RIGHT);
    }

    public TabPreviewCombo(Layout previewLO) {

        mainPanel = new SplitLayoutPanel();
        this.initWidget(mainPanel);

        centerTab = new TabPane<Widget>();
        previews = new PreviewTabPane(eventHub);

        this.setSize("100%", "100%");
        if (previewLO != null) {
            previewTitlePanel = new TitlePanel("Put the title here", previews);
            previewWrapper = new ShadowedPanel(previewTitlePanel);
            addWidget(previewLO, previewWrapper, 400);
        }

        centerTitlePanel = new TitlePanel("Put the title here", centerTab);

        centerTab.setSize("100%", "100%");

        centerWrapper = new ShadowedPanel(centerTitlePanel);
        addWidget(Layout.CENTER, centerWrapper, 400);

        centerTab.getEventManager().addListener(TabPane.TAB_ADDED, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    TabPane.Tab tab = (TabPane.Tab) ev.getData();
                    if (tab != null && tab.getContent() instanceof TablePanel) {
                        tableAdded((TablePanel) tab.getContent());
                    }
                }
            });

        final NewTableEventHandler newTableListener = new NewTableEventHandler(eventHub, centerTab);
        centerTab.addCloseHandler(new CloseHandler<TabPane>(){
                    public void onClose(CloseEvent<TabPane> tabPaneCloseEvent) {
                        WebEventManager.getAppEvManager().removeListener(Name.NEW_TABLE_RETRIEVED, newTableListener);
                    }
                });
        showPreviews(false);
    }

    private void tableAdded(final TablePanel table) {

        showPreviews(true);

        for(final TablePanel.View v : table.getViews()) {
            table.getEventManager().addListener(v.getName(), new WebEventListener(){
                        public void eventNotify(WebEvent ev) {
                            if (!isSwiching) {
                                isSwiching = true;
                                for (int i = 0; i < centerTab.getWidgetCount(); i++) {
                                    TabPane.Tab<Widget> tab = centerTab.getVisibleTab(i);
                                    if (tab != null && tab.getContent() instanceof TablePanel) {
                                        ((TablePanel)tab.getContent()).switchView(v.getName());
                                    }
                                }
                                if (v.getName().equals(TextView.NAME)) {
                                    showPreviews(false);
                                } else {
                                    showPreviews(true);
                                }
                                isSwiching = false;
                            }
                        }
                    });
        }


        table.getEventManager().addListener(TablePanel.ON_DETACH, new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        eventHub.unbind(table);
                        showPreviews(eventHub.getTables().size() > 0);
                    }
                });

        if (centerTab.getWidgetCount() == 1) {
            centerTab.selectTab(0);
        }
        eventHub.bind(table);
    }

    public void showPreviews(boolean show) {
        if (show) {
            GwtUtil.SplitPanel.showWidget(mainPanel, previewWrapper);
        } else {
            GwtUtil.SplitPanel.hideWidget(mainPanel, previewWrapper);
        }
    }

    public TitlePanel getCenterTitlePanel() {
        return centerTitlePanel;
    }

    public TitlePanel getPreviewTitlePanel() {
        return previewTitlePanel;
    }

    public void setPreviewTitle(String titleStr) {
        if (previewTitlePanel != null) {
            previewTitlePanel.setTitle(titleStr);
        }
    }

    public void setTabGroupTitle(String titleStr) {
        centerTitlePanel.setTitle(titleStr);
    }

    public PreviewTabPane getPreviews() {
        return previews;
    }

    public TabPane.Tab addTab(Widget w, String title) {
        return addTab(w, title, null, false);
    }

    public TabPane.Tab addTab(Widget w, String title, String tips, boolean closeable) {
        return centerTab.addTab(w, title, tips, closeable);
    }

//    public TablePanel addTable(String name, Loader<TableDataView> loader) {
//        return addTable(name, loader, false);
//    }
//
//    public TablePanel addTable(String name, Loader<TableDataView> loader, boolean closeable) {
//        SelectableTablePanel table = new SelectableTablePanel(name, loader);
//        addTable(table, closeable);
//        return table;
//    }
//
//    public void addTable(TablePanel table) {
//        addTable(table, false);
//    }
//
//    public void addTable(final TablePanel table, boolean closeable) {
//        TabPane.Tab t = centerTab.addTab(table, table.getName(), table.getShortDesc(), closeable);
//        table.setName(t.getName());
//
//        table.getEventManager().addListener(TablePanel.ON_VIEW_ASTABLE, new WebEventListener(){
//                    public void eventNotify(WebEvent ev) {
//                        if (!isSwiching) {
//                            isSwiching = true;
//                            for (int i = 0; i < centerTab.getWidgetCount(); i++) {
//                                Widget w = centerTab.getVisibleTab(i).getContent();
//                                if (w instanceof TablePanel) {
//                                    ((TablePanel)w).viewAsTable();
//                                }
//                            }
//                            showPreviews(true);
//                            isSwiching = false;
//                        }
//                    }
//                });
//
//        table.getEventManager().addListener(TablePanel.ON_VIEW_ASTEXT, new WebEventListener(){
//                    public void eventNotify(WebEvent ev) {
//                        if (!isSwiching) {
//                            isSwiching = true;
//                            for (int i = 0; i < centerTab.getWidgetCount(); i++) {
//                                Widget w = centerTab.getVisibleTab(i).getContent();
//                                if (w instanceof TablePanel) {
//                                    ((TablePanel)w).viewAsText();
//                                }
//                            }
//                            showPreviews(false);
//                            isSwiching = false;
//                        }
//                    }
//                });
//        table.getEventManager().addListener(TablePanel.ON_DETACH, new WebEventListener(){
//                    public void eventNotify(WebEvent ev) {
//                        eventHub.unbind(table);
//                        showPreviews(eventHub.getTables().size() > 0);
//                    }
//                });
//
//        if (centerTab.getWidgetCount() == 1) {
//            centerTab.selectTab(0);
//        }
//        eventHub.bind(table);
//    }

//    public TablePanel getSelectedTable() {
//        Widget w = centerTab.getSelectedTab() == null ? null :
//                centerTab.getSelectedTab().getContent();
//        if (w instanceof TablePanel) {
//            return (TablePanel) w;
//        }
//        return null;
//    }

    public TabPane getTabPane() {
        return centerTab;
    }

    public TablePanel getTablePanel(String name) {
        TabPane.Tab t = centerTab.getVisibleTab(name);
        if (t.getContent() instanceof TablePanel) {
            return (TablePanel) t.getContent();
        }
        return null;
    }

    public void unmaskTable(String name) {
        TabPane.Tab t = centerTab.getVisibleTab(name);
        if (t != null) {
            t.unmask();
        }
    }

    public void maskTable(String name, String msg) {
        TabPane.Tab t = centerTab.getVisibleTab(name);
        if (t != null) {
            t.mask(msg);
        }
    }

    public boolean isActive() {
        return GwtUtil.isOnDisplay(this);
    }

    private void addWidget(Layout lo, Widget w, double size) {
        switch (lo) {
            case TOP : mainPanel.addNorth(w, size); break;
            case BOTTOM : mainPanel.addSouth(w, size); break;
            case LEFT : mainPanel.addWest(w, size); break;
            case RIGHT : mainPanel.addEast(w, size); break;
            default : mainPanel.add(w); break;
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