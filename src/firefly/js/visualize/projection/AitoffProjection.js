/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
// TEST WITH: file e90gal04.fits
import {DtoR,RtoD} from './ProjectionUtil.js';
import {makeProjectionPt,makeImagePt} from '../Point.js';

export const AitoffProjection = {

	revProject (ra, dec, hdr) {
		var theta;

		const {crpix1,crpix2, cdelt1, cdelt2}= hdr;
		const glong  = hdr.crval1;
		// const glat   = hdr.crval2;

		var lon = ra * DtoR;
		var lat = dec * DtoR;

		const rpp1 = cdelt1 * DtoR;
		const rpp2 = cdelt2 * DtoR;

		const lon0 = glong * DtoR;
		// const lat0 = glat * DtoR;


		// get delta-lon in range -180 to +180 
		if ((lon - lon0) > Math.PI) lon -= 2 * Math.PI;
		if ((lon - lon0) < -Math.PI) lon += 2 * Math.PI;

		const rho = Math.acos(Math.cos(lat) * Math.cos((lon - lon0) / 2.));
		if ((rho < 0.0001) && (rho > -0.0001)) {
			theta = 0;
		}
		else {
			var asin_arg = Math.cos(lat) * Math.sin((lon - lon0) / 2.) / Math.sin(rho);
			if (asin_arg > 1.0) asin_arg = 1.0;
			if (asin_arg < -1.0) asin_arg = -1.0;
			theta = Math.asin(asin_arg);
		}

		var fsamp = 4./ rpp1 * Math.sin(rho / 2.) * Math.sin(theta);
		var fline = 2./ rpp2 * Math.sin(rho / 2.) * Math.cos(theta);

		if (lat < 0.) fline = -fline;

		const x = fsamp + crpix1 - 1;
		const y = fline + crpix2 - 1;

		return makeImagePt(x, y);
	},

	fwdProject( x, y, hdr, useProjException= false) {
		var asin_arg;
		var fsamp, fline;
		var lat, lon;
		var temp;

		const {crpix1,crpix2, cdelt1, cdelt2}= hdr;
		var glong  = hdr.crval1;
		var glat   = hdr.crval2;

		fsamp = x - crpix1 + 1;
		fline = y - crpix2 + 1;

		const rpp1 = cdelt1 * DtoR;        /* radians per pixel */
		const rpp2 = cdelt2 * DtoR;        /* radians per pixel */
		const xx = (fsamp / 2.) * rpp1;
		const yy = (fline / 2.) * rpp2;

		if (((4.- xx * xx - 4.* yy * yy) < 2) || (glat != 0.0)) {
			if (useProjException) throw Error('undefined location');
			else                  return null;
		}

		temp = Math.sqrt(4.- xx * xx - 4.* yy * yy);

		lat = Math.asin(temp * yy);
		asin_arg = (temp * xx / (2.* Math.cos(lat)));
		if (asin_arg > 1.0) asin_arg = 1.0;
		if (asin_arg < -1.0) asin_arg = -1.0;
		lon = 2 * Math.asin(asin_arg);

		lat = lat * RtoD;
		lon = glong + lon * RtoD;
		if (lon < 0.0) {
			lon += 360.0;
		}

		return makeProjectionPt(lon, lat);
	}
};

