/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.visualize.plot.Pt;
/**
 * User: roby
 * Date: Sep 4, 2008
 * Time: 2:34:39 PM
 */


/**
 * @author Trey Roby
 */
public class RecSelection {

    private final Pt _pt0;
    private final Pt _pt1;

    public RecSelection(Pt pt0, Pt pt1) {
        _pt0= pt0;
        _pt1= pt1;
    }

    public Pt getPt0() { return _pt0; }
    public Pt getPt1() { return _pt1; }
//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


}

