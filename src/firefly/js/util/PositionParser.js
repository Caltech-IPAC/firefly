/* eslint prefer-template:0 */
import {WorldPt} from '../visualize/Point.js';
import * as StringUtils from './StringUtils.js';
import CoordinateSys from '../visualize/CoordSys.js';

var PositionParsedInput={Name: 'Name', Position:'Position'};

var makePositionParser = function(helper) {

    var retPP= {};

    var DEFAULT_COORD_SYS = 'equ j2000';
    var RA = 'ra';
    var DEC = 'dec';
    var COORDINATE_SYS = 'coordsys';
    var INVALID = 'invalid';
    var _ra = null;
    var _dec = null;
    var _coordSys = null;
    var _objName = null;
    var _inputType= PositionParsedInput.Position;
    var isValid= false;

    if (helper === null) {
        helper = {
            convertStringToLon: function() { return NaN; },
            convertStringToLat: function() { return NaN; },
            resolveName       : function() { return null; },
            matchesIgnoreCase : function() { return false; }
        };
    }

    /**
     * parse the given string into the resolver.
     * Returns true if the string is a valid position.
     * @param s
     * @return
     */
    retPP.parse= function(s) {

        isValid = false;
        _ra = null;
        _dec = null;
        _objName = null;
        _coordSys = null;

        if (!StringUtils.isEmpty(s) ) {
            s= StringUtils.crunch(s);
            s= StringUtils.polishString(s); //remove non-standard-ASCII characters.
            _inputType= determineType(s);
            if (_inputType ===PositionParsedInput.Name) {
                if (s.trim().length>1) {
                    _objName= s;
                    isValid = true;
                }
            } else {
                var map= getPositionMap(s);
                _coordSys= getCoordSysFromString(map[COORDINATE_SYS]);
                _ra = map[RA];
                _dec = map[DEC];
                isValid = retPP.getCoordSys() !==CoordinateSys.UNDEFINED && !isNaN(retPP.getRa()) && !isNaN(retPP.getDec());
            }
        }

        return isValid;
    };

    retPP.getInputType= function() {
        return _inputType;
    };

    retPP.isValid= function() {
        return isValid;
    };

    retPP.getRa= function() {
        var v = isNaN(_ra) ? _ra : _ra + 'd';
        return helper.convertStringToLon(v, _coordSys);
    };

    retPP.getDec= function() {
        var v = isNaN(_dec) ? _dec : _dec + 'd';
        return helper.convertStringToLat(v, _coordSys);
    };

    retPP.getRaString= function() {
        return _ra;
    };

    retPP.getDecString= function() {
        return _dec;
    };

    /**
     * returns CoordinateSys.UNDEFINED if it's a bad coordinate system.
     * @return  CoordinateSys object
     */
    retPP.getCoordSys= function() {
        return _coordSys === null ? CoordinateSys.UNDEFINED : _coordSys;
    };

    function getCoordSysFromString(s) {
        return StringUtils.isEmpty(s) ? null : CoordinateSys.parse(s);
    }

    retPP.getObjName= function() {
        return _objName;
    };

    retPP.setObjName= function(objName) {
        isValid = true;
        _inputType = PositionParsedInput.Name;
        _objName = objName;
    };

    retPP.getPosition= function() {
        if (!isValid) {
            return null;
        }
        var wp = null;
        if (retPP.getInputType()===PositionParsedInput.Name) {
            wp = helper.resolveName(retPP.getObjName());
        } else {
            wp = new WorldPt( retPP.getRa(), retPP.getDec(), retPP.getCoordSys());
        }
        return wp;
    };

    // -------------------- static methods --------------------


    function getPositionMap(text) {
        var map = {};
        try {
            var array;
            var parsedText= parseAndConvertToMeta(text).split('&');
            for(var i=0; (i<parsedText.length); i++) {
                array = parsedText[i].split('=');
                if (array.length>1) {
                    map[array[0]]= array[1];
                }
            }
        } catch (e) {
            // do nothing - parse failed
        }
        return map;
    }

    function parseAndConvertToMeta(text) {
        var numericList = [];
        var alphabetList = [];
        var retval;
        var idx;

        //1. convert string to Ra&DEC&{CoordSys} format
        var ra='';
        var dec='';
        var coordSys='';
        var i;
        var item;

        if (text.indexOf(',')>-1) {
            // Ra, DEC {Coord-Sys} case
            var values = text.split(',');
            ra = values[0];
            if (values.length>1) {
                var aVal= values[1].split(/[ ]|[,]/);
                for (i=0; (i<aVal.length); i++) {
                    item= aVal[i];
                    if (item.length>0) {
                        if (isNaN(item)) {
                            alphabetList.push(item);
                        } else {
                            numericList.push(item);
                        }
                    }
                }
                if (numericList.length>0) {
                    for (i=0; (i<numericList.length); i++) {
                        dec += (item+' ');
                    }
                    for (i=0; (i<alphabetList.length); i++) {
                        coordSys += (item+' ');
                    }
                } else {
                    if (alphabetList.length===1) {
                        dec = alphabetList[0];
                        coordSys = DEFAULT_COORD_SYS;
                    } else if (alphabetList.length>1){
                        for (i=0; (i<alphabetList.length); i++) {
                            if (i) {
                                coordSys +=(alphabetList[i]+' ');
                            }
                            else {
                                dec = alphabetList[i];
                            }
                        }
                    }

                }
            }
        } else {
            // Ra DEC {coordSys} case (no comma)
            var tokenAry= text.split(' ');
            var token;
            for (i=0; (i<tokenAry.length); i++) {
                token= tokenAry[i];
                if (token.length>0) {
                    if (isNaN(token)) {
                        alphabetList.push(token);
                    } else {
                        numericList.push(token);
                    }
                }
            }
            if (numericList.length>0) {
                //so we have more than one numeric strings, divide numeric strings
                //list in half, first half = ra, second half = dec.
                if (numericList.length===2) {
                    ra = numericList[0];
                    dec = numericList[1];

                } else if (numericList.length>2) {
                    idx=0;
                    for (i=0; (i<numericList.length); i++) {
                        item= numericList[i];
                        if ((idx++)*2<numericList.length) {
                            ra += (item + ' ');
                        } else {
                            dec += (item + ' ');
                        }
                    }
                } else if (numericList.length===1) {
                    ra= tokenAry[0];
                    if (tokenAry.length>1) {
                        dec= tokenAry[1];
                    }
                }

                if (tokenAry.length>=3) {
                    for (i=0; (i<alphabetList.length); i++) {
                        coordSys += (alphabetList[i]+' ');
                    }
                }
            } else {
                // Ra and DEC are non-numeric strings
                if (alphabetList.length>0) {
                    idx =0;
                    ra = '';
                    dec = '';
                    coordSys = '';
                    var array;
                    for (i=0; (i<alphabetList.length); i++) {
                        item= alphabetList[i];
                        if (idx===0) {
                            if (item.indexOf('+')>0) {
                                // 0042443+411608 case
                                array = item.split('+');
                                ra = array[0];
                                dec = array[1];

                                if (!isNaN(ra)) {
                                    ra = convertRa(ra);
                                }
                                if (!isNaN(dec)) {
                                    dec = convertDEC(dec);
                                }
                            } else if (item.indexOf('-')>0) {
                                // 0042443-411608 case
                                array = item.split('-');
                                ra = array[0];
                                dec = '-'+array[1];

                                if (!isNaN(ra)) {
                                    ra = convertRa(ra);
                                }
                                if (!isNaN(dec)) {
                                    dec = convertDEC(dec);
                                }
                            } else {
                                ra = item;
                            }
                        } else {
                            if (dec.length===0) {
                                dec = item;
                            }
                            else {
                                coordSys += (item+' ');
                            }
                        }
                        idx++;
                    }
                }
            }
        }
        ra = ra.trim();
        dec = dec.trim();
        coordSys = getvalidCoordSys(coordSys);
        if (coordSys.length===0) {
            coordSys = DEFAULT_COORD_SYS;
        }
        retval = RA+'='+ra + '&'+DEC+'=' + dec +'&'+COORDINATE_SYS+'='+coordSys.trim();
        return retval;
    }


    function convertRa(s) {
        var retval;
        var hms= ['','',''];
        var i;
        var c;

        var idx=0;
        for(i=0; (i< s.length); i++) {
            c= s.charAt(i);
            if (hms[idx].length>=2) {
                if (idx < (hms.length-1)) {
                    idx++;
                }
            }
            hms[idx] += c;
        }

        for (i=0; (i<hms.length-1); i++) {
            if (!hms[i].length) {
                hms[i] = '0';
            }
        }
        if (hms[2].length>2) {
            hms[2] = hms[2].substring(0,2)+'.'+hms[2].substring(2);
        }
        retval = hms[0]+' '+hms[1]+' '+hms[2];
        return retval;
    }

    function convertDEC(s) {
        var retval;
        var dms= ['','',''];
        var i;
        var c;

        var idx=0;
        for(i=0; (i< s.length); i++) {
            c= s.charAt(i);
            if (idx < (dms.length-1)) {
                if (idx===0 && ((StringUtils.startsWith(dms[idx],'+')) || (StringUtils.startsWith(dms[idx],'-')))) {
                    if (dms[idx].length>2) {
                        idx++;
                    }
                } else if (dms[idx].length>=2) {
                    idx++;
                }
            }
            dms[idx] += c;
        }

        for (i=0; (i<dms.length-1); i++) {
            if (!dms[i].length) {
                dms[i] = '0';
            }
        }
        if (dms[2].length>2) {
            dms[2] = dms[2].substring(0,2)+'.'+dms[2].substring(2);
        }
        retval = dms[0]+' '+dms[1]+' '+dms[2];
        return retval;
    }

    var F1950 = '^B1950$|^B195$|^B19$|^B1$|^B$';
    var FJ2000 = '^J2000$|^J200$|^J20$|^J2$|^J$';
    var EJ2000 = 'J2000$|J200$|J20$|J2$|J$';
    var E1950 = 'B1950$|B195$|B19$|B1$|B$';
    var SECL = '^ECLIPTIC|^ECL|^EC';
    var SEQ = '^EQUATORIAL|^EQU|^EQ';


    var COMBINE_SYS = '(^ECLIPTIC|^ECL|^EC|^EQUATORIAL|^EQU|^EQ)('+EJ2000+ '|' +E1950+ ')';
    var ECL_1950 = '('+SECL +')('+E1950+ ')';
    var ECL_2000 = '('+SECL +')('+EJ2000+ ')';

    var EQ_1950 = '('+SEQ+')(' +E1950+ ')';
    var EQ_2000 = '('+SEQ+')(' +EJ2000+ ')';


    function getvalidCoordSys(s) {
        var retval='EQ_J2000';
        var array = s.trim().split(' ');

        if (!StringUtils.isEmpty(s) && array.length>0) {
            if (array.length===1 && matches(array[0],COMBINE_SYS)) {
                if (StringUtils.startsWith(array[0].toUpperCase(),'EC')) {
                    if (matches(array[0],ECL_1950)) {
                        retval='EC_B1950';
                    } else if (matches(array[0],ECL_2000)) {
                        retval='EC_J2000';
                    }
                } else if (StringUtils.startsWith(array[0].toUpperCase(),'EQ')) {
                    if (matches(array[0],EQ_1950)) {
                        retval='EQ_B1950';
                    } else if (matches(array[0],EQ_2000)) {
                        retval='EQ_J2000';
                    }
                }
            } else if (matches(array[0],'^EQUATORIAL$|^EQU$|^EQ$|^E$')) {
                if (array.length>1) {
                    if (matches(array[1], F1950)) {
                        retval='EQ_B1950';
                    } else if (matches(array[1], FJ2000)) {
                        retval='EQ_J2000';
                    }
                    else {
                        retval=INVALID+' COORDINATE SYSTEM: '+s;
                    }
                } else {
                    retval='EQ_J2000';
                }
            } else if (matches(array[0],'^ECLIPTIC$|^ECL$|^EC$')) {
                if (array.length>1) {
                    if (matches(array[1], F1950)) {
                        retval='EC_B1950';
                    } else if (matches(array[1], FJ2000)) {
                        retval='EC_J2000';
                    }
                    else {
                        retval=INVALID+' COORDINATE SYSTEM: '+s;
                    }
                } else {
                    retval='EC_J2000';
                }
            } else if (matches(array[0],'^GALACTIC$|^GAL$|^GA$|^G$')) {
                retval='GALACTIC';
            } else if (matches(array[0], FJ2000)) {
                retval='EQ_J2000';
            } else if (matches(array[0], F1950)) {
                retval='EQ_B1950';
            } else {
                retval=INVALID+' COORDINATE SYSTEM: '+s;
            }
        }
        return retval;
    }



    function coordToString(csys) {
        var retval= '';

        if (csys===CoordinateSys.EQ_J2000) {
            retval= 'Equ J2000';
        }
        else if (csys===CoordinateSys.EQ_B1950) {
            retval= 'Equ B1950';
        }
        else if (csys===CoordinateSys.GALACTIC) {
            retval= 'Gal';
        }
        else if (csys===CoordinateSys.ECL_J2000) {
            retval= 'Ecl J2000';
        }
        else if (csys===CoordinateSys.ECL_B1950) {
            retval= 'Ecl B1950';
        }

        return retval;

    }

    function matches(s, regExp) {
        return helper.matchesIgnoreCase(s, regExp);
    }


    /**
     * Determine the type of input.
     * The input is a position if any of the following test are true
     * <ul>
     * <li>name: the first character is not a digit, a '+', or a decimal point
     * <li>position: first string is numeric or is a parse-able lon
     * </ul>
     * @param s the crunched input string
     * @return the Input type
     */
    function determineType(s) {
        var retval;
        var firstChar= s.charAt(0);
        var firstStr= '';
        var sAry;

        if (s.length>=2) {
            if (StringUtils.startsWith(s,'-') || StringUtils.startsWith(s,'+')) {
                sAry= s.substring(1).split(/[+ ,-]/);
                firstStr= s.charAt(0) + ((sAry.length>0) ? sAry[0] : '');

            }
            else {
                sAry= s.split(/[+ ,-]/);
                if (sAry.length>0) {
                    firstStr= sAry[0];
                }
            }
        }


        if (s.length<2) {
            retval= PositionParsedInput.Name;
        }
        else if (isNaN(firstChar) && firstChar!=='+' && firstChar!=='-' && firstChar!=='.') {
            retval= PositionParsedInput.Name;
        }
        else if (!isNaN(firstStr) || !isNaN(helper.convertStringToLon(firstStr,CoordinateSys.EQ_J2000))) {
            retval= PositionParsedInput.Position;
        }
        else {
            retval= PositionParsedInput.Name;
        }
        return retval;
    }

    return retPP;


};

exports.makePositionParser= makePositionParser;
exports.PositionParsedInput= PositionParsedInput;

