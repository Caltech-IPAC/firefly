/*jshint browserify:true*/


/**
 * Created by roby on 12/2/14.
 */


var StringUtils= (function(retUtil) {
    "use strict";

    var hasModule = (typeof module !== 'undefined' && module.exports);


    var matchesIgCase= function(s, regExp) {
        return matches(s, regExp, true);
    };

    var matches= function(s, regExp, ignoreCase) {
        if (s === null) {
            return false;
        }
        var re = ignoreCase ? new RegExp(regExp, "i") : new RegExp(regExp);
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
    };



    /**
     * removes extra spaces from a string.
     * <ul><li><code>" bbb    ccc  ddd"</code></li></ul>
     * should become:
     * <ul><li><code>aaa "bbb ccc ddd" eee</code></li></ul>
     */
    var crunch= function (s) {
        if (s !== null) {
            s = s.replace(/[ \t\n\r\f]/g, " ");
            s = s.trim();
            s = s.replace(/\s{2,}/g, ' ');
        }
        return s;
    };

    var polishString= function(str) {
        if (str!==null && str.length>0) {
            str = convertExtendedAscii(str);
        }
        return str;
    };



    var convertExtendedAscii= function(sbOriginal)  {
        if (null===sbOriginal)  {
            return null;
        }

        var retval= sbOriginal;
        var origCharAsInt;
        for (var isb = 0; isb < retval.length; isb++)  {

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
    };

    var isEmpty= function(s) {
        return s === null || s.trim().length===0;
    };

    var startsWith= function(s,startStr) {
        return s.substring(0, startStr.length) === startStr;
    };

    var endsWith= function(s,endStr) {
        return (s.indexOf(endStr, s.length - endStr.length) !== -1);
    };

    var parseHelper= function(s, max, splitToken) {

        var sAry= null;
        if (s) {
            sAry= s.split(splitToken,max+1);
            if (sAry.length>max)  sAry= null;
        }
        if (!sAry) throw "wrong number of tokens in String";
        return sAry;

    }

    var parseStringList= function(s,token,max=500) {
        if (s.startsWith('[') && s.endsWith(']')) {
            var ss= s.substring(1,s.length-1);
            var sAry= ss.split(token,max);
            var retval=[];
            sAry.forEach(function(item) {
                if (item) {retval.push(item);}
            });
        }
        return retval;
    };

    var parseStringMap= function(s,token) {
        var map= new Map();
        if (s.startsWith('[') && s.endsWith(']')) {
            s= s.substring(1,s.length-1);
            var sAry = s.split(token,500);
            for(var i= 0; (i<sAry.length-1); i+=2) {
                if (sAry[i]  && sAry[i+1]) {
                    map.set(sAry[i],sAry[i+1]);
                }
            }
        }
        return map;
    };



    retUtil.matchesIgCase= matchesIgCase;
    retUtil.matches= matches;
    retUtil.crunch= crunch;
    retUtil.polishString= polishString;
    retUtil.convertExtendedAscii= convertExtendedAscii;
    retUtil.isEmpty= isEmpty;
    retUtil.startsWith= startsWith;
    retUtil.endsWith= endsWith;
    retUtil.parseHelper= parseHelper;
    retUtil.parseStringList= parseStringList;
    retUtil.parseStringList= parseStringMap;

    if (hasModule) {
        exports.matchesIgCase= matchesIgCase;
        exports.matches= matches;
        exports.crunch= crunch;
        exports.polishString= polishString;
        exports.convertExtendedAscii= convertExtendedAscii;
        exports.isEmpty= isEmpty;
        exports.startsWith= startsWith;
        exports.endsWith= endsWith;
        exports.parseHelper= parseHelper;
        exports.parseStringList= parseStringList;
        exports.parseStringMap= parseStringMap;
    }

    return retUtil;
}(StringUtils || {}));
