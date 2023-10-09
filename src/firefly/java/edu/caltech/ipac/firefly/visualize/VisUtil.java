/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.visualize;

import edu.caltech.ipac.astro.conv.CoordConv;
import edu.caltech.ipac.astro.conv.LonLat;
import edu.caltech.ipac.visualize.plot.CoordinateSys;
import edu.caltech.ipac.visualize.plot.ImagePt;
import edu.caltech.ipac.visualize.plot.Pt;
import edu.caltech.ipac.visualize.plot.WorldPt;

import java.util.ArrayList;
import java.util.List;
/**
 * Date: Nov 13, 2008
 * @author Trey Roby
 */
public class VisUtil {

    private static final double DtoR = Math.PI / 180.0;
    private static final double RtoD = 180.0 / Math.PI;

    public enum FullType {ONLY_WIDTH, WIDTH_HEIGHT, ONLY_HEIGHT, SMART}

//======================================================================
//----------------------- Public Methods -------------------------------
//======================================================================

    public static double computeScreenDistance(double x1, double y1, double x2, double y2) {
        double deltaXSq = (x1 - x2) * (x1 - x2);
        double deltaYSq = (y1 - y2) * (y1 - y2);
        return Math.sqrt(deltaXSq + deltaYSq);
    }

    /**
     * compute the angular distance on the sky between two world points
     * @param p1
     * @param p2
     * @return angle (in degree!) representing the angular separation between
     * the two coordinates
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
        if (wpt==null) return null;
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

            //Yi added:
            double lonInput = pt.getLon();
            if (lonInput > 359.0) {
                lonInput -= 360.0;
            }

            //Yi changed to use lonInput:
            if (lonInput > max_lon) {
                max_lon = lonInput;
            }
            if (lonInput < min_lon) {
                min_lon = lonInput;
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
        // Yi added:
        if (lon < 0) lon += 360;

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

    static public double getPositionAngle(WorldPt pt0, WorldPt pt1) {
    	return getPositionAngle(pt0.getLon(), pt0.getLat(), pt1.getLon(), pt1.getLat());
    }
    
    /**
     * Compute position angle in degrees east of north
     *
     * @param ra0  the equatorial RA in degrees of the first object
     * @param dec0 the equatorial DEC in degrees of the first object
     * @param ra   the equatorial RA in degrees of the second object
     * @param dec  the equatorial DEC in degrees of the second object
     * @return position angle in degrees east of north between the two objects
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
            if (Math.abs(cospa) > 1.0){
            	cospa=cospa/Math.abs(cospa);
            }
//            if (cospa < -1.0) cospa = -1.0;
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


    public record CentralPointRetval(WorldPt worldPt, double radius) { }


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
        return (x + w > x0 && y + h > y0 && x < x0 + w0 && y < y0 + h0);
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
        return (x >= x0 && y >= y0 && x < x0 + w0 && y < y0 + h0);
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



    /**
	 * FIXME: EJ: Expecting WorldPt coord sys in J200 otherwise that method doesn't make any sense
	 * to me!
	 * 
	 * @param pos1
	 * @param offsetRa in arcsecs
	 * @param offsetDec in arcsecs
	 * @return
	 */
    public static WorldPt calculatePosition(WorldPt pos1, double offsetRa, double offsetDec ) {
        double ra = Math.toRadians(pos1.getLon());
        double dec = Math.toRadians(pos1.getLat());
        double de = Math.toRadians(offsetRa/3600.0); // east
        double dn = Math.toRadians(offsetDec)/3600.0; // north

        double cos_ra,sin_ra,cos_dec,sin_dec;
        double cos_de,sin_de,cos_dn,sin_dn;
        double rhat[] = new double[3];
        double shat[] = new double[3];
        double uhat[] = new double[3];
        double uxy;
        double ra2, dec2;

        cos_ra  = Math.cos(ra);
        sin_ra  = Math.sin(ra);
        cos_dec = Math.cos(dec);
        sin_dec = Math.sin(dec);

        cos_de = Math.cos(de);
        sin_de = Math.sin(de);
        cos_dn = Math.cos(dn);
        sin_dn = Math.sin(dn);


        rhat[0] = cos_de * cos_dn;
        rhat[1] = sin_de * cos_dn;
        rhat[2] = sin_dn;

        shat[0] = cos_dec * rhat[0] - sin_dec * rhat[2];
        shat[1] = rhat[1];
        shat[2] = sin_dec * rhat[0] + cos_dec * rhat[2];

        uhat[0] = cos_ra * shat[0] - sin_ra * shat[1];
        uhat[1] = sin_ra * shat[0] + cos_ra * shat[1];
        uhat[2] = shat[2];

        uxy = Math.sqrt(uhat[0] * uhat[0] + uhat[1] * uhat[1]);
        if (uxy>0.0)
            ra2 = Math.atan2(uhat[1],uhat[0]);
        else
            ra2 = 0.0;
        dec2 = Math.atan2(uhat[2],uxy);

        ra2  = Math.toDegrees(ra2);
        dec2 = Math.toDegrees(dec2);

        if (ra2 < 0.0) ra2 +=360.0;

        /*
        System.out.println("PositionUtil: " );
        System.out.println("ra: " + ra2);
        System.out.println("dec: " + dec2);
        */
        return new WorldPt(ra2, dec2);
    }

    /**
     * Returns new world position rotated by an angle in radian around 
     * @param positionToRotate
     * @param rotAng
     * @return
     */
    public static WorldPt rotateByAngle(WorldPt positionToRotate, double rotAng){
    	double ra = Math.toRadians(positionToRotate.getLon());
	    double dec = Math.toRadians(positionToRotate.getLat());
	    // Compute (x, y, z), the unit vector in R3 corresponding to positionToRotate
	    double cos_ra = Math.cos(ra);
	    double sin_ra = Math.sin(ra);
	    double cos_dec = Math.cos(dec);
	    double sin_dec = Math.sin(dec);
	    
	    double x = cos_ra * cos_dec;
	    double y = sin_ra * cos_dec;
	    double z = sin_dec;
	    
	    // Rotate by angle theta = -ra1 around the z axis:
	    //
	    // [ x1 ]   [ cos(ra1) -sin(ra1) 0 ]   [ x ]
	    // [ y1 ] = [ sin(ra1) cos(ra1)  0 ] * [ y ]
	    // [ z1 ]   [ 0        0         1 ]   [ z ]
	    double cos_theta = Math.cos(rotAng);
	    double sin_theta = Math.sin(rotAng);
	    double x1 = cos_theta * x - sin_theta * y;
	    double y1 = sin_theta * x + cos_theta * y;
	    double z1 = z;
	    
	    // Convert the unit vector result back to a WorldPt.
	    double d = x1 * x1 + y1 * y1;
	    double lon = 0.0;
	    double lat = 0.0;
	    if (d != 0.0) {
	        lon = Math.toDegrees(Math.atan2(y1, x1));
	        if (lon < 0.0) {
	            lon += 360.0;
	        }
	    }
	    if (z1 != 0.0) {
	        lat = Math.toDegrees(Math.atan2(z1, Math.sqrt(d)));
	        if (lat > 90.0) {
	            lat = 90.0;
	        } else if (lat < -90.0) {
	            lat = -90.0;
	        }
	    }
	    return new WorldPt(lon, lat);
    }
    
    /**
	 * Rotates the given input position and returns the result. The rotation
	 * applied to positionToRotate is the one which maps referencePosition to
	 * rotatedReferencePosition.
	 * @author Serge Monkewitz
	 * @param referencePosition 
	 * @param rotatedReferencePosition
	 * @param positionToRotate
	 * @return
	 */
	public static WorldPt getTranslateAndRotatePosition(WorldPt referencePosition,
	                                     WorldPt rotatedReferencePosition,
	                                     WorldPt positionToRotate) {
	    // Extract coordinates and transform to radians
	    double ra1 = Math.toRadians(referencePosition.getLon());
	    double dec1 = Math.toRadians(referencePosition.getLat());
	    double ra2 = Math.toRadians(rotatedReferencePosition.getLon());
	    double dec2 = Math.toRadians(rotatedReferencePosition.getLat());
	    double ra = Math.toRadians(positionToRotate.getLon());
	    double dec = Math.toRadians(positionToRotate.getLat());

	    // Compute (x, y, z), the unit vector in R3 corresponding to positionToRotate
	    double cos_ra = Math.cos(ra);
	    double sin_ra = Math.sin(ra);
	    double cos_dec = Math.cos(dec);
	    double sin_dec = Math.sin(dec);

	    double x = cos_ra * cos_dec;
	    double y = sin_ra * cos_dec;
	    double z = sin_dec;

	    // The rotation that maps referencePosition to rotatedReferencePosition
	    // can be broken down into 3 rotations. The first is a rotation by an
	    // angle of -ra1 around the z axis. The second is a rotation around the
	    // y axis by an angle equal to (dec1 - dec2), and the last is around the
	    // the z axis by ra2. We compute the individual rotations by
	    // multiplication with the corresponding 3x3 rotation matrix (see
	    // https://en.wikipedia.org/wiki/Rotation_matrix#Basic_rotations)

	    // Rotate by angle theta = -ra1 around the z axis:
	    //
	    // [ x1 ]   [ cos(ra1) -sin(ra1) 0 ]   [ x ]
	    // [ y1 ] = [ sin(ra1) cos(ra1)  0 ] * [ y ]
	    // [ z1 ]   [ 0        0         1 ]   [ z ]
	    double cos_theta = Math.cos(-ra1);
	    double sin_theta = Math.sin(-ra1);
	    double x1 = cos_theta * x - sin_theta * y;
	    double y1 = sin_theta * x + cos_theta * y;
	    double z1 = z;

	    // Rotate by angle theta = (dec1 - dec2) around the y axis:
	    //
	    // [ x ]   [ cos(dec1 - dec2)  0 sin(dec1 - dec2) ]   [ x1 ]
	    // [ y ] = [ 0                 1 0                ] * [ y1 ]
	    // [ z ]   [ -sin(dec1 - dec2) 0 cos(dec1 - dec2) ]   [ z1 ]
	    cos_theta = Math.cos(dec1 - dec2);
	    sin_theta = Math.sin(dec1 - dec2);
	    x = cos_theta * x1 + sin_theta * z1;
	    y = y1;
	    z = -sin_theta * x1 + cos_theta * z1;

	    // Rotate by angle theta = ra2 around the z axis:
	    //
	    // [ x1 ]   [ cos(ra2) -sin(ra2) 0 ]   [ x ]
	    // [ y1 ] = [ sin(ra2) cos(ra2)  0 ] * [ y ]
	    // [ z1 ]   [ 0        0         1 ]   [ z ]
	    cos_theta = Math.cos(ra2);
	    sin_theta = Math.sin(ra2);
	    x1 = cos_theta * x - sin_theta * y;
	    y1 = sin_theta * x + cos_theta * y;
	    z1 = z;

	    // Convert the unit vector result back to a WorldPt.
	    double d = x1 * x1 + y1 * y1;
	    double lon = 0.0;
	    double lat = 0.0;
	    if (d != 0.0) {
	        lon = Math.toDegrees(Math.atan2(y1, x1));
	        if (lon < 0.0) {
	            lon += 360.0;
	        }
	    }
	    if (z1 != 0.0) {
	        lat = Math.toDegrees(Math.atan2(z1, Math.sqrt(d)));
	        if (lat > 90.0) {
	            lat = 90.0;
	        } else if (lat < -90.0) {
	            lat = -90.0;
	        }
	    }
	    return new WorldPt(lon, lat);
	}


    /**
     * This function will create a line. either based on y or based on x depend on which is farther apart. The is will return
     * a point with x,y

     * @param x1 start point x
     * @param y1 start point y
     * @param x2 end point x
     * @param y2 end point y
     * @param independentValue the x or y depending how the line is computed
     * @return {number}
     */
    public static ImagePt getXorYinEquation(double x1, double y1, double x2, double y2, double independentValue) {
        double deltaX = Math.abs(x2 - x1);
        double deltaY = Math.abs(y2 - y1);

        if (deltaX > deltaY) {
            double slope = (y2-y1)/(x2-x1);
            double yIntercept = y1-slope*x1;
            double y= (slope*independentValue + yIntercept);
            return new ImagePt(independentValue,y);
        } else {
            double  islope = (x2-x1)/(y2-y1);
            double xIntercept = x1-islope*y1;
            double x = (islope*independentValue + xIntercept);
            return new ImagePt(x,independentValue);
        }
    }



	
    /**
     * Find the corners of a bounding box given the center and the radius
     * of a circle
     *
     * @param center the center of the circle
     * @param radius in arcsec
     * @return
     */
    public static Corners getCorners(WorldPt center, double radius) {
        WorldPt pos_left = calculatePosition(center, +radius, 0.0);
        WorldPt pos_right = calculatePosition(center, -radius, 0.0);
        WorldPt pos_up = calculatePosition(center, 0.0, +radius);
        WorldPt pos_down = calculatePosition(center, 0.0, -radius);
        WorldPt upperLeft = new WorldPt(pos_left.getLon(), pos_up.getLat());
        WorldPt upperRight = new WorldPt(pos_right.getLon(), pos_up.getLat());
        WorldPt lowerLeft = new WorldPt(pos_left.getLon(), pos_down.getLat());
        WorldPt lowerRight = new WorldPt(pos_right.getLon(), pos_down.getLat());

        return new Corners(upperLeft, upperRight, lowerLeft, lowerRight);
    }

    public record Corners(WorldPt upperLeft, WorldPt upperRight, WorldPt lowerLeft, WorldPt lowerRight) { }

    public record NorthEastCoords(int x1, int y1, int x2, int y2, int barbX1, int barbY1, int barbX2, int barbY2, int textX,
                                  int textY) {
    }
}


