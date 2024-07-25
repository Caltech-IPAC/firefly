import {parsePosition,PositionParsedInputType} from '../util/PositionParser.js';
import CoordinateSys from '../visualize/CoordSys.js';
import CoordUtil from '../visualize/CoordUtil.js';

import {sprintf} from '../externalSource/sprintf.js';
import {convertCelestial} from '../visualize/VisUtil';

const errMsgRoot= 'Error: ';

export function positionValidateSoft(aValue) {
    const s = (aValue ===null) ? null : aValue.toString();
    return positionValidateInternal(s, false);
}

function positionValidateInternal(s, hard, nullAllowed= true) {
    if (!s) {
        if (nullAllowed) return {valid:true};
        throw 'You must enter a valid position or name';
    }
    const {valid, inputType, objName, position, ra, dec,
        coordSys, raParseErr, decParseErr} = parsePosition(s);
    if (valid) return {valid, inputType, objName, position};

    // validate ObjName
    if (inputType===PositionParsedInputType.Name) {
        if (hard) throw 'Object names must be more than one character';
    }
    else {
        // check coordinate system
        if (coordSys === CoordinateSys.UNDEFINED) {
            throw errMsgRoot + 'Invalid coordinate system';
        }

        // validate RA
        if (isNaN(ra)) {
            const errRA = raParseErr || 'Unable to parse RA';
            if (errRA) throw `${errMsgRoot}${errRA}`;
        }
        // validate DEC
        if (isNaN(dec)) {
            const errDec = decParseErr || 'Unable to parse DEC';
            if (errDec) throw `${errMsgRoot}${errDec}`;
        }
    }
    return {valid:true};
}

/**
 *
 * @param csys coordinate system
 * @return string
 */
export function coordToString(csys) {
    if (csys === CoordinateSys.EQ_J2000) return 'Equ J2000';
    else if (csys === CoordinateSys.EQ_B1950) return 'Equ B1950';
    else if (csys === CoordinateSys.GALACTIC) return 'Gal';
    else if (csys === CoordinateSys.ECL_J2000) return 'Ecl J2000';
    else if (csys === CoordinateSys.ECL_B1950) return 'Ecl B1950';
    return '';
}

export function formatTargetForHelp(wp) {
    if (!wp) return '';
    if (!wp.objName || !wp.resolver) return formatPosForHelp(wp);
    return `<div style="font-size:14px;"><b>${wp.objName}</b> <span style="font-size:10px;">resolved by</span> ${wp.resolver.desc}</span>
            <div>
              ${formatPosForHelp(wp)}
            </div>`;
}

function getEQJ2000(wp) {
    const wpEQ = convertCelestial(wp, CoordinateSys.EQ_J2000);
    if (typeof wpEQ === 'undefined') return '';
    const [ra, dec] = [wpEQ.getLon().toFixed(5), wpEQ.getLat().toFixed(5)];
    const hmsRa = CoordUtil.convertLonToString(ra,  CoordinateSys.EQ_J2000);
    const hmsDec = CoordUtil.convertLatToString(dec,  CoordinateSys.EQ_J2000);
    const res = `<div style="font-size:10px;">
                  ${ra},&nbsp;${dec}&nbsp;Equ J2000
                  &nbsp;&nbsp;&nbsp;or&nbsp;&nbsp;&nbsp;
                  ${hmsRa},&nbsp;${hmsDec}&nbsp;&nbsp;Equ J2000
                </div>`;
    return res;
}

/**
 *
 * @param wp WorldPt
 * @return String
 */
export function formatPosForHelp(wp) {
    if (wp ===null) return '';
    let s;
    const lonStr = sprintf('%.5f',wp.getLon());
    const latStr = sprintf('%.5f',wp.getLat());
    const csys = coordToString(wp.getCoordSys());
    if (wp.getCoordSys().isEquatorial()) {
        const hmsRa = CoordUtil.convertLonToString(wp.getLon(), wp.getCoordSys());
        const hmsDec = CoordUtil.convertLatToString(wp.getLat(), wp.getCoordSys());

        s = `<div style="font-size:10px;">
              ${lonStr},&nbsp;${latStr}&nbsp;&nbsp;${csys}
              &nbsp;&nbsp;&nbsp;or&nbsp;&nbsp;&nbsp;
              ${hmsRa},&nbsp;${hmsDec}&nbsp;&nbsp;${csys}
            </div>`;

        if (wp.getCoordSys() !== CoordinateSys.EQ_J2000) {
            s += getEQJ2000(wp);
        }
    }
    else {
        s = `<div style=\"font-size:10px;\"> 
            ${lonStr},&nbsp;${latStr}&nbsp;&nbsp;${csys}</div>`;
        s += getEQJ2000(wp);
    }
    return s;

}

/**
 *
 * @param wp world point
 * @returns {*}
 */
export function formatPosForTextField(wp) {
    try {
        let lon;
        let lat;
        if (wp.getCoordSys().isEquatorial()) {
            lon = CoordUtil.convertLonToString(wp.getLon(), wp.getCoordSys());
            lat = CoordUtil.convertLatToString(wp.getLat(), wp.getCoordSys());
        }
        else {
            lon= sprintf('%.5f',wp.getLon());
            lat= sprintf('%.5f',wp.getLat());
        }
        return `${lon} ${lat} ${coordToString(wp.getCoordSys())}`;
    } catch (e) {
        return '';
    }
}