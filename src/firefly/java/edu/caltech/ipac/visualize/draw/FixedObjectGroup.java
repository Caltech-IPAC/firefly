/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.astro.target.TargetUtil;
import edu.caltech.ipac.util.AppProperties;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.util.ServerStringUtil;
import edu.caltech.ipac.util.TableConnectionList;
import edu.caltech.ipac.util.action.ClassProperties;
import edu.caltech.ipac.visualize.VisConstants;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.NewPlotNotificationEvent;
import edu.caltech.ipac.visualize.plot.NewPlotNotificationListener;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.PlotContainer;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.awt.Color;
import java.awt.Graphics2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
public class FixedObjectGroup implements TableConnectionList,
                                         PropertyChangeListener,
                                         Serializable,
                                         Iterable<FixedObject> {




   private final static ClassProperties _prop= new ClassProperties(
                                                    FixedObjectGroup.class);

//===================================================================
//---------- private Constants for the table column name ------------
//===================================================================
    private enum Direction {LON,LAT}

   private final String ENABLED_COL     = _prop.getColumnName("on");
   private final String HILIGHT_COL     = _prop.getColumnName("hilight");
   private final String SHAPE_COL       = _prop.getColumnName("shape");
   private final String SHOW_NAME_COL   = _prop.getColumnName("showName");
   private final String TNAME_COL       = _prop.getColumnName("targetName");
   protected final String USER_RA_COL     = _prop.getColumnName("userRa");
   protected final String USER_DEC_COL    = _prop.getColumnName("userDec");
   private final String USER_LON_COL    = _prop.getColumnName("userLon");
   private final String USER_LAT_COL    = _prop.getColumnName("userLat");

//====================================================================
//---------- public Constants for the table column index ------------
//====================================================================

    public enum ParseInstruction {NONE,SEARCH_HMS_J2000,SEARCH_HMS_B1950}

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

    static public final String BULK_UPDATE    = "bulkUpdate";
    static public final String SELECTED_COUNT = "selectedCount";
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
   private ArrayList<FixedObject> _objects= new ArrayList<FixedObject>(200);
   private int            _selectedCount;
   private boolean        _doingBulkUpdates  = false;
   private DataGroup      _extraData;
   private transient List<PlotInfo> _plots= null;
   private transient PropertyChangeSupport _propChange;
   private boolean        _showPosInDecimal= AppProperties.getBooleanProperty(
                                     VisConstants.COORD_DEC_PROP, false);
   private String         _csysDesc= AppProperties.getProperty(
                                           VisConstants.COORD_SYS_PROP,
                                           CoordinateSys.EQ_J2000_STR);

   private int         _extraDataColumnRemap[]= null;

    static {
        _nf.setMaximumFractionDigits(5);
        _nf.setMinimumFractionDigits(5);
    }

   public FixedObjectGroup() {
       this(true,null,null);
   }

    public FixedObjectGroup(boolean usesWorldCoordSys) {
        this(usesWorldCoordSys,null,null);
    }

    public FixedObjectGroup(String title, DataGroup extraData) {
        this(true,title,extraData);
    }

   public FixedObjectGroup(boolean usesWorldCoordSys, String title, DataGroup extraData) {
       init(title,usesWorldCoordSys,extraData,null);
   }

    public FixedObjectGroup(DataGroup dataGroup) throws ColumnException {
        this(dataGroup,null,null,null);
    }


    public FixedObjectGroup (DataGroup dataGroup,
                             int  tnameIdx,
                             int  raIdx,
                             int  decIdx) throws NumberFormatException,
                                                 IllegalArgumentException {
        constructHelper(dataGroup, tnameIdx, raIdx, decIdx);
    }

    public FixedObjectGroup (DataGroup dataGroup,
                             String  targetNameOptions[],
                             String  raNameOptions[],
                             String  decNameOptions[])
                                  throws NumberFormatException,
                                         IllegalArgumentException,
                                         ColumnException {
        this(dataGroup,targetNameOptions,makeParseGroupList(raNameOptions,decNameOptions));
    }

    private static List<ParseGroup> makeParseGroupList(String  raNameOptions[],
                                                       String  decNameOptions[]) {
        List<ParseGroup> pgList= new ArrayList<ParseGroup>(1);
        if (raNameOptions==null) {
            raNameOptions= DEFAULT_RA_NAME_OPTIONS;
        }
        if (decNameOptions==null) {
            decNameOptions= DEFAULT_DEC_NAME_OPTIONS;
        }
        pgList.add(new ParseGroup(raNameOptions, decNameOptions,
                                      ParseInstruction.NONE));
        return pgList;
    }


    public FixedObjectGroup (DataGroup dataGroup,
                             String  targetNameOptions[],
                             List<ParseGroup> passedPGList )
                                          throws NumberFormatException,
                                                 IllegalArgumentException,
                                                  ColumnException {

        List<ParseGroup> pgList;

        if (passedPGList!=null) {
            pgList= passedPGList;
        }
        else {
            pgList= new ArrayList<ParseGroup>(1);
            pgList.add(new ParseGroup(DEFAULT_RA_NAME_OPTIONS,
                                      DEFAULT_DEC_NAME_OPTIONS,
                                      ParseInstruction.NONE));
        }

        if (targetNameOptions==null) {
            targetNameOptions= DEFAULT_TNAME_OPTIONS;
        }

        int tnameIdx= -1;
        int raIdx= -1;
        int decIdx= -1;
        DataType[] originalDataDef= dataGroup.getDataDefinitions();


        for(int i=0; i<originalDataDef.length; i++) {
            Class classType= originalDataDef[i].getDataType();
            String key= originalDataDef[i].getKeyName();
            String lKey= null;
            if (key!=null) lKey= key.toLowerCase();

            if (tnameIdx == -1 &&
                (classType==String.class) &&
                matchesList(lKey,targetNameOptions)) {
                tnameIdx= i;
            }
            else if (raIdx == -1 && (classType==Double.class || classType==Float.class)) {
                ParseInstruction pi= matchesRAList(lKey,pgList);
                if (pi!=null) raIdx= i;
            }
            else if (decIdx == -1 && (classType==Double.class || classType==Float.class)) {
                ParseInstruction pi= matchesDecList(lKey,pgList);
                if (pi!=null) decIdx= i;
            }
        }

        if (raIdx==-1 && decIdx==-1 && passedPGList!=null) {
            addHMSColumnsIfPosible(pgList, dataGroup);
            originalDataDef= dataGroup.getDataDefinitions();
            for(int i=0; i<originalDataDef.length; i++) {
                String key= originalDataDef[i].getKeyName();

                if (key.equals("_ra")) raIdx= i;
                else  if (key.equals("_dec")) decIdx= i;


            }

        }

        if (raIdx == -1) {
            throw new ColumnException(
                            "Could not convert to FixedObjectGroup.  "+
                            "There is not a field that can be converted to a RA",
                            ColumnException.ColumnType.RA,
                            tnameIdx, raIdx, decIdx);
        }
        if (decIdx == -1) {
            throw new ColumnException(
                         "Could not convert to FixedObjectGroup.  "+
                         "There is not a field that can be converted to a Dec",
                         ColumnException.ColumnType.DEC,
                         tnameIdx, raIdx, decIdx);
        }

        constructHelper(dataGroup, tnameIdx, raIdx, decIdx);
    }


    void addHMSColumnsIfPosible(List<ParseGroup> pgList,
                                DataGroup dataGroup)  throws  ColumnException {
        DataType[] originalDataDef= dataGroup.getDataDefinitions();
        int raIdx= -1;
        int decIdx= -1;
        int len= dataGroup.size();
        DataType raDataType= new DataType("_ra","RA", Double.class);
        DataType decDataType= new DataType("_dec","Dec", Double.class);
        ParseInstruction parseInstruction= ParseInstruction.SEARCH_HMS_J2000;

        CoordinateSys convertTarget= CoordinateSys.EQ_J2000;
        for(int i=0; i<originalDataDef.length; i++) {
            Class classType= originalDataDef[i].getDataType();
            String key= originalDataDef[i].getKeyName();

            if (raIdx == -1 && (classType==String.class)) {
                ParseInstruction pi= matchesRAList(key,pgList);
                if (pi!=null) {
                    try {
                        TargetUtil.convertStringToLon((String)dataGroup.get(0).getDataElement(originalDataDef[i]),
                                                      convertTarget);
                        dataGroup.addDataDefinition(raDataType);
                        double lon;
                        for(int j= 0; (j<len); j++) {
                            lon= TargetUtil.convertStringToLon((String)dataGroup.get(j).getDataElement(originalDataDef[i]),
                                                               convertTarget);
                            dataGroup.get(j).setDataElement(raDataType,lon);
                        }
                        raIdx= i;
                        parseInstruction= pi;
                    } catch (CoordException e) {
                        // not nothing - not found
                    }
                }
            }
            else if (decIdx == -1 && (classType==String.class)) {
                ParseInstruction pi= matchesDecList(key,pgList);
                if (pi!=null) {

                    try {
                        TargetUtil.convertStringToLat((String)dataGroup.get(0).getDataElement(originalDataDef[i]),
                                                      CoordinateSys.EQ_J2000);
                        dataGroup.addDataDefinition(decDataType);
                        double lat;
                        for(int j= 0; (j<len); j++) {
                            lat= TargetUtil.convertStringToLat((String)dataGroup.get(j).getDataElement(originalDataDef[i]),
                                                               CoordinateSys.EQ_J2000);
                            dataGroup.get(j).setDataElement(decDataType,lat);
                        }

                        decIdx= i;
                        parseInstruction= pi;
                    } catch (CoordException e) {
                        // not nothing - not found
                    }
                }
            }
        }
        if (raIdx == -1) {
            throw new ColumnException( "Could not convert to FixedObjectGroup.  "+
                                       "There is not a field that can be converted to a RA",
                                       ColumnException.ColumnType.RA,
                                       -1, raIdx, decIdx);
        }
        if (decIdx == -1) {
            throw new ColumnException( "Could not convert to FixedObjectGroup.  "+
                                       "There is not a field that can be converted to a Dec",
                                       ColumnException.ColumnType.DEC,
                                       -1, raIdx, decIdx);
        }



        if (parseInstruction==ParseInstruction.SEARCH_HMS_B1950) {
            DataObject data;
            for(int j= 0; (j<len); j++) {
                data= dataGroup.get(j);
                double ra= (Double)data.getDataElement(raDataType);
                double dec= (Double)data.getDataElement(decDataType);
                WorldPt wp= new WorldPt(ra,dec,CoordinateSys.EQ_B1950);
//                Position p= new Position(ra,dec,
//                                edu.caltech.ipac.astro.target.CoordinateSys.EQ_B1950);
                wp= Plot.convert(wp,convertTarget);
                data.setDataElement(raDataType, wp.getLon());
                data.setDataElement(decDataType, wp.getLat());
            }
        }

    }


    private void constructHelper(DataGroup dataGroup,
                                 int tnameIdx,
                                 int raIdx,
                                 int decIdx) throws NumberFormatException {

        DataType[] originalDataDef= dataGroup.getDataDefinitions();

        int outIdx=0;
        int extraDataRemapAry[]= new int[originalDataDef.length];
        Arrays.fill(extraDataRemapAry,-1);

        for(int i=0; i<originalDataDef.length; i++) {
            if (i == tnameIdx || i== raIdx || i==decIdx) {
                originalDataDef[i].setImportance(DataType.Importance.IGNORE);
            }
            else {
                extraDataRemapAry[outIdx++]= i;
            }
        }

        init (dataGroup.getTitle(), true, dataGroup, extraDataRemapAry);

        Iterator i= dataGroup.iterator();
        DataObject element;
        FixedObject fixedObj;
        beginBulkUpdate();
        _objects.ensureCapacity(dataGroup.size());
        while(i.hasNext()) {
            element= (DataObject)i.next();
            fixedObj= makeFixedObject(element, tnameIdx, raIdx, decIdx);
            add(fixedObj);
        }
        endBulkUpdate();
    }


    DataType getExtraDataElement(int i) {
        int remap= i;
        if (_extraDataColumnRemap!=null)  {
            remap= _extraDataColumnRemap[i];
        }
        DataType  retval= null;
        if (remap>-1) retval= _extraData.getDataDefinitions()[remap];
        return retval;
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

    public void beginBulkUpdate() { _doingBulkUpdates = true; }

    public void endBulkUpdate()   {
        if (_doingBulkUpdates) {
            getPropChange().firePropertyChange ( BULK_UPDATE, null, this);
            doRepair();
        }
        _doingBulkUpdates= false;
    }

    /**
     * Set the current object
     * @param current  the new current object
     */
    public void setCurrent(FixedObject current) {
        _current= current;
    }

    /**
     * Return the current object
     * @return FixedObject  the current object
     */
    public FixedObject getCurrent() { return _current; }


    public DataGroup getExtraData() { return _extraData; }

    public void setAllShapes(SkyShape shape) {
         if (size() > 0) {
            beginBulkUpdate();
            for (FixedObject  fixedObj: _objects) {
                fixedObj.getDrawer().setSkyShape(shape);
            }
            endBulkUpdate();
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
         beginBulkUpdate();
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
         endBulkUpdate();
    }

    public void add(FixedObject s) {
       _objects.add(s);
       s.addPropertyChangeListener(this);
       CoordinateSys csys= CoordinateSys.parse(_csysDesc);
       s.setCoordinateSys(csys);
       computeAllTransformsForObject(s);
       if (!_doingBulkUpdates) {
           getPropChange().firePropertyChange ( ADD, null, this);
       }
    }

    public void remove(FixedObject s) {
        Assert.tst(_objects.contains(s));
        s.removePropertyChangeListener(this);

        FixedObject newFo= null;
        int line= indexOf(s);
        if ( (line+1) < size()) newFo= get(line+1);

        _objects.remove(s);
        if (!_doingBulkUpdates) {
            getPropChange().firePropertyChange ( REMOVE, s, newFo);
        }
    }

    public void clear() {
       FixedObject s;
       for(Iterator<FixedObject> i= _objects.iterator(); (i.hasNext()); ) {
          s= i.next();
          i.remove();
          s.removePropertyChangeListener(this);
       }
       getPropChange().firePropertyChange ( ALL_ENTRIES_UPDATED, null, this);
    }


    public void doRepair() {
        Plot p;
        for(PlotInfo plotInfo: getPlots()) {
             p= plotInfo._p;
//             p.repair();
        }
    }


    public void drawOnPlot(Plot p, Graphics2D g2) {
       int idx= findPlot(p);
       PlotInfo pInfo= getPlots().get(idx);
       if (pInfo._show) {
           //System.out.println("drawOnPlot");
           for(FixedObject fixedObj: _objects) {
               if (fixedObj.isEnabled())
                      fixedObj.getDrawer().drawOnPlot(idx, g2);
           }
       }
    }

    public void addPlotView(PlotContainer container) {
       for(Plot p: container) addPlot(p);
    }

    public void removePlotView(PlotContainer container) {
       for(Plot p: container) removePlot(p);
    }



    public int getSelectedCount() {
          return _selectedCount;
    }

    public FixedObject get(int i) { return _objects.get(i); }



//======================================================================
//------------- Methods from PropertyChangeListener Interface ----------
//======================================================================

    public void propertyChange(PropertyChangeEvent ev) {
       String propName= ev.getPropertyName();
       if (!_doingBulkUpdates) {
           if (propName.equals(FixedObject.SELECTED)) {
               updateSelectedCount(
                              ((Boolean)ev.getNewValue()).booleanValue());
               FixedObject fixedObj= (FixedObject)ev.getSource();
           }
           else if (propName.equals(FixedObject.POSITION)) {
               FixedObject fixedObj= (FixedObject)ev.getSource();
               computeAllTransformsForObject(fixedObj);
               int idx= _objects.indexOf(fixedObj);
               Assert.tst(idx >= 0);
           }
           else if (propName.equals(FixedObject.SHOW_NAME)) {
               FixedObject fixedObj= (FixedObject)ev.getSource();
               if (!_doingBulkUpdates && fixedObj.isEnabled()) {
                   getPropChange().firePropertyChange ( ENTRY_UPDATED, null,
                                                    fixedObj);
               }
           }
           else if (propName.equals(FixedObject.ENABLED)) {
               FixedObject fixedObj= (FixedObject)ev.getSource();
               getPropChange().firePropertyChange ( ENTRY_UPDATED, null,
                                                fixedObj);
           }
       }
                              //=========================

       if (propName.equals(VisConstants.COORD_DEC_PROP)) {
            _showPosInDecimal= AppProperties.getBooleanProperty(
                               VisConstants.COORD_DEC_PROP,false);

           getPropChange().firePropertyChange ( ALL_ENTRIES_UPDATED, null, this);
       }
       else if (propName.equals(VisConstants.COORD_SYS_PROP)) {
            _csysDesc = AppProperties.getProperty(
                                  VisConstants.COORD_SYS_PROP,
                                  CoordinateSys.EQ_J2000_STR);
            updateCoordinateSystem();
           getPropChange().firePropertyChange ( ALL_ENTRIES_UPDATED, null, this);
       }
    }

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

    private void addPlot(Plot p) {
        NewPlotNotificationListener psl=
              new NewPlotNotificationListener() {
                  public void newPlot(NewPlotNotificationEvent e) {
                        computeImageTransform(e.getPlot());
                  }
              };
        p.addPlotStatusListener(psl);
        getPlots().add(new PlotInfo(p, psl) ); // needs to add to empty slot
        if (p.isPlotted()) computeImageTransform(p);
    }

    private void removePlot(Plot p) {
       Iterator<PlotInfo> i= getPlots().iterator();
       boolean found= false;
       PlotInfo plotInfo;
       for(; (i.hasNext() && !found); ) {
           plotInfo= i.next();
           if (p==plotInfo._p) {
               found= true;
               p.removePlotStatusListener(plotInfo._psl);
               i.remove();
           }
       }
    }



    private void updateSelectedCount(boolean newChange) {
         // this method also needs to work with bulk updates
         Integer oldCount= new Integer(_selectedCount);
         _selectedCount= newChange ? (_selectedCount+1) : (_selectedCount-1);
         getPropChange().firePropertyChange (SELECTED_COUNT, oldCount,
                                         new Integer(_selectedCount) );
    }

    private void updateCoordinateSystem() {
        CoordinateSys csys= CoordinateSys.parse(_csysDesc);
        Assert.tst(csys);
        for(FixedObject fixedObj: _objects) {
            fixedObj.setCoordinateSys(csys);
        }
        updateTitles();
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

    private int findPlot(Plot p) {
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

    protected void computeImageTransform(Plot p) {
          int idx= findPlot(p);
          for (FixedObject  fixedObj: _objects) {
              fixedObj.getDrawer().computeTransform(idx, p);
          }
    }


    private static ParseInstruction matchesRAList(String s, List<ParseGroup> pgList) {
        ParseInstruction pi= null;
        for(ParseGroup pg : pgList) {
            if (ServerStringUtil.matchesRegExpList(s, pg.getRaNameOptions(), true)) {
                pi= pg.getParseInstruction();
            }
        }
        return pi;
    }

    private static ParseInstruction matchesDecList(String s, List<ParseGroup> pgList) {
        ParseInstruction pi= null;
        for(ParseGroup pg : pgList) {
            if (ServerStringUtil.matchesRegExpList(s, pg.getDecNameOptions(), true)) {
                pi= pg.getParseInstruction();
            }
        }
        return pi;
    }

    private static boolean matchesList(String s, String regExpArray[]) {
        return ServerStringUtil.matchesRegExpList(s, regExpArray, true);
    }



    private void init(String title,
                      boolean   usesWorldCoordSys,
                      DataGroup extraData,
                      int      extraDataColumnRemap[]) {
        _title           = title;
        _extraDataColumnRemap= extraDataColumnRemap;
        _extraData= extraData;
        _usesWorldCoordSys= usesWorldCoordSys;
//        AppProperties.addPropertyChangeListener(this);
        if (extraData != null) {
            _numColumns= BASE_NUM_COLUMNS + getExtraUsedLength();
        }
        else {
            _numColumns = BASE_NUM_COLUMNS;
        }
        _colNames= new String[_numColumns];
        initColumnTitles();
        updateTitles();

        if (_extraData != null) {
            int realLength= getExtraUsedLength();
            for(int i= 0; (i<realLength); i++) {
                _colNames[BASE_NUM_COLUMNS+i]=
                               getExtraDataElement(i).getDefaultTitle();
            }
        }
    }

    private int getExtraUsedLength() {
        int realLength= 0;
        if (_extraDataColumnRemap==null) {
            realLength= _extraData.getDataDefinitions().length;
        }
        else {
            for(int i=0; i<_extraDataColumnRemap.length; i++) {
                if (_extraDataColumnRemap[i]!=-1) realLength++;
            }
        }
        return realLength;
    }

    private void initColumnTitles() {
        _colNames[ENABLED_IDX]   = ENABLED_COL;
        _colNames[HILIGHT_IDX]   = HILIGHT_COL;
        _colNames[SHOW_NAME_IDX] = SHOW_NAME_COL;
        _colNames[SHAPE_IDX]     = SHAPE_COL;
        _colNames[TNAME_IDX]     = TNAME_COL;
    }

    protected PropertyChangeSupport getPropChange() {
        if (_propChange==null)  {
            _propChange= new PropertyChangeSupport(this);
        }
        return _propChange;
    }



    protected List<PlotInfo> getPlots() {
        if (_plots==null)  {
            _plots= new ArrayList<PlotInfo>(20);
        }
        return _plots;
    }


//===================================================================
//------------------------- Factory Methods -------------------------
//===================================================================


    protected FixedObject makeFixedObject(DataObject da,
                                          int tnameIdx,
                                          int raIdx,
                                          int decIdx)
                                       throws NumberFormatException {
        return new FixedObject(da, tnameIdx, raIdx, decIdx);
    }

    public FixedObject makeFixedObject(WorldPt pt) {
        return new FixedObject(pt,_extraData);
    }

//===================================================================
//------------------------- Public Inner classes --------------------
//===================================================================


    public static class ParseGroup {
        private final String  _raNameOptions[];
        private final String  _decNameOptions[];
        private final FixedObjectGroup.ParseInstruction _parseInstruction;

        public ParseGroup(String  raNameOptions[],
                      String  decNameOptions[],
                      FixedObjectGroup.ParseInstruction parseInstruction ) {
            _raNameOptions= raNameOptions;
            _decNameOptions= decNameOptions;
            _parseInstruction= parseInstruction ;
        }

        public String[] getRaNameOptions() {
            return  _raNameOptions;
        }

        public String[] getDecNameOptions() {
            return  _decNameOptions;
        }

        public FixedObjectGroup.ParseInstruction getParseInstruction() {
            return _parseInstruction;
        }

    }

    /**
     *
     */
    private static class PlotInfo {
        public boolean _show= true;
        public Plot _p;
        public NewPlotNotificationListener _psl;
        PlotInfo( Plot p, NewPlotNotificationListener psl) {
           _p= p;
           _psl= psl;
        }
    }
}
