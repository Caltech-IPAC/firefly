/**
 * This File is Deprecated.  It is only used by 3 other modules and that will be cleaned up eventually
 */


/**
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 * Created by roby on 12/2/14.
 */


export function matches(s, regExp, ignoreCase) {
    if (s === null) {
        return false;
    }
    const re = ignoreCase ? new RegExp(regExp, 'i') : new RegExp(regExp);
    const result = re.exec(s);
    let found = false;
    if (result !== null && result.length > 0) {
        for (let i = 0; (i < result.length); i++) {
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
 * @param {String} s
 */
export function crunch(s) {
    if (s) {
        s = s.replace(/[ \t\n\r\f]/g, ' ');
        s = s.trim();
        s = s.replace(/\s{2,}/g, ' ');
    }
    return s;
}

export function polishString(str) {
    if (str) return convertExtendedAscii(str);
    return str;
}

export function convertExtendedAscii(sbOriginal) {
    if (sbOriginal === null) {
        return null;
    }

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

function replaceAt(str, index, replacement) {
    return str.substr(0, index) + replacement+ str.substr(index + replacement.length);
}