/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
// TEST WITH: DSS
import {DtoR,RtoD,computeDistance} from './ProjectionUtil.js';
import {makeProjectionPt, makeImagePt} from '../Point.js';


/**
 * This file is a translation of a file from CfA
 * The original comments remain 
 * 
 * File saoimage/wcslib/platepos.c
 * May 4, 1995
 * By Doug Mink, Harvard-Smithsonian Center for Astrophysics

 * Compute WCS from Digital Sky Survey plate fit

 platepos() converts from pixel location to RA,Dec 
 platexy()  converts from RA,Dec         to pixel location   

 where "(RA,Dec)" are more generically (long,lat). These functions
 are based on the astrmcal.c portion of GETIMAGE, which is distributed
 with the Digital Sky Survey.
 */


const ARCSECONDS_PER_RADIAN = 206264.8062470964;


export const PlateProjection = {

    /**
     * Routine to determine accurate position for pixel coordinates
     * returns 0 if successful otherwise 1 = angle too large for projection;
     * based on amdpos() from getimage 
     * 
     * @param xpix x pixel number  (RA or long without rotation) 
     * @param ypix y pixel number  (dec or lat without rotation) 
     * @param hdr
     */
    fwdProject(xpix, ypix, hdr) {

        const cond2r = Math.PI / 180;
        const cons2r = 3600 * 180 / Math.PI;
        const twopi = 2 * Math.PI;
        
        const {amd_x_coeff, amd_y_coeff, ppo_coeff, x_pixel_size,
               crpix1, crpix2, y_pixel_size, plate_ra, plate_dec }= hdr;

        /*  Ignore magnitude and color terms 
         double mag = 0.0;
         double color = 0.0; */

        /* Convert from pixels to millimeters */

        /*
         x = xpix + hdr.x_pixel_offset - 1;
         y = ypix + hdr.y_pixel_offset - 1;
         */
        /*
         x = xpix;
         y = ypix;
         */
        /* use the following if Trey hands me IRAF coordinates */
        /*
         x = xpix - hdr.crpix1;
         y = ypix - hdr.crpix2;
         */

        // use the following if Trey hands me SC coordinates 
        const x = xpix - crpix1 + 1;
        const y = ypix - crpix2 + 1;

        // x and y are actually fsamp and fline  

        const xmm = (ppo_coeff[2] - x * x_pixel_size) / 1000.0;
        const ymm = (y * y_pixel_size - ppo_coeff[5]) / 1000.0;
        const xmm2 = xmm * xmm;
        const ymm2 = ymm * ymm;
        const xmm3 = xmm * xmm2;
        const ymm3 = ymm * ymm2;
        const x2y2 = xmm2 + ymm2;

        //  Compute coordinates from x,y and plate model 

        const xi = amd_x_coeff[0] * xmm + amd_x_coeff[1] * ymm +
            amd_x_coeff[2] + amd_x_coeff[3] * xmm2 +
            amd_x_coeff[4] * xmm * ymm + amd_x_coeff[5] * ymm2 +
            amd_x_coeff[6] * (x2y2) + amd_x_coeff[7] * xmm3 +
            amd_x_coeff[8] * xmm2 * ymm + amd_x_coeff[9] * xmm * ymm2 +
            amd_x_coeff[10] * ymm3 + amd_x_coeff[11] * xmm * (x2y2) +
            amd_x_coeff[12] * xmm * x2y2 * x2y2;

        /*  Ignore magnitude and color terms 
         + hdr.amd_x_coeff[13]*mag	+ hdr.amd_x_coeff[14]*mag*mag +
         hdr.amd_x_coeff[15]*mag*mag*mag + hdr.amd_x_coeff[16]*mag*xmm +
         hdr.amd_x_coeff[17]*mag*x2y2	+ hdr.amd_x_coeff[18]*mag*xmm*x2y2 +
         hdr.amd_x_coeff[19]*color; */

        const eta = amd_y_coeff[0] * ymm + amd_y_coeff[1] * xmm +
            amd_y_coeff[2] + amd_y_coeff[3] * ymm2 +
            amd_y_coeff[4] * xmm * ymm + amd_y_coeff[5] * xmm2 +
            amd_y_coeff[6] * (x2y2) + amd_y_coeff[7] * ymm3 +
            amd_y_coeff[8] * ymm2 * xmm + amd_y_coeff[9] * ymm * xmm2 +
            amd_y_coeff[10] * xmm3 + amd_y_coeff[11] * ymm * (x2y2) +
            amd_y_coeff[12] * ymm * x2y2 * x2y2;

        /*  Ignore magnitude and color terms 
         + hdr.amd_y_coeff[13]*mag	+ hdr.amd_y_coeff[14]*mag*mag +
         hdr.amd_y_coeff[15]*mag*mag*mag + hdr.amd_y_coeff[16]*mag*ymm +
         hdr.amd_y_coeff[17]*mag*x2y2)	+ hdr.amd_y_coeff[18]*mag*ymm*x2y2 +
         hdr.amd_y_coeff[19]*color; */

        // Convert to radians 

        const xir = xi / cons2r;
        const etar = eta / cons2r;

        // Convert to RA and Dec 

        const ctan = Math.tan(plate_dec);
        const ccos = Math.cos(plate_dec);
        const raoff = Math.atan2(xir / ccos, 1.0 - etar * ctan);
        var ra = raoff + plate_ra;
        if (ra < 0.0) ra = ra + twopi;
        const xpos = ra / cond2r;

        const dec = Math.atan(Math.cos(raoff) * ((etar + ctan) / (1.0 - (etar * ctan))));
        const ypos = dec / cond2r;

        return makeProjectionPt(xpos, ypos);
    },

    /* Mar  6 1995	Original version of this code
     May  4 1995	Fix eta cross terms which were all in y
     */








    /******************************************************************************
     * C Source: Astrometric Calibration                                           *
     *                                                                             *
     * File:                                                                       *
     *    astrmcal.c                                                               *
     *                                                                             *
     * Description:                                                                *
     *    This set of functions are used for applying the astrometric of a plate   *
     *    to ultimately compute the x-y position on a plate from a given right     *
     *    ascension and declination. These functions were either originally        *
     *    written, or ported to C by R. White.                                     *
     *                                                                             *
     * Module List:                                                                *
     *    amdinv                                                                   *
     *       Computes x-y position on a plate using CALOBCC solution.              *
     *    amdpos                                                                   *
     *       Routine to convert x,y to RA,Dec using the CALOBCC solution.          *
     *    pltmodel                                                                 *
     *       Computes plate model and it's partial derivatives.                    *
     *    ppoinv                                                                   *
     *       Computes x-y position on a plate using PDS orientation solution.      *
     *    xypos                                                                    *
     *       Computes x-y from right ascension and declination.                    *
     *                                                                             *
     * History:                                                                    *
     *    Created    : 13-AUG-93 by J. Doggett,                                    *
     *       for the Association of Universities for Research in Astronomy, Inc.   *
     *          at the Space Telescope Science Institute,                          *
     *       to facilitate the accessing of data from the CD-ROM set of the        *
     *          Digitized Sky Survey.                                              *
     *    Delivered  : 01-MAR-94 by J. Doggett,                                    *
     *       GetImage Version 1.0                                                  *
     *                                                                             *
     *    Updated    : 01-AUG-94 by J. Doggett,                                    *
     *       Updated pltmodel function.                                            *
     *    Redelivered: 30-NOV-94 by J. Doggett,                                    *
     *       GetImage Version 1.1.                                                 *
     *                                                                             *
     * Copyright (c) 1993, 1994, Association of Universities for Research in       *
     * Astronomy, Inc.  All Rights Reserved.                                       *
     * Produced under National Aeronautics and Space Administration grant          *
     * NAG W-2166.                                                                 *
     ******************************************************************************/


    /**
     * ****************************************************************************
     * Module:                                                                     *
     *    amdinv                                                                   *
     *                                                                             *
     * File:                                                                       *
     *    astrmcal.c                                                               *
     *                                                                             *
     * Description:                                                                *
     *    Computes the x,y position on the plate from the right ascension and      *
     *    declination using the inverse of the CALOBCC solution. Newton's method   *
     *    is used to iterate from a starting position until convergence is         *
     *    reached.                                                                 *
     *                                                                             *
     * Return Value:                                                               *
     *    none                                                                     *
     *                                                                             *
     * Arguments:                                                                  *
     *                                                                             *
     * @param ra The right ascension of the coordinates to convert to x-y.        *
     * @param dec The declination of the coordinates to convert to x-y.           *
     * @param hdr HEADER, Reference                                               *
     *             Structure with header information.                              *
     *       mag                                                                   *
     *          float, Value                                                       *
     *             Magnitude for the calibration solution.                         *
     *       col                                                                   *
     *          float, Value                                                       *
     *             Color for the calibration solution.                             *
     *    Output Arguments:                                                        *
     *       x                                                                     *
     *          float, Reference                                                   *
     *             Result x position.                                              *
     *       y                                                                     *
     *          float, Reference                                                   *
     *             Result y position.                                              *
     *                                                                             *
     * History:                                                                    *
     *    Created    : 07-MAY-90 by R. White,                                      *
     *       Converted from GASP IDL routine to Fortran.                           *
     *    Delivered  : 30-JUL-91 by R. White,                                      *
     *       Converted from Fortran to C.                                          *
     *                                                                             *
     *    Updated    : 13-AUG-93 by J. Doggett,                                    *
     *       Adopted into software used to facilitate the accessing of data from   *
     *          the CD-ROM set of the Digitized Sky Survey.                        *
     *    Updated    : 14-DEC-93 by J. Doggett,                                    *
     *       Modified to reflect change in calling sequence to traneqstd function. *
     *    Redelivered: 01-MAR-94 by J. Doggett,                                    *
     *       GetImage Version 1.0                                                  *
     *******************************************************************************
     * @param useProjException when the is a failure: true throw exception, false return null
     */
    revProject (ra, dec, hdr, useProjException= false) {
        var x, y;
        var colour, mag;
        var i;
        var object_x, object_y, f, fx, fy, g, gx, gy;
        var cjunk, x4, y4;

        const {plt_scale, amd_x_coeff, amd_y_coeff, ppo_coeff, 
               x_pixel_size, crpix1, crpix2, y_pixel_size, plate_ra, plate_dec }= hdr;

        // avoid mirror image at opposite point on sphere 
        const distance = computeDistance(ra, dec, plate_ra * RtoD, plate_dec * RtoD);
        if (distance > 90.0) {
            if (useProjException) throw new Error('coordinates not on image');
            else return null;
        }

        ra = ra * DtoR;
        dec = dec * DtoR;


        // Initialize
        const max_iterations = 50;
        const tolerance = 0.0000005;
        var delta_x = tolerance;
        var delta_y = tolerance;

        // Convert RA and Dec to St.coords
        var pt = traneqstd(plate_ra, plate_dec, ra, dec);
        const xi = pt.x;
        const eta = pt.y;

        // Set initial value for x,y

        object_x = xi / plt_scale;
        object_y = eta / plt_scale;


        // Iterate by Newtons method
        for (i = 0; i < max_iterations; i++) {

            //pltmodel(imgp,object_x,object_y,mag,col,&f,&fx,&fy,&g,&gx,&gy);
            x = object_x;
            y = object_y;
            colour = 0.0;
            mag = 0.0;
            // X plate model

            cjunk = (x * x + y * y) * (x * x + y * y);
            x4 = (x * x) * (x * x);
            y4 = (y * y) * (y * y);
            f = amd_x_coeff[0] * x + amd_x_coeff[1] * y +
                amd_x_coeff[2] + amd_x_coeff[3] * x * x +
                amd_x_coeff[4] * x * y + amd_x_coeff[5] * y * y +
                amd_x_coeff[6] * (x * x + y * y) + amd_x_coeff[7] * x * x * x +
                amd_x_coeff[8] * x * x * y + amd_x_coeff[9] * x * y * y +
                amd_x_coeff[10] * y * y * y + amd_x_coeff[11] * x * (x * x + y * y) +
                amd_x_coeff[12] * x * cjunk + amd_x_coeff[13] * mag +
                amd_x_coeff[14] * mag * mag + amd_x_coeff[15] * mag * mag * mag +
                amd_x_coeff[16] * mag * x + amd_x_coeff[17] * mag * (x * x + y * y) +
                amd_x_coeff[18] * mag * x * (x * x + y * y) + amd_x_coeff[19] * colour;

            // Derivative of X model wrt x

            fx = amd_x_coeff[0] +
                amd_x_coeff[3] * 2.0 * x +
                amd_x_coeff[4] * y +
                amd_x_coeff[6] * 2.0 * x +
                amd_x_coeff[7] * 3.0 * x * x +
                amd_x_coeff[8] * 2.0 * x * y +
                amd_x_coeff[9] * y * y +
                amd_x_coeff[11] * (3.0 * x * x + y * y) +
                amd_x_coeff[12] * (5.0 * x4 + 6.0 * x * x * y * y + y4  ) +
                amd_x_coeff[16] * mag +
                amd_x_coeff[17] * mag * 2.0 * x +
                amd_x_coeff[18] * mag * (3.0 * x * x + y * y);

            // Derivative of X model wrt y

            fy = amd_x_coeff[1] +
                amd_x_coeff[4] * x +
                amd_x_coeff[5] * 2.0 * y +
                amd_x_coeff[6] * 2.0 * y +
                amd_x_coeff[8] * x * x +
                amd_x_coeff[9] * x * 2.0 * y +
                amd_x_coeff[10] * 3.0 * y * y +
                amd_x_coeff[11] * 2.0 * x * y +
                amd_x_coeff[12] * 4.0 * x * y * (x * x + y * y) +

                /******************
                 * pltmodel page 3 *
                 ******************/

                amd_x_coeff[17] * mag * 2.0 * y +
                amd_x_coeff[18] * mag * 2.0 * x * y;

            // Y plate model

            g = amd_y_coeff[0] * y + amd_y_coeff[1] * x +
                amd_y_coeff[2] + amd_y_coeff[3] * y * y +
                amd_y_coeff[4] * y * x + amd_y_coeff[5] * x * x +
                amd_y_coeff[6] * (x * x + y * y) + amd_y_coeff[7] * y * y * y +
                amd_y_coeff[8] * y * y * x + amd_y_coeff[9] * y * x * x +
                amd_y_coeff[10] * x * x * x + amd_y_coeff[11] * y * (x * x + y * y) +
                amd_y_coeff[12] * y * cjunk + amd_y_coeff[13] * mag +
                amd_y_coeff[14] * mag * mag + amd_y_coeff[15] * mag * mag * mag +
                amd_y_coeff[16] * mag * y + amd_y_coeff[17] * mag * (x * x + y * y) +
                amd_y_coeff[18] * mag * y * (x * x + y * y) + amd_y_coeff[19] * colour;

            // Derivative of Y model wrt x

            gx = amd_y_coeff[1] +
                amd_y_coeff[4] * y +
                amd_y_coeff[5] * 2.0 * x +
                amd_y_coeff[6] * 2.0 * x +
                amd_y_coeff[8] * y * y +
                amd_y_coeff[9] * y * 2.0 * x +
                amd_y_coeff[10] * 3.0 * x * x +
                amd_y_coeff[11] * 2.0 * x * y +
                amd_y_coeff[12] * 4.0 * x * y * (x * x + y * y) +
                amd_y_coeff[17] * mag * 2.0 * x +
                amd_y_coeff[18] * mag * y * 2.0 * x;

            // Derivative of Y model wrt y

            gy = amd_y_coeff[0] +
                amd_y_coeff[3] * 2.0 * y +
                amd_y_coeff[4] * x +
                amd_y_coeff[6] * 2.0 * y +
                amd_y_coeff[7] * 3.0 * y * y +
                amd_y_coeff[8] * 2.0 * y * x +
                amd_y_coeff[9] * x * x +
                amd_y_coeff[11] * (x * x + 3.0 * y * y) +
                amd_y_coeff[12] * (5.0 * y4 + 6.0 * x * x * y * y + x4  ) +
                amd_y_coeff[16] * mag +
                amd_y_coeff[17] * mag * 2.0 * y +
                amd_y_coeff[18] * mag * (x * x + 3.0 * y * y);

            f = f - xi;
            g = g - eta;
            delta_x = (-f * gy + g * fy) / (fx * gy - fy * gx);
            delta_y = (-g * fx + f * gx) / (fx * gy - fy * gx);
            object_x = object_x + delta_x;
            object_y = object_y + delta_y;
            if ((Math.abs(delta_x) < tolerance) && (Math.abs(delta_y) < tolerance)) break;
        }

        // Convert mm from plate center to pixels

        const xout = (ppo_coeff[2] - object_x * 1000.0) / x_pixel_size;
        const yout = (ppo_coeff[5] + object_y * 1000.0) / y_pixel_size;

        // RBH added 

        /*
         xout = xout + 1 - _hdr.x_pixel_offset;
         yout = yout + 1 - _hdr.y_pixel_offset;
         */

        x = xout + crpix1 - 1;
        y = yout + crpix2 - 1;

        // end RBH added

        return makeImagePt(x, y);
    }
};

/******************************************************************************
* Module:                                                                     *
*    pltmodel                                                                 *
*                                                                             *
* File:                                                                       *
*    astrmcal.c                                                               *
*                                                                             *
* Description:                                                                *
*    Computes values of a plate model and its parital derivatives for use in  *
*    computing inverse astrometric solutions.                                 *
*                                                                             *
* Return Value:                                                               *
*    none                                                                     *
*                                                                             *
* Arguments:                                                                  *
*                                                                             *
*    Input Arguments:                                                         *
*       h                                                                     *
*          HEADER, Reference                                                  *
*             Structure with header information.                              *
*       x                                                                     *
*          double, Value                                                      *
*             X pixel position.                                               *
*       y                                                                     *
*          double, Value                                                      *
*             Y pixel position.                                               *
*       mag                                                                   *
*          double, Value                                                      *
*             Magnitude                                                       *
*       color                                                                 *
*          double, Value                                                      *
*             Color                                                           *
*    Input/Output Arguments:                                                  *
*       none                                                                  *
*    Output Arguments:                                                        *
*       f                                                                     *
*          double, Reference                                                  *
*             Xi standard coordinate.                                         *
*       fx                                                                    *
*          double, Reference                                                  *
*             Derivative of xi with respect to x.                             *
*       fy                                                                    *
*          double, Reference                                                  *
*             Derivative of xi with respect to y.                             *
*       g                                                                     *
*          double, Reference                                                  *
*             eta standard coordinate.                                        *
*       gx                                                                    *
*          double, Reference                                                  *
*             Derivative of eta with respect to x.                            *
*       gy                                                                    *
*          double, Reference                                                  *
*             Derivative of eta with respect to y.                            *
*                                                                             *
* History:                                                                    *
*    Created    : 07-MAY-90 by R. White,                                      *
*       Converted from GASP IDL routine to Fortran.                           *
*    Delivered  : 30-JUL-91 by R. White,                                      *
*       Converted from Fortran to C.                                          *
*                                                                             *
*    Updated    : 13-AUG-93 by J. Doggett,                                    *
*       Adopted into software used to facilitate the accessing of data from   *
*          the CD-ROM set of the Digitized Sky Survey.                        *
*******************************************************************************

*******************************************************************************
*    Redelivered: 01-MAR-94 by J. Doggett,                                    *
*       GetImage Version 1.0                                                  *
*                                                                             *
*    Updated    : 01-AUG-94 by J. Doggett,                                    *
*       Corrected error in term [11] of the derivative of Y model wrt y.      *
*    Redelivered: 30-NOV-94 by J. Doggett,                                    *
*       GetImage Version 1.1.                                                 *
******************************************************************************/

/******************************************************************************
* Module:                                                                     *
*    traneqstd                                                                *
*                                                                             *
* File:                                                                       *
*    astronmy.c                                                               *
*                                                                             *
* Description:                                                                *
*    Routine to convert RA and Dec in radians to standard coordinates on a    *
*    plate.                                                                   *
*                                                                             *
* Return Value:                                                               *
*    none                                                                     *
*                                                                             *
* Arguments:                                                                  *
*                                                                             *
*    Input Arguments:                                                         *
*       plt_center_ra                                                         *
*          double, Value                                                      *
*             Plate center right ascension in radians                         *
*       plt_center_dec                                                        *
*          double, Value                                                      *
*             Plate center declination in radians                             *
*       object_ra                                                             *
*          double, Value                                                      *
*             Input right ascension in radians.                               *
*       object_dec                                                            *
*          double, Value                                                      *
*             Input declination in radians.                                   *
*    Input/Output Arguments:                                                  *
*       none                                                                  *
*    Output Arguments:                                                        *
*       Object_Xi                                                             *
*          double, Reference                                                  *
*             Output xi standard coordinate in arcseconds.                    *
*       Object_Eta                                                            *
*          double, Reference                                                  *
*             Output eta standard coordinate in arcseconds.                   *
*                                                                             *
* History:                                                                    *
*    Created    : 07-MAY-90 by R. White,                                      *
*       Converted to Fortran from IDL routine.                                *
*    Delivered  : 30-JUL-91 by R. White,                                      *
*       Convert from Fortran to C.                                            *
*                                                                             *
*    Updated    : 13-AUG-93 by J. Doggett,                                    *
*       Adopted into software used to facilitate the accessing of data from   *
*          the CD-ROM set of the Digitized Sky Survey.                        *
*    Updated    : 14-DEC-93 by J. Doggett,                                    *
*       Modified to take plate center coordinates explicitly as arguments     *
*          rather than from the plate header.                                 *
*    Redelivered: 01-MAR-94 by J. Doggett,                                    *
*       GetImage Version 1.0                                                  *
******************************************************************************/
function traneqstd (plt_center_ra, plt_center_dec, object_ra, object_dec) {
    
     // Find divisor

    const div=(Math.sin(object_dec)*Math.sin(plt_center_dec)+
        Math.cos(object_dec)*Math.cos(plt_center_dec)*
        Math.cos(object_ra - plt_center_ra));
    
     // Compute standard coords and convert to arcsec
   
    const object_xi=Math.cos(object_dec)*Math.sin(object_ra - plt_center_ra)*
        ARCSECONDS_PER_RADIAN/div;

    const object_eta=(Math.sin(object_dec)*Math.cos(plt_center_dec)-
        Math.cos(object_dec)*Math.sin(plt_center_dec)*
        Math.cos(object_ra - plt_center_ra))*
        ARCSECONDS_PER_RADIAN/div;

    const pt = makeProjectionPt(object_xi, object_eta);
    return pt;

} /* traneqstd */

