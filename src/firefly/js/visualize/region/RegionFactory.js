/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import { RegionType, RegionCsys, RegionValueUnit, regionPropsList,
         makeRegionOptions, RegionValue, RegionDimension, RegionPointType, makeRegionMsg,
         getRegionType, getRegionCoordSys, getRegionPointType, makeRegionFont, makeRegion,
         makeRegionPoint, makeRegionText, makeRegionBox, makeRegionBoxAnnulus,
         makeRegionAnnulus, makeRegionCircle, makeRegionEllipse, makeRegionEllipseAnnulus,
         makeRegionLine, makeRegionPolygon, setRegionPropDefault} from './Region.js';
import validator from 'validator';
import CoordUtil from '../CoordUtil.js';
import {CoordinateSys} from '../CoordSys.js';
import {makeWorldPt, makeImagePt} from '../Point.js';
import {convertAngle} from '../VisUtil.js';
import CsysConverter from '../CsysConverter.js';
import {primePlot} from '../PlotViewUtil.js';
import {visRoot } from '../ImagePlotCntlr.js';
import {logError} from '../../util/WebUtil.js'

import {set, unset, has, get, isEmpty} from 'lodash';
import Enum from 'enum';

const CoordType = new Enum(['lon', 'lat']);
const defaultCoord = 'PHYSICAL';

var RegionParseError = {
    InvalidCoord:   'region coordinate undefined',
    InvalidType:    'region type undefined',
    InvalidParam:   'invalid region description syntax',
    InvalidProp:    'invalid region properties',
    InvalidGlobalProp: 'invalid global properties',
    NotImplemented: 'region type is not yet implemented'
};

function outputError(rg, rgStr, bReport = 1) {
    if (rg.type === RegionType.message || rg.message) {
        if (bReport) logError(rg.message);
    } else if (!rg.options) {
        if (bReport) logError(`[invalid region: no options created]: '${rgStr}'`);
    } else if (rg.type === RegionType.undefined ) {
        if (bReport) logError(`[invalid region: undefined region type] '${rgStr}'`);
    } else {
        return 0;   // no error
    }
    return 1;
}

export class RegionFactory {

    /**
     * parse region description for server generated JSON
     * @param regionData
     * @returns {null}
     */
    static parseRegionJson(regionData) {

        var globalOptions = Object.assign({}, makeRegionOptions({[regionPropsList.COORD]: defaultCoord}));

        return regionData? regionData.reduce ( (prev, region, index) => {
            const rg = RegionFactory.parsePart(region, index, globalOptions);

            if (rg) {            // skip comment line and no good line
                if (outputError(rg, region) ===  0) prev.push(rg);
            }
            return prev;
        }, []) : null;
    }

    // add global coordinate system, coordSys, into globalOptions.
    /**
     * parse ds9 region description, globalOptions.coordSys = 'PHYSICAL'|'IMAGE'|<world_sys_string>
     *                               rg.options.coordSys = RegionSys.PHYSICAL | RegionSys.IMAGE | <RegionSys.xx>
     * @param regionData
     * @param bAllowHeader
     * @param stopAt
     * @returns {array} an array of Region object
     */
    static parseRegionDS9(regionData, bAllowHeader = true, stopAt) {
        const sep = ';';
        const dLeft = ['{', '"', '\''];
        const dRight = ['}', '"', '\''];

        // collect region lines and each line may contain coordinate, region description or both
        var regionLines = regionData.reduce( (rLines, oneLine) => {

            // split each string into a set of units which are separated by ';' with the consideration that the semicolon
            // contained in the text or tag string is not counted as separator.
            // Each unit could contain a coordinate string or a region description string
            var getStringUnits = (oneLine) => {
                var isInText = false;   // check if sep is inside a text string enclosed by a pair of delimiter
                var crtSeg = '';        // current string unit
                var dLimitIdx = -1;
                var isLeftDelimiter = (c) => dLeft.findIndex((t) => (t === c));

                var addNewUnitTo = (prev) => {
                    if (crtSeg.length > 0) {
                        prev.push(crtSeg.slice(0));
                        crtSeg = '';
                    }
                };

                var incUnit = (v) => {
                    crtSeg = crtSeg.concat(v);
                };

                var lUnits = oneLine.split('').reduce((oneLineUnits, v) => {
                                if (v === sep) {
                                    isInText ? incUnit(v) : addNewUnitTo(oneLineUnits);
                                } else {
                                    if (isInText) {
                                        if (v === dRight[dLimitIdx]) {
                                            isInText = false;
                                        }
                                    } else {
                                        dLimitIdx = isLeftDelimiter(v);
                                        isInText = dLimitIdx >= 0;
                                    }
                                    incUnit(v);
                                }
                                return oneLineUnits;
                            }, []);

                if (crtSeg.length > 0) {
                    lUnits.push(crtSeg.slice());
                }
                return lUnits;
            };

            var units = getStringUnits(oneLine);
            var lastIdx = units.length - 1;
            var preCsys = null;                   // coordinate status of previous unit

            // combine the coordinate unit and region description unit into one string line
            var lines = units.reduce((unitSet, crtVal, index) => {
                var crtCsys = getRegionCoordSys(crtVal);

                if (crtCsys !== RegionCsys.UNDEFINED) {    // current string is coordinate unit
                    if (index === lastIdx) {
                        unitSet = [...unitSet, crtVal];
                    } else {
                        preCsys = crtCsys;
                    }
                } else {
                    unitSet = [...unitSet, (preCsys ? `${units[index - 1]};${crtVal}` : crtVal)];
                    preCsys = null;
                }
                return unitSet;
            }, []);

            return [...rLines,...lines];
        }, []);

        var globalOptions = Object.assign({}, makeRegionOptions({[regionPropsList.COORD]: defaultCoord}));

        return regionLines.reduce ( (prev, region, index) => {
            if (!stopAt || stopAt > 0) {
                const rg = RegionFactory.parsePart(region.trim(), index + 1, globalOptions, bAllowHeader);

                if (rg) {            // skip comment line and no good line
                    if (bAllowHeader && rg.type === RegionType.global) {
                        if (outputError(rg, region) == 0) {                 // there is no error, update global options
                            globalOptions = Object.assign({}, rg.options);   // reset global option setting
                        }
                        bAllowHeader = false;
                    } else {
                        if (outputError(rg, region) === 0) {
                            if (stopAt) --stopAt;
                            prev.push(rg);
                        }
                    }

                }
            }
            return prev;
        }, []);
    }
    /**
     * parsePart parses the region data of JSON result (one item from RegionData array)
     * @param regionStr each string is either like coordinate;region_description or region_descrption
     * @param index  line number shown in message
     * @param globalOptions
     * @param bAllowHeader
     * @returns {makeRegion} including makeRegion().message to contain error message if there is
     */
    static parsePart(regionStr, index = -1, globalOptions = null, bAllowHeader = false) {

        const rgMsg = (index === -1) ? regionStr : `<${index}>: ${regionStr}`;
        var rf = new RegionFactory();
        var regionCoord, regionDes, regionOptions;
        var regionParams;
        var tmpAry;
        var rg;        // Region
        var rgProps;   // RegionOptions
        var opInclude = -1;
        var bCoord = false;  // no coordinate definition included in the string

        if (isEmpty(regionStr) || regionStr.startsWith('#')) {    // comment line
            return null;
        }

        // parse the global line (from RegionDS9)
        const GLOBAL = 'global';

        // create Region object with global type,
        // message is set if there is error in global, or options is set
        if (bAllowHeader && regionStr.startsWith(GLOBAL)) {
            rg = makeRegion({options: makeRegionOptions({}), type: RegionType.global});
            var gOpStr = regionStr.slice(GLOBAL.length+1).trim();

            rgProps = rf.parseRegionOptions(gOpStr, opInclude, rg.options );

            if (rgProps.message) {
                rg.message = `[${RegionParseError.InvalidGlobalProp}] ${rgProps.message} at ${rgMsg}`;
                return rg;
            } else {
                Object.keys(rgProps).forEach( (prop) => setRegionPropDefault(prop, rgProps[prop]) );
                set(rgProps, regionPropsList.COORD, defaultCoord);
            }
            return rg;
        }

        // check if coordination system (from RegionDS9)
        tmpAry = regionStr.split(';');
        var csys = getRegionCoordSys(tmpAry[0]);  // test the split first string

        if (csys !== RegionCsys.UNDEFINED) {
            if (globalOptions) globalOptions.coordSys = tmpAry[0].toLowerCase();

            if (tmpAry.length <= 1) {    // pure coordinate string
                return null;
            }
            if (tmpAry.length > 2) {     // description contain ';'
                tmpAry = [tmpAry[0], tmpAry.slice(1).join(';')];     // coordinate and description
            }
        } else if (tmpAry.length >= 2) {  // no coordinate and description contain ';'
            tmpAry = [tmpAry.join(';')];
        }


        if (tmpAry.length > 1) {         // coordinate and description
            regionCoord = tmpAry[0].trim();
            tmpAry.shift();              // remove the coordinate element

            bCoord = true;
        } else {                        // description only
            // default coordinate is PHYSICAL in case not specified
            regionCoord = globalOptions && has(globalOptions, regionPropsList.COORD)  ?
                globalOptions[regionPropsList.COORD] : defaultCoord;
        }

        // separate the region description and property part
        tmpAry = tmpAry[0].split('#');
        if (tmpAry.length > 2) {     // in case '#' occurs in property part, like color=#ffffff
            tmpAry[1] = tmpAry.slice(1).join('#');
        }

        // -- regionDes --
        // '+' or '-' cases
        regionDes = tmpAry[0].trim();
        if (regionDes.startsWith('-') || regionDes.startsWith('+')) {
            if (regionDes[0] === '-') {   // ignore '+'
                opInclude = 0;
            }
            regionDes = regionDes.slice(1);   // rmove the first character, - or +
        }

        // -- regionOptions --
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

        regionParams = rf.parseRegionDescription(regionDes);  // delimiter: ( ) , \s
        if (regionParams.length <= 2) {
            return makeRegionMsg(`[${RegionParseError.InvalidParam}] ${rgMsg}`);
        }

        // check region type
        var regionType; // RegionType
        var pointType;
        var ptIdx;

        // region point with shape in description, ex. circle point x y
        if ((regionType = getRegionType(regionParams[1])) === RegionType.point) {
            if ((pointType = getRegionPointType(regionParams[0])) === RegionPointType.undefined) {
                return makeRegionMsg(`[${RegionParseError.InvalidType}] ${rgMsg}`);
            }
            ptIdx = 2;
        } else {
            regionType = getRegionType(regionParams[0]);
            if (regionType === RegionType.undefined) {
                return makeRegionMsg(`[${RegionParseError.InvalidType}] ${rgMsg}`);
            } else if (regionType === RegionType.vector ||
                regionType === RegionType.ruler ||
                regionType === RegionType.compass ||
                regionType === RegionType.projection ||
                regionType === RegionType.panda ||
                regionType === RegionType.epanda ||
                regionType === RegionType.bpanda ||
                regionType === RegionType.composite) {
                return makeRegionMsg(`[${RegionParseError.NotImplemented}] ${rgMsg}`);
            }
            ptIdx = 1;
        }

        rg = rf.parseRegionParams(regionType, regionParams.slice(ptIdx), regionCsys, pointType);

        if (!rg) {
            return makeRegionMsg(`[${RegionParseError.InvalidParam}] ${rgMsg}`);
        }

        // check region properties
        rgProps = rf.parseRegionOptions(regionOptions, opInclude,  (has(rg, 'options') ? rg.options : null), regionCsys);

        if (rgProps.message) {
            return makeRegionMsg(`[${RegionParseError.InvalidProp}] ${rgProps.message} at ${rgMsg}`);
        }

        rg.options = rgProps;
        rg.desc = bCoord ? regionStr : `${regionCoord.toLowerCase()};${regionStr}`;
        return rg;
    }

    /**
     * get starting and ending index of string value based on given string and pairs of delimiters candidates
     * @param optionStr
     * @param delimiterAry
     * @returns {{sIdx: number, eIdx: number}}
     */
    getValueInPairs(optionStr, delimiterAry) {

        var sIdx = -1, eIdx = -1;
        var rightDelimit = null;

        if (Array.isArray(delimiterAry)) {
            sIdx = delimiterAry.reduce( (prev, pair) => {
                var s;

                if (Array.isArray(pair) && pair.length >= 2) {
                    s = optionStr.indexOf(pair[0]);
                    if (s >= 0 && (prev === -1 || s < prev)) {   // valid delimiter happens prior to end of the string
                        prev = s;
                        rightDelimit = pair[1];
                    }
                }
                return prev;
            }, -1);

            if ( rightDelimit ) {
                eIdx = optionStr.indexOf(rightDelimit, sIdx + 1);
                sIdx++;          // index of beginning of the string value
            }
        }
        return {sIdx, eIdx};
    }

    /**
     * get text string from possible delimiter pairs
     * @param targetStr
     * @param delimiterAry
     * @returns {{text: *, delimiterIndex: *}}
     */
    getStringFromDelimiter( targetStr,...delimiterAry ) {
        var {sIdx, eIdx} = this.getValueInPairs(targetStr, delimiterAry);
        var valueStr = null;

        if (sIdx >= 0 && eIdx >= 0) {
            valueStr = targetStr.substring(sIdx, eIdx);
            sIdx--;    // back to delimiter position
        }

        return { text: valueStr, delimiterIndex: sIdx};
    }

    /**
     * seperate region description elements and extract text string
     * @param regionDes
     * @returns {Array}
     */
    parseRegionDescription(regionDes) {
        var rIdx = regionDes.indexOf(')');
        var lIdx = regionDes.indexOf('(');
        var newdes;

        var getParams = (des) => {
            var pAry, tmpAry;
            var desWithnoText;
            var textRes = this.getStringFromDelimiter(des, ['{', '}'], ['"', '"'], ['\'', '\'']);
            if (textRes.text) {
                desWithnoText = des.slice(0, textRes.delimiterIndex);  // exclude text for further split
            } else {
                desWithnoText = des.slice();
            }

            if (desWithnoText.includes(',')) {
                tmpAry = desWithnoText.split(',');

                pAry = tmpAry.reduce( (prev, t) => {

                    if (t.length === 0 || !t.trim()) { // count empty or blank between ','
                        prev = [...prev, t];
                    } else {
                        var ary = t.trim().split(/\s+/);
                        prev = [...prev, ...ary];
                    }
                    return prev;
                }, []);
            } else {
                pAry = desWithnoText.split(/\s+/);   // blank string is ignored
            }

            if (textRes.text) {
                pAry.push(textRes.text);
            }
            return pAry;
        };


        if ( rIdx >= 0 && lIdx >= 0 && rIdx > lIdx ) {    // with ()
            newdes = regionDes.slice(0, rIdx).replace('(', ' ').trim();
            return getParams(newdes);
        } else if (rIdx < 0 && lIdx < 0) {               // with no ()
            return getParams(regionDes);
        } else {
            return [];
        }
    }

    /**
     * parse region description based on the region type
     * @param regionType
     * @param params
     * @param regionCsys
     * @param pointType
     * @returns {*}
     */

    parseRegionParams(regionType, params, regionCsys, pointType = null) {
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
                if (params.length < 2) break;

                if (wp1 = this.parseXY(regionCsys, params[n], params[++n])) {
                    region = makeRegionText(wp1.pt);
                }
                if (params.length >= 3) {
                    region.options = makeRegionOptions({text: params[2]});
                }
                break;

            case RegionType.point:
                if (params.length < 2) break;

                if (wp1 = this.parseXY(regionCsys, params[n], params[++n])) {
                    region = makeRegionPoint(wp1.pt);
                }
                if (pointType) {
                    region.options = makeRegionOptions({pointType});
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

                // width & height
                while (params.length > (n + 3)) {
                    w = this.convertToRegionValueForDim(params[++n], regionCsys);
                    h = this.convertToRegionValueForDim(params[++n], regionCsys);
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
                if (!(angle = this.convertToRegionValueForAngle(params[++n]))) {
                    break;
                }

                if (isAnnulus) {   // boxannulus
                    region = makeRegionBoxAnnulus(wp1.pt, dimAry, angle);
                } else {
                    region = makeRegionBox(wp1.pt, dimAry[0], angle);
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
                    r = this.convertToRegionValueForDim(params[++n], regionCsys);
                    if (!r) {
                        radAry = [];
                        break;
                    } else {
                        radAry.push(r);
                    }
                }
                if (radAry.length >= 2) {
                    region = makeRegionAnnulus(wp1.pt, radAry);
                }
                break;

            case RegionType.circle:  // 3 params, x, y, radius
                if (params.length < 3) {
                    break;
                }

                if (!(wp1 = this.parseXY(regionCsys, params[n], params[++n]))) {
                    break;
                }

                if (!(r = this.convertToRegionValueForDim(params[++n], regionCsys))) {
                    break;
                }

                region = makeRegionCircle(wp1.pt, r);
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
                    const r1 = this.convertToRegionValueForDim(params[++n], regionCsys);
                    const r2 = this.convertToRegionValueForDim(params[++n], regionCsys);
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

                if (!(angle = this.convertToRegionValueForAngle(params[++n]))) {
                    break;
                }

                if (isAnnulus) {
                    region = makeRegionEllipseAnnulus(wp1.pt, dimAry, angle);
                } else {
                    region = makeRegionEllipse(wp1.pt, dimAry[0], angle);
                }
                break;

            case RegionType.line: // 4 params, x1, y1. x2, y2
                if (params.length !== 4) {
                    break;
                }

                if (!(wp1 = this.parseXY(regionCsys, params[n], params[++n]))) {
                    break;
                }
                if (!(wp2 = this.parseXY(regionCsys, params[++n], params[++n]))) {
                    break;
                }
                region = makeRegionLine(wp1.pt, wp2.pt);
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
                    wpAry.push(wp1.pt);
                    n++;
                }
                if (wpAry.length >= 3) {
                    region = makeRegionPolygon(wpAry);
                }
                break;
            default:
                region = null;
        }

        if (region) {
            region.isOnWorld = wp1.isOnWorld;
        }
        return region;
    }


    /**
     * process region poisition coordinate
     * @param coordSys
     * @param xStr
     * @param yStr
     * @returns {*}
     */
    parseXY(coordSys, xStr, yStr) {

        var {rgValX, rgValY} = xStr && yStr && this.convertToRegionValueForPt(xStr, yStr, coordSys);

        if (!rgValX || !rgValY || rgValX.unit !== rgValY.unit) {
            return null;
        }

        // the coordinate values are refactored based on the coordinate system it works on.
        return this.refactorRegionValueUnit(  rgValX, rgValY, coordSys);
    }

    /**
     * refactor the region coordinate value based on the coordinate system the region is defined at
     * @param rvX
     * @param rvY
     * @param coordsys
     * @returns {*}
     */
    refactorRegionValueUnit(rvX, rvY, coordsys) {

        var isWorldUnit = (c) => (  (c !== RegionCsys.PHYSICAL) &&
                                    (c !== RegionCsys.UNDEFINED) &&
                                    (c !== RegionCsys.IMAGE) &&
                                    (c !== RegionCsys.ICRS) &&
                                    (c !== RegionCsys.AMPLIFIER) &&
                                    (c !== RegionCsys.LINEAR) &&
                                    (c !== RegionCsys.DETECTOR) );

        var makePt = (vx, vy, cs) => {
            if (vx.unit === RegionValueUnit.IMAGE_PIXEL || vx.unit === RegionValueUnit.SCREEN_PIXEL) {
                return makeImagePt(vx.value, vy.value);
            } else {
                return makeWorldPt(vx.value, vy.value, this.parse_coordinate(cs));
            }
        };

        return {pt: makePt(rvX, rvY, coordsys), isOnWorld: isWorldUnit(coordsys)};
        /*
        if (isWorldUnit(coordsys)) {
            pt = cc.getWorldCoords(makePt(rvX, rvY, coordsys));
        } else if (coordsys === RegionCsys.IMAGE ) {
            pt = cc.getImageCoords(makePt(rvX, rvY, coordsys));
        } else if (coordsys === RegionCsys.PHYSICAL) {
            pt = cc.getScreenCoords(makePt(rvX, rvY, coordsys));
        } else {
            pt = null;
        }
        return pt;
        */
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
            case RegionCsys.PHYSICAL:
                cs =  CoordinateSys.EQ_J2000;
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


    /**
     * parse coordinate string into value and unit, convert sexigesimal into degree
     * @param vstr
     * @param coordSys
     * @param vType  lon or lat
     * @returns {*}
     */
    textToValueAndUnit(vstr, coordSys, vType) {
        var unit_char = vstr.charAt(vstr.length-1);
        var unit = RegionValueUnit.CONTEXT;
        var nstr;
        var isTransformationChecked = false;

        if (!validator.isInt(unit_char) && (unit_char !== 's')) {
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
                    unit = RegionValueUnit.IMAGE_PIXEL; // treat 'p' same as 'i' (more detail transformation will be
                                                        // further investigated)
                    isTransformationChecked = true;
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
            var ret;

            if (vType) {
                ret = this.parseCoordinates(nstr, coordSys, vType);
            }
            if (ret) {
                nstr = ret.toString();
                unit = RegionValueUnit.DEGREE;
            } else {
                return null;
            }
        }

        var val = parseFloat(nstr);
        return {unit, val, isTransformationChecked};
    }

    // coordinate: image or pixel coordinate will convert sexagesimal to be in J2000 first (in degree)
    // the degree will be further converted to to in image or pixel unit in refactorRegionValueUnit
    parseCoordinates(vString, coordSys, vType) {
        if (vType === CoordType.lat && coordSys) {
            return CoordUtil.convertStringToLat(vString, this.parse_coordinate(coordSys));
        } else if (vType === CoordType.lon && coordSys) {
            return CoordUtil.convertStringToLon(vString, this.parse_coordinate(coordSys));
        } else {
            return null;
        }
    }

    // angle: xxx, xxxd, xxxr, xxx', xxx'', xxxp or xxxi => xxxr
    /**
     * convert angle value based on the suffixed unit if there is
     * @param vstr
     * @returns {null}
     */
    convertToRegionValueForAngle(vstr) {
        var {unit, val} = this.textToValueAndUnit(vstr);


        // context is for degree unit
        if (unit === RegionValueUnit.CONTEXT) {
            unit = RegionValueUnit.DEGREE;
        } else if (unit === RegionValueUnit.ARCMIN ||
            unit === RegionValueUnit.ARCSEC ||
            unit === RegionValueUnit.RADIAN) {
            val = convertAngle(unit.key, 'degree', val);
            unit = RegionValueUnit.DEGREE;
        } else if (!unit) {
            return null;
        }

        return RegionValue(val, unit);
    }

     /**
     * convert value of position based on the relevant unit
     * @param xStr
     * @param yStr
     * @param coordSys
     * @returns {Array}
     */
    convertToRegionValueForPt(xStr, yStr, coordSys) {
        var {unit: xUnit, val: xVal, isTransformationChecked} = this.textToValueAndUnit(xStr, coordSys, CoordType.lon);
        var {unit: yUnit, val: yVal} = this.textToValueAndUnit(yStr, coordSys, CoordType.lat);

        if (xUnit !== yUnit) {
            return null;
        }

        var unit = xUnit;

        // keep the unit as origianlly indicated if there is
        if (unit === RegionValueUnit.CONTEXT) {
            if (coordSys === RegionCsys.IMAGE) {
                unit = RegionValueUnit.IMAGE_PIXEL;
            } else if (coordSys === RegionCsys.PHYSICAL) {
                unit = RegionValueUnit.IMAGE_PIXEL;
                isTransformationChecked = true;
            } else {
                unit = RegionValueUnit.DEGREE;
            }
        } else if (unit === RegionValueUnit.ARCMIN ||
                   unit === RegionValueUnit.ARCSEC ||
                   unit === RegionValueUnit.RADIAN) {
            xVal = convertAngle(unit.key, 'degree', xVal);
            yVal = convertAngle(unit.key, 'degree', yVal);
            unit = RegionValueUnit.DEGREE;
        } else if (!unit) {
            return null;
        }

        if (isTransformationChecked) {
            var pt = this.transformPhytoImage(xVal, yVal);

            xVal = pt.imgX;
            yVal = pt.imgY;
        }

        return { rgValX: RegionValue(xVal, unit), rgValY: RegionValue(yVal, unit)};
    }

    /**
     * convert value of dimension based on the relevant unit
     * @param vStr
     * @param coordSys
     * @returns RegionValue
     */
    convertToRegionValueForDim(vStr, coordSys) {
        var {unit, val, isTransformationChecked} = this.textToValueAndUnit(vStr, coordSys);

        // keep the unit as origianlly indicated if there is
        if (unit === RegionValueUnit.CONTEXT) {
            if (coordSys === RegionCsys.IMAGE) {
                unit = RegionValueUnit.IMAGE_PIXEL;
            } else if (coordSys === RegionCsys.PHYSICAL) {
                unit = RegionValueUnit.IMAGE_PIXEL;
                isTransformationChecked = true;
            } else {
                unit = RegionValueUnit.DEGREE;
            }
        } else if (unit === RegionValueUnit.ARCMIN ||
                   unit === RegionValueUnit.ARCSEC ||
                   unit === RegionValueUnit.RADIAN) {
            val = convertAngle(unit.key, 'degree', val);
            unit = RegionValueUnit.DEGREE;
        } else if (!unit) {
            return null;
        }

        if (isTransformationChecked) {
            var pt = this.transformPhytoImage(val, 0.0, true);

            val = pt.imgX;
        }

        return RegionValue(val, unit);
    }

    transformPhytoImage(x, y, forDimension = false) {
        var [LTM1_1, LTM1_2, LTM2_1, LTM2_2] = [1, 0, 0, 1];
        var [LTV1, LTV2] = [0, 0];

        var img_x = LTM1_1 * x + LTM1_2 * y;
        var img_y = LTM2_1 * x + LTM2_2 * y;

        if (forDimension) {
            return {imgX: img_x, imgY: img_y};
        } else {
            return {imgX : img_x + LTV1, imgY: img_y + LTV2};
        }
    }

    /**
     * parse the region options
     * @param {string} optionStr
     * @param {int} include
     * @param {object} rgOps
     * @param {object} rgCsys
     * @returns {*}
     */
    parseRegionOptions(optionStr, include, rgOps = null, rgCsys = null) {
        var rgOptions = rgOps ? rgOps : makeRegionOptions({});
        var ops;
        var idx;
        const [ERR, CONT, STOP] = [0, 1, 2];
        const optionsName = [ 'color', 'dashlist','text', 'width','font','select', 'highlite',
                              'dash', 'fixed',  'edit', 'move', 'delete', 'include', 'rotate',
                               'source', 'background', 'line', 'ruler', 'point'];

        if (rgCsys) {
            set(rgOptions, regionPropsList.COORD, rgCsys);
        }

        if (include === 0) {
            rgOptions.include = 0;
        }

        if (!optionStr) {    // no region property, return default region property setting
            return rgOptions;
        }

        var getValueBeforeNextOp = (opStr) => {
            var foundIdx = -1;

            optionsName.forEach((op) => {
                var idx = opStr.indexOf(op);

                if (idx >= 0) {
                    foundIdx = foundIdx < 0 ? idx : Math.min(foundIdx, idx);
                }
            });

            return { sIdx: 0, eIdx: (foundIdx < 0 ? opStr.length : foundIdx-1) };  // opStr[foundIdx-1] is assumed to be blank
        };


        var getValueBeforeChar = (optionStr, delimiterAry) => {
            var sIdx, eIdx;
            var delimiter = ' ';

            if (Array.isArray(delimiterAry)) {
                delimiter = get(delimiterAry, ['0'], ' ');
            }

            sIdx = 0;
            eIdx = optionStr.indexOf(delimiter, (sIdx+1));
            if (eIdx < 0) {
                eIdx = optionStr.length;
            }
            return {sIdx, eIdx};
        };

        var getValueInDelimiters = (optionStr, delimiterAry) => {
            return this.getValueInPairs(optionStr, delimiterAry);
        };

        // extract string match to the regexp
        // delimiterAry : [[' '], ['{', '}'], ['"', '"'] or parsefunc]
        var getOptionValue = (optionStr, getValueFunc, ...delimiterAry) => {
            var valueStr = '';
            var toContinue = CONT;
            var {sIdx=-1, eIdx=-1} = getValueFunc &&
                                    (delimiterAry? getValueFunc(optionStr, delimiterAry) : getValueFunc(optionStr));


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
            var isTrue = (s) => (parseInt(s) !== 0 ? 1 : 0);

            idx = 0;
            if ((idx = ops.indexOf('=', idx)) < 0) {   // no more property
                break;
            } else if (idx === 0) {
                set(rgOptions, regionPropsList.MSG, 'empty region property found');
                return rgOptions;
            }

            opName = ops.substring(0, idx).trim();
            ops = ops.substring(idx + 1).trim();
            if (ops.length === 0) {
                set(rgOptions, regionPropsList.MSG, `invalid setting of ${opName}`);
                break;
            }

            switch (opName) {
                case 'color':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.COLOR, opValRes.valueStr.slice(0));
                    }
                    break;
                case 'width':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.LNWIDTH, parseInt(opValRes.valueStr));
                    }
                    break;
                case 'text':
                    opValRes = getOptionValue(ops, getValueInDelimiters, ['{', '}'], ['"', '"'], ['\'', '\'']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.TEXT, opValRes.valueStr.slice(0));
                    }
                    break;
                case 'tag':
                    opValRes = getOptionValue(ops, getValueInDelimiters, ['{', '}'], ['"', '"'], ['\'', '\'']);
                    if (opValRes.valueStr) {
                        if (has(rgOptions, regionPropsList.TAG)) {
                            rgOptions[regionPropsList.TAG].push(opValRes.valueStr.slice(0));
                        } else {
                            rgOptions[regionPropsList.TAG] = [opValRes.valueStr.slice(0)];
                        }
                    }
                    break;
                case 'font':
                    opValRes = getOptionValue(ops, getValueInDelimiters, ['"', '"']);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.FONT, this.parseFont(opValRes.valueStr));
                    }
                    break;
                case 'highlight':
                case 'highlite':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.HIGHLITE,  isTrue(opValRes.valueStr));
                    }
                    break;
                case 'select':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.SELECT,  isTrue(opValRes.valueStr));
                    }
                    break;
                case 'include':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.INCLUDE, isTrue(opValRes.valueStr));
                    }
                    break;
                case 'edit':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.EDIT,  isTrue(opValRes.valueStr));
                    }
                    break;
                case 'move':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.MOVE, isTrue(opValRes.valueStr));
                    }
                    break;
                case 'rotate':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.ROTATE, isTrue(opValRes.valueStr));
                    }
                    break;
                case 'delete':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.DELETE, isTrue(opValRes.valueStr));
                    }
                    break;
                case 'offsetx':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.OFFX, parseInt(opValRes.valueStr));
                    }
                    break;
                case 'offsety':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.OFFY, parseInt(opValRes.valueStr));
                    }
                    break;
                case 'dash':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.DASH, isTrue(opValRes.valueStr));
                    }
                    break;
                case 'dashlist':
                    opValRes = getOptionValue(ops, getValueBeforeNextOp);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.DASHLIST, opValRes.valueStr.slice(0));
                    }
                    break;
                case 'fixed':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.FIXED, isTrue(opValRes.valueStr));
                    }
                    break;
                case 'point':      // point value is set at the end of the string with type and size
                    opValRes = getOptionValue(ops, getValueBeforeNextOp);
                    if (opValRes.valueStr) {
                        this.parsePointProp(opValRes.valueStr, rgOptions);
                        if (!has(rgOptions, regionPropsList.PTTYPE)) {
                            opValRes.toContinue = ERR;
                        }
                    }
                    break;
                case 'ruler':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.RULER, opValRes.valueStr.slice(0));
                    }
                    break;
                case 'line':
                    opValRes = getOptionValue(ops, getValueBeforeNextOp);
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.LINE, opValRes.valueStr.slice(0));
                    }
                    break;
                case 'source':
                    opValRes = getOptionValue(ops, getValueBeforeChar, ' ');
                    if (opValRes.valueStr) {
                        set(rgOptions, regionPropsList.SOURCE, isTrue(opValRes.valueStr));
                    }
                    break;

                default:
                    set(rgOptions, regionPropsList.MSG, `invalid region property, ${opName},`);
                    return rgOptions;
            }
            if (opValRes.toContinue === CONT) {
                ops = ops.slice(opValRes.endIndex + 1).trim();
            } else if (opValRes.toContinue === STOP) {   // at end of line
                if (include === 0) {
                    set(rgOptions, regionPropsList.INCLUDE, isTrue('0'));
                }
                break;
            } else if (opValRes.toContinue === ERR) {    // parse error
                set(rgOptions, regionPropsList.MSG, `invalid setting of ${opName}`);
                break;
            }
        }

        return rgOptions;
    }

    /**
     * parse point property
     * @param ptStr
     * @param option
     * @returns {*}
     */
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
                    var s;

                    if (pType === RegionPointType.arrow) {
                        s = parseInt(parseFloat(features[1])+0.5);
                    } else {
                        s = parseInt(parseFloat(features[1]) + 0.5) / 2;
                    }

                    set(ops, regionPropsList.PTSIZE, s);
                    set(ops, regionPropsList.PTTYPE, pType);
                }
            }
        }
        return ops;
    }

    /**
     * parse font property
     * @param fontStr
     */
    parseFont(fontStr) {
        const params = fontStr.split(' ');

        return makeRegionFont(...params);

    }
}




