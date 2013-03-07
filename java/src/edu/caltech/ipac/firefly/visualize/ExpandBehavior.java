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

    private Map<PopoutWidget, Float> _oldZoomLevelMap = new HashMap<PopoutWidget, Float>(10);


    public PopoutWidget.PopoutChoice getPopoutWidgetList(PopoutWidget activatingPopout) {

        AllPlots allPlots = AllPlots.getInstance();
        List<PopoutWidget> all = new ArrayList<PopoutWidget>(allPlots.getAllPopouts().size());
        List<PopoutWidget> selected = new ArrayList<PopoutWidget>(allPlots.getAllPopouts().size());

        for (MiniPlotWidget mpw : allPlots.getAll()) {
            if (mpw.getCurrentPlot() != null && mpw.getCurrentPlot().isAlive()) {
                all.add(mpw);
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

        // handle expand only plots
        if (selected.size()==0 && !activatingPopout.isCollapsible() && activatingPopout instanceof MiniPlotWidget) {
            MiniPlotWidget mpw= (MiniPlotWidget)activatingPopout;
            selected.add(mpw);
            all.add(mpw);
        }

        return new PopoutWidget.PopoutChoice(selected, all);
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
            else if (!popout.isCollapsible()) { // handle expand only case
                _oldZoomLevelMap.put(popout, .00001F);
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
                if (_oldZoomLevelMap.containsKey(popout) && popout.isCollapsible()) {
                    plotView.setZoomTo(_oldZoomLevelMap.get(popout), false);
                }
                mpw.getGroup().setLastPoppedOut(null);
                AllPlots.getInstance().hideMenuBarPopup();
                mpw.updateUISelectedLook();
                mpw.setShowInlineTitle(false);
            }
            DeferredCommand.addCommand(new Command() {
                public void execute() {
                    plotView.smartCenter();
                }
            });
            plotView.setScrollBarsEnabled(mpw.getShowScrollBars() || expanded);

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


        if (mpwOld != null && mpwNew != null && oldGroup.contains(mpwNew)) {
            float oldArcsecPerPix = ZoomUtil.getArcSecPerPix(oldPlot);
            float newArcsecPerPix = ZoomUtil.getArcSecPerPix(newPlot);
            if (Math.abs(oldArcsecPerPix - newArcsecPerPix) > .01) {
                setExpandZoomByArcsecPerScreenPix(mpwNew, oldArcsecPerPix);
            }
        } else if (mpwNew != null) {
            MiniPlotWidget mpwLast = mpwNew.getGroup().getLastPoppedOut();
            if (mpwLast == null) {
                float zLevel = ZoomUtil.getEstimatedFullZoomFactor(newPlot, dim);
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
                    mpwNew.getPlotView().centerOnPoint(_pagingCenter);
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
                            PlotWidgetGroup pwg = mpw.getGroup();
                            VisUtil.FullType fullType = VisUtil.FullType.ONLY_WIDTH;
                            if (pwg!=null) {
                                fullType = pwg.getGridPopoutZoomType();
                            }
                            float zlevel = ZoomUtil.getEstimatedFullZoomFactor(mpw.getCurrentPlot(), dim, fullType, -1, 1);
                            setExpandedZoom(plotView, zlevel, true);
                            plotView.setScrollBarsEnabled(false);
                            mpw.setShowInlineTitle(true);
                        } else if (viewType == PopoutWidget.ViewType.ONE) {
                            float zLevel = ZoomUtil.getEstimatedFullZoomFactor(mpw.getCurrentPlot(),
                                                                               new Dimension(dim.getWidth() - 15, dim.getHeight()));
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
        plotView.setZoomTo(level, isFullScreen);
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
                    plotView.setZoomTo(level, true);
                } else {
                    plotView.setZoomByArcsecPerScreenPix(arcsecPerPix, true);
                }
            }
        } else {
            plotView.setZoomByArcsecPerScreenPix(arcsecPerPix, true);
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
