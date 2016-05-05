/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import { RegionType, RegionCsys, RegionValueUnit, regionPropsList,
         makeRegionOptions, RegionValue, RegionDimension, RegionPointType, makeRegionMsg,
         getRegionType, getRegionCoordSys, getRegionPointType, makeRegionFont,
         makeRegionPoint, makeRegionText, makeRegionBox, makeRegionBoxAnnulus,
         makeRegionAnnulus, makeRegionCircle, makeRegionEllipse, makeRegionEllipseAnnulus,
         makeRegionLine, makeRegionPolygon} from './Region.js';
import validator from 'validator';
import {CoordinateSys} from '../CoordSys.js';
import {makeWorldPt, makeImagePt, makeScreenPt} from '../Point.js';
import {convertAngle} from '../VisUtil.js';
import {set, unset, has} from 'lodash';

var RegionParseError = {
    InvalidCoord:   'region coordinate undefined',
    InvalidType:    'region type undefined',
    InvalidParam:   'invalid region description syntax',
    InvalidProp:    'invalid region properties'
};

export class RegionFactory {

    static parseRegionJson(regionData) {
        return regionData? regionData.reduce ( (prev, region, index) => {
                const rg = RegionFactory.parsePart(region, index);  // skip comment line
                if (rg) {            // skip comment line
                    prev.push(rg);
                }
                return prev;
            }, []) : null;
    }

    /**
     * parsePart parses the region data of JSON result (one item from RegionData array)
     * @param regionStr
     * @param index
     * @returns {makeRegion} including makeRegion().message to contain error message if there is
     */
    static parsePart(regionStr, index = -1) {

        const rgMsg = (index == -1) ? regionStr : `<${index}>: ${regionStr}`;
        var rf = new RegionFactory();
        var regionCoord, regionDes, regionOptions;
        var regionParams;
        var tmpAry;

        // split the region line into regionCoord ; regionParams # regionProps
        tmpAry = regionStr.split(';');
        if (tmpAry.length <= 1) {
            if (tmpAry.length === 1 && tmpAry.startsWith('#')) {
                return null;        // comment line
            }
            // bad syntax
            return makeRegionMsg(`[${RegionParseError.ErrSyntax}] ${rgMsg}`);
        }

        // split string into region coordinate, description and property portions
        regionCoord = tmpAry[0].trim();


        tmpAry = tmpAry[1].split('#');
        regionDes = tmpAry[0].trim();

        if (tmpAry.length >= 2) {
            regionOptions = tmpAry[1].trim();
        } else {
            regionOptions = null;
        }

        // check coordinate system
        var regionCsys = getRegionCoordSys(regionCoord);
        if (regionCsys === RegionCsys.UNDEFINED) {
            return makeRegionMsg(`[${RegionParseError.InvalidCoord}] ${rgMsg}`);
        }


        // check region description syntax and region type
        // [ RegionType, wp.x, wp.y, ...], at least 3 items
        regionParams = regionDes.split(' ');
        if (regionParams.length <= 2) {
            return makeRegionMsg(`[${RegionParseError.InvalidParam}] ${rgMsg}`);
        }

        // check region type
        var rgProps;   // RegionOptions
        var rg;        // Region
        var regionType; // RegionType

        if ((regionType = getRegionType(regionParams[0])) === RegionType.undefined) {
            return makeRegionMsg(`[${RegionParseError.InvalidType}] ${rgMsg}`);
        }

        rg = rf.parseRegionParams(regionType, regionParams.slice(1), regionCsys);
        if (!rg) {
            return makeRegionMsg(`[${RegionParseError.InvalidParam}] ${rgMsg}`);
        }

        // check region properties
        rgProps = rf.parseRegionOptions(regionOptions);

        if (rgProps.message) {
            return makeRegionMsg(`[${RegionParseError.InvalidProp}] ${rgProps.message} at ${rgMsg}`);
        }

        rg.options = rgProps;
        return rg;
    }


    /**
     * parse region description based on the region type
     * @param regionType
     * @param params
     * @param regionCsys
     * @returns {*}
     */

    parseRegionParams(regionType, params, regionCsys) {
        var wp1, wp2;
        var region = null;
        var isAnnulus, n;
        var angle;
        var r, h, w;
        var dimAry = [];
        var radAry = [];

        n = 0;
        switch (regionType) {   // 2 params
            case RegionType.text:
                if (wp1 = this.parseXY(regionCsys, params[n], params[++n])) {
                    region = makeRegionText(wp1);
                }
                break;

            case RegionType.point:
                if (wp1 = this.parseXY(regionCsys, params[n], params[++n])) {
                    region = makeRegionPoint(wp1);
                }
                break;

            case RegionType.box: // 5 (x, y, w, h, angle) or 7 (x, y, w, h, w, h, angle) params or more
                if (params.length < 5) {
                    break;
                }

                if (!(wp1 = this.parseXY(regionCsys, params[n], params[++n]))) {
                    break;    // wrong x, y
                }

                dimAry = [];
                isAnnulus = -1;

                while (params.length > (n + 3)) {
                    w = this.convertToRegionValue(params[++n], regionCsys);
                    h = this.convertToRegionValue(params[++n], regionCsys);
                    if (!h || !w) {
                        isAnnulus = -1;    // width or height error
                        break;
                    } else {
                        dimAry.push(RegionDimension(w, h));
                    }
                    isAnnulus++;
                }

                if (isAnnulus < 0) {   //  width or height error
                    break;
                }
                if (!(angle = this.convertToRegionValue(params[++n], regionCsys))) {
                    break;
                }

                if (isAnnulus) {   // boxannulus
                    region = makeRegionBoxAnnulus(wp1, dimAry, angle);
                } else {
                    region = makeRegionBox(wp1, dimAry[0], angle);
                }
                break;

            case RegionType.annulus: // at least 4 params, x, y, r1, r2, ...
                if (params.length < 4) {
                    break;
                }

                if (!(wp1 = this.parseXY(regionCsys, params[n], params[++n]))) {
                    break;
                }

                radAry = [];

                while (params.length > (n + 1)) {
                    r = this.convertToRegionValue(params[++n], regionCsys);
                    if (!r) {
                        radAry = [];
                        break;
                    } else {
                        radAry.push(r);
                    }
                }
                if (radAry.length >= 2) {
                    region = makeRegionAnnulus(wp1, radAry);
                }
                break;

            case RegionType.circle:  // 3 params, x, y, radius
                if (params.length < 3) {
                    break;
                }

                if (!(wp1 = this.parseXY(regionCsys, params[n], params[++n]))) {
                    break;
                }

                if (!(r = this.convertToRegionValue(params[++n], regionCsys))) {
                    break;
                }
                region = makeRegionCircle(wp1, r);
                break;

            case RegionType.ellipse:  // 4 params x, y, r1, r2, r3, r4,.. angle
                if (params.length < 4) {
                    break;
                }

                if (!(wp1 = this.parseXY(regionCsys, params[n], params[++n]))) {
                    break;
                }

                isAnnulus = -1;
                dimAry = [];

                while (params.length > (n + 3)) {
                    const r1 = this.convertToRegionValue(params[++n], regionCsys);
                    const r2 = this.convertToRegionValue(params[++n], regionCsys);
                    if (!r1 || !r2) {
                        isAnnulus = -1;    // width or height error
                        break;
                    } else {
                        dimAry.push(RegionDimension(r1, r2));
                    }
                    isAnnulus++;
                }

                if (isAnnulus < 0) {   //  width or height error
                    break;
                }

                if (!(angle = this.convertToRegionValue(params[++n], regionCsys))) {
                    break;
                }

                if (isAnnulus) {
                    region = makeRegionEllipseAnnulus(wp1, dimAry, angle);
                } else {
                    region = makeRegionEllipse(wp1, dimAry[0], angle);
                }
                break;

            case RegionType.line: // 4 params, x1, y1. x2, y2
                if (params.length < 4) {
                    break;
                }

                if (!(wp1 = this.parseXY(regionCsys, params[n], params[++n]))) {
                    break;
                }
                if (!(wp2 = this.parseXY(regionCsys, params[++n], params[++n]))) {
                    break;
                }
                region = makeRegionLine(wp1, wp2);
                break;

            case RegionType.polygon:  // at least 6 params x1, y1, x2, y2, x3, y3, ...
                if (params.length < 6) {
                    break;
                }

                var wpAry = [];

                while (params.length > (n + 1)) {
                    wp1 = this.parseXY(regionCsys, params[n], params[++n]);
                    if (!wp1) {
                        wpAry = [];
                        break;
                    }
                    wpAry.push(wp1);
                    n++;
                }
                if (wpAry.length >= 3) {
                    region = makeRegionPolygon(wpAry);
                }
                break;
            default:
                region = null;
        }

        return region;
    }

    parseXY(coordSys, xStr, yStr) {
        var rgValX = this.convertToRegionValue(xStr, coordSys);
        var rgValY = this.convertToRegionValue(yStr, coordSys);

        if (!rgValX || !rgValY) {
            return null;
        }

        if (coordSys === RegionCsys.IMAGE) {
            return makeImagePt(rgValX.value, rgValY.value);
        } else if (coordSys === RegionCsys.SCREEN_PIXEL) {
            return makeScreenPt(rgValX.value, rgValY.value);
        } else {
            var csys = this.parse_coordinate(coordSys);

            return makeWorldPt(rgValX.value, rgValY.value, csys);
        }
    }


    parse_coordinate(coordSys) {
        var cs;

        switch (coordSys) {
            case RegionCsys.FK4:
            case RegionCsys.B1950:
                cs = CoordinateSys.EQ_B1950;
                break;
            case RegionCsys.FK5:
            case RegionCsys.J2000:
            case RegionCsys.ICRS:
                cs = CoordinateSys.EQ_J2000;
                break;
            case RegionCsys.ECLIPTIC:
                cs = CoordinateSys.ECL_J2000;
                break;
            case RegionCsys.GALACTIC:
                cs = CoordinateSys.GALACTIC;
                break;
            case RegionCsys.IMAGE:
                cs = CoordinateSys.PIXEL;
                break;
            case RegionCsys.PHYSICAL:
                cs = CoordinateSys.SCREEN_PIXEL;
                break;
            case RegionCsys.LINEAR:
            case RegionCsys.AMPLIFIER:
            case RegionCsys.DETECTOR:
            case RegionCsys.UNDEFINED:
                cs = CoordinateSys.UNDEFINED;
                break;
            default:
                cs = CoordinateSys.EQ_J2000;
        }
        return cs;
    }

    convertToRegionValue(vstr, coordSys) {
        var unit_char = vstr.charAt(vstr.length-1);
        var unit = RegionValueUnit.CONTEXT;
        var nstr;

        if (!validator.isInt(unit_char)) {
            switch(unit_char) {
                case '"':
                    unit = RegionValueUnit.ARCSEC;
                    break;
                case '\'':
                    unit = RegionValueUnit.ARCMIN;
                    break;
                case 'd':
                    unit = RegionValueUnit.DEGREE;
                    break;
                case 'r':
                    unit = RegionValueUnit.RADIAN;
                    break;
                case 'p':
                    unit = RegionValueUnit.SCREEN_PIXEL;
                    break;
                case 'i':
                    unit = RegionValueUnit.IMAGE_PIXEL;
                    break;
                default:
                    unit = RegionValueUnit.CONTEXT;
            }
            nstr = vstr.substring(0, vstr.length-1);
        } else {
            nstr = vstr.slice();
        }

        // check if the string is a valid float number (including integer)
        if (!validator.isFloat(nstr)) {
            return null;
        }

        var val = parseFloat(nstr);

        if (unit === RegionValueUnit.CONTEXT) {
            if (coordSys === RegionCsys.IMAGE) {
                unit = RegionValueUnit.IMAGE_PIXEL;
            } else if (coordSys === RegionCsys.PHYSICAL) {
                unit = RegionValueUnit.SCREEN_PIXEL;
            } else {
                unit = RegionValueUnit.DEGREE;
            }
        } else if (unit === RegionValueUnit.ARCMIN ||
                   unit === RegionValueUnit.ARCSEC ||
                   unit === RegionValueUnit.RADIAN) {
            val = convertAngle(unit.key, 'degree', val);
            unit = RegionValueUnit.DEGREE;
        }

        return RegionValue(val, unit);
    }

    parseRegionOptions(optionStr, rgOps = null) {
        var rgOptions = rgOps ? rgOps : makeRegionOptions({});
        var ops;
        var idx;
        const [ERR, CONT, STOP] = [0, 1, 2];

        if (!optionStr) {    // no region property, return default region property setting
            return rgOptions;
        }
        // extract property value prior to delimiter or between the delimters
        var getOptionValue = (optionStr, delimiter) => {
            var sIdx = -1, eIdx = -1;
            var valueStr = '';
            var toContinue = CONT;

            if (delimiter.length === 1) {  // extract value before the delimiter
                sIdx = 0;
                eIdx = optionStr.indexOf(delimiter[0], (sIdx + 1));
                if (eIdx < 0) {
                    eIdx = optionStr.length;     // end of line
                }
            } else if (delimiter.length > 1) {
                sIdx = optionStr.indexOf(delimiter[0])+1;
                if (sIdx > 0) {
                    eIdx = optionStr.indexOf(delimiter[1], sIdx+1);
                }
            }
            if (sIdx >= 0 && eIdx >= 0) {
                valueStr = optionStr.substring(sIdx, eIdx).trim();
                if (eIdx >= optionStr.length - 1) {   // hit the end of line
                    toContinue = STOP;
                }
            }
            if (!valueStr) {             // empty string
                toContinue = ERR;
            }

            return {endIndex: eIdx, valueStr, toContinue};
        };

        ops = optionStr.slice(0);
        // return null: if some property syntax is not correct
        while (true) {
            var opName;
            var opValRes = {};
            var isTrue = (s) => (parseInt(s) !== 0);

            idx = 0;
            if ((idx = ops.indexOf('=', idx)) < 0) {   // no more property
                break;
            } else if (idx === 0) {
                set(rgOptions, regionPropsList.MSG, 'empty region property found');
                return rgOptions;
            }

            opName = ops.substring(0, idx);

            ops = ops.substring(idx + 1).trim();
            if (ops.length === 0) {
                set(rgOptions, regionPropsList.MSG, `invalid setting of ${opName}`);
                break;
            }

            switch (opName) {

                case 'color':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.COLOR, opValRes.valueStr.slice(0));
                    }
                    break;
                case 'width':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.LNWIDTH, parseInt(opValRes.valueStr));
                    }
                    break;
                case 'text':
                    opValRes = getOptionValue(ops, ['{', '}']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.TEXT, opValRes.valueStr.slice(0));
                    }
                    break;
                case 'font':
                    opValRes = getOptionValue(ops, ['"', '"']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.FONT, this.parseFont(opValRes.valueStr));
                    }
                    break;
                case 'select':
                case 'highlight':
                case 'highlite':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.HIGHLITE,  isTrue(opValRes.valueStr));
                    }
                    break;
                case 'include':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.INCLUDE, isTrue(opValRes.valueStr));
                    }
                    break;
                case 'edit':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.EDIT,  isTrue(opValRes.valueStr));
                    }
                    break;
                case 'move':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.MOVE, isTrue(opValRes.valueStr));
                    }
                    break;
                case 'rotate':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.ROTATE, isTrue(opValRes.valueStr));
                    }
                    break;
                case 'delete':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.DELETE, isTrue(opValRes.valueStr));
                    }
                    break;
                case 'offsetx':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.OFFX, parseInt(opValRes.valueStr));
                    }
                    break;
                case 'offsety':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.OFFY, parseInt(opValRes.valueStr));
                     }
                    break;
                case 'fixed':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.FIXED, isTrue(opValRes.valueStr));
                    }
                    break;
                case 'point':      // point value is set at the end of the string with type and size
                    opValRes = getOptionValue(ops, [';']);
                    if (opValRes.valueStr) {
                        this.parsePointProp(opValRes.valueStr, rgOptions);
                        if (!has(rgOptions, regionPropsList.PTTYPE)) {
                            opValRes.toContinue = ERR;
                        }
                    }
                    break;
                default:
                    set(rgOptions, regionPropsList.MSG, `invalid region property, ${opName},`);
                    return rgOptions;
            }
            if (opValRes.toContinue === CONT) {
                ops = ops.slice(opValRes.endIndex + 1).trim();
            } else if (opValRes.toContinue === STOP) {   // at end of line
                break;
            } else if (opValRes.toContinue === ERR) {    // parse error
                set(rgOptions, regionPropsList.MSG, `invalid setting of ${opName}`);
                break;
            }
        }

        return rgOptions;
    }


    parsePointProp(ptStr, option = null) {
        var ops = option ? option : makeRegionOptions({});
        const features = ptStr.split(' ');

        if (has(option, regionPropsList.PTTYPE)) {
            unset(option, regionPropsList.PTTYPE);
        }
        if (has(option, regionPropsList.PTSIZE)) {
            unset(option, regionPropsList.PTSIZE);
        }
        if (features.length >= 1) {
            var pType = getRegionPointType(features[0]);

            if ( pType && pType !== RegionPointType.undefined ) {
                if (features.length === 1) {
                    set(ops, regionPropsList.PTTYPE, pType);
                } else if (validator.isFloat(features[1])) {
                    set(ops, regionPropsList.PTSIZE, parseInt(features[1]));
                    set(ops, regionPropsList.PTTYPE, pType);
                }
            }
        }
        return ops;
    }

    parseFont(fontStr) {
        const params = fontStr.split(' ');

        return makeRegionFont(...params);

    }
}




