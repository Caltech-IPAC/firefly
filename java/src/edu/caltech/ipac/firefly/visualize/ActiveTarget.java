package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.targetgui.net.Resolver;
import edu.caltech.ipac.util.ComparisonUtil;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Feb 13, 2009
 * Time: 10:51:57 AM
 */


/**
 * @author Trey Roby
 */
public class ActiveTarget {


    private static ActiveTarget _instance= null;
    private final List<PosEntry> _entries= new ArrayList<PosEntry>(30);
    private PosEntry _active;
    private float _radiusInDeg;
    private WorldPt wpCorners[]= null;

//======================================================================
//----------------------- Constructors ---------------------------------
//======================================================================
    private ActiveTarget() {}

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public static ActiveTarget getInstance() {
        if (_instance==null) _instance= new ActiveTarget();
        return _instance;
    }

    public String getName() {
        return (_active!=null) ? _active.getName() : null;
    }

    public Resolver getResolver() {
        return (_active!=null) ? _active.getResolver() : Resolver.NONE;
    }

    public WorldPt getPos() {
        return (_active!=null) ? _active.getPt() : null;
    }

    public boolean isComputed() {
        return (_active!=null) ? _active.isComputed() : true;
    }

    public PosEntry getActive() { return _active; }

    public void setActive(String name,
                          WorldPt pt,
                          Resolver resolver,
                          boolean computed) {
        _active= new PosEntry(pt, name, resolver, computed);
        if (_entries.contains(_active)) _entries.remove(_active);
        _entries.add(_active);
    }

    public void setImageCorners(WorldPt wp1, WorldPt wp2, WorldPt wp3, WorldPt wp4) {
        wpCorners= new WorldPt[] { wp1, wp2, wp3, wp4};
    }

    public WorldPt[] getImageCorners() {
        return wpCorners;
    }

    public void setActive(WorldPt pt) { setActive(null,pt,Resolver.NONE,true); }

    public int getHistorySize() { return _entries.size(); }
    public PosEntry getHistoryElement(int i) { return _entries.get(i); }


    /**
     * set the radius in degrees
     * @param radisuInDeg the radius in degree units
     */
    public void setRadius(float radisuInDeg) { _radiusInDeg= radisuInDeg; }
    /**
     * get the radius in degrees
     * @return the radius in degree units
     */
    public float getRadius() { return _radiusInDeg; }


// =====================================================================
// -------------------- Inner Classes --------------------------------
// =====================================================================

    public static class PosEntry {
        private final WorldPt _pt;
        private final String _name;
        private final boolean _computed;
        private final Resolver _resolver;

        public PosEntry(WorldPt pt, boolean computed) {
            this(pt,null, Resolver.NONE,computed);
        }

        public PosEntry(WorldPt pt, String name, Resolver resolver, boolean computed) {
            _pt= pt;
            _name= name;
            _computed= computed;
            _resolver= resolver;
        }

        public WorldPt getPt() { return _pt; }
        public String getName() { return _name; }
        public boolean  isComputed() { return _computed; }
        public Resolver getResolver() { return _resolver; }

        public boolean equals(Object other) {
            boolean retval= false;
            if (other==this) {
                retval= true;
            }
            else if (other!=null && other instanceof PosEntry) {
                PosEntry pe= (PosEntry)other;
                retval= ComparisonUtil.equals(pe._pt,this._pt);
            }
            return retval;
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
 * A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312- 2313) 
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
