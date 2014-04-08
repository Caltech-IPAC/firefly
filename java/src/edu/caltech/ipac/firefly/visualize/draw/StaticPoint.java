package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 5/8/12
 * Time: 2:27 PM
 */


import edu.caltech.ipac.util.HandSerialize;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.io.Serializable;

/**
 * @author Trey Roby
 */
public class StaticPoint implements Serializable, HandSerialize {

    private final static String SPLIT_TOKEN= "--StaticPoint--";

    private WorldPt pt;
    private DrawSymbol symbol;

    private StaticPoint() {}

    public StaticPoint(WorldPt pt, DrawSymbol symbol) {
        this.pt = pt;
        this.symbol = symbol;
    }

    public WorldPt getPt() { return pt; }

    public DrawSymbol getSymbol() { return symbol; }

    public String serialize() {
        return pt.toString() + SPLIT_TOKEN + symbol.toString();
    }

    public static StaticPoint parse(String s) {

        StaticPoint retval= null;
        String sAry[]= StringUtils.parseHelper(s, 2, SPLIT_TOKEN);
        if (sAry!=null) {
            WorldPt pt= WorldPt.parse(sAry[0]);
            try {
                DrawSymbol symbol = Enum.valueOf(DrawSymbol.class, sAry[1]);
                if (pt!=null && symbol!=null) {
                    retval= new StaticPoint(pt,symbol);
                }
            } catch (Exception e) {
                // ignore, just push to fail case
            }
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
