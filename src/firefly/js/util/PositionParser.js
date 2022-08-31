/* eslint prefer-template:0 */
import {makeWorldPt} from '../visualize/Point.js';
import CoordinateSys from '../visualize/CoordSys.js';
import {crunch, matchesIgCase} from './WebUtil';
import CoordUtil, {parseDBIdToCoord} from 'firefly/visualize/CoordUtil.js';

export const PositionParsedInputType={Name: 'Name', Position:'Position', DB_ID:'DB_ID'};

const DEFAULT_COORD_SYS = 'equ j2000';
const INVALID = 'invalid';


/**
 * parse the given string into the resolver.
 * Returns true if the string is a valid position.
 * @param s
 * @return {*}
 */
export function parsePosition(s) {

    let ra;
    let dec;
    let coordSys;
    let objName;
    let inputType= PositionParsedInputType.Position;
    let valid= false;
    let raParseErr='';
    let decParseErr='';
    let position;
    s= s?.trim();
    if (!s) return {valid};

    s= crunch(s);
    s= polishString(s); //remove non-standard-ASCII characters.
    inputType= determineType(s);
    if (inputType ===PositionParsedInputType.Name) {
        objName= s;
        valid = s.length>1;
    }
    else if (inputType ===PositionParsedInputType.DB_ID) {
        const r= parseDBIdToCoord(s);
        if (r) {
            ra= ra= r.lon;
            dec= dec= r.lat;
            coordSys= r.cSys;
            valid= true;
            position= makeWorldPt(r.lon,r.lat,coordSys);
        }
    }
    else {
        const {raStr,decStr,csysStr}= parseAndConvertToMeta(s);
        coordSys= getCoordSysFromString(csysStr);
        let validRa= true;
        let validDec= true;
        try {
            ra=  isNaN(Number(raStr)) ? CoordUtil.convertStringToLon(raStr, coordSys) : Number(raStr);
            if (Number(raStr)) {
                if (ra >= 360 || ra < 0) {
                    raParseErr = 'Longitude is out of range [0, 359]';
                    validRa = false;
                    //positionValidateInternal in PositionFieldDef.js expects ra to be NaN to throw an error
                    ra = NaN; //set ra to NaN as it is out of bounds
                }
            }
        } catch (e) {
            raParseErr = e;
            validRa = false;
        }
        try {
            dec= isNaN(Number(decStr)) ? CoordUtil.convertStringToLat(decStr, coordSys) : Number(decStr);
            if (Number(decStr)) {
                if (dec > 90 || dec < -90) {
                    decParseErr = 'Latitude is out of range [-90.0, +90.0]';
                    //positionValidateInternal in PositionFieldDef.js expects dec to be NaN to throw an error
                    dec = NaN; //set dec to NaN as it is out of bounds
                    validDec = false;
                }
            }
        } catch (e) {
            decParseErr = e;
            validDec = false;
        }
        // determineType uses the first string to decide if the input is a position or object name.
        // "12 mus" (a valid object name in NED) would be classified as a position.
        if (!validDec && !Number(decStr)) {
            //validDec may be false when dec is out of range [-90, 90] as well
            inputType = PositionParsedInputType.Name;
            objName = s;
            valid = true;
        } else {
            valid = coordSys !== CoordinateSys.UNDEFINED && validRa && validDec;
            if (valid) position= makeWorldPt(ra,dec,coordSys);
        }

    }
    return { valid, coordSys, inputType, raParseErr, decParseErr, ra, dec, objName, position };
}

// -------------------- static methods --------------------


function parseAndConvertToMeta(text) {
    const numericList = [];
    const alphabetList = [];
    let idx;

    //1. convert string to Ra&DEC&{CoordSys} format
    let ra='';
    let dec='';
    let coordSys='';
    let i;
    let item;

    if (text.indexOf(',')>-1) {
        // Ra, DEC {Coord-Sys} case
        const values = text.split(',');
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
                    dec += (numericList[i] + ' ');
                }
                for (i=0; (i<alphabetList.length); i++) {
                    coordSys += (alphabetList[i]+' ');
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
        const tokenAry= text.split(' ');
        let token;
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
                let array;
                for (i=0; (i<alphabetList.length); i++) {
                    item= alphabetList[i];
                    if (idx===0) {
                        if (item.indexOf('+')>0) {
                            // 0042443+411608 case
                            array = item.split('+');
                            ra = array[0];
                            dec = array[1];
                            if (!isNaN(ra)) ra = convertRa(ra);
                            if (!isNaN(dec)) dec = convertDEC(dec);
                        } else if (item.indexOf('-')>0) {
                            // 0042443-411608 case
                            array = item.split('-');
                            ra = array[0];
                            dec = '-'+array[1];
                            if (!isNaN(ra)) ra = convertRa(ra);
                            if (!isNaN(dec)) dec = convertDEC(dec);
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
    coordSys = getvalidCoordSys(coordSys);
    if (coordSys.length===0) coordSys = DEFAULT_COORD_SYS;
    return {raStr:ra.trim(), decStr:dec.trim(), csysStr:coordSys};
}


function convertRa(s) {
    const hms= ['','',''];
    let i;
    let c;

    let idx=0;
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
    return hms[0]+' '+hms[1]+' '+hms[2];
}

function convertDEC(s) {
    const dms= ['','',''];
    let i;
    let c;

    let idx=0;
    for(i=0; (i< s.length); i++) {
        c= s.charAt(i);
        if (idx < (dms.length-1)) {
            if (idx===0 && ((dms[idx].startsWith('+')) || (dms[idx].startsWith('-')))) {
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
    return dms[0]+' '+dms[1]+' '+dms[2];
}

const F1950 = '^B1950$|^B195$|^B19$|^B1$|^B$';
const FJ2000 = '^J2000$|^J200$|^J20$|^J2$|^J$';
const EJ2000 = 'J2000$|J200$|J20$|J2$|J$';
const E1950 = 'B1950$|B195$|B19$|B1$|B$';
const SECL = '^ECLIPTIC|^ECL|^EC|^EC_|ECL_';
const SEQ = '^EQUATORIAL|^EQU|^EQ|^EQ_';


const COMBINE_SYS = '(^ECLIPTIC|^ECL|^ECL_|^EC|^EC_|^EQUATORIAL|^EQU|^EQ_|^EQ)('+EJ2000+ '|' +E1950+ ')';
const ECL_1950 = '('+SECL +')('+E1950+ ')';
const ECL_2000 = '('+SECL +')('+EJ2000+ ')';

const EQ_1950 = '('+SEQ+')(' +E1950+ ')';
const EQ_2000 = '('+SEQ+')(' +EJ2000+ ')';


function getvalidCoordSys(s) {
    const array = s.trim().split(' ');
    if (!s || array.length===0 ) return 'EQ_J2000';
    const inCoord0= array[0].toUpperCase();
    if (array.length===1 && matches(inCoord0,COMBINE_SYS)) {
        if (inCoord0.startsWith('EC')) {
            if (matches(array[0],ECL_1950)) {
                return 'EC_B1950';
            } else if (matches(inCoord0,ECL_2000)) {
                return 'EC_J2000';
            }
        } else if (inCoord0.startsWith('EQ')) {
            if (matches(inCoord0,EQ_1950)) {
                return 'EQ_B1950';
            } else if (matches(inCoord0,EQ_2000)) {
                return 'EQ_J2000';
            }
        }
    } else if (matches(inCoord0,'^EQUATORIAL$|^EQU$|^EQ$|^EQ_$|^E$')) {
        if (array.length>1) {
            if (matches(array[1], F1950)) {
                return 'EQ_B1950';
            } else if (matches(array[1], FJ2000)) {
                return 'EQ_J2000';
            }
            else {
                return INVALID+' COORDINATE SYSTEM: '+s.trim();
            }
        } else {
            return 'EQ_J2000';
        }
    } else if (matches(inCoord0,'^ECLIPTIC$|^ECL$|^EC$|^EC_$|^ECL_$')) {
        if (array.length>1) {
            if (matches(array[1], F1950)) {
                return 'EC_B1950';
            } else if (matches(array[1], FJ2000)) {
                return 'EC_J2000';
            }
            else {
                return INVALID+' COORDINATE SYSTEM: '+s.trim();
            }
        } else {
            return 'EC_J2000';
        }
    } else if (matches(inCoord0,'^GALACTIC$|^GAL$|^GA$|^G$')) {
        return 'GALACTIC';
    } else if (matches(inCoord0, FJ2000)) {
        return 'EQ_J2000';
    } else if (matches(inCoord0, F1950)) {
        return 'EQ_B1950';
    } else {
        return INVALID+' COORDINATE SYSTEM: '+s.trim();
    }
}

function stringToLon(s, coordSys) {
    try {
        return CoordUtil.convertStringToLon(s, coordSys);
    } catch (e) {
        return NaN;
    }
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
    const firstChar= s.charAt(0);
    let firstStr= '';
    let sAry;

    if (s.length>=2) {
        if (s.startsWith('-') || s.startsWith('+')) {
            sAry= s.substring(1).split(/[+ ,-]/);
            firstStr= s.charAt(0) + ((sAry.length>0) ? sAry[0] : '');

        }
        else if (parseDBIdToCoord(s)) {
            return PositionParsedInputType.DB_ID;
        }
        else {
            sAry= s.split(/[+ ,-]/);
            if (sAry.length>0) firstStr= sAry[0];
        }
    }

    if (s.length<2) {
        return PositionParsedInputType.Name;
    }
    else if (isNaN(firstChar) && firstChar!=='+' && firstChar!=='-' && firstChar!=='.') {
        return PositionParsedInputType.Name;
    }
    else if (!isNaN(firstStr) || !isNaN(stringToLon(firstStr,CoordinateSys.EQ_J2000))) {
        return PositionParsedInputType.Position;
    }
    else {
        return PositionParsedInputType.Name;
    }
}



const matches= (s, regExp) => matchesIgCase(s, regExp);
const getCoordSysFromString= (s) => !s ? null : CoordinateSys.parse(s);
const polishString= (str) => str ? convertExtendedAscii(str) : str;
const replaceAt= (str, index, replacement) => str.substr(0, index) + replacement+ str.substr(index + replacement.length);

function convertExtendedAscii(sbOriginal) {
    if (!sbOriginal) return '';
    let retval= sbOriginal;
    let origCharAsInt;
    for (let isb = 0; isb < retval.length; isb++) {

        origCharAsInt = retval.charCodeAt(isb);
        if (origCharAsInt<=255) {
            switch (origCharAsInt) {
                case 223:
                case 224:
                    retval = replaceAt(retval, isb, '"');
                    break;
                case 150:
                case 151:
                    retval = replaceAt(retval, isb, '-');
                    break;
                default:
                    if (origCharAsInt>127) {
                        retval = replaceAt(retval, isb, '?');
                    }
                    break;
            }
        }
        else {
            switch (retval.charAt(isb)) {
                case '\u2018': // left single quote
                case '\u2019': // right single quote
                case '\u201A': // lower quotation mark
                case '\u2039': // Single Left-Pointing Quotation Mark
                case '\u203A': // Single right-Pointing Quotation Mark
                    retval = replaceAt(retval, isb, '\'');
                    break;

                case '\u201C': // left double quote
                case '\u201D': // right double quote
                case '\u201E': // double low quotation mark
                    retval = replaceAt(retval, isb, '"');
                    break;

                case '\u02DC':
                    retval = replaceAt(retval, isb, '~');
                    break;  // Small Tilde

                case '\u2013': // En Dash
                case '\u2014': // EM Dash
                    retval = replaceAt(retval, isb, '-');
                    break;

                default:
                    if (origCharAsInt>127) {
                        retval = replaceAt(retval, isb, '?');
                    }
                    break;
            }
        }
    }
    return retval;
}
