/*eslint no-empty:0*/


import numeral from 'numeral';

/**
 *  This class provides conversion of user's
 *  coordinate string to a double
 *  Translated from Rick Ebert's coord.c
 *
 *  April 10, 2001
 *  Copied from the original Coord.java, modified to provide the convenient
 *  static methods to do validation, formatting, and conversion from decimal
 *  degree to HMS/DMS string and vice versa.  -xiuqin
 *  @author Booth Hartley, G.Turek, Xiuqin Wu
 */
const DEFAULT_PRECISION = 8;
const MAX_PRECISION = 8;
const FORM_DECIMAL_DEGREE = 0;  // 12.34d
const FORM_DMS = 1;             // 12d34m23.4s
const FORM_HMS = 2;             // 12h34m23.4s

const LAT_OUT_RANGE = 'Latitude is out of range [-90.0, +90.0]';
//var LON_OUT_RANGE = 'Longitude is out of range [0.0, 360.0)';
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
var sex2dd = function (coordstr, islat, isequ) {

    var sign;
    var p, r;
    var pointseen, i, done, cntdigits, numseen, parts;
    var degrees;
    var sep = [];
    var part = [];
    var modangle, angle, base;
    var point = [];
    var strpart = [];
    var isdec;

    //System.out.println("Coord:  now in c_coord");
    //System.out.println("Coord: coord = " + coordstr);
    //System.out.println("Coord: islat = " + islat);
    //System.out.println("Coord: isequ = " + isequ);

    r = null;
    parts = 0;
    degrees = -1;

    p = coordstr.trim();
    if (p.length === 0) {
        throw 'length 0';
    }

    sign = 1;  // assume positive
    if (p.charAt(0) === '-') {
        sign = -1;
        p = p.substring(1);
    }
    else if (p.charAt(0) === '+') {
        sign = 1;
        p = p.substring(1);
    }
    p = p.trim();  // allow space between sign and nbr
    if (p.length === 0) {
        throw INVALID_STRING;
    }

    for (i = 0; i < 3 && (p.length > 0); i++) {
        pointseen = 0;
        done = 0;
        cntdigits = 0;
        numseen = 0;
        r = null;

        while ((p.length > 0) && done === 0) {
            if (p.charAt(0) === '.') {
                if (pointseen === 1) {
                    throw INVALID_STRING;
                }
                else {
                    pointseen = 1;
                    if (r ===null) {
                        r = p;
                    }  //Mark this place - number starts here
                }
            }
            else if (!isNaN(p.charAt(0))) {
                if (pointseen===0) {
                    cntdigits++;
                }
                if (r===null) {
                    r = p;
                }  //Mark this place - number starts here
                numseen++;
            }
            else {
                done = 1;
            }

            if (done===0) {
                p = p.substring(1);
            }
        }  // end of inner-while

        if (numseen===0) {
            throw INVALID_STRING;
        }

        //convert it and save it
        //System.out.println("RBH i = "+i + " numseen = "+numseen);
        strpart[i] = r.substring(0, numseen + pointseen);
        //part[i] = (Double.valueOf(strpart[i])).doubleValue();
        part[i] = parseFloat(strpart[i]);
        //System.out.println("RBH i = "+i+" strpart[i] = "+strpart[i] + " part[i] = " + part[i] );

        r = p;  // now deal with the separator
        p = p.trim();
        sep[i] = cSeparator(p);
        if (sep[i] === '\0') {
            // this character is NOT a separator
            if (r.charAt(0) === ' ') {
                sep[i] = ' ';
                p = r;
            }
            else {
                throw INVALID_STRING;
            }
        }
        point[i] = pointseen;
        parts++;
        if (p.length > 0) {
            p = p.substring(1);
        }
        p = p.trim();
    } // outer-for loop ends here

    // when we get here we've cracked as many as 3 parts
    // if there's anything left, the whole string is junk
    p = p.trim();
    if (p.length > 0) {
        throw INVALID_STRING;
    }

    // Another way to be junk is to have a decimal point in any but
    // the 'last' part

    if ((parts ===3 && (point[0] ===1 || point[1] ===1 )) || (parts ===2 && point[0] ===1)) {
        throw 'Invalid input';
    }

    // and another way to be junk is to use inconsistent or out
    // of order separators
    // Here we also crack whether the input was ""decimal"" or not

    if (parts=== 3) {
        isdec = false;
        if (sep[0] ==='h' && sep[1] ==='m' && sep[2] ==='s') {
            degrees = 0;
        }
        else if (sep[0] ==='h' && sep[1] ==='m' && sep[2] ===' ') {
            degrees = 0;
        }
        else if (sep[0] ==='d' && sep[1] ==='m' && sep[2] ==='s') {
            degrees = 1;
        }
        else if (sep[0] ==='d' && sep[1] ==='m' && sep[2] ===' ') {
            degrees = 1;
        }
        else if (sep[0] ===':' && sep[1] ===':' && sep[2] ===' ') {
        }
        else if (sep[0] ===' ' && sep[1] ===' ' && sep[2] ===' ') {
        }
        else if (sep[0] ==='d' && sep[1] ==='\'' && sep[2] ==='\"') {
            degrees = 1;
        }
        else if (sep[0] ==='d' && sep[1] ==='\'' && sep[2] ===' ') {
            degrees = 1;
        }
        else {
            throw INVALID_SEPARATOR;
        }
    }
    else if (parts ===2) {
        isdec = false;
        if (sep[0] ==='h' && sep[1] ==='m') {
            degrees = 0;
        }
        else if (sep[0] ==='h' && sep[1] ===' ') {
            degrees = 0;
        }
        else if (sep[0] ==='d' && sep[1] ==='m') {
            degrees = 1;
        }
        else if (sep[0] ==='d' && sep[1] ===' ') {
            degrees = 1;
        }
        else if (sep[0] ==='m' && sep[1] ==='s') {
        }
        else if (sep[0] ==='m' && sep[1] ===' ') {
        }
        else if (sep[0] ===':' && sep[1] ===' ') {
        }
        else if (sep[0] ===' ' && sep[1] ===' ') {
        }
        else if (sep[0] ==='d' && sep[1] ==='\'') {
            degrees = 1;
        }
        else if (sep[0] ==='\'' && sep[1] ==='\"') {
            degrees = 1;
        }
        else if (sep[0] ==='\'' && sep[1] ===' ') {
            degrees = 1;
        }
        else {
            throw INVALID_SEPARATOR;
        }
    }
    else if (parts ===1) {
        if (sep[0] ==='h') {
            degrees = 0;
            isdec = false;

        } else if (sep[0] ==='d') {
            degrees = 1;
            isdec = true;

        } else if (sep[0] ===' ') {

            isdec= (isequ && !islat);

        } else if (sep[0] ==='m') {
            isdec = false;

        } else if (sep[0] ==='s') {
            isdec = false;

        } else if (sep[0] ==='\'') {
            degrees = 1;
            isdec = false;

        } else if (sep[0] ==='\"') {
            degrees = 1;
            isdec = false;

        } else {
            throw INVALID_SEPARATOR;
        }

    } else {
        throw INVALID_STRING;
    }  // parts == 0

    if (degrees=== -1) {
        if (islat) {
            degrees = 1;
        } // input is a latitude
        else if (isequ) {
            degrees = 0;
        }  // input is equatorial longitude (RA)
        else {
            degrees = 1;
        }  // all else is DMS
    }

    // No HMS for latitudes
    if (degrees=== 0)  // not in degree format
    {
        if (islat){
            throw HMS_FOR_LAT;
        }
    }

    // No sexigesimal input for non-equatorial systems
    if (!isequ && !isdec) {
        throw SEX_FOR_NONE_EQU;
    }

    // now modularize the input and convert to a double

    //System.out.println("part[0] = "+part[0]+"  part[1] = "+part[1]+ " part[2] ="+part[2]);
    if (parts > 1) {
        for (i = parts - 1; i >= 1; i--) {
            if (part[i] >= 60.0) {
                throw MIN_SEC_TOO_BIG;
                //part[i-1] += Math.floor(part[i] / 60.0);
                //part[i] = part[i] % 60.0;
            }
        }
    }
    //System.out.println("part[0] = "+part[0]+"  part[1] = "+part[1]+ "  part[2] ="+part[2]);

    if (degrees !==0) {
        modangle = 360.0;
    }    // in degree
    else {
        modangle = 24.0;
    }   // Hours RA

    for (i = 0, angle = 0.0, base = 1.0; i < parts; i++, base *= 60.0){
        angle += part[i] / base;
    }


    // the input might be  xxm[xx[.xx]s] or just xx[.xx]s or xx[.xx]m
    // so we apply appropriate weighting - whether it's min arc or time
    // we worry about further on.

    if (sep[0] ==='m' || sep[0] ==='\'') {
        angle /= 60.0;
    }
    else if (sep[0] ==='s' || sep[0] ==='\"') {
        angle /= 3600.0;
    }

    angle *= sign;

    if (islat) {
        if (angle > 90.0 || angle < -90.0) {
            throw LAT_OUT_RANGE;
        }
    }
    else // input is longitude
    {
        if (angle >= modangle) {
            if (degrees !==0) {
                throw LON_TOO_BIG;
            }
            else {
                throw RA_TOO_BIG;
            }
            //angle = angle % modangle;
        }
        else if (angle < 0.0) {
            throw LON_NEGATIVE;
            //angle = (angle % modangle) + modangle;
        }
    }

    // make angle degrees if it isn't already and needs to be,
    // from hours to degrees.
    // we've already disallowed 'hours' as a valid form for latitudes

    if (degrees ===0) {
        angle *= 15.0;
    }

    // canonical string
    /*
     if (isequ)
     {
     if (islat) form = 1;
     else form = 2;
     }
     else form = 0;


     dangle = angle;
     dangle_set = true;

     buf = c_ddsex(form);

     */
    //System.out.println("RBH coordstr [" + coordstr + "]");

    //System.out.println("Coord.java: buf = " + buf);
    //System.out.println("Coord.java: isdec = " + isdec);
    //System.out.println("Coord: dangle = " + dangle);

    return angle;
};


/**
 * Converts decimal angle to string format
 * @param dangle decimal angle
 * @param islat true if the coordstr is latitude
 * @param isequ true if the coordstr is in equatorial system
 * @param precision for DMS, precision = 4+ number of
 *                            decimal digits you want in seconds
 *                       for HMS, precision = 3+ number of
 *                            decimal digits you want in seconds

 *  @return String representation of angle
 */
var dd2sex = function (dangle, islat, isequ, precision) {
    var cPrecision;
    var form;
    var tmp, dfs;
    var ofs;
    var hd, m, s;
    var rhd, rm, rs, rfs;
    var drfs;
    var d;
    var circ;
    var chd;
    var cm = 'm';
    var cs = 's';
    var isign = 1;
    var signstr;
    //NumberFormat df;
    var fmtstr;
    var i;
    var buf;

    if (precision) {
        precision = 5;
    }
    if (precision <= 0) {
        cPrecision = 0;
    }
    else if (precision <= MAX_PRECISION) {
        cPrecision = precision;
    }
    else {
        cPrecision = DEFAULT_PRECISION;
    }

    if ((dangle < 0.0) && islat) {
        isign = -1;
    }
    if ((dangle < 0.0) && !islat) {
        dangle = (dangle % 360.0) + 360.0;
    }
    if ((dangle >= 360.0) && !islat) {
        dangle = dangle % 360.0;
    }

    // sign for Latitude/Dec  '+' or '-'
    if (isign ===1) {
        signstr=  islat ? '+' : '';
    }
    else {
        signstr = '-';
    }

    if (isequ) {
        form= islat ? FORM_DMS : FORM_HMS;
    }
    else {
        form = FORM_DECIMAL_DEGREE;
    }

    if (form ===FORM_HMS) {
        dangle /= 15.0;
    }  // convert degree to hours
    if ((form ===FORM_DMS) || (form ===FORM_HMS)) {
        tmp = Math.abs(dangle);
        hd = Math.floor(tmp);
        tmp -= hd;
        tmp *= 60.0;
        m = Math.floor(tmp);
        tmp -= m;
        tmp *= 60.0;
        s = Math.floor(tmp);
        tmp -= s;
        dfs = tmp;

        if (form ===FORM_DMS) {
            // sexigesimal degrees
            circ = 360;
            chd = 'd';
            switch (cPrecision) {
                case 0:
                    d = 1;
                    break;

                case 1:
                case 2:
                    d = 2;
                    break;

                case 3:
                case 4:
                    d = 3;
                    break;

                default:
                    d = cPrecision - 1;    // 3 + precision + 4
                    break;
            }
            drfs = cPrecision - 4;
        }
        else {
            // sexigesimal hours min sec
            circ = 24;
            chd = 'h';
            switch (cPrecision) {
                case 0:
                case 1:
                    d = 2;
                    break;

                case 2:
                case 3:
                    d = 3;
                    break;

                default:
                    d = cPrecision;
                    break;    // 3+p-3
            }
            drfs = cPrecision - 3;   // the digits after seconds
        }
        switch (d) {
            case 1:   // only degree
                rhd = hd + ((m >= 30) ? 1 : 0);
                if (rhd >= circ) {
                    rhd -= circ;
                }
                buf = signstr;
                //df = NumberFormat.getFormat('00');
                //buf += df.format(rhd);
                buf += numeral(rhd).format('00');
                buf += 'd';
                break;

            case 2:   // degree + minutes
                rm = m + ((s >= 30) ? 1 : 0);
                rhd = hd;
                if (rm >= 60) {
                    rm -= 60;
                    rhd++;
                }
                if (rhd >= circ) {
                    rhd -= circ;
                }
                buf = signstr;
                buf += rhd;
                buf += chd;
                //df = NumberFormat.getFormat('00');
                //buf += df.format(rm);
                buf += numeral(rm).format('00');
                buf += cm;
                break;

            case 3:   // degree + minutes + seconds
                rs = s + ((dfs >= 0.5) ? 1 : 0);
                rm = m;
                rhd = hd;
                if (rs >= 60) {
                    rs -= 60;
                    rm++;
                }
                if (rm >= 60) {
                    rm -= 60;
                    rhd++;
                }
                if (rhd >= circ) {
                    rhd -= circ;
                }
                buf = signstr;
                buf += rhd;
                buf += chd;
                //df = NumberFormat.getFormat('00');
                //buf += df.format(rm);
                buf += numeral(rm).format('00');
                buf += cm;
                //buf += df.format(rs);
                buf += numeral(rs).format('00');
                buf += cs;
                break;

            default:
                rs = s;
                rm = m;
                rhd = hd;
                tmp = Math.pow(10, drfs);
                ofs = Math.floor(tmp);
                rfs = Math.round(dfs * ofs);
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
                if (rhd >= circ) {
                    rhd -= circ;
                }
                buf = signstr;
                buf += rhd;
                buf += chd;
                //df = NumberFormat.getFormat('00');
                //buf += df.format(rm);
                buf += numeral(rm).format('00');
                buf += cm;
                //buf += df.format(rs);
                buf += numeral(rs).format('00');
                buf += '.';
                for (i = 0, fmtstr = ''; i < drfs; i++) {
                    fmtstr += '0';
                }
                //df = NumberFormat.getFormat(fmtstr);
                //buf += df.format(rfs);
                buf += numeral(rfs).format(fmtstr);
                buf += cs;
                break;
        }
    }
    else // form == FORM_DECIMAL_DEGREE or otherwise unrecognized
    {
        buf= (islat && dangle >= 0) ? '+' : '';
        for (i = 0, fmtstr = '0.'; i < cPrecision; i++) {
            fmtstr += '0';
        }
        //df = NumberFormat.getFormat(fmtstr);
//      NumberFormat nf = NumberFormat.getInstance(Locale.US);
//      df= (DecimalFormat)nf;
//       df.setMaximumFractionDigits(cPrecision);
//      buf += df.format(dangle);
        buf += numeral(dangle).format(fmtstr);
        buf += 'd';
    }

    return buf;
};

function cSeparator (str) {
    if (str.length===0) {
        return ' ';
    }
    switch (str.charAt(0)) {
        case 'h':
        case 'H':
            return 'h';
        case 'd':
        case 'D':
            return 'd';
        case 'm':
        case 'M':
            return 'm';
        case 's':
        case 'S':
            return 's';
        case ':':
            return ':';
        case '\'':
        case '\"':
            return str.charAt(0);
        case ' ':
        case '\t':
            return '\0';
        default:
            return '\0';
    }
}


var validLon = function (hms, coordSystem) {
    var returnVal= false;
    try {
        returnVal.convertStringToLon(hms, coordSystem);
        returnVal = true;
    } catch (e) {
        returnVal = false;
    }
    return returnVal;
};


var convertLonToString = function (lon, coordSystem) {
  return dd2sex(lon, false, coordSystem.isEquatorial(), 5);
};


var convertLatToString = function (lat, coordSystem) {
  return dd2sex(lat, true, coordSystem.isEquatorial(), 5);
};

var convertStringToLon = function (hms, coordSystem) {
  var eq = coordSystem.isEquatorial();
  return sex2dd(hms, false, eq);
};

var convertStringToLat = function (dms, coordSystem) {
  var eq = coordSystem.isEquatorial();
  return sex2dd(dms, true, eq);
};

var CoordUtil= {
  convertLonToString, convertLatToString, convertStringToLon, convertStringToLat, validLon
};

export default CoordUtil;

