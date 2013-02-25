package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;

/**
 * @author tatianag
 * @version $Id: DrawObj.java,v 1.8 2012/02/10 01:39:40 roby Exp $
 */
public abstract class DrawObj {

    private final String DEF_COLOR= "red";

    private String _color= null;
    private String _userSetColor= null;
    private boolean _plotOnTop= false;
    private Shapes _shapes;

    public DrawObj() { }

    public boolean hasDetails() { return false; }
    public Widget makeDetailDisplay() { return null;   }



    public String getColor() { return _color; }

    public void setColor(String c) {
        _color = c;
    }

    public void setUserColor(String c) {
        _color= _userSetColor= c;
    }

    public void resetColor() {
        _color = _userSetColor;
    }

    public boolean plotOnTop() { return _plotOnTop; }

    public void setPlotOnTop(boolean p) { _plotOnTop = p; }

    public abstract double getScreenDist(WebPlot plot, ScreenPt pt) throws ProjectionException;

    protected void setShapes(Shapes s) { _shapes= s; }
    protected Shapes getShapes() { return _shapes; }

    protected boolean getSupportsWebPlot() { return true; }

    public abstract Pt getCenterPt();

    /**
     *
     * @param g
     * @param p
     * @param front true it this obj should be drawn on top of others
     * @param ac the AutoColor obj, may be null
     * @throws UnsupportedOperationException
     */
    public abstract void draw(Graphics g, WebPlot p, boolean front, AutoColor ac) throws UnsupportedOperationException;

    /**
     *
     * @param g
     * @param front true it this obj should be drawn on top of others
     * @param ac the AutoColor obj, may be null
     * @throws UnsupportedOperationException
     */
    public abstract void draw(Graphics g, boolean front, AutoColor ac) throws UnsupportedOperationException;

    public void update(Graphics g, boolean front, AutoColor ac) {}
    public void update(Graphics g, WebPlot p, boolean front, AutoColor ac) {}


    protected String calculateColor(AutoColor ac) {
        return (ac==null) ? (_color==null? DEF_COLOR : _color)  :  ac.getColor(_color);
    }


}
