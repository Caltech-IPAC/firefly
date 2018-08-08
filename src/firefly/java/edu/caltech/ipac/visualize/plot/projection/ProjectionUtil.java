/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

package edu.caltech.ipac.visualize.plot.projection;
/**
 * User: roby
 * Date: 7/17/18
 * Time: 1:20 PM
 */


import edu.caltech.ipac.visualize.plot.plotdata.FitsRead;
import edu.caltech.ipac.visualize.plot.ImageHeader;
import nom.tam.fits.FitsException;

/**
 * @author Trey Roby
 */
public class ProjectionUtil {

    public static boolean isSameProjection(FitsRead firstFitsRead, FitsRead secondFitsread) {
        boolean result = false;

        if (firstFitsRead.getProjectionType()== secondFitsread.getProjectionType()) {
            try {
                ImageHeader H1 = new ImageHeader(firstFitsRead.getHeader());
                ImageHeader H2 = new ImageHeader(secondFitsread.getHeader());
                if (H1.maptype == Projection.PLATE) {
                    result = checkPlate(H1, H2);
                } else {
                    result = checkOther(H1, H2);
                }
            } catch (FitsException e) {
                result= false;
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

}
