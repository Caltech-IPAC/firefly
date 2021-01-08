/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.astro.net;
/**
 * User: roby
 * Date: Mar 4, 2009
 */
import java.io.Serializable;

/**
 * @author Trey Roby
 */
public enum Resolver implements Serializable {
    NED("ned", "NED", (Resolver) null),
    Simbad("simbad", "Simbad", (Resolver) null),
    NedThenSimbad("nedthensimbad", "Try NED then Simbad", NED, Simbad),
    SimbadThenNed("simbadthenned", "Try Simbad then NED", Simbad, NED),
    PTF("ptf", "PTF", (Resolver) null),
    UNKNOWN("unknown", "resolved with unknown resolver", (Resolver) null),
    NONE("none", "None", (Resolver) null);

    private final String _key;
    private final String _desc;
    private final Resolver[] _concreteResolvers;

    Resolver(String key, String desc, Resolver... concreteResolvers) {
        _key = key;
        _desc = desc;
        _concreteResolvers = concreteResolvers;
    }

    public String toString() { return _key; }
    public Resolver[] getConcertResolvers() { return _concreteResolvers !=null ? _concreteResolvers : new Resolver[] {this}; }

    public static Resolver parse(String resolveStr) {
        if (resolveStr.equalsIgnoreCase("ned")) return Resolver.NED;
        else if (resolveStr.equalsIgnoreCase("simbad")) return Resolver.Simbad;
        else if (resolveStr.equalsIgnoreCase("nedthensimbad")) return Resolver.NedThenSimbad;
        else if (resolveStr.equalsIgnoreCase("simbadthenned")) return Resolver.SimbadThenNed;
        else if (resolveStr.equalsIgnoreCase("ptf")) return Resolver.PTF;
        else return Resolver.NONE;
    }
}