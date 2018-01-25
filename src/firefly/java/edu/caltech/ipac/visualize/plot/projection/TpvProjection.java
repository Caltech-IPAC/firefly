package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.ProjectionPt;
import edu.caltech.ipac.visualize.plot.Pt;


/**
 * See ref https://fits.gsfc.nasa.gov/registry/tpvwcs.html
 * TPV distortion is applied to sky coordinates (compared to SIP/Gnomonic where distortion is applied to image pixel coordinates)
 *
 * This implementation was based on TPV concept and mathematics here (D. Shupe, et.al.):
 *
 * https://authors.library.caltech.edu/36801/
 *
 * In the paper above, intermediate coordinates (u,v) are renamed here fsamp,fline
 * to follow same nomenclature as in @{@link GnomonicProjection}
 * @author ejoliet
 */
public class TpvProjection {


    static double dtr = Projection.dtr;
    static double rtd = Projection.rtd;
    /**
     * Convert world coordinate to projected/image coordinate
     * @param ra
     * @param dec
     * @param hdr
     * @param useProjException
     * @return
     * @throws ProjectionException
     */
    static public ProjectionPt RevProject(double ra, double dec,
                                          ProjectionParams hdr, boolean useProjException) throws ProjectionException {


       // Transform WC Ra,dec to intermediate coordinate (tan plane)

        double lat, lon;
        double rpp1, rpp2, lat0, lon0;
        double aa, ff1, ff2;
        double fline, fsamp, rtwist, temp;
        double x, y;

        double crpix1 = hdr.crpix1;
        double crpix2 = hdr.crpix2;
        double glong = hdr.crval1;
        double glat = hdr.crval2;
        double cdelt1 = hdr.cdelt1;
        double cdelt2 = hdr.cdelt2;
        double twist = hdr.crota2;
        boolean using_cd = hdr.using_cd;
        double dc1_1 = hdr.dc1_1;
        double dc1_2 = hdr.dc1_2;
        double dc2_1 = hdr.dc2_1;
        double dc2_2 = hdr.dc2_2;

        lon = ra * dtr;
        lat = dec * dtr;

        rpp1 = -cdelt1 * dtr;
        rpp2 = -cdelt2 * dtr;

        lon0 = glong * dtr;
        lat0 = glat * dtr;

        aa = Math.cos(lat) * Math.cos(lon - lon0);
        ff1 = 1. / (Math.sin(lat0) * Math.sin(lat) + aa * Math.cos(lat0));
        ff2 = 1. / (Math.sin(lat0) * Math.sin(lat) + aa * Math.cos(lat0));

        if (ff1 < 0) {
	    /* we're more than 90 degrees from projection center */
            if (useProjException) throw new ProjectionException("coordinates not on image");
            else return null;
        } else {
            fline = -ff2 * (Math.cos(lat0) * Math.sin(lat) - aa * Math.sin(lat0));
            fsamp = -ff1 * Math.cos(lat) * Math.sin(lon - lon0);
        }

        // Recover uncorrected-intermediate coordinate before TPV distortion

        double[] axis1poly = hdr.pv1;
        double[] axis2poly = hdr.pv2;
        double X = axis1poly[0];
        double Y = axis2poly[0];
        double dx;
        double dy;
        double xx = 0;
        double yy = 0;
        int niter = 20; //Seems that after 4 is already enough but this is a rule of thumb.
        int iter = 0;
        double m1, m2, m3, m4;

        while (iter < niter) {
            iter++;
            double r;

            if ((xx == 0.0) & (yy == 0.0)) r = 1;
            else r = Math.sqrt(xx * xx + yy * yy);
            m1 = axis1poly[1] +
                    axis1poly[3] * xx / r +
                    2 * axis1poly[4] * xx +
                    axis1poly[5] * yy +
                    3 * axis1poly[7] * xx * xx +
                    axis1poly[9] * yy * yy +
                    2 * axis1poly[8] * yy * xx +
                    3 * axis1poly[11] * xx * Math.sqrt(xx * xx + yy * yy);
            m2 = axis2poly[2] +
                    axis2poly[3] * xx / r +
                    2 * axis2poly[6] * xx +
                    axis2poly[5] * yy +
                    3 * axis2poly[10] * xx * xx +
                    axis2poly[8] * yy * yy +
                    2 * axis2poly[9] * yy * xx +
                    3 * axis2poly[11] * xx * Math.sqrt(xx * xx + yy * yy);

            m3 = axis1poly[2] +
                    axis1poly[3] * yy / r +
                    2 * axis1poly[6] * yy +
                    axis1poly[5] * xx +
                    3 * axis1poly[10] * yy * yy +
                    2 * axis1poly[9] * yy * xx +
                    axis1poly[8] * xx * xx +
                    3 * axis1poly[11] * yy * Math.sqrt(xx * xx + yy * yy);
            m4 = axis2poly[1] +
                    axis2poly[3] * yy / r +
                    2 * axis2poly[4] * yy +
                    axis2poly[5] * xx +
                    3 * axis2poly[7] * yy * yy +
                    2 * axis2poly[8] * yy * xx +
                    axis2poly[9] * xx * xx +
                    3 * axis2poly[11] * yy * Math.sqrt(xx * xx + yy * yy);
            double det = m1 * m4 - m2 * m3;
            double tmp = m4 / det;
            m2 /= -det;
            m3 /= -det;
            m4 = m1 / det;
            m1 = tmp;

            //newton raphson to find the best coordinates on the plane tangent
            dx = m1 * (fsamp - X) + m3 * (fline - Y);
            dy = m2 * (fsamp - X) + m4 * (fline - Y);

            xx += dx;
            yy += dy;
            r = Math.sqrt(xx * xx + yy * yy);

            X = axis1poly[0] +
                    axis1poly[2] * yy +
                    axis1poly[1] * xx +
                    axis1poly[3] * Math.sqrt(xx * xx + yy * yy) +
                    axis1poly[6] * yy * yy +
                    axis1poly[4] * xx * xx +
                    axis1poly[5] * yy * xx +
                    axis1poly[10] * yy * yy * yy +
                    axis1poly[7] * xx * xx * xx +
                    axis1poly[9] * yy * yy * xx +
                    axis1poly[8] * yy * xx * xx +
                    axis1poly[11] * r * r * r;
            //   X  *= dtr ;
            Y = axis2poly[0] +
                    axis2poly[1] * yy +
                    axis2poly[2] * xx +
                    axis2poly[3] * Math.sqrt(xx * xx + yy * yy) +
                    axis2poly[4] * yy * yy +
                    axis2poly[6] * xx * xx +
                    axis2poly[5] * yy * xx +
                    axis2poly[7] * yy * yy * yy +
                    axis2poly[10] * xx * xx * xx +
                    axis2poly[8] * yy * yy * xx +
                    axis2poly[9] * yy * xx * xx +
                    axis2poly[11] * r * r * r;
        }


        // Finally, image pixel derived from above intermdiate coordinates found

        if (using_cd) {
            temp = -(dc1_1 * fsamp + dc1_2 * fline) * rtd;
            fline = -(dc2_1 * fsamp + dc2_2 * fline) * rtd;
            fsamp = temp;
        } else {
        /* do the twist */
            rtwist = twist * dtr;       /* convert to radians */
            temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
            fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
            fsamp = temp;

            fsamp = (fsamp / rpp1);     /* now apply cdelt */
            fline = (fline / rpp2);
        }

        x = fsamp + crpix1 - 1;
        y = fline + crpix2 - 1;

        return new ProjectionPt(x, y);

    }

    /**
     * Transform from pixel space to ra,dec assuming TPV projection
     * <p>
     * image pixel to world coordinate
     *
     * @param px  pixel x coordinate
     * @param py  pixel y coordinate
     * @param hdr header - image header, needs to contain fields crpix1, crpix2, cd1_1. cd2_2, crval1, crval2, ctype1
     * @return
     */

    static public Pt FwdProject(double px, double py,
                                ProjectionParams hdr) throws ProjectionException {

        double x, y; //sky coords

        double crpix1 = hdr.crpix1;
        double crpix2 = hdr.crpix2;
        double glong = hdr.crval1;
        double glat = hdr.crval2;
        double cd11 = hdr.cd1_1;
        double cd12 = hdr.cd1_2;
        double cd21 = hdr.cd2_1;
        double cd22 = hdr.cd2_2;

        // the intermediate coordinates offset from the distortion-center origin
        double fsamp = px - crpix1 + 1;
        double fline = py - crpix2 + 1;
//        double rx = crpix1;
//        double ry = crpix2;

        //Distortion is applied to intermediate (tangent) world coordinates so lets calculate those
        // by inverting cd matrix
        x = -(cd11 * fsamp + cd12 * fline) * dtr;
        y = -(cd21 * fsamp + cd22 * fline) * dtr;
//        x = cd11 * (px - rx) + cd12 * (py - ry);
//        y = cd21 * (px - rx) + cd22 * (py - ry);


        // Apply PV distortion
        double[] xy = distortion(x, y, hdr);

        // distortioned-corrected intermediate coordinates:
        double xx = xy[0];
        double yy = xy[1];

//        if ((xy[0] == 0.0) && (xy[1] == 0.0))
//            xy[1] = 1.0; /* avoid domain error in atan2 */
//        // make sure angles between 0, 360 deg;
//        double phi = Math.atan2(-xy[0], xy[1]);
//        if (((x > 0) && (y > 0)) || ((x < 0) && (y > 0))) {
//            phi = phi + Math.toRadians(180);
//        } else if ((x < 0) && (y < 0)) {
//            phi = phi + 2 * Math.toRadians(180);
//        }
//        double rt = Math.toRadians(Math.sqrt(Math.pow(xy[0], 2) + Math.pow(xy[1], 2)));
//        double theta = Math.atan(1. / rt);
//        double sp = Math.sin(phi);
//        double cp = Math.cos(phi);
//        double st = Math.sin(theta);
//        double ct = Math.cos(theta);
//        double sd = Math.sin(Math.toRadians(glat));
//        double cd = Math.cos(Math.toRadians(glat));
//        double a = glong + Math.toDegrees((Math.atan(ct * sp / (st * cd + ct * cp * sd))));
//        double d = Math.toDegrees(Math.asin(st * sd - ct * cp * cd));

        // Same as TAN/Gnomonic FWD transform
        // u,v = inv(CD)*(xx,yy)
        double delta = Math.atan(Math.sqrt(xx * xx + yy * yy));

        if ((xx == 0.0) && (yy == 0.0))
            yy = 1.0;  /* avoid domain error in atan2 */
        double beta = Math.atan2(-xx, yy);
        double glatr = glat * dtr;
        double glongr = glong * dtr;
        double lat = Math.asin(-Math.sin(delta) * Math.cos(beta) * Math.cos(glatr) +
                Math.cos(delta) * Math.sin(glatr));
        double xxx = Math.sin(glatr) * Math.sin(delta) * Math.cos(beta) +
                Math.cos(glatr) * Math.cos(delta);
        double yyy = Math.sin(delta) * Math.sin(beta);
        double lon = glongr + Math.atan2(yyy, xxx);

        double d = lat * rtd;
        double a = lon * rtd;
        Pt image_pt = new Pt(a, d);
        return image_pt;
    }

    /**
     * Distortion polynomial applied to intermediate coordinates using PV coefficients
     *
     * @param x fsamp, the intermediate longitude offset from the distortion-center origin
     * @param y fline, the intermediate latitude offset from the distortion-center origin
     * @param head projection parameters
     * @return 1d array size 2 with distortioned-corrected intermediate longitude, latitude
     */
    private static double[] distortion(double x, double y, ProjectionParams head) {
        ///Correct projection plane coordinates for field distortion""";
        // Distortion coefficients
        double[] pv1 = head.pv1;
        double[] pv2 = head.pv2;
//        pv1[1] = pv2[1] = 1;

        // Apply correction (source http//iraf.noao.edu/projects/ccdmosaic/tpv.html);
        double r = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
        double xprime = pv1[0] + pv1[1] * x + pv1[2] * y + pv1[3] * r +
                pv1[4] * Math.pow(x, 2) + pv1[5] * x * y + pv1[6] * Math.pow(y, 2) +
                pv1[7] * Math.pow(x, 3) + pv1[8] * Math.pow(x, 2) * y + pv1[9] * x * Math.pow(y, 2) + pv1[10] * Math.pow(y, 3) + pv1[11] * Math.pow(r, 3) +
                pv1[12] * Math.pow(x, 4) + pv1[13] * Math.pow(x, 3) * y + pv1[14] * Math.pow(x, 2) * Math.pow(y, 2) + pv1[15] * x * Math.pow(y, 3) + pv1[16] * Math.pow(y, 4);
        double yprime = pv2[0] + pv2[1] * y + pv2[2] * x + pv2[3] * r +
                pv2[4] * Math.pow(y, 2) + pv2[5] * y * x + pv2[6] * Math.pow(x, 2) +
                pv2[7] * Math.pow(y, 3) + pv2[8] * Math.pow(y, 2) * x + pv2[9] * y * Math.pow(x, 2) + pv2[10] * Math.pow(x, 3) + pv2[11] * Math.pow(r, 3) +
                pv2[12] * Math.pow(y, 4) + pv2[13] * Math.pow(y, 3) * x + pv2[14] * Math.pow(y, 2) * Math.pow(x, 2) + pv2[15] * y * Math.pow(x, 3) + pv2[16] * Math.pow(x, 4);


        return new double[]{xprime, yprime};
    }
}

