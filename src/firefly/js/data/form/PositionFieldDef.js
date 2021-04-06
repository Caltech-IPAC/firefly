import PositionParser from '../../util/PositionParser.js';
import CoordinateSys from '../../visualize/CoordSys.js';
import CoordUtil from '../../visualize/CoordUtil.js';
import Point, {makeResolvedWorldPt, makeWorldPt} from '../../visualize/Point.js';

import {sprintf} from '../../externalSource/sprintf';
import {matchesIgCase} from '../../util/WebUtil.js';

/**
 * Created by roby on 12/2/14.
 */
/* eslint prefer-template:0 */
var makePositionFieldDef= function(properties) {
    var _parser = PositionParser.makePositionParser(new ClientPositionResolverHelper());

    var nullAllowed= true;

    if (properties) {
        nullAllowed= (properties.nullAllowed) ? false : properties.nullAllowed;
    }



    var validateSoft = function (aValue) {
        var s = (aValue ===null) ? null : aValue.toString();
        return validateInternal(s, false);
    };


    var validate = function (aValue) {
        var s = (aValue ===null) ? null : aValue.toString();
        return validateInternal(s, true);
    };

    function validateInternal(s, hard) {
        if (!s) {
            if (isNullAllow()) {
                return true;
            } else {
                throw 'You must enter a valid position or name';
            }
        }
        var isValid = _parser.parse(s);

        if (!isValid) {

            // validate ObjName
            if (_parser.getInputType()===PositionParser.PositionParsedInput.Name) {
                if (hard) {
                    throw 'Object names must be more than one character';
                }
            } else {

                // check coordinate system
                if (_parser.getCoordSys() === CoordinateSys.UNDEFINED) {
                    throw getErrMsg() + 'Invalid coordinate system';
                }

                // validate RA
                var ra = _parser.getRa();
                if (isNaN(ra)) {
                    const errRA = _parser.getRAParseError() || 'Unable to parse RA';
                    if (errRA) {
                        throw `${getErrMsg()}${errRA}`;
                    }
                    /*
                    var raStr = _parser.getRaString();

                    if (hard || (raStr !==null && !(raStr.length === 1 && raStr.charAt(0) === '.'))) {
                        throw getErrMsg() + 'Unable to parse RA.';
                    }
                    */
                }
                // validate DEC
                var dec = _parser.getDec();
                if (isNaN(dec)) {
                    const errDec = _parser.getDECParseError() || 'Unable to parse DEC';
                    if (errDec) {
                        throw `${getErrMsg()}${errDec}`;
                    }
                    /*
                    var decStr = _parser.getDecString();
                    if (hard || (decStr !== null && !(decStr.length === 1 && (decStr.charAt(0) === '+' || decStr.charAt(0) === '-' || decStr.charAt(0) === '.')))) {
                        throw getErrMsg() + 'Unable to parse DEC.';
                    }
                    */
                }
            }
        }

        return true;
    }

    // -------------------- public methods --------------------
    function isNullAllow() {
        return nullAllowed;
    }

    function getErrMsg() {
        return 'Error: ';
    }

    // -------------------- public methods --------------------

    /**
     *
     * @return {object} world position
     */
    var getPosition= function() {
        return _parser.getPosition();
    };


    /**
     *
     * returns PositionParser.Input
     */
    var getInputType= function() {
        return _parser.getInputType();
    };

    /**
     *
     * returns String
     */
    var getObjectName= function() {
        return _parser.getObjName();
    };

    var setObjectName= function(name) {
        _parser.setObjName(name);
    };


    /**
     *
     * @param {WorldPt} wp
     * @param {string}name
     * @param {Resolver} resolver
     * @return {string}
     */
    var formatTargetForHelp= function(wp, name, resolver) {
        var retval = null;
        if (!wp) {
            retval = '';
        }

        if (name && resolver) {
            if (retval === null && wp.type=== Point.W_PT) {
                var rWp = wp;
                if (rWp.getResolver() !==null && rWp.getObjName() !==null) {
                    retval = formatTargetForHelp(rWp);
                }
            }
            if (retval ===null) {
                retval = formatTargetForHelp(makeResolvedWorldPt(makeWorldPt(wp.getLon, wp.getLat(), wp.getCoordSys()), name, resolver));
            }
        }
        else {
            name = wp.getObjName();
            resolver = wp.getResolver();
            var s;

            if (name && resolver) {
                s = ' <b>' + name + '</b>' +
                    ' <i>resolved by</i> ' + resolver.desc +
                    '<div  style=\"padding-top:6px;\">' +
                    formatPosForHelp(wp) +
                    '</div>';
            }
            else {
                s = formatPosForHelp(wp);
            }
            return s;
        }

        return retval;
    };

    /**
     *
     * @param wp WorldPt
     * @return String
     */
    function formatPosForHelp(wp) {
        if (wp ===null) {
            return '';
        }
        var s;



        var lonStr = sprintf('%.5f',wp.getLon());
        var latStr = sprintf('%.5f',wp.getLat());
        var csys = coordToString(wp.getCoordSys());
        if (wp.getCoordSys().isEquatorial()) {

            var hmsRa = CoordUtil.convertLonToString(wp.getLon(), wp.getCoordSys());
            var hmsDec = CoordUtil.convertLatToString(wp.getLat(), wp.getCoordSys());

            s = '<div class=\"on-dialog-help faded-text\" style=\"font-size:10px;\">' +
                lonStr + ',&nbsp;' + latStr + '&nbsp;&nbsp;' + csys +
                ' &nbsp;&nbsp;&nbsp;<i>or</i> &nbsp;&nbsp;&nbsp;' +
                hmsRa + ',&nbsp;' + hmsDec + '&nbsp;&nbsp;' + csys +
                '</div>';
        }
        else {
            s = '<div class=on-dialog-help>' +
                lonStr + ',&nbsp;' + latStr + '&nbsp;&nbsp;' + csys + '</div>';
        }
        return s;

    }

    function ClientPositionResolverHelper() {
        let _ra_parse_err = null;
        let _dec_parse_err = null;

        this.convertStringToLon= function(s, coordSys) {
            _ra_parse_err = null;
            try {
                return CoordUtil.convertStringToLon(s, coordSys);
            } catch (e) {
                _ra_parse_err = e;
                return NaN;
            }
        };

        this.convertStringToLat= function(s, coordSys) {
            _dec_parse_err = null;
            try {
                return CoordUtil.convertStringToLat(s, coordSys);
            } catch (e) {
                _dec_parse_err = e;
                return NaN;
            }
        };

        this.resolveName= function() {
            return null;
        };

        this.matchesIgnoreCase= function(s, regExp) {
            return matchesIgCase(s, regExp);
        };

        this.getRAError = function() {
            return _ra_parse_err;
        };

        this.getDECError = function() {
            return _dec_parse_err;
        };
    }


    var retObj = {};
    retObj.validateSoft = validateSoft;
    retObj.validate = validate;
    retObj.getPosition= getPosition;
    retObj.getInputType= getInputType;
    retObj.getObjectName= getObjectName;
    retObj.setObjectName= setObjectName;
    retObj.coordToString= coordToString;
    retObj.formatPosForTextField= formatPosForTextField;
    retObj.formatTargetForHelp= formatTargetForHelp;
    retObj.formatPosForHelp= formatPosForHelp;




    return retObj;

};

var PositionFieldDef = {makePositionFieldDef, formatPosForTextField};
export default PositionFieldDef;

/**
 *
 * @param csys coordinate system
 * @return string
 */
export function coordToString(csys) {
    var retval= '';

    if (csys === CoordinateSys.EQ_J2000)      {
        retval = 'Equ J2000';
    }
    else if (csys === CoordinateSys.EQ_B1950) {
        retval = 'Equ B1950';
    }
    else if (csys === CoordinateSys.GALACTIC) {
        retval = 'Gal';
    }
    else if (csys === CoordinateSys.ECL_J2000) {
        retval = 'Ecl J2000';
    }
    else if (csys === CoordinateSys.ECL_B1950) {
        retval = 'Ecl B1950';
    }

    return retval;

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



