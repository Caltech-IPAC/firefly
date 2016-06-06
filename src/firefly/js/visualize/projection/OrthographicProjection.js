/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
// TEST WITH: 2MASS
import {DtoR, RtoD, computeDistance, MAX_SIP_LENGTH} from './ProjectionUtil.js';
import {makeProjectionPt,makeImagePt} from '../Point.js';



export const OrthographicProjection= {


	revProject(ra, dec, hdr, useProjException= false) {
		var i, j;
		var fsamp_correction, fline_correction;
		var temp;

		const crpix1 = hdr.crpix1;
		const crpix2 = hdr.crpix2;
		const glong  = hdr.crval1;
		const glat   = hdr.crval2;
		const cdelt1 = hdr.cdelt1;
		const cdelt2 = hdr.cdelt2;
		const twist  = hdr.crota2;
		const using_cd = hdr.using_cd;
		const dc1_1 = hdr.dc1_1;
		const dc1_2 = hdr.dc1_2;
		const dc2_1 = hdr.dc2_1;
		const dc2_2 = hdr.dc2_2;


		const distance = computeDistance(ra, dec, glong, glat);
		var lon = ra * DtoR;
		if (distance > 89) {
			// we're more than 89 degrees from projection center
			if (useProjException) throw new Error('coordinates not on image');
			else return null;
		}

		var lat = dec * DtoR;


		const rpp1 = -cdelt1 * DtoR;
		const rpp2 = -cdelt2 * DtoR;

		const lon0 = glong * DtoR;
		const lat0 = glat * DtoR;

		var fsamp = -Math.cos(lat) * Math.sin(lon - lon0);
		var fline = -Math.sin(lat) * Math.cos(lat0) + Math.cos(lat) * Math.sin(lat0) * Math.cos(lon - lon0);


		if (using_cd) {
			temp = -(dc1_1 * fsamp + dc1_2 * fline) * RtoD;
			fline = -(dc2_1 * fsamp + dc2_2 * fline) * RtoD;
			fsamp = temp;
		}
		else {
			// do the twist
			const rtwist = twist * DtoR;       // convert to radians
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

		const x = fsamp + crpix1 - 1;
		const y = fline + crpix2 - 1;

		return makeImagePt(x, y);
	},

	fwdProject(x, y, hdr, useProjException= false) {
		var i, j;
		var fsamp_correction, fline_correction;
		var rad;
		var fsamp, fline;
		var lat, lon;
		var rpp1, rpp2, glongr, glatr ;
		var rtwist, temp;
		var xx, yy;

		const crpix1 = hdr.crpix1;
		const crpix2 = hdr.crpix2;
		const glong  = hdr.crval1;
		const glat   = hdr.crval2;
		const cdelt1 = hdr.cdelt1;
		const cdelt2 = hdr.cdelt2;
		const twist  = hdr.crota2;
		const using_cd = hdr.using_cd;
		const cd1_1 = hdr.cd1_1;
		const cd1_2 = hdr.cd1_2;
		const cd2_1 = hdr.cd2_1;
		const cd2_2 = hdr.cd2_2;

		fsamp = x - crpix1 + 1;
		fline = y - crpix2 + 1;

		if (hdr.map_distortion) {
			// apply SIRTF distortion corrections
			fsamp_correction = 0.0;
			var len= Math.floor(Math.min(hdr.a_order+1, MAX_SIP_LENGTH));
			for (i = 0; i < len; i++) {
				for (j = 0; j < len; j++) {
					if (i + j <= hdr.a_order) {
						fsamp_correction +=
							hdr.a[i][j] * Math.pow(fsamp, i) * Math.pow(fline, j);
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
			xx = (cd1_1 * fsamp + cd1_2 * fline) * DtoR;
			yy = (cd2_1 * fsamp + cd2_2 * fline) * DtoR;
		}
		else {
			rpp1 = cdelt1 * DtoR;        /* radians per pixel */
			rpp2 = cdelt2 * DtoR;        /* radians per pixel */
			xx = fsamp * rpp1;
			yy = fline * rpp2;

			rtwist = twist * DtoR;       /* convert to radians */
			temp = xx * Math.cos(rtwist) - yy * Math.sin(rtwist); /* do twist */
			yy = xx * Math.sin(rtwist) + yy * Math.cos(rtwist);
			xx = temp;
		}


		glatr = glat * DtoR;
		glongr = glong * DtoR;
		if ((1 - xx * xx - yy * yy) < 0) {
			if (useProjException) throw new Error('undefined location');
			else                  return null;
		}
		rad = Math.sqrt(1 - xx * xx - yy * yy);
		lat = Math.asin(yy * Math.cos(glatr) + Math.sin(glatr) * rad);
		lon = glongr + Math.atan2(xx,
				(Math.cos(glatr) * rad - yy * Math.sin(glatr)));


		lat = lat * RtoD;
		lon = (360. + lon * RtoD) % 360.; //TLau 012512: handle negative value and greater than 360 value

		var _pt = makeProjectionPt(lon, lat);
		return (_pt);
	}

};



