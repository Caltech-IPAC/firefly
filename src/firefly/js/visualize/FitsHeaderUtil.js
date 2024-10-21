/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isArray} from 'lodash';
import {isImage} from './WebPlot.js';


/**
 * @global
 * @public
 * @typedef {Array.<{}>} FitsHeader
 *
 * @prop <Array
 * @prop {number} height
 *
 */
export const HdrConst= {

    // Common FITS Headers
    CTYPE1   : 'CTYPE1',
    CTYPE2   : 'CTYPE2',
    CTYPE3   : 'CTYPE3',
    BITPIX   : 'BITPIX',
    BSCALE   : 'BSCALE',
    BUNIT   : 'BUNIT',
    BZERO    : 'BZERO',
    CRPIX1   : 'CRPIX1',
    CRPIX2   : 'CRPIX2',
    CRVAL1   : 'CRVAL1',
    CRVAL2   : 'CRVAL2',
    CDELT1   : 'CDELT1',
    CDELT2   : 'CDELT2',
    CROTA1   : 'CROTA1',
    CROTA2   : 'CROTA2',
    DATAMAX  : 'DATAMAX',
    DATAMIN  : 'DATAMIN',
    EXTNAME  : 'EXTNAME',
    EXTTYPE  : 'EXTTYPE',
    NAXIS1   : 'NAXIS1',
    NAXIS2   : 'NAXIS2',
    NAXIS3   : 'NAXIS3',
    NAXIS4   : 'NAXIS4',

    // FITS Headers that are added by firefly
    SPOT_OFF : 'SPOT_OFF', // Extension Offset (added by Firefly)
    SPOT_EXT : 'SPOT_EXT', // Extension Number (added by Firefly)
    SPOT_HS  : 'SPOT_HS',  // Header block size on disk (added by Firefly)
    SPOT_BP  : 'SPOT_BP',  // Original Bitpix value (added by Firefly)
    SPOT_PL  : 'SPOT_PL',  // Plane of FITS cube (added by Firefly)
};




export function makeHeaderParse(header, altWcs='') {
    return {
        header,
        getIntValue: (key, def= 0) => Math.trunc(getNumberHeader(header,key,def)),

        getIntOneOfValue: (keyAry, def=0) => {
            const foundKey= keyAry.find( (k) => !isNaN(getNumberHeader(header,k,NaN)) );
            return foundKey ? getNumberHeader(header, foundKey,def) : def;
        },
        getValue: (key, def= '') => getHeader(header, key, def),
        getDoubleValue: (key, def) => getNumberHeader(header,key,def),
        getDoubleOneOfValue: (keyAry, def=0) => {
            const foundKey= keyAry.find( (k) => !isNaN(getNumberHeader(header,k,NaN)) );
            return foundKey ? getNumberHeader(header, foundKey,def) : def;
        },
        isDefinedHeaderList(list) {
            const key= list.find( (i) =>  header[i+altWcs]);
            return Boolean(key);
        },

        getDoubleAry(keyRoot,altWcs, startIdx,endIdx,def=undefined) {
            if (startIdx>endIdx) return def;
            const retAry= [];
            let i= 0;
            for (let headerIdx = startIdx; headerIdx <= endIdx; headerIdx++) {
                retAry[i]= getNumberHeader(header, `${keyRoot}${headerIdx}${altWcs}`,def);
                i++;
            }
            return retAry;
        },
        hasKeyStartWith(startKeys){
          let key;
          for (key in header) {
            if (key.startsWith(startKeys)) {
              return true;
            }
          }
          return false;
        }
    };
}

export function makeDoubleHeaderParse(header,zeroHeader,altWcs) {
    const hp= makeHeaderParse(header, altWcs);
    const zhp= zeroHeader && makeHeaderParse(zeroHeader, altWcs);
    return {
        header,
        zeroHeader,
        getIntValue(key, def = 0) {
            const v1 = hp.getIntValue(key, NaN);
            if (!isNaN(v1)) return v1;
            return zhp ? zhp.getIntValue(key, def) : def;
        },
        getIntOneOfValue: (keyAry, def=0) => {
            const v1= hp.getIntOneOfValue(keyAry,NaN);
            if (!isNaN(v1)) return v1;
            return zhp ? zhp.getIntOneOfValue(keyAry, def) : def;
        },
        getValue: (key, def = '') => {
            const v1 = hp.getValue(key, 'TEST--EMPTY');
            if (v1!=='TEST--EMPTY') return v1;
            return zhp ? zhp.getValue(key, def) : def;
        },
        getDoubleValue(key, def) {
            const v1 = hp.getDoubleValue(key, NaN);
            if (!isNaN(v1)) return v1;
            return zhp ? zhp.getDoubleValue(key, def) : def;
        },
        getDoubleOneOfValue(keyAry, def) {
            const v1= hp.getDoubleOneOfValue(keyAry,NaN);
            if (!isNaN(v1)) return v1;
            return zhp ? zhp.getDoubleOneOfValue(keyAry, def) : def;
        },
        getDoubleAry(keyRoot,altWcs, startIdx,endIdx,def=undefined) {
            if (startIdx>endIdx) return def;
            const retAry= hp.getDoubleAry(keyRoot,altWcs,startIdx,endIdx, def);
            const validValueArr = retAry.filter( (v)=> v!==def );
            if (validValueArr.length>0) return retAry;
            return zhp ? zhp.getDoubleAry(keyRoot,altWcs, startIdx,endIdx,def) : def;
        },
        hasKeyStartWith(startKeys){
           if (hp.hasKeyStartWith(startKeys)){
               return true;
           }
           else {
             return zhp ? zhp.hasKeyStartWith(startKeys):false;
           }
        },
        isDefinedHeaderList: (list) => hp.isDefinedHeaderList(list) || (zhp && zhp.isDefinedHeaderList(list))
    };
}


//=============================================================
//=============================================================
//---------- Header functions
//=============================================================
//=============================================================




/**
 *
 * @param {WebPlot|Object} plotOrHeader image WebPlot or Header obj
 * @param headerKey
 * @return {{value:string,comment:string,idx:number}|Array.<{value:string,comment:string,idx:number}>}
 */
function getHeaderObj(plotOrHeader,headerKey) {
    if (!plotOrHeader || !headerKey) return {};
    if (plotOrHeader.plotImageId) {
        if (!isImage(plotOrHeader) ) return {};
        return plotOrHeader.header?.[headerKey] ?? {};
    }
    else {
        return plotOrHeader[headerKey] || {};
    }
}

/**
 * return a header description given a header key
 * @param {WebPlot|Object} plotOrHeader image WebPlot or Header obj
 * @param {string} headerKey key
 * @return {string} the description of the header if it exist otherwise an empty string
 */
export function getHeaderDesc(plotOrHeader,headerKey) {
    const v= getHeaderObj(plotOrHeader, headerKey);
    return (isArray(v)) ? v[v.length-1].comment ?? '' : v.comment ?? '';
}

/**
 * return a header value given a header key
 * @param {WebPlot|Object} plotOrHeader image WebPlot or Header obj
 * @param {string} headerKey key
 * @param {string} [defVal] the default value
 * @return {string} the value of the header if it exist, otherwise the default value
 */
export function getHeader(plotOrHeader,headerKey, defVal= undefined) {
    const v= getHeaderObj(plotOrHeader, headerKey);
    const result= (isArray(v)) ? v[v.length-1].value : v.value;
    return result || defVal;
}

/**
 * Check if a header has multiple values. this is uncommon except for entries like history
 * @param {WebPlot|Object} plotOrHeader image WebPlot or Header obj
 * @param {string} headerKey key
 * @return {Boolean} true if multi value
 */
const isMultiValueHeader= (plotOrHeader,headerKey) => isArray(getHeaderObj(plotOrHeader, headerKey));


/**
 *
 * @param {WebPlot|Object} plotOrHeader image WebPlot or Header obj
 * @param {string} headerKey key
 * @return {Array.<{value:string,comment:string,idx:number}>}
 */
export function getAllValuesOfHeader(plotOrHeader, headerKey) {
    const v= getHeaderObj(plotOrHeader, headerKey);
    return isArray(v) ? v : [v];
}

/**
 * get the name if the extension if it exist
 * @param {WebPlot|Object|undefined} plotOrHeader image WebPlot or Header obj
 * @return {string}
 */
export const getExtName= (plotOrHeader) => getHeader(plotOrHeader,HdrConst.EXTNAME,'');
export const getExtType= (plotOrHeader) => getHeader(plotOrHeader,HdrConst.EXTTYPE,'');
export const getBixPix= (plotOrHeader) => getNumberHeader(plotOrHeader,HdrConst.BITPIX,-32);
export const getBScale= (plotOrHeader) => getNumberHeader(plotOrHeader,HdrConst.BSCALE,1);
export const getBZero= (plotOrHeader) => getNumberHeader(plotOrHeader,HdrConst.BZERO,0);

export function hasFloatingData(plotOrHeader) {
    const bitpix= getBixPix(plotOrHeader);
    return bitpix===-64 || bitpix===-32;
}

/**
 * return a header number value given a header key
 * @param {WebPlot|Object} plotOrHeader image WebPlot or Header obj
 * @param {string} headerKey key
 * @param {number} [defVal] the default value
 * @return {number} the number value of the header if it exist and can be converted to a number, otherwise the default value
 */
export function getNumberHeader(plotOrHeader, headerKey, defVal= NaN) {
    const v= getHeader(plotOrHeader,headerKey,'');
    if (v==='') return defVal;
    const n= Number(v);
    return isNaN(n) ? defVal : n;
}

/**
 * convert the tables types to 'boolean', 'int', 'long', 'float', 'double', 'string'
 * @param fdt
 * @return {string}
 */
export function convertFitsTableDataType(fdt) {
    const aStr= fdt.match(/^[1-9]*/)?.[0];
    const typeChar= fdt[aStr?.length??0];
    switch (typeChar) {
        case 'L': return 'boolean';
        case 'X': return 'int';
        case 'B': return 'int';
        case 'I': return 'int';
        case 'J': return 'int';
        case 'K': return 'long';
        case 'A': return 'string';
        case 'E': return 'float';
        case 'D': return 'double';
        case 'C': return 'string';
        case 'M':
        case 'P': return 'string';
        case 'Q': return 'string';
        default: return;
    }
}


export function isFitsTableDataTypeArray(fdt) {
    const aStr= fdt.match(/^[1-9]*/)?.[0];
    return (aStr && aStr!=='1');
}

export function isFitsTableDataTypeSimpleNumeric(fdt) {
    if (isFitsTableDataTypeArray(fdt)) return false;
    return isFitsTableDataTypeNumeric(fdt);
}
export function isFitsTableDataTypeNumeric(fdt) {
    const v= convertFitsTableDataType(fdt);
    return v==='int' || v==='long' || v==='double' || v==='float';
}




