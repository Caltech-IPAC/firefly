/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 12/15/11
 * Time: 1:06 PM
 */


import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.ViewPortPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * @author Trey Roby
 */
public class LineDrawConnector extends DrawConnector {


    @Override
    public void draw(Graphics g, WebPlot p, AutoColor ac, WorldPt wp1, WorldPt wp2) throws UnsupportedOperationException {


        if (wp1==null || wp2==null) return;

        String color= calculateColor(ac);
        ViewPortPt pt0= p.getViewPortCoords(wp1);
        ViewPortPt pt1= p.getViewPortCoords(wp2);
        if (pt0==null || pt1==null) return;

        if (p.pointInViewPort(pt0) || p.pointInViewPort(pt1)) {
            g.drawLine(color, 2, pt0.getIX(), pt0.getIY(), pt1.getIX(), pt1.getIY());
        }
    }

    @Override
    public void draw(Graphics g, AutoColor ac, ScreenPt pt1, ScreenPt pt2) throws UnsupportedOperationException {
        throw new UnsupportedOperationException ("this type only supports drawing with WebPlot");
    }
}

