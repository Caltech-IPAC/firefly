package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.SimplePanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.BaseCallback;
import edu.caltech.ipac.firefly.core.background.BackgroundUIHint;
import edu.caltech.ipac.firefly.core.background.MonitorItem;
import edu.caltech.ipac.firefly.data.DownloadRequest;
import edu.caltech.ipac.firefly.data.TableServerRequest;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.creator.PrimaryTableUI;
import edu.caltech.ipac.firefly.ui.creator.TablePanelCreator;
import edu.caltech.ipac.firefly.ui.creator.TablePrimaryDisplay;
import edu.caltech.ipac.firefly.ui.creator.WidgetFactory;
import edu.caltech.ipac.firefly.ui.table.builder.BaseTableConfig;
import edu.caltech.ipac.firefly.ui.table.builder.TableConfig;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.ui.BaseLayoutElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: 7/1/14
 *
 * @author loi
 * @version $Id: $
 */
public class TableResultsDisplay extends BaseLayoutElement {

    private static final MonItemCreatedListener newMonItemListener = new MonItemCreatedListener();
    private TabPane tabpane;
    private List<TableHolder> onDisplay = new ArrayList<TableHolder>();
    private EventHub hub;

    public TableResultsDisplay() {
        this(Application.getInstance().getEventHub());
    }

    public TableResultsDisplay(EventHub hub) {
        this.hub = hub;
        tabpane = new TabPane();
        tabpane.setSize("100%", "100%");
        newMonItemListener.setTableDisplay(this);

        this.hub.bind(tabpane);

        tabpane.getEventManager().addListener(TabPane.TAB_REMOVED, new WebEventListener() {
                    public void eventNotify(WebEvent ev) {
                        TabPane.Tab tab = (TabPane.Tab) ev.getData();
                        TableHolder th = getTableHolder(tab);
                        if (th != null) {
                            onDisplay.remove(th);
                        }
                        fireEvent(EventType.CONTENT_CHANGED);
                    }
                });
        tabpane.getEventManager().addListener(TabPane.TAB_SELECTED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                TabPane.Tab tab = (TabPane.Tab) ev.getData();
                TableHolder th = getTableHolder(tab);
                if (th != null && th.table != null) {
                    th.table.onShow();
                }
            }
        });

        setContent(tabpane);
    }

    public TabPane getTabPane() { return tabpane; }

    public void addTable(MonitorItem item) {
        TableHolder th = getTableHolder(item);
        if (th == null) {
            th = new TableHolder(item);
            onDisplay.add(th);
        }
        tabpane.selectTab(th.getTab());
        fireEvent(EventType.CONTENT_CHANGED);
    }

    /**
     * override hasContent to return true when there's at least 1 table.
     * @return
     */
    public boolean hasContent() {
        return onDisplay.size() > 0;
    }

//====================================================================
//
//====================================================================

    private TableHolder getTableHolder(MonitorItem item) {
        for (TableHolder th : onDisplay) {
            if (item.getStatus() != null && th.getItem().getID().equals(item.getID())) {
                return th;
            }
        }
        return null;
    }

    private TableHolder getTableHolder(TabPane.Tab item) {
        for (TableHolder th : onDisplay) {
            if (item != null && item.equals(th.tab)) {
                return th;
            }
        }
        return null;
    }

//====================================================================
//
//====================================================================

    /**
     * inter
     */
    public class TableHolder implements MonitorItem.UpdateListener {

        private final HTML title= new HTML();
        private final HTML status= new HTML();
        private final Image workingIcon= new Image(GwtUtil.LOADING_ICON_URL);
        private final SimplePanel iconHolder = new SimplePanel();
        private MonitorItem item;
        private TabPane.Tab tab;
        private TablePanel table;

        public TableHolder(MonitorItem item) {
            item.addUpdateListener(this);
            this.title.setHTML(item.getTitle());
            this.item = item;
            init();
        }

        public MonitorItem getItem() {
            return item;
        }

        public TabPane.Tab getTab() {
            return tab;
        }

        //====================================================================
        //  implements UpdateListener
        //====================================================================
        public void update(MonitorItem item) {
            String titleStr = item.getTitle();
            String stateStr= "";
            iconHolder.clear();
            if (item.getStatus().isActive()) iconHolder.setWidget(workingIcon);

            switch (item.getState()) {
                case WAITING:
                case STARTING:
                case WORKING:
                    tab.mask("working...");
                    titleStr = "<img src='" + GwtUtil.LOADING_ICON_URL +"' height='11' width='11'/>&nbsp;" + titleStr;
                    break;
                case USER_ABORTED: stateStr= "aborted";
                    tab.unmask();
                    break;
                case UNKNOWN_PACKAGE_ID:
                case FAIL:         stateStr= "Failed";
                    tab.unmask();
                    break;
                case SUCCESS:
                    tab.unmask();
                    showTable();
                    break;
            case CANCELED:     stateStr= "Canceled";
            tab.unmask();
            break;
        }
        tab.setLabel(titleStr);
            title.setHTML(titleStr);
            status.setHTML(stateStr);
        }

        private void init() {
            title.setWidth("250px");
            GwtUtil.setStyle(title, "textAlign", "left");
            FlowPanel working = new FlowPanel();
            working.add(status);
            GwtUtil.setStyle(status, "display", "inline");
            GwtUtil.setStyle(iconHolder, "display", "inline");
            tab = tabpane.addTab(working, item.getTitle(), item.getTitle(), true);
        }

        private void showTable() {
            final TableConfig tconfig = new BaseTableConfig((TableServerRequest) item.getRequest(), item.getTitle(), item.getTitle());
            WidgetFactory factory = Application.getInstance().getWidgetFactory();
            Map<String, String> params = new HashMap<String, String>(2);
            params.put(TablePanelCreator.TITLE, tconfig.getTitle());
            params.put(TablePanelCreator.SHORT_DESC, tconfig.getShortDesc());
            final PrimaryTableUI tableUI = factory.createPrimaryUI(WidgetFactory.TABLE, tconfig.getSearchRequest(), params);
            tableUI.bind(hub);
            tableUI.load(new BaseCallback<Integer>() {
                public void doSuccess(Integer result) {
                    DownloadRequest dlreq = tconfig.getDownloadRequest();
                    if (dlreq != null) {
                        tableUI.addDownloadButton(tconfig.getDownloadSelectionIF(), dlreq.getRequestId(),
                                dlreq.getFilePrefix(), dlreq.getTitlePrefix(), null);
                    }
                    tab.setContent(tableUI.getDisplay());
                    if (tableUI instanceof TablePrimaryDisplay) {
                        table = ((TablePrimaryDisplay) tableUI).getTable();
                        table.getTable().setShowUnits(true);
                        table.getTable().showFilters(true);
                    }
                }
            });
        }
    }

    private static class MonItemCreatedListener implements WebEventListener<MonitorItem> {
        private TableResultsDisplay tableDisplay;

        private MonItemCreatedListener() {
            WebEventManager.getAppEvManager().addListener(Name.MONITOR_ITEM_CREATE, this);
        }

        public void eventNotify(WebEvent<MonitorItem> ev) {
            MonitorItem item= ev.getData();
            if (item.getUIHint()== BackgroundUIHint.RAW_DATA_SET) {
                if (tableDisplay != null) {
                    tableDisplay.addTable(item);
                }
            }
        }

        public void setTableDisplay(TableResultsDisplay tableDisplay) {
            this.tableDisplay = tableDisplay;
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
