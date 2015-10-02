/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * @author tatianag
 * @version $Id: DrawConnector.java,v 1.1 2011/12/19 23:42:32 roby Exp $
 */
public abstract class DrawConnector {

    private final String FALLBACK_COLOR= "red";

    private String _color= null;

    public DrawConnector() {
    }

    public String getColor() { return _color; }

    public void setColor(String c) { _color = c; }





    protected boolean getSupportsWebPlot() { return true; }

    public void beginDrawing() {
    }

    public void endDrawing() {

    }

    /**
     *
     * @param g
     * @param p
     * @param def the AutoColor obj, may be null
     * @throws UnsupportedOperationException
     */
    public abstract void draw(Graphics g,
                              WebPlot p,
                              DrawingDef def,
                              WorldPt wp1,
                              WorldPt wp2) throws UnsupportedOperationException;

    /**
     *
     * @param g
     * @param def the AutoColor obj, may be null
     * @throws UnsupportedOperationException
     */
    public abstract void draw(Graphics g,
                              DrawingDef def,
                              ScreenPt pt1,
                              ScreenPt pt2) throws UnsupportedOperationException;

    protected String calculateColor(DrawingDef def) {
        String defColor= (def!=null && def.getDefColor()!=null) ? def.getDefColor() : FALLBACK_COLOR;
        return _color==null? defColor : _color;
    }


}
