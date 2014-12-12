package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.PlotContainer;
import edu.caltech.ipac.visualize.plot.PlotPaintEvent;
import edu.caltech.ipac.visualize.plot.PlotPaintListener;
import edu.caltech.ipac.visualize.plot.PlotViewStatusEvent;
import edu.caltech.ipac.visualize.plot.PlotViewStatusListener;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * This class maintains the location and state of a ScalableObject 
 * and controls repairs.
 *
 * @author Trey Roby
 * @version $Id: ScalableObjectPosition.java,v 1.7 2009/07/23 16:29:21 roby Exp $
 *
 */
public class ScalableObjectPosition implements PlotPaintListener, 
                                               PlotViewStatusListener {

   private ScalableObject _viewObject; 
   private boolean        _showing= true;
   private Map<Plot,PlotInfo>  _plotMap= new HashMap<Plot,PlotInfo>(20);

   /**
    * Constructor
    * @param viewObject  the ScalableObject to manage.
    */
   public ScalableObjectPosition(ScalableObject viewObject) {
      _viewObject= viewObject;
   }


   /**
    * Get the current ScalableObject  
    * @return ScalableObject  the ScalableObject
    */
   public ScalableObject getScalableObject() {
      return _viewObject;
   }

   /**
    * Set the position of a scalable object on a plot.
    * @param p set the position on this plot
    * @param ra the ra in J2000
    * @param dec the dec in J2000
    */
   public void setPosition(Plot p, double ra, double dec) {
      PlotInfo pInfo= _plotMap.get(p);
      Assert.tst(pInfo);
      pInfo._pt= new WorldPt(ra,dec); 
      pInfo._positionSet= true;
   }

    public WorldPt getPosition(Plot p) {
        PlotInfo pInfo= _plotMap.get(p);
        Assert.tst(pInfo);
        return pInfo._pt;
    }

   /**
    * Set the position of a scalable object all plots.
    * @param ra the ra in J2000
    * @param dec the dec in J2000
    */
   public void setPosition(double ra, double dec) {
      for(PlotInfo pInfo: _plotMap.values()) {
          pInfo._pt= new WorldPt(ra,dec);
          pInfo._pt2= null;
          pInfo._positionSet= true;
      }
   }

   public void setScanPosition(WorldPt wpt, WorldPt wpt2) {
      for(PlotInfo pInfo: _plotMap.values()) {
          pInfo._pt= wpt;
          pInfo._pt2= wpt2;
          pInfo._positionSet= true;
      }
   }

   /**
    * Set the rotation of a scalable object on a plot.
    * @param p set the rotation on this plot
    * @param rotation the rotation angle
    */
   public void setRotation(Plot p, ScalableObject.RotationInfo rotation) {
      PlotInfo pInfo= _plotMap.get(p);
      Assert.tst(pInfo);
      pInfo._rotation= rotation;
   }

   /**
    * Set the rotation of a scalable object all plots.
    * @param rotation the rotation angle
    */
   public void setRotation(ScalableObject.RotationInfo rotation) {
      for(PlotInfo pInfo: _plotMap.values()) {
          pInfo._rotation= rotation;
      }
   }

   /**
    * Set the offset of a scalable object on a plot.
    * @param p set the rotation on this plot
    * @param  offset
    */
   public void setOffset(Plot p, WorldPt offset) {
      PlotInfo pInfo= _plotMap.get(p);
      Assert.tst(pInfo);
      pInfo._offset= offset;
   }

   /**
    * Set the offset on all the plots.
    */
   public void setOffset(WorldPt offset) {
      for(PlotInfo pInfo: _plotMap.values()) {
          pInfo._offset= offset;
      }
   }

   /**
    * Draw the ScalablObject on a plot with the current state.
    * @param p the plot to draw on
    * @param g2
    */
   public void drawOnPlot(Plot p, Graphics2D g2) {
      PlotInfo pInfo= _plotMap.get(p);
      ScalableObject.DrawOnPlotReturn soReturn;
      Assert.tst(pInfo);
      if (pInfo._showing && _showing && pInfo._positionSet) {
         soReturn= _viewObject.drawOnPlot(p, g2, pInfo._pt, pInfo._pt2,
                                              pInfo._rotation, pInfo._offset,
                                              null,true);
         pInfo._repairArea= soReturn.getRepairArea();
      }
   }


   /**
    * repair (redraw) the plot
    */
   public void repair(Plot p) {
      PlotInfo pInfo= _plotMap.get(p);
      Assert.tst(pInfo);
      p.repair(pInfo._repairArea);
   }

   /**
    * repair (redraw) all the plots this ScalableObject is drawn on
    */
   public void repair() {
      Plot plot;
      PlotInfo pInfo;
      for(Map.Entry<Plot,PlotInfo> entry: _plotMap.entrySet()) {
          plot= (Plot)entry.getKey();
          pInfo= (PlotInfo)entry.getValue();
          plot.repair(pInfo._repairArea);
      }
   }

   /**
    * enable/disable this ScalableObject for a given plot
    * @param p the plot enable/disable on
    * @param enabled true to enable, false to disable
    */
   public void setEnabledForPlot(Plot p, boolean enabled) { 
      PlotInfo pInfo= _plotMap.get(p);
      Assert.tst(pInfo);
      pInfo._showing= enabled;
   }

   /**
    * is this ScalableObject enabled/disabled for a given plot
    * @param p the plot in question
    * @return boolean true if enabled, false if disabled
    */
   public boolean isEnabledForPlot(Plot p) { 
      PlotInfo pInfo= _plotMap.get(p);
      Assert.tst(pInfo);
      return pInfo._showing;
   }

   /**
    * Enable this ScalableObject 
    */
   public void setEnable(boolean enabled) { _showing= enabled;}
   /**
    * is this ScalableObject enabled
    * @return boolean true if enabled, false if disabled
    */
   public boolean isEnable() { return _showing;}

   public void addPlotView(PlotContainer container) {
       Plot p;
       Iterator  j= container.iterator();
       while(j.hasNext()) {
          p= (Plot)j.next();
          addPlot(p);
       }
       container.addPlotViewStatusListener( this);
       container.addPlotPaintListener(this);
   }

   public void removePlotView(PlotContainer container) {
       Plot p;
       Iterator  j= container.iterator();
       while(j.hasNext()) {
          p= (Plot)j.next();
          removePlot(p);
       }
       container.removePlotViewStatusListener( this);
       container.removePlotPaintListener(this);
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
//------------------------- Private Methods -------------------------
//===================================================================
   /**
    * add a plot to draw on
    * @param p the plot to add
    */
   private void addPlot(Plot p) {
        _plotMap.put(p, new PlotInfo());
   }

   /**
    * remove a plot to draw on
    * @param p the plot to remove
    */
   private void removePlot(Plot p) {
       PlotInfo pInfo= _plotMap.get(p);
       _plotMap.remove(p);
   }
//===================================================================
//------------------------- Private Inner class ---------------------
//===================================================================

    /**
     * Store information about the plot
     */
    private static class PlotInfo {
        public WorldPt       _pt= null;
        public WorldPt       _pt2= null;
        public WorldPt       _offset= null;
        public ScalableObject.RotationInfo _rotation= null;
        public Rectangle          _repairArea= null;
        public boolean            _positionSet= false;
        public boolean            _showing    = true;
        PlotInfo( ) {
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
