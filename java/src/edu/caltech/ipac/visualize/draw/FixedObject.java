package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.astro.target.TargetUtil;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.util.DataGroup;
import edu.caltech.ipac.util.DataObject;
import edu.caltech.ipac.util.DataType;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.Plot;
import edu.caltech.ipac.visualize.plot.WorldPt;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.Pt;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;

/**
 * This class is component class for defining object on the sky.  One object is
 * defined per FixedObject instance.  List of FixedObjects are combined in
 * FixedObjectGroup.
 *   <i>This class need more documentation and needs some general clean up</i>
 *
 * @see FixedObjectGroup
 *
 * @author Trey Roby
 * @version $Id: FixedObject.java,v 1.9 2012/10/02 23:03:34 roby Exp $
 *
 */
public class FixedObject implements Serializable {

//======================================================================
//---------- public Constants for property change events ---------------
//======================================================================
    static public final String SELECTED  = "selected";
    static public final String SHOW_NAME = "showName";
    static public final String ENABLED   = "enabled";
    static public final String POSITION  = "position";




//======================================================================
//----------------------- private / protected variables ----------------
//======================================================================
    private boolean      _show        = true;
    private boolean      _hilighted   = false;
    private boolean      _selected    = false;
    private boolean      _showName    = false;
    private boolean      _showPt      = true;

    private boolean      _usesWorldCoordSys= true;
    private ImagePt      _imagePt= null;
    private WorldPt      _position;
    private CoordinateSys _defCSys= CoordinateSys.EQ_J2000;
//    private WorldPt      _j2000Position;
    private String       _targetName[] = {"" };
    private PropertyChangeSupport _propChange= new PropertyChangeSupport(this);
    private transient FixedObjectDrawer _drawer;
    private DataObject   _extraData;

    //protected FixedObject(WorldPt pt) { this(pt,null);}
    protected FixedObject(DataGroup dataGroup) { this((WorldPt)null,dataGroup);}

    protected FixedObject(WorldPt pt, DataGroup dataGroup) {
       setPosition(pt);
       _extraData= new DataObject(dataGroup);
    }


    //protected FixedObject(ImagePt pt) { this(pt,null);}

    protected FixedObject(ImagePt pt, DataGroup dataGroup) {
        _usesWorldCoordSys= false;
        _imagePt= pt;
        setPosition(null);
        _extraData= new DataObject(dataGroup);
    }

    protected FixedObject(DataObject da,
                          int tnameIdx,
                          int raIdx,
                          int decIdx) throws NumberFormatException {

        Assert.argTst(da!=null,
                      "A non-null DataObject is required for this call");
        DataType dataTypes[]= da.getDataDefinitions();
        double ra= getPosValue(raIdx, da);
        double dec= getPosValue(decIdx, da);
        if (tnameIdx==-1 || da.getDataElement(dataTypes[tnameIdx])==null) {
            setTargetName("");
        }
        else {
            setTargetName(da.getDataElement(dataTypes[tnameIdx]).toString());
        }
        setPosition(new WorldPt(ra, dec));
        _extraData= da;
    }

    public FixedObjectDrawer getDrawer() {
         if (_drawer==null) {
             _drawer= new FixedObjectDrawer(this);
         }
         return _drawer;
    }

    public boolean  isEnabled()         { return _show;       }
    public boolean  isHiLighted()       { return _hilighted;  }
    public boolean  isSelected()        { return _selected;   }
    public boolean  getShowName()       { return _showName;   }
    public boolean  getShowPoint()       { return _showPt;   }
    public String   getTargetName()     {
       String retval= null;
       if (_targetName != null && _targetName.length > 0) {
          retval= _targetName[0];
       }
       return retval;
    }
    public String[] getFullTargetName() { return _targetName; }

    public void setEnabled(boolean show) {
        Boolean oldShow= new Boolean(_show);
        _show= show;
        _propChange.firePropertyChange ( ENABLED, oldShow, new Boolean(_show));
    }
    public void setHiLighted(boolean hilighted) { _hilighted= hilighted; }

    public void setSelected(boolean select) {
        Boolean oldSelected= _selected;
        _selected= select;
        _propChange.firePropertyChange ( SELECTED, oldSelected, Boolean.valueOf(_selected) );
    }

    void setSelectedWithNoEvent(boolean select) {
        _selected= select;
    }

    public void setShowName(boolean show)    {
        Boolean oldShow= _showName;
        _showName= show;
       _propChange.firePropertyChange ( SHOW_NAME, oldShow, Boolean.valueOf(_showName) );
    }


    public void setShowPoint(boolean show)    {
        Boolean oldShow= show;
        _showPt = show;
        _propChange.firePropertyChange ( SHOW_NAME, oldShow, Boolean.valueOf(_showName) );
    }



    public void setTargetName(String tname)  {
       _targetName= new String[1];
       _targetName[0]= tname;
    }
    public void setTargetName(String tname[])  {
       _targetName= new String[tname.length];
       System.arraycopy(tname,0,_targetName,0, tname.length);
    }

    public void setCoordinateSys(CoordinateSys csys) {
        _defCSys= csys;
    }


    public void setPosition(WorldPt pt) {
        WorldPt _oldPosition= _position;
        _position= pt;
//        if (pt!=null) {
//            _j2000Position= Plot.convert(pt, CoordinateSys.EQ_J2000) ;
//        }
//        else {
//            _j2000Position= null;
//        }
        _propChange.firePropertyChange(POSITION, _oldPosition, _position);
    }

    public WorldPt getPosition()   { return _position;   }

    public void setImagePt(ImagePt ipt) {
        ImagePt _oldPt= _imagePt;
        _imagePt= ipt;
        _propChange.firePropertyChange(POSITION, _oldPt, ipt);
    }


    public ImagePt getImagePt() { return _imagePt; }

    public Pt getPoint() {
        return _usesWorldCoordSys ? getPosition() : getImagePt();
    }

    public void setPoint(Pt pt) {
        if (pt instanceof ImagePt) {
            setImagePt((ImagePt)pt);

        }
        else if (pt instanceof WorldPt) {
            setPosition((WorldPt)pt);
        }
        else {
            Assert.argTst(true, "Only ImagePt or WorldPt is allowed.");
        }

    }

    /**
     * if this FixedObject support world coords then return the
     * lon else return the image x
     */
    public double getX() {
        return (_usesWorldCoordSys) ? _position.getLon() : _imagePt.getX();
    }

    /**
     * if this FixedObject support world coords then return the
     * lat else return the image y
     */
    public double getY() {
        return (_usesWorldCoordSys) ? _position.getLat() : _imagePt.getY();
    }


//    public void setEqJ2000Position(WorldPt pt) {
//        Assert.tst(pt.getCoordSys() == CoordinateSys.EQ_J2000);
//        _j2000Position= pt;
//        syncToJ2000Position();
//    }

    public WorldPt getEqJ2000Position() { return Plot.convert(_position, CoordinateSys.EQ_J2000); }

//    public void setJ2000Ra( double ra) {
//        if (_j2000Position== null) {
//            _j2000Position= new WorldPt(ra, 0.0);
//        }
//        else {
//            _j2000Position= new WorldPt(ra, _j2000Position.getLat() );
//        }
//        syncToJ2000Position();
//        computeStringPostion(_position);
//    }
//
//    public void setJ2000Dec( double dec) {
//        if (_j2000Position== null) {
//            _j2000Position= new WorldPt(0.0, dec);
//        }
//        else {
//            _j2000Position= new WorldPt(_j2000Position.getLon(), dec );
//        }
//        syncToJ2000Position();
//        computeStringPostion(_position);
//    }


    public String toString() {
        return "Name:            " + getTargetName() + "\n" +
               "User coords:    (" + computeStringPostion(_position) + ")\n" +
               "Decimal coords: (" + _position.getLon() + "," +
               _position.getLat() + ")";
    }



    public void setExtraData(DataType fdt, Object fde) {
        Assert.argTst(_extraData!=null,
                      "You did not create this FixedObject to suport ExtraData");
        _extraData.setDataElement(fdt, fde);
    }

    public Object getExtraData(String name) {
        Assert.argTst(_extraData!=null,
                      "You did not create this FixedObject to suport ExtraData");
       return _extraData.getDataElement(name);
    }

    public Object getExtraData(DataType fdt) {
        Assert.argTst(_extraData!=null,
                      "You did not create this FixedObject to suport ExtraData");
        return _extraData.getDataElement(fdt);
    }

    public int getExtraElementCount() {
        Assert.argTst(_extraData!=null,
                      "You did not create this FixedObject to suport ExtraData");
        return _extraData.size();
    }

    public DataType[] getDataDefinitions() {
        Assert.argTst(_extraData!=null,
                      "You did not create this FixedObject to suport ExtraData");
        return _extraData.getDataDefinitions();
    }




    public boolean isWorldCoordSys() {
        return _usesWorldCoordSys;
    }


//=====================================================================
//----------- add / remove PropertyChangeListener methods -----------
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

// -------------------------------------------------------------------
// ==================  Private / Protected Methods   =================
// -------------------------------------------------------------------



    private double getPosValue(int idx, DataObject da) {
        DataType dataTypes[]= da.getDataDefinitions();
        Object v= da.getDataElement(dataTypes[idx]);
        double retval;
        if (v instanceof Number) {
            retval= ((Number)v).doubleValue();
        }
        else {
            throw new NumberFormatException("cannot convert "+
                   da.getDataDefinitions()[idx].getDefaultTitle() + " of type "+
                   v.getClass().getName() + " to a double");
        }
        return retval;
    }

//    private void syncToJ2000Position() {
//        WorldPt oldPosition= _position;
//        if (_position == null) {
//            _position= _j2000Position;
//        }
//        else {
//            _position= Plot.convert(_j2000Position, _position.getCoordSys() ) ;
//        }
//        _propChange.firePropertyChange(POSITION, oldPosition, _position);
//    }


    private static String computeStringPostion(WorldPt pt) {
        String userRa;
        String userDec;
        try {
            userRa= TargetUtil.convertLonToString(pt.getLon(),
                                                   pt.getCoordSys().isEquatorial());
        } catch (CoordException e) {
            userRa= pt.getLon() + "";
        }
        try {
            userDec= TargetUtil.convertLatToString(pt.getLat(),
                                                    pt.getCoordSys().isEquatorial());
        } catch (CoordException e) {
            userDec= pt.getLat() + "";
        }
        return userRa + "," + userDec;
    }
}
