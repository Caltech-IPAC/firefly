/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.PlotContainer;
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
public class ScalableObjectPosition {

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
   public void drawOnPlot(Plot p, ActiveFitsReadGroup frGroup, Graphics2D g2) {
      PlotInfo pInfo= _plotMap.get(p);
      ScalableObject.DrawOnPlotReturn soReturn;
      Assert.tst(pInfo);
      if (pInfo._showing && _showing && pInfo._positionSet) {
         soReturn= _viewObject.drawOnPlot(p,frGroup, g2, pInfo._pt, pInfo._pt2,
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
