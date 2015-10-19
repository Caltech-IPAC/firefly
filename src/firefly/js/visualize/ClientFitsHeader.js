/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


const SPLIT_TOKEN= '--ClientFitHead--';

import parseInt from '../util/StringUtils.js';
import join from 'underscore.string/join';
import words from 'underscore.string/words';

export const C = {
    PLANE_NUMBER: 'planeNumber',
    BITPIX: 'bitpix',
    NAXIS: 'naxis',
    NAXIS1: 'naxis1',
    NAXIS2: 'naxis2',
    NAXIS3: 'naxis3',
    CDELT2: 'cdelt2',
    BSCALE: 'bscale',
    BZERO: 'bzero',
    BLANK_VALUE: 'blankValue',
    DATA_OFFSET: 'dataOffset'
};


class ClientFitsHeader {
    construtor(headers) {
        this.headers= headers;
    }


    getPlaneNumber() { return parseInt(this.headers[C.PLANE_NUMBER],0); }
    getBixpix()      { return parseInt(this.headers[C.BITPIX],0); }
    getNaxis()       { return parseInt(this.headers[C.NAXIS],0); }
    getNaxis1()      { return parseInt(this.headers[C.NAXIS1],0); }
    getNaxis2()      { return parseInt(this.headers[C.NAXIS2],0); }
    getNaxis3()      { return parseInt(this.headers[C.NAXIS3],0); }

    getCDelt2() { return parseFloat(this.headers[C.CDELT2],0); }
    getBScale() { return parseFloat(this.headers[C.BSCALE],0); }
    getBZero() { return parseFloat(this.headers[C.BZERO],0); }
    getBlankValue() { return parseFloat(this.headers[C.BLANK_VALUE],0); }
    getDataOffset() { return parseFloat(this.headers[C.DATA_OFFSET],0); }


    setHeader(key, value) { this.header[key]= value; }

    //toString() {
    //    return join(SPLIT_TOKEN, Object.keys(this.headers).map( (key) => key+'='+this.headers[key]));
    //}
    //
    //static parse(s) {
    //    if (!s) return null;
    //    var headers= words(s,SPLIT_TOKEN).reduce( (obj,str) =>  {
    //                           var vStr= words(str,'=');
    //                           if (vStr.length===2) obj[vStr[0]]=vStr[1];
    //                           return obj;
    //                       },{});
    //    return Object.keys(headers).length ? new ClientFitsHeader(headers) : null;
    //}
}

export default ClientFitsHeader;

export const makeClientFitsHeader= function(headers) {
    return new ClientFitsHeader(headers);
};

