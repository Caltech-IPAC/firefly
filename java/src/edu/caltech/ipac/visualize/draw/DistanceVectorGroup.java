package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.astro.CoordUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.VisConstants;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.PlotView;
import edu.caltech.ipac.visualize.plot.WorldPt;

import javax.swing.table.AbstractTableModel;
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * This class is the data class for any set of objects that we show on
 * plots.  <i>This class need more documentation.</i>
 * 
 * @see FixedGroupDialog
 * @see FixedObject
 *
 * @author Trey Roby
 * @version $Id: DistanceVectorGroup.java,v 1.9 2010/04/20 21:42:47 roby Exp $
 *
 */
public class DistanceVectorGroup extends   AbstractTableModel 
                                implements PropertyChangeListener,
                                           VectorDataListener {

   private final static ClassProperties _prop= new ClassProperties(
                                                  DistanceVectorGroup.class);

//===================================================================
//---------- private Constants for the table column name ------------
//===================================================================

   private static final String ENABLED_COL= _prop.getColumnName("on");
   private static final String POS1_COL   = _prop.getColumnName("pos1");
   private static final String POS2_COL   = _prop.getColumnName("pos2");
   private static final String DIST_COL   = _prop.getColumnName("distance");
   //private static final String ANGLE_COL  = _prop.getColumnName("angle");
   private static final String RA_LABEL   = _prop.getName("pos.ra");
   private static final String DEC_LABEL  = _prop.getName("pos.dec");

//===================================================================
//---------- private Constants for properties            ------------
//===================================================================

   //private static final String ALL_ENABLED  = "ALL_ENABLED";

//====================================================================
//---------- public Constants for the table column index ------------
//====================================================================
   public static final int ENABLED_IDX    = 0;
   public static final int POS1_IDX       = 1;
   public static final int POS2_IDX       = 2;
   public static final int DIST_IDX       = 3;
   //public static final int ANGLE_IDX      = 4;

   public static final int NUM_COLUMNS    = 4;

//====================================================================
//--------------------- more public Constants ------------------------
//====================================================================
   public static final int UNIT_DEGREE    = 0;
   public static final int UNIT_ARCSEC    = 1;
   public static final int UNIT_ARCMIN    = 2;

//======================================================================
//----------------------- private / protected variables ----------------
//======================================================================

   private String         _colNames[];
   private List<PlotView>  _plotViews  = new ArrayList<PlotView>(50);
   private List<TableObject> _objects= new ArrayList<TableObject>(10);
   private NumberFormat   _nf= NumberFormat.getInstance();// OK for i18n
   private PropertyChangeSupport _propChange= new PropertyChangeSupport(this);

   private boolean  _showPosInDecimal= AppProperties.getBooleanPreference(
                                     VisConstants.COORD_DEC_PROP, false);
   private String   _csysDesc= AppProperties.getProperty(
                                           VisConstants.COORD_SYS_PROP,
                                           CoordinateSys.EQ_J2000_STR);
   private Color   _defColor;
   private int     _distanceUnitType= UNIT_DEGREE;


   public DistanceVectorGroup() {
       _colNames             = new String[NUM_COLUMNS];
       _colNames[ENABLED_IDX]= ENABLED_COL;
       _colNames[POS1_IDX]   = POS1_COL;
       _colNames[POS2_IDX]   = POS2_COL;
       _colNames[DIST_IDX]   = DIST_COL;
       //_colNames[ANGLE_IDX]  = ANGLE_COL;
       _nf.setMaximumFractionDigits(3);
       _nf.setMinimumFractionDigits(3);
       AppProperties.addPropertyChangeListener(this);
   }
 
    /**
     * Return an iterator for all the VectorObject in this group
     * @return Iterator  the iterator
     */
    public Iterator iterator() {
              // create a list that actually has VectorObjects in it 
              // and then make a iterator from that.
        int len= _objects.size();
        List<VectorObject> list= new ArrayList<VectorObject>(len);
        for(TableObject entry: _objects) {
                list.add( entry._vector );
        }
        return Collections.unmodifiableList(list).iterator();
    }

    public void add(VectorObject v) {
       TableObject t= new TableObject(v);
       _objects.add(t);
       fireTableDataChanged();
    }

    public void remove(VectorObject v) {
       int entry= findVectorEntry(v);
       Assert.tst(entry != -1);
       _objects.remove(entry);
       fireTableDataChanged();
       cleanUpVector(v);
    }

    public void remove(int indexes[]) {
       TableObject t[]= new TableObject[indexes.length];
       int i;
       for(i=0; (i<t.length); i++) {
          t[i]= _objects.get(indexes[i]);
       }
       for(i=0; (i<t.length); i++) {
          _objects.remove(t[i]);
          cleanUpVector(t[i]._vector);
       }
       fireTableDataChanged();
    }

    public void cleanUpVector(VectorObject v) {
       v.removeAllPlots();

    }

    public void clear() {
       VectorObject v;
       for(Iterator<TableObject> i= _objects.iterator(); (i.hasNext()); ) {
          v= i.next()._vector;
          i.remove();
          v.removeAllPlots();
       }
       fireTableDataChanged();
    }


    public VectorObject makeVector(LineShape line, WorldPt pts[]) {
       line.setColor(_defColor);
       VectorObject v= new VectorObject(line, pts);
       v.getStringShape().setColor(_defColor);
       CoordinateSys csys= CoordinateSys.parse(_csysDesc);
       v.setCoordinateSys(csys);
       for(PlotView pv: _plotViews) {
          v.addPlotView(pv);
       }
       add(v);
       v.addVectorDataListener(this);
       return v;
    }




    public void addPlotView(PlotView pv) {
        _plotViews.add(pv);
        for(TableObject entry: _objects) {
            entry._vector.addPlotView(pv);
        }
    }
    public void removePlotView(PlotView pv) {
        _plotViews.remove(pv);
        for(TableObject entry: _objects) {
            entry._vector.removePlotView(pv);
        }
    }

    public void setAllEnabled(boolean enable) {
        for(TableObject entry: _objects) {
            entry._vector.setEnabled(enable);
        }
        fireTableDataChanged();
    }

    public VectorObject get(int i) {
       return ((TableObject)_objects.get(i))._vector;
    }
 
    public void setInfo(VectorObject v, double dist, boolean showOnVect) {
       int row= findVectorEntry(v);
       Assert.tst((row>-1), "entry should never be -1");
       TableObject entry= _objects.get(row);
       entry._distance= dist;
       if (showOnVect) {
          setInfoOnVect(v,dist);
       }
    }


/*
    public void setLabels(VectorObject v, String labels[]) {
       int row= findVectorEntry(v);
       Assert.tst((row>-1), "entry should never be -1");
       TableObject entry= (TableObject)_objects.get(row); 
       entry._vector.setLabelStrings(labels);
    }
*/


    public void setAllLineColor(Color c) {
        LineShape line;
        _defColor= c;
        for(TableObject entry: _objects) {
            line= entry._vector.getLineShape();
            if (line != null) line.setColor(c);
        }
    }

    public void setAllTextColor(Color c) {
        StringShape stringShape;
        for(TableObject entry: _objects) {
            stringShape= entry._vector.getStringShape();
            stringShape.setColor(c);
            if (stringShape != null) stringShape.setColor(c);
        }
    }

    public void setDistanceUnits(int unit) {
        Assert.tst( unit == UNIT_DEGREE ||
                    unit == UNIT_ARCSEC ||
                    unit == UNIT_ARCMIN);
        _distanceUnitType= unit;
        for(TableObject entry: _objects) {
            setInfoOnVect(entry._vector,entry._distance);
        }
        fireTableDataChanged();
    }

    public String formatDistance(double dist) {
        double multiplier= 1.0;
        switch (_distanceUnitType) {
             case UNIT_DEGREE : multiplier= 1.0;
                                _nf.setMaximumFractionDigits(3);
                                _nf.setMinimumFractionDigits(3);
                                break;
             case UNIT_ARCSEC : multiplier= VisConstants.DEG_TO_ARCSEC;
                                _nf.setMaximumFractionDigits(2);
                                _nf.setMinimumFractionDigits(2);
                                break;
             case UNIT_ARCMIN : multiplier= VisConstants.DEG_TO_ARCMIN;
                                _nf.setMaximumFractionDigits(3);
                                _nf.setMinimumFractionDigits(3);
                                break;
             default :          Assert.stop();
                                break;
         }
         return _nf.format(dist * multiplier);
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

//======================================================================
//------------- Methods from PropertyChangeListener Interface ----------
//======================================================================

    public void propertyChange(PropertyChangeEvent ev) {
       String propName= ev.getPropertyName();
       if (propName.equals(VisConstants.COORD_DEC_PROP)) {
            _showPosInDecimal= AppProperties.getBooleanPreference(
                               VisConstants.COORD_DEC_PROP,false);
            fireTableDataChanged();
       }
       else if (propName.equals(VisConstants.COORD_SYS_PROP)) {
            _csysDesc = AppProperties.getPreference(
                                  VisConstants.COORD_SYS_PROP,
                                  CoordinateSys.EQ_J2000_STR);
            updateCoordinateSystem();
            fireTableDataChanged();
       }
    }

//======================================================================
//------------- Methods from VectorDataListener Interface ----------
//======================================================================

     public void dataChanged(VectorDataEvent ev) { // needs work
         int row= findVectorEntry( ev.getVectorObject() );
         Assert.tst(row > -1);
         fireTableRowsUpdated(row,row);
     }

//-------------------------------------------------------------------
//-------------------------------------------------------------------
//===================== Methods from AbstractTableModel =============
//-------------------------------------------------------------------
//-------------------------------------------------------------------

    public int getRowCount() {
        return _objects.size();
    }
    public int getColumnCount() {
        return NUM_COLUMNS;
    }


    public String getColumnName(int idx) {
        return _colNames[idx];
    }

    public boolean isCellEditable(int row, int column) {
        boolean retval= false;
        if (column == ENABLED_IDX) {
                retval= true;
        }
        return retval;
    }

    public Object getValueAt(int row, int column) {
         WorldPt        wpt;
         Object         retval= null; 
         TableObject    entry= _objects.get(row);
         VectorObject   vector= entry._vector;
         MultiLineValue mentry[];
         Assert.tst(vector);
        //System.out.printf("Updating: row: %d, col: %d%n", row,column);
         switch (column) {
            case  ENABLED_IDX:
                       retval= new Boolean(vector.isEnabled());
                       break;
            case  POS1_IDX:
                       wpt= vector.getWorldPt(0);
                       mentry=  new MultiLineValue[2];
                       mentry[0]= new MultiLineValue(RA_LABEL, 
                                   formatLon(wpt.getLon(),_showPosInDecimal) );
                       mentry[1]= new MultiLineValue(DEC_LABEL, 
                                   formatLat(wpt.getLat(),_showPosInDecimal) );
                       retval= mentry;
                       break;
            case  POS2_IDX:
                       wpt= vector.getWorldPt(1);
                       mentry=  new MultiLineValue[2];
                       mentry[0]= new MultiLineValue(RA_LABEL, 
                                   formatLon(wpt.getLon(),_showPosInDecimal) );
                       mentry[1]= new MultiLineValue(DEC_LABEL, 
                                   formatLat(wpt.getLat(),_showPosInDecimal) );
                       retval= mentry;
                       break;
            case  DIST_IDX:
                       retval= formatDistance(entry._distance);
                       break;
            //case  ANGLE_IDX:
            //           retval= new Double(entry._angle);
            //           break;
            default :        
                       Assert.stop();
                       break;
       }
       return retval;
    }

    public void setValueAt(Object aValue, int row, int column) {
         TableObject  entry= _objects.get(row);
         VectorObject vector= entry._vector;
         Assert.tst(vector);
         switch (column) {
            case  ENABLED_IDX:
                       vector.setEnabled(
                                ((Boolean)aValue).booleanValue());
                       break;
            default :  
                       Assert.stop();
                       break;
       }
    }

//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================

    public void setInfoOnVect(VectorObject v, double dist) {
       String str[]= new String[1];
       //str[0]= "Dummy: xxx";
       //str[1]= formatDistance(dist);
       str[0]= formatDistance(dist);
       v.setLabelStrings(str);
    }

    public String formatLon(double lon, boolean inDecimal) {
        return formatPos(lon,false,inDecimal);// false: not latitude
    }

    public String formatLat(double lat, boolean inDecimal) {
        return formatPos(lat,true,inDecimal); // true: is latitude
    }

    private String formatPos(double x, boolean isLat, boolean inDecimal) {
       String retval= null;
       if (inDecimal) {
           _nf.setMaximumFractionDigits(3);
           _nf.setMinimumFractionDigits(3);
           retval= _nf.format(x);
       }
       else {
          try {
               //retval= new Coord(x, false, true).c_ddsex(ddsexValue);
               retval= CoordUtil.dd2sex(x, isLat, true);
          } catch (CoordException ce) {
               retval= "";
          }
       }
       return retval;
    }


    private int findVectorEntry(VectorObject v) {
        int retval= -1;
        int j=0;
        VectorObject vector;
        for(TableObject entry: _objects) {
            vector= entry._vector;
            if (v == vector) {
                retval= j;
                break;
            }
            j++;
        }
        return retval;
    }


//===================================================================
//------------------------- Factory Methods -------------------------
//===================================================================



//===================================================================
//------------------------- Private Inner classes -------------------
//===================================================================
 
    private void updateCoordinateSystem() {
        CoordinateSys csys= CoordinateSys.parse(_csysDesc);
        Assert.tst(csys);
        for(TableObject entry: _objects) {
            entry._vector.setCoordinateSys(csys);
        }
    }


    /**
     *
     */
    private static class TableObject {
        public VectorObject  _vector;
        public double        _distance;
        public double        _angle;
        TableObject( VectorObject vector) {
           _vector= vector;
           _distance= 0.0;
           _angle   = 0.0;
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
