/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.ActiveFitsReadGroup;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.PlotContainer;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;
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
   private Map<ImagePlot,PlotInfo>  _plotMap= new HashMap<>(20);

   /**
    * Constructor
    * @param viewObject  the ScalableObject to manage.
    */
   public ScalableObjectPosition(ScalableObject viewObject) {
      _viewObject= viewObject;
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
   public void setRotation(ImagePlot p, ScalableObject.RotationInfo rotation) {
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
   public void drawOnPlot(ImagePlot p, ActiveFitsReadGroup frGroup, Graphics2D g2) {
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
    * Enable this ScalableObject 
    */
   public void setEnable(boolean enabled) { _showing= enabled;}
   /**
    * is this ScalableObject enabled
    * @return boolean true if enabled, false if disabled
    */
   public boolean isEnable() { return _showing;}

   public void addPlotView(PlotContainer container) {
       for (ImagePlot imagePlot : container) {
           addPlot(imagePlot);
       }
   }


//===================================================================
//------------------------- Private Methods -------------------------
//===================================================================
   /**
    * add a plot to draw on
    * @param p the plot to add
    */
   private void addPlot(ImagePlot p) {
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
