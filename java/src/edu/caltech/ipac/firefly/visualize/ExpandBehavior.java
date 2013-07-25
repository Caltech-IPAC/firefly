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
import edu.caltech.ipac.firefly.util.WebAssert;
import edu.caltech.ipac.firefly.util.event.Name;
import edu.caltech.ipac.firefly.util.event.WebEvent;
import edu.caltech.ipac.firefly.util.event.WebEventListener;
import edu.caltech.ipac.firefly.util.event.WebEventManager;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
class ExpandBehavior extends PopoutWidget.Behavior {

    private static final FireflyCss fireflyCss = CssData.Creator.getInstance().getFireflyCss();
    private WorldPt _pagingCenter;
    private PopoutWidget.FillType oneFillStyle;
    private PopoutWidget.FillType gridFillStyle;

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
                            ap.setSelectedWidget(mpw, true);
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
                AllPlots.getInstance().setWCSSync(false);
                mpw.updateUISelectedLook();
                mpw.setShowInlineTitle(false,true);
            }
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    plotView.smartCenter();
                }
            });
            plotView.setScrollBarsEnabled(mpw.getShowScrollBars() || expanded);
            mpw.getTitleLayoutPanel().setPlotIsExpanded(expanded);

        } else if (popout instanceof XYPlotWidget) {
            ((XYPlotWidget) popout).onPostExpandCollapse(expanded);
        }

    }


    public void onPrePageInExpandedMode(PopoutWidget oldPopout, PopoutWidget newPopout, Dimension dim) {
        MiniPlotWidget mpwNew = null;
        MiniPlotWidget mpwOld = null;
        WebPlot oldPlot = null;
        PlotWidgetGroup oldGroup = null;
        WebPlot newPlot = null;


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
            mpwNew.getPlotView().setScrollBarsEnabled(true);
        }



        if (mpwOld != null && mpwNew != null && oldGroup.contains(mpwNew) && oldGroup.getLockRelated() ) {
            float oldArcsecPerPix = ZoomUtil.getArcSecPerPix(oldPlot);
            float newArcsecPerPix = ZoomUtil.getArcSecPerPix(newPlot);
            if (Math.abs(oldArcsecPerPix - newArcsecPerPix) > .01) {
                setExpandZoomByArcsecPerScreenPix(mpwNew, oldArcsecPerPix);
            }
            else {
                mpwNew.getCurrentPlot().refreshWidget();
            }
        } else if (mpwNew != null) {
            MiniPlotWidget mpwLast = mpwNew.getGroup().getLastPoppedOut();
            if (mpwLast == null || !mpwNew.getGroup().getLockRelated()) {
//                float zLevel = ZoomUtil.getEstimatedFullZoomFactor(newPlot, dim);
                float zLevel = computeZoomFactorInOneMode(newPopout,newPlot,dim);
                setExpandedZoom(mpwNew.getPlotView(), zLevel, true);
            } else {
                final WebPlot lastPlot = mpwLast.getCurrentPlot();
                float oldArcsecPerPix = (float) lastPlot.getImagePixelScaleInArcSec() / lastPlot.getZoomFact();
                float newArcsecPerPix = (float) newPlot.getImagePixelScaleInArcSec() / newPlot.getZoomFact();
                if (Math.abs(oldArcsecPerPix - newArcsecPerPix) > .01) {
                    setExpandZoomByArcsecPerScreenPix(mpwNew, oldArcsecPerPix);
                }

            }
        }
    }

    public void onPostPageInExpandedMode(PopoutWidget oldPopout, PopoutWidget newPopout, Dimension dim) {
        if (newPopout instanceof MiniPlotWidget) {
            final MiniPlotWidget mpwNew = (MiniPlotWidget) newPopout;
            mpwNew.getGroup().setLastPoppedOut(mpwNew);
            AllPlots.getInstance().setSelectedWidget(mpwNew, true);
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    if (AllPlots.getInstance().isWCSSync()) {
                        mpwNew.getPlotView().smartCenter();
                    }
                    else {
                        mpwNew.getPlotView().centerOnPoint(_pagingCenter);
                    }
                }
            });
        } else {
            newPopout.widgetResized(dim.getWidth(), dim.getHeight());
        }
    }

    public PopoutWidget chooseCurrentInExpandMode() {
        return AllPlots.getInstance().getMiniPlotWidget();
    }


    public void onResizeInExpandedMode(PopoutWidget popout, Dimension dim, PopoutWidget.ViewType viewType, boolean adjustZoom) {

        if (dim.getWidth() > 0 && dim.getHeight() > 0) {
            if (popout instanceof MiniPlotWidget) {
                if (adjustZoom) {
                    MiniPlotWidget mpw = (MiniPlotWidget) popout;
                    final WebPlotView plotView = mpw.getPlotView();
                    if (mpw.getCurrentPlot() != null) {
                        if (viewType == PopoutWidget.ViewType.GRID) {
                            float zlevel = computeZoomFactorInGridMode(mpw,mpw.getCurrentPlot(), dim);
                            setExpandedZoom(plotView, zlevel, true);
                            plotView.setScrollBarsEnabled(false);
                            mpw.setShowInlineTitle(true);
                        } else if (viewType == PopoutWidget.ViewType.ONE) {
//                            float zLevel = ZoomUtil.getEstimatedFullZoomFactor(mpw.getCurrentPlot(),
//                                                                               new Dimension(dim.getWidth() - 15, dim.getHeight()));
                            float zLevel = computeZoomFactorInOneMode(popout, mpw.getCurrentPlot(), dim);
                            setExpandedZoom(plotView, zLevel, true);
                            mpw.getGroup().setLastPoppedOut(mpw);
                            plotView.setScrollBarsEnabled(true);
                            mpw.setShowInlineTitle(false);

                        } else {
                            WebAssert.argTst(false, "only two view types, GRID & ONE");
                        }
                    }
                }

            } else {
                popout.widgetResized(dim.getWidth(), dim.getHeight());
            }

        }
    }


    private float computeZoomFactorInGridMode(MiniPlotWidget mpw, WebPlot plot, Dimension dim) {
        float zLevel = 1;
        switch (gridFillStyle) {
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
        this.oneFillStyle = fillStyle;
    }

    public void setGridPlotFillStyle(PopoutWidget.FillType fillStyle) {
        this.gridFillStyle = fillStyle;
    }

    public String getGridBorderStyle(PopoutWidget popout) {
        if (popout == AllPlots.getInstance().getMiniPlotWidget()) {
//            return "2px solid "+fireflyCss.selectedColor();
//            return "2px solid green";
            return "2px ridge orange";
        } else {
//            return "2px solid "+fireflyCss.highlightColor();
            return "2px groove " + fireflyCss.highlightColor();
        }

    }

    private static void setExpandedZoom(WebPlotView plotView, float level, boolean isFullScreen) {
        if (plotView.containsAttributeKey(WebPlot.MAX_EXPANDED_ZOOM_LEVEL)) {
            Object maxZ = plotView.getAttribute(WebPlot.MAX_EXPANDED_ZOOM_LEVEL);
            if (maxZ instanceof Number) {
                level = ((Number) maxZ).floatValue();
            }
        }
        if (level!=plotView.getPrimaryPlot().getZoomFact()) {
            plotView.setZoomTo(level, isFullScreen, false);
        }
        else {
            plotView.getPrimaryPlot().refreshWidget();
        }
    }


    private static void setExpandZoomByArcsecPerScreenPix(MiniPlotWidget mpwNew, float arcsecPerPix) {
        WebPlotView plotView = mpwNew.getPlotView();
        WebPlot p = plotView.getPrimaryPlot();

        if (plotView.containsAttributeKey(WebPlot.MAX_EXPANDED_ZOOM_LEVEL)) {
            float zfact = (float) p.getImagePixelScaleInArcSec() / arcsecPerPix;
            Object maxZ = plotView.getAttribute(WebPlot.MAX_EXPANDED_ZOOM_LEVEL);
            if (maxZ instanceof Number) {
                float level = ((Number) maxZ).floatValue();
                if (level > zfact) {
                    plotView.setZoomTo(level, true, false);
                } else {
                    plotView.setZoomByArcsecPerScreenPix(arcsecPerPix, true,false);
                }
            }
        } else {
            plotView.setZoomByArcsecPerScreenPix(arcsecPerPix, true,false);
        }

    }

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
