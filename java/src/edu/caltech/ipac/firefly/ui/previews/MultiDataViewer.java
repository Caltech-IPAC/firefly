package edu.caltech.ipac.firefly.ui.previews;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.data.ServerRequest;
import edu.caltech.ipac.firefly.data.table.MetaConst;
import edu.caltech.ipac.firefly.data.table.TableData;
import edu.caltech.ipac.firefly.data.table.TableMeta;
import edu.caltech.ipac.firefly.fuse.data.ConverterStore;
import edu.caltech.ipac.firefly.fuse.data.DatasetInfoConverter;
import edu.caltech.ipac.firefly.fuse.data.ImagePlotDefinition;
import edu.caltech.ipac.firefly.fuse.data.PlotData;
import edu.caltech.ipac.firefly.fuse.data.config.SelectedRowData;
import edu.caltech.ipac.firefly.resbundle.images.IconCreator;
import edu.caltech.ipac.firefly.ui.BadgeButton;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.ui.table.TablePanel;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.ui.DataVisGrid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Date: Feb 20, 2009
 *
 * @author Trey
 * @version $Id: DataViewerPreview.java,v 1.41 2012/10/11 22:23:56 tatianag Exp $
 */
public class MultiDataViewer {

    public static final String NOT_AVAILABLE_MESS=  "FITS image or spectrum is not available";
    public static final String NO_ACCESS_MESS=  "You do not have access to this data";
    public static final String NO_PREVIEW_MESS=  "No Preview";


    private static final IconCreator _ic= IconCreator.Creator.getInstance();

    private GridCard activeGridCard= null;
    private boolean showing = true;
    private Object currDataContainer;
    private Map<Object, GridCard> viewDataMap= new HashMap<Object, GridCard>();
    private DeckLayoutPanel plotDeck = new DeckLayoutPanel();
    private DockLayoutPanel mainPanel= new DockLayoutPanel(Style.Unit.PX);
    private FlowPanel toolbar= new FlowPanel();
    private FlowPanel toolbarInner= new FlowPanel();
    private Widget threeColor;
    private CheckBox relatedView= GwtUtil.makeCheckBox("Show Related", "Show all related images", true);
    private HTML noDataAvailableLabel= new HTML("No Image Data Requested");
    private Widget noDataAvailable= makeNoDataAvailable();
    private UpdateListener updateListener= new UpdateListener();
    private BadgeButton popoutButton;
    private boolean expanded= false;
    private RefreshListener refreshListener= null;
    private DataVisGrid.MpwFactory mpwFactory= null;
    private EventHub hub= null;


    public MultiDataViewer() {
        plotDeck.add(noDataAvailable);
        plotDeck.setWidget(noDataAvailable);
        mainPanel.addNorth(toolbar, 30);
        mainPanel.add(plotDeck);
        buildToolbar();
        reinitConverterListeners();
    }

    public void setRefreshListener(RefreshListener l) { this.refreshListener= l; }
    public void setMpwFactory(DataVisGrid.MpwFactory mpwFactory) { this.mpwFactory = mpwFactory; }
    public Widget getWidget() { return mainPanel; }
    public void setNoDataMessage(String m) { noDataAvailableLabel.setHTML(m); }

    public void forceExpand() {
        expanded= true;
    }

    public void cancelExpand() {
        expanded= false;
    }

    public void bind(EventHub hub) {
//        super.bind(hub);
        
        WebEventListener wel =  new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                if (ev.getSource() instanceof TablePanel) {
                    TablePanel table = (TablePanel) ev.getSource();
                    updateGridWithTable(table);
                }
            }
        };


        WebEventListener removeList=  new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                TablePanel table = (TablePanel) ev.getData();
                if (table== currDataContainer) {
                    currDataContainer = null;
                    if (refreshListener!=null) refreshListener.preDataChange();
                    removeTable(table);
                }
            }
        };

        hub.getEventManager().addListener(EventHub.ON_ROWHIGHLIGHT_CHANGE, wel);
        hub.getEventManager().addListener(EventHub.ON_TABLE_SHOW, wel);
        hub.getEventManager().addListener(EventHub.ON_TABLE_REMOVED, removeList);
        this.hub= hub;
    }


    public void addGridInfo(TableMeta meta) {
        currDataContainer= meta;
        updateGrid(meta);
    }

    public void ensureMPWSelected() {
        if (activeGridCard!=null) {
           activeGridCard.getVisGrid().ensureMPWSelected();
        }
    }


    public void updateGridWithTable(TablePanel table) {
        if (table.getDataset().getMeta().contains(MetaConst.DATASET_CONVERTER)) {
            currDataContainer = table;
            if (refreshListener!=null) refreshListener.preDataChange();
            if (updateTabVisible(table))  updateGrid(table);
        }
    }

    public void forceGridUpdate() {
        updateGrid(currDataContainer);
    }


    public void onShow() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                showing = true;
                updateGrid(currDataContainer);
            }
        });
    }

    public void onHide() {
        showing = false;
        GridCard gridCard= viewDataMap.get(currDataContainer);
        if (gridCard!=null) gridCard.getVisGrid().setActive(false);
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
//            if (results) currDataContainer = table;
//
//            show= (catalog || results || init);
//            if (catalog && !init) show= false;
//
//            getEventHub().setPreviewEnabled(this,show);
//        }
//        return show;
    }






    //=============================== Entry Points
    //=============================== Entry Points
    //=============================== Entry Points


    private void removeTable(TablePanel table) {
        GridCard gridCard= viewDataMap.get(table);
        if (gridCard!=null) {
            gridCard.getVisGrid().cleanup();
            plotDeck.remove(gridCard.getVisGrid().getWidget());
            viewDataMap.remove(table);
            if (isNoData()) {
                plotDeck.setWidget(noDataAvailable);
                GwtUtil.setHidden(toolbar, true);
            }
        }
    }


    public boolean hasContent() {
        boolean retval= false;
        if (viewDataMap.size()>0) {
            for(GridCard card : viewDataMap.values()) {
                retval= card.getVisGrid().hasImagesPlotted();
                if (retval) break;
            }
        }
        return retval;
    }



    private boolean isNoData() {
        boolean noData= false;
        if (viewDataMap.size()==0) {
            noData= true;
        }
        else if (currDataContainer!=null) {
            DatasetInfoConverter info= getInfo(currDataContainer);
            int cnt= info.getImagePlotDefinition().getImageCount();
            if (cnt==0) {
                PlotData dyn= info.getPlotData();
                noData= !dyn.hasPlotsDefined();
            }
        }
        return noData;
    }


    //TODO: how do we deal with the no data message
    //TODO: how do we deal with the plot fail message
    //TODO: how do we deal with the  no access message
    private void updateGrid(final Object dataContainer) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                updateGridStep1(dataContainer);
            }
        });
    }


    private void updateGridStep1(Object dataContainer) {
        SelectedRowData rowData= makeRowData(dataContainer);
        final DatasetInfoConverter info= getInfo(rowData);
        ImagePlotDefinition def= null;
        if (info!=null) {
            def= info.getImagePlotDefinition();
            insureGridCard(def,rowData, dataContainer,info);
        }



        if (info==null || rowData==null || (!GwtUtil.isOnDisplay(mainPanel) && !expanded) ) return;

        if (!expanded) expanded= AllPlots.getInstance().isExpanded();

        GwtUtil.setHidden(toolbar, false);
        GridCard gridCard= viewDataMap.get(dataContainer);
        if (activeGridCard!=null) {
            if (gridCard!=activeGridCard) {
                activeGridCard.getVisGrid().setActive(false);
            }
        }
//        if (gridCard==null) gridCard= makeGridCard(def,rowData, dataContainer,info);

        if (expanded) gridCard.getVisGrid().makeNextPlotExpanded();
        expanded= false;

        final PlotData plotData= info.getPlotData();
        if (info.isSupport(DatasetInfoConverter.DataVisualizeMode.FITS_3_COLOR ) && plotData.is3ColorOptional()) {
            GwtUtil.setHidden(threeColor, false);
        }
        else {
            GwtUtil.setHidden(threeColor, true);
        }

        GwtUtil.setHidden(relatedView, def.getImageCount()<2);
        final DatasetInfoConverter.GroupMode mode;
        if (relatedView.getValue()) {
            gridCard.getVisGrid().clearShowMask();
            mode= DatasetInfoConverter.GroupMode.WHOLE_GROUP;
        }
        else {
            mode= DatasetInfoConverter.GroupMode.TABLE_ROW_ONLY;
        }


        gridCard.getVisGrid().setActive(true);
        plotDeck.showWidget(gridCard.getVisGrid().getWidget());

        activeGridCard= gridCard;


        info.update(rowData, new AsyncCallback<String>() {
            public void onFailure(Throwable caught) { }

            public void onSuccess(String result) {
                updateGridStandardStep2(plotData.getImageRequest(mode), info);
                if ((activeGridCard.isThreeColorShowing() && plotData.hasOptional3ColorImages()) || plotData.hasDynamic3ColorImages()) {
                    updateGridThreeStep2(plotData.get3ColorImageRequest(), info);
                }
            }
        });
    }




    private void updateGridStandardStep2(final Map<String, WebPlotRequest> reqMap, final DatasetInfoConverter info) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                DataVisGrid grid= activeGridCard.getVisGrid();
                addNewToGrid(activeGridCard,reqMap.keySet(),true);
                if (!relatedView.getValue() && reqMap.size()==1) {
                    grid.setShowMask(new ArrayList<String>(reqMap.keySet()));
                }
                grid.getWidget().onResize();
                grid.load(reqMap,info, new AsyncCallback<String>() {
                    public void onFailure(Throwable caught) { }

                    public void onSuccess(String result) {
                        if (refreshListener!=null) refreshListener.viewerRefreshed();
                        //todo???
                        GwtUtil.setHidden(popoutButton.getWidget(),  activeGridCard.getVisGrid().getImageShowCount()<2);
                        ensureMPWSelected();
                    }
                });
            }
        });
    }



    private void updateGridThreeStep2(final Map<String, List<WebPlotRequest>> reqMap, final DatasetInfoConverter info) {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            public void execute() {
                DataVisGrid grid= activeGridCard.getVisGrid();
                addNewToGrid(activeGridCard,reqMap.keySet(),true);
                grid.getWidget().onResize();
                grid.load3Color(reqMap,new AsyncCallback<String>() {
                    public void onFailure(Throwable caught) { }

                    public void onSuccess(String result) {
                        if (refreshListener!=null) refreshListener.viewerRefreshed();
                        GwtUtil.setHidden(popoutButton.getWidget(),  activeGridCard.getVisGrid().getImageShowCount()<2);
                        //todo???
                    }
                });
            }
        });
    }

    public void addNewToGrid(GridCard gridCard, Set<String> keys, boolean addToGroup) {
        DataVisGrid grid= gridCard.getVisGrid();
        boolean addedKey= false;
        for(String key : keys) {
            if (!grid.containsKey(key) && !gridCard.containsDeletedID(key)) {
                grid.addWebPlotImage(key,null,addToGroup,true,false);
                addedKey= true;
            }
        }
        if (addedKey) grid.reinitGrid();
    }



    private void add3Color() {
        if (currDataContainer !=null) {
            GridCard gridCard= viewDataMap.get(currDataContainer);
            DatasetInfoConverter info= getInfo(currDataContainer);
            if (gridCard!=null && info!=null) {
                ImagePlotDefinition def= info.getImagePlotDefinition();
                gridCard.setThreeColorShowing(true);
                gridCard.clearDeletedIDs();
                for(String id : def.get3ColorViewerIDs()) {{
                    gridCard.getVisGrid().addWebPlotImage(id,null,true,true,true);
                }}
                updateGrid(currDataContainer);
            }
        }
    }



    //======================================================================
    //=============================== End ENTRY Points
    //======================================================================


    private void insureGridCard(ImagePlotDefinition def,
                                  SelectedRowData rowData,
                                  Object dataContainer,
                                  DatasetInfoConverter info) {
        GridCard gridCard= viewDataMap.get(dataContainer);
        if (gridCard==null) makeGridCard(def,rowData,dataContainer,info);
    }

    private GridCard makeGridCard(ImagePlotDefinition def,
                                  SelectedRowData rowData,
                                  Object dataContainer,
                                  DatasetInfoConverter info) {
        DataVisGrid visGrid= new DataVisGrid(hub,def.getViewerIDs(rowData),0,
                                             def.getViewerToDrawingLayerMap(), def.getGridLayout());
        visGrid.setDatasetInfoConverter(info);
        if (mpwFactory!=null) visGrid.setMpwFactory(mpwFactory);
        visGrid.setDeleteListener(new DataVisGrid.DeleteListener() {
            public void mpwDeleted(String id) { handleDelete(id); }
        });
        GridCard gridCard= new GridCard(visGrid,rowData.getTableMeta(), dataContainer, info);
        plotDeck.add(visGrid.getWidget());
        viewDataMap.put(dataContainer,gridCard);
        if (info.getImagePlotDefinition().getImageCount()==0) { // if only user loaded images
            visGrid.setLockRelated(false);
        }
        return gridCard;
    }


    private Widget makeNoDataAvailable() {
        FlowPanel fp= new FlowPanel();

        fp.add(noDataAvailableLabel);
        GwtUtil.setStyles(noDataAvailableLabel, "display", "table",
                                 "margin", "30px auto 0",
                                 "fontSize", "12pt");
        return fp;

    }

    private void handleDelete(String id) {
        DatasetInfoConverter info= getInfo(currDataContainer);
        info.getPlotData().deleteID(id);
        if (activeGridCard!=null) {
            activeGridCard.addDeletedID(id);
        }
        if (refreshListener!=null)  refreshListener.imageDeleted();
        if (isNoData()) {
            plotDeck.setWidget(noDataAvailable);
            GwtUtil.setHidden(toolbar, true);
        }
    }


    private void buildToolbar() {
        threeColor= GwtUtil.makeLinkButton("Add 3 Color", "Add 3 Color view", new ClickHandler() {
            public void onClick(ClickEvent event) {
                add3Color();
            }
        });

        popoutButton= GwtUtil.makeBadgeButton(new Image(_ic.getExpandToGridIcon()),
                                                          "Expand this panel to take up a larger area",
                                                          true, new ClickHandler() {
            public void onClick(ClickEvent event) {
                AllPlots.getInstance().forceExpand();
                MiniPlotWidget mpw= AllPlots.getInstance().getMiniPlotWidget();
                if (mpw!=null) {
                    mpw.forceSwitchToGrid();
                }
            }
        });

        relatedView.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                if (currDataContainer != null) {
                    updateGrid(currDataContainer);
                }
            }
        });

        GwtUtil.setStyle(toolbar, "backgroundColor", "rgb(200,200,200)");



        addToolbarWidget(threeColor);
        addToolbarWidget(relatedView);

        toolbar.add(toolbarInner);
        toolbar.add(popoutButton.getWidget());
        GwtUtil.setStyle(toolbarInner, "display", "inline-block");
        GwtUtil.setStyles(popoutButton.getWidget(), "display", "inline-block",
                          "cssFloat", "right",
                          "padding", "0 5px 0 0 ");
        GwtUtil.setHidden(threeColor, true);
        GwtUtil.setHidden(relatedView, true);
        GwtUtil.setHidden(toolbar, true);
    }

    public void addToolbarWidget(Widget w) {
        toolbarInner.add(w);
        GwtUtil.setStyles(w, "display", "inline-block",
                             "padding", "3px 2px 0 7px");
    }

    public void addToolbarWidgetAtBeginning(Widget w) {
        toolbarInner.insert(w,0);
        GwtUtil.setStyle(w, "display", "inline-block");
    }

    private void reinitConverterListeners() {
        for(DatasetInfoConverter c : ConverterStore.getConverters()) {
            PlotData dd= c.getPlotData();
            dd.removeListener(updateListener);
            dd.addListener(updateListener);
        }
    }




    //======================================================================================================
    //----------------- Converters, should be temporary code
    //======================================================================================================



    private DatasetInfoConverter getInfo(Object dataContainer) {
        DatasetInfoConverter retval= null;
        if (dataContainer!=null) {
            if      (dataContainer instanceof SelectedRowData) retval= getInfo((SelectedRowData)dataContainer);
            else if (dataContainer instanceof TableMeta)       retval= getInfo((TableMeta)dataContainer);
            else                                               retval= getInfo(makeRowData(dataContainer));
        }
        return retval;
    }


    private DatasetInfoConverter getInfo(SelectedRowData rowData) {
        return rowData!=null ? getInfo(rowData.getTableMeta()) : null;
    }

    private DatasetInfoConverter getInfo(TableMeta meta) {
        if (meta==null) return null;
        return ConverterStore.get(meta.getAttribute(MetaConst.DATASET_CONVERTER));
    }




    private SelectedRowData makeRowData(Object obj) {
        SelectedRowData retval= null;
        if (obj!=null) {
            if   (obj instanceof TablePanel) retval= makeRowData((TablePanel)obj);
            else if (obj instanceof TableMeta) retval= makeRowData((TableMeta)obj);
        }
        return retval;
    }

    private SelectedRowData makeRowData(TableMeta meta) {
        return new SelectedRowData(null,meta,null);
    }

    private SelectedRowData makeRowData(TablePanel table) {
        TableData.Row<String> row= table.getTable().getHighlightedRow();
        ServerRequest request= table.getDataModel().getRequest();
        return row!=null ? new SelectedRowData(row,table.getDataset().getMeta(),request) : null;
    }

    private Object findDataContainer(PlotData dpd) {
        Object retval= null;
        for(GridCard card : viewDataMap.values()) {
            if (dpd==card.getInfo().getPlotData()) {
                retval= card.getDataContainer();
                break;
            }
        }
        return retval;
    }

    private static class GridCard {
        private long accessTime;
        private TableMeta tableMeta;
        private final DataVisGrid visGrid;
        private final Object dataContainer;
        private final DatasetInfoConverter info;
        private final List<String> deleteIDList= new ArrayList<String>(10);
        private boolean threeColorShowing= false;

        public GridCard(DataVisGrid visGrid, TableMeta tableMeta, Object dataContainer, DatasetInfoConverter info) {
            this.visGrid = visGrid;
            this.tableMeta = tableMeta;
            this.dataContainer= dataContainer;
            this.info= info;
            updateAccess();
        }

        private TableMeta getTableMeta() {
            updateAccess();
            return tableMeta;
        }

        private void setTable(TableMeta tableMeta) {
            updateAccess();
            this.tableMeta = tableMeta;
        }

        public DataVisGrid getVisGrid() {
            updateAccess();
            return visGrid;
        }

        public void updateAccess() { accessTime= System.currentTimeMillis();}

        public long getAccessTime() { return accessTime; }

        public Object getDataContainer() {
            updateAccess();
            return dataContainer;
        }

        public DatasetInfoConverter getInfo() {
            return info;
        }
        public void addDeletedID(String id) {
            if (!deleteIDList.contains(id)) {
                deleteIDList.add(id);
            }

        }
        public void clearDeletedIDs() { deleteIDList.clear(); }
        public boolean containsDeletedID(String id) { return deleteIDList.contains(id); }

        private boolean isThreeColorShowing() { return threeColorShowing; }

        private void setThreeColorShowing(boolean threeColorShowing) {
            this.threeColorShowing = threeColorShowing;
        }
    }

    public static interface RefreshListener {
        public void preDataChange();
        public void viewerRefreshed();
        public void imageDeleted();
    }


    private class UpdateListener implements PlotData.DynUpdateListener {

        public UpdateListener() { }

        public void bandAdded(PlotData dpd, String id) {
            currDataContainer= findDataContainer(dpd);
            updateGrid(currDataContainer);
        }

        public void bandRemoved(PlotData dpd, String id) {
            currDataContainer= findDataContainer(dpd);
            updateGrid(currDataContainer);
        }

        public void newImage(PlotData dpd) {
            if (refreshListener!=null) refreshListener.preDataChange();
            currDataContainer= findDataContainer(dpd);
            updateGrid(currDataContainer);
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
