import {isEmpty, isArray, isObject} from 'lodash';
import React from 'react';
import {WSCH} from '../core/History';
import {dispatchAddActionWatcher} from '../core/MasterSaga.js';
import {Logger} from '../util/Logger.js';
import {makeWorldPt, parseWorldPt} from '../visualize/Point';


const API_STR= 'api';

export const WEB_API_CMD= 'WebApi.Launch';

export const WebApiStat= {
    API_NOT_USED: 'API_NOT_USED',
    SHOW_HELP: 'SHOW_ERROR',
    EXECUTE_API_CMD: 'EXECUTE_API_CMD',
};

export const WebApiHelpType= {
    NONE: 'NONE',
    OVERVIEW_HELP: 'OVERVIEW_HELP',
    COMMAND_NOT_FOUND: 'COMMAND_NOT_FOUND',
    NO_COMMAND: 'NO_COMMAND',
    INVALID_PARAMS: 'INVALID_PARAMS',
    COMMAND_HELP: 'COMMAND_HELP',
};

export const ReservedParams= {
    POSITION : {
        name:'POSITION',
        replace: ['worldPt', 'ra', 'dec'],
        desc: [
            'worldPt format - lon;lat;Csys - eg: 1.0;2.3;EQ_J2000 or 5;4;GAL',
            'ra, dec format - ra=1&dec=2 - always j2000'
         ]
    },
    SR : {
        name: 'SR',
        replace: ['sr'],
        desc: [
            'sr format options: d or none - degrees, m - arcminutes, s - arcseconds',
            'sr=1.1d or sr=1.1 - 1.1 degrees',
            'sr=150s - 150 arcseconds',
            'sr=22m - 22 arcminutes',
        ]
    },
    imageSize : {
        name: 'imageSize',
        replace: ['imagesize'],
        desc: [
            'imageSize format options: d or none - degrees, m - arcminutes, s - arcseconds',
            'imageSize=1.1d or imageSize=1.1 - 1.1 degrees',
            'imageSize=150s - 150 arcseconds',
            'imageSize=22m - 22 arcminutes',
        ]
    },
};

/**
 * @global
 * @public
 * @typedef WebApiExample
 * @summary a url api command to execute
 *
 * @prop {string} desc - a description of the example
 * @prop {string} url - the example URL
 */


/**
 * @global
 * @public
 * @typedef WebApiParameter
 * @summary a url api command to execute
 *
 * @prop {string|Array.<string>} desc - a description of the example
 * @prop {boolean} [isRequired]
 */


/**
 * @global
 * @public
 * @typedef WebApiCommand
 * @summary a url api command to execute
 *
 * @prop {string} cmd - the command string, a command of undefined by be specified, in the case the validate function should
 * always return valid of false, and a general msg and general example covering the whole API
 * @prop {boolean} [allowAdditionalParameters=false]
 * @prop {boolean} [needParams= true]
 * @prop {Function} execute
 * @prop {Function} validate - a function return an object {valid:boolean, msg: String, examples: Array.<WebApiExample>},
 * @prop {Array.<string>} overview - a description of the example
 * @prop {Array.<Object.<{string,WebApiParameter}>|Object.<{string,string}>|Object.<{string,Array.<string>}>>} parameters - a description of the example
 * @prop {Array.<WebApiExample>} [examples] if error type is SHOW_HELP, then should contain the error message
 * if valid msg and example is ignored
 */

/**
 * @typedef UrlApiStatus
 *
 * @prop {string} status
 * @prop {string} [helpType]
 * @prop {string} [contextMessage] if error type is SHOW_HELP, then should contain the error message
 * @prop {string} [cmd]
 * @prop {Function} [execute] - passed on WebApiStat.EXECUTE_API_CMD
 * @prop {Object} [params]
 */


/**
 * Returns true if the URL is attempting to use the firefly web api and that the commands array is non-empty
 * @param {Array.<WebApiCommand>} commandsAry - checks to see if array is non-empty
 * @param {String} url - override url
 * @return {boolean}
 */
export const isUsingWebApi= (commandsAry, url) => !isEmpty(commandsAry) && validAPIURL(url ?? document.location);

export function initWebApi(webApiCommands) {
    dispatchAddActionWatcher({
        id: 'webApiWatcher',
        actions: [WEB_API_CMD],
        callback: receiveRemoteWebApiRequest,
        params: {webApiCommands,callId:0}
    });
}

let firstRemoteApiCall= true;

function receiveRemoteWebApiRequest(action, cancelSelf, {webApiCommands,callId}) {
    const {payload,type}= action ?? {};
    if (type!==WEB_API_CMD) return;
    if (!payload.paramStr) return;
    const workingUrl= new URL(document.location);
    const url= workingUrl.origin+workingUrl.pathname + '?'+ payload.paramStr;
    const webApi= isUsingWebApi(webApiCommands, url);
    if (!webApi) return;
    const {status, cmd, execute, params}= evaluateWebApi(webApiCommands, url);
    const logger = Logger('remote web api');
    switch (status) {
        case WebApiStat.EXECUTE_API_CMD:
            const callParams= {...params, callId: `callid-${callId}` };
            // setTimeout(() => execute?.(cmd,callParams), firstRemoteApiCall ? 2000 : 10);
            execute?.(cmd,callParams);
            firstRemoteApiCall= false;
            callId++;
            break;
        case WebApiStat.SHOW_HELP:
            logger.warn('show help');
           break;
        default:
            logger.error(`Unexpect status, can't handle web api: ${status}`);
            break;
    }
    return {webApiCommands,callId};
}




const {NONE, OVERVIEW_HELP, COMMAND_NOT_FOUND, NO_COMMAND, INVALID_PARAMS, COMMAND_HELP}= WebApiHelpType;

/**
 *
 * @param {Array.<WebApiCommand>} commandsAry
 * @param {String} [overrideUrl] - is passed use this instead of location
 * @return {UrlApiStatus}
 */
export function evaluateWebApi(commandsAry, overrideUrl) {

    const url= overrideUrl ?? document.location;

    if (isEmpty(commandsAry) || !validAPIURL(url) ) return {status:WebApiStat.API_NOT_USED};

    const cmd= new URL(url).searchParams.getAll(API_STR)[0];
    const urlApiCmd= commandsAry.find( (c) => cmd && (c.cmd.toLocaleLowerCase()===cmd.toLocaleLowerCase()));
    const {params,originalParams,badParams}= makeParams(url, urlApiCmd);
    const status= WebApiStat.SHOW_HELP;

    if (!urlApiCmd) {
        let helpType;
        if (isEmpty(params) && !cmd) helpType= OVERVIEW_HELP;
        else if (cmd) helpType= COMMAND_NOT_FOUND;
        else helpType= NO_COMMAND;

        return {status, helpType, params:originalParams, cmd};
    }

    const {valid, invalidStatObj}= validate(urlApiCmd, params, originalParams, badParams);

    return valid ?
        {status:WebApiStat.EXECUTE_API_CMD, cmd:urlApiCmd.cmd, params, execute:urlApiCmd.execute} :
        invalidStatObj;
}


/**
 *
 * @param {WebApiCommand} urlApiCmd
 * @param {Object} params
 * @param {Object} originalParams
 * @param {Array.<string>} inBadParams
 * @return {{valid: boolean, invalidStatObj: {helpType: string, cmd: (string|string), params: *, status: string}}|*}
 */
function validate(urlApiCmd, params, originalParams, inBadParams=[]) {
    const needsParams= urlApiCmd.needsParams ?? true;
    let badParams;
    let missingReqParams;

    const makeInvalid= (invalidStatObj,helpType= INVALID_PARAMS) => ({
            valid:false,
            invalidStatObj:{status:WebApiStat.SHOW_HELP, cmd: urlApiCmd.cmd, params:originalParams, helpType, ...invalidStatObj}}
    );

    // ---- check for no parameters
    if (isEmpty(params) && urlApiCmd && needsParams)  {
        return makeInvalid({contextMessage: 'requires parameters', params:undefined},COMMAND_HELP);
    }

    // ---- check for additional parameters beyond the normal list
    if (!urlApiCmd.allowAdditionalParameters) {
        badParams= findExtraParams(Object.keys(urlApiCmd.parameters), params);
        badParams.push(...inBadParams);
        missingReqParams= checkForRequiredParams(params,urlApiCmd.parameters);
        if (!isEmpty(badParams) || isEmpty(params)) {
            return makeInvalid({contextMessage:`url contains unsupported params: ${badParams?badParams.join():''}`,
                                badParams, missingParams:missingReqParams});
        }
    }
    badParams= inBadParams;

    // --- check for parameters that are required
    missingReqParams= checkForRequiredParams(params,urlApiCmd.parameters);
    if (!isEmpty(missingReqParams)) {
        return makeInvalid({contextMessage:'Missing required parameters', missingParams: missingReqParams});
    }

    // --- call the validation function for the command
    const {valid,msg:contextMessage, badParams:missingParams=[]}=
                   urlApiCmd?.validate(params) ?? {valid:false, msg:'no validation function defined'};

    return valid ? {valid:true} : makeInvalid({contextMessage, badParams, missingParams});
}

/**
 *
 * @param {Object} passedParams - parameters from url
 * @param {Array} parameters - possible parameter list
 * @return {Array.<string>} return an array of missing parameters names
 */
function checkForRequiredParams(passedParams, parameters) {
    const required= Object.entries(parameters).filter( ([k,v]) => isObject(v) && v.isRequired).map( ([k]) => k);
    if (isEmpty(required)) return [];
    const missingReq= required.filter( (r) => !passedParams[r]);
    if (!missingReq.length) return undefined;
    const rNames= Object.keys(ReservedParams);
    return missingReq.map( (p) => rNames.includes(p) ? ReservedParams[p].replace.join() : p);
}


function validAPIURL(url) {
    const {search,pathname} = new URL(url);
    const pathLower= pathname.toLocaleLowerCase();
    if (pathLower.endsWith(`/${API_STR}`) || pathLower.endsWith(`/${API_STR}/`)) return true;

    if (search.startsWith(`?${API_STR}`)) {
        return true;
    }
}

function makeParams(url, urlApiCmd) {
    const params= {};
    const {searchParams} = new URL(url);
    for(const k of searchParams.keys()) {
        if (k!== API_STR && k!== WSCH) {
            params[k]= searchParams.getAll(k);
            if (params[k].length===1) params[k]= params[k][0];
        }
    }

    if (!urlApiCmd || !urlApiCmd.parameters) return params;

    const pKeys= Object.keys(urlApiCmd.parameters);
    const normalizedParams= Object.entries(params)
        .reduce((obj, [k,v]) => {
            const foundKey= findInsensitiveStr(pKeys,k);
            obj[foundKey||k]= v;
            return obj;
        }, {});

    const {params:convertedParams,badParams}= convertReservedParams(normalizedParams, urlApiCmd.parameters);
    return {originalParams:normalizedParams, params:convertedParams,badParams};
}

function findInsensitiveStr(list, str) {
    const lowerStr= str.toLowerCase();
    return list.find( (aStr) => aStr.toLowerCase()===lowerStr);
}


function getDegreeVal(inVal) {
    if (!isNaN(Number(inVal))) {
        return Number(inVal);
    }
    else if (inVal.endsWith('d') && !isNaN(Number(inVal.substring(0,inVal.length-1)))) {
        return Number(inVal.substring(0,inVal.length-1));
    }
    else if (inVal.endsWith('m') && !isNaN(Number(inVal.substring(0,inVal.length-1)))) {
        return Number(inVal.substring(0,inVal.length-1)) /60;
    }
    else if (inVal.endsWith('s') && !isNaN(Number(inVal.substring(0,inVal.length-1)))) {
        return Number(inVal.substring(0,inVal.length-1)) /3600;
    }
}

const getV= (v) => isArray(v) ? v[0] : v;

function convertReservedParams(inParams, parameters) {

    const outParams= {...inParams};
    const pKeys= Object.keys(inParams);
    const badParams= [];
    if (parameters[ReservedParams.POSITION.name]) {
        const worldPtKey= findInsensitiveStr(pKeys,'WorldPt');
        const raKey= findInsensitiveStr(pKeys,'ra');
        const decKey= findInsensitiveStr(pKeys,'dec');
        let wp;
        if (inParams[worldPtKey]) {
            wp= parseWorldPt(getV(inParams[worldPtKey]));
            Reflect.deleteProperty(outParams, worldPtKey);
            if (!wp) badParams.push(worldPtKey);
        }
        if (inParams[raKey] || inParams[decKey]) {
            if (!wp) wp= makeWorldPt(Number(getV(inParams[raKey])), Number(getV(inParams[decKey])));
            Reflect.deleteProperty(outParams, raKey);
            Reflect.deleteProperty(outParams, decKey);
            if (!wp) {
                if (isNaN(Number(inParams[raKey]))) badParams.push(raKey);
                if (isNaN(Number(inParams[decKey]))) badParams.push(decKey);
            }
        }
        if (wp) outParams[ReservedParams.POSITION.name]= wp;
    }
    if (parameters[ReservedParams.SR.name]) {
        const srKey= findInsensitiveStr(pKeys,'sr');
        if (inParams[srKey]) {
            const srVal= getV(inParams[srKey]);
            const degVal= getDegreeVal(srVal);
            if (!isNaN(degVal)) {
                Reflect.deleteProperty(outParams, srKey);
                outParams[ReservedParams.SR.name]= degVal;
            }
            else {
                badParams.push(srKey);
            }
        }
    }
    if (parameters[ReservedParams.imageSize.name]) {
        const isKey= findInsensitiveStr(pKeys,'imageSize');
        if (inParams[isKey]) {
            const isVal= inParams[isKey].toLowerCase();
            const degVal= getDegreeVal(isVal);
            if (!isNaN(degVal)) {
                Reflect.deleteProperty(outParams, isKey);
                outParams[ReservedParams.imageSize.name]= degVal;
            }
            else {
                badParams.push(isKey);
            }
        }
    }

    return {params:outParams,badParams};
}


/**
 *
 * @param {string} desc
 * @param {string} cmd
 * @param {Object.<String,String>} [params] - key is the param, value is the param value
 * @return {{url: string, desc: string}}
 */
export function makeExample(desc,cmd=undefined, params=undefined) {
    const {origin,pathname}= new URL(window.location);
    const sp= params ? Object.entries(params)
        .reduce( (str,[k,v]) => {
            const ary= isArray(v) ? v : [v];
            const result= ary.reduce( (pStr,e) => (pStr +`&${k}=${e}`),'');
            return str+ result;
        } ,'') : '';
    const cmdStr= cmd ? `${API_STR}=${cmd}${sp}` : API_STR;
    const url= `${origin}${pathname}?${cmdStr}`;
    return { desc, url };
}


export function makeExamples(cmd, exampleAry) {
    return exampleAry.map( ({sectionDesc,examples, desc,params}) => {
        return sectionDesc ?
            {sectionDesc,
                examples: examples.map( ({desc,params}) => makeExample(desc,cmd,params)) } :
            makeExample(desc,cmd,params);
    });
}


export function findExtraParams(paramList, passedParams) {
    const pKeys=Object.keys(passedParams);
    const rpNames= Object.values(ReservedParams).map( (v) => v.name.toLowerCase());
    const additionalParams= paramList
        .filter( (p) => rpNames.includes(p.toLowerCase()))
        .map( (p) => ReservedParams[p].replace)
        .flat();
    const extraKeys= pKeys.filter( (p) => !paramList.includes(p) && !additionalParams.includes(p));
    return extraKeys.length>0 ? extraKeys : [];
}

export function getReservedParamKeyDesc(paramKey) {
    switch (paramKey) {
        case ReservedParams.POSITION.name :
            return 'worldPt or ra/dec';
        case ReservedParams.SR.name :
            return ReservedParams.SR.replace[0];
        default:
            return paramKey;

    }

}
