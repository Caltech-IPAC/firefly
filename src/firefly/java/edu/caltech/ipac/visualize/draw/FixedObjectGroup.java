/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePlot;
import edu.caltech.ipac.visualize.plot.PlotContainer;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is the data class for any set of objects that we show on
 * plots.  <i>This class need more documentation.</i>
 * 
 * @see FixedObject
 *
 * @author Trey Roby
 * @version $Id: FixedObjectGroup.java,v 1.25 2010/09/01 18:27:43 roby Exp $
 *
 */
public class FixedObjectGroup implements Serializable, Iterable<FixedObject> {

//===================================================================
//---------- private Constants for the table column name ------------
//===================================================================
   private final String ENABLED_COL     = "On";
   private final String HILIGHT_COL     = "Hi.";
   private final String SHAPE_COL       = "Shape";
   private final String SHOW_NAME_COL   = "Show Name";
   private final String TNAME_COL       = "Tgt. Name";
   protected final String USER_RA_COL     = "RA";
   protected final String USER_DEC_COL    = "Dec";
   private final String USER_LON_COL    = "Lon";
   private final String USER_LAT_COL    = "Lat";



//====================================================================
//---------- public Constants for the table column index ------------
//====================================================================

   public static final int ENABLED_IDX     = 0;
   public static final int HILIGHT_IDX     = 1;
   public static final int SHAPE_IDX       = 2;
   public static final int TNAME_IDX       = 3;
   public static final int SHOW_NAME_IDX   = 4;
   public static final int USER_RA_IDX     = 5;
   public static final int USER_DEC_IDX    = 6;

   public static final int BASE_NUM_COLUMNS= 7;

//======================================================================
//---------- public Constants for property change events ---------------
//======================================================================

    static public final String ADD            = "add";
    static public final String REMOVE         = "remove";
//====================================================================
//---------- constants for the type of color we can set --------------
//====================================================================
   public static final int COLOR_TYPE_HIGHLIGHT= 45;
   public static final int COLOR_TYPE_STANDARD = 47;
   public static final int COLOR_TYPE_SELECTED = 48;


//====================================================================
//---------- defaults for imports - package accesss ------------------
//====================================================================

    protected final static String DEFAULT_TNAME_OPTIONS[]= {
                   ".*name.*",         // generic
                   ".*pscname.*",      // IRAS
                   ".*target.*",       // our own table output
                   ".*designation.*",  // 2MASS, WISE
                   ".*objid.*",        // SPITZER
                   ".*starid.*"        // PCRS
    };
    protected final static String DEFAULT_RA_NAME_OPTIONS[]= {".*ra.*"};
    protected final static String DEFAULT_DEC_NAME_OPTIONS[]= {".*dec.*"};

//======================================================================
//----------------------- Private / Protected variables ----------------
//======================================================================

    private static NumberFormat   _nf= NumberFormat.getInstance();// OK for i18n
    private boolean      _usesWorldCoordSys;
   private int            _numColumns;
   private String         _colNames[];
   private String         _title;
   private FixedObject    _current;
   private ArrayList<FixedObject> _objects= new ArrayList<>(200);
   private int            _selectedCount;
   private transient List<PlotInfo> _plots= null;
   private final String _csysDesc= CoordinateSys.EQ_J2000_STR;
    static {
        _nf.setMaximumFractionDigits(5);
        _nf.setMinimumFractionDigits(5);
    }

   public FixedObjectGroup() {
       this(true,null);
   }

   public FixedObjectGroup(boolean usesWorldCoordSys, String title) {
       init(title,usesWorldCoordSys);
   }

    public String getTitle() { return _title;  }
    public void setTitle(String title) { _title= title;  }

    /**
     * Return an iterator for all the objects in this group
     * @return Iterator  the iterator
     */
    public Iterator<FixedObject> iterator() {
        return _objects.iterator();
    }

    public boolean isWorldCoordSys() { return _usesWorldCoordSys; }


    public void setAllShapes(SkyShape shape) {
         if (size() > 0) {
            for (FixedObject  fixedObj: _objects) {
                fixedObj.getDrawer().setSkyShape(shape);
            }
         }
    }

    /**
     * Set the color for all the objects.
     * The are three type of colors highlight color, standard color,
     * and selected color.
     * @param colorType the color type.  Must be one of the constants:
     *    <code>COLOR_TYPE_HIGHLIGHT</code>,
     *    <code>COLOR_TYPE_STANDARD</code>,
     *    <code>COLOR_TYPE_SELECTED</code>
     * @param c the color to set
     */
    public void setAllColor(int colorType, Color c) {
          Assert.tst(colorType == COLOR_TYPE_HIGHLIGHT ||
                     colorType == COLOR_TYPE_STANDARD   ||
                     colorType == COLOR_TYPE_SELECTED);
         for (FixedObject  fixedObj: _objects) {
             switch (colorType) {
                  case COLOR_TYPE_HIGHLIGHT :
                                fixedObj.getDrawer().setHighLightColor(c);
                                break;
                  case COLOR_TYPE_STANDARD :
                                fixedObj.getDrawer().setStandardColor(c);
                                break;
                  case COLOR_TYPE_SELECTED :
                                fixedObj.getDrawer().setSelectedColor(c);
                                break;
                  default :
                                Assert.tst(false);
                                break;
             } // end switch
         } // end loop
    }

    public void add(FixedObject s) {
       _objects.add(s);
       computeAllTransformsForObject(s);
    }


    public void drawOnPlot(ImagePlot p, Graphics2D g2) {
       int idx= findPlot(p);
       PlotInfo pInfo= getPlots().get(idx);
       if (pInfo._show) {
           for(FixedObject fixedObj: _objects) {
               if (fixedObj.isEnabled())
                      fixedObj.getDrawer().drawOnPlot(idx, g2);
           }
       }
    }

    public void addPlotView(PlotContainer container) {
       for(ImagePlot p: container) addPlot(p);
    }

    public FixedObject get(int i) { return _objects.get(i); }



//======================================================================
//------------- Methods from PropertyChangeListener Interface ----------
//======================================================================


    public int indexOf(FixedObject fixedObj) {
        return indexOf((Object)fixedObj);
    }

    public int indexOf(Object object) {
        return _objects.indexOf((FixedObject)object);
    }

    public int size() { return _objects.size(); }
    public int getColumnCount() { return _numColumns; } //TODO: remove

    public String getColumnName(int idx) { return _colNames[idx]; } //TODO: remove


//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    private void addPlot(ImagePlot p) {
        getPlots().add(new PlotInfo(p) ); // needs to add to empty slot
        computeImageTransform(p);
    }



    private void updateTitles() {
       if (_csysDesc.equals(CoordinateSys.GALACTIC_STR) ||
           _csysDesc.equals(CoordinateSys.SUPERGALACTIC_STR) ) {
              _colNames[USER_RA_IDX]   = USER_LON_COL;
              _colNames[USER_DEC_IDX]  = USER_LAT_COL;
       }
       else {
              _colNames[USER_RA_IDX]   = USER_RA_COL;
              _colNames[USER_DEC_IDX]  = USER_DEC_COL;
       }
    }

    private int findPlot(ImagePlot p) {
       int retval= -1;
       Iterator<PlotInfo> i= getPlots().iterator();
       boolean found= false;
       PlotInfo plotInfo= null;
       PlotInfo retPlotInfo= null;
       for(; (i.hasNext() && !found); ) {
           plotInfo= i.next();
           if (p==plotInfo._p) {
               found= true;
               retPlotInfo= plotInfo;
           }
       }
       if (found) retval= getPlots().indexOf(retPlotInfo);
       //System.out.println("findPlot: found= " + found + "  retval= "+ retval);
       return retval;
    }

    protected void computeAllTransformsForObject(FixedObject fixedObj) {
          int length= getPlots().size();
          PlotInfo plotInfo;
          List<PlotInfo> plots= getPlots();
          for(int i=0; (i<length);i++ ) {
              plotInfo= plots.get(i);
              fixedObj.getDrawer().computeTransform(i, plotInfo._p);
          }
    }

    protected void computeImageTransform(ImagePlot p) {
          int idx= findPlot(p);
          for (FixedObject  fixedObj: _objects) {
              fixedObj.getDrawer().computeTransform(idx, p);
          }
    }


    private void init(String title, boolean usesWorldCoordSys) {
        _title           = title;
        _usesWorldCoordSys= usesWorldCoordSys;
        _numColumns = BASE_NUM_COLUMNS;
        _colNames= new String[_numColumns];
        initColumnTitles();
        updateTitles();

    }


    private void initColumnTitles() {
        _colNames[ENABLED_IDX]   = ENABLED_COL;
        _colNames[HILIGHT_IDX]   = HILIGHT_COL;
        _colNames[SHOW_NAME_IDX] = SHOW_NAME_COL;
        _colNames[SHAPE_IDX]     = SHAPE_COL;
        _colNames[TNAME_IDX]     = TNAME_COL;
    }


    protected List<PlotInfo> getPlots() {
        if (_plots==null)  _plots= new ArrayList<>();
        return _plots;
    }


//===================================================================
//------------------------- Factory Methods -------------------------
//===================================================================

    public FixedObject makeFixedObject(WorldPt pt) {
        return new FixedObject(pt,null);
    }

//===================================================================
//------------------------- Public Inner classes --------------------
//===================================================================


    /**
     *
     */
    private static class PlotInfo {
        public boolean _show= true;
        public ImagePlot _p;
        PlotInfo( ImagePlot p) {
           _p= p;
        }
    }
}
