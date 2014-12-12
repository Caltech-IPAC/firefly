package edu.caltech.ipac.targetgui.net;
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
