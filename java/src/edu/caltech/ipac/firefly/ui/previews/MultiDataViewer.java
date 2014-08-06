package edu.caltech.ipac.firefly.ui.previews;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter;
import edu.caltech.ipac.firefly.fuse.data.ImagePlotDefinition;
import edu.caltech.ipac.firefly.fuse.data.TwoMassDataSetInfoConverter;
import edu.caltech.ipac.firefly.fuse.data.config.SelectedRowData;
import edu.caltech.ipac.firefly.fuse.data.provider.WiseDataSetInfoConverter;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.AbstractTablePreview;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.Band;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ui.DataVisGrid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Date: Feb 20, 2009
 *
 * @author Trey
 * @version $Id: DataViewerPreview.java,v 1.41 2012/10/11 22:23:56 tatianag Exp $
 */
public class MultiDataViewer extends AbstractTablePreview {

    public static final String NOT_AVAILABLE_MESS=  "FITS image or spectrum is not available";
    public static final String NO_ACCESS_MESS=  "You do not have access to this data";
    public static final String NO_PREVIEW_MESS=  "No Preview";



    private GridCard activeGridCard= null;
    private boolean catalog= false;
    private Map<Band,WebPlotRequest> currentReqMap = null;
    private boolean init = false;
    private boolean showing = true;
    private TablePanel currTable;
    private Map<TablePanel, GridCard> viewDataMap= new HashMap<TablePanel, GridCard>(11);
    private DeckLayoutPanel plotDeck = new DeckLayoutPanel();
    private DockLayoutPanel mainPanel= new DockLayoutPanel(Style.Unit.PX);
    private FlowPanel toolbar= new FlowPanel();
    private final Widget threeColor;
    private boolean threeColorShowing= false;


    public MultiDataViewer() {
        super("Fits, title placeholder", "Fits Image, this is a tip placeholder");
        setDisplay(mainPanel);

        mainPanel.addNorth(toolbar, 40);
        mainPanel.add(plotDeck);

        threeColor= GwtUtil.makeLinkButton("3 Color", "Add 3 Color view", new ClickHandler() {
            public void onClick(ClickEvent event) {
                add3Color();
            }
        });
        toolbar.add(threeColor);
        GwtUtil.setHidden(threeColor, true);
    }

    @Override
    public void bind(EventHub hub) {
        super.bind(hub);
        
        WebEventListener wel =  new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                if (ev.getSource() instanceof TablePanel) {
                    TablePanel table = (TablePanel) ev.getSource();
                    currTable= table;
                    if (updateTabVisible(table))  updateGrid(table);
                }
            }
        };
        hub.getEventManager().addListener(EventHub.ON_ROWHIGHLIGHT_CHANGE, wel);
        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, wel);
    }


    public boolean isInitiallyVisible() { return false; }

    // todo: this method to complex:
    // todo: what to do when a catalog table is active?
    // todo: should the preview ever be hidden?
    private boolean updateTabVisible(TablePanel table) {

        return true;
//        boolean show= false;
//        if (table!=null && table.getDataset()!=null) {
//            catalog =  table.getDataset().getMeta().contains(MetaConst.CATALOG_OVERLAY_TYPE);
//
//            DatasetInfoConverter info= getInfo(table);
//            boolean results= (info!=null) && (info.isSupport(FITS) || info.isSupport(SPECTRUM));
//            if (results) currTable = table;
//
//            show= (catalog || results || init);
//            if (catalog && !init) show= false;
//
//            getEventHub().setPreviewEnabled(this,show);
//        }
//        return show;
    }



    private void add3Color() {
        if (currTable!=null) {
            GridCard gridCard= viewDataMap.get(currTable);
            DatasetInfoConverter info= getInfo(currTable);
            if (gridCard!=null && info!=null) {
                ImagePlotDefinition def= info.getImagePlotDefinition(null);
                threeColorShowing= true;
                for(String id : def.get3ColorViewerIDs()) {{
                    gridCard.getVisGrid().addWebPlotImage(id,null);
                }}


                updateGrid(currTable);

            }
        }




    }



    //=============================== Entry Points
    //=============================== Entry Points
    //=============================== Entry Points


    //TODO: how do we deal with the no data message
    //TODO: how do we deal with the plot fail message
    //TODO: how do we deal with the  no access message

    private void updateGrid(TablePanel table) {
        DatasetInfoConverter info= getInfo(table);
        SelectedRowData rowData= makeRowData(table);
        if (info==null || rowData==null) return;

        GridCard gridCard= viewDataMap.get(table);
        if (activeGridCard!=null) {
            if (gridCard!=activeGridCard) {
                activeGridCard.getVisGrid().setActive(false);
            }
        }
        if (gridCard==null) {
            ImagePlotDefinition def= info.getImagePlotDefinition(table.getDataset().getMeta());
            DataVisGrid visGrid= new DataVisGrid(def.getViewerIDs(),0,def.getViewerToDrawingLayerMap());
            gridCard= new GridCard(visGrid,table);
            plotDeck.add(visGrid.getWidget());
            viewDataMap.put(table,gridCard);
        }

        if (info.isSupport(DatasetInfoConverter.DataVisualizeMode.FITS_3_COLOR ) &&  info.is3ColorOptional()) {
            GwtUtil.setHidden(threeColor, false);
        }
        else {
            GwtUtil.setHidden(threeColor, true);
        }

        gridCard.getVisGrid().setActive(true);
        plotDeck.showWidget(gridCard.getVisGrid().getWidget());

        activeGridCard= gridCard;
        info.getImageRequest(rowData, DatasetInfoConverter.GroupMode.WHOLE_GROUP, new AsyncCallback<Map<String, WebPlotRequest>>() {
            public void onFailure(Throwable caught) { }

            public void onSuccess(Map<String, WebPlotRequest> reqMap) {
                updateGridStep2(reqMap);
            }
        });

        if (threeColorShowing) {
            info.getThreeColorPlotRequest(rowData, null, new AsyncCallback<Map<String, List<WebPlotRequest>>>() {
                public void onFailure(Throwable caught) {
                }

                public void onSuccess(Map<String, List<WebPlotRequest>> reqMap) {
                    updateGridThreeStep2(reqMap);
                }
            });

        }
    }


    private void updateGridStep2(final Map<String, WebPlotRequest> reqMap) {
        final DataVisGrid grid= activeGridCard.getVisGrid();
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                grid.getWidget().onResize();
                grid.load(reqMap,new AsyncCallback<String>() {
                    public void onFailure(Throwable caught) { }

                    public void onSuccess(String result) {
                        //todo???
                    }
                });
            }
        });
    }

    private void updateGridThreeStep2(final Map<String, List<WebPlotRequest>> reqMap) {
        final DataVisGrid grid= activeGridCard.getVisGrid();
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                grid.getWidget().onResize();
                grid.load3Color(reqMap,new AsyncCallback<String>() {
                    public void onFailure(Throwable caught) { }

                    public void onSuccess(String result) {
                        //todo???
                    }
                });
            }
        });
    }


    @Override
    public void onShow() {
        showing = true;
        updateGrid(currTable);
        super.onShow();
    }

    @Override
    public void onHide() {
        showing = false;
        if (currTable!=null)  {
            GridCard gridCard= viewDataMap.get(currTable);
            if (gridCard!=null) gridCard.getVisGrid().setActive(false);
        }
        super.onHide();
    }

    protected void updateDisplay(TablePanel table) {
        if (table!=null) {
            currTable= table;
            updateGrid(table);
        }
    }

    //======================================================================
    //=============================== End ENTRY Points
    //======================================================================

    private WiseDataSetInfoConverter wConv= null;
    private TwoMassDataSetInfoConverter twoconv= null;

    private DatasetInfoConverter getInfo(TablePanel table) {
        DatasetInfoConverter c;
        if (table.getDataset().getMeta().contains(MetaConst.CATALOG_OVERLAY_TYPE)) {
            c=null;
        }
        else if (table.getDataset().getMeta().contains(MetaConst.DATA_PRIMARY)) {
            c=null;
        }
        else if (table.getDataset().getMeta().contains("ProductLevel")) {
            if (wConv==null) wConv= new WiseDataSetInfoConverter();
            c= wConv;
        }
        else {
            if (twoconv==null) twoconv= new TwoMassDataSetInfoConverter();
            c= twoconv;
        }
        return c;
    }

    private SelectedRowData makeRowData(TablePanel table) {
        TableData.Row<String> row= table.getTable().getHighlightedRow();
        return row!=null ? new SelectedRowData(row,table.getDataset().getMeta()) : null;
    }


    private static class GridCard {
        private long accessTime;
        private TablePanel table;
//        final int cardIdx;
        final private DataVisGrid visGrid;

        public GridCard(DataVisGrid visGrid, TablePanel table) {
//            this.cardIdx = cardIdx;
            this.visGrid = visGrid;
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

//        public int getCardIdx() {
//            updateAccess();
//            return cardIdx;
//        }

        public DataVisGrid getVisGrid() {
            updateAccess();
            return visGrid;
        }

        public void updateAccess() { accessTime= System.currentTimeMillis();}

        public long getAccessTime() { return accessTime; }

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
