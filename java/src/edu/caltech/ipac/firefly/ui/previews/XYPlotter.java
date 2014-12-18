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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

/**
 * @author Trey Roby
 */
public class XYPlotter {

    private static final int MAX_CARDS= 4;

    private final DeckLayoutPanel panel= new DeckLayoutPanel();
    private final List<XYCard> cardList= new ArrayList<XYCard>(MAX_CARDS);
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
        if (!tableMeta.contains(MetaConst.CATALOG_OVERLAY_TYPE)) return;



        XYCard card= getCard(table);
        final XYPlotWidget xyPlotWidget;

        if (card==null) {
            if (cardList.size()<MAX_CARDS) {
                XYPlotMeta meta = new XYPlotMeta("none", 800, 200, new CustomMetaSource(new HashMap<String, String>()));
                meta.setAspectRatio(panel.getOffsetWidth()/panel.getOffsetHeight());
                meta.setStretchToFill(true);
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

        if (card.getCardIdx()!=currentShowingCard) {
            panel.showWidget(card.getCardIdx());
            currentShowingCard= card.getCardIdx();
            if (card.isDataChange()) {
                if (table.getDataModel() != null && table.getDataModel().getTotalRows()>0) {
                    xyPlotWidget.setVisible(true);
                    xyPlotWidget.makeNewChart(table.getDataModel(), "XY Plot");
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
        }

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
            updateAccess();
        }

        private TablePanel getTable() {
            updateAccess();
            return table;
        }

        private void setTable(TablePanel table) {
            updateAccess();
            this.table = table;
        }


        public int getCardIdx() {
            updateAccess();
            return cardIdx;
        }

        public XYPlotWidget getXyPlotWidget() {
            updateAccess();
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

