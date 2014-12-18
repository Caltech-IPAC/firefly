package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.visualize.plot.WorldPt;
/**
 * User: roby
 * Date: Sep 4, 2008
 * Time: 2:34:39 PM
 */


/**
 * @author Trey Roby
 */
public class RecSelection {

    private final WorldPt _pt0;
    private final WorldPt _pt1;

    public RecSelection(WorldPt pt0,
                        WorldPt pt1) {
        _pt0= pt0;
        _pt1= pt1;
    }

    public WorldPt getPt0() { return _pt0; }
    public WorldPt getPt1() { return _pt1; }
//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


}

