/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.draw;

import edu.caltech.ipac.astro.CoordException;
import edu.caltech.ipac.astro.target.TargetUtil;
import edu.caltech.ipac.table.DataGroup;
import edu.caltech.ipac.util.Assert;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

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
    private String[]     _targetName = {"" };
    private transient FixedObjectDrawer _drawer;

    protected FixedObject(WorldPt pt, DataGroup dataGroup) {
       setPosition(pt);
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

    public void setShowName(boolean show)    {
        _showName= show;
    }


    public void setShowPoint(boolean show)    {
        _showPt = show;
    }

    public void setTargetName(String tname)  {
       _targetName= new String[1];
       _targetName[0]= tname;
    }

    public void setPosition(WorldPt pt) {
        _position= pt;
    }

    public WorldPt getPosition()   { return _position;   }

    public void setImagePt(ImagePt ipt) {
        _imagePt= ipt;
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

    public String toString() {
        return "Name:            " + getTargetName() + "\n" +
               "User coords:    (" + computeStringPostion(_position) + ")\n" +
               "Decimal coords: (" + _position.getLon() + "," +
               _position.getLat() + ")";
    }



    public boolean isWorldCoordSys() {
        return _usesWorldCoordSys;
    }

// -------------------------------------------------------------------
// ==================  Private / Protected Methods   =================
// -------------------------------------------------------------------


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
