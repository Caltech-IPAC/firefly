/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {DtoR} from './ProjectionUtil.js';
import {makeProjectionPt,makeImagePt} from '../Point.js';

export const LinearProjection= {

	revProject(lon, lat, hdr) {
		var fline, fsamp;
		var temp;

		const {crpix1,crpix2,cdelt1,cdelt2, dc1_1, dc1_2, dc2_1, dc2_2, using_cd}= hdr;
		const glong  = hdr.crval1;
		const glat   = hdr.crval2;
		const twist  = hdr.crota2;

		if (using_cd) {
			fsamp = ((lon - glong) );
			fline = ((lat - glat) );
			temp = (dc1_1 * fsamp + dc1_2 * fline);
			fline = (dc2_1 * fsamp + dc2_2 * fline);
			fsamp = temp;
		}
		else {
			fsamp = ((lon - glong) / cdelt1 );
			fline = ((lat - glat) / cdelt2 );
			// do the twist
			const rtwist = - twist * DtoR;       // convert to radians
			temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
			fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
			fsamp = temp;
		}
		const x = fsamp + crpix1 - 1;
		const y = fline + crpix2 - 1;

		return makeImagePt(x, y);
	},

	fwdProject( x, y, hdr) {
		var lat, lon;

		const {crpix1,crpix2,cdelt1,cdelt2, cd1_1, cd1_2, cd2_1, cd2_2, using_cd}= hdr;
		var glong  = hdr.crval1;
		var glat   = hdr.crval2;
		var twist  = hdr.crota2;

		var fsamp = x - crpix1 + 1;
		var fline = y - crpix2 + 1;

		if (using_cd) {
			lon = (cd1_1 * fsamp + cd1_2 * fline);
			lat = (cd2_1 * fsamp + cd2_2 * fline);
			lon += glong;
			lat += glat;
		}
		else {
			const rtwist = - twist * DtoR;       // convert to radians
			const temp = fsamp * Math.cos(rtwist) - fline * Math.sin(rtwist); // do twist
			fline = fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
			fsamp = temp;

			lon = glong + fsamp * cdelt1;
			lat = glat + fline * cdelt2;
		}

		return makeProjectionPt(lon, lat);
	}
};

