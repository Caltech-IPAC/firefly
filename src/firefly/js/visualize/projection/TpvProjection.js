/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {DtoR,RtoD, MAX_SIP_LENGTH, acosd,asind,atan2d,atand,sind,cosd,tand} from './ProjectionUtil.js';
import {makeProjectionPt,makeImagePt} from '../Point.js';

const DEF_PV1= [0,1];
const DEF_PV2= [0,1];
DEF_PV1.length=40;
DEF_PV2.length=40;

DEF_PV1.fill(0,2,39);
DEF_PV2.fill(0,2,39);

export const TpvProjection= {

    revProject (ra, dec, hdr) {
		let i, j;
		let fsamp_correction, fline_correction;
		let fline, fsamp, rtwist, temp;

		const {crpix1,crpix2,cdelt1,cdelt2, dc1_1, dc1_2, dc2_1, dc2_2, using_cd,
               ctype1, ctype2, using_tpv=false, pv1= DEF_PV1, pv2=DEF_PV2}= hdr;
		const glong  = hdr.crval1;
		const glat   = hdr.crval2;


        /// TODO: Implement forward reverse projection here
        /// TODO: Implement forward reverse projection here
        /// TODO: Implement forward reverse projection here


		return makeImagePt(1, 1);
	},

	fwdProject( x, y, hdr) {
		const {crpix1,crpix2,cdelt1,cdelt2, cd1_1, cd1_2, cd2_1, cd2_2, using_cd, map_distortion,
		       using_tpv=false, pv1= DEF_PV1, pv2=DEF_PV2}= hdr;
		const glong  = hdr.crval1;
		const glat   = hdr.crval2;



		/// TODO: Implement forward TPV projection here
        /// TODO: Implement forward TPV projection here
        /// TODO: Implement forward TPV projection here



		return makeProjectionPt(2.22, 2.22);
	}

};

