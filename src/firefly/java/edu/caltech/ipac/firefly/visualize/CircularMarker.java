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

import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.ShapeDataObj;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
 * Refactored from original Marker class.
* @author Trey Roby
*/
public class CircularMarker extends Marker {
   
    public static final String FONT= "SansSerif";

    private WorldPt startPt;
    protected WorldPt endPt;
    private float workingScreenRadius;
    private String title=null;
    private Corner textCorner= Corner.SE;

    public CircularMarker(int screenRadius) {
        workingScreenRadius = screenRadius;
        startPt= null;
        endPt= null;
    }

    public CircularMarker(WorldPt center, WebPlot plot, int screenRadius) {
        ScreenPt cpt= plot.getScreenCoords(center);
        workingScreenRadius = screenRadius;
       
        ScreenPt pt1= new ScreenPt(cpt.getIX()-screenRadius, cpt.getIY()-screenRadius);
        ScreenPt pt2= new ScreenPt(cpt.getIX()+screenRadius, cpt.getIY()+screenRadius);
        updateRadius(plot, false);
        startPt= plot.getWorldCoords(pt1);
        endPt= plot.getWorldCoords(pt2);
    }
//
//    public Marker(WorldPt endPt, WorldPt startPt) {
//        this.endPt = endPt;
//        this.startPt = startPt;
//    }

    public CircularMarker(int i, boolean hasCross) {
		this(i);
	}

	public String getTitle() { return title; }
    public void setTitle(String title) { this.title= title; }
    public String getFont() { return FONT; }

    public void setTitleCorner(Corner c) { textCorner= c; }
    public Corner getTextCorner() { return textCorner; }

    public boolean isReady() {
        return (startPt!=null && endPt!=null);
    }

    private void updateRadius(WebPlot plot, boolean largeChangeOnly) {
        if (plot!=null && startPt!=null && endPt!=null) {
            ScreenPt spt= plot.getScreenCoords(startPt);
            ScreenPt ept= plot.getScreenCoords(endPt);
            if (spt==null || ept==null) return;
            int xDist= Math.abs(spt.getIX() - ept.getIX());
            int yDist= Math.abs(spt.getIY()-ept.getIY());
            int newRadius= Math.min(xDist,yDist)/2;
            if (largeChangeOnly) {
                if (Math.abs(newRadius-workingScreenRadius)>2) {
                    workingScreenRadius= newRadius;
                }
            }
            else {
                workingScreenRadius= newRadius;
            }
        }
    }

    public void move(WorldPt center, WebPlot plot) {
    	ScreenPt cpt;
		if(center==null){
    		cpt = getCenter(plot);
    	}
        else{
        	cpt= plot.getScreenCoords(center);
        }
        if (cpt==null) return;
        updateRadius(plot,true);
        int radius= Math.round(workingScreenRadius);
        ScreenPt spt= new ScreenPt(cpt.getIX()-radius, cpt.getIY()-radius);
        ScreenPt ept= new ScreenPt(cpt.getIX()+radius, cpt.getIY()+radius);
        startPt= plot.getWorldCoords(spt);
        endPt= plot.getWorldCoords(ept);
    }

    public void adjustStartEnd(WebPlot plot) {
        ScreenPt center= getCenter(plot);
        if (center!=null) {
            int r= (int)workingScreenRadius;
            ScreenPt sp= new ScreenPt(center.getIX()-r, center.getIY()-r);
            ScreenPt ep= new ScreenPt(center.getIX()+r, center.getIY()+r);
            startPt= plot.getWorldCoords(sp);
            endPt= plot.getWorldCoords(ep);
        }
    }

    public boolean contains(ScreenPt pt, WebPlot plot) {
        boolean retval= false;
        ScreenPt center= getCenter(plot);
        if (center!=null) {
            retval= VisUtil.containsCircle(pt.getIX(),pt.getIY(),
                                           center.getIX(),center.getIY(),
                                           (int)workingScreenRadius);
        }
        return retval;

    }

    public void setEditCorner(Corner corner, WebPlot plot) {
        ScreenPt spt= plot.getScreenCoords(startPt);
        ScreenPt ept= plot.getScreenCoords(endPt);
        if (spt==null || ept==null) return;
        int minX= Math.min(spt.getIX(), ept.getIX());
        int minY= Math.min(spt.getIY(), ept.getIY());
        int maxX= Math.max(spt.getIX(), ept.getIX());
        int maxY= Math.max(spt.getIY(), ept.getIY());

        switch (corner) {
            case NE:
                spt= new ScreenPt(minX,maxY);
                ept= new ScreenPt(maxX,minY);
                break;
            case NW:
                spt= new ScreenPt(maxX,maxY);
                ept= new ScreenPt(minX,minY);
                break;
            case SE:
                spt= new ScreenPt(minX,minY);
                ept= new ScreenPt(maxX,maxY);
                break;
            case SW:
                spt= new ScreenPt(maxX,minY);
                ept= new ScreenPt(minX,maxY);
                break;
        }

        startPt= plot.getWorldCoords(spt);
        endPt= plot.getWorldCoords(ept);

    }

    public MinCorner getMinCornerDistance(ScreenPt pt, WebPlot plot) {
        MinCorner retval= null;
        ScreenPt spt= plot.getScreenCoords(startPt);
        ScreenPt ept= plot.getScreenCoords(endPt);
        if (spt==null || ept==null) return null;
        int x= pt.getIX();
        int y= pt.getIY();

        int x1= spt.getIX();
        int y1= spt.getIY();
        int x2= ept.getIX();
        int y2= ept.getIY();

        int nwD= (int)VisUtil.computeScreenDistance(x,y,x1,y1);
        int neD= (int)VisUtil.computeScreenDistance(x,y,x2,y1);
        int seD= (int)VisUtil.computeScreenDistance(x,y,x2,y2);
        int swD= (int)VisUtil.computeScreenDistance(x,y,x1,y2);
        MinCorner mdAry[]= new MinCorner[] {
                new MinCorner(OverlayMarker.Corner.NW,nwD),
                new MinCorner(OverlayMarker.Corner.NE,neD),
                new MinCorner(OverlayMarker.Corner.SE,seD),
                new MinCorner(OverlayMarker.Corner.SW,swD)
        };

        int minDist= Integer.MAX_VALUE;
        for(MinCorner c : mdAry) {
            if (c.getDistance()<minDist) {
                minDist= c.getDistance();
                retval= c;
            }
        }
        return retval;
    }

    public ScreenPt getCorner(Corner corner, WebPlot plot) {
        ScreenPt retval= null;
        ScreenPt spt= plot.getScreenCoords(startPt);
        ScreenPt ept= plot.getScreenCoords(endPt);
        if (spt==null || ept==null) return null;

        int x1= spt.getIX();
        int y1= spt.getIY();
        int x2= ept.getIX();
        int y2= ept.getIY();

        switch (corner) {
            case NE:
                retval= new ScreenPt(x2,y1);
                break;
            case NW:
                retval= new ScreenPt(x1,y1);
                break;
            case SE:
                retval= new ScreenPt(x2,y2);
                break;
            case SW:
                retval= new ScreenPt(x1,y2);
                break;
        }

        return retval;
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

    public WorldPt getStartPt() { return startPt; }
    public WorldPt getEndPt() { return endPt; }
    public void setEndPt(WorldPt endPt,WebPlot plot) {
        if (endPt==null) return;
        this.endPt = endPt;
        updateRadius(plot,false);
    }
    public OffsetScreenPt getTitlePtOffset() {
        OffsetScreenPt retval= null;
        int radius= (int)workingScreenRadius;
        switch (textCorner) {
            case NE:
                retval= new OffsetScreenPt(-1*radius, -1*(radius+10));
                break;
            case NW:
                retval= new OffsetScreenPt(radius, -1*(radius));
                break;
            case SE:
                retval= new OffsetScreenPt(-1*radius, radius+5);
                break;
            case SW:
                retval= new OffsetScreenPt(radius, radius);
                break;
        }
        return retval;
    }

	public List<DrawObj> getShape() {
		ArrayList<DrawObj> lst = new ArrayList<>();
		lst.add(0, ShapeDataObj.makeCircle(getStartPt(), getEndPt()));
		lst.trimToSize();
		return lst;
	}
}

