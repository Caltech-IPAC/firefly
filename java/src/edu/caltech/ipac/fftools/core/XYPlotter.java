package edu.caltech.ipac.fftools.core;
/**
 * User: roby
 * Date: 10/21/13
 * Time: 12:44 PM
 */


import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.fftools.FFToolEnv;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
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

    public XYPlotter() {

        FFToolEnv.getHub().getEventManager().addListener(EventHub.ON_TABLE_SHOW, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                updateXyPlot();
            }
        });
        FFToolEnv.getHub().getEventManager().addListener(EventHub.ON_DATA_LOAD, new WebEventListener() {
            public void eventNotify(WebEvent ev) {
                updateXyPlot();
            }
        });
        FFToolEnv.getHub().getEventManager().addListener(EventHub.ON_TABLE_REMOVED, new WebEventListener() {
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
                panel.remove(card.getXyPlotWidget());
                cardList.remove(card);
                currentShowingCard = -1;
            }
        }
    }

//    private void updateXYPlotShowing() {
//        final TablePanel table = FFToolEnv.getHub().getActiveTable();
//        panel.setVisible(table!=null && !table.getDataModel().isMaxRowsExceeded());
//    }


    private void updateXyPlot() {

        final TablePanel table = FFToolEnv.getHub().getActiveTable();

        boolean v= table!=null && !table.getDataModel().isMaxRowsExceeded();
        panel.setVisible(v);
        if (!v)  return;


        XYCard card= getCard(table);
        final XYPlotWidget xyPlotWidget;

        if (card==null) {
            if (cardList.size()<MAX_CARDS) {
                XYPlotMeta meta = new XYPlotMeta("none", 0, 0, new CustomMetaSource(new HashMap<String, String>()));
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

        if (card.getCardIdx()!=currentShowingCard) {
            panel.showWidget(card.getCardIdx());
            currentShowingCard= card.getCardIdx();
            if (card.isDataChange()) {
                if (table.getDataModel() != null && table.getDataModel().getTotalRows()>0) {
                    xyPlotWidget.setVisible(true);
                    xyPlotWidget.makeNewChart(table.getDataModel(), "XY Plot");
                } else {
                    xyPlotWidget.setVisible(false);
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

/*
 * THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA 
 * INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH 
 * THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE 
 * IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS 
 * AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND, 
 * INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR 
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
