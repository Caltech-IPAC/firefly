package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ResizeComposite;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.layout.LayoutSelector;
import edu.caltech.ipac.firefly.data.*;
import edu.caltech.ipac.firefly.data.table.TableDataView;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.ResizablePanel;
import edu.caltech.ipac.firefly.ui.StatefulWidget;
import edu.caltech.ipac.firefly.ui.creator.TablePrimaryDisplay;
import edu.caltech.ipac.firefly.ui.creator.XYPlotViewCreator;
import edu.caltech.ipac.firefly.ui.gwtclone.SplitLayoutPanelFirefly;
import edu.caltech.ipac.firefly.ui.table.builder.TableConfig;
import edu.caltech.ipac.firefly.util.AsyncCallbackGroup;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;

/**
 * Date: Feb 24, 2009
 *
 * @author loi
 * @version $Id: TableGroupPreviewCombo.java,v 1.61 2012/09/18 23:08:08 tatianag Exp $
 */
public class TableGroupPreviewCombo extends ResizeComposite implements StatefulWidget {
    private WebEventListener newTableListener;

    public enum Layout {TOP, BOTTOM, LEFT, RIGHT, CENTER}

    private SplitLayoutPanelFirefly mainPanel;

    private TabPane<Widget> tabPane;
    private PreviewTabPane preview;
    private ResizablePanel previewWrapper;
    private ResizablePanel tableWrapper;
    private String stateId = "TGPC";
//    private TitlePanel tableTitlePanel;
//    private TitlePanel previewTitlePanel;
    private boolean isSwitching = false;


    public TableGroupPreviewCombo() {
        this(Layout.CENTER, Layout.RIGHT);
    }

    public TableGroupPreviewCombo(PreviewTabPane preview) {
        this(Layout.CENTER, Layout.RIGHT, preview);
    }

    public TableGroupPreviewCombo(Layout tableLO, Layout previewLO) {
        this(tableLO, previewLO, new PreviewTabPane());
    }

    public TableGroupPreviewCombo(Layout tableLO, Layout previewLO, PreviewTabPane preview) {

        mainPanel = new SplitLayoutPanelFirefly();
        mainPanel.setMinCenterSize(30,30);
        this.initWidget(mainPanel);

        tabPane = new TabPane<Widget>();

        this.preview = preview;
        this.setSize("100%", "100%");
        if (preview != null) {
//            previewTitlePanel = new TitlePanel("Put the title here",preview);
            previewWrapper = new ResizablePanel(preview);
//            preview.setSize("100%", "100%");

            addWidget(previewLO, previewWrapper, 400);
        }

//        tableTitlePanel = new TitlePanel("Put the title here",tabPane);

        tabPane.setSize("100%", "100%");

        tableWrapper = new ResizablePanel(tabPane);
//        tableWrapper.setSize("100%", "100%");
        addWidget(tableLO, tableWrapper, 400);

//        if (preview != null) {
//            setScrollMode(previewWrapper, "visible");
//        }
//        setScrollMode(sp, "hidden");

        tabPane.getEventManager().addListener(TabPane.TAB_REMOVED, new WebEventListener(){
                public void eventNotify(WebEvent ev) {
                    TabPane.Tab<SelectableTablePanel> tab = (TabPane.Tab<SelectableTablePanel>) ev.getData();
                    if (tab.isRemovable()) {
                        TableGroupPreviewCombo.this.preview.unBind(tab.getContent());
                    }
                }
            });

        newTableListener = new WebEventListener(){
                    public void eventNotify(WebEvent ev) {
                        NewTableResults tableInfo = (NewTableResults) ev.getData();
                        if (tableInfo != null && tableInfo.getConfig() != null) {
                            final TableConfig conf = tableInfo.getConfig();

                            final TablePanel table = addTable(conf.getTitle(), conf.getLoader(), true);
                            table.init(new AsyncCallback<Integer>(){
                                public void onFailure(Throwable caught) {}
                                public void onSuccess(Integer result) {
                                    tabPane.selectTab(tabPane.getTab(table.getName()));
                                    DownloadSelectionIF downloadIF = conf.getDownloadSelectionIF();
                                    if (downloadIF != null) {
                                        TablePrimaryDisplay.createDownloadButton(table, downloadIF, conf.getDownloadRequest());
                                    }
                                }
                            });
                        }
                    }
                };

        WebEventManager.getAppEvManager().addListener(Name.NEW_TABLE_RETRIEVED, newTableListener);
        tabPane.addCloseHandler(new CloseHandler<TabPane>(){
                    public void onClose(CloseEvent<TabPane> tabPaneCloseEvent) {
                        WebEventManager.getAppEvManager().removeListener(Name.NEW_TABLE_RETRIEVED, newTableListener);
                    }
                });

        LayoutSelector loSel = Application.getInstance().getLayoutManager().getLayoutSelector();
        loSel.setHub(preview.getEventHub());
    }

    public TablePanel addTable(String name, Loader<TableDataView> loader) {
        return addTable(name, loader, false);
    }

    public TablePanel addTable(String name, Loader<TableDataView> loader, boolean closeable) {
        SelectableTablePanel table = new SelectableTablePanel(name, loader);
        addTable(table, closeable);
        return table;
    }

    public void addTable(TablePanel table) {
        addTable(table, false);
    }

    public void addTable(TablePanel table, boolean closeable) {
        TabPane.Tab t = tabPane.addTab(table, table.getName(), table.getShortDesc(), closeable);
        table.setName(t.getName());

        if (tabPane.getWidgetCount() == 1) {
            tabPane.selectTab(0);
        }
        if (preview != null) {
            preview.bind(table);
        }
    }

    public void addTab(Widget content, String name,  boolean closeable) {
        TabPane.Tab t = tabPane.addTab(content, name);
        t.setRemovable(closeable);

        if (tabPane.getWidgetCount() == 1) {
            tabPane.selectTab(0);
        }
    }

    public TablePanel getSelectedTable() {
        Widget w = tabPane.getSelectedTab() == null ? null :
                    tabPane.getSelectedTab().getContent();
        return (w instanceof TablePanel) ? (TablePanel) w : null;
    }

    public TabPane getTabPane() {
        return tabPane;
    }

    public TablePanel getTablePanel(String name) {
        TabPane.Tab<Widget> t = tabPane.getVisibleTab(name);
        return (t != null && t.getContent() instanceof TablePanel) ?
                    (TablePanel)t.getContent() : null;
    }

    public PreviewTabPane getPreview() {
        return preview;
    }

    public void unmaskTable(String name) {
        TabPane.Tab t = tabPane.getVisibleTab(name);
        if (t != null) {
            t.unmask();
        }
    }

    public void maskTable(String name, String msg) {
        TabPane.Tab t = tabPane.getVisibleTab(name);
        if (t != null) {
            t.mask(msg);
        }
    }

    public String getStateId() {
        return stateId;
    }

    public void setStateId(String id) {
        stateId = id;
        getTabPane().setStateId(id + "_Table");
        if (getPreview() != null) {
            getPreview().setStateId(id + "_Preview");
        }
    }

    public void recordCurrentState(Request request) {
        getTabPane().recordCurrentState(request);
        if (getPreview() != null) {
            getPreview().recordCurrentState(request);
        }
    }

    public void moveToRequestState(final Request request, AsyncCallback callback) {
        final AsyncCallbackGroup acg = new AsyncCallbackGroup(callback);

        AsyncCallback cb = acg.newCallback(new Command() {
            public void execute() {
                if (getPreview() != null) {
                    if (getTabPane().getSelectedTab().getContent() instanceof TablePanel) {
                        getPreview().getEventHub().getEventManager().fireEvent(new WebEvent(getTabPane().getSelectedTab().getContent(),
                                TablePreviewEventHub.ON_TABLE_SHOW));
                        getPreview().moveToRequestState(request, acg.newCallback());
                    }
                }
            }
        }, null);
        getTabPane().moveToRequestState(request, cb);
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