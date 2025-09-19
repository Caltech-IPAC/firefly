/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.projection;

import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.ProjectionPt;
import edu.caltech.ipac.visualize.plot.Pt;

public class StereographicProjection {

    static private final double DtoR = Math.PI/180.0;
    static private final double RtoD = 180.0/Math.PI;

    static public ProjectionPt RevProject(double ra, double dec, ProjectionParams hdr, boolean useProjException)
            throws ProjectionException {
        // variables for linear transformation from pixels coordinates to
        // intermediate world (projection plane) coordinates
        double rpp1, rpp2;
        double rtwist, temp;

        double lon = ra * DtoR;
        double lat = dec * DtoR;
        double alpha0 = hdr.crval1 * DtoR;  // reference longitude
        double delta0 = hdr.crval2 * DtoR;  // reference latitude

        double cos_lat = Math.cos(lat);
        double sin_lat = Math.sin(lat);
        double cos_delta0 = Math.cos(delta0);
        double sin_delta0 = Math.sin(delta0);
        double cos_dlon = Math.cos(lon - alpha0);
        double sin_dlon = Math.sin(lon - alpha0);

        // Calculate angular distance from reference point
        double cos_c = sin_delta0 * sin_lat + cos_delta0 * cos_lat * cos_dlon;

        // Check for points on opposite hemisphere (not visible in stereographic)
        if (cos_c <= 0) {
            if (useProjException) {
                throw new ProjectionException("Point not visible in stereographic projection");
            }
            return null;
        }

        double k = 2.0 / (1.0 + cos_c);

        // Calculate intermediate / projection plane coordinates
        double fsamp = k * cos_lat * sin_dlon;
        double fline = k * (cos_delta0 * sin_lat - sin_delta0 * cos_lat * cos_dlon);

        // Linear transformation from projection plane coordinates to pixel coordinates
        if (hdr.using_cd)
        {
            temp = (hdr.dc1_1 * fsamp + hdr.dc1_2 * fline) * RtoD;
            fline = (hdr.dc2_1 * fsamp + hdr.dc2_2 * fline) * RtoD;
            fsamp = temp;
        }
        else
        {
            rpp1 = hdr.cdelt1 * DtoR;
            rpp2 = hdr.cdelt2 * DtoR;
            /* do the twist */
            rtwist = hdr.crota2 * DtoR;       /* convert to radians */
            temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
            fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
            fsamp = temp;

            fsamp = (fsamp / rpp1);     /* now apply cdelt */
            fline = (fline / rpp2);
        }

        // Apply inverse SIP distortion corrections if present
        // Inverse SIP must be applied after the projection math but before converting to final pixel coords
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

    static public Pt FwdProject(double x, double y, ProjectionParams hdr)
            throws ProjectionException {
        // variables for linear transformation from pixels coordinates to
        // intermediate world (projection plane) coordinates
        double rpp1, rpp2;
        double rtwist, temp;
        double xx, yy;

        // Convert from pixel coordinates to intermediate pixel coordinates

        // historical variable names:
        // fsamp = intermediate x-coordinate (sample direction)
        // fline = intermediate y-coordinate (line direction)
        double fsamp = x - hdr.crpix1 + 1;
        double fline = y - hdr.crpix2 + 1;

        // Apply SIP distortion corrections if present
        // SIP must be applied in pixel space, before the linear CD/PC transform and the projection
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
        if (hdr.using_cd)
        {
            // cdelt scaling factors are included int CD matrix coefficients
            // both are in units of degrees
            xx = (hdr.cd1_1 * fsamp + hdr.cd1_2 * fline) * DtoR;
            yy = (hdr.cd2_1 * fsamp + hdr.cd2_2 * fline) * DtoR;
        }
        else
        {
            rpp1 = hdr.cdelt1 * DtoR;        /* radians per pixel */
            rpp2 = hdr.cdelt2 * DtoR;        /* radians per pixel */
            xx = fsamp * rpp1;
            yy = fline * rpp2;

            rtwist = hdr.crota2 * DtoR;       /* convert to radians */
            temp = xx * Math.cos(rtwist) - yy * Math.sin(rtwist); /* do twist */
            yy = xx * Math.sin(rtwist) + yy * Math.cos(rtwist);
            xx = temp;
        }

        // Convert to radians
        double xi_rad = xx;  // intermediate x-coordinate already in radians
        double eta_rad = yy;  // intermediate y-coordinate already in radians
        double alpha0 = hdr.crval1 * DtoR;  // reference longitude
        double delta0 = hdr.crval2 * DtoR;  // reference latitude

        // Stereographic projection formulas
        double rho = Math.sqrt(xi_rad * xi_rad + eta_rad * eta_rad);
        double c = 2.0 * Math.atan(rho / 2.0);

        if (rho == 0.0) {
            // At the reference point
            return new Pt(hdr.crval1, hdr.crval2);
        }

        double cos_c = Math.cos(c);
        double sin_c = Math.sin(c);
        double cos_delta0 = Math.cos(delta0);
        double sin_delta0 = Math.sin(delta0);

        // Calculate latitude
        double lat = Math.asin(cos_c * sin_delta0 + (eta_rad * sin_c * cos_delta0) / rho);

        // Calculate longitude
        double lon = alpha0 + Math.atan2(xi_rad * sin_c,
                rho * cos_delta0 * cos_c - eta_rad * sin_delta0 * sin_c);

        lat = lat * RtoD;
        lon = (360. + lon * RtoD) % 360.; // handle negative value and greater than 360 value

        return new Pt(lon, lat);
    }
}