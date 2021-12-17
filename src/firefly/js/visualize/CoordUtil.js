/*eslint no-empty:0*/
/*eslint quotes:0*/
import {sprintf} from '../externalSource/sprintf';
import {CoordinateSys} from './CoordSys.js';
import {isDigit} from 'firefly/util/MathUtil.js';

/**
 *  This class provides conversion of user's
 *  coordinate string to a double
 *  Translated to javascript from java from Rick Ebert's coord.c
 *
 *  April 10, 2001
 *  Copied from the original Coord.java, modified to provide the convenient
 *  static methods to do validation, formatting, and conversion from decimal
 *  degree to HMS/DMS string and vice versa.  -xiuqin
 *  March 26, 2021 - Trey
 *  Cleaned up the code more for JavaScript, cut out about 400 lines, removed dd2sex functionality that we don't use.
 *  Still more cleanup could be done
 *  @author Booth Hartley, G.Turek, Xiuqin Wu
 */
const MAX_PRECISION = 8;
const FORM_DMS = 1;             // 12d34m23.4s
const FORM_HMS = 2;             // 12h34m23.4s
const LAT_OUT_RANGE = 'Latitude is out of range [-90.0, +90.0]';
const LON_TOO_BIG = 'Longitude is too big (>=360.0)';
const LON_NEGATIVE = 'Longitude can not be negative';
const RA_TOO_BIG = 'RA is too big (>=24 hours)';
const INVALID_STRING = 'Invalid Input';
const HMS_FOR_LAT = 'HMS notation not valid for latitude';
const SEX_FOR_NONE_EQU = 'Sexagesimal for non-Equatorial coordinate';
const MIN_SEC_TOO_BIG = 'Greater than 60 minutes or seconds';
const INVALID_SEPARATOR = 'Invalid input';



/**
 * @param coordstr
 * @param islat true if the coordstr is latitude
 * @param isequ true if the coordstr is in equatorial system
 */
const sex2dd = function (coordstr, islat, isequ) {
    const sep = [];
    const part = [];
    const point = [];
    let isdec= false;
    let degrees = -1;
    let p = coordstr.trim();

    if (!p.length) throw 'length 0';

    let sign = 1;  // assume positive
    if (p.charAt(0) === '-') {
        sign = -1;
        p = p.substring(1);
    }
    else if (p.charAt(0) === '+') {
        sign = 1;
        p = p.substring(1);
    }
    p = p.trim();  // allow space between sign and nbr
    if (!p.length) throw INVALID_STRING;

    for (let i = 0; i < 3 && (p.length > 0); i++) {
        let pointseen = 0;
        let done = false;
        let numseen = 0;
        let r = undefined;

        while ((p.length > 0) && !done) {
            if (p.charAt(0) === '.') {
                if (pointseen === 1) throw INVALID_STRING;
                pointseen = 1;
                if (!r) r = p;   //Mark this place - number starts here
            }
            else if (isDigit(p.charAt(0))) {
                if (!r) r = p;  //Mark this place - number starts here
                numseen++;
            }
            else {
                done= true;
            }
            if (!done) p = p.substring(1);
        }  // end of inner-while

        if (numseen===0) throw INVALID_STRING;

        //convert it and save it
        part[i] = parseFloat(r?.substring(0, numseen + pointseen));

        r = p;  // now deal with the separator
        p = p.trim();
        sep[i] = cSeparator(p);
        if (sep[i] === '\0') { // this character is NOT a separator
            if (r.charAt(0) !== ' ') throw INVALID_STRING;
            sep[i] = ' ';
            p = r;
        }
        point[i] = Boolean(pointseen);
        if (p.length > 0) p = p.substring(1);
        p = p.trim();
    } // outer-for loop ends here

    // when we get here we've cracked as many as 3 parts
    // if there's anything left, the whole string is junk
    if (p.trim().length > 0) throw INVALID_STRING;

    // Another way to be junk is to have a decimal point in any but
    // the 'last' part
    if (point.slice(0,point.length-1).some( (pt) => pt)) throw 'Invalid input';
    // must has found 1 to 3 parts
    if (part.length===0 || part.length>3) throw INVALID_STRING;


    // and another way to be junk is to use inconsistent or out
    // of order separators
    // Here we also crack whether the input was "decimal" or not

    const sepJoin= sep.join(''); // join all the separators for easier testing
    if (part.length===3) {
        if (sepJoin==='hms' || sepJoin==='hm ') degrees = 0;
        else if (sepJoin==='dms' || sepJoin==='dm ' || sepJoin===`d'"` || sepJoin===`d' `) degrees = 1;
        else if (sepJoin===':: ' || sepJoin==='   ') degrees = -1;
        else throw INVALID_SEPARATOR;
    }
    else if (part.length===2) {
        if (sepJoin==='hm' || sepJoin==='h ') degrees = 0;
        else if (sepJoin==='dm' || sepJoin==='d ' || sepJoin===`d'` ||  sepJoin===`'"` || sepJoin===`' `) degrees = 1;
        else if (sepJoin==='ms' || sepJoin==='m ' || sepJoin===': ' || sepJoin==='  ') degrees = -1;
        else throw INVALID_SEPARATOR;
    }
    else if (part.length===1) {
        if (sep[0] ==='h') {
            degrees = 0;
        } else if (sep[0] ==='d') {
            degrees = 1;
            isdec = true;
        } else if (sep[0] ===' ') {
            degrees = -1;
            isdec= !(isequ && !islat);
        } else if (sep[0] ==='m' || sep[0] ==='s') {
            degrees = -1;
        } else if (sep[0] ===`'` || sep[0] ==='"') {
            degrees = 1;
        } else {
            throw INVALID_SEPARATOR;
        }
    }

    if (degrees===-1) {
        if (islat)  degrees = 1;     // input is a latitude
        else if (isequ) degrees = 0; // input is equatorial longitude (RA)
        else degrees = 1;            // all else is DMS
    }

    // No HMS for latitudes, not in degree format
    if (degrees=== 0 && islat) throw HMS_FOR_LAT;

    // No sexagesimal input for non-equatorial systems
    if (!isequ && !isdec) throw SEX_FOR_NONE_EQU;


    // now modularize the input and convert to a double

    if (part.length > 1) {
        for (let i = part.length - 1; i >= 1; i--) {
            if (part[i] >= 60.0) throw MIN_SEC_TOO_BIG;
        }
    }

    const modangle = (degrees !==0) ? 360.0 : 24.0;

    let returnAngle= 0;
    for (let i = 0, base = 1.0; i < part.length; i++, base *= 60.0) {
        returnAngle += part[i] / base;
    }

    // the input might be  xxm[xx[.xx]s] or just xx[.xx]s or xx[.xx]m
    // so we apply appropriate weighting - whether it's min arc or time
    // we worry about further on.

    if (sep[0] ==='m' || sep[0] ==='\'') returnAngle /= 60.0;
    else if (sep[0] ==='s' || sep[0] ==='"') returnAngle /= 3600.0;

    returnAngle *= sign;

    if (islat) {
        if (returnAngle > 90.0 || returnAngle < -90.0) throw LAT_OUT_RANGE;
    }
    else  { // input is longitude
        if (returnAngle >= modangle) {
            if (degrees !==0) throw LON_TOO_BIG;
            else throw RA_TOO_BIG;
        }
        else if (returnAngle < 0.0) {
            throw LON_NEGATIVE;
        }
    }

    // make angle degrees if it isn't already and needs to be,
    // from hours to degrees.
    // we've already disallowed 'hours' as a valid form for latitudes
    if (degrees ===0) returnAngle *= 15.0;

    return returnAngle;
};


/**
 * Converts decimal angle to string format
 * @param {number} dangle decimal angle
 * @param {boolean} islat true if the coordstr is latitude
 * @param {boolean} isequ true if the coordstr is in equatorial system
 * @param {number} precision for DMS
 * @return String representation of angle
 */
export const dd2sex = function (dangle, islat, isequ, precision=5) {
    const cPrecision= (precision <= 0) ? 0: (precision >= MAX_PRECISION) ? MAX_PRECISION : precision;

    if ((dangle < 0.0) && !islat) dangle = (dangle % 360.0) + 360.0;
    if ((dangle >= 360.0) && !islat) dangle = dangle % 360.0;

    // sign for Latitude/Dec  '+' or '-'
    if (!isequ){  // form == form_decimal_degree or otherwise unrecognized
        return ((islat && dangle >= 0) ? '+' : '') + sprintf(`%.${cPrecision}f`,dangle) +'d';
    }

    const form=islat ? FORM_DMS : FORM_HMS;

    if (form===FORM_HMS)  dangle /= 15.0;   // convert degree to hours

    const circ = form===FORM_HMS ? 24 : 360;
    const secPrecision = form===FORM_HMS ? cPrecision - 3 : cPrecision - 4;
    const isign =  ((dangle < 0.0) && islat) ? -1 : 1;
    const signstr= isign ===1 ? islat ? '+' : '' : '-';

    let tmp = Math.abs(dangle);
    const degOrHours = Math.floor(tmp);
    tmp -= degOrHours;
    tmp *= 60.0;
    const minutes = Math.floor(tmp);
    tmp -= minutes;
    tmp *= 60.0;
    const seconds = Math.floor(tmp);
    tmp -= seconds;
    const dfs = tmp;

    let rs = seconds;
    let rm = minutes;
    let rhd = degOrHours;
    const ofs = Math.floor(Math.pow(10, secPrecision));
    let rfs = Math.round(dfs * ofs);
    if (rfs >= ofs) {
        rfs -= ofs;
        rs++;
    }
    if (rs >= 60) {
        rs -= 60;
        rm++;
    }
    if (rm >= 60) {
        rm -= 60;
        rhd++;
    }
    if (rhd >= circ) rhd -= circ;

    return signstr + rhd + (form===FORM_HMS ? 'h' : 'd')+
        sprintf('%02d',rm) + 'm' +
        sprintf('%02d',rs) + '.' + sprintf(`%0${secPrecision}d`,rfs) + 's';
};



function makeSecStr(s) {
    if (s.length<5) return '';
    const secPart= s.substring(4);
    if (s.length===5 || s.length===6 || s.includes('.')) return secPart;
    return secPart.substring(0,2) +'.' + secPart.substring(2);
}

function coordSysFromFirstChar(c) {
    switch (c.toLowerCase()) {
        case 'g' : return CoordinateSys.GALACTIC;
        case 'f' :
        case 'j' : return CoordinateSys.EQ_J2000;
        case 'b' : return CoordinateSys.EQ_B1950;
        default:   return CoordinateSys.EQ_J2000;
    }
}

/**
 *
 * @param s
 * return {{lon:number,lat:number,cSys:CoordinateSys}|false}
 */
export function parseDBIdToCoord(s) {
    if (!s) return false;
    if (!(['g','f','j','b'].includes(s[0].toLowerCase())) && !isDigit(s[0])) return false;
    if (!s.includes('-') && !s.includes('+')) return false;
    if (s.includes(' ')) return false;
    const firstChar= s[0].toUpperCase();
    const start= isDigit(s[0]) ? 0 : 1;
    const realS= start===0 ? s : s.substring(1);
    const sAry= realS.split(/[+ ,-]/);
    if (sAry.length!==2) return false;
    if (!Number(sAry[0]) || !Number(sAry[1])) return false;
    const sign= realS.includes('+') ? 1 : -1;
    const cSys= coordSysFromFirstChar(firstChar);
    if (cSys===CoordinateSys.GALACTIC) {
        const glon= Number(sAry[0]);
        const glat= Number(sAry[1])*sign;
        if (glon<=360 && glat<=Math.abs(180)) return {lon:glon,lat:glat,cSys};
        return false;
    }
    const [lonStr,latStr]= sAry;
    if (lonStr.length<4 || latStr.length<4) return false;

    const lon= `${lonStr[0]}${lonStr[1]}:${lonStr[2]}${lonStr[3]}:${makeSecStr(lonStr)}`;
    const lat= `${sign>0?'+':'-'}${latStr[0]}${latStr[1]}:${latStr[2]}${latStr[3]}:${makeSecStr(latStr)}`;
    try {
        return {lon:convertStringToLon(lon, cSys),lat:convertStringToLat(lat, cSys),cSys};
    }
    catch (e) {
        return false;
    }
}

function cSeparator(str) {
    if (!str) return ' ';
    const char0= str.charAt(0).toLowerCase();
    return [ 'h', 'd', 'm', 's', ':', '\'', '"'].includes(char0) ? char0 : '\0';
}

const validLon = (hms, coordSystem) => {
    try {
        convertStringToLon(hms, coordSystem);
        return true;
    } catch (e) {
        return false;
    }
};

const convertLonToString = (lon, coordSystem) => dd2sex(lon, false, coordSystem.isEquatorial());
const convertLatToString = (lat, coordSystem) => dd2sex(lat, true, coordSystem.isEquatorial());
const convertStringToLon = (hms, coordSystem) => sex2dd(hms, false, coordSystem.isEquatorial());
const convertStringToLat = (dms, coordSystem) => sex2dd(dms, true, coordSystem.isEquatorial());

const CoordUtil= { convertLonToString, convertLatToString, convertStringToLon, convertStringToLat, validLon };

export default CoordUtil;