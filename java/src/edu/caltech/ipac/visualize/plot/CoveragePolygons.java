/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot;

import java.io.Serializable;
import java.util.List;

/**
 * Date: Apr 28, 2009
 *
 * @author booth 
 * @version $Id: CoveragePolygons.java,v 1.1 2009/06/15 22:29:33 booth Exp $
 */
public class CoveragePolygons implements Serializable {

    private List<ImageCorners> _corners;
    private Circle _circle;


    public CoveragePolygons() {
    };

    public CoveragePolygons(List<ImageCorners> corners, Circle circle) {
	_corners = corners;
	_circle = circle;
    }

    public List<ImageCorners> getCorners()
    {
	return _corners;
    }

    public Circle getCircle()
    {
	return _circle;
    }
}
