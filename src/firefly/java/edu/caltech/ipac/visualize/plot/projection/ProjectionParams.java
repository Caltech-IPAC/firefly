/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.visualize.plot.projection;



import java.io.Serializable;


public class ProjectionParams implements Serializable {
    public static final int MAX_SIP_LENGTH = 10;


    // If you modify this class please modify -  edu.caltech.ipac.firefly.visualize.ProjectionSerializer
    // which know how to serialize this class for GWT

    public int bitpix, naxis, naxis1, naxis2, naxis3;
    public double crpix1, crpix2, crval1, crval2, cdelt1, cdelt2, crota2;
    public double crota1;
    public double file_equinox;
    public String ctype1;
    public String ctype2;
    public String radecsys;
    public double datamax, datamin;
    //public double bscale, bzero;
    //public String bunit;
    //public double blank_value;
    public int maptype;
    public double cd1_1, cd1_2, cd2_1, cd2_2;
    public double dc1_1, dc1_2, dc2_1, dc2_2;
    public boolean using_cd = false;

    /* the following are for PLATE projection */
    public double plate_ra, plate_dec;
    public double x_pixel_offset, y_pixel_offset;
    public double x_pixel_size, y_pixel_size;
    public double plt_scale;
    public double ppo_coeff[], amd_x_coeff[], amd_y_coeff[];

    /* the following are for SIRTF distortion corrections to the */
    /* GNOMONIC projection (ctype1 ending in -SIP)*/
    public double a_order, ap_order, b_order, bp_order;
    public double a[][] = new double[MAX_SIP_LENGTH][MAX_SIP_LENGTH];
    public double ap[][] = new double[MAX_SIP_LENGTH][MAX_SIP_LENGTH];
    public double b[][] = new double[MAX_SIP_LENGTH][MAX_SIP_LENGTH];
    public double bp[][] = new double[MAX_SIP_LENGTH][MAX_SIP_LENGTH];
    public boolean map_distortion = false;
    public String keyword;

    public ProjectionParams() {}
}
