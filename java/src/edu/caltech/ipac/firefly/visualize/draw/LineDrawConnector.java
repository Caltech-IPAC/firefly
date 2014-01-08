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
