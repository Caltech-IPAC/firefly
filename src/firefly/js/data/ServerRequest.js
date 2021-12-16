/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {has} from 'lodash';
import {parseWorldPt} from '../visualize/Point.js';
import {replaceAll, isDefined} from '../util/WebUtil.js';
import {toBoolean} from '../util/WebUtil';


const REQUEST_CLASS= 'RequestClass';
const SERVER_REQUEST_CLASS = 'ServerRequest';
const PARAM_SEP = '&';
const URL_SUB = 'URL_PARAM_SEP';
const KW_VAL_SEP = '=';
const ID_KEY = 'id';

export const ID_NOT_DEFINED = 'ID_NOT_DEFINED';

export class ServerRequest {
    constructor(id, copyFromReq) {
        this.params= {};
        if (copyFromReq) {
            Object.assign(this.params, copyFromReq.params ? copyFromReq.params : copyFromReq);
        }
        if (id) this.setRequestId(id);
        if (!this.params[ID_KEY]) this.params[ID_KEY]= ID_NOT_DEFINED;
        this.setRequestClass(SERVER_REQUEST_CLASS);
    }

    getRequestId() { return this.getParam(ID_KEY); }

    setRequestId(id) { this.params[ID_KEY]= id; }


    getRequestClass() {
        return this.containsParam(REQUEST_CLASS) ? this.getParam(REQUEST_CLASS) : SERVER_REQUEST_CLASS;
    }

    setRequestClass(reqType) { this.setParam(REQUEST_CLASS,reqType); }

    containsParam(paramKey) { return Boolean(this.params[paramKey]); }

    getParam(paramKey) { return this.params[paramKey]; }

    getParams() { return this.params; }


    /**
     * This method can take multiple types a parameter
     * If a single object literal is passed then it will look for a name & value field.
     * If two strings are passed the the first will me the name and the second will be the value.
     * if more then two string are passed then the first is the name and the others are join together as the value
     * {object|string}
     * {string}
     */
    setParam() {
        if (arguments.length===1 && typeof arguments[0] === 'object') {
            const v= arguments[0];
            if (v.name && v.value) {
                this.params[v.name]= v.value;
            }
            else if (Object.keys(v).length===1) {
                Object.assign(this.params,v);
            }
        }
        else if (arguments.length===2) {
            this.params[arguments[0]]= arguments[1];
        }
        else if (arguments.length>2) {
            const values= [];
            for(let i=2; i<arguments.length; i++) {
               values.push(arguments[i]);
            }
            this.params[arguments[0]]= values.join(',');
        }
    }


    setParams(params) { Object.assign(this.params, params); }

    setSafeParam(name,val) {
        this.params[name]= val ? replaceAll(val+'',PARAM_SEP,URL_SUB) : null;
    }

    /**
     *
     * @param name
     * @return {string}
     */
    getSafeParam(name) {
        const val= this.params[name];
        return val ? replaceAll(val,URL_SUB,PARAM_SEP) : null;
    }

    isValid() { return Boolean(this.params[ID_KEY]); }

    removeParam(name) { delete this.params[name]; }

//====================================================================


    copyFrom(req) {
        Object.assign(this.params,req.params?req.params:req);
    }

    /**
     * Parses the string argument into a ServerRequest object.
     * This method is reciprocal to toString().
     * @param str
     * @param req
     * @return {ServerRequest} the passed request
     */
    static parseAndAdd(str,req) {
        if (!str) return null;
        str.split(PARAM_SEP).forEach((p) => {
            if (!p) return;
            const outParam= p.split(/=(.+)?/,2);
            if (outParam.length===2) {
                const newParam= {name : outParam[0], value:outParam[1]};
                req.setParam(newParam);
            }
        });
        return req;
    }



    /**
     * Serialize this object into its string representation.
     * This class uses the url convention as its format.
     * Parameters are separated by '&'.  Keyword and value are separated
     * by '='.  If the keyword contains a '/' char, then the left side is
     * the keyword, and the right side is its description.
     * @return {string}
     */
    toString() {
        const idStr= (ID_KEY+KW_VAL_SEP+this.params[ID_KEY]);
        const retStr= Object.keys(this.params).sort().reduce((str,key) => {
            if (key!==ID_KEY && isDefined(this.params[key])) str+= PARAM_SEP+key+KW_VAL_SEP+this.params[key];
            return str;
        },idStr);
        return retStr;
    }


    cloneRequest() {
        const sr = this.newInstance();
        sr.copyFrom(this);
        return sr;
    }

    newInstance() {
        return new ServerRequest();
    }

    equals(obj) {
        if (obj instanceof ServerRequest) {
            return this.toString()===obj.toString();
        }
        return false;
    }

//====================================================================
//  convenience data converting routines
//====================================================================
    getBooleanParam(key, def=false) {
        return has(this.params,key) ? toBoolean(this.params[key]) : def;
    }

    getIntParam(key, def=0) {
        const retval= parseInt(this.getParam(key)+'');
        return !isNaN(retval) ? retval : def;
    }

    /**
     *
     * @param key
     * @param def
     * @return {number}
     */
    getFloatParam(key, def=0) {
        const retval= parseFloat(this.getParam(key)+'');
        return !isNaN(retval) ? retval : def;
    }

    /**
     *
     * @param key
     * @return {WorldPt}
     */
    getWorldPtParam(key) {
        const wpStr= this.getParam(key);
        return wpStr ? parseWorldPt(wpStr) : null;
    }

    static addParam(str, key, value) {
        if (str && key && value) {
            str+= (PARAM_SEP+key+KW_VAL_SEP+value);
        }
        return str;
    }
}
