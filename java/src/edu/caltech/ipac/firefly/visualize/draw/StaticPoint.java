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

