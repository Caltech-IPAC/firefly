/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.ui.previews;
/**
 * User: roby
 * Date: 10/21/13
 * Time: 12:44 PM
 */


import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.graph.CustomMetaSource;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotMeta;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;
import edu.caltech.ipac.util.ComparisonUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * @author Trey Roby
 */
public class XYPlotter {

//    private static final int MAX_CARDS= 2;
    private static final int MAX_ROWS= 10000;

    private final SimpleLayoutPanel panel= new SimpleLayoutPanel();
//    private final SimplePanel panel= new SimplePanel();
    private final List<XYCard> cardList= new ArrayList<XYCard>(10);
    private HashMap<TablePanel, XYPlotMeta> metas = new HashMap<TablePanel, XYPlotMeta>(10);
    private XYCard activeCard= null;
    private final EventHub hub;

    public XYPlotter(EventHub hub) {
        this.hub= hub;

        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                updateXyPlot();
            }
        });
        hub.getEventManager().addListener(EventHub.ON_DATA_LOAD, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                updateXyPlot();
            }
        });
        hub.getEventManager().addListener(EventHub.ON_TABLE_REMOVED, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                removeActiveTableCard((TablePanel)ev.getData());
            }
        });
    }

    public Widget getWidget() {
        return panel;
    }

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void removeActiveTableCard(TablePanel table) {
        if (table!=null) {
            // remove XYPlotMeta for this table
            metas.remove(table);
            // remove card
            XYCard card= getCard(table);
            if (card!=null) {
                card.getXyPlotWidget().setVisible(false);
                AllPlots.getInstance().deregisterPopout(card.getXyPlotWidget());
                cardList.remove(card);
                if (card==activeCard) activeCard= null;
                panel.clear();
            }
        }
    }

    public boolean getHasPlots() { return activeCard!=null; }

    private void updateXyPlot() {

        final TablePanel table = hub.getActiveTable();

        boolean v= table!=null && !table.getDataModel().isMaxRowsExceeded();
        panel.setVisible(v);
        if (!v)  return;

        TableMeta tableMeta= table.getDataset().getMeta();
        // can handle only the catalogs at the moment
        if (!tableMeta.contains(MetaConst.CATALOG_OVERLAY_TYPE)) return;

        XYCard card= getCard(table);
        final XYPlotWidget xyPlotWidget;
        XYPlotMeta restoredMeta = metas.get(table);

        if (activeCard!=null) {
            XYPlotWidget oldXYPlotWidget = activeCard.getXyPlotWidget();
            AllPlots.getInstance().deregisterPopout(oldXYPlotWidget);
        }


        if (card==null) {
            if (!isUnderTotalRows(table)) {
                List<XYCard>delCards= getOldestCardsToDelete(table);
                for(XYCard c : delCards) {
                    panel.remove(c.getXyPlotWidget());
                    AllPlots.getInstance().deregisterPopout(c.getXyPlotWidget());
                    cardList.remove(c);
                }
            }
            XYPlotMeta meta = getXYPlotMeta(table);
            xyPlotWidget = new XYPlotWidget(meta);
            xyPlotWidget.setTitleAreaAlwaysHidden(true);
            card = new XYCard(xyPlotWidget,table);
            cardList.add(card);
        }
        else {
            xyPlotWidget= card.getXyPlotWidget();
        }

        AllPlots.getInstance().registerPopout(xyPlotWidget);



        if (card!=activeCard) {
            panel.clear();
            panel.setWidget(xyPlotWidget);
            activeCard= card;
            if (card.isDataChange()) {
                if (table.getDataModel() != null && getRows(table)>0) {
                    xyPlotWidget.setVisible(true);
                    if (restoredMeta != null) {
                        //plotMeta need to be restored
                        xyPlotWidget.makeNewChart(restoredMeta, table.getDataModel(), "XY Plot");
                    } else {
                        xyPlotWidget.makeNewChart(table.getDataModel(), "XY Plot");
                    }
                } else {
                    xyPlotWidget.setVisible(false);
                    AllPlots.getInstance().deregisterPopout(xyPlotWidget);
                }
            }
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                public void execute() {
                    xyPlotWidget.onResize();
                }
            });
            card.updateDataCtx();
            card.updateAccess();
        }
    }

    private XYPlotMeta getXYPlotMeta(TablePanel table) {
        TableMeta tableMeta= table.getDataset().getMeta();
        TableMeta.LonLatColumns llc = tableMeta.getLonLatColumnAttr(MetaConst.CATALOG_COORD_COLS);

        HashMap<String, String> params = new HashMap<String, String>();
        if (llc != null) {
            params.put(CustomMetaSource.XCOL_KEY, llc.getLonCol());
            params.put(CustomMetaSource.YCOL_KEY, llc.getLatCol());
        }
        String plotTitle = tableMeta.getAttribute(MetaConst.CATALOG_OVERLAY_TYPE);
        if (plotTitle == null) plotTitle = "none";
        XYPlotMeta meta = new XYPlotMeta(plotTitle, 800, 200, new CustomMetaSource(params));
        meta.setAspectRatio(panel.getOffsetWidth()/panel.getOffsetHeight());
        meta.setStretchToFill(true);
        metas.put(table, meta);
        return meta;
    }

    private XYCard getCard(TablePanel table) {
        XYCard retval= null;
        for(XYCard card : cardList) {
            if (card.getTable()==table) {
                retval= card;
                break;
            }
        }
        return retval;
    }

    private boolean isUnderTotalRows(TablePanel newTable) {
        int total= getRows(newTable);
        TablePanel t;
        for(XYCard c : cardList) {
            total+= getRows(c.getTable());
        }
        return total<MAX_ROWS;
    }

    private List<XYCard> getOldestCardsToDelete(TablePanel newTableToFit) {

        // 1. if less then 2 entries then nothing to delete
        if (cardList.size()<3) return Collections.emptyList();

        // 2. if total with new table less than max then nothing to delete
        int totalRows= getRows(newTableToFit);
        for(XYCard c : cardList) {
            totalRows+= getRows(c.getTable());
        }
        if (totalRows<MAX_ROWS) return Collections.emptyList();


        // sort to know which tables are the first to go
        Collections.sort(cardList, new Comparator<XYCard>() {
            public int compare(XYCard c1, XYCard c2) {
                return ComparisonUtil.doCompare(c2.getAccessTime(), c1.getAccessTime());
            }
        });

        // 3. it the new table is bigger than max rows then return array with all but the youngest entry
        List<XYCard> retList;
        if (getRows(newTableToFit)>MAX_ROWS) {
            retList= new ArrayList<XYCard>(cardList);
            retList.remove(retList.size()-1);
        }
        // 4. return a list of the oldest that will max us under the max
        else {
            retList= new ArrayList<XYCard>(cardList.size());
            for(XYCard c : cardList) {
                totalRows-= getRows(c.getTable());
                retList.add(c);
                if (totalRows< MAX_ROWS) {
                    break;
                }
            }
        }

        return retList;
    }


    private static int getRows(TablePanel t) {
        int r= t.getDataModel().getTotalRows();
        return r>30000 ? 30000 : r;
    }


    private XYCard getOldestCard() {
        if (cardList.size()==0) return null;
        if (cardList.size()==1) return cardList.get(0);

        Collections.sort(cardList, new Comparator<XYCard>() {
            public int compare(XYCard c1, XYCard c2) {
                return ComparisonUtil.doCompare(c1.getAccessTime(), c2.getAccessTime());
            }
        });
        return cardList.get(0);
    }

//======================================================================
//------------------ Inner Classes  ------------------------------------
//======================================================================

    private static class XYCard {
        private long accessTime;
        private TablePanel table;
        final private XYPlotWidget xyPlotWidget;
        private ServerRequest req= null;

        public XYCard(XYPlotWidget xyPlotWidget, TablePanel table) {
            this.xyPlotWidget = xyPlotWidget;
            this.table = table;
        }

        private TablePanel getTable() {
            return table;
        }

        private void setTable(TablePanel table) {
            this.table = table;
            req = null;
        }



        public XYPlotWidget getXyPlotWidget() {
            return xyPlotWidget;
        }

        public boolean isDataChange() {
            ServerRequest testReq= table.getDataModel().getRequest();
            return !testReq.equals(req);
        }

        private void updateAccess() { accessTime= System.currentTimeMillis();}

        public void updateDataCtx() {
            req= table.getDataModel().getRequest();
        }

        private long getAccessTime() { return accessTime; }

    }

}

