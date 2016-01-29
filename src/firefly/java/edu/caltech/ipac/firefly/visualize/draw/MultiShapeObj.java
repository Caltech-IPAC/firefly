/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize.draw;
/**
 * User: roby
 * Date: 2/22/13
 * Time: 12:23 PM
 */


import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.visualize.ScreenPt;
import edu.caltech.ipac.firefly.visualize.WebPlot;
import edu.caltech.ipac.util.dd.Region;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Trey Roby
 */
public class MultiShapeObj extends DrawObj {

    private List<DrawObj> drawObjList;

    public MultiShapeObj() {
        this.drawObjList = new ArrayList<DrawObj>(8);
    }

    public MultiShapeObj(List<DrawObj> drawObjList) {
        this.drawObjList = new ArrayList<DrawObj>(drawObjList);
    }

//=======================================================================
//-------------- Public Methods  --------------------------------------
//=======================================================================

    public void addDrawObj(DrawObj d) { drawObjList.add(d); }
    public void removeDrawObj(DrawObj d) { drawObjList.remove(d); }
    public boolean contains(DrawObj d) { return drawObjList.contains(d); }
    public int indexOf(DrawObj d) { return drawObjList.indexOf(d); }

    @Override
    public int getLineWidth() { return 0; }

    //=======================================================================
//-------------- Override Abstract Methods  -----------------------------
//=======================================================================

    @Override
    public double getScreenDist(WebPlot plot, ScreenPt pt) {
        double minDist= Double.MAX_VALUE;
        double dist;
        for(DrawObj d : drawObjList) {
            dist= d.getScreenDist(plot,pt);
            if (dist>-1 && dist<minDist) minDist= dist;
        }
        return minDist;
    }

    @Override
    public Pt getCenterPt() {
        int xTot= 0;
        int yTot= 0;
        Pt objCenter;
        for(DrawObj d : drawObjList) {
            objCenter= d.getCenterPt();
            xTot+= objCenter.getX();
            yTot+= objCenter.getY();
        }
        return new Pt(xTot/drawObjList.size(), yTot/drawObjList.size());
    }

    @Override
    public void draw(Graphics g, WebPlot p, DrawingDef def, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException {
        for(DrawObj d : drawObjList) {
            d.draw(g,p, def,useStateColor, false);
        }
    }

    @Override
    public void draw(Graphics g, DrawingDef def, boolean useStateColor, boolean onlyAddToPath) throws UnsupportedOperationException {
        for(DrawObj d : drawObjList) {
            d.draw(g, def,useStateColor, false);
        }
    }


//=======================================================================
//-------------- Override Methods  --------------------------------------
//=======================================================================

    @Override
    public void setColor(String c) {
        super.setColor(c);
        for(DrawObj d : drawObjList) d.setColor(c);
    }

    @Override
    public void setHighlightColor(String c) {
        super.setHighlightColor(c);
        for(DrawObj d : drawObjList) d.setHighlightColor(c);
    }

    @Override
    public void setSelectColor(String c) {
        super.setSelectColor(c);
        for(DrawObj d : drawObjList) d.setSelectColor(c);
    }


    @Override
    protected boolean getSupportsWebPlot() {
        boolean retval= true;
        for(DrawObj d : drawObjList) {
            if (!d.getSupportsWebPlot()) {
                retval= false;
                break;
            }
        }
        return retval;
    }

    @Override
    public List<Region> toRegion(WebPlot plot, DrawingDef def) {
        List<Region> retList= new ArrayList<Region>(100);
        for(DrawObj drawObj : drawObjList)  {
            retList.addAll(drawObj.toRegion(plot, def));
        }
        return retList;
    }

    private static Logger  logger= GwtUtil.getClientLogger();
    /**
	 * translate the draw objects to apt
	 * @param plot
	 * @param apt world point to translate to
	 */
	public void translateTo(WebPlot plot, WorldPt apt) {
		 List<DrawObj> retList= new ArrayList<DrawObj>(100);
		 for(DrawObj drawObj : drawObjList)  {
			 	try{
				 drawObj.translateTo(plot, apt);
				 retList.add(drawObj);
			 }catch(Exception e){
				 // implemented only in some children... 
				 logger.log(Level.INFO, "Only ShapeDataObj and FootprintObj have implementing translation", e);
			 }
			 
        }
		 drawObjList = retList;
	}
	
	public void rotateAround(WebPlot plot, double angle, WorldPt wc) {
		List<DrawObj> retList= new ArrayList<DrawObj>(100);
		 for(DrawObj drawObj : drawObjList)  {
			 	try{
				 drawObj.rotateAround(plot, angle, wc);
				 retList.add(drawObj);
			 }catch(Exception e){
				 // implemented only in some children... 
				 logger.log(Level.INFO, "Only ShapeDataObj and FootprintObj have implementing rotation", e);
			 }
			 
       }
		 drawObjList = retList;
	}
}

