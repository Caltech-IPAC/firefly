/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {celset,sphfwd,sphrev,DtoR,RtoD} from './ProjectionUtil.js';
import {makeProjectionPt, makeImagePt} from '../Point.js';


export const CylindricalProjection= {

    revProject (lon, lat, hdr, useProjException= false) {
        var fline, fsamp;
        const celref= [];
        const euler= [];

        const {crpix1, crpix2, cdelt1, cdelt2, dc1_1, dc1_2, dc2_1, dc2_2, using_cd}= hdr;
        const glong = hdr.crval1;
        const glat = hdr.crval2;
        const twist = hdr.crota2;

        celref[0] = glong;
        celref[1] = glat;
        celref[2] = 999.0;
        celref[3] = 999.0;

        const celsetSuccess = celset(celref,euler,useProjException);
        if (!celsetSuccess && !useProjException)  return null;

        var result = sphfwd(lon, lat, euler);
        var xx = result[0];

        var yy = Math.sin(lat * DtoR) * 180.0 / Math.PI;

        if (using_cd) {
            fsamp = dc1_1 * xx + dc1_2 * yy;
            fline = dc2_1 * xx + dc2_2 * yy;
        }
        else {
            fsamp = xx / cdelt1;
            fline = yy / cdelt2;
        }

        // do the twist
        const rtwist = -twist * DtoR;       // convert to radians
        const temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
        fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
        fsamp = temp;

        const x = fsamp + crpix1 - 1;
        const y = fline + crpix2 - 1;

        return makeImagePt(x, y);
    },

    fwdProject(x, y, hdr, useProjException= false) {
        var xx, yy;
        const celref= [];
        const euler= [];

        const {crpix1, crpix2, cdelt1, cdelt2, cd1_1, cd1_2, cd2_1, cd2_2, using_cd}= hdr;
        const glong = hdr.crval1;
        const glat = hdr.crval2;
        const twist = hdr.crota2;
        
        var fsamp = x - crpix1 + 1;
        var fline = y - crpix2 + 1;


        const rtwist = -twist * DtoR;       // convert to radians */
        const temp = fsamp * Math.cos(rtwist) - fline * Math.sin(rtwist);
        // do twist
        fline = fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
        fsamp = temp;

        if (using_cd) {
            xx = (cd1_1 * fsamp + cd1_2 * fline);
            yy = (cd2_1 * fsamp + cd2_2 * fline);
        }
        else {
            xx = fsamp * cdelt1;
            yy = fline * cdelt2;
        }
        // Initialize projection parameters. 
        // Set reference angles for the native grid. 
        celref[0] = glong;
        celref[1] = glat;
        celref[2] = 999.0;
        celref[3] = 999.0;

        const celsetSuccess = celset(celref,euler, useProjException);
        if (!celsetSuccess && !useProjException)  return null;

        yy = Math.asin(yy * DtoR) * RtoD;

        const result = sphrev(xx, yy, euler);
        var lon = result[0];
        var lat = result[1];

        if (lon < 0.0) {
            lon += 360.0;
        }

        return makeProjectionPt(lon, lat);
    }
};
