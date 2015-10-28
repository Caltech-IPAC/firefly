/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 2/13/12
 * Time: 1:21 PM
 */


import java.util.ArrayList;
import java.util.List;

import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * Refactored from original Marker class.
* @author Trey Roby
*/
public class RectangleMarker extends Marker {
   
    public static final String FONT= "SansSerif";

    private WorldPt startPt;
    private WorldPt endPt;
    private int workingScreenXDelta, workingScreenYDelta;

    public RectangleMarker(int w, int h) {
    	workingScreenXDelta = w;
    	workingScreenYDelta = h;
    	
        startPt= null;
        endPt= null;
	}

    public boolean isReady() {
        return (startPt!=null && endPt!=null);
    }


    public void move(WorldPt center, WebPlot plot) {
        ScreenPt cpt= plot.getScreenCoords(center);
        if (cpt==null) return;
//        updateGeom(plot,true);
//        int delta= Math.round(workingScreenXDelta);
        int halfWidth = (int) (workingScreenXDelta/2);
        
        int halfHeight = workingScreenYDelta/2;
		ScreenPt spt= new ScreenPt(cpt.getIX()-halfWidth, cpt.getIY()-halfHeight);
        ScreenPt ept= new ScreenPt(cpt.getIX()+halfWidth, cpt.getIY()+halfHeight);
        startPt= plot.getWorldCoords(spt);
        endPt= plot.getWorldCoords(ept);
    }

    public void adjustStartEnd(WebPlot plot) {
        ScreenPt center= getCenter(plot);
        if (center!=null) {
            int deltax= (int)workingScreenXDelta/2;
            int deltay= (int)workingScreenYDelta/2;
            ScreenPt sp= new ScreenPt(center.getIX()-deltax, center.getIY()-deltay);
            ScreenPt ep= new ScreenPt(center.getIX()+deltax, center.getIY()+deltay);
            startPt= plot.getWorldCoords(sp);
            endPt= plot.getWorldCoords(ep);
        }
    }

    public boolean contains(ScreenPt pt, WebPlot plot) {
       return containsRect(pt, plot);

    }

    private boolean containsRect(ScreenPt pt, WebPlot plot) {
        ScreenPt spt= plot.getScreenCoords(startPt);
        ScreenPt ept= plot.getScreenCoords(endPt);
        if (spt==null || ept==null) return false;

        int x1= spt.getIX();
        int y1= spt.getIY();
        int x2= ept.getIX();
        int y2= ept.getIY();
        int w= Math.abs(x1 - x2);
        int h= Math.abs(y1-y2);

        return VisUtil.contains( Math.min(x1,x2), Math.min(y1,y2), w,h, pt.getIX(), pt.getIY());

    }

    public int getCenterDistance(ScreenPt pt, WebPlot plot) {
        int retval= -1;
        int x= pt.getIX();
        int y= pt.getIY();
        ScreenPt cpt= getCenter(plot);
        if (cpt!=null)  retval= (int)VisUtil.computeScreenDistance(x,y,cpt.getIX(),cpt.getIY());
        return retval;

    }

    public ScreenPt getCenter(WebPlot plot) {

        ScreenPt retval= null;
        if (startPt!=null && endPt!=null) {
            ScreenPt pt0= plot.getScreenCoords(startPt);
            ScreenPt pt1= plot.getScreenCoords(endPt);
            if (pt0==null || pt1==null) return null;

            int cx= Math.min(pt0.getIX(),pt1.getIX()) + Math.abs(pt0.getIX()-pt1.getIX())/2;
            int cy= Math.min(pt0.getIY(),pt1.getIY()) + Math.abs(pt0.getIY()-pt1.getIY())/2;
            retval= new ScreenPt(cx,cy);

        }
        return retval;
    }
//    
//    public void updateGeom(WebPlot plot, boolean largeChangeOnly) {
//        if (plot!=null && startPt!=null && endPt!=null) {
//            ScreenPt spt= plot.getScreenCoords(startPt);
//            ScreenPt ept= plot.getScreenCoords(endPt);
//            if (spt==null || ept==null) return;
//			int xDist = Math.abs(spt.getIX() - ept.getIX());
//			int yDist = Math.abs(spt.getIY() - ept.getIY());
//			int newDeltax = xDist / 2;
//			int newDeltay = yDist / 2;
//            if (largeChangeOnly) {
//                if (Math.abs(newDeltax-workingScreenXDelta)>2) {
//                    workingScreenXDelta= newDeltax;
//                }
//                if (Math.abs(newDeltay-workingScreenYDelta)>2) {
//                    workingScreenXDelta= newDeltax;
//                }
//            }
//            else {
//            	workingScreenXDelta= newDeltax;
//            	workingScreenYDelta= newDeltay;             
//            }
//        }
//    }
    

    public void setEndPt(WorldPt endPt,WebPlot plot) {
        if (endPt==null) return;
        this.endPt = endPt;
//        updateGeom(plot,false);
    }
    
	public List<ShapeDataObj> getShape() {
		ArrayList<ShapeDataObj> lst = new ArrayList<ShapeDataObj>();
		lst.add(ShapeDataObj.makeRectangle(getStartPt(), getEndPt()));
		lst.trimToSize();
		return lst;
	}
}

