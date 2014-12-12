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

    public String toString() {
        return getDesc() + "  " + pt.toString() +"  " + options.serialize();
    }

    public boolean isHighlighted() { return highlighted; }

    public void setHighlighted(boolean selected) { this.highlighted = selected; }
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
