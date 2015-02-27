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
import com.google.gwt.user.client.ui.DeckLayoutPanel;
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

import java.util.*;

/**
 * @author Trey Roby
 */
public class XYPlotter {

    private static final int MAX_CARDS= 2;

    private final DeckLayoutPanel panel= new DeckLayoutPanel();
    private final List<XYCard> cardList= new ArrayList<XYCard>(MAX_CARDS);
    private HashMap<TablePanel, XYPlotMeta> metas = new HashMap<TablePanel, XYPlotMeta>(10);
    private int currentShowingCard= -1;
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

    public DeckLayoutPanel getWidget() { return panel;
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
                if (card.getCardIdx()==currentShowingCard) currentShowingCard= -1;
            }
        }
    }

    public boolean getHasPlots() {
        return currentShowingCard>-1;
    }

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

        if (card==null) {
            if (cardList.size()<MAX_CARDS) {
                XYPlotMeta meta = getXYPlotMeta(table);
                xyPlotWidget = new XYPlotWidget(meta);
                xyPlotWidget.setTitleAreaAlwaysHidden(true);
                panel.add(xyPlotWidget);
                int cardNum= panel.getWidgetIndex(xyPlotWidget);
                card = new XYCard(cardNum,xyPlotWidget,table);
                cardList.add(card);
            }
            else {
                card= getOldestCard();
                card.setTable(table);
                xyPlotWidget= card.getXyPlotWidget();
                if (restoredMeta == null) {
                    restoredMeta = getXYPlotMeta(table);
                }
            }
        }
        else {
            xyPlotWidget= card.getXyPlotWidget();
        }

        if (currentShowingCard>=0) {
            XYPlotWidget oldXYPlotWidget = cardList.get(currentShowingCard).getXyPlotWidget();
            AllPlots.getInstance().deregisterPopout(oldXYPlotWidget);
        }
        AllPlots.getInstance().registerPopout(xyPlotWidget);

        // can we assume that oldest card is never current showing?
        if (card.getCardIdx()!=currentShowingCard) {
            panel.showWidget(card.getCardIdx());
            currentShowingCard= card.getCardIdx();
            if (card.isDataChange()) {
                if (table.getDataModel() != null && table.getDataModel().getTotalRows()>0) {
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
            else {
                Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                    public void execute() {
                        xyPlotWidget.onResize();
                    }
                });
            }
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
        final int cardIdx;
        final private XYPlotWidget xyPlotWidget;
        private ServerRequest req= null;

        public XYCard(int cardIdx, XYPlotWidget xyPlotWidget, TablePanel table) {
            this.cardIdx = cardIdx;
            this.xyPlotWidget = xyPlotWidget;
            this.table = table;
        }

        private TablePanel getTable() {
            return table;
        }

        private void setTable(TablePanel table) {
            this.table = table;
        }


        public int getCardIdx() {
            return cardIdx;
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

