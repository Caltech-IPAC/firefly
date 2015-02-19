/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.astro.net.Resolver;
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
        if (wp1==null || wp2==null || wp2==null || wp4==null) {
            throw new IllegalArgumentException("Can't set corner, null parameter");
        }
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

