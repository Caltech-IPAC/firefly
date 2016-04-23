/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import { RegionType, RegionCsys, RegionValueUnit,
         RegionOptions, RegionValue, RegionDimension, RegionPointType, RegParseException,
         getRegionType, getRegionCoordSys, getRegionPointType, makeFont,
         makePoint, makeText, makeBox, makeBoxAnnulus, makeAnnulus, makeCircle, makeEllipse, makeEllipseAnnulus,
         makeLine, makePolygon} from './Region.js';
import validator from 'validator';
import {CoordinateSys} from '../CoordSys.js';
import {makeWorldPt} from '../Point.js';
import {convertAngle} from '../VisUtil.js';

var RegionParseError = {
    ErrSyntax: 'wrong region syntax',
    InvalidCoord: 'region coordinate undefined',
    InvalidType:  'region type undefined',
    InvalidParam:  'invalid region syntax',
    InvalidProp: 'invalid region properties'
};

export class RegionFactory {

    static parseRegionJson(RegionData) {
        try {
            return RegionData.reduce ( (prev, region, index) => {
                const rg = RegionFactory.parsePart(region, index);  // skip comment line
                if (rg) {
                    prev.push(rg);
                }
                return prev;
            }, []);
        }
        catch (e) {
            throw e;
        }
    }

    /**
     * parsePart parses the region data of JSON result (one item from RegionData array)
     * @param regionStr
     * @param index
     * @returns {Region} or throw RegParseException
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
            throw new RegParseException(`[${RegionParseError.ErrSyntax}] ${rgMsg}`);
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
            throw new RegParseException(`[${RegionParseError.InvalidCoord}] ${rgMsg}`);
        }


        // check region description syntax and region type
        // [ RegionType, wp.x, wp.y, ...], at least 3 items
        regionParams = regionDes.split(' ');
        if (regionParams.length <= 2) {
            throw new RegParseException(`[${RegionParseError.InvalidParam}] ${rgMsg}`);
        }

        // check region type
        var rgProps;   // RegionOptions
        var rg;        // Region
        var regionType; // RegionType

        if ((regionType = getRegionType(regionParams[0])) === RegionType.undefined) {
            throw new RegParseException(`[${RegionParseError.InvalidType}] ${rgMsg}`);
        }

        // check region properties
        rgProps = rf.parseRegionOptions(regionOptions);


        if (rgProps.message) {
            throw new RegParseException(`[${RegionParseError.InvalidProp}] ${rgProps.message} at ${rgMsg}`);
        }

        rg = rf.parseRegionParams(regionType, regionParams.slice(1), regionCsys);
        if (!rg) {
            throw new RegParseException(`[${RegionParseError.InvalidParam}] ${rgMsg}`);
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
                    region = makeText(wp1);
                }
                break;

            case RegionType.point:
                if (wp1 = this.parseXY(regionCsys, params[n], params[++n])) {
                    region = makePoint(wp1);
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
                    region = makeBoxAnnulus(wp1, dimAry, angle);
                } else {
                    region = makeBox(wp1, dimAry[0], angle);
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
                    region = makeAnnulus(wp1, radAry);
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
                region = makeCircle(wp1, r);
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
                    region = makeEllipseAnnulus(wp1, dimAry, angle);
                } else {
                    region = makeEllipse(wp1, dimAry[0], angle);
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
                region = makeLine(wp1, wp2);
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
                    region = makePolygon(wpAry);
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

        var csys = this.parse_coordinate(coordSys);
        return makeWorldPt(rgValX.value, rgValY.value, csys);
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
                    unit = RegionValueUnit.RADIUS;
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
        } else if (unit === RegionValueUnit.ARCMIN) {
            val = convertAngle('arcmin', 'degree', val);
            unit = RegionValueUnit.DEGREE;
        } else if (unit === RegionValueUnit.ARCSEC) {
            val = convertAngle('arcsec', 'degree', val);
            unit = RegionValueUnit.DEGREE;
        } else if (unit === RegionValueUnit.RADIUS) {
            val = convertAngle('radius', 'degree', val);
            unit = RegionValueUnit.DEGREE;
        }

        return RegionValue(val, unit);
    }

    parseRegionOptions(optionStr, rgOps = null) {
        var rgOptions = rgOps ? rgOps : RegionOptions({});
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
                rgOptions.message = 'empty region property found';
                return rgOptions;
            }

            opName = ops.substring(0, idx);

            ops = ops.substring(idx + 1).trim();
            if (ops.length === 0) {
                rgOptions.message = `invalid setting of ${opName}`;
                break;
            }

            switch (opName) {

                case 'color':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        rgOptions.color = opValRes.valueStr.slice(0);
                    }
                    break;
                case 'width':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        rgOptions.lineWidth = parseInt(opValRes.valueStr);
                    }
                    break;
                case 'text':
                    opValRes = getOptionValue(ops, ['{', '}']);
                    if (opValRes.valueStr) {
                        rgOptions.text = opValRes.valueStr.slice(0);
                    }
                    break;
                case 'font':
                    opValRes = getOptionValue(ops, ['"', '"']);
                    if (opValRes.valueStr) {
                        rgOptions.font = this.parseFont(opValRes.valueStr);
                    }
                    break;
                case 'select':
                case 'highlight':
                case 'highlite':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        rgOptions.highlightable = isTrue(opValRes.valueStr);
                    }
                    break;
                case 'include':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        rgOptions.include = isTrue(opValRes.valueStr);
                    }
                    break;
                case 'edit':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        rgOptions.editable = isTrue(opValRes.valueStr);
                    }
                    break;
                case 'move':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        rgOptions.movable = isTrue(opValRes.valueStr);
                    }
                    break;
                case 'rotate':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        rgOptions.rotatable = isTrue(opValRes.valueStr);
                    }
                    break;
                case 'delete':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        rgOptions.deletable = isTrue(opValRes.valueStr);
                    }
                    break;
                case 'offsetx':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        rgOptions.offsetX = parseInt(opValRes.valueStr);
                    }
                    break;
                case 'offsety':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        rgOptions.offsetY = parseInt(opValRes.valueStr);
                     }
                    break;
                case 'fixed':
                    opValRes = getOptionValue(ops, [' ']);
                    if (opValRes.valueStr) {
                        rgOptions.fixedSize = isTrue(opValRes.valueStr);
                    }
                    break;
                case 'point':      // point value is set at the end of the string with type and size
                    opValRes = getOptionValue(ops, [';']);
                    if (opValRes.valueStr) {
                        this.parsePointProp(opValRes.valueStr, rgOptions);
                        if (!rgOptions.pointType) {
                            opValRes.toContinue = ERR;
                        }
                    }
                    break;
                default:
                    rgOptions.message = `invalid region property, ${opName},`;
                    return rgOptions;
            }
            if (opValRes.toContinue === CONT) {
                ops = ops.slice(opValRes.endIndex + 1).trim();
            } else if (opValRes.toContinue === STOP) {   // at end of line
                break;
            } else if (opValRes.toContinue === ERR) {    // parse error
                rgOptions.message = `invalid setting of ${opName}`;
                break;
            }
        }

        return rgOptions;
    }


    parsePointProp(ptStr, option = null) {
        var ops = option ? option : RegionOptions({});
        const features = ptStr.split(' ');

        ops.pointType = null;
        ops.pointSize = -1;

        if (features.length >= 1) {
            var pType = getRegionPointType(features[0]);

            if ( pType && pType !== RegionPointType.undefined ) {
                if (features.length === 1) {
                    ops.pointType = pType;
                } else if (validator.isFloat(features[1])) {
                    ops.pointSize = parseInt(features[1]);
                    ops.pointType = pType;
                }
            }
        }
        return ops;
    }

    parseFont(fontStr) {
        const params = fontStr.split(' ');

        const name = params.length >= 1 ?  params[0] : null;
        const point = params.length >= 2 ? params[1] : null;
        const weight = params.length >= 3 ? params[2] : null;
        const slant = params.length >= 4 ? params[3] : null;

        return makeFont(name, point, weight, slant);

    }
}




