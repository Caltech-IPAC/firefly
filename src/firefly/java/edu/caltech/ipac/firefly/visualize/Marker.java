/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 2/13/12
 * Time: 1:21 PM
 */


import edu.caltech.ipac.visualize.plot.WorldPt;

/**
* @author Trey Roby
*/
public abstract class Marker implements OverlayMarker {
    public static final String FONT= "SansSerif";

    private WorldPt startPt;
    private WorldPt endPt;
    private String title=null;
    private Corner textCorner= OverlayMarker.Corner.SE;

//    public Marker(int screenRadius) {
//        startCorner= Corner.NW;
//        endCorner= Corner.SE;
//        workingScreenRadius = screenRadius;
//        startPt= null;
//        endPt= null;
//    }

//    public Marker(WorldPt center, int screenRadius, WebPlot plot) throws ProjectionException {
//        ScreenPt cpt= plot.getScreenCoords(center);
//        if (cpt==null) throw new ProjectionException("Could not convert points");
//        ScreenPt pt1= new ScreenPt(cpt.getIX()-screenRadius, cpt.getIY()-screenRadius);
//        ScreenPt pt2= new ScreenPt(cpt.getIX()+screenRadius, cpt.getIY()+screenRadius);
//        startCorner= Corner.NW;
//        endCorner= Corner.SE;
//        workingScreenRadius = screenRadius;
//        startPt= plot.getWorldCoords(pt1);
//        endPt= plot.getWorldCoords(pt2);
//        if (startPt==null || endPt==null) throw new ProjectionException("Could not convert points");
//    }
//
//    public Marker(WorldPt endPt, WorldPt startPt) {
//        this.endPt = endPt;
//        this.startPt = startPt;
//    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title= title; }
    public String getFont() { return FONT; }

    public void setTitleCorner(Corner c) { textCorner= c; }
    public Corner getTextCorner() { return textCorner; }

    public boolean isReady() {
        return (startPt!=null && endPt!=null);
    }

    public abstract void move(WorldPt center, WebPlot plot);

    public abstract void adjustStartEnd(WebPlot plot);

    public abstract boolean contains(ScreenPt pt, WebPlot plot);

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
    
    public abstract void setEndPt(WorldPt endPt,WebPlot plot);
    
}

