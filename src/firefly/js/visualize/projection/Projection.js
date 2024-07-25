/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Projection as AladinProjection} from '../../externalSource/aladinProj/AladinProjections.js';
import {CoordinateSys} from '../CoordSys.js';
import {makeImagePt, makeProjectionPt, makeWorldPt} from '../Point.js';
import {computeDistance, convertAngle, isAngleUnit} from '../VisUtil.js';
import {AitoffProjection} from './AitoffProjection.js';
import {ARCProjection} from './ARCProjection.js';
import {CartesianProjection} from './CartesianProjection.js';
import {CylindricalProjection} from './CylindricalProjection.js';
import {GnomonicProjection} from './GnomonicProjection.js';
import {LinearProjection} from './LinearProjection.js';
import {NCPProjection} from './NCPProjection.js';
import {OrthographicProjection} from './OrthographicProjection.js';
import {PlateProjection} from './PlateProjection.js';
import {SansonFlamsteedProjection} from './SansonFlamsteedProjection.js';
import {TpvProjection} from './TpvProjection.js';


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
export const TPV          = 1011;
export const UNSPECIFIED  = 1998; // TESTED
export const UNRECOGNIZED = 1999; // TESTED



export const HIPS_SIN     = 5;
export const HIPS_AITOFF  = 6;
export const HIPS_DATA_WIDTH = 10000000000;
export const HIPS_DATA_HEIGHT = 10000000000;



const projTypes= {
	[GNOMONIC] : {
		name: 'GNOMONIC',
		fwdProject: GnomonicProjection.fwdProject,
		revProject: GnomonicProjection.revProject,
		implemented : true,
		wrapping : false
	},
    [TPV] : {
        name: 'TPV',
        fwdProject: TpvProjection.fwdProject,
        revProject: TpvProjection.revProject,
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
        implemented : true,
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

    [HIPS_SIN] : {
        name: 'HIPS_SIN',
		fwdProject : (x, y, header) => fwdHiPSProjection(AladinProjection.PROJ_SIN,x,y,header),
		revProject : (ra, dec, header) => revHiPSProjection(AladinProjection.PROJ_SIN, ra,dec,header),
		implemented : true,
        wrapping : false,
    },

	[HIPS_AITOFF] : {
		name: 'HIPS_AITOFF',
		fwdProject : (x, y, header) => fwdHiPSProjection(AladinProjection.PROJ_AITOFF,x,y,header),
		revProject : (ra, dec, header) => revHiPSProjection(AladinProjection.PROJ_AITOFF, ra,dec,header),
		implemented : true,
		wrapping : true,
	},


};


const translateProjectionName= (maptype) => projTypes[maptype]?.name ?? 'UNRECOGNIZED';
const isImplemented= (header) => projTypes[header.maptype]?.implemented ?? false;
const isWrappingProjection= (header) => projTypes[header.maptype]?.wrapping ?? false;




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
	 * @prop {number} pixelScaleDeg
	 * @prop {number} pixelScaleArcSec
	 * @prop {CoordinateSys} coordSys
	 * @public
	 */
    constructor(header, coordSys)  {
        this.header = {...header};
        this.coordSys = coordSys;
        const {crpix1, crpix2, cdelt1, cunit1} = header;
        if (!cdelt1 && crpix1 && crpix2) {
            const projCenter = this.getWorldCoords(crpix1 - 1, crpix2 - 1);
            const oneToRight = this.getWorldCoords(crpix1, crpix2 - 1);
            const oneUp = this.getWorldCoords(crpix1 - 1, crpix2);
            if (oneToRight && oneUp) {
                this.header.cdelt1 = - computeDistance( projCenter, oneToRight);
                this.header.cdelt2 = computeDistance( projCenter, oneUp);
            }

        }
		if (coordSys.isCelestial()) {
			// celestial coordinate systems must have degree as a unit
			this.pixelScaleDeg = Math.abs(this.header.cdelt1);
		} else if (isAngleUnit(cunit1)) {
			this.pixelScaleDeg = convertAngle(cunit1, 'deg', Math.abs(this.header.cdelt1));
		} else {
			this.pixelScaleDeg = NaN;
		}
		this.pixelScaleArcSec = this.pixelScaleDeg * 3600.0;
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
    getImageCoords(ra, dec) { return getImageCoordsInternal(ra, dec, this.header); }

	/**
	 * @summary convert from a image point to a world point
	 * @param x
	 * @param y
	 * @return {WorldPt}
	 * @public
	 */
	getWorldCoords( x, y) { return getWorldCoordsInternal(x, y, this.header, this.coordSys); }

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

export function makeProjectionNew(header, csys) {
    return new Projection(header, csys);
}


function fwdHiPSProjection(aProj, x, y, header) {

    const widthHalf = header.crpix1;
    const heightHalf = header.crpix2;
    const yshift = 20;
    const height = header.crpix2 * 2;
    const yFlip = height - y;


    let pX = (x - widthHalf) / widthHalf;
    if (pX === -1) pX = -.99;
    else if (pX === 1) pX = .99;
    const pY = (yFlip - heightHalf) / (heightHalf + yshift);

	try {
        const pt = new AladinProjection(aProj, header.crval1, header.crval2).unproject(pX, pY);
        return pt ? makeProjectionPt(pt.ra, pt.dec) : undefined;
    } catch (e) {
        return undefined;
    }
}

function revHiPSProjection(aProj, ra, dec,  header) {
	const widthHalf= header.crpix1;
	const heightHalf= header.crpix2;
	const height= header.crpix2*2;
	const yshift= 20;

	const pt= new AladinProjection(aProj, header.crval1, header.crval2).project(ra,dec);
	if (!pt) return undefined;
	const {x, y}= pt;

	const imX= x*widthHalf +widthHalf;
	const imY= y*(heightHalf+yshift) + heightHalf;
	return makeImagePt(  imX, height - imY);
}


/**
 *
 * @param {CoordinateSys} coordinateSys
 * @param lon
 * @param lat
 * @param {boolean} fullSky
 * @return {Projection}
 */
export function makeHiPSProjection(coordinateSys, lon = 0, lat = 0, fullSky = false) {
	const header = {
		cdelt1: 180 / HIPS_DATA_WIDTH,
		cdelt2: 180 / HIPS_DATA_HEIGHT,
		maptype: fullSky ? HIPS_AITOFF : HIPS_SIN,
		crpix1: HIPS_DATA_WIDTH * .5,
		crpix2: HIPS_DATA_HEIGHT * .5,
		crval1: lon,
		crval2: lat
	};
	return makeProjection({header, coorindateSys: coordinateSys.toString()});
}