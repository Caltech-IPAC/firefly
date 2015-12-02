/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;

import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj.ShapeType;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.Collections;
import java.util.List;

/**
 * @author Trey, tatianag
 * @version $Id: DrawObj.java,v 1.8 2012/02/10 01:39:40 roby Exp $
 */
public abstract class DrawObj {

    private final String FALLBACK_COLOR = "red";

    private String color= null;
    private String selectColor= null;
    private String highlightColor= null;
    private boolean selected= false;
    private boolean highlighted= false;
    private int representCnt= 1;
    private AdvancedGraphics.Shadow shadow= null;
    private ScreenPt translation= null;

	private double rotAngle;

    public DrawObj() { }

    public boolean hasDetails() { return false; }
    public Widget makeDetailDisplay() { return null;   }

    public boolean getCanUsePathEnabledOptimization() { return false; }

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

   // public abstract ShapeType getShape();

    public abstract Pt getCenterPt();

    public boolean getSupportDuplicate() { return false; }
    public DrawObj duplicate() { return null; }

    /**
     *
     *
     * @param g
     * @param p
     * @param def the AutoColor obj, may be null
     * @param useStateColor if true then draw with highlight or selected color, otherwise use normal color
     * @param onlyAddToPath
     * @throws UnsupportedOperationException
     */
    public abstract void draw(Graphics g, WebPlot p, DrawingDef def, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException;

    /**
     *
     *
     * @param g
     * @param def the AutoColor obj, may be null
     * @param useStateColor if true then draw with highlight or selected color, otherwise use normal color
     * @param onlyAddToPath
     * @throws UnsupportedOperationException
     */
    public abstract void draw(Graphics g, DrawingDef def, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException;


    public String calculateColor(DrawingDef def, boolean useStateColor) {
        String color= this.color;
        String defColor= (def!=null && def.getDefColor()!=null) ? def.getDefColor() : FALLBACK_COLOR;
        if (useStateColor) {
            if (isSelected()) color= this.selectColor;
            if (isHighlighted()) color= this.highlightColor;
        }
        return color==null? defColor : color;
    }

    public List<Region> toRegion(WebPlot   plot, DrawingDef def) { return Collections.emptyList(); }


    public int getRepresentCnt() {
        return representCnt;
    }

    public void setShadow(AdvancedGraphics.Shadow shadow) { this.shadow = shadow; }
    public AdvancedGraphics.Shadow getShadow() { return shadow; }

    public ScreenPt getTranslation() { return translation; }

    public void setTranslation(ScreenPt translation) { this.translation = translation; }

    public void setRotation(double angInRad) { this.rotAngle = angInRad; }
    
    public double getRotation() { return this.rotAngle; }
    
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
    
    
	/**
	 * (re)Build world points translated to apt
	 * 
	 * @param plot
	 * @param apt
	 *            world point to translate to
	 */
	public void translateTo(WebPlot plot, WorldPt apt) {
		throw new RuntimeException("translateTo() should be implemented in the child class");
	}
    
	/**
	 * Apply a rotation of an angle [rad] all points around a center wc [world
	 * point]
	 * 
	 * @param plot
	 *            plot object
	 * @param angle
	 *            angle to rotate in radians
	 * @param wc
	 *            center world point to rotate around
	 */
	public void rotateAround(WebPlot plot, double angle, WorldPt wc) {
		throw new RuntimeException("rotateAround() should be implemented in the child class");
	}
}

