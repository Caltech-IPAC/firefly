package edu.caltech.ipac.visualize.plot.projection;



import java.io.Serializable;


public class ProjectionParams implements Serializable {


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
    public double a[][] = new double[5][5];
    public double ap[][] = new double[5][5];
    public double b[][] = new double[5][5];
    public double bp[][] = new double[5][5];
    public boolean map_distortion = false;
    public String keyword;

    public ProjectionParams() {}
}
/*
* THIS SOFTWARE AND ANY RELATED MATERIALS WERE CREATED BY THE CALIFORNIA
* INSTITUTE OF TECHNOLOGY (CALTECH) UNDER A U.S. GOVERNMENT CONTRACT WITH
* THE NATIONAL AERONAUTICS AND SPACE ADMINISTRATION (NASA). THE SOFTWARE
* IS TECHNOLOGY AND SOFTWARE PUBLICLY AVAILABLE UNDER U.S. EXPORT LAWS
* AND IS PROVIDED AS-IS TO THE RECIPIENT WITHOUT WARRANTY OF ANY KIND,
* INCLUDING ANY WARRANTIES OF PERFORMANCE OR MERCHANTABILITY OR FITNESS FOR
* A PARTICULAR USE OR PURPOSE (AS SET FORTH IN UNITED STATES UCC 2312-2313)
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
