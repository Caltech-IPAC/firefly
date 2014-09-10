package edu.caltech.ipac.firefly.visualize.ui;
/**
 * User: roby
 * Date: 8/21/14
 * Time: 9:20 AM
 */


import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.util.Dimension;
import edu.caltech.ipac.firefly.visualize.AllPlots;
import edu.caltech.ipac.firefly.visualize.MiniPlotWidget;
import edu.caltech.ipac.firefly.visualize.graph.XYPlotWidget;

import java.util.List;
import java.util.Map;

/**
 * @author Trey Roby
 */
public class AutoGridRenderer implements GridRenderer {

    private static final int GRID_RESIZE_DELAY= 500;
    private MyGridLayoutPanel grid= new MyGridLayoutPanel();
    private List<String> showMask= null;
    private Map<String,MiniPlotWidget> mpwMap= null;
    private List<XYPlotWidget> xyList= null;
    private Dimension dimension= null;


    public  void reinitGrid(Map<String,MiniPlotWidget> mpwMap, List<XYPlotWidget> xyList) {
        this.mpwMap= mpwMap;
        this.xyList= xyList;
        grid.clear();
        if (mpwMap==null && xyList==null) return;
        int mpwSize= showMask==null ? mpwMap.size() : showMask.size();
        int size = mpwSize + xyList.size();
        if (size > 0) {
            int rows = 1;
            int cols = 1;
            if (size >= 7) {
                rows = size / 4 + (size % 4);
                cols = 4;
            } else if (size == 5 || size == 6) {
                rows = 2;
                cols = 3;
            } else if (size == 4) {
                rows = 2;
                cols = 2;
            } else if (size == 3) {
                rows = 2;
                cols = 2;
            } else if (size == 2) {
                rows = 1;
                cols = 2;
            }
            Widget p= grid.getParent();
            int w = p.getOffsetWidth() / cols;
            int h = p.getOffsetHeight() / rows;

            grid.resize(rows, cols);
            grid.setCellPadding(2);

            int col = 0;
            int row = 0;
            for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()) {
                if (showMask==null || showMask.contains(entry.getKey())) {
                    MiniPlotWidget mpw= entry.getValue();
                    grid.setWidget(row, col, mpw);
                    mpw.setPixelSize(w, h);
                    mpw.onResize();
                    col = (col < cols - 1) ? col + 1 : 0;
                    if (col == 0) row++;
                }
            }

            for (XYPlotWidget xy : xyList) {
                grid.setWidget(row, col, xy);
                xy.setPixelSize(w, h);
                xy.onResize();
                col = (col < cols - 1) ? col + 1 : 0;
                if (col == 0) row++;
            }
            AllPlots.getInstance().updateUISelectedLook();
        }
    }

    public void clear() {
        grid.clear();
    }

    public void onResize() {
        grid.onResize();
    }

    public Widget getWidget() {
        return grid;
    }

    public void setShowMask(List<String> showMask) {
        if (this.showMask==null || !this.showMask.equals(showMask)) {
            this.showMask= showMask;
            reinitGrid(mpwMap,xyList);
        }
    }

    public void postPlotting() {}

    private Dimension getGridDimension() {
        final int margin = 4;
        final int panelMargin =14;
        Widget p= grid.getParent();
        if (!GwtUtil.isOnDisplay(p)) return null;
        int rows= grid.getRowCount();
        int cols= grid.getColumnCount();
        if (rows==0 || cols==0) return null;
        int w= (p.getOffsetWidth() -panelMargin)/cols -margin;
        int h= (p.getOffsetHeight()-panelMargin)/rows -margin;
        return new Dimension(w,h);
    }



    private class MyGridLayoutPanel extends Grid implements RequiresResize {
        private GridResizeTimer _gridResizeTimer= new GridResizeTimer();
        public void onResize() {
            Dimension dim= getGridDimension();
            if (dim==null) return;
            int w= dim.getWidth();
            int h= dim.getHeight();
            this.setPixelSize(w,h);
            for(Map.Entry<String,MiniPlotWidget> entry : mpwMap.entrySet()) {
                if (showMask==null || showMask.contains(entry.getKey())) {
                    MiniPlotWidget mpw= entry.getValue();
                    mpw.setPixelSize(w, h);
                    mpw.onResize();
                }
            }
            for (XYPlotWidget xy : xyList) {
                xy.setPixelSize(w, h);
                xy.onResize();
            }
            _gridResizeTimer.cancel();
            _gridResizeTimer.setupCall(w,h, true);
            _gridResizeTimer.schedule(GRID_RESIZE_DELAY);
        }
    }

    public void setDimension(Dimension dim) {
        this.dimension= dim;
    }

    private class GridResizeTimer extends Timer {
        private int w= 0;
        private int h= 0;
        private boolean adjustZoom;

        public void setupCall(int w, int h, boolean adjustZoom) {
            this.w= w;
            this.h= h;
            this.adjustZoom = adjustZoom;
        }

        @Override
        public void run() {
            //todo: should I adjust zoom?
//            _behavior.onGridResize(_expandedList, new Dimension(w,h), adjustZoom);
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
