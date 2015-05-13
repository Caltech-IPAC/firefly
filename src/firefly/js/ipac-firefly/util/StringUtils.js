/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Created by roby on 12/2/14.
 */

'use strict';


export function matches(s, regExp, ignoreCase) {
    if (s === null) {
        return false;
    }
    var re = ignoreCase ? new RegExp(regExp, 'i') : new RegExp(regExp);
    var result = re.exec(s);
    var found = false;
    if (result !== null && result.length > 0) {
        for (var i = 0; (i < result.length); i++) {
            if (s===result[i]) {
                found = true;
                break;
            }
        }
    }
    return found;
}

export function matchesIgCase(s, regExp) {
    return matches(s, regExp, true);
}

/**
 * removes extra spaces from a string.
 * <ul><li><code>" bbb    ccc  ddd"</code></li></ul>
 * should become:
 * <ul><li><code>aaa "bbb ccc ddd" eee</code></li></ul>
 */
export function crunch(s) {
    if (s !== null) {
        s = s.replace(/[ \t\n\r\f]/g, ' ');
        s = s.trim();
        s = s.replace(/\s{2,}/g, ' ');
    }
    return s;
}

export function polishString(str) {
    if (str!==null && str.length>0) {
        str = convertExtendedAscii(str);
    }
    return str;
}

export function convertExtendedAscii(sbOriginal) {
    if (sbOriginal === null) {
        return null;
    }

    var retval= sbOriginal;
    var origCharAsInt;
    for (var isb = 0; isb < retval.length; isb++) {

        origCharAsInt = retval.charCodeAt(isb);
        if (origCharAsInt<=255) {
            switch (origCharAsInt) {
                case 223:
                case 224:
                    retval[isb]= '"';
                    break;
                case 150:
                case 151:
                    retval[isb]= '-';
                    break;
                default:
                    if (origCharAsInt>127) {
                        retval[isb]= '?';
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
                    retval[isb]= '\'';
                    break;

                case '\u201C': // left double quote
                case '\u201D': // right double quote
                case '\u201E': // double low quotation mark
                    retval[isb]= '"';
                    break;

                case '\u02DC':
                    retval[isb]= '~';
                    break;  // Small Tilde

                case '\u2013': // En Dash
                case '\u2014': // EM Dash
                    retval[isb]= '-';
                    break;

                default:
                    if (origCharAsInt>127) {
                        retval[isb]= '?';
                    }
                    break;
            }
        }
    }
    return sbOriginal;
}

export function isEmpty(s) {
    return s === null || s.trim().length===0;
}

export function startsWith(s,startStr) {
    return s.substring(0, startStr.length) === startStr;
}

export function endsWith (s,endStr) {
    return (s.indexOf(endStr, s.length - endStr.length) !== -1);
}

export function parseHelper(s, max, splitToken) {

    var sAry= null;
    if (s) {
        sAry= s.split(splitToken,max+1);
        if (sAry.length>max) { sAry = null; }
    }
    if (!sAry) { throw 'wrong number of tokens in String'; }
    return sAry;

}

export function parseStringList(s,token,max=500) {
    var retval=[];
    if (s.startsWith('[') && s.endsWith(']')) {
        var ss= s.substring(1,s.length-1);
        var sAry= ss.split(token,max);
        sAry.forEach(function(item) {
            if (item) {retval.push(item); }
        });
    }
    return retval;
}

export function parseStringMap(s,token) {
    var map= new Map();
    if (s.startsWith('[') && s.endsWith(']')) {
        s= s.substring(1,s.length-1);
        var sAry = s.split(token,500);
        for(var i= 0; (i<sAry.length-1); i+=2) {
            if (sAry[i] && sAry[i+1]) {
                map.set(sAry[i],sAry[i+1]);
            }
        }
    }
    return map;
}

export function checkNull(s) {
    if (!s) { return null; }
    else if (s==='null') { return null; }
    else { return s; }
}

