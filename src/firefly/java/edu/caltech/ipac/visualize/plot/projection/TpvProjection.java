package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.ProjectionPt;


/**
 * See ref https://fits.gsfc.nasa.gov/registry/tpvwcs.html
 *
 * @author ejoliet
 */
public class TpvProjection {


    static double dtr = Projection.dtr;
    static double rtd = Projection.rtd;

    static public ProjectionPt RevProject(double ra, double dec,
                                          ProjectionParams hdr, boolean useProjException) throws ProjectionException {
        double alphai = hdr.crval1;
        double deltai = hdr.crval2;
        double del = dec;
        double cdelz = FastMath.cos(deltai * dtr);
        double sdelz = FastMath.sin(deltai * dtr);
        double dalpha = (ra - alphai) * dtr;
        double cos_del = FastMath.cos(del * dtr);
        double sin_del = FastMath.sin(del * dtr);
        double sin_dalpha = FastMath.sin(dalpha);
        double cos_dalpha = FastMath.cos(dalpha);
        double x_tet_phi = cos_del * sin_dalpha;
        double y_tet_phi = sin_del * cdelz - cos_del * sdelz * cos_dalpha;

        double phi;
        double tet;

        if (dalpha > Math.PI) dalpha = -2 * Math.PI + dalpha;
        if (dalpha < -Math.PI) dalpha = +2 * Math.PI + dalpha;
        if ((-sin_del * sdelz) / (cos_del * cdelz) > 1) {
            if (useProjException) throw new ProjectionException("Outside the projection");
        } else if (((-sin_del * sdelz) / (cos_del * cdelz) > -1) && (Math.abs(dalpha) > Math.acos((-sin_del * sdelz) / (cos_del * cdelz)))) {
            if (useProjException) throw new ProjectionException("Outside the projection");
        }
        double den = sin_del * sdelz + cos_del * cdelz * cos_dalpha;
        double x_stand = x_tet_phi / den;
        double y_stand = y_tet_phi / den;
        x_stand *= rtd;
        y_stand *= rtd;

        double[] axis1poly = hdr.pv1poly;
        double[] axis2poly = hdr.pv2poly;
        double X = axis1poly[0];
        double Y = axis2poly[0];
        double dx;
        double dy;
        double xx = 0;
        double yy = 0;
        int niter = 20;
        int iter = 0;
        double m1, m2, m3, m4;

        while (iter < niter) {
                       /* Changes for SCAMP */
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

            //newton raphson
            dx = m1 * (x_stand - X) + m3 * (y_stand - Y);
            dy = m2 * (x_stand - X) + m4 * (y_stand - Y);

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

        return new ProjectionPt(xx, yy);

    }

    /**
     * Transform from pixel space to ra,dec assuming TAN (gnomonic) or TPV projection
     * <p>
     * image pixel to world coordinate
     *
     * @param px  pixel x coordinate
     * @param py  pixel y coordinate
     * @param hdr header - image header, needs to contain fields crpix1, crpix2, cd1_1. cd2_2, crval1, crval2, ctype1
     * @return
     */

    static public ProjectionPt FwdProject(double px, double py,
                                          ProjectionParams hdr) throws ProjectionException {

        double x, y;

        double crpix1 = hdr.crpix1;
        double crpix2 = hdr.crpix2;
        double glong = hdr.crval1;
        double glat = hdr.crval2;
//        double cdelt1 = hdr.cdelt1;
//        double cdelt2 = hdr.cdelt2;
        double cd11 = hdr.cd1_1;
        double cd12 = hdr.cd1_2;
        double cd21 = hdr.cd2_1;
        double cd22 = hdr.cd2_2;

        double rx = crpix1;
        double ry = crpix2;

        //Distortion is applied to intermediate (tangent) world coordinates
        x = cd11 * (px - rx) + cd12 * (py - ry);
        y = cd21 * (px - rx) + cd22 * (py - ry);


        double[] xy = distortion(x, y, hdr);

        if ((xy[0] == 0.0) && (xy[1] == 0.0))
            xy[1] = 1.0; /* avoid domain error in atan2 */
        // make sure angles between 0, 360 deg;
        double phi = Math.atan2(-xy[0], xy[1]);
        if (((x > 0) && (y > 0)) || ((x < 0) && (y > 0))) {
            phi = phi + Math.toRadians(180);
        } else if ((x < 0) && (y < 0)) {
            phi = phi + 2 * Math.toRadians(180);
        }
        double rt = Math.toRadians(Math.sqrt(Math.pow(xy[0], 2 + Math.pow(xy[1], 2))));
        double theta = Math.atan(1. / rt);
        double sp = Math.sin(phi);
        double cp = Math.cos(phi);
        double st = Math.sin(theta);
        double ct = Math.cos(theta);
        double sd = Math.sin(Math.toRadians(glat));
        double cd = Math.cos(Math.toRadians(glat));
        double a = glong + Math.toDegrees((Math.atan(ct * sp / (st * cd + ct * cp * sd))));
        double d = Math.toDegrees(Math.asin(st * sd - ct * cp * cd));
        ProjectionPt image_pt = new ProjectionPt(a, d);
        return image_pt;
    }

    /**
     * Distortion polynomial applied to intermediate coordinates using PV coefficients
     *
     * @param xi
     * @param eta
     * @param head
     * @return
     */
    private static double[] distortion(double xi, double eta, ProjectionParams head) {
        ///Correct projection plane coordinates for field distortion""";
        // Distortion coefficients
        double[] pv1 = head.pv1poly;
        double[] pv2 = head.pv2poly;
//        pv1[1] = pv2[1] = 1;

        // Apply correction (source http//iraf.noao.edu/projects/ccdmosaic/tpv.html);
        double r = Math.sqrt(Math.pow(xi, 2) + Math.pow(eta, 2));
        double xiprime = pv1[0] + pv1[1] * xi + pv1[2] * eta + pv1[3] * r +
                pv1[4] * Math.pow(xi, 2) + pv1[5] * xi * eta + pv1[6] * Math.pow(eta, 2) +
                pv1[7] * Math.pow(xi, 3) + pv1[8] * Math.pow(xi, 2) * eta + pv1[9] * xi * Math.pow(eta, 2) + pv1[10] * Math.pow(eta, 3) + pv1[11] * Math.pow(r, 3) +
                pv1[12] * Math.pow(xi, 4) + pv1[13] * Math.pow(xi, 3) * eta + pv1[14] * Math.pow(xi, 2) * Math.pow(eta, 2) + pv1[15] * xi * Math.pow(eta, 3) + pv1[16] * Math.pow(eta, 4);
        double etaprime = (pv2[0] + pv2[1] * eta + pv2[2] * xi + pv2[3] * r +
                pv2[4] * Math.pow(eta, 2)) + pv2[5] * eta * xi + pv2[6] * Math.pow(xi, 2) +
                pv2[7] * Math.pow(eta, 3) + pv2[8] * Math.pow(eta, 2) * xi + pv2[9] * eta * Math.pow(xi, 2) + pv2[10] * Math.pow(xi, 3) + pv2[11] * Math.pow(r, 3) +
                pv2[12] * Math.pow(eta, 4) + pv2[13] * Math.pow(eta, 3) * xi + pv2[14] * Math.pow(eta, 2) * Math.pow(xi, 2) + pv2[15] * eta * Math.pow(xi, 3) + pv2[16] * Math.pow(xi, 4);


        return new double[]{xiprime, etaprime};
    }
}

