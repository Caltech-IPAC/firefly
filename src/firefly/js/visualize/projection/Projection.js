/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import {makeImagePt, makeProjectionPt, makeWorldPt} from '../Point.js';
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
import {Projection as AladinProjection} from '../../externalSource/aladinProj/AladinProjections.js';



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



export const ALADIN_SIN     = 5;



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
		wrapping : true
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
	},

    [ALADIN_SIN] : {
        name: 'ALADIN_SIN',
        fwdProject : fwdAladinSinProject,
        revProject : revAladinSinProject,
        implemented : true,
        wrapping : false,
    },


};


const translateProjectionName= (maptype) => get(projTypes, [maptype,'name'],'UNRECOGNIZED');
const isImplemented= (header) => get(projTypes, [header.maptype, 'implemented'],false);
const isWrappingProjection= (header) => get(projTypes, [header.maptype, 'wrapping'],false);




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
	if (!projTypes[header.maptype]) return null;
	const image_pt = projTypes[header.maptype].revProject( ra, dec, header);
	return image_pt;
}




export class Projection {

	/**
	 * @summary Contains data about the state of a plot.
	 * This object is never created directly if is always instantiated from the json sent from the server.
	 * @param {Object} header data from the fits file
	 * @param {CoordinateSys} coordSys
	 * Note - constructor should never be call directly
	 *
     *
	 * @prop {object} header
	 * @prop {number} scale1
	 * @prop {number} scale2
	 * @prop {number} pixelScaleArcSec
	 * @prop {CoordinateSys} coordSys
	 * @public
	 */
    constructor(header, coordSys)  {
        this.header= header;
        this.pixelScaleDeg= Math.abs(this.header.cdelt1);
        this.pixelScaleArcSec= this.pixelScaleDeg* 3600.0;
        this.coordSys= coordSys;
    }

	/**
	 *
	 * @return {number}
	 * @public
	 */
	getPixelScaleDegree() { return this.pixelScaleDeg; }


	/**
	 * @summary the scale of an image pixel in arcsec
	 * @return {number}
	 * @public
	 */
	getPixelScaleArcSec() { return this.pixelScaleArcSec; }


	/**
	 * @summary convert from a world point to a image point
	 * @param ra
	 * @param dec
	 * @return {ImagePt}
	 * @public
	 */
    getImageCoords(ra, dec) { return getImageCoordsInternal(ra, dec, this.header, false); }

	/**
	 * @summary convert from a image point to a world point
	 * @param x
	 * @param y
	 * @return {WorldPt}
	 * @public
	 */
	getWorldCoords( x, y) { return getWorldCoordsInternal(x, y, this.header, this.coordSys, false); }

	/**
	 * @return {boolean} true, if this projection is implemented
	 * @public
	 */
	isImplemented() { return isImplemented(this.header); }

	/**
	 * @return {boolean} true, if this projection is specified
	 * @public
	 */
	isSpecified() { return this.header.maptype!==UNSPECIFIED; }

	/**
	 * @return {boolean} true, if this projection is wrapping, e.g. AITOFF
	 * @public
	 */
	isWrappingProjection() { return isWrappingProjection(this.header); }

	/**
	 * @return true, if this projection is wrapping, e.g. AITOFF
	 * @public
	 */
    getProjectionName() { return translateProjectionName(this.header.maptype); }
}

export function makeProjection(projJSON) {
	return new Projection(projJSON.header, CoordinateSys.parse(projJSON.coorindateSys));
}


function fwdAladinSinProject(x, y, header) {

    const widthHalf = header.crpix1;
    const heightHalf = header.crpix2;
    const yshift = 20;

    const height = header.crpix2 * 2;
    const yFlip = height - y;


    let pX = (x - widthHalf) / widthHalf;
    if (pX === -1) pX = -.99;
    else if (pX === 1) pX = .99;
    const pY = (yFlip - heightHalf) / (heightHalf + yshift);


    const p = new AladinProjection(header.crval1, header.crval2);
    p.setProjection(AladinProjection.PROJ_SIN);
    try {
        const pt = p.unproject(pX, pY);
        if (!pt) return null;
        const retPt = makeProjectionPt(pt.ra, pt.dec);
        return retPt;
    } catch (e) {
        return null;
    }
}

function revAladinSinProject(ra, dec,  header) {
        const widthHalf= header.crpix1;
        const heightHalf= header.crpix2;
        const width= header.crpix1*2;
        const height= header.crpix2*2;
        const yshift= 20;

        const p= new AladinProjection(header.crval1, header.crval2);
        p.setProjection(AladinProjection.PROJ_SIN);
        const pt= p.project(ra,dec);
        if (!pt) return null;
        const x= pt.X;
        const y= pt.Y;

        const imX= x*widthHalf +widthHalf;
        const imY= y*(heightHalf+yshift) + heightHalf;
        return makeImagePt(  imX, height - imY);
}
