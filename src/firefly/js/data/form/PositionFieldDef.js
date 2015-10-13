import * as StringUtils from '../../util/StringUtils.js';
import PositionParser from '../../util/PositionParser.js';
import CoordinateSys from '../../visualize/CoordSys.js';
import CoordUtil from '../../visualize/CoordUtil.js';
import {WorldPt} from '../../visualize/Point.js';

import numeral from 'numeral';

/**
 * Created by roby on 12/2/14.
 */
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
        if (StringUtils.isEmpty(s)) {
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
                if (_parser.getCoordSys() ===null) {
                    throw getErrMsg() + '- invalid coordinate system.';
                }

                // validate RA
                var ra = _parser.getRa();
                if (isNaN(ra)) {
                    var raStr = _parser.getRaString();
                    if (hard || (raStr !==null && !(raStr.length === 1 && raStr.charAt(0) === '.'))) {
                        throw getErrMsg() + '- unable to parse RA.';
                    }
                }
                // validate DEC
                var dec = _parser.getDec();
                if (isNaN(dec)) {
                    var decStr = _parser.getDecString();
                    if (hard || (decStr !== null && !(decStr.length === 1 && (decStr.charAt(0) === '+' || decStr.charAt(0) === '-' || decStr.charAt(0) === '.')))) {
                        throw getErrMsg() + '- unable to parse DEC.';
                    }
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
        return 'error: ';
    }

    // -------------------- public methods --------------------
    /**
     *
     * @returns WorldPt
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
     * @param csys coordinate system
     * @returns String
     */
    var coordToString= function(csys) {
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

    };

    /**
     *
     */
    function formatPosForTextField(wp) {
        var retval;
        try {
            var lon;
            var lat;
            if (wp.getCoordSys().isEquatorial()) {
                lon = CoordUtil.convertLonToString(wp.getLon(), wp.getCoordSys());
                lat = CoordUtil.convertLatToString(wp.getLat(), wp.getCoordSys());
            }
            else {
                lon = numeral(wp.getLon()).format('#.xxxxxx');
                lat = numeral(wp.getLat()).format('#.xxxxxx');
            }
            retval = lon + ' ' + lat + ' ' + coordToString(wp.getCoordSys());

        } catch (e) {
            retval = '';
        }
        return retval;

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
            if (retval === null && wp instanceof WorldPt) {
                var rWp = wp;
                if (rWp.getResolver() !==null && rWp.getObjName() !==null) {
                    retval = formatTargetForHelp(rWp);
                }
            }
            if (retval ===null) {
                retval = formatTargetForHelp(new WorldPt(wp.getLon, wp.getLat(), wp.getCoordSys(), name, resolver));
            }
        }
        else {
            name = wp.getObjName();
            resolver = wp.getResolver();
            var s;

            if (name !==null && resolver !==null) {
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



        var lonStr = numeral(wp.getLon()).format('#.0[00000]');
        var latStr = numeral(wp.getLat()).format('#.0[00000]');
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

    };

    function ClientPositionResolverHelper() {

        this.convertStringToLon= function(s, coordSys) {
            try {
                return CoordUtil.convertStringToLon(s, coordSys);
            } catch (e) {
                return NaN;
            }
        };

        this.convertStringToLat= function(s, coordSys) {
            try {
                return CoordUtil.convertStringToLat(s, coordSys);
            } catch (e) {
                return NaN;
            }
        };

        this.resolveName= function() {
            return null;
        };

        this.matchesIgnoreCase= function(s, regExp) {
            return StringUtils.matchesIgCase(s, regExp);
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

var PositionFieldDef = {makePositionFieldDef};
export default PositionFieldDef;



