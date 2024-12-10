/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isObject, isString} from 'lodash';
import {getCellValue, getColumnByRef, getTblById} from '../tables/TableUtil.js';
import {ColNameIdx, OBSTAPCOLUMNS, UCDSyntax, UtypeColIdx} from './VoConst.js';

/**
 * @see {@link http://www.ivoa.net/documents/VOTable/20130920/REC-VOTable-1.3-20130920.html#ToC54}
 * A.1 link substitution
 * @param tableModel    table model with data and columns info
 * @param href          the href value of the LINK
 * @param rowIdx        row index to be resolved
 * @param fval          the field's value, or cell data.  Append field's value to href, if no substitution is needed.
 * @param encodeValues  encodeURIComponent the values.  default to false.
 * @returns {string}    the resolved href after substitution
 */
export function applyLinkSub(tableModel, href='', rowIdx, fval='', encodeValues=false) {
    const rhref = applyTokenSub(tableModel, href, rowIdx, '', encodeValues);
    if (rhref === href) {
        fval = encodeValues ? encodeURIComponent(fval) : fval;
        return fval ? href + fval : '';       // no substitution given, append defval to the url.  set A.1
    }
    return rhref;
}

/**
 * applies token substitution if any.  If the resulting value is nullish, return the def val.
 * @see {@link http://www.ivoa.net/documents/VOTable/20130920/REC-VOTable-1.3-20130920.html#ToC54}
 * A.1 link substitution
 * @param tableModel    table model with data and columns info
 * @param val           the value to resolve
 * @param rowIdx        row index to be resolved
 * @param def           return value if val is nullish
 * @param encodeValues  encodeURIComponent the values.  default to false.
 * @returns {string}    the resolved href after substitution
 */
export function applyTokenSub(tableModel, val='', rowIdx, def='', encodeValues=false) {

    const vars = val?.match?.(/\${[\w -.]+}/g);
    let rval = val;
    if (vars) {
        vars.forEach((v) => {
            const [,cname] = v.match(/\${([\w -.]+)}/) || [];
            const col = getColumnByRef(tableModel, cname);
            let cval = col ? getCellValue(tableModel, rowIdx, col.name) : '';  // if the variable cannot be resolved, return empty string
            cval = encodeValues ? encodeURIComponent(cval) : cval;
            rval = (!cval && v === rval) ? cval : rval.replace(v, cval);
        });
    }
    return rval ? rval : rval === 0 ? 0 : def;
}


export function getObsTabColEntry(title) {
    const UcdColIdx = 1;
    const e = OBSTAPCOLUMNS.find((entry) => entry[ColNameIdx] === title);
    return e && {name: e[ColNameIdx], ucd: e[UcdColIdx], utype: e[UtypeColIdx]};
}

/**
 * check if ucd value contains the searched ucd word at the right position
 * @param ucdValue
 * @param ucdWord
 * @param syntaxCode 'P': only first word, 'S': only secondary, 'Q' either first or secondary
 */
export function isUCDWith(ucdValue, ucdWord, syntaxCode = UCDSyntax.any) {
    const atoms = ucdValue.split(';');
    const idx = atoms.findIndex((atom) => {
        return atom.toLowerCase() === ucdWord.toLowerCase();
    });

    return (syntaxCode === UCDSyntax.primary && idx === 0) ||
        (syntaxCode === UCDSyntax.secondary && idx >= 1) ||
        (syntaxCode === UCDSyntax.any && idx >= 0);
}

/**
 * Given a TableModel or a table id return a table model
 * @param {TableModel|String} tableOrId - a table model or a table id
 * @return {TableModel|undefined} return the table model if found or undefined
 */
export function getTableModel(tableOrId) {
    if (isString(tableOrId)) return getTblById(tableOrId);  // was passed a table Id
    if (isObject(tableOrId)) return tableOrId;
}

export const hsaServiceSelfDescription= (tableOrId) => Boolean(getServiceSelfDescription(tableOrId));

export function getServiceSelfDescription(tableOrId) {
    const table= getTableModel(tableOrId);
    return table.resources
        ?.filter( (r) => r.type==='meta' && (r.utype==='adhoc:this'||r.utype==='adhoc:service'))?.[0];
}




