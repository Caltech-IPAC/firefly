/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;

import java.awt.Graphics2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Map;

/**
 * using the Grid class to draw grid on the Plot
 *
 * @version $Id: GridLayer.java,v 1.5 2009/03/03 21:45:39 roby Exp $
 * @author		Xiuqin Wu
**/
public class GridLayer  {
   public static final String COORD_SYSTEM         = "CoordSystem";

     /*  the coordinate system that user wants the Grid to be drawn 
	 It has to be set to the constants defined in Plot
      */
   private CoordinateSys         _csys;
   private Grid                  _grid;
   private Map<Plot,PlotInfo>    _plotMap   = new HashMap<Plot,PlotInfo>(20);
   private boolean               _showing   = true;
   private PropertyChangeSupport _propChange= new PropertyChangeSupport(this);

	/**
	 * Creates a new GridLayer.
	**/
   public GridLayer(CoordinateSys csys) {
       super();
       _grid = new Grid(csys);
       _csys = csys;
       _grid.setSexigesimalLabel(
                (_csys.equals(CoordinateSys.EQ_J2000) || _csys.equals(CoordinateSys.EQ_B1950)));
   }

   public GridLayer() { this(CoordinateSys.EQ_J2000); }


   public void setCoordSystem(String coordSys) {
      boolean sexigesimalLabel = false;

      if (coordSys.equals("equJ2000")) {
         _csys = CoordinateSys.EQ_J2000;
         sexigesimalLabel = true;
         }
      else if (coordSys.equals("equJ2000D"))
         _csys = CoordinateSys.EQ_J2000;
      else if (coordSys.equals("equB1950")) {
         _csys = CoordinateSys.EQ_B1950;
         sexigesimalLabel = true;
         }
      else if (coordSys.equals("equB1950D"))
         _csys = CoordinateSys.EQ_B1950;
      else if (coordSys.equals("eclJ2000"))
         _csys = CoordinateSys.ECL_J2000;
      else if (coordSys.equals("eclB1950"))
         _csys = CoordinateSys.ECL_B1950;
      else if (coordSys.equals("gal"))
         _csys = CoordinateSys.GALACTIC;
      else if (coordSys.equals("superG"))
         _csys = CoordinateSys.SUPERGALACTIC;
      else
         Assert.tst(false, coordSys);

      _grid.setCoordSystem(_csys);
      _grid.setSexigesimalLabel(sexigesimalLabel);
      _propChange.firePropertyChange ( COORD_SYSTEM, null, coordSys);
   }

   public Grid getGrid() {
      return _grid;
   }

    public void drawOnPlot(Plot p, Graphics2D g2){
       if (_showing) _grid.paint(g2, p);
    }


    //=====================================================================
    //----------- add / remove property Change listener methods -----------
    //=====================================================================

    /**
     * Add a property changed listener.
     * @param p  the listener
     */
    public void addPropertyChangeListener (PropertyChangeListener p) {
       _propChange.addPropertyChangeListener (p);
    }

    /**
     * Remove a property changed listener.
     * @param p  the listener
     */
    public void removePropertyChangeListener (PropertyChangeListener p) {
       _propChange.removePropertyChangeListener (p);
    }


    //=====================================================================
    //----------- Private / Protected Methods -----------------------------
    //=====================================================================

    /**
     remember the relationship of plot and ppl so the ppl can be
     removed when the control panel for Grid is removed.
    */
    private void addPlot(Plot p) {
        _plotMap.put(p, new PlotInfo());
    }

    private void removePlot(Plot p) {
        PlotInfo pInfo= _plotMap.get(p);
        if (pInfo != null) {
	   _plotMap.remove(p);
        }
    }


//===================================================================
//------------------------- Private Inner classes -------------------
//===================================================================
 
    private static class PlotInfo {
       public int  _csys_int;
       PlotInfo( ) {
        }

    }
}



