/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot.projection;
/*
 * User: roby
 * Date: 7/17/18
 * Time: 1:20 PM
 */


import edu.caltech.ipac.visualize.plot.ImageHeader;
import edu.caltech.ipac.visualize.plot.ProjectionException;
import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;

/**
 * @author Trey Roby
 */
public class ProjectionUtil {

    private static final double dtr = Projection.dtr;
    private static final double rtd = Projection.rtd;
    private static final double WCSTRIG_TOL = 1e-10;

    public static boolean isSameProjection(FitsRead firstFitsRead, FitsRead secondFitsread) {
        boolean result = false;

        if (firstFitsRead.getProjectionType() == secondFitsread.getProjectionType()) {
            ImageHeader H1 = new ImageHeader(firstFitsRead.getHeader());
            ImageHeader H2 = new ImageHeader(secondFitsread.getHeader());
            if (H1.maptype == Projection.PLATE) {
                result = checkPlate(H1, H2);
            } else {
                result = checkOther(H1, H2);
            }
        }
        return result;
    }

    public static boolean checkDistortion(ImageHeader H1, ImageHeader H2) {
        boolean result = false;
        if ((H1.ap_order == H2.ap_order) &&
                (H1.a_order == H2.a_order) &&
                (H1.bp_order == H2.bp_order) &&
                (H1.b_order == H2.b_order)) {
            result = true;
            for (int i = 0; i <= H1.a_order; i++) {
                for (int j = 0; j <= H1.a_order; j++) {
                    if ((i + j <= H1.a_order) && (i + j > 0)) {
                        if (H1.a[i][j] != H2.a[i][j]) {
                            result = false;
                            break;
                        }
                    }
                }
            }
            for (int i = 0; i <= H1.ap_order; i++) {
                for (int j = 0; j <= H1.ap_order; j++) {
                    if ((i + j <= H1.ap_order) && (i + j > 0)) {
                        if (H1.ap[i][j] != H2.ap[i][j]) {
                            result = false;
                            break;
                        }
                    }
                }
            }
            for (int i = 0; i <= H1.b_order; i++) {
                for (int j = 0; j <= H1.b_order; j++) {
                    if ((i + j <= H1.b_order) && (i + j > 0)) {
                        if (H1.b[i][j] != H2.b[i][j]) {
                            result = false;
                            break;
                        }
                    }
                }
            }
            for (int i = 0; i <= H1.bp_order; i++) {
                for (int j = 0; j <= H1.bp_order; j++) {
                    if ((i + j <= H1.bp_order) && (i + j > 0)) {
                        if (H1.bp[i][j] != H2.bp[i][j]) {
                            result = false;
                            break;
                        }
                    }
                }
            }
        }
        return result;

    }

    public static boolean checkOther(ImageHeader H1, ImageHeader H2) {
        boolean result = false;
        if (
                (H1.naxis1 == H2.naxis1) &&
                        (H1.naxis2 == H2.naxis2) &&
                        (H1.crpix1 == H2.crpix1) &&
                        (H1.crpix2 == H2.crpix2) &&
                        (H1.cdelt1 == H2.cdelt1) &&
                        (H1.cdelt2 == H2.cdelt2) &&
                        (H1.crval1 == H2.crval1) &&
                        (H1.crval2 == H2.crval2) &&
                        (H1.crota2 == H2.crota2) &&
                        (H1.getJsys() == H2.getJsys()) &&
                        (H1.file_equinox == H2.file_equinox)) {
            /* OK so far - now check distortion correction */
            if (H1.map_distortion &&
                    H2.map_distortion) {
                result = checkDistortion(H1, H2);

            } else {
                result = true;
            }
        }
        return result;
    }

    public static boolean checkPlate(ImageHeader H1, ImageHeader H2) {

        boolean result = false;
        if ((H1.plate_ra == H2.plate_ra) &&
                (H1.plate_dec == H2.plate_dec) &&
                (H1.x_pixel_offset == H2.x_pixel_offset) &&
                (H1.y_pixel_offset == H2.y_pixel_offset) &&
                (H1.plt_scale == H2.plt_scale) &&
                (H1.x_pixel_size == H2.x_pixel_size) &&
                (H1.y_pixel_size == H2.y_pixel_size)) {

            result = true;

            /* OK so far - now check coefficients */
            for (int i = 0; i < 6; i++) {
                if (H1.ppo_coeff[i] != H2.ppo_coeff[i]) {
                    result = false;
                    break;
                }
            }
            for (int i = 0; i < 20; i++) {
                if (H1.amd_x_coeff[i] != H2.amd_x_coeff[i]) {
                    result = false;
                    break;
                }
                if (H1.amd_y_coeff[i] != H2.amd_y_coeff[i]) {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    // ========================================================================
    // Spherical Coordinate Transformations (Celestial <-> Native)
    // ========================================================================

    /**
     * Convert celestial coordinates to native spherical coordinates.
     * This is a convenience method that handles the transformation setup.
     *
     * @param lng    celestial longitude (RA) in degrees
     * @param lat    celestial latitude (Dec) in degrees
     * @param crval1 reference celestial longitude (CRVAL1) in degrees
     * @param crval2 reference celestial latitude (CRVAL2) in degrees
     * @return LonLat with phi and theta in degrees, or null if transformation fails
     */
    public static LonLat celestialToNative(double lng, double lat, double crval1, double crval2) {
        double[] celref = new double[4];
        double[] euler = new double[5];

        // Initialize projection parameters
        celref[0] = crval1;
        celref[1] = crval2;
        celref[2] = 999.0;
        celref[3] = 999.0;

        try {
            boolean success = celset(celref, euler, false);
            if (!success) return null;
        } catch (ProjectionException e) {
            return null;
        }

        return sphfwd(lng, lat, euler);
    }

    /**
     * Convert native spherical coordinates to celestial coordinates.
     * This is a convenience method that handles the transformation setup.
     *
     * @param phi    native longitude in degrees
     * @param theta  native latitude in degrees
     * @param crval1 reference celestial longitude (CRVAL1) in degrees
     * @param crval2 reference celestial latitude (CRVAL2) in degrees
     * @return LonLat with celestial longitude and latitude in degrees, or null if transformation fails
     */
    public static LonLat nativeToCelestial(double phi, double theta, double crval1, double crval2) {
        double[] celref = new double[4];
        double[] euler = new double[5];

        // Initialize projection parameters
        celref[0] = crval1;
        celref[1] = crval2;
        celref[2] = 999.0;
        celref[3] = 999.0;

        try {
            boolean success = celset(celref, euler, false);
            if (!success) return null;
        } catch (ProjectionException e) {
            return null;
        }

        return sphrev(phi, theta, euler);
    }

    /**
     * Initialize celestial transformation parameters.
     * Computes the Euler angles and celestial pole position needed for coordinate transformations.
     *
     * <p>Based on WCSLIB's celset function.</p>
     *
     * @param celref           array of [crval1, crval2, lonpole, latpole] - modified in place
     * @param euler            array of 5 elements for Euler angles - set by this method
     * @param useProjException if true, throw exceptions; if false, return false on error
     * @return true if successful, false if error (when useProjException is false)
     * @throws ProjectionException if useProjException is true and computation fails
     */
    private static boolean celset(double[] celref, double[] euler, boolean useProjException)
            throws ProjectionException {
        double tol = 1.0e-10;
        double clat0, cphip, cthe0, slat0, sphip, sthe0;
        double latp, latp1, latp2;
        double u, v, x, y, z;

        /* Compute celestial coordinates of the native pole. */

        /* Reference point away from the native pole. */

        /* Set default for longitude of the celestial pole. */
        if (celref[1] < 0.0)
            celref[2] = 180.0;
        else
            celref[2] = 0.0;

        clat0 = cosd(celref[1]);
        slat0 = sind(celref[1]);
        cphip = cosd(celref[2]);
        sphip = sind(celref[2]);
        cthe0 = 1.0;
        sthe0 = 0.0;

        x = cthe0 * cphip;
        y = sthe0;
        z = Math.sqrt(x * x + y * y);
        if (z == 0.0) {
            if (slat0 != 0.0) {
                if (useProjException) throw new ProjectionException("failure in spherical projection setup");
                else return false;
            }

            /* latp determined by LATPOLE in this case. */
            latp = celref[3];
        } else {
            if (Math.abs(slat0 / z) > 1.0) {
                if (useProjException) throw new ProjectionException("failure in spherical projection setup");
                else return false;
            }

            u = atan2d(y, x);
            v = acosd(slat0 / z);

            latp1 = u + v;
            if (latp1 > 180.0) {
                latp1 -= 360.0;
            } else if (latp1 < -180.0) {
                latp1 += 360.0;
            }

            latp2 = u - v;
            if (latp2 > 180.0) {
                latp2 -= 360.0;
            } else if (latp2 < -180.0) {
                latp2 += 360.0;
            }

            if (Math.abs(celref[3] - latp1) < Math.abs(celref[3] - latp2)) {
                if (Math.abs(latp1) < 90.0 + tol) {
                    latp = latp1;
                } else {
                    latp = latp2;
                }
            } else {
                if (Math.abs(latp2) < 90.0 + tol) {
                    latp = latp2;
                } else {
                    latp = latp1;
                }
            }

            celref[3] = latp;
        }

        euler[1] = 90.0 - latp;

        z = cosd(latp) * clat0;
        if (Math.abs(z) < tol) {
            if (Math.abs(clat0) < tol) {
                /* Celestial pole at the reference point. */
                euler[0] = celref[0];
                euler[1] = 90.0;
            } else if (latp > 0.0) {
                /* Celestial pole at the native North Pole.*/
                euler[0] = celref[0] + celref[2] - 180.0;
                euler[1] = 0.0;
            } else if (latp < 0.0) {
                /* Celestial pole at the native South Pole. */
                euler[0] = celref[0] - celref[2];
                euler[1] = 180.0;
            }
        } else {
            x = (sthe0 - sind(latp) * slat0) / z;
            y = sphip * cthe0 / clat0;
            if (x == 0.0 && y == 0.0) {
                if (useProjException) throw new ProjectionException("failure in spherical projection setup");
                else return false;
            }
            euler[0] = celref[0] - atan2d(y, x);
        }

        euler[2] = celref[2];
        euler[3] = cosd(euler[1]);
        euler[4] = sind(euler[1]);

        /* Check for ill-conditioned parameters. */
        if (Math.abs(latp) > 90.0 + tol) {
            if (useProjException) throw new ProjectionException("ill-conditioned parameters in spherical projection");
            else return false;
        }
        return true;
    }

    /**
     * Forward spherical transformation: celestial coordinates to native coordinates.
     *
     * <p>Based on WCSLIB's sphfwd function.</p>
     *
     * @param lng   celestial longitude in degrees
     * @param lat   celestial latitude in degrees
     * @param euler Euler angles from celset
     * @return LonLat with phi and theta in degrees
     */
    private static LonLat sphfwd(double lng, double lat, double[] euler) {
        double tol = 1.0e-5;
        double phi, theta;
        double coslat, coslng, dlng, dphi, sinlat, sinlng, x, y, z;

        coslat = cosd(lat);
        sinlat = sind(lat);

        dlng = lng - euler[0];
        coslng = cosd(dlng);
        sinlng = sind(dlng);

        /* Compute native coordinates. */
        x = sinlat * euler[4] - coslat * euler[3] * coslng;
        if (Math.abs(x) < tol) {
            /* Rearrange formula to reduce roundoff errors. */
            x = -cosd(lat + euler[1]) + coslat * euler[3] * (1.0 - coslng);
        }
        y = -coslat * sinlng;
        if (x != 0.0 || y != 0.0) {
            dphi = atan2d(y, x);
        } else {
            /* Change of origin of longitude. */
            dphi = dlng - 180.0;
        }
        phi = euler[2] + dphi;

        /* Normalize. */
        if (phi > 180.0) {
            phi -= 360.0;
        } else if (phi < -180.0) {
            phi += 360.0;
        }

        if (dlng % 180.0 == 0.0) {
            theta = lat + coslng * euler[1];
            if (theta > 90.0) theta = 180.0 - theta;
            if (theta < -90.0) theta = -180.0 - theta;
        } else {
            z = sinlat * euler[3] + coslat * euler[4] * coslng;
            if (Math.abs(z) > 0.99) {
                /* Use an alternative formula for greater numerical accuracy. */
                theta = acosd(Math.sqrt(x * x + y * y));
                if (z < 0.0)
                    theta = -Math.abs(theta);
                else
                    theta = Math.abs(theta);
            } else {
                theta = asind(z);
            }
        }

        return new LonLat(phi, theta);
    }

    /**
     * Reverse spherical transformation: native coordinates to celestial coordinates.
     * Based on WCSLIB's sphrev function.
     *
     * @param phi   native longitude in degrees
     * @param theta native latitude in degrees
     * @param euler Euler angles from celset
     * @return LonLat with celestial longitude and latitude in degrees
     */
    private static LonLat sphrev(double phi, double theta, double[] euler) {
        double tol = 1.0e-5;
        double lng, lat;
        double cosphi, costhe, dlng, dphi, sinphi, sinthe, x, y, z;

        costhe = cosd(theta);
        sinthe = sind(theta);

        dphi = phi - euler[2];
        cosphi = cosd(dphi);
        sinphi = sind(dphi);

        /* Compute celestial coordinates. */
        x = sinthe * euler[4] - costhe * euler[3] * cosphi;
        if (Math.abs(x) < tol) {
            /* Rearrange formula to reduce roundoff errors. */
            x = -cosd(theta + euler[1]) + costhe * euler[3] * (1.0 - cosphi);
        }
        y = -costhe * sinphi;
        if (x != 0.0 || y != 0.0) {
            dlng = atan2d(y, x);
        } else {
            /* Change of origin of longitude. */
            dlng = dphi + 180.0;
        }
        lng = euler[0] + dlng;

        /* Normalize the celestial longitude to [0, 360) */
        lng = lng % 360.0;
        if (lng < 0.0) lng += 360.0;
        /* Handle floating-point edge cases */
        final double epsilon = 1e-10;
        if (lng >= 360.0 - epsilon) lng = 0.0;
        if (Math.abs(lng) < epsilon) lng = 0.0;

        if (dphi % 180.0 == 0.0) {
            lat = theta + cosphi * euler[1];
            if (lat > 90.0) lat = 180.0 - lat;
            if (lat < -90.0) lat = -180.0 - lat;
        } else {
            z = sinthe * euler[3] + costhe * euler[4] * cosphi;
            if (Math.abs(z) > 0.99) {
                /* Use an alternative formula for greater numerical accuracy. */
                lat = acosd(Math.sqrt(x * x + y * y));
                if (z < 0.0)
                    lat = -Math.abs(lat);
                else
                    lat = Math.abs(lat);
            } else {
                lat = asind(z);
            }
        }

        return new LonLat(lng, lat);
    }

    // ========================================================================
    // Trigonometric Helper Functions (degree-based)
    // ========================================================================

    private static double cosd(double angle) {
        return Math.cos(angle * dtr);
    }

    private static double sind(double angle) {
        return Math.sin(angle * dtr);
    }

    private static double acosd(double v) {
        if (v >= 1.0) {
            if (v - 1.0 < WCSTRIG_TOL) return 0.0;
        } else if (v == 0.0) {
            return 90.0;
        } else if (v <= -1.0) {
            if (v + 1.0 > -WCSTRIG_TOL) return 180.0;
        }

        return Math.acos(v) * rtd;
    }

    private static double asind(double v) {
        if (v <= -1.0) {
            if (v + 1.0 > -WCSTRIG_TOL) return -90.0;
        } else if (v == 0.0) {
            return 0.0;
        } else if (v >= 1.0) {
            if (v - 1.0 < WCSTRIG_TOL) return 90.0;
        }

        return Math.asin(v) * rtd;
    }

    private static double atan2d(double y, double x) {
        if (y == 0.0) {
            if (x >= 0.0) {
                return 0.0;
            } else if (x < 0.0) {
                return 180.0;
            }
        } else if (x == 0.0) {
            if (y > 0.0) {
                return 90.0;
            } else if (y < 0.0) {
                return -90.0;
            }
        }

        return Math.atan2(y, x) * rtd;
    }

}
