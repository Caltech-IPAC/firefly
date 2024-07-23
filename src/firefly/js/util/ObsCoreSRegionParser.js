/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isEmpty, isNil} from 'lodash';
import Enum from 'enum';
import {CoordinateSys} from '../visualize/CoordSys.js';
import PointDataObj from '../visualize/draw/PointDataObj.js';
import Point, {makeWorldPt, makeImagePt} from '../visualize/Point.js';
import {calculatePosition, convertAngle} from '../visualize/VisUtil.js';
import ShapeDataObj from '../visualize/draw/ShapeDataObj.js';

const regionShape = new Enum(['circle', 'box', 'polygon', 'position', 'point']);
const CoordSys = new Enum(['ECLIPTIC', 'FK4', 'FK5', 'J2000', 'GALACTIC', 'ICRS', 'UNKNOWNFRAME']);
const RefPos = new Enum(['BARYCENTER', 'GEOCENTER', 'HELIOCENTER', 'LSR', 'TOPOCENTER', 'RELOCATABLE', 'UNKNOWNREFPOS']);
const Flavor = new Enum(['CARTESIAN2', 'CARTESIAN3', 'SPHERICAL2']);

const CoordPos = 1;
const Coord1 = 1;
const Coord2 = 2;
const Width = 3;
const Height = 4;
const Radius = 3;

const ErrorRegion = new Enum(['notSupportCoordSys', 'errorShape','errorNumber', 'invalidRegion' ]);
const errorMessage = {[ErrorRegion.notSupportCoordSys.key]: 'coordinate system is not supported',
                      [ErrorRegion.errorShape.key]: 'region shape is not supported',
                      [ErrorRegion.errorNumber.key]: 'invalid coordinate or size number',
                      [ErrorRegion.invalidRegion.key]: 'invalid region description'};



function getPairCoord(sAry, pairIdx) {
    //if pairIdx included indices that should have returned, for instance, [149,0], then we would only get [149]
    //The filter() function in JavaScript removes elements that are "falsy" values, and 0 is considered falsy.
    //That's why 0 was being ignored when we did filter((n) => n)....making the change below in filter fixes this issue
    return pairIdx.map((coord) => (isNaN(sAry[coord]) ? null : Number(sAry[coord])))
        .filter((n) => !isNil(n));

}

const makeWpt = (pCoord, coordSys, unit) => {
    if (coordSys !== CoordinateSys.FITSPIXEL) {
        return makeWorldPt(convertAngle(unit, 'deg', pCoord[0]),
            convertAngle(unit, 'deg', pCoord[1]),
            coordSys);
    } else {
        return makeImagePt(pCoord[0], pCoord[1]);
    }
};


const getUnitType = (coordSys) => {
    return (coordSys === CoordinateSys.FITSPIXEL) ? ShapeDataObj.UnitType.IMAGE_PIXEL : ShapeDataObj.UnitType.ARCSEC;
};

const getDim = (dim, coordSys, unit, toUnit) => {
    return getUnitType() === ShapeDataObj.UnitType.IMAGE_PIXEL ? dim : convertAngle(unit, toUnit.key, Number(dim));
};


/**
 * get corners from the center and horizontal width, w,  and vertical height, h.
 * @param centerPt
 * @param w  horizontal width to east and west sides
 * @param h  vertical height to north and soutn sides
 * @returns {Array}
 */
function  getCornersByCenter(centerPt, w, h) {
    const corners = [];

    if (centerPt.type !== Point.W_PT) {   // image pixel
        [[-w, -h], [-w, h], [w, h], [w, -h]].forEach((d) => {
            corners.push(makeImagePt(centerPt.x+ d[0], centerPt.x+d[1]));
        });
    } else {
        const posLeft = calculatePosition(centerPt, +w, 0.0); // go east
        const posRight = calculatePosition(centerPt, -w, 0.0); // go west
        const posUp = calculatePosition(centerPt, 0.0, +h);   // go north
        const posDown = calculatePosition(centerPt, 0.0, -h); // go south

        corners.push(makeWorldPt(posLeft.getLon(), posUp.getLat()));
        corners.push(makeWorldPt(posRight.getLon(), posUp.getLat()));
        corners.push(makeWorldPt(posLeft.getLon(), posDown.getLat()));
        corners.push(makeWorldPt(posRight.getLon(), posDown.getLat()));
    }
    return corners;
}

const resetPos = (count) => {
    const rPos = (pos) => (pos+count);

    return { coord1: rPos(Coord1), coord2: rPos(Coord2),
             width: rPos(Width), height: rPos(Height),
             radius: rPos(Radius)};
};


/**
 * parse s_region description and covert the description to DrawObj object or
 * find the corners of the area covering the region described by s_region string
 * for position and polygon region, the points in the description are collected
 * for box, circle region, the four corners covering the region are collected
 * the parsing is based on the syntax described in section 6 Simple STC-S BNF of TAP 1.0.pdf
 * @param sRegionVal s_region description
 * @param unit  unit set in s_region column
 * @param isCorners get corners or get DrawObj
 * @returns {object}
 */
export function parseObsCoreRegion(sRegionVal, unit='deg', isCorners=false) {
    if (!sRegionVal) return {valid:false};
    let  shapeObj = null;
    let  valid = true;
    let  message = '';
    const corners = [];

    const sAry = sRegionVal.trim().split(/\s+/);

    const retVal = (v, m) => {
        return Object.assign({}, {valid: v, message: m},
                            isCorners ? {corners} : {drawObj: shapeObj});
    };

    if (isEmpty(sAry) || sAry.length < (Coord2+1)) {
        return retVal(false, errorMessage[ErrorRegion.invalidRegion.key]);
    }

    const {coordSys, count} = parseCoordinateSys(sAry);
    if (!coordSys) {
        return retVal(false, errorMessage[ErrorRegion.notSupportCoordSys.key]);
    }
    const {coord1, coord2, width, height, radius} = resetPos(count);
    let  pairCoord;

    switch(sAry[0].toLowerCase()) {
        case regionShape.position.key :
        case regionShape.point.key :
            if (sAry.length === coord2 + 1) {
                pairCoord = getPairCoord(sAry, [coord1, coord2]);

                if (pairCoord.length === 2) {
                    if (isCorners) {
                        corners.push(makeWpt(pairCoord, coordSys, unit));
                    } else {
                        shapeObj = PointDataObj.make(makeWpt(pairCoord, coordSys, unit));
                    }
                }
            } else {
                valid = false;
                message = errorMessage[ErrorRegion.invalidRegion.key];
            }
            break;

        case regionShape.circle.key:
            if (sAry.length === radius + 1) {
                pairCoord = getPairCoord(sAry, [coord1, coord2]);
                const rad = isNaN(sAry[radius]) ? null : getDim(sAry[radius], coordSys, unit, ShapeDataObj.UnitType.ARCSEC);

                if ((pairCoord.length === 2) && rad !== null) {
                    if (isCorners) {
                        const circleCorners = getCornersByCenter(makeWpt(pairCoord, coordSys, unit), rad, rad);
                        corners.push(...circleCorners);
                    } else {
                        shapeObj = ShapeDataObj.makeCircleWithRadius(makeWpt(pairCoord, coordSys, unit),
                                                                     rad, getUnitType(coordSys));
                    }
                }
            } else {
                valid = false;
                message = errorMessage[ErrorRegion.invalidRegion.key];
            }
            break;

        case regionShape.polygon.key:
            const coordCount = sAry.length - 1 - count;
            if (coordCount >= 6 && coordCount%2 === 0) {
                const wptAry = [];

                for (let i = coord1; i < sAry.length; i += 2) {
                    pairCoord = getPairCoord(sAry, [i, i + 1]);
                    if (pairCoord.length === 2) {
                        wptAry.push(makeWpt(pairCoord, coordSys, unit));
                    } else {
                        valid = false;
                        message = errorMessage[ErrorRegion.errorNumber.key];
                        break;
                    }
                }
                if (valid) {
                    if (isCorners) {
                        corners.push(...wptAry);
                    } else {
                        shapeObj = ShapeDataObj.makePolygon(wptAry);
                    }
                }
            } else {
                valid = false;
                message = errorMessage[ErrorRegion.invalidRegion.key];
            }
            break;

        case regionShape.box.key:
            if (sAry.length = height + 1) {
                const w = getDim(sAry[width], coordSys, unit, 'arcsec');
                const h = getDim(sAry[height], coordSys, unit, 'arcsec');
                pairCoord = getPairCoord(sAry, [coord1, coord2]);

                if (pairCoord.length === 2 && w !== null && h !== null) {
                    if (isCorners) {
                        const boxCorners = getCornersByCenter(makeWpt(pairCoord, coordSys, unit), w / 2, h / 2);
                        corners.push(...boxCorners);
                    } else {
                        shapeObj = ShapeDataObj.makeRectangleByCenter(makeWpt(pairCoord, coordSys, unit), w, h, getUnitType(coordSys),
                            0.0, ShapeDataObj.UnitType.ARCSEC, coordSys !== CoordinateSys.FITSPIXEL);
                    }
                }
            } else {
                valid = false;
                message = errorMessage[ErrorRegion.invalidRegion.key];
            }
            break;

        default:
            valid = false;
            message = errorMessage[ErrorRegion.errorShape.key];
            break;
    }

    return retVal(valid, message);
}


function parseCoordinateSys(sAry) {
    let cs = null;
    let ref = null;
    let flavor = null;
    let count = 0;

    const coordSys = sAry[CoordPos];

    cs = null;
    switch (coordSys.toUpperCase()) {
        case CoordSys.ECLIPTIC.key:
            cs = CoordinateSys.ECL_J2000;
            break;
        case CoordSys.FK4.key:
            cs = CoordinateSys.EQ_B1950;
            break;
        case CoordSys.FK5.key:
        case CoordSys.ICRS.key:
        case CoordSys.J2000.key:
            cs = CoordinateSys.EQ_J2000;
            break;
        case CoordSys.GALACTIC.key:
            cs = CoordinateSys.GALACTIC;
            break;
        case CoordSys.UNKNOWNFRAME:
            cs = CoordinateSys.UNDEFINED;
            break;
    }

    if (cs) {
        count++;
    }

    const refpos = sAry[CoordPos+count];

    if (Object.keys(RefPos).includes(refpos.toUpperCase())){
        ref = RefPos.get(refpos.toUpperCase());
        count++;
    } else {
        ref = null;
    }

    const flavorpos = sAry[CoordPos+count];

    if (Object.keys(Flavor).includes(flavorpos.toUpperCase())){
        flavor = Flavor.get(flavorpos.toUpperCase());
        count++;
    } else {
        flavor = null;
    }

    if (count === 0) {
        cs = CoordinateSys.EQ_J2000;
    }

    return {coordSys: cs, ref, flavor, count};
}