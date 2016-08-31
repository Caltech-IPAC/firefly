/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import {makeImagePt, makeWorldPt} from '../Point.js';
import {CoordinateSys} from '../CoordSys.js';
import {AitoffProjection} from './AitoffProjection.js';
import {NCPProjection} from './NCPProjection.js';
import {ARCProjection} from './ARCProjection.js';
import {GnomonicProjection} from './GnomonicProjection.js';
import {SansonFlamsteedProjection } from './SansonFlamsteedProjection.js';
import {LinearProjection} from './LinearProjection.js';
import {CartesianProjection} from './CartesianProjection.js';
import {OrthographicProjection} from './OrthographicProjection.js';
import {CylindricalProjection} from './CylindricalProjection.js';
import {PlateProjection} from './PlateProjection.js';



const unspecifiedProject= () => null;
const unimplementedProject= () => null;

export const GNOMONIC     = 1001; // TESTED
export const ORTHOGRAPHIC = 1002; // TESTED
export const NCP          = 1003;
export const AITOFF       = 1004; // TESTED
export const CAR          = 1005;
export const LINEAR       = 1006;
export const PLATE        = 1007; // TESTED
export const ARC          = 1008;
export const SFL          = 1009; // TESTED
export const CEA          = 1010;
export const UNSPECIFIED  = 1998; // TESTED
export const UNRECOGNIZED = 1999; // TESTED

const projTypes= {
	[GNOMONIC] : {
		name: 'GNOMONIC',
		fwdProject: GnomonicProjection.fwdProject,
		revProject: GnomonicProjection.revProject,
		implemented : true,
		wrapping : false
	},
	[ORTHOGRAPHIC] : {
		name: 'ORTHOGRAPHIC',
		fwdProject: OrthographicProjection.fwdProject,
		revProject: OrthographicProjection.revProject,
		implemented : true,
		wrapping : false
	},
	[NCP] : {
		name: 'NCP',
		fwdProject: NCPProjection.fwdProject,
		revProject: NCPProjection.revProject,
		implemented : true,
		wrapping : false
	},
	[ARC] : {
		name: 'ARC',
		fwdProject: ARCProjection.fwdProject,
		revProject: ARCProjection.revProject,
		implemented : true,
		wrapping : false
	},
	[AITOFF] : {
		name: 'AITOFF',
		fwdProject: AitoffProjection.fwdProject,
		revProject: AitoffProjection.revProject,
		implemented : true,
		wrapping : true
	},
	[CAR] : {
		name: 'CAR',
		fwdProject: CartesianProjection.fwdProject,
		revProject: CartesianProjection.revProject,
		implemented : true,
		wrapping : false
	},

	[CEA] : {
		name: 'CEA',
		fwdProject: CylindricalProjection.fwdProject,
		revProject: CylindricalProjection.revProject,
		implemented : true,
		wrapping : false
	},
	[SFL] : {
		name: 'SFL',
		fwdProject: SansonFlamsteedProjection.fwdProject,
		revProject: SansonFlamsteedProjection.revProject,
		wrapping : false
	},
	[LINEAR] : {
		name: 'LINEAR',
		fwdProject: LinearProjection.fwdProject,
		revProject: LinearProjection.revProject,
		implemented : true,
		wrapping : false
	},
	[PLATE] : {
		name: 'PLATE',
		fwdProject: PlateProjection.fwdProject,
		revProject: PlateProjection.revProject,
		implemented : true,
		wrapping : false
	},
	[UNSPECIFIED] : {
		name: 'UNSPECIFIED',
		fwdProject: unspecifiedProject,
		revProject: unspecifiedProject,
		implemented : false,
		wrapping : false
	},
	[UNRECOGNIZED] : {
		name: 'UNRECOGNIZED',
		fwdProject: unimplementedProject,
		revProject: unimplementedProject,
		implemented : false,
		wrapping : false
	}
};


const translateProjectionName= (maptype) => get(projTypes, [maptype,'name'],'UNRECOGNIZED');
const isImplemented= (header) => get(header, ['maptype.implemented'],false);
const isWrappingProjection= (header) => get(header, ['maptype.wrapping'],false);




/**
 * Convert 'ProjectionPt' coordinates to World coordinates
 *    'ProjectionPt' coordinates have 0,0 in center of lower left pixel
 *    (same as 'Skyview Screen' coordinates)
 * @param x double precision in 'ProjectionPt' coordinates
 * @param y double precision in 'ProjectionPt' coordinates
 * @param header the header info
 * @param coordSys used for the worldPt
 */
function getWorldCoordsInternal(x, y, header, coordSys)  {
	if (!projTypes[header.maptype]) return null;
	const pt = projTypes[header.maptype].fwdProject( x, y, header);
	return pt ? makeWorldPt(pt.x, pt.y, coordSys) : null;
}

/**
 *   Convert World coordinates to 'Skyview Screen' coordinates
 *    'Skyview Screen' coordinates have 0,0 in center of lower left pixel
 * @param ra double precision
 * @param dec double precision
 * @param header the header info
 * @return ImagePt with X,Y in 'Skyview Screen' coordinates
 */
function getImageCoordsInternal(ra, dec, header) {
	//console.log('in Project.js');
	//console.log(header);
	//console.log(header.maptype);
	if (!projTypes[header.maptype]) return null;
	//console.log(projTypes[header.maptype]);
	const image_pt = projTypes[header.maptype].revProject( ra, dec, header);
	return image_pt;
}




export class Projection {

    constructor(header, coordSys)  {
        this.header= header;
        this.scale1= 1/header.cdelt1;
        this.scale2= 1/header.cdelt2;
        this.pixelScaleArcSec= Math.abs(header.cdelt1) * 3600.0;
        this.coordSys= coordSys;
		// console.log('Projection: '+translateProjectionName(header.maptype));
    }

    getPixelWidthDegree() { return Math.abs(this.header.cdelt1); }
    getPixelHeightDegree() { return Math.abs(this.header.cdelt2); }
	getPixelScaleArcSec() { return this.pixelScaleArcSec; }

    /**
     * Return a point the represents the passed point with a distance in
     * World coordinates added to it.
     * @param pt the x and y coordinate in image coordinates
     * @param x the x distance away from the point in world coordinates
     * @param y the y distance away from the point in world coordinates
     * @return ImagePt the new point
     */
    getDistanceCoords(pt, x, y) {
        return makeImagePt ( pt.x+(x * this.scale1), pt.y+(y * this.scale2) );
    }

    getImageCoords(ra, dec) { return getImageCoordsInternal(ra, dec, this.header, false); }

    getWorldCoords( x, y) { return getWorldCoordsInternal(x, y, this.header, this.coordSys, false); }

	isImplemented() { isImplemented(this.header); }

	isSpecified() { return this.header.maptype!==UNSPECIFIED; }

	isWrappingProjection() { return isWrappingProjection(this.header); }

    getProjectionName() { return translateProjectionName(this.header.maptype); }
}

export function makeProjection(projJSON) {
	return new Projection(projJSON.header, CoordinateSys.parse(projJSON.coorindateSys));
}

