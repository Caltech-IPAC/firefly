package edu.caltech.ipac.firefly.ui.table;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.RequestHandler;
import edu.caltech.ipac.firefly.data.Request;
import edu.caltech.ipac.firefly.ui.StatefulWidget;
import edu.caltech.ipac.firefly.ui.creator.drawing.DrawingLayerProvider;
import edu.caltech.ipac.firefly.ui.creator.eventworker.EventWorker;
import edu.caltech.ipac.firefly.ui.previews.TableCtx;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.draw.CatalogDisplay;
import edu.caltech.ipac.firefly.visualize.draw.DataConnectionDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * This class respresents the hub of a network of tables and previews.
 * Once a table or a preview is binded, its events will be broadcast out to
 * the rest of the network.
 *
 *
 * Date: Mar 25, 2010
 *
 * @author loi
 * @version $Id: TablePreviewEventHub.java,v 1.21 2012/07/05 23:08:51 roby Exp $
 */
public class TablePreviewEventHub implements StatefulWidget {

    public static final Name ON_DATA_LOAD           = TablePanel.ON_DATA_LOAD;
    public static final Name ON_PAGE_LOAD           = TablePanel.ON_PAGE_LOAD;
    public static final Name ON_PAGE_CHANGE         = TablePanel.ON_PAGE_CHANGE;
    public static final Name ON_PAGE_ERROR          = TablePanel.ON_PAGE_ERROR;
    public static final Name ON_PAGECOUNT_CHANGE    = TablePanel.ON_PAGECOUNT_CHANGE;
    public static final Name ON_ROWSELECT_CHANGE    = TablePanel.ON_ROWSELECT_CHANGE;
    public static final Name ON_ROWHIGHLIGHT_CHANGE = TablePanel.ON_ROWHIGHLIGHT_CHANGE;

    public static final Name ON_TAB_SELECTED        = TabPane.TAB_SELECTED;
    public static final Name ON_TAB_ADDED           = TabPane.TAB_ADDED;

    public static final Name ON_TABLE_SHOW     = new Name("TablePreviewEventHub.TableShow",
                                                    "When a table becomes visible.");
    public static final Name ON_TABLE_HIDE     = new Name("TablePreviewEventHub.TableHide",
                                                    "When a table becomes invisible");
    public static final Name ON_TABLE_INIT     = new Name("TablePreviewEventHub.TableInit",
                                                    "When a table is initialized");

    public static final Name ON_TABLE_ADDED    = new Name("TablePreviewEventHub.TableAdded",
                                                    "When a new table is added.");
    public static final Name ON_TABLE_REMOVED  = new Name("TablePreviewEventHub.TableRemoved",
                                                    "When a table is removed.");
    public static final Name ENABLE_PREVIEW    = new Name("TablePreviewEventHub.EnablePreview",
                                                    "Fired when a preview is enabled");
    public static final Name DISABLE_PREVIEW    = new Name("TablePreviewEventHub.DisablePreview",
                                                    "Fired when a preview is disabled");
    public static final Name ON_EVENT_WORKER_COMPLETE    = new Name("TablePreviewEventHub.EventWorkerComplete",
                                                    "When an EventWorker is completed with data available for listener");
    public static final Name ON_EVENT_WORKER_START    = new Name("TablePreviewEventHub.EventWorkerStart",
                                                    "When an EventWorker is invoked");
    public static final Name DRAWING_LAYER_INIT = new Name("TablePreviewEventHub.DrawingLayerInit",
                                                                    "Notifies that this drawing layer has initialized");
    public static final Name DRAWING_LAYER_REMOVE = new Name("TablePreviewEventHub.DrawingLayerRemove",
                                                           "Notifies that this drawing layer should be added");
    public static final Name DRAWING_LAYER_ADD = new Name("TablePreviewEventHub.DrawingLayerAdd",
                                                             "Notifies that this drawing layer should be removed");

    private WebEventManager eventManager = new WebEventManager();
    private List<TablePanel> tables = new ArrayList<TablePanel>(5);
    private List<TablePreview> previews = new ArrayList<TablePreview>(5);
    private List<EventWorker> evWorkers = new ArrayList<EventWorker>(5);
    private List<TableCtx> externalTables = new ArrayList<TableCtx>(3); // experimental
    private EventBridge eventBridge = new EventBridge();
    private List<StatefulWidget> statefuls = new ArrayList<StatefulWidget>(5);

    private TablePanel curActiveTable = null;
    private CatalogDisplay catalogDisplay= new CatalogDisplay(this);
    private DataConnectionDisplay dataDisplay= new DataConnectionDisplay(this);
    private List<TabPane> tabs = new ArrayList<TabPane>(1);
    private List<DockLayoutPanel> layoutPanels = new ArrayList<DockLayoutPanel>(1);


    public TablePreviewEventHub() {
        final WebEvent<TablePreviewEventHub> ev= new WebEvent<TablePreviewEventHub>(this,Name.EVENT_HUB_CREATED,this);
        WebEventManager.getAppEvManager().fireDeferredEvent(ev);
    }

    public WebEventManager getEventManager() {
        return eventManager;
    }

    public void registerStatefulComponent(StatefulWidget widget) {
        if (statefuls.size() == 0) {
            Application.getInstance().getRequestHandler().registerComponent(
                            getStateId(), RequestHandler.Context.INCL_SEARCH, this);
         }
        statefuls.add(widget);
    }

    public void bind(final TablePanel table) {
        if (!tables.contains(table)) {
            tables.add(table);
            table.getEventManager().addListener(ON_PAGE_LOAD, eventBridge);
            table.getEventManager().addListener(ON_PAGE_CHANGE, eventBridge);
            table.getEventManager().addListener(ON_PAGE_ERROR, eventBridge);
            table.getEventManager().addListener(ON_PAGECOUNT_CHANGE, eventBridge);
            table.getEventManager().addListener(ON_ROWSELECT_CHANGE, eventBridge);
            table.getEventManager().addListener(ON_ROWHIGHLIGHT_CHANGE, eventBridge);
            table.getEventManager().addListener(TablePanel.ON_SHOW, eventBridge);
            table.getEventManager().addListener(TablePanel.ON_HIDE, eventBridge);
            table.getEventManager().addListener(TablePanel.ON_INIT, eventBridge);
            table.getEventManager().addListener(TablePanel.ON_VIEW_CHANGE, eventBridge);

            registerStatefulComponent(table);

            if (table.isInit()) {
                if (table.getDataset() != null) {
                    getEventManager().fireEvent(
                            new WebEvent<TablePanel>(TablePreviewEventHub.this,
                                    TablePreviewEventHub.ON_TABLE_ADDED, table));
                }
            } else {
                table.getEventManager().addListener(TablePanel.ON_INIT,
                        new WebEventListener(){
                            public void eventNotify(WebEvent ev) {
                                if (table.getDataset() != null) {
                                    getEventManager().fireEvent(
                                            new WebEvent<TablePanel>(TablePreviewEventHub.this,
                                                    TablePreviewEventHub.ON_TABLE_ADDED, table));
                                }
                                table.getEventManager().removeListener(TablePanel.ON_INIT, this);
                            }
                        });
            }

        }
    }

    public void bind(TablePreview preview) {
        if (!previews.contains(preview)) {
            previews.add(preview);

            if (preview instanceof StatefulWidget) {
                registerStatefulComponent((StatefulWidget) preview);
            }
        }
    }

    public void bind(EventWorker evWorker) {
        if (!evWorkers.contains(evWorker)) {
            evWorkers.add(evWorker);
        }
    }

    public void bind(TabPane tPane) {
        if (!tabs.contains(tPane)) {
            tabs.add(tPane);

            tPane.getEventManager().addListener(ON_TAB_SELECTED, eventBridge);
            tPane.getEventManager().addListener(ON_TAB_ADDED, eventBridge);
        }
    }

    public void bind(TableCtx externalTable) {
        if (!externalTables.contains(externalTable)) {
            externalTables.add(externalTable);
        }
    }

    public void bind(DockLayoutPanel panel) {
        if (!layoutPanels.contains(panel)) {
            layoutPanels.add(panel);
        }
    }

    public void unbind(TablePanel table) {
        tables.remove(table);
        getEventManager().fireEvent( new WebEvent<TablePanel>(this,
                        TablePreviewEventHub.ON_TABLE_REMOVED, table));
    }

    public void unbind(EventWorker evWorker) {
        evWorkers.remove(evWorker);
    }

    public void unbind(TabPane tPane) {
        tabs.remove(tPane);
    }

    public void unbind(TableCtx externalTable) {
        externalTables.remove(externalTable);
    }

    public void unbind(TablePreview preview) {
        previews.remove(preview);

    }

    public TablePanel getActiveTable() {
        return curActiveTable;
    }

    public List<DockLayoutPanel> getLayoutPanels() {
        return layoutPanels;
    }

    public List<TablePanel> getTables() {
        return tables;
    }

    public CatalogDisplay getCatalogDisplay() {
        return catalogDisplay;
    }

    public DataConnectionDisplay getDataConnectionDisplay() {
        return dataDisplay;
    }

    public List<TablePreview> getPreviews() {
        return previews;
    }

    public List<EventWorker> getEventWorkers() {
        return evWorkers;
    }

    public List<TableCtx> getExternalTables() {
        return externalTables;
    }


    public List<DrawingLayerProvider> getDrawingProviders() {
        List<DrawingLayerProvider> list= new ArrayList<DrawingLayerProvider>(evWorkers.size());
        for(EventWorker ew : evWorkers) {
            if (ew instanceof DrawingLayerProvider) list.add((DrawingLayerProvider)ew);
        }
        return list;
    }


    public void setPreviewEnabled(TablePreview preview, boolean isEnable) {
        if (previews.contains(preview)) {
            Name name = (isEnable ? ENABLE_PREVIEW : DISABLE_PREVIEW);
            getEventManager().fireEvent(new WebEvent(preview, name));
        }
    }

//====================================================================
//  implements StatefulWidget
//====================================================================

    public String getStateId() {
        return "EventHub";
    }

    public void setStateId(String iod) {}

    public void recordCurrentState(Request request) {
        for (StatefulWidget sw : statefuls) {
            if (sw.isActive()) {
                sw.recordCurrentState(request);
            }
        }
    }

    public void moveToRequestState(Request request, AsyncCallback callback) {
        for (StatefulWidget sw : statefuls) {
            if (sw.isActive()) {
                AsyncCallback acb = new AsyncCallback() {
                            public void onFailure(Throwable caught) {}
                            public void onSuccess(Object result) {}
                        };

                sw.moveToRequestState(request, acb);
            }
        }
        callback.onSuccess(null);
    }

    public boolean isActive() {
        return true;
    }

//====================================================================
//
//====================================================================

    class EventBridge implements WebEventListener {
        public void eventNotify(WebEvent ev) {

//            if (ev.getSource() instanceof TablePanel) {
//                curActiveTable = (TablePanel) ev.getSource();
//            }
//
            if (ev.getName().equals(TablePanel.ON_SHOW)) {
                curActiveTable = (TablePanel) ev.getSource();
                if (curActiveTable.isInit()) {
                    getEventManager().fireEvent(new WebEvent(ev.getSource(), ON_TABLE_SHOW, ev.getData()));
                }
            } else if (ev.getName().equals(TablePanel.ON_HIDE)) {
                if (curActiveTable != null && ev.getSource().equals(curActiveTable)) {
                    curActiveTable = null;
                }
                 getEventManager().fireEvent(new WebEvent(ev.getSource(), ON_TABLE_HIDE, ev.getData()));
            } else if (ev.getName().equals(TablePanel.ON_INIT)) {
                getEventManager().fireEvent(new WebEvent(ev.getSource(), ON_TABLE_INIT, ev.getData()));
            } else if (ev.getName().equals(TabPane.TAB_SELECTED)) {
                getEventManager().fireEvent(new WebEvent(ev.getSource(), ON_TAB_SELECTED, ev.getData()));
            } else if (ev.getName().equals(TabPane.TAB_ADDED)) {
                getEventManager().fireEvent(new WebEvent(ev.getSource(), ON_TAB_ADDED, ev.getData()));
            } else {
                getEventManager().fireEvent(ev);
            }
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
