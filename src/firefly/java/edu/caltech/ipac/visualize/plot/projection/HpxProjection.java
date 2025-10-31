/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.ProjectionPt;
import edu.caltech.ipac.visualize.plot.Pt;

/**
 * HPX (HEALPix) Projection implementation.
 * The HPX projection is defined in:
 *   Calabretta, M. R. & Roukema, B. F. (2007),
 *   "Mapping on the HEALPix grid".
 * The reference C implementation is in WCSLIB (prj.c):
 *   hpxset(), hpxs2x() [sphere to plane], and hpxx2s() [plane to sphere].
 */
public class HpxProjection {

    static private final double DtoR = Math.PI / 180.0;
    static private final double RtoD = 180.0 / Math.PI;

    /**
     * Get H parameter (number of facets around the equator) from header
     * PV2_1 = pv2[0], default 4
     */
    private static double getH(ProjectionParams hdr) {
        return (hdr.pv2 != null && hdr.pv2.length > 0) ? hdr.pv2[0] : 4.0;
    }

    /**
     * Get K parameter (vertical scaling) from header
     * PV2_2 = pv2[1], default 3
     */
    private static double getK(ProjectionParams hdr) {
        return (hdr.pv2 != null && hdr.pv2.length > 1) ? hdr.pv2[1] : 3.0;
    }

    /**
     * RevProject: Convert world coordinates (RA, Dec) to pixel coordinates.
     * This is the "reverse" projection: celestial sphere -> projection plane -> pixels
     */
    static public ProjectionPt RevProject(double ra, double dec, ProjectionParams hdr)
            throws ProjectionException {

        double rtwist, temp;

        // Get H and K parameters
        double H = getH(hdr);
        double K = getK(hdr);

        // Convert to native coordinates (phi, theta)
        double[] nativeCoords = ProjectionUtil.celestialToNative(ra, dec, hdr.crval1, hdr.crval2);
        if (nativeCoords == null) {
            return null;
        }

        double theta = nativeCoords[1] * DtoR;  // native latitude in radians
        double phi = nativeCoords[0] * DtoR;    // native longitude in radians

        // Normalize phi to [-pi, pi]
        while (phi > Math.PI) phi -= 2.0 * Math.PI;
        while (phi < -Math.PI) phi += 2.0 * Math.PI;

        double abs_theta = Math.abs(theta);
        double fsamp, fline;

        // HPX projection formulas
        // Transition latitude depends on K
        double theta_transition = Math.asin((K - 1.0) / K);

        // Check which region we're in
        if (abs_theta <= theta_transition) {
            // Equatorial region
            // Output in degrees to match the paper and wcslib
            fsamp = phi * RtoD;   // eq. (15)
            fline = (90.0 * K / H) * Math.sin(theta);   // eq. (16)
        } else {
            // Polar regions (matches wcslib implementation)
            double sigma = Math.sqrt(K * (1.0 - Math.abs(Math.sin(theta))));  // eq. (19)
            double sign_theta = (theta >= 0) ? 1 : -1;

            // facet center - phi_c for K odd or theta > 0
            // Work in degrees
            double phi_deg = phi * RtoD;
            // phi_c for K odd or theta > 0, eq. (20)
            double phi_c = -180.0 + (2 * Math.floor((phi_deg + 180.0) * H / 360.0) + 1.0) * 180.0 / H;

            // (phi - phi_c) in degrees
            double phi_minus_phic = phi_deg - phi_c;

            // Apply offset adjustment for southern polar half-facets when K is even
            boolean odd_k = ((int)K % 2) == 1;
            boolean odd_h = ((int)H % 2) == 1;
            boolean offset = !odd_k && theta <= 0.0;

            if (offset) {
                // Offset the southern polar half-facets for even K
                double facet_width = 180.0 / H;
                int h = (int)Math.floor(phi_deg / facet_width) + (odd_h ? 1 : 0);
                if (h % 2 == 1) {
                    phi_minus_phic -= facet_width;
                } else {
                    phi_minus_phic += facet_width;
                }
            }

            // Compute final coordinates
            // x = phi_c + (phi - phi_c) * sigma = phi + (phi - phi_c) * (sigma - 1)  // eq. (17)
            double xi = sigma - 1.0;
            fsamp = phi_deg + phi_minus_phic * xi;
            fline = sign_theta * (180.0 / H) * ((K + 1) / 2 - sigma);  // eq. (18)

            // Put the phi = 180 meridian in the expected place
            if (fsamp > 180.0) fsamp = 360.0 - fsamp;
        }

        // fsamp and fline are now in degrees (matching wcslib output)

        // Linear transformation from projection plane coordinates to pixel coordinates
        if (hdr.using_cd) {
            // CD matrix expects degrees, outputs degrees
            temp = hdr.dc1_1 * fsamp + hdr.dc1_2 * fline;
            fline = hdr.dc2_1 * fsamp + hdr.dc2_2 * fline;
            fsamp = temp;
        } else {
            // CDELT expects degrees
            rtwist = hdr.crota2 * DtoR;
            temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
            fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
            fsamp = temp;

            // cdelt is in degrees/pixel, so divide degrees by degrees/pixel to get pixels
            fsamp = fsamp / hdr.cdelt1;
            fline = fline / hdr.cdelt2;
        }

        // Apply inverse SIP distortion corrections if present
        if (hdr.map_distortion) {
            double fsamp_correction = 0.0;
            int len = (int)Math.min(hdr.ap_order + 1, ImageHeader.MAX_SIP_LENGTH);
            for (int i = 0; i < len; i++) {
                for (int j = 0; j < len; j++) {
                    if (i + j <= hdr.ap_order) {
                        fsamp_correction += hdr.ap[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
                    }
                }
            }

            double fline_correction = 0.0;
            len = (int)Math.min(hdr.bp_order + 1, ImageHeader.MAX_SIP_LENGTH);
            for (int i = 0; i < len; i++) {
                for (int j = 0; j < len; j++) {
                    if (i + j <= hdr.bp_order) {
                        fline_correction += hdr.bp[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
                    }
                }
            }
            fsamp += fsamp_correction;
            fline += fline_correction;
        }

        double x = fsamp + hdr.crpix1 - 1;
        double y = fline + hdr.crpix2 - 1;

        return new ProjectionPt(x, y);
    }

    /**
     * FwdProject: Convert pixel coordinates to world coordinates (RA, Dec).
     * This is the "forward" projection: pixels -> projection plane -> celestial sphere
     */
    static public Pt FwdProject(double x, double y, ProjectionParams hdr)
            throws ProjectionException {

        double rtwist, temp;
        double xx, yy;

        // Convert from pixel coordinates to intermediate pixel coordinates
        double fsamp = x - hdr.crpix1 + 1;
        double fline = y - hdr.crpix2 + 1;

        // Apply SIP distortion corrections if present
        if (hdr.map_distortion) {
            double fsamp_correction = 0.0;
            int len = (int)Math.min(hdr.a_order + 1, ImageHeader.MAX_SIP_LENGTH);
            for (int i = 0; i < len; i++) {
                for (int j = 0; j < len; j++) {
                    if (i + j <= hdr.a_order) {
                        fsamp_correction += hdr.a[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
                    }
                }
            }

            double fline_correction = 0.0;
            len = (int)Math.min(hdr.b_order + 1, ImageHeader.MAX_SIP_LENGTH);
            for (int i = 0; i < len; i++) {
                for (int j = 0; j < len; j++) {
                    if (i + j <= hdr.b_order) {
                        fline_correction += hdr.b[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
                    }
                }
            }
            fsamp += fsamp_correction;
            fline += fline_correction;
        }

        // Linear transformation to intermediate world (projection plane) coordinates
        if (hdr.using_cd) {
            // CD matrix: pixels to degrees
            xx = hdr.cd1_1 * fsamp + hdr.cd1_2 * fline;
            yy = hdr.cd2_1 * fsamp + hdr.cd2_2 * fline;
        } else {
            // cdelt is in degrees/pixel, so multiply pixels by degrees/pixel to get degrees
            xx = fsamp * hdr.cdelt1;
            yy = fline * hdr.cdelt2;

            rtwist = hdr.crota2 * DtoR;
            temp = xx * Math.cos(rtwist) - yy * Math.sin(rtwist);
            yy = xx * Math.sin(rtwist) + yy * Math.cos(rtwist);
            xx = temp;
        }
        // xx and yy are now in degrees (matching wcslib input)

        // HPX inverse formulas
        // Get H and K parameters
        double H = getH(hdr);
        double K = getK(hdr);

        double x_proj = xx;  // degrees
        double y_proj = yy;  // degrees

        double abs_y = Math.abs(y_proj);
        double theta, phi;

        // Transition value
        double y_transition = 90.0 * (K - 1) / H;

        // Determine region and compute (phi, theta)
        if (abs_y <= y_transition) {
            // Equatorial region
            phi = x_proj;  // eq. (22), degrees
            theta = Math.asin(y_proj / (90.0 * K / H)) * RtoD;  // eq. (23), degrees
        } else {
            // Polar regions (matches wcslib implementation)
            double sign_y = (y_proj >= 0) ? 1 : -1;
            double sigma = (K + 1) / 2.0 - abs_y / (180.0 / H);

            boolean odd_k = ((int)K % 2) == 1;
            boolean odd_h = ((int)H % 2) == 1;
            // offset = false for K odd or y > 0, otherwise true (for southern polar half-facets with even K)
            boolean offset = !odd_k && y_proj <= 0.0;

            // x_c for K odd or theta > 0, eq. (27)
            double x_c = -180.0 + (2 * Math.floor((x_proj + 180.0) * H / 360.0) + 1.0) * 180.0 / H;

            // (x - x_c) in degrees
            double x_minus_xc = x_proj - x_c;

            // Apply offset adjustment for southern polar half-facets when K is even
            if (offset) {
                double facet_width = 180.0 / H;
                int h = (int)Math.floor(x_proj / facet_width) + (odd_h ? 1 : 0);
                if (h % 2 == 1) {
                    x_minus_xc -= facet_width;
                } else {
                    x_minus_xc += facet_width;
                }
            }

            // Compute phi using the adjusted (x - x_c), eq. (24)
            double r = (1.0 / sigma) * x_minus_xc;
            phi = x_proj + (r != 0.0 ? r - x_minus_xc : 0.0);  // degrees
            theta = sign_y * Math.asin(1 - sigma * sigma / K) * RtoD;  // eq. (25), degrees
        }

        // phi and theta are now in degrees (native coordinates)

        // Convert to celestial coordinates
        double[] celestialCoords = ProjectionUtil.nativeToCelestial(phi, theta, hdr.crval1, hdr.crval2);
        if (celestialCoords == null) {
            return null;
        }

        double lon = celestialCoords[0];
        double lat = celestialCoords[1];

        return new Pt(lon, lat);
    }
}
