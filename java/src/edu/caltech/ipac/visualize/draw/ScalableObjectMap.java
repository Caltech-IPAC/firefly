package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.PlotPaintComplexListener;
import edu.caltech.ipac.visualize.plot.PlotPaintEvent;
import edu.caltech.ipac.visualize.plot.PlotView;
import edu.caltech.ipac.visualize.plot.PlotViewStatusEvent;
import edu.caltech.ipac.visualize.plot.PlotViewStatusListener;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * This class maintains a set of points to draw a ScalableObject 
 * and controls repairs.
 *
 * @author Trey Roby
 * @version $Id: ScalableObjectMap.java,v 1.8 2012/10/11 21:52:56 roby Exp $
 *
 */
public class ScalableObjectMap implements PlotPaintComplexListener, 
                                          PlotViewStatusListener {

   private ScalableObject              _viewObject; 
   private boolean                     _showing        = true;
   private Map<Plot,PlotInfo>          _plotMap        = new HashMap<Plot,PlotInfo>(20);
   private MapPt                       _defaultMap[]   = null;
   private ScalableObject.RotationInfo _defaultRotation= null;
   public  WorldPt                     _defaultOffset  = null;
   public  boolean                     _scanMap        = false;

   /**
    * Constructor
    * @param viewObject  the ScalableObject to manage.
    */
   public ScalableObjectMap(ScalableObject viewObject) {
      this(viewObject,false);
   }

   /**
    * Constructor
    * @param viewObject  the ScalableObject to manage.
    */
   public ScalableObjectMap(ScalableObject viewObject, boolean scanMap) {
      _viewObject= viewObject;
      _scanMap   = scanMap;
   }

   /**
    * Get the current ScalableObject  
    * @return ScalableObject  the ScalableObject
    */
   public ScalableObject getScalableObject() {
      return _viewObject;
   }

   /**
    * Set the map to draw
    * @param pts an array of world points that define the map
    */
   public void setDefaultMap(WorldPt pts[]) {
      if (_scanMap) {
          Assert.tst((_scanMap && pts.length==2),
              "This ScalableObjectMap is setup to be a scan map. " + 
              "You must pass "+
              "an array to two points: " + "points passed= "+pts.length);
      }
      _defaultMap= new MapPt[pts.length];
      System.arraycopy(pts,0,_defaultMap,0,pts.length);
      resetCacheMaps();
   }

   /**
    * Set the map to draw
    * @param pts a list of WorldPt that define the map
    */
   public void setDefaultMap(List<WorldPt> pts) {
      if (_scanMap) {
         Assert.tst((_scanMap && pts.size()==2),
             "This ScalableObjectMap is setup to be a scan map. You must pass "+
             "an array to two points: " + "points passed= "+pts.size());
      }
      _defaultMap= new MapPt[pts.size()]; 
      Iterator<WorldPt> j= pts.iterator();
      for(int i= 0; (i<_defaultMap.length); i++) {
             _defaultMap[i]= new MapPt(j.next());
      }
      resetCacheMaps();
   }

   public void setWorldFocalPlane(ShapeInfo worldFocalPlane[]) {
      _viewObject.setWorldFocalPlane(worldFocalPlane);
      resetCacheMaps();
   }


   /**
    * Rotation of each shape in the map
    * @param rotation  the rotation at a point
    */
   public void setDefaultRotation(ScalableObject.RotationInfo rotation) {
      _defaultRotation= rotation;
   }


   /**
    * Offset of each shape in the map
    * @param offset the offset
    */
   public void setDefaultOffset(WorldPt offset) {
      _defaultOffset= offset;
   }

   /**
    * Enabled/disable all the parts of a maps at a position.
    */
   public void setPointsAtLocationEnabled(WorldPt pt, boolean enabled) {
         MapPt map[]= _defaultMap;
         for(int i=0; (i<map.length); i++) {
              if (map[i].pt.equals(pt)) {
                   map[i].enabled= enabled;
              } 
         } // end loop
   }

   /**
    * Enabled/disable all the parts at this reference
    */
   public void setPointRefEnabled(WorldPt pt, boolean enabled) {
         MapPt map[]= _defaultMap;
         boolean found= false;
         for(int i=0; (i<map.length && !found); i++) {
              if (map[i].pt == pt) {
                   map[i].enabled= enabled;
                   found= true;
              } 
         } // end loop
   }

    public boolean isOnPlot(Plot p, Graphics2D g2) {
        ImagePlot ip= (ImagePlot)p;
        p.getPlotGroup().beginPainting(g2);
        ImagePlot.SaveG2Stuff ss=ImagePlot.prePaint(g2, ip.getFitsRead().getImageScaleFactor(),ip.getPercentOpaque());
        boolean remove= false;
        if (!_plotMap.containsKey(p)) {
            remove= true;
            addPlot(p);
            _viewObject.addPlot(p);
        }
        boolean retval= drawOnPlot(p,g2,false);
        if (remove) {
            removePlot(p);
            _viewObject.removePlot(p);
        }
        ImagePlot.postPaint(ss);
        return retval;
    }

   /**
    * Draw the ScalablObject on a plot with the current state.
    * @param p the plot to draw on
    * @param g2 the Graphics2D
    */
   public boolean drawOnPlot(Plot p, Graphics2D g2, boolean enableDraw) {
      PlotInfo pInfo= _plotMap.get(p);
      Assert.tst(pInfo);
      boolean insideClip= false;
      if (_showing) {
         ScalableObject.RotationInfo rotation= _defaultRotation ;
         WorldPt offset  = _defaultOffset;
         MapPt   map[]   = _defaultMap;
         ScalableObject.DrawOnPlotReturn soReturn;
         Assert.tst( pInfo._defaultMapCache       !=null &&
                     pInfo._defaultMapCache.length== map.length);
         if (_scanMap) {
              if (map[0].enabled) {
                 soReturn= _viewObject.drawOnPlot(p, g2, 
                                                  map[0].pt, map[1].pt, 
                                                  rotation, offset,
                                                  pInfo._defaultMapCache[0],
                                                  enableDraw );
                 if (soReturn._insideClip) insideClip= true;
                 pInfo._repairArea        = soReturn.getRepairArea();
                 pInfo._defaultMapCache[0]= soReturn.getCachePtInfo();
              }
         }
         else {
            for(int i=0; (i<map.length); i++) {
                 if (map[i].enabled) {
                     soReturn= _viewObject.drawOnPlot(p, g2, map[i].pt, 
                                                   rotation, offset, 
                                                   pInfo._defaultMapCache[i],
                                                   enableDraw);
                     if (soReturn._insideClip) insideClip= true;
                     pInfo._repairArea        = soReturn.getRepairArea();
                     pInfo._defaultMapCache[i]= soReturn.getCachePtInfo();
                 }
            } // end loop
         }
      } // end if
       return insideClip;
   }


   /**
    * repair (redraw) all the plots this ScalableObject is drawn on
    */
   public void repair() {
      Set<Map.Entry<Plot,PlotInfo>> plots= _plotMap.entrySet();
      Plot plot;
      PlotInfo pInfo;
      for(Map.Entry<Plot,PlotInfo> entry: plots) {
          plot= entry.getKey();
          pInfo= entry.getValue();
          plot.repair(pInfo._repairArea);
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
    * Enable this ScalableObject map
    */
   public void setEnable(boolean enabled) { _showing= enabled;}
   /**
    * is this ScalableObject map enabled
    * @return boolean true if enabled, false if disabled
    */
   public boolean isEnable() { return _showing;}


    public void addPlotView(PlotView pv) {
       for(Plot p: pv) {
          addPlot(p);
       }
       pv.addPlotViewStatusListener( this);
       pv.addPlotPaintListener(this);
    }

    public void removePlotView(PlotView pv) {
       for(Plot p: pv) {
          removePlot(p);
       }
       pv.removePlotViewStatusListener( this);
       pv.removePlotPaintListener(this);
    }

  //===================================================================
  //------------------------- Private Methods -------------------------
  //===================================================================
   /**
    * add a plot to draw on
    * @param p the plot to add
    */
   private void addPlot(Plot p) {
        PlotInfo pInfo= new PlotInfo();
        if (_defaultMap != null) {
            pInfo._defaultMapCache= 
                   new ScalableObject.CachePtInfo[_defaultMap.length];
        }
        _plotMap.put(p, pInfo);
   }

   /**
    * remove a plot to draw on
    * @param p the plot to remove
    */
   private void removePlot(Plot p) {
       PlotInfo pInfo= _plotMap.get(p);
       Assert.tst(pInfo);
       _plotMap.remove(p);
   }

   private void resetCacheMaps() {
      PlotInfo pInfo;
      Iterator i= _plotMap.entrySet().iterator();
      Map.Entry entry;

      while(i.hasNext()) {
          entry= (Map.Entry)i.next();
          pInfo= (PlotInfo)entry.getValue();
          pInfo._defaultMapCache= 
                   new ScalableObject.CachePtInfo[_defaultMap.length];
      }
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
         drawOnPlot( ev.getPlot(), ev.getGraphics(), true );
    }

//===================================================================
//------------------------- Private Inner class ---------------------
//===================================================================

    /**
     * Store information about the plot
     */
    private static class PlotInfo {
        public Rectangle                    _repairArea= null;
        public ScalableObject.CachePtInfo[] _defaultMapCache;
        PlotInfo( ) {
        }
    }

    /**
     * A Point on the map.  A given point map be enabled or disabled.
     */
    private static class MapPt {
        public WorldPt pt;
        public boolean enabled= true;
        public MapPt(WorldPt pt) { this.pt= pt;}
    }
}
