package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.*;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class CircleObject implements ShapeObject {

    private WorldPt _center;
    private WorldPt _aPt;
    private Map<Plot,PlotInfo>  _plotMap    = new HashMap<Plot,PlotInfo>(20);
    private LineShape        _line;
    private StringShape      _stringShape= new StringShape(0,StringShape.SE);
    private boolean          _show   = true;
    private String           _labelStrings[];
    private CoordinateSys    _csys= CoordinateSys.EQ_J2000;



    public CircleObject(LineShape line, WorldPt center, WorldPt aPt) {
        _line= line;
        _center  = center;
        _aPt = aPt;
    }


    public CircleObject(WorldPt center, WorldPt aPt) {
        this(new LineShape(), center, aPt);
    }


    public LineShape   getLineShape()   { return _line; }
    public StringShape getStringShape() { return _stringShape; }


    public void setLabelStrings(String strs[]) {
        _labelStrings= new String[strs.length];
        System.arraycopy(strs,0,_labelStrings, 0,_labelStrings.length);
        doRepair();
    }

    public void setEnabled(boolean show){
        _show= show;
        doRepair();
    }

    public boolean  isEnabled()             { return _show;}

    public void setLineType(LineShape line) { _line= line; }

    public void drawOnPlot(Plot p, Graphics2D g2) {
        if (_show) {
            PlotInfo pInfo= _plotMap.get(p);
            Assert.tst(pInfo);
            _line.draw(g2, pInfo._circle);
            try {
                ImageWorkSpacePt iCenter = (pInfo._plot).getImageCoords(_center);
                _stringShape.draw(g2, iCenter, 0.0F, _labelStrings);
            } catch (ProjectionException e) {
                e.printStackTrace();
            }
        }
    }



    private void doRepair() {
        for(Plot p: _plotMap.keySet()) {
            p.repair();
        }
    }

    public void addPlotView(PlotContainer container) {
        for(Plot p : container) addPlot(p);
        container.addPlotViewStatusListener( this);
        container.addPlotPaintListener(this);
    }

    public void removePlotView(PlotContainer container) {
        for(Plot p : container) removePlot(p);
        container.removePlotViewStatusListener( this);
        container.removePlotPaintListener(this);
    }

    public void removeAllPlots() {
        Iterator j= _plotMap.entrySet().iterator();
        PlotInfo pInfo;
        Map.Entry entry;
        while( j.hasNext() ) {
            entry= (Map.Entry)j.next();
            pInfo= (PlotInfo)entry.getValue();
            j.remove();
            if (pInfo._plot.getPlotView()!=null) {
                pInfo._plot.getPlotView().removePlotPaintListener(this);
            }
            pInfo._plot.repair();
        }

    }

    public WorldPt getCenter() {
        return _center;
    }

    public WorldPt getPoint() {
        return _aPt;
    }

    public void setCenter(WorldPt center) {
        updateCircle(center, _aPt);
    }

    public void setPoint(WorldPt pt) {
        updateCircle(_center, pt);
    }

    public void setCoordinateSys(CoordinateSys csys) {
        if (_csys != csys) {
            _csys= csys;
            _center = Plot.convert(_center, csys);
            _aPt = Plot.convert(_aPt, csys);
        }
    }

    
    public Ellipse2D.Double getCircleShape(Plot plot) {
        PlotInfo pInfo = _plotMap.get(plot);
        if (pInfo == null)
            return (new PlotInfo(plot))._circle;
        else
            return pInfo._circle;
    }

    // ===================================================================
    // ------------------  Methods  from PlotViewStatusListener -----------
    // ===================================================================
    public void plotAdded(PlotViewStatusEvent ev) {
        addPlot(ev.getPlot());
    }
    public void plotRemoved(PlotViewStatusEvent ev) {
        removePlot(ev.getPlot());
    }

    // ===================================================================
    // ------------------  Methods  from PlotPaintListener ---------------
    // ===================================================================

    public void paint(PlotPaintEvent ev) {
        drawOnPlot( ev.getPlot(), ev.getGraphics() );
    }



    //===================================================================
    //------------------------- Private / Protected Methods -------------
    //===================================================================


    private void addPlot(Plot p) {
        PlotInfo pInfo= new PlotInfo(p);
        _plotMap.put(p, pInfo);
    }

    private void removePlot(Plot p) {
        PlotInfo pInfo= _plotMap.get(p);
        if (pInfo != null) {
            _plotMap.remove(p);
        }
    }




    private void updateCircle(WorldPt center, WorldPt aPt) {
        if (_center != center || _aPt != aPt) {
            _center = center;
            _aPt = aPt;

            PlotInfo pInfo;
            for(Map.Entry<Plot,PlotInfo> entry : _plotMap.entrySet()) {
                pInfo= entry.getValue();
                pInfo.updateCircleShape(center, aPt);
            }
        }
        doRepair();
    }


    //===================================================================
    //------------------------- Private Inner classes -------------------
    //===================================================================

    private class PlotInfo {
        Ellipse2D.Double     _circle;
        Plot                 _plot;

        PlotInfo( Plot p) {
            _plot= p;
            updateCircleShape(_center, _aPt);

        }

        public void updateCircleShape(WorldPt center, WorldPt aPt) {
            try {
                ImageWorkSpacePt iCenter= _plot.getImageCoords(center);
                ImageWorkSpacePt iPoint = _plot.getImageCoords(aPt);
                // radius in image coordinates
                double deltaX = iCenter.getX() - iPoint.getX();
                double deltaY = iCenter.getY() - iPoint.getY();
                double radius = Math.sqrt(deltaX*deltaX+deltaY*deltaY);
                // ellipse is defined by "upper left" corner  (smallest x, smallest y)
                if (_circle == null) {
                    _circle = new Ellipse2D.Double(iCenter.getX()-radius, iCenter.getY()-radius, radius*2, radius*2);
                } else {
                    _circle.x = iCenter.getX()-radius;
                    _circle.y = iCenter.getY()-radius;
                    _circle.width = radius*2;
                    _circle.height = _circle.width;
                }
            } catch (ProjectionException e) {
                _circle = null;
            }
        }
    }
}
