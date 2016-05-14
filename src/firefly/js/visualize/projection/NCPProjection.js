/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {DtoR,RtoD} from './ProjectionUtil.js';
import {makeProjectionPt,makeImagePt} from '../Point.js';

export const NCPProjection = {

	revProject (ra, dec, hdr) {

		const {crpix1,crpix2,cdelt1,cdelt2}= hdr;
		const glong  = hdr.crval1;
		const glat   = hdr.crval2;
		const twist  = hdr.crota2;

		var lon = ra * DtoR;
		var lat = dec * DtoR;

		const rpp1 = -cdelt1 * DtoR;
		const rpp2 = -cdelt2 * DtoR;

		const lon0 = glong * DtoR;
		const lat0 = glat * DtoR;

		var fsamp = -Math.cos(lat) * Math.sin(lon - lon0);
		var fline = (-Math.cos(lat0) + Math.cos(lat) * Math.cos(lon - lon0)) / Math.sin(lat0);


		// do the twist
		const rtwist = twist * DtoR;       // convert to radians
		const temp = fsamp * Math.cos(rtwist) + fline * Math.sin(rtwist);
		fline = -fsamp * Math.sin(rtwist) + fline * Math.cos(rtwist);
		fsamp = temp;

		fsamp = (fsamp / rpp1);     // now apply cdelt
		fline = (fline / rpp2);

		const x = fsamp + crpix1 - 1;
		const y = fline + crpix2 - 1;

		return makeImagePt(x, y);
	},

	fwdProject( x, y, hdr) {
		const {crpix1,crpix2,cdelt1,cdelt2}= hdr;
		const glong  = hdr.crval1;
		const glat   = hdr.crval2;
		const twist  = hdr.crota2;

		var fsamp = x - crpix1 + 1;
		var fline = y - crpix2 + 1;

		const rpp1 = cdelt1 * DtoR;        /* radians per pixel */
		const rpp2 = cdelt2 * DtoR;        /* radians per pixel */
		var xx = fsamp * rpp1;
		var yy = fline * rpp2;

		const rtwist = twist * DtoR;       /* convert to radians */
		const temp = xx * Math.cos(rtwist) - yy * Math.sin(rtwist); /* do twist */
		yy = xx * Math.sin(rtwist) + yy * Math.cos(rtwist);
		xx = temp;


		const glatr = glat * DtoR;
		const glongr = glong * DtoR;

		var lon = glongr + Math.atan(xx / (Math.cos(glatr) - yy * Math.sin(glatr)));
		var lat = Math.abs( Math.acos((Math.cos(glatr) - yy * Math.sin(glatr)) /
			Math.cos(lon - glongr)));
		if (glatr < 0) lat = -lat;

		lat = lat * RtoD;
		lon = lon * RtoD;

		return makeProjectionPt(lon, lat);
	}
};



