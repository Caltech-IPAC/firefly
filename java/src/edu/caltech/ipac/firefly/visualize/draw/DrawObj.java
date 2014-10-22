package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.dd.Region;
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
    private int representCnt= 1;
    private AdvancedGraphics.Shadow shadow= null;
    private ScreenPt translation= null;

    public DrawObj() { }

    public boolean hasDetails() { return false; }
    public Widget makeDetailDisplay() { return null;   }

    public boolean isPathOptimized() { return false; }

    /**
     * method may return a 0 to indicate that with width varies
     * @return width of line to use to draw this object
     */
    public int getLineWidth() { return 1; }

    public String getColor() { return color; }
    public void setColor(String c) { color = c; }

    protected String getSelectColor() { return selectColor; }
    public void setSelectColor(String selectColor) { this.selectColor = selectColor; }
    protected String getHighlightColor() { return highlightColor; }
    public void setHighlightColor(String highlightColor) { this.highlightColor = highlightColor; }

//    public void setUserColor(String c) { color= userSetColor= c; }
//    public void resetColor() { color = userSetColor; }

    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isHighlighted() { return highlighted; }
    public void setHighlighted(boolean highlighted) { this.highlighted = highlighted; }

    /**
     * get the screen distance in pixels. Will return a negative number on error conditions.
     * @param plot
     * @param pt
     * @return
     */
    public abstract double getScreenDist(WebPlot plot, ScreenPt pt);


    protected boolean getSupportsWebPlot() { return true; }

    public abstract Pt getCenterPt();

    public boolean getSupportDuplicate() { return false; }
    public DrawObj duplicate() { return null; }

    /**
     *
     *
     * @param g
     * @param p
     * @param ac the AutoColor obj, may be null
     * @param useStateColor if true then draw with highlight or selected color, otherwise use normal color
     * @param onlyAddToPath
     * @throws UnsupportedOperationException
     */
    public abstract void draw(Graphics g, WebPlot p, AutoColor ac, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException;

    /**
     *
     *
     * @param g
     * @param ac the AutoColor obj, may be null
     * @param useStateColor if true then draw with highlight or selected color, otherwise use normal color
     * @param onlyAddToPath
     * @throws UnsupportedOperationException
     */
    public abstract void draw(Graphics g, AutoColor ac, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException;


    public String calculateColor(AutoColor ac, boolean useStateColor) {
        String color= this.color;
        if (useStateColor) {
            if (isSelected()) color= this.selectColor;
            if (isHighlighted()) color= this.highlightColor;
        }

        return (ac==null) ? (color==null? DEF_COLOR : color)  :  ac.getColor(color);
    }

    public List<Region> toRegion(WebPlot   plot,
                                 AutoColor ac) { return Collections.emptyList(); }


    public int getRepresentCnt() {
        return representCnt;
    }

    public void setShadow(AdvancedGraphics.Shadow shadow) { this.shadow = shadow; }
    public AdvancedGraphics.Shadow getShadow() { return shadow; }

    public ScreenPt getTranslation() { return translation; }

    public void setTranslation(ScreenPt translation) { this.translation = translation; }

    public void setRepresentCnt(int representCnt) {
        this.representCnt = representCnt;
    }

    public void incRepresentCnt() {
        this.representCnt++;
    }

    public void incRepresentCnt(int value) {
        this.representCnt+=value;
    }

    protected void copySetting(DrawObj d) {
        setColor(d.getColor());
        setSelectColor(d.getSelectColor());
        setHighlightColor(d.getHighlightColor());
    }
}

