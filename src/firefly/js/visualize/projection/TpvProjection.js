/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {DtoR,RtoD} from './ProjectionUtil.js';
import {makeProjectionPt,makeImagePt} from '../Point.js';

const DEF_PV1= [0,1];
const DEF_PV2= [0,1];
DEF_PV1.length=40;
DEF_PV2.length=40;

DEF_PV1.fill(0,2,39);
DEF_PV2.fill(0,2,39);
/*
 See https://fits.gsfc.nasa.gov/registry/tpvwcs/tpv.html
 http://www.atnf.csiro.au/people/mcalabre/WCS/ccs.pdf
 */
export const TpvProjection= {

    revProject (ra, dec, hdr) {
		let fline, fsamp, rtwist, temp;

		const {crpix1,crpix2,cdelt1,cdelt2, dc1_1, dc1_2, dc2_1, dc2_2, using_cd, pv1= DEF_PV1, pv2=DEF_PV2}= hdr;
		const glong  = hdr.crval1;
		const glat   = hdr.crval2;

        // Transform WC Ra,dec to intermediate coordinate (tan plane)


        const twist = hdr.crota2;

        const lon = ra * DtoR;
        const lat = dec * DtoR;

        const rpp1 = -cdelt1 * DtoR;
        const rpp2 = -cdelt2 * DtoR;

        const lon0 = glong * DtoR;
        const lat0 = glat * DtoR;

        const aa = Math.cos(lat) * Math.cos(lon - lon0);
        const ff1 = 1. / (Math.sin(lat0) * Math.sin(lat) + aa * Math.cos(lat0));
        const ff2 = 1. / (Math.sin(lat0) * Math.sin(lat) + aa * Math.cos(lat0));

        if (ff1 < 0) {
            /* we're more than 90 degrees from projection center */
            return null;
        } else {
            fline = -ff2 * (Math.cos(lat0) * Math.sin(lat) - aa * Math.sin(lat0));
            fsamp = -ff1 * Math.cos(lat) * Math.sin(lon - lon0);
        }

        // Recover uncorrected-intermediate coordinate before TPV distortion

        const axis1poly = pv1;
        const axis2poly = pv2;
        let X = axis1poly[0];
        let Y = axis2poly[0];
        let dx, dy;
        let xx  = 0;
        let yy = 0;
        const niter = 20; //Seems that after 4 is already enough but this is a rule of thumb.
        let iter = 0;
        let m1, m2, m3, m4;

        while (iter < niter) {
            iter++;
            let r;

            if ((xx === 0.0) && (yy === 0.0)) r = 1;
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
            const det = m1 * m4 - m2 * m3;
            const tmp = m4 / det;
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
            //   X  *= DtoR ;
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
        fsamp = xx;
        fline = yy;
        if (using_cd) {
            temp = -(dc1_1 * fsamp + dc1_2 * fline) * RtoD;
            fline = -(dc2_1 * fsamp + dc2_2 * fline) * RtoD;
            fsamp = temp;
        } else {
            /* do the twist */
            rtwist = twist * DtoR;       /* convert to radians */
            temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
            fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
            fsamp = temp;

            fsamp = (fsamp / rpp1);     /* now apply cdelt */
            fline = (fline / rpp2);
        }

        const x = fsamp + crpix1 - 1;
        const y = fline + crpix2 - 1;
		return makeImagePt(x, y);
	},

	fwdProject( px, py, hdr) {
        let lat, lon;
        let x, y; //Intermediate coords undistortioned

        const {crpix1,crpix2,cdelt1,cdelt2, cd1_1, cd1_2, cd2_1, cd2_2, using_cd,
		       pv1= DEF_PV1, pv2=DEF_PV2}= hdr;
		const glong  = hdr.crval1;
		const glat   = hdr.crval2;
        const twist  = hdr.crota2;

        // the intermediate coordinates offset from the distortion-center origin
        const fsamp = px - crpix1 + 1;
        const fline = py - crpix2 + 1;

        //Distortion is applied to intermediate (tangent) world coordinates so lets calculate those
        // by inverting cd matrix
        if (using_cd) {
            x = -(cd1_1 * fsamp + cd1_2 * fline) * DtoR;
            y = -(cd2_1 * fsamp + cd2_2 * fline) * DtoR;
        }
        else {
            const rpp1 = cdelt1 * DtoR;        // radians per pixel
            const rpp2 = cdelt2 * DtoR;        // radians per pixel
            x = -fsamp * rpp1;
            y = -fline * rpp2;

            const rtwist = twist * DtoR;       // convert to radians
            const temp = x * Math.cos(rtwist) - y * Math.sin(rtwist); // do twist
            y = x * Math.sin(rtwist) + y * Math.cos(rtwist);
            x = temp;
        }

        // Apply PV distortion
        const xy = distortion(x, y, pv1, pv2);

        //Intermediate coords distorsioned
        const {xprime, yprime} = xy;
        const xx = xprime;
        let yy = yprime;
        const delta = Math.atan(Math.sqrt(xx * xx + yy * yy));

        if ((xx===0.0) && (yy===0.0)) yy = 1.0;  // avoid domain error in atan2
        const beta = Math.atan2(-xx, yy);
        const glatr = glat * DtoR;
        const glongr = glong * DtoR;
        lat = Math.asin(-Math.sin(delta) * Math.cos(beta) * Math.cos(glatr) + Math.cos(delta) * Math.sin(glatr));
        const xxx = Math.sin(glatr) * Math.sin(delta) * Math.cos(beta) + Math.cos(glatr) * Math.cos(delta);
        const yyy = Math.sin(delta) * Math.sin(beta);
        lon = glongr + Math.atan2(yyy, xxx);

        lat = lat * RtoD;
        lon = lon * RtoD;

		return makeProjectionPt(lon, lat);
	},



};

/**
 * 	Correct projection plane coordinates for field distortion;
 * 	Distortion coefficients pv1, pv2
 * @param x
 * @param y
 * @param pv1
 * @param pv2
 * @return {{xprime: *, yprime: *}}
 */
function distortion (x, y, pv1, pv2) {

    // Apply correction (source http//iraf.noao.edu/projects/ccdmosaic/tpv.html);
    const r = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
    const xprime = pv1[0] + pv1[1] * x + pv1[2] * y + pv1[3] * r +
        pv1[4] * Math.pow(x, 2) + pv1[5] * x * y + pv1[6] * Math.pow(y, 2) +
        pv1[7] * Math.pow(x, 3) + pv1[8] * Math.pow(x, 2) * y + pv1[9] * x * Math.pow(y, 2) + pv1[10] * Math.pow(y, 3) + pv1[11] * Math.pow(r, 3) +
        pv1[12] * Math.pow(x, 4) + pv1[13] * Math.pow(x, 3) * y + pv1[14] * Math.pow(x, 2) * Math.pow(y, 2) + pv1[15] * x * Math.pow(y, 3) + pv1[16] * Math.pow(y, 4);
    const yprime = pv2[0] + pv2[1] * y + pv2[2] * x + pv2[3] * r +
        pv2[4] * Math.pow(y, 2) + pv2[5] * y * x + pv2[6] * Math.pow(x, 2) +
        pv2[7] * Math.pow(y, 3) + pv2[8] * Math.pow(y, 2) * x + pv2[9] * y * Math.pow(x, 2) + pv2[10] * Math.pow(x, 3) + pv2[11] * Math.pow(r, 3) +
        pv2[12] * Math.pow(y, 4) + pv2[13] * Math.pow(y, 3) * x + pv2[14] * Math.pow(y, 2) * Math.pow(x, 2) + pv2[15] * y * Math.pow(x, 3) + pv2[16] * Math.pow(x, 4);


    return {xprime, yprime};
}

