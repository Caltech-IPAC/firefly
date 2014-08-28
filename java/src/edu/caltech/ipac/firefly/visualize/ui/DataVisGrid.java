package edu.caltech.ipac.firefly.visualize.ui;
/**
 * User: roby
 * Date: 7/28/14
 * Time: 2:39 PM
 */


import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.fuse.data.BaseImagePlotDefinition;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.PlotWidgetFactory;
import edu.caltech.ipac.firefly.visualize.PlotWidgetOps;
import edu.caltech.ipac.firefly.visualize.Vis;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.WebPlotRequest;
import edu.caltech.ipac.firefly.visualize.WebPlotView;
import edu.caltech.ipac.firefly.visualize.graph.CustomMetaSource;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotMeta;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class DataVisGrid {

    private static int groupNum=0;
    private static final String GROUP_NAME_ROOT= "DataVisGrid-";
    private Map<String,MiniPlotWidget> mpwMap;
    private List<String> showMask= null;
    private List<XYPlotWidget> xyList;
    private SimpleLayoutPanel panel = new SimpleLayoutPanel();
    private Map<String,WebPlotRequest> currReqMap= new HashMap<String, WebPlotRequest>(9);
    private Map<String,List<WebPlotRequest>> curr3ReqMap= new HashMap<String, List<WebPlotRequest>>(5);
    private int plottingCnt;
    private String groupName= GROUP_NAME_ROOT+(groupNum++);
    private final GridRenderer gridRenderer ;
    private DeleteListener deleteListener= null;
    private boolean nextPlotIsExpanded= false;
    private MpwFactory mpwFactory= new DefaultMpwFactory();
    private boolean lockRelated= true;

    public DataVisGrid(List<String> plotViewerIDList, int xyPlotCount, Map<String,List<String>> viewToLayerMap ) {
        this(plotViewerIDList, xyPlotCount, viewToLayerMap, BaseImagePlotDefinition.GridLayoutType.AUTO);
    }

    public DataVisGrid(List<String> plotViewerIDList,
                       int xyPlotCount,
                       Map<String,List<String>> viewToLayerMap,
                       BaseImagePlotDefinition.GridLayoutType gridLayout) {
        mpwMap= new HashMap<String, MiniPlotWidget>(plotViewerIDList.size()+7);
        xyList= new ArrayList<XYPlotWidget>(xyPlotCount);

        switch (gridLayout) {
            case FINDER_CHART:
            case SINGLE_ROW:
            case AUTO:
            default:
                gridRenderer= new AutoGridRenderer(); break;
        }


        panel.add(gridRenderer.getWidget());
        for(String id : plotViewerIDList) {
            final MiniPlotWidget mpw=makeMpw(groupName, id,
                                             viewToLayerMap!=null ? viewToLayerMap.get(id) : null, false);
            mpwMap.put(id,mpw);
        }
        for(int i=0; (i<xyPlotCount); i++) {
            XYPlotMeta meta = new XYPlotMeta("none", 800, 200, new CustomMetaSource(new HashMap<String, String>()));
            XYPlotWidget xy= new XYPlotWidget(meta);
            xy.setTitleAreaAlwaysHidden(true);
            xyList.add(xy);
        }
        reinitGrid();
        groupNum++;

        WebEventManager.getAppEvManager().addListener(Name.REGION_HIDE, new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                Region source = (Region) ev.getSource();
                if (LayoutManager.POPOUT_REGION.equals(source.getId())) {
                    gridRenderer.onResize();
                }
            }
        });
    }

    public void setLockRelated(boolean lockRelated) {
        this.lockRelated= lockRelated;
    }

    public void setDeleteListener(DeleteListener dl) {
        this.deleteListener= dl;
    }

    public void setMpwFactory(MpwFactory mpwFactory) {
        this.mpwFactory = mpwFactory;
    }

    public void makeNextPlotExpanded() {
        nextPlotIsExpanded= true;
    }

    public boolean hasImagesPlotted() {
        boolean activeMpw= false;
        for(MiniPlotWidget mpw : mpwMap.values()) {
            activeMpw= mpw.isActive();
            if (activeMpw) break;
        }
        return activeMpw;
    }

    public void setActive(boolean active) {
        for (MiniPlotWidget mpw : mpwMap.values())  {
            mpw.setActive(active);
            if (active)  {
                mpw.notifyWidgetShowing();
                mpw.recallScrollPos();
            }
        }
    }

    public void setShowMask(List<String> showMask) {
        if (this.showMask==null || !this.showMask.equals(showMask)) {
            this.showMask= showMask;
            gridRenderer.setShowMask(showMask);
            reinitGrid();
        }
    }

    public void clearShowMask() {
        if (showMask==null) return;
        showMask= null;
        reinitGrid();
    }

    public boolean containsKey(String id) { return mpwMap.containsKey(id); }

    public void addWebPlotImage(String id, List<String> layerList, boolean addToGroup, boolean canDelete, boolean reinitNow) {
        if (!mpwMap.containsKey(id)) {
            final MiniPlotWidget mpw=makeMpw(addToGroup?groupName:null, id, layerList,canDelete);
            mpwMap.put(id, mpw);
            reinitGrid();
        }
    }

    private MiniPlotWidget makeMpw(String groupName,
                                   final String id,
                                   final List<String> idList,
                                   boolean canDelete) {
        final EventHub hub= Application.getInstance().getEventHub();
        final MiniPlotWidget mpw= mpwFactory.make(groupName);
        if (canDelete) mpw.setPlotWidgetFactory(new GridPlotWidgetFactory());
        Vis.init(mpw, new Vis.InitComplete() {
            public void done() {
                mpw.setExpandButtonAlwaysSingleView(true);
                mpw.getPlotView().setAttribute(WebPlotView.GRID_ID,id);
                mpw.setRemoveOldPlot(true);
                mpw.setTitleAreaAlwaysHidden(true);
                mpw.setInlineToolbar(true);
                mpw.setSaveImageCornersAfterPlot(true);
                mpw.setInlineTitleAlwaysOnIfCollapsed(true);
                mpw.setShowInlineTitle(true);
                mpw.setPreferenceColorKey(id);
                hub.getCatalogDisplay().addPlotView(mpw.getPlotView());
                if (idList!=null) hub.getDataConnectionDisplay().addPlotView(mpw.getPlotView(),idList);
            }
        });
        return mpw;
    }

    public void cleanup() {
        for (MiniPlotWidget mpw : mpwMap.values()) {
            mpw.freeResources();
        }
        for (XYPlotWidget xy : xyList) {
            xy.freeResources();
        }
        mpwMap.clear();
        xyList.clear();
        gridRenderer.clear();
    }

    public SimpleLayoutPanel getWidget() { return panel; }

    public void load(final Map<String,WebPlotRequest> reqMap,  final AsyncCallback<String> allDoneCB) {
        loadAsGroup(reqMap,allDoneCB);
    }

    public void loadSingleINTERAL(final Map<String,WebPlotRequest> reqMap,  final AsyncCallback<String> allDoneCB) {
        plottingCnt= 0;
        for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()){
            final String key= entry.getKey();
            if (showMask==null || showMask.contains(key)) {
                final MiniPlotWidget mpw= entry.getValue();
                boolean visible= true;
                WebPlotRequest req= reqMap.get(key);
                if (reqMap.containsKey(key) && req==null)  visible= false;
                if (visible && req!=null && !req.equals(currReqMap.get(key))) {
                    plottingCnt++;



                    mpw.getOps(new MiniPlotWidget.OpsAsync() {
                        public void ops(PlotWidgetOps widgetOps) {

                            final WebPlotRequest req= reqMap.get(key);
                            currReqMap.put(key,req.makeCopy());
                            req.setZoomToWidth(mpw.getOffsetWidth());
                            req.setZoomToHeight(mpw.getOffsetHeight());

                            widgetOps.plot(req, false, new AsyncCallback<WebPlot>() {
                                public void onFailure(Throwable caught) {
                                    plottingCnt--;
                                    completePlotting(allDoneCB);
                                    currReqMap.remove(key);
                                }

                                public void onSuccess(WebPlot result) {
                                    mpw.setShowInlineTitle(true);
                                    mpw.getGroup().setLockRelated(lockRelated);
                                    plottingCnt--;
                                    completePlotting(allDoneCB);
                                }
                            });
                        }
                    });
                }
            }
        }
    }


    public void loadAsGroup(final Map<String,WebPlotRequest> reqMap,  final AsyncCallback<String> allDoneCB) {
        List<String> keysToPlot= new ArrayList<String>(20);
        for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()){
            final String key= entry.getKey();
            if (showMask==null || showMask.contains(key)) {
                WebPlotRequest req= reqMap.get(key);
                boolean visible= true;
                if (reqMap.containsKey(key) && req==null)  visible= false;
                if (visible && req!=null && !req.equals(currReqMap.get(key))) {
                    keysToPlot.add(key);
                    currReqMap.put(key,req.makeCopy());
                    MiniPlotWidget mpw= mpwMap.get(key);
                    req.setZoomToWidth(mpw.getOffsetWidth());
                    req.setZoomToHeight(mpw.getOffsetHeight());
                }
            }
        }
        if (keysToPlot.size()>0) {
            final List<MiniPlotWidget> mpwList= new ArrayList<MiniPlotWidget>(keysToPlot.size());
            List<WebPlotRequest> rList= new ArrayList<WebPlotRequest>(keysToPlot.size());
            for(String key : keysToPlot) {
                mpwList.add(mpwMap.get(key));
                rList.add(reqMap.get(key));
            }

            PlotWidgetOps.plotGroup(panel,rList,mpwList,nextPlotIsExpanded, new AsyncCallback<WebPlot>() {
                public void onFailure(Throwable caught) { }

                public void onSuccess(WebPlot result) {
                    for(MiniPlotWidget mpw : mpwList) {
                        mpw.setShowInlineTitle(true);
                        mpw.getGroup().setLockRelated(lockRelated);
                        allDoneCB.onSuccess("OK");
                    }
                }
            });
            nextPlotIsExpanded= false;
        }
    }





    public void load3Color(final Map<String,List<WebPlotRequest>> reqMap,  final AsyncCallback<String> allDoneCB) {
        plottingCnt= 0;
        for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()){
            final String key= entry.getKey();
            if (showMask==null || showMask.contains(key)) {
                final MiniPlotWidget mpw= entry.getValue();
                boolean visible= true;
                List<WebPlotRequest> reqList= reqMap.get(key);
                if (reqMap.containsKey(key) && reqList==null)  visible= false;
                if (visible && reqList!=null && reqList.size()==3 && !threeColorReqSame(reqList,curr3ReqMap.get(key))) {
                    plottingCnt++;
                    mpw.getOps(new MiniPlotWidget.OpsAsync() {
                        public void ops(PlotWidgetOps widgetOps) {

                            final List<WebPlotRequest> reqList= reqMap.get(key);
                            final List<WebPlotRequest> copyList= new ArrayList<WebPlotRequest>(reqList.size());
                            for (WebPlotRequest r : reqList) {
                                copyList.add(r.makeCopy());
                                r.setZoomToWidth(mpw.getOffsetWidth());
                                r.setZoomToHeight(mpw.getOffsetHeight());
                            }
                            curr3ReqMap.put(key, copyList);

                            widgetOps.plot3Color(reqList.get(0), reqList.get(1), reqList.get(2), false, new AsyncCallback<WebPlot>() {
                                public void onFailure(Throwable caught) {
                                    plottingCnt--;
                                    curr3ReqMap.remove(key);
                                    completePlotting(allDoneCB);
                                    nextPlotIsExpanded= false;
                                }

                                public void onSuccess(WebPlot result) {
                                    mpw.setShowInlineTitle(true);
                                    mpw.getGroup().setLockRelated(lockRelated);
                                    plottingCnt--;
                                    completePlotting(allDoneCB);
                                    nextPlotIsExpanded= false;
                                }
                            });
                        }
                    });
                }
            }
        }
    }


    private boolean threeColorReqSame(List<WebPlotRequest> reqList, List<WebPlotRequest> curr3) {
        if (curr3==null || curr3.size()!=3) return false;
        return reqList.get(0).equals(curr3.get(0)) &&
               reqList.get(1).equals(curr3.get(1)) &&
               reqList.get(2).equals(curr3.get(2));


    }




    void completePlotting(AsyncCallback<String> allDoneCB) {
        if (plottingCnt==0) {
            allDoneCB.onSuccess("OK");
        }
    }


    public void reinitGrid() {
        gridRenderer.reinitGrid(mpwMap,xyList);
    }


    private class GridPlotWidgetFactory implements PlotWidgetFactory {
        public MiniPlotWidget create() {
            return null;
        }

        public String getCreateDesc() {
            return null;
        }

        public void prepare(MiniPlotWidget mpw, Vis.InitComplete initComplete) {
        }

        public WebPlotRequest customizeRequest(MiniPlotWidget mpw, WebPlotRequest wpr) {
            return null;
        }

        public boolean isPlottingExpanded() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void delete(MiniPlotWidget mpw) {
            String id= (String)mpw.getPlotView().getAttribute(WebPlotView.GRID_ID);
            final EventHub hub= Application.getInstance().getEventHub();
            hub.getDataConnectionDisplay().removePlotView(mpw.getPlotView());
            hub.getCatalogDisplay().removePlotView(mpw.getPlotView());
            mpwMap.remove(id);
            currReqMap.remove(id);
            curr3ReqMap.remove(id);
            final AllPlots ap= AllPlots.getInstance();
            ap.delete(mpw);
            if (deleteListener!=null) deleteListener.mpwDeleted(id);
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                public void execute() {
                    if (ap.isExpanded()) ap.updateExpanded(PopoutWidget.getViewType());
                    reinitGrid();
                }
            });
        }
    }

    public static interface DeleteListener {
        public void mpwDeleted(String id);
    }

    public static interface MpwFactory {
        public MiniPlotWidget make(String groupName);
    }

    public static class DefaultMpwFactory implements MpwFactory {
        public MiniPlotWidget make(String groupName) {
            return new MiniPlotWidget(groupName);
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
