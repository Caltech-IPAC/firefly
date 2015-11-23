/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.ui;
/**
 * User: roby
 * Date: 7/28/14
 * Time: 2:39 PM
 */


import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import edu.caltech.ipac.firefly.core.Application;
import edu.caltech.ipac.firefly.core.layout.LayoutManager;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.fuse.DatasetInfoConverter;
import edu.caltech.ipac.firefly.data.fuse.ImagePlotDefinition;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.ui.creator.CommonParams;
import edu.caltech.ipac.firefly.ui.table.EventHub;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.WebAssert;
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
import edu.caltech.ipac.firefly.visualize.ZoomType;
import edu.caltech.ipac.firefly.visualize.graph.CustomMetaSource;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotMeta;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;
import edu.caltech.ipac.util.ComparisonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class DataVisGrid {

    private Map<String,MiniPlotWidget> mpwMap;
    private DatasetInfoConverter currInfo= null;
    private List<String> showMask= null;
    private List<XYPlotWidget> xyList;
    private SimpleLayoutPanel panel = new SimpleLayoutPanel();
    private Map<String,WebPlotRequest> currReqMap= new LinkedHashMap<String, WebPlotRequest>();
    private Map<String,List<WebPlotRequest>> curr3ReqMap= new LinkedHashMap<String, List<WebPlotRequest>>();
    private List<String> keysInvisible= new ArrayList<String>(20);
    private int plottingCnt;
    private final GridRenderer gridRenderer ;
    private DeleteListener deleteListener= null;
    private boolean nextPlotIsExpanded= false;
    private MpwFactory mpwFactory= new DefaultMpwFactory();
    private boolean lockRelated= true;
    private Dimension defaultDim= null;
    private DatasetInfoConverter info= null;
    private final EventHub hub;
    private static boolean renderFactoryInit= false;
    private static Map<String,GridRenderFactory> renderFactoryMap= new HashMap<String, GridRenderFactory>(7);
    private final String groupName;
    private boolean primaryOnly= false;


    public DataVisGrid(EventHub hub,
                       List<String> plotViewerIDList,
                       int xyPlotCount,
                       Map<String,List<String>> viewToLayerMap,
                       String gridLayout,
                       String groupName) {
        this.hub= hub!=null ? hub : Application.getInstance().getEventHub();
        mpwMap= new LinkedHashMap<String, MiniPlotWidget>();
        xyList= new ArrayList<XYPlotWidget>(xyPlotCount);
        gridRenderer= makeGridRenderer(gridLayout);
        this.groupName= groupName;

        panel.add(gridRenderer.getWidget());
        for(String id : plotViewerIDList) {
            List<String> idList= null;
            if (viewToLayerMap!= null) {
                idList= viewToLayerMap.get(viewToLayerMap.containsKey(id) ? id : CommonParams.ALL);
            }
            final MiniPlotWidget mpw=makeMpw(this.groupName, id, idList, false);
            mpwMap.put(id,mpw);
        }
        for(int i=0; (i<xyPlotCount); i++) {
            XYPlotMeta meta = new XYPlotMeta("none", 800, 200, new CustomMetaSource(new HashMap<String, String>()));
            XYPlotWidget xy= new XYPlotWidget(meta);
            xy.setTitleAreaAlwaysHidden(true);
            xyList.add(xy);
        }
        reinitGrid();

        WebEventManager.getAppEvManager().addListener(Name.REGION_HIDE, new WebEventListener(){
            public void eventNotify(WebEvent ev) {
                Region source = (Region) ev.getSource();
                if (LayoutManager.POPOUT_REGION.equals(source.getId())) {
                    gridRenderer.onResize();
                }
            }
        });
    }


    public void setDatasetInfoConverter(DatasetInfoConverter info) {
        this.info= info;
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



    public void showPrimaryOnly(boolean primaryOnly) {
        this.primaryOnly= primaryOnly;
        gridRenderer.showPrimaryOnly(primaryOnly);
        redrawAndCenter();
    }

    public void iteratePrimary(boolean forward) {
        if (primaryOnly) {
            MiniPlotWidget mpwArray[]= mpwMap.values().toArray(new MiniPlotWidget[mpwMap.size()]);
            MiniPlotWidget mpw= AllPlots.getInstance().getMiniPlotWidget();
            MiniPlotWidget targetMpw= mpwArray[0];
            int len= mpwArray.length;
            for(int i=0; (i<len);i++) {
                if (mpwArray[i]==mpw) {
                    if (forward) {
                        targetMpw= i<len-1 ? mpwArray[i+1] : mpwArray[0];
                    }
                    else {
                        targetMpw= i>0 ? mpwArray[i-1] : mpwArray[len-1];
                    }
                }
            }
            AllPlots.getInstance().setSelectedMPW(targetMpw);
            Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
                @Override
                public void execute() {
                    redrawAndCenter();
                }
            });
        }
    }


    private void redrawAndCenter() {
        reinitGrid();
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                for(MiniPlotWidget mpw : mpwMap.values()) {
                    if (mpw.getPlotView()!=null) {
                        mpw.getPlotView().smartCenter();
                    }
                }
            }
        });
    }

    public void ensureMPWSelected() {
        for(MiniPlotWidget mpw : mpwMap.values()) {
            if (mpw.getCurrentPlot()!=null) {
                AllPlots.getInstance().setSelectedMPW(mpw);
                break;
            }
        }
    }


    public boolean hasImagesPlotted() {
        boolean activeMpw= false;
        for(MiniPlotWidget mpw : mpwMap.values()) {
            activeMpw= mpw.isActive();
            if (activeMpw) break;
        }
        return activeMpw;
    }


    public int getImageShowCount() {
        int total=0;
        for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()) {
            MiniPlotWidget mpw= entry.getValue();
            if (mpw!=null && mpw.getCurrentPlot()!=null &&
                    (showMask==null || showMask.contains(entry.getKey()) )) {
                total++;
            }
        }
        return total;
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
        gridRenderer.setShowMask(null);
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
//        final EventHub hub= Application.getInstance().getEventHub();
        final MiniPlotWidget mpw= mpwFactory.make(groupName);
        if (canDelete) mpw.setPlotWidgetFactory(new GridPlotWidgetFactory());
        Vis.init(mpw, new Vis.InitComplete() {
            public void done() {
                mpw.setExpandButtonAlwaysSingleView(true);
                mpw.setRemoveOldPlot(true);
                mpw.setTitleAreaAlwaysHidden(true);
                mpw.setInlineToolbar(true);
                mpw.setSaveImageCornersAfterPlot(true);
                mpw.setInlineTitleAlwaysOnIfCollapsed(true);
                mpw.setShowInlineTitle(true);
                mpw.setPreferenceColorKey(id);
                hub.getCatalogDisplay().addPlotView(mpw.getPlotView());
                mpw.getPlotView().setAttribute(WebPlotView.GRID_ID,id);
                if (info!=null) mpw.getPlotView().setAttribute(WebPlotView.DATASET_INFO_CONVERTER,info);
                if (idList!=null) hub.getDataConnectionDisplay().addPlotView(mpw.getPlotView(),idList);
                mpwFactory.addAttributes(mpw);

//                mpw.setErrorDisplayHandler(new MiniPlotWidget.PlotError() {
//                    public void onError(WebPlot wp, String briefDesc, String desc, String details, Exception e) {
//                        //todo: right now it is silent
//                    }
//                });


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

    public void load(Map<String,WebPlotRequest> reqMap,  DatasetInfoConverter info, AsyncCallback<String> allDoneCB) {
        loadAsGroup(reqMap,info, allDoneCB);
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
                if (visible && req!=null && !req.equalsPlottingParams(currReqMap.get(key))) {
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


    public void loadAsGroup(final Map<String,WebPlotRequest> reqMap,
                            DatasetInfoConverter info,
                            final AsyncCallback<String> allDoneCB) {
        List<String> keysToPlot= new ArrayList<String>(20);
        keysInvisible.clear();
        currInfo= info;
        for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()){
            final String key= entry.getKey();
            if (showMask==null || showMask.contains(key)) {
                WebPlotRequest req= reqMap.get(key);
                boolean visible= true;
                if (reqMap.containsKey(key) && req==null)  visible= false;
                if (visible && req!=null && !req.equalsPlottingParams(currReqMap.get(key))) {
                    keysToPlot.add(key);
                    currReqMap.put(key,req.makeCopy());
                    MiniPlotWidget mpw= mpwMap.get(key);
                    setWidgetSizing(mpw,req);
                }
                if (!visible) keysInvisible.add(key);
            }
        }
        if (keysToPlot.size()>0) {
            final List<MiniPlotWidget> mpwList= new ArrayList<MiniPlotWidget>(keysToPlot.size());
            List<WebPlotRequest> rList= new ArrayList<WebPlotRequest>(keysToPlot.size());
            plottingCnt= 0;
            for(String key : keysToPlot) {
                plottingCnt++;
                mpwList.add(mpwMap.get(key));
                rList.add(reqMap.get(key));
            }

            Map<String,SubGroupContainer> sgcMap= breakUpInSubgroups(mpwList,rList);
            for(Map.Entry<String,SubGroupContainer> entry : sgcMap.entrySet()) {
                Element maskElement= panel.getElement();
                if (gridRenderer.getMaskingElement(entry.getKey())!=null)  {
                    maskElement= gridRenderer.getMaskingElement(entry.getKey());
                }
                SubGroupContainer sgCon= entry.getValue();

                PlotWidgetOps.plotGroup(maskElement,sgCon.rList,sgCon.mpwList,nextPlotIsExpanded
                        , new AsyncCallback<WebPlot>() {
                    public void onFailure(Throwable caught) {
                        plottingCnt--;
                        completeGroupPlotting(allDoneCB, mpwList);
                    }

                    public void onSuccess(WebPlot result) {
                        plottingCnt--;
                        completeGroupPlotting(allDoneCB, mpwList);
                    }
                });
            }

            nextPlotIsExpanded= false;
        }
        else {
            allDoneCB.onSuccess("ok");
        }
    }


    void completeGroupPlotting(AsyncCallback<String> allDoneCB, List<MiniPlotWidget> mpwList) {
        if (plottingCnt==0) {
            for(MiniPlotWidget mpw : mpwList) {
                mpw.setShowInlineTitle(true);
                mpw.getGroup().setLockRelated(lockRelated);
            }
            allDoneCB.onSuccess("OK");
            reinitGrid();
            gridRenderer.postPlotting();
        }
    }




    private void setWidgetSizing(MiniPlotWidget mpw, WebPlotRequest req) {
        if (req==null) return;
        if (req.getZoomToWidth()==0)  {
            req.setZoomToWidth(mpw.getOffsetWidth());
            if (primaryOnly) req.setZoomToWidth(gridRenderer.getWidget().getOffsetWidth());
        }
        else { // if the request is TO_WIDTH and the width is set the set the widget also
            if (req.getZoomType()== ZoomType.TO_WIDTH || req.getZoomType()== ZoomType.FULL_SCREEN) {
                mpw.setWidth(req.getZoomToWidth() + "");
            }
        }
        if (req.getZoomToHeight()==0) {
            req.setZoomToHeight(mpw.getOffsetHeight());
            if (primaryOnly) req.setZoomToHeight(gridRenderer.getWidget().getOffsetHeight());
        }
        else {
            if (req.getZoomType()== ZoomType.TO_HEIGHT ||
                    req.getZoomType()== ZoomType.FULL_SCREEN ||
                    req.getZoomType()== ZoomType.TO_WIDTH ) {
                mpw.setHeight(req.getZoomToHeight() + "");
            }
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
                                copyList.add( r!=null ? r.makeCopy() : null);
                            }

                            for(WebPlotRequest r : reqList) setWidgetSizing(mpw, r);

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
                                    gridRenderer.postPlotting();
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
        return ComparisonUtil.equals(reqList.get(0), curr3.get(0)) &&
               ComparisonUtil.equals(reqList.get(1), curr3.get(1)) &&
               ComparisonUtil.equals(reqList.get(2), curr3.get(2));
    }




    void completePlotting(AsyncCallback<String> allDoneCB) {
        if (plottingCnt==0) {
            allDoneCB.onSuccess("OK");
        }
    }


    public void reinitGrid() {
        Map<String,MiniPlotWidget> workMap= new LinkedHashMap<String, MiniPlotWidget>(mpwMap);
        for(String key : keysInvisible)  {
            workMap.remove(key);
        }
        if (currInfo!=null && currInfo.getImagePlotDefinition().getImagePlotDimension()!=null) {
            gridRenderer.setDimension(currInfo.getImagePlotDefinition().getImagePlotDimension());
        }
        gridRenderer.reinitGrid(workMap,xyList);
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
                    gridRenderer.postPlotting();
                }
            });
        }
    }



    public Map<String,SubGroupContainer> breakUpInSubgroups(List<MiniPlotWidget> mpwList, List<WebPlotRequest> rList) {
        boolean hasSubGroup= false;
        Map<String,SubGroupContainer> retMap= new HashMap<String,SubGroupContainer>(10);
        for(WebPlotRequest r : rList) {
            if (r.getDrawingSubGroupId()!=null) {
                hasSubGroup= true;
                break;
            }
        }

        if (hasSubGroup) {
            for(int i= 0; (i<mpwList.size()); i++) {
                WebPlotRequest r= rList.get(i);
                MiniPlotWidget mpw= mpwList.get(i);
                String sgID= r.getDrawingSubGroupId();
                if (sgID==null) sgID= "EVERYTHING";
                SubGroupContainer sgContainer= null;

                if (retMap.containsKey(sgID)) {
                    sgContainer= retMap.get(sgID);
                }
                else {
                    sgContainer= new SubGroupContainer();
                    retMap.put(sgID,sgContainer);
                }

                sgContainer.mpwList.add(mpw);
                sgContainer.rList.add(r);
            }

        }
        else {
            retMap.put("EVERYTHING", new SubGroupContainer(mpwList,rList));
        }
        return retMap;
    }


    private static class SubGroupContainer {
        final List<MiniPlotWidget> mpwList;
        final List<WebPlotRequest> rList;

        private SubGroupContainer(List<MiniPlotWidget> mpwList, List<WebPlotRequest> rList) {
            this.mpwList = mpwList;
            this.rList = rList;
        }

        private SubGroupContainer() {
            this.mpwList= new ArrayList<MiniPlotWidget>(10);
            this.rList= new ArrayList<WebPlotRequest>(10);
        }
    }

    public interface DeleteListener {
        void mpwDeleted(String id);
    }

    public interface MpwFactory {
        MiniPlotWidget make(String groupName);
        void addAttributes(MiniPlotWidget mpw);
    }

    public static class DefaultMpwFactory implements MpwFactory {
        public MiniPlotWidget make(String groupName) {
            return new MiniPlotWidget(groupName);
        }

        public void addAttributes(MiniPlotWidget mpw) {/*do nothing*/ }
    }

    public interface GridRenderFactory {
        GridRenderer makeGridRenderer();
    }

    public static GridRenderer makeGridRenderer(String typeString) {
        initRenderFactory();
        GridRenderFactory f= renderFactoryMap.get(typeString);
        if (f==null) f= renderFactoryMap.get(ImagePlotDefinition.AUTO_GRID_LAYOUT);
        WebAssert.argTst(f!=null, "Could not find a GridRendererFactory");
        return f.makeGridRenderer();
    }

    private static void initRenderFactory() {
        if (!renderFactoryInit) {
            renderFactoryInit= true;
            renderFactoryMap.put(ImagePlotDefinition.AUTO_GRID_LAYOUT, new GridRenderFactory() {
                public GridRenderer makeGridRenderer() { return new AutoGridRenderer(); }
            });
            renderFactoryMap.put(ImagePlotDefinition.FINDER_CHART_GRID_LAYOUT, new GridRenderFactory() {
                public GridRenderer makeGridRenderer() { return new FinderChartGridRenderer(); }
            });
        }

    }

    public static void setGridRenderer(String typeString, GridRenderFactory f) {
        initRenderFactory();
        renderFactoryMap.put(typeString,f);
    }
}

