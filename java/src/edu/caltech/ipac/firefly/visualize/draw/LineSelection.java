package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
/**
 * User: roby
 * Date: Sep 4, 2008
 * Time: 2:34:39 PM
 */



/**
 * @author Trey Roby
 */
public class LineSelection {

    private final ImageWorkSpacePt _pt1;
    private final ImageWorkSpacePt _pt2;

    public LineSelection(ImageWorkSpacePt pt1,
                         ImageWorkSpacePt pt2) {
        _pt1= pt1;
        _pt2= pt2;
    }

    public ImageWorkSpacePt getPt1() { return _pt1; }
    public ImageWorkSpacePt getPt2() { return _pt2; }
//======================================================================
//------------------ Private / Protected Methods -----------------------
//======================================================================


}
