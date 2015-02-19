/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;
/**
 * User: roby
 * Date: Mar 4, 2009
 * Time: 10:34:19 AM
 */


import java.io.Serializable;

/**
 * @author Trey Roby
 */
public enum Resolver implements Serializable {
    NED("ned", "NED"),
    Simbad("simbad", "Simbad"),
    NedThenSimbad("nedthensimbad", "Try NED then Simbad", NED, Simbad),
    SimbadThenNed("simbadthenned", "Try Simbad then NED", Simbad, NED),
    PTF("ptf", "PTF"),
    Smart("smart", "Try NED and Simbad, then decide"),
    UNKNOWN("unknown", "resolved with unknown resolver"),
    NONE("none", "None");

    private String _key;
    private String _desc;
    private Resolver[] _combinationAry;

    private Resolver() {}

    Resolver(String key, String desc) {
        _key = key;
        _desc = desc;
        _combinationAry= null;
    }

    Resolver(String key, String desc, Resolver... combinationAry) {
        _key = key;
        _desc = desc;
        _combinationAry = combinationAry;
    }

    public String toString() { return _key; }
    public boolean isCombination() { return _combinationAry!=null; }
    public Resolver[] getCombinationAry() { return _combinationAry; }
    public String getUserDesc() { return _desc; }
    public String getKey() { return _key; }

    public static Resolver parse(String resolveStr) {
        Resolver retval;
        if (resolveStr.equalsIgnoreCase("ned")) {
            retval= Resolver.NED;
        } else if (resolveStr.equalsIgnoreCase("simbad")) {
            retval= Resolver.Simbad;
        } else if (resolveStr.equalsIgnoreCase("nedthensimbad")) {
            retval= Resolver.NedThenSimbad;
        } else if (resolveStr.equalsIgnoreCase("simbadthenned")) {
            retval= Resolver.SimbadThenNed;
        } else if (resolveStr.equalsIgnoreCase("ptf")) {
            retval= Resolver.PTF;
        } else if (resolveStr.equalsIgnoreCase("smart")) {
            retval= Resolver.Smart;
        } else {
            retval= Resolver.NONE;
        }
        return retval;
    }
}

