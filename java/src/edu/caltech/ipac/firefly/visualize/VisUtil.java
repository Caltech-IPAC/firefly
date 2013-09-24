package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.astro.conv.CoordConv;
import edu.caltech.ipac.astro.conv.LonLat;
import edu.caltech.ipac.firefly.visualize.draw.DrawObj;
import edu.caltech.ipac.firefly.visualize.draw.RecSelection;
import edu.caltech.ipac.util.StringUtils;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImageWorkSpacePt;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.List;
/**
 * User: roby
 * Date: Nov 13, 2008
 * Time: 1:37:56 PM
 */


/**
 * Shared by client and server
 *
 * @author Trey Roby
 */
public class VisUtil {

    private static final double DtoR = Math.PI / 180.0;
    private static final double RtoD = 180.0 / Math.PI;

    public enum FullType {ONLY_WIDTH, WIDTH_HEIGHT, ONLY_HEIGHT, SMART}

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public static boolean isLargePlot(float zFact, int width, int height) {
        return (zFact > 4 && (width > 2000 || height > 2000));
    }

    public static double computeScreenDistance(ScreenPt p1, ScreenPt p2) {
        return computeScreenDistance(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public static double computeScreenDistance(double x1, double y1, double x2, double y2) {
        double deltaXSq = (x1 - x2) * (x1 - x2);
        double deltaYSq = (y1 - y2) * (y1 - y2);
        return Math.sqrt(deltaXSq + deltaYSq);
    }

    public static double computeDistance(double ra0, double dec0, double ra1, double dec1) {
        return computeDistance(new WorldPt(ra0, dec0),
                               new WorldPt(ra1, dec1));
    }

    /**
     * compute the distance on the sky between two world points
     * @param p1
     * @param p2
     * @return
     */
    public static double computeDistance(WorldPt p1, WorldPt p2) {
        double lon1Radius = p1.getLon() * DtoR;
        double lon2Radius = p2.getLon() * DtoR;
        double lat1Radius = p1.getLat() * DtoR;
        double lat2Radius = p2.getLat() * DtoR;
        double cosine = Math.cos(lat1Radius) * Math.cos(lat2Radius) *
                Math.cos(lon1Radius - lon2Radius)
                + Math.sin(lat1Radius) * Math.sin(lat2Radius);

        if (Math.abs(cosine) > 1.0) cosine = cosine / Math.abs(cosine);
        return RtoD * Math.acos(cosine);
    }

    public static double computeDistance(Pt p1, Pt p2) {
        double dx = p1.getX() - p2.getX();
        double dy = p1.getY() - p2.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static WorldPt convertToJ2000(WorldPt wpt) {
        return convert(wpt, CoordinateSys.EQ_J2000);
    }

    /**
     * Convert from one coordinate system to another.
     *
     * @param wpt the world point to convert
     * @param to  the coordinate system to convert to
     * @return WorldPt the world point in the new coordinate system
     */
    public static WorldPt convert(WorldPt wpt, CoordinateSys to) {
        WorldPt retval;
        CoordinateSys from = wpt.getCoordSys();
        if (from.equals(to) || to == null) {
            retval = wpt;
        } else {
            double tobs = 0.0;
            if (from.equals(CoordinateSys.EQ_B1950)) tobs = 1983.5;
            LonLat ll = CoordConv.doConv(from.getJsys(), from.getEquinox(),
                                         wpt.getLon(), wpt.getLat(),
                                         to.getJsys(), to.getEquinox(), tobs);
            retval = new WorldPt(ll.getLon(), ll.getLat(), to);
        }
        return retval;
    }


    /**
     * Find an approximate central point and search radius for a group of positions
     *
     * @param inPoints array of points for which the central point is desired
     * @return CentralPointRetval WorldPt and search radius
     */
    public static CentralPointRetval computeCentralPointAndRadius(List<WorldPt> inPoints) {
        double lon, lat;
        double radius;
        double max_radius = Double.NEGATIVE_INFINITY;

        List<WorldPt> points= new ArrayList<WorldPt>(inPoints.size());
        for(WorldPt wp : inPoints) points.add(convertToJ2000(wp));


        /* get max,min of lon and lat */
        double max_lon = Double.NEGATIVE_INFINITY;
        double min_lon = Double.POSITIVE_INFINITY;
        double max_lat = Double.NEGATIVE_INFINITY;
        double min_lat = Double.POSITIVE_INFINITY;

        for (WorldPt pt : points) {
            if (pt.getLon() > max_lon) {
                max_lon = pt.getLon();
            }
            if (pt.getLon() < min_lon) {
                min_lon = pt.getLon();
            }
            if (pt.getLat() > max_lat) {
                max_lat = pt.getLat();
            }
            if (pt.getLat() < min_lat) {
                min_lat = pt.getLat();
            }
        }
        if (max_lon - min_lon > 180) {
            min_lon = 360 + min_lon;
        }
        lon = (max_lon + min_lon) / 2;
        if (lon > 360) lon -= 360;
        lat = (max_lat + min_lat) / 2;
        WorldPt central_point = new WorldPt(lon, lat);

        for (WorldPt pt : points) {
            radius = VisUtil.computeDistance(central_point,
                                             new WorldPt(pt.getLon(), pt.getLat()));
            if (max_radius < radius) {
                max_radius = radius;
            }
        }
        return new CentralPointRetval(central_point, max_radius);
    }


    /**
     * Compute position angle
     *
     * @param ra0  the equatorial RA in degrees of the first object
     * @param dec0 the equatorial DEC in degrees of the first object
     * @param ra   the equatorial RA in degrees of the second object
     * @param dec  the equatorial DEC in degrees of the second object
     * @return position angle in degrees between the two objects
     */
    static public double getPositionAngle(double ra0, double dec0,
                                          double ra, double dec) {
        double alf, alf0, del, del0;
        double sd, sd0, cd, cd0, cosda, cosd, sind, sinpa, cospa;
        double dist;
        double pa;

        alf = ra * DtoR;
        alf0 = ra0 * DtoR;
        del = dec * DtoR;
        del0 = dec0 * DtoR;

        sd0 = Math.sin(del0);
        sd = Math.sin(del);
        cd0 = Math.cos(del0);
        cd = Math.cos(del);
        cosda = Math.cos(alf - alf0);
        cosd = sd0 * sd + cd0 * cd * cosda;
        dist = Math.acos(cosd);
        pa = 0.0;
        if (dist > 0.0000004) {
            sind = Math.sin(dist);
            cospa = (sd * cd0 - cd * sd0 * cosda) / sind;
            if (cospa > 1.0) cospa = 1.0;
            sinpa = cd * Math.sin(alf - alf0) / sind;
            pa = Math.acos(cospa) * RtoD;
            if (sinpa < 0.0) pa = 360.0 - (pa);
        }
        dist *= RtoD;
        if (dec0 == 90.) pa = 180.0;
        if (dec0 == -90.) pa = 0.0;

        return (pa);
    }


    /**
     * Compute new position given a position and a distance and position angle
     *
     * @param ra   the equatorial RA in degrees of the first object
     * @param dec  the equatorial DEC in degrees of the first object
     * @param dist the distance in degrees to the second object
     * @param phi  the position angle in degrees to the second object
     * @return WorldPt of the new object
     */
    static public WorldPt getNewPosition(double ra, double dec,
                                         double dist, double phi) {
        double tmp, newdec, delta_ra;
        double ra1, dec1;

        ra *= DtoR;
        dec *= DtoR;
        dist *= DtoR;
        phi *= DtoR;

        tmp = Math.cos(dist) * Math.sin(dec) +
                Math.sin(dist) * Math.cos(dec) * Math.cos(phi);
        newdec = Math.asin(tmp);
        dec1 = newdec * RtoD;

        tmp = Math.cos(dist) * Math.cos(dec) -
                Math.sin(dist) * Math.sin(dec) * Math.cos(phi);
        tmp /= Math.cos(newdec);
        delta_ra = Math.acos(tmp);
        if (Math.sin(phi) < 0.0)
            ra1 = ra - delta_ra;
        else
            ra1 = ra + delta_ra;
        ra1 *= RtoD;
        return new WorldPt(ra1, dec1);
    }

    public static String getBestTitle(WebPlot plot) {
        String t = plot.getPlotDesc();
        if (StringUtils.isEmpty(t)) {
            MiniPlotWidget mpw = plot.getPlotView().getMiniPlotWidget();
            if (mpw != null) t = mpw.getTitle();
        }
        return t;
    }


    public static double getRotationAngle(WebPlot plot) {
        double retval = 0;
        try {
            double iWidth = plot.getImageWidth();
            double iHeight = plot.getImageHeight();
            double ix = iWidth / 2;
            double iy = iHeight / 2;
            WorldPt wptC = plot.getWorldCoords(new ImageWorkSpacePt(ix, iy));
//            double cdelt1 = plot.getImagePixelScaleInDeg();
            WorldPt wpt2 = plot.getWorldCoords(new ImageWorkSpacePt(ix, iHeight/4));
            retval = getPositionAngle(wptC.getLon(), wptC.getLat(), wpt2.getLon(), wpt2.getLat());
        } catch (ProjectionException e) {
            retval = 0;
        }
        return retval;
    }

    public static boolean isPlotNorth(WebPlot plot) {

        boolean retval;
        try {
            double iWidth = plot.getImageWidth();
            double iHeight = plot.getImageHeight();
            double ix = iWidth / 2;
            double iy = iHeight / 2;
            WorldPt wpt1 = plot.getWorldCoords(new ImageWorkSpacePt(ix, iy));
            double cdelt1 = plot.getImagePixelScaleInDeg();
            float zfact = plot.getZoomFact();
            WorldPt wpt2 = new WorldPt(wpt1.getLon(), wpt1.getLat() + (Math.abs(cdelt1) / zfact) * (5));

            ScreenPt spt1 = plot.getScreenCoords(wpt1);
            ScreenPt spt2 = plot.getScreenCoords(wpt2);
            retval = spt1.getIX() == spt2.getIX() && spt1.getIY() > spt2.getIY();
        } catch (ProjectionException e) {
            retval = false;
        }
        return retval;

    }

    public static float[] getPossibleZoomLevels() {
        return ZoomUtil._levels;
    }

    public static float getEstimatedFullZoomFactor(FullType fullType,
                                                   int dataWidth,
                                                   int dataHeight,
                                                   int screenWidth,
                                                   int screenHeight) {

        return getEstimatedFullZoomFactor(fullType, dataWidth, dataHeight, screenWidth, screenHeight, -1);
    }


    public static float getEstimatedFullZoomFactor(FullType fullType,
                                                   int dataWidth,
                                                   int dataHeight,
                                                   int screenWidth,
                                                   int screenHeight,
                                                   float tryMinFactor) {
        float zFact;
        if (fullType == FullType.ONLY_WIDTH || screenHeight <= 0 || dataHeight <= 0) {
            zFact = (float) screenWidth / (float) dataWidth;
        } else if (fullType == FullType.ONLY_HEIGHT || screenWidth <= 0 || dataWidth <= 0) {
            zFact = (float) screenHeight / (float) dataHeight;
        } else {
            float zFactW = (float) screenWidth / (float) dataWidth;
            float zFactH = (float) screenHeight / (float) dataHeight;
            if (fullType == FullType.SMART) {
                zFact = zFactW;
                if (zFactW > Math.max(tryMinFactor, 2)) {
                    zFact = Math.min(zFactW, zFactH);
                }
            } else {
                zFact = Math.min(zFactW, zFactH);
            }
        }
        return zFact;
    }


    public static class CentralPointRetval {
        private final WorldPt _wp;
        private final double _radius;

        public CentralPointRetval(WorldPt wp, double radius) {
            _wp = wp;
            _radius = radius;
        }

        public WorldPt getWorldPt() { return _wp; }
        public double getRadius() { return _radius; }
    }


    /**
     * Test to see if two rectangles intersect
     * @param x0 the first point x, top left
     * @param y0 the first point y, top left
     * @param w0 the first rec width
     * @param h0 the first rec height
     * @param x the second point x, top left
     * @param y the second point y, top left
     * @param w h the second rec width
     * @param h the second rec height
     * @return true if rectangles intersect
     */
    public static boolean intersects(int x0, int y0, int w0, int h0,
                                     int x, int y, int w, int h) {
        if (w0 <= 0 || h0 <= 0 || w <= 0 || h <= 0) {
            return false;
        }
        return (x + w > x0 &&
                y + h > y0 &&
                x < x0 + w0 &&
                y < y0 + h0);

    }


    /**
     * test to see if a point is in a rectangle
     * @param x0 the point x of the rec, top left
     * @param y0 the point y of the rec, top left
     * @param w0 the rec width
     * @param h0 the rec height
     * @param x the second point x, top left
     * @param y the second point y, top left
     * @return true if rectangles intersect
     */
    public static boolean contains(int x0, int y0, int w0, int h0, int x, int y) {
        return (x >= x0 && y >= y0 &&
                x < x0 + w0 && y < y0 + h0);
    }
    /**
     * test to see if the first rectangle contains the second rectangle
     * @param x0 the point x of the rec, top left
     * @param y0 the point y of the rec, top left
     * @param w0 the rec width
     * @param h0 the rec height
     * @param x the second point x, top left
     * @param y the second point y, top left
     * @param w h the second rec width
     * @param h the second rec height
     * @return true if rectangles intersect
     */
    public static boolean containsRec(int x0, int y0, int w0, int h0, int x, int y, int w, int h) {
        return contains(x0,y0,w0,h0,x,y) && contains(x0,y0,w0,h0,x+w,y+h);
    }

    public static boolean containsCircle(int x, int y, int centerX, int centerY, int radius) {
        return Math.pow((x - centerX), 2) + Math.pow((y - centerY), 2) < radius * radius;
    }

    public static NorthEastCoords getArrowCoords(int x1, int y1, int x2, int y2) {

        double barb_length = 10;

        /* compute shaft angle from arrowhead to tail */
        int delta_y = y2 - y1;
        int delta_x = x2 - x1;
        double shaft_angle = Math.atan2(delta_y, delta_x);
        double barb_angle = shaft_angle - 20 * Math.PI / 180; // 20 degrees from shaft
        double barbX = x2 - barb_length * Math.cos(barb_angle);  // end of barb
        double barbY = y2 - barb_length * Math.sin(barb_angle);

        float extX = x2 + 6;
        float extY = y2 + 6;

        int diffX = x2 - x1;
        int mult = ((y2 < y1) ? -1 : 1);
        if (diffX == 0) {
            extX = x2;
            extY = y2 + mult * 14;
        } else {
            float slope = ((float) y2 - y1) / ((float) x2 - x1);
            if (slope >= 3 || slope <= -3) {
                extX = x2;
                extY = y2 + mult * 14;
            } else if (slope < 3 || slope > -3) {
                extY = y2 - 6;
                if (x2 < x1) {
                    extX = x2 - 8;
                } else {
                    extX = x2 + 2;
                }
            }

        }
        return new NorthEastCoords(x1, y1, x2, y2, x2, y2, (int) barbX, (int) barbY, (int) extX, (int) extY);

    }

    public static Integer[] getSelectedPts(RecSelection selection, WebPlot plot, List<DrawObj> objList) {
        Integer retval[]= new Integer[0];
        if (selection!=null && plot!=null && objList!=null && objList.size()>0) {
            try {
                ScreenPt pt0= plot.getScreenCoords(selection.getPt0());
                ScreenPt pt1= plot.getScreenCoords(selection.getPt1());
                int x= Math.min( pt0.getIX(),  pt1.getIX());
                int y= Math.min(pt0.getIY(), pt1.getIY());
                int width= Math.abs(pt0.getIX()-pt1.getIX());
                int height= Math.abs(pt0.getIY()-pt1.getIY());
                int idx= 0;
                ScreenPt objCenter;
                List<Integer> selectedList= new ArrayList<Integer>(400);
                for(DrawObj obj : objList) {
                    try {
                        objCenter = plot.getScreenCoords(obj.getCenterPt());
                        if (VisUtil.contains(x,y,width,height,objCenter.getIX(), objCenter.getIY())) {
                            selectedList.add(idx);
                        }
                    } catch (ProjectionException e) {
                        // ignore
                    }
                    idx++;
                }
                if (selectedList.size()>0) {
                    retval= selectedList.toArray(new Integer[selectedList.size()]);
                }
            } catch (ProjectionException e) {
                // ignore, false through to failed case
            }
        }
        return retval;

    }

    public static class NorthEastCoords {
        public final int x1, y1, x2, y2;
        public final int barbX1, barbY1;
        public final int barbX2, barbY2;
        public final int textX, textY;

        public NorthEastCoords(int x1, int y1,
                               int x2, int y2,
                               int barbX1, int barbY1,
                               int barbX2, int barbY2,
                               int textX, int textY) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.barbX1 = barbX1;
            this.barbY1 = barbY1;
            this.barbX2 = barbX2;
            this.barbY2 = barbY2;
            this.textX = textX;
            this.textY = textY;
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
