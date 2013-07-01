package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;

import java.util.Collections;
import java.util.List;

/**
 * @author Trey, tatianag
 * @version $Id: DrawObj.java,v 1.8 2012/02/10 01:39:40 roby Exp $
 */
public abstract class DrawObj {

    private final String DEF_COLOR= "red";

    private String color= null;
    private String selectColor= null;
    private String highlightColor= null;
    private boolean selected= false;
    private boolean highlighted= false;

    public DrawObj() { }

    public boolean hasDetails() { return false; }
    public Widget makeDetailDisplay() { return null;   }



    public String getColor() { return color; }
    public void setColor(String c) { color = c; }

//    public String getSelectColor() { return selectColor; }
    public void setSelectColor(String selectColor) { this.selectColor = selectColor; }
//    public String getHighlightColor() { return highlightColor; }
    public void setHighlightColor(String highlightColor) { this.highlightColor = highlightColor; }

//    public void setUserColor(String c) { color= userSetColor= c; }
//    public void resetColor() { color = userSetColor; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isHighlighted() { return highlighted; }
    public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }

    public abstract double getScreenDist(WebPlot plot, ScreenPt pt) throws ProjectionException;


    protected boolean getSupportsWebPlot() { return true; }

    public abstract Pt getCenterPt();

    /**
     *
     * @param g
     * @param p
     * @param ac the AutoColor obj, may be null
     * @param useStateColor if true then draw with highlight or selected color, otherwise use normal color
     * @throws UnsupportedOperationException
     */
    public abstract void draw(Graphics g, WebPlot p, AutoColor ac, boolean useStateColor) throws UnsupportedOperationException;

    /**
     *
     * @param g
     * @param ac the AutoColor obj, may be null
     * @param useStateColor if true then draw with highlight or selected color, otherwise use normal color
     * @throws UnsupportedOperationException
     */
    public abstract void draw(Graphics g, AutoColor ac, boolean useStateColor) throws UnsupportedOperationException;

    public void update(Graphics g, boolean front, AutoColor ac) {}
    public void update(Graphics g, WebPlot p, boolean front, AutoColor ac) {}


    protected String calculateColor(AutoColor ac, boolean useStateColor) {
        String color= this.color;
        if (useStateColor) {
            if (isSelected()) color= this.selectColor;
            if (isHighlighted()) color= this.highlightColor;
        }

        return (ac==null) ? (color==null? DEF_COLOR : color)  :  ac.getColor(color);
    }

    public List<Region> toRegion(WebPlot   plot,
                                 AutoColor ac) { return Collections.emptyList(); }

}
