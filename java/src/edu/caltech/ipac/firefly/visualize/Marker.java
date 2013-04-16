package edu.caltech.ipac.firefly.visualize;
/**
 * User: roby
 * Date: 2/13/12
 * Time: 1:21 PM
 */


import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.WorldPt;

/**
* @author Trey Roby
*/
public class Marker {
    public enum Corner {NE,NW,SE,SW}
    public static final String FONT= "SansSerif";

    private WorldPt startPt;
    private WorldPt endPt;
    private Corner startCorner;
    private Corner endCorner;
    private float workingScreenRadius;
    private String title=null;
    private Corner textCorner= Corner.SE;

    public Marker(int screenRadius) {
        startCorner= Corner.NW;
        endCorner= Corner.SE;
        workingScreenRadius = screenRadius;
        startPt= null;
        endPt= null;
    }

    public Marker(WorldPt center, int screenRadius, WebPlot plot) throws ProjectionException {
        ScreenPt cpt= plot.getScreenCoords(center);
        ScreenPt pt1= new ScreenPt(cpt.getIX()-screenRadius, cpt.getIY()-screenRadius);
        ScreenPt pt2= new ScreenPt(cpt.getIX()+screenRadius, cpt.getIY()+screenRadius);
        startCorner= Corner.NW;
        endCorner= Corner.SE;
        workingScreenRadius = screenRadius;
        startPt= plot.getWorldCoords(pt1);
        endPt= plot.getWorldCoords(pt2);
    }

    public Marker(WorldPt endPt, WorldPt startPt) {
        this.endPt = endPt;
        this.startPt = startPt;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title= title; }
    public String getFont() { return FONT; }

    public void setTitleCorner(Corner c) { textCorner= c; }
    public Corner getTextCorner() { return textCorner; }

    public boolean isReady() {
        return (startPt!=null && endPt!=null);
    }

    public void updateRadius(WebPlot plot) {
        try {
            ScreenPt spt= plot.getScreenCoords(startPt);
            ScreenPt ept= plot.getScreenCoords(endPt);
            int xDist= Math.abs(spt.getIX() - ept.getIX());
            int yDist= Math.abs(spt.getIY()-ept.getIY());
            workingScreenRadius= Math.min(xDist,yDist)/2;
        } catch (ProjectionException e) {
        }
    }

    public void move(WorldPt center, WebPlot plot) throws ProjectionException {
        ScreenPt cpt= plot.getScreenCoords(center);
        int radius= Math.round(workingScreenRadius);
        ScreenPt spt= new ScreenPt(cpt.getIX()-radius, cpt.getIY()-radius);
        ScreenPt ept= new ScreenPt(cpt.getIX()+radius, cpt.getIY()+radius);
        startPt= plot.getWorldCoords(spt);
        endPt= plot.getWorldCoords(ept);
    }

    public void adjustStartEnd(WebPlot plot) {
        try {
            ScreenPt center= getCenter(plot);
            if (center!=null) {
                int r= (int)workingScreenRadius;
                ScreenPt sp= new ScreenPt(center.getIX()-r, center.getIY()-r);
                ScreenPt ep= new ScreenPt(center.getIX()+r, center.getIY()+r);
                startPt= plot.getWorldCoords(sp);
                endPt= plot.getWorldCoords(ep);
            }
        } catch (ProjectionException e) {
            // do nothing
        }
    }

    public boolean contains(ScreenPt pt, WebPlot plot) {
        boolean retval= false;
        try {
            ScreenPt center= getCenter(plot);
            if (center!=null) {
                retval= VisUtil.containsCircle(pt.getIX(),pt.getIY(),
                                               center.getIX(),center.getIY(),
                                               (int)workingScreenRadius);
            }
        } catch (ProjectionException e) {
            retval= false;
        }
        return retval;

    }

    public boolean containsSquare(ScreenPt pt, WebPlot plot) {
        boolean retval;
        try {
            ScreenPt spt= plot.getScreenCoords(startPt);
            ScreenPt ept= plot.getScreenCoords(endPt);

            int x1= spt.getIX();
            int y1= spt.getIY();
            int x2= ept.getIX();
            int y2= ept.getIY();
            int w= Math.abs(x1 - x2);
            int h= Math.abs(y1-y2);

            retval= VisUtil.contains( Math.min(x1,x2), Math.min(y1,y2), w,h, pt.getIX(), pt.getIY());
        } catch (ProjectionException e) {
            retval= false;
        }
        return retval;

    }

    public void setEditCorner(Corner corner, WebPlot plot) {
        try {
            ScreenPt spt= plot.getScreenCoords(startPt);
            ScreenPt ept= plot.getScreenCoords(endPt);
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
        } catch (ProjectionException e) {
        }

    }

    public MinCorner getMinCornerDistance(ScreenPt pt, WebPlot plot) {
        MinCorner retval= null;
        try {
            ScreenPt spt= plot.getScreenCoords(startPt);
            ScreenPt ept= plot.getScreenCoords(endPt);
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
                    new MinCorner(Corner.NW,nwD),
                    new MinCorner(Corner.NE,neD),
                    new MinCorner(Corner.SE,seD),
                    new MinCorner(Corner.SW,swD)
            };

            int minDist= Integer.MAX_VALUE;
            for(MinCorner c : mdAry) {
                if (c.getDistance()<minDist) {
                    minDist= c.getDistance();
                    retval= c;
                }
            }
        } catch (ProjectionException e) {
            retval= null;
        }
        return retval;
    }

    public ScreenPt getCorner(Corner corner, WebPlot plot) {
        ScreenPt retval= null;
        try {
            ScreenPt spt= plot.getScreenCoords(startPt);
            ScreenPt ept= plot.getScreenCoords(endPt);

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

        } catch (ProjectionException e) {
            retval= null;
        }
        return retval;
    }

    public int getCenterDistance(ScreenPt pt, WebPlot plot) {
        int retval= -1;
        try {
            int x= pt.getIX();
            int y= pt.getIY();
            ScreenPt cpt= getCenter(plot);
            if (cpt!=null) {
                retval= (int)VisUtil.computeScreenDistance(x,y,cpt.getIX(),cpt.getIY());
            }
        } catch (ProjectionException e) {
            retval= -1;
        }
        return retval;

    }

    public ScreenPt getCenter(WebPlot plot) throws  ProjectionException {

        ScreenPt retval= null;
        if (startPt!=null && endPt!=null) {
            ScreenPt pt0= plot.getScreenCoords(startPt);
            ScreenPt pt1= plot.getScreenCoords(endPt);

            int xDist= Math.abs(pt0.getIX()-pt1.getIX())/2;
            int yDist= Math.abs(pt0.getIY()-pt1.getIY())/2;
            int radius= Math.min(xDist,yDist);

            int cx= Math.min(pt0.getIX(),pt1.getIX()) + Math.abs(pt0.getIX()-pt1.getIX())/2;
            int cy= Math.min(pt0.getIY(),pt1.getIY()) + Math.abs(pt0.getIY()-pt1.getIY())/2;
            retval= new ScreenPt(cx,cy);

        }
        return retval;
    }

    public WorldPt getStartPt() { return startPt; }
    public WorldPt getEndPt() { return endPt; }
    public void setEndPt(WorldPt endPt,WebPlot plot) {
        this.endPt = endPt;
        updateRadius(plot);
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

    public static class MinCorner {
        private final Corner corner;
        private final int distance;

        public MinCorner(Corner corner, int distance) {
            this.corner = corner;
            this.distance = distance;
        }

        public int getDistance() {
            return distance;
        }

        public Corner getCorner() {
            return corner;
        }
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
