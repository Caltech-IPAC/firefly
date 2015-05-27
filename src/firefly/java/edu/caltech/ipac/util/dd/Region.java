/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.util.dd;


import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.RegionFactory;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.Serializable;
import java.util.List;

/**
 * This class contains the specifications of the DS9 region
 * @author Booth Hartley
 */
public abstract class Region implements Serializable, HandSerialize, RegionFileElement, ContainsOptions {


   static public final int UNDEFINED = 0;
   static public final int FK4 = 1;
   static public final int FK5 = 2;
   static public final int IMAGE = 3;
   static public final int ECLIPTIC = 4;
   static public final int GALACTIC = 5;


    private WorldPt pt;
    private RegionOptions options= new RegionOptions();
    private transient boolean highlighted = false;



    private Region() { this(null); }

    public Region(WorldPt pt) { this.pt= pt; }

    public WorldPt getPt() { return pt; }

    public void setPt(WorldPt pt) { this.pt = pt; }

    public RegionOptions getOptions() {
        return options;
    }

    public void setOptions(RegionOptions options) {
        this.options = options;
    }

    public String getColor() { return options.getColor(); }

    public abstract String getDesc();

    public String serialize() {
        return RegionFactory.serialize(this);
    }

    public static Region parse(String s) {
        Region retval= null;
        try {
            List<RegionFileElement> resultList= RegionFactory.parsePart(s);
            for(RegionFileElement result : resultList) {
                if (result instanceof Region) {
                    retval= (Region)result;
                    break;
                }
            }
        } catch (RegParseException e) {
            retval= null;
        }
        return retval;
    }

    public static Region parseWithErrorChecking(String s) throws RegParseException {
        Region retval= null;
        List<RegionFileElement> resultList= RegionFactory.parsePart(s);
        for(RegionFileElement result : resultList) {
            if (result instanceof Region) {
                retval= (Region)result;
                break;
            }
        }
        return retval;
    }



    public String toString() {
        return getDesc() + "  " + pt.toString() +"  " + options.serialize();
    }

    public boolean isHighlighted() { return highlighted; }

    public void setHighlighted(boolean selected) { this.highlighted = selected; }

    @Override
    public int hashCode() {
        return serialize().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        boolean retval= false;
        if (obj instanceof Region) {
            retval= this.serialize().equals((((Region) obj).serialize()));
        }
        return retval;
    }
}

