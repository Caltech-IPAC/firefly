/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
// TEST WITH: IRAS
import {DtoR,RtoD, MAX_SIP_LENGTH} from './ProjectionUtil.js';
import {makeProjectionPt,makeImagePt} from '../Point.js';

export const GnomonicProjection= {

    revProject (ra, dec, hdr, useProjException= false) {
		var i, j;
		var fsamp_correction, fline_correction;
		var fline, fsamp, rtwist, temp;

		const {crpix1,crpix2,cdelt1,cdelt2, dc1_1, dc1_2, dc2_1, dc2_2, using_cd}= hdr;
		const glong  = hdr.crval1;
		const glat   = hdr.crval2;
		const twist  = hdr.crota2;

		var lon = ra * DtoR;
		var lat = dec * DtoR;

		const rpp1 = -cdelt1 * DtoR;
		const rpp2 = -cdelt2 * DtoR;

		const lon0 = glong * DtoR;
		const lat0 = glat * DtoR;

		const aa = Math.cos(lat) * Math.cos(lon - lon0);
		const ff1 = 1./ (Math.sin(lat0) * Math.sin(lat) + aa * Math.cos(lat0));
		const ff2 = 1./ (Math.sin(lat0) * Math.sin(lat) + aa * Math.cos(lat0));

		if (ff1 < 0) {
			// we're more than 90 degrees from projection center 
			if (useProjException) throw new Error('coordinates not on image');
			else return null;
		}
		else {
			fline = -ff2 * (Math.cos(lat0) * Math.sin(lat) - aa * Math.sin(lat0));
			fsamp = -ff1 * Math.cos(lat) * Math.sin(lon - lon0);

			if (using_cd) {
				temp = -(dc1_1 * fsamp + dc1_2 * fline) * RtoD;
				fline = -(dc2_1 * fsamp + dc2_2 * fline) * RtoD;
				fsamp = temp;
			}
			else {
				// do the twist
				rtwist = twist * DtoR;       // convert to radians
				temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
				fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
				fsamp = temp;

				fsamp = (fsamp / rpp1);     // now apply cdelt
				fline = (fline / rpp2);
			}


			if (hdr.map_distortion) {
				// apply SIRTF distortion corrections
				fsamp_correction = 0.0;

				var len= Math.floor(Math.min(hdr.ap_order+1, MAX_SIP_LENGTH));
				for (i = 0; i < len; i++) {
					for (j = 0; j < len; j++) {
						if (i + j <= hdr.ap_order) {
							fsamp_correction +=
								hdr.ap[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
						}
					}
				}


				fline_correction = 0.0;
				len= Math.floor(Math.min(hdr.bp_order+1, MAX_SIP_LENGTH));
				for (i = 0; i < len; i++) {
					for (j = 0; j < len; j++) {
						if (i + j <= hdr.bp_order) {
							fline_correction +=
								hdr.bp[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
						}
					}
				}
				fsamp += fsamp_correction;
				fline += fline_correction;

			}
		}

		const x = fsamp + crpix1 - 1;
		const y = fline + crpix2 - 1;

		return makeImagePt(x, y);
	},

	fwdProject( x, y, hdr) {
		var i, j;
		var fsamp_correction, fline_correction;
		var lat, lon;
		var xxx, yyy, xx, yy;

		const {crpix1,crpix2,cdelt1,cdelt2, cd1_1, cd1_2, cd2_1, cd2_2, using_cd, map_distortion}= hdr;
		const glong  = hdr.crval1;
		const glat   = hdr.crval2;
		const twist  = hdr.crota2;

		var fsamp = x - crpix1 + 1;
		var fline = y - crpix2 + 1;

		if (map_distortion) {
			// apply SIRTF distortion corrections
			fsamp_correction = 0.0;
			var len= Math.floor(Math.min(hdr.a_order+1, MAX_SIP_LENGTH));
			for (i = 0; i < len; i++) {
				for (j = 0; j < len; j++) {
					if (i + j <= hdr.a_order) {
						fsamp_correction += hdr.a[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
					}
				}
			}


			fline_correction = 0.0;
			len= Math.floor(Math.min(hdr.b_order+1, MAX_SIP_LENGTH));
			for (i = 0; i < len; i++) {
				for (j = 0; j < len; j++) {
					if (i + j <= hdr.b_order) {
						fline_correction +=
							hdr.b[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
					}
				}
			}
			fsamp += fsamp_correction;
			fline += fline_correction;
		}

		if (using_cd) {
			xx = -(cd1_1 * fsamp + cd1_2 * fline) * DtoR;
			yy = -(cd2_1 * fsamp + cd2_2 * fline) * DtoR;
		}
		else {
			const rpp1 = cdelt1 * DtoR;        // radians per pixel
			const rpp2 = cdelt2 * DtoR;        // radians per pixel
			xx = -fsamp * rpp1;
			yy = -fline * rpp2;

			const rtwist = twist * DtoR;       // convert to radians
			const temp = xx * Math.cos(rtwist) - yy * Math.sin(rtwist); // do twist
			yy = xx * Math.sin(rtwist) + yy * Math.cos(rtwist);
			xx = temp;
		}

		const delta = Math.atan(Math.sqrt(xx * xx + yy * yy));

		if ((xx == 0.0) && (yy == 0.0)) yy = 1.0;  // avoid domain error in atan2
		const beta = Math.atan2(-xx, yy);
		const glatr = glat * DtoR;
		const glongr = glong * DtoR;
		lat = Math.asin(-Math.sin(delta) * Math.cos(beta) * Math.cos(glatr) + Math.cos(delta) * Math.sin(glatr));
		xxx = Math.sin(glatr) * Math.sin(delta) * Math.cos(beta) + Math.cos(glatr) * Math.cos(delta);
		yyy = Math.sin(delta) * Math.sin(beta);
		lon = glongr + Math.atan2(yyy, xxx);

		lat = lat * RtoD;
		lon = lon * RtoD;

		const pt = makeProjectionPt(lon, lat);
		return (pt);
	}

};



