/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 10/31/11
 * Time: 11:32 AM
 */


import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import edu.caltech.ipac.firefly.resbundle.css.CssData;
import edu.caltech.ipac.firefly.resbundle.css.FireflyCss;
import edu.caltech.ipac.firefly.ui.PopoutWidget;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotBasicWidget;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class ExpandBehavior extends PopoutWidget.Behavior {

    private static final FireflyCss fireflyCss = CssData.Creator.getInstance().getFireflyCss();
    private WorldPt _pagingCenter;
    private PopoutWidget.FillType oneFillStyle;
    private PopoutWidget.FillType gridFillStyle;
    private boolean gridFillTypeChange= false;
    private boolean oneFillTypeChange= false;
    private Dimension savedDim= new Dimension(-1,-1);

    private Map<PopoutWidget, Float> _oldZoomLevelMap = new HashMap<PopoutWidget, Float>(10);
    private ZoomSaveListener zSave= new ZoomSaveListener();
    private PopoutWidget.PopoutChoice lastList= null;



    public PopoutWidget.PopoutChoice getPopoutWidgetList(PopoutWidget activatingPopout) {

        AllPlots allPlots = AllPlots.getInstance();
        List<PopoutWidget> all = new ArrayList<PopoutWidget>(allPlots.getAllPopouts().size());
        List<PopoutWidget> selected = new ArrayList<PopoutWidget>(allPlots.getAllPopouts().size());

        for (MiniPlotWidget mpw : allPlots.getAll()) {
            if (mpw.getCurrentPlot() != null && mpw.getCurrentPlot().isAlive()) {
                all.add(mpw);
                WebEventManager evM= mpw.getPlotView().getEventManager();
                evM.removeListener(Name.REPLOT,zSave);
                evM.addListener(Name.REPLOT,zSave);
            }
        }
        all.addAll(allPlots.getAdditionalPopoutList());

        if (allPlots.getDefaultExpandUseType() == PopoutWidget.ExpandUseType.GROUP &&
                activatingPopout instanceof MiniPlotWidget) {
            PlotWidgetGroup group = ((MiniPlotWidget) activatingPopout).getGroup();
            for (MiniPlotWidget mpw : group.getAllActive()) {
                if (mpw.getCurrentPlot() != null && mpw.getCurrentPlot().isAlive()) {
                    selected.add(mpw);
                }
            }
        } else {
            selected.addAll(all);
        }

        // handle starting expanded plots
        if (selected.size()==0 && activatingPopout.isStartingExpanded() && activatingPopout instanceof MiniPlotWidget) {
            MiniPlotWidget mpw= (MiniPlotWidget)activatingPopout;
            selected.add(mpw);
            all.add(mpw);
        }
        lastList= new PopoutWidget.PopoutChoice(selected, all);

        return lastList;
    }

    public void onPreExpandCollapse(PopoutWidget popout,
                                    boolean expanded,
                                    final PopoutWidget activatingPopout) {
        if (popout instanceof MiniPlotWidget) {
            MiniPlotWidget mpw = (MiniPlotWidget) popout;
            if (expanded && mpw.getPlotView().getPrimaryPlot()!=null) {
                float oldZoomLevel = mpw.getPlotView().getPrimaryPlot().getZoomFact();
                _oldZoomLevelMap.put(popout, oldZoomLevel);
            }
        }
    }

    public void onPostExpandCollapse(final PopoutWidget popout,
                                     final boolean expanded,
                                     final PopoutWidget activatingPopout) {

        if (popout instanceof MiniPlotWidget) {
            final MiniPlotWidget mpw = (MiniPlotWidget) popout;
            final WebPlotView plotView = mpw.getPlotView();
            if (plotView == null)  return;

            if (plotView.getPrimaryPlot()!=null) {
                WebPlotGroup.fireReplotEvent(ReplotDetails.Reason.REPARENT, plotView.getPrimaryPlot());
            }
            if (expanded) {
                if (popout == activatingPopout) {
                    DeferredCommand.addCommand(new Command() {
                        public void execute() {
                            AllPlots ap = AllPlots.getInstance();
                            ap.setSelectedMPW(mpw, true);
                            ap.showMenuBarPopup();
                        }
                    });
                }
            } else {
                if (_oldZoomLevelMap.containsKey(popout)) {
                    plotView.setZoomTo(_oldZoomLevelMap.get(popout), false,false);
                }
                mpw.getGroup().setLastPoppedOut(null);
                AllPlots.getInstance().hideMenuBarPopup();
                AllPlots.getInstance().disableWCSMatch();
                mpw.updateUISelectedLook();
                mpw.setShowInlineTitle(false,true);
                savedDim= new Dimension(-1,-1);
            }
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    plotView.smartCenter();
                }
            });
            plotView.setScrollBarsEnabled(mpw.getShowScrollBars() || expanded);
            mpw.getTitleLayoutPanel().setPlotIsExpanded(expanded);

        } else if (popout instanceof XYPlotBasicWidget) {
            ((XYPlotBasicWidget) popout).onPostExpandCollapse(expanded);
        }

    }


    public void onPrePageInExpandedMode(PopoutWidget oldPopout, PopoutWidget newPopout, Dimension dim) {
        MiniPlotWidget mpwNew = null;
        MiniPlotWidget mpwOld = null;
        WebPlot oldPlot = null;
        PlotWidgetGroup oldGroup = null;
        WebPlot newPlot = null;
        AllPlots ap= AllPlots.getInstance();


        if (oldPopout instanceof MiniPlotWidget) {
            mpwOld = (MiniPlotWidget) oldPopout;
            oldPlot = mpwOld.getCurrentPlot();
            oldGroup = mpwOld.getGroup();
            _pagingCenter = oldPlot.getPlotView().findCurrentCenterWorldPoint();
        }

        if (newPopout instanceof MiniPlotWidget) {
            mpwNew = (MiniPlotWidget) newPopout;
            newPlot = mpwNew.getCurrentPlot();
            mpwNew.setShowInlineTitle(false);
            mpwNew.getPlotView().setScrollBarsEnabled(!ap.isWCSMatch());
        }



        if (mpwOld != null && mpwNew != null && oldGroup.contains(mpwNew) && oldGroup.getLockRelated() ) {
            if (ap.isWCSMatch()) {
                if (ZoomUtil.isWCSSynced(mpwOld,mpwNew))  mpwNew.refreshWidget();
                else                                      ZoomUtil.wcsSyncToMatch(mpwOld, mpwNew,ap.isWCSMatchIsNorth());
            }
            else if (!getScalesMatch(oldPlot,newPlot)) {
                setSingleModeZoomByScale(mpwNew, ZoomUtil.getArcSecPerPix(oldPlot));
            }
            else {
                mpwNew.refreshWidget();
            }
        } else if (mpwNew != null) {
            MiniPlotWidget mpwLast = mpwNew.getGroup().getLastPoppedOut();
            if (mpwLast == null || !mpwNew.getGroup().getLockRelated()) {
                setSingleModeZoom(mpwNew, dim);
            } else {
                WebPlot lastPlot = mpwLast.getCurrentPlot();
                if (!getScalesMatch(lastPlot,newPlot)) {
                    setSingleModeZoomByScale(mpwNew, ZoomUtil.getArcSecPerPix(lastPlot));
                }
            }
        }
    }

    public void onPostPageInExpandedMode(PopoutWidget oldPopout, PopoutWidget newPopout, Dimension dim) {
        if (newPopout instanceof MiniPlotWidget) {
            final MiniPlotWidget mpwNew = (MiniPlotWidget) newPopout;
            mpwNew.getGroup().setLastPoppedOut(mpwNew);
            AllPlots.getInstance().setSelectedMPW(mpwNew, true);
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    if (AllPlots.getInstance().isWCSMatch()) {
                        mpwNew.getPlotView().smartCenter();
                    }
                    else {
                        mpwNew.getPlotView().centerOnPoint(_pagingCenter);
                    }
                }
            });
        } else {
            AllPlots.getInstance().setSelectedMPW(null, true);
            newPopout.widgetResized(dim.getWidth(), dim.getHeight());
        }
    }

    public PopoutWidget chooseCurrentInExpandMode() {
        return AllPlots.getInstance().getSelectPopoutWidget();
    }


    private boolean isInvalidDim(Dimension dim) {
        return (dim==null || dim.getWidth() == 0 || dim.getHeight() == 0);
    }


    public void onSingleResize(PopoutWidget popout, Dimension dim, boolean adjustZoom) {
        if (isInvalidDim(dim)) return;

        AllPlots ap= AllPlots.getInstance();
        if (popout instanceof MiniPlotWidget) {
            MiniPlotWidget mpw = (MiniPlotWidget) popout;
            if (oneDrawingDimChange(dim) && adjustZoom && mpw.getCurrentPlot()!=null) {
                WebPlotView plotView = mpw.getPlotView();
                setSingleModeZoom(mpw, dim);
                mpw.getGroup().setLastPoppedOut(mpw);
                plotView.setScrollBarsEnabled(!ap.isWCSMatch());
                mpw.setShowInlineTitle(false);
                saveCurrentDrawingDim(dim);
            }
        } else {
            popout.widgetResized(dim.getWidth(), dim.getHeight());
        }

    }



    public void onGridResize(List<PopoutWidget> popoutList, Dimension dim, boolean adjustZoom) {
        if (isInvalidDim(dim) || popoutList.size()==0) return;

        AllPlots ap= AllPlots.getInstance();
        MiniPlotWidget mpwPrim= ap.getMiniPlotWidget();
        WebPlot primPlot= mpwPrim!=null ? mpwPrim.getCurrentPlot() : null;
        for(PopoutWidget popout : popoutList) {
            if (popout instanceof MiniPlotWidget) {
                MiniPlotWidget mpw = (MiniPlotWidget) popout;
                if (gridDrawingDimChange(dim) && adjustZoom && mpw.getCurrentPlot()!=null) {
                    WebPlotView plotView = mpw.getPlotView();

                    if (ap.isWCSMatch()) {
                        float zlevel = computeZoomFactorInGridMode(mpwPrim, dim);
                        float arcsecPerPix= ZoomUtil.getArcSecPerPix(primPlot,zlevel);
                        ZoomUtil.wcsSyncToAS(mpwPrim, mpw, arcsecPerPix, ap.isWCSMatchIsNorth());
                    }
                    else {
                        float zlevel = computeZoomFactorInGridMode(mpw, dim);
                        setGridModeZoom(plotView, zlevel);
                    }
                    plotView.setScrollBarsEnabled(false);
                    mpw.setShowInlineTitle(true);

                }

            } else {
                popout.widgetResized(dim.getWidth(), dim.getHeight());
            }
        }
        saveCurrentDrawingDim(dim);

    }


    private boolean gridDrawingDimChange(Dimension dim) {
        return !dim.equals(savedDim) || gridFillTypeChange;
    }
    private boolean oneDrawingDimChange(Dimension dim) {
        return !dim.equals(savedDim) || oneFillTypeChange;
    }

    private void saveCurrentDrawingDim(Dimension dim) {
        gridFillTypeChange= false;
        oneFillTypeChange= false;
        savedDim= dim;
    }

    private float computeZoomFactorInGridMode(MiniPlotWidget mpw, Dimension dim) {
        float zLevel = 1;
        PopoutWidget.FillType ft= gridFillStyle;
        if (AllPlots.getInstance().isWCSMatch()) ft= PopoutWidget.FillType.FIT;
        WebPlot plot= mpw.getCurrentPlot();
        switch (ft) {
            case OFF:
                zLevel= plot.getZoomFact();
                break;
            case CONTEXT:
                VisUtil.FullType fullType = VisUtil.FullType.ONLY_WIDTH;
                if (mpw.getGroup()!=null)  fullType = mpw.getGroup().getGridPopoutZoomType();
                zLevel = ZoomUtil.getEstimatedFullZoomFactor(plot, dim, fullType, -1, 1);
                break;
            case FILL:
                zLevel = ZoomUtil.getEstimatedFullZoomFactor(plot, dim, VisUtil.FullType.ONLY_WIDTH,-1,1);
                break;
            case FIT:
                zLevel = ZoomUtil.getEstimatedFullZoomFactor(plot, dim, VisUtil.FullType.WIDTH_HEIGHT ,-1, 1);
                break;
        }
        return zLevel;

    }


    private float computeZoomFactorInOneMode(PopoutWidget popout, WebPlot plot, Dimension dim) {
        Dimension nDim= new Dimension(dim.getWidth() - 15, dim.getHeight());
        float zLevel = 1;
        switch (oneFillStyle) {
            case OFF:
                zLevel= _oldZoomLevelMap.containsKey(popout) ?_oldZoomLevelMap.get(popout) : 1;
                if (zLevel>plot.getZoomFact()) zLevel= plot.getZoomFact();
                if (getLastExpandedZoomLevel(plot)>0) zLevel= getLastExpandedZoomLevel(plot);
                break;
            case FILL:
                zLevel = ZoomUtil.getEstimatedFullZoomFactor(plot, nDim, VisUtil.FullType.ONLY_WIDTH );
                break;
            case FIT:
                zLevel = ZoomUtil.getEstimatedFullZoomFactor(plot, nDim, VisUtil.FullType.WIDTH_HEIGHT );
                break;
            case CONTEXT: // for now do a fit
                zLevel = ZoomUtil.getEstimatedFullZoomFactor(plot, nDim, VisUtil.FullType.WIDTH_HEIGHT );
                break;
        }
        return zLevel;
    }

    @Override
    public void setOnePlotFillStyle(PopoutWidget.FillType fillStyle) {
        oneFillTypeChange= fillStyle!=oneFillStyle;
        this.oneFillStyle = fillStyle;
    }

    public void setGridPlotFillStyle(PopoutWidget.FillType fillStyle) {
        gridFillTypeChange= fillStyle!=gridFillStyle;
        gridFillStyle = fillStyle;
    }

    public String getGridBorderStyle(PopoutWidget popout) {
        if (popout == AllPlots.getInstance().getMiniPlotWidget()) {
//            return "2px solid "+fireflyCss.selectedColor();
//            return "2px solid green";
            return "3px ridge orange";
        } else {
//            return "2px solid "+fireflyCss.highlightColor();
            return "3px groove " + fireflyCss.highlightColor();
        }

    }

    //------------------------------------------------------------------
    //----------- Zoom related methods
    //------------------------------------------------------------------

    private static boolean getScalesMatch(WebPlot p1, WebPlot p2) {
        float scale1 = ZoomUtil.getArcSecPerPix(p1);
        float scale2 =  ZoomUtil.getArcSecPerPix(p2);
        return (Math.abs(scale1 - scale2) < .01);
    }


    private static float getMaxZoomLevel(WebPlotView pv, float level) {
        if (pv.containsAttributeKey(WebPlot.MAX_EXPANDED_ZOOM_LEVEL)) {
            Object maxZ = pv.getAttribute(WebPlot.MAX_EXPANDED_ZOOM_LEVEL);
            if (maxZ instanceof Number) {
                level = ((Number) maxZ).floatValue();
            }
        }
        return level;
    }

    public static boolean keepZoomLevel(MiniPlotWidget mpw) {
        boolean retval= false;
        if (mpw!=null && mpw.getPlotView()!=null) {
            WebPlot p= mpw.getPlotView().getPrimaryPlot();
            if (p!=null && p.getPlotState()!=null) {
                PlotState state= p.getPlotState();
                WebPlotRequest wpr= state.getWebPlotRequest(state.firstBand());
                retval= wpr.getZoomType()==ZoomType.FORCE_STANDARD;
            }
        }
        return retval;
    }


    private  void setSingleModeZoom(MiniPlotWidget mpw, Dimension dim) {
        WebPlotView pv= mpw.getPlotView();
        WebPlot p= pv.getPrimaryPlot();
        float level = computeZoomFactorInOneMode(mpw, mpw.getCurrentPlot(), dim);
        level= getMaxZoomLevel(pv,level);
        if (Math.abs(level-p.getZoomFact())>.03) {
            pv.setZoomTo(level, true, false);
        }
        else {
            mpw.refreshWidget();
        }
    }

    private static void setSingleModeZoomByScale(MiniPlotWidget mpwNew, float arcsecPerPix) {
        WebPlotView pv = mpwNew.getPlotView();
        float testLevel= getMaxZoomLevel(pv, -1);
        float zfact = (float) pv.getPrimaryPlot().getImagePixelScaleInArcSec() / arcsecPerPix;
        if (testLevel>0 && testLevel<zfact) {
            pv.setZoomTo(testLevel, true, false);
        }
        else {
            pv.setZoomByArcsecPerScreenPix(arcsecPerPix, true, false);
        }

    }


    private static void setGridModeZoom(WebPlotView plotView, float level) {
        level= getMaxZoomLevel(plotView,level);
        if (Math.abs(level-plotView.getPrimaryPlot().getZoomFact())>.03) {
            plotView.setZoomTo(level, true, false);
        }
        else {
            plotView.getPrimaryPlot().refreshWidget();
        }
    }




    //------------------------------------------------------------------
    //----------- Zoom related methods
    //------------------------------------------------------------------



    private float getLastExpandedZoomLevel(WebPlot p) {
        float retval= 0;
        if (p.containsAttributeKey(WebPlot.LAST_EXPANDED_ZOOM_LEVEL)) {
            retval= (Float)p.getAttribute(WebPlot.LAST_EXPANDED_ZOOM_LEVEL);
        }
        return retval;
    }

    private class ZoomSaveListener implements WebEventListener {
        public void eventNotify(WebEvent ev) {
            if (ev.getName().equals(Name.REPLOT) &&
                oneFillStyle == PopoutWidget.FillType.OFF &&
                (PopoutWidget.getViewType()==PopoutWidget.ViewType.ONE ||  lastList.getSelectedList().size()==1)) {
                if (ev.getData() instanceof ReplotDetails) {
                    ReplotDetails d= (ReplotDetails)ev.getData();
                    if (d.getReplotReason()== ReplotDetails.Reason.ZOOM_COMPLETED) {
                        if (d.getPlot().getPlotView().getMiniPlotWidget().isExpanded()) {
                                WebPlot p= d.getPlot();
                                d.getPlot().setAttribute(WebPlot.LAST_EXPANDED_ZOOM_LEVEL, p.getZoomFact());

                        }
                    }
                }
            }
        }
    }


}

