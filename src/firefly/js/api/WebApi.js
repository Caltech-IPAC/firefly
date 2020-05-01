import {isEmpty} from 'lodash';
import {WSCH} from '../core/History';


const API_STR= 'api';
// const CMD_STR= 'cmd';

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



/**
 * @global
 * @public
 * @typedef WebApiExample
 * @summary a url api command to execute
 *
 * @prop {String} desc - a description of the example
 * @prop {String} url - the example URL
 */




/**
 * @global
 * @public
 * @typedef WebApiCommand
 * @summary a url api command to execute
 *
 * @prop {String} cmd - the command string, a command of undefined by be specified, in the case the validate function should
 * always return valid of false, and a general msg and general example covering the whole API
 * @prop {Function} execute
 * @prop {Function} validate - a function return an object {valid:boolean, msg: String, examples: Array.<WebApiExample>},
 * @prop {Array.<String>} overview - a description of the example
 * @prop {Array.<String>} parameters - a description of the example
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
 * @return {boolean}
 */
export const isUsingWebApi= (commandsAry) => !isEmpty(commandsAry) && validAPIURL(document.location);




const {NONE, OVERVIEW_HELP, COMMAND_NOT_FOUND, NO_COMMAND, INVALID_PARAMS, COMMAND_HELP}= WebApiHelpType;

/**
 *
 * @param {Array.<WebApiCommand>} commandsAry
 * @return {UrlApiStatus}
 */
export function evaluateWebApi(commandsAry) {

    const url= document.location;

    if (isEmpty(commandsAry) || !validAPIURL(url) ) return {status:WebApiStat.API_NOT_USED};

    let cmd= new URL(url).searchParams.getAll(API_STR)[0];
    const urlApiCmd= commandsAry.find( (c) => cmd && (c.cmd.toLocaleLowerCase()===cmd.toLocaleLowerCase()));
    const params= makeParams(url, urlApiCmd);
    const needsParams= urlApiCmd?.needsParams ?? true;
    const status= WebApiStat.SHOW_HELP;

    if (!urlApiCmd) {
        let helpType;
        if (isEmpty(params) && !cmd) helpType= OVERVIEW_HELP;
        else if (cmd) helpType= COMMAND_NOT_FOUND;
        else helpType= NO_COMMAND;

        return {status, helpType, params, cmd};
    }
    cmd= urlApiCmd.cmd; //fixes case issues

    const {valid,msg:contextMessage, badParams}= urlApiCmd?.validate(params) ?? {valid:false, msg:'no validation function defined'};
    if (isEmpty(params) && urlApiCmd && needsParams)  {
        return {status, helpType:COMMAND_HELP, contextMessage, cmd};
    }

    if (!valid) return {status, helpType:INVALID_PARAMS, contextMessage, cmd, params, badParams};

    return {status:WebApiStat.EXECUTE_API_CMD, helpType:NONE, cmd, params, execute:urlApiCmd.execute};
}

// export function evaluateWebApi(commandsAry) {
//
//     const url= document.location;
//
//     if (isEmpty(commandsAry) || !validAPIURL(url) ) return {status:WebApiStat.API_NOT_USED};
//
//     const cmd= new URL(url).searchParams.getAll(CMD_STR)[0];
//     const urlApiCmd= commandsAry.find( (c) => cmd && (c.cmd===cmd));
//     const params= makeParams(url, urlApiCmd);
//     const needsParams= urlApiCmd?.needsParams ?? true;
//     const status= WebApiStat.SHOW_HELP;
//
//     if (!urlApiCmd) {
//         let helpType;
//         if (isEmpty(params) && !cmd) helpType= OVERVIEW_HELP;
//         else if (cmd) helpType= COMMAND_NOT_FOUND;
//         else helpType= NO_COMMAND;
//
//         return {status, helpType, params, cmd};
//     }
//
//     const {valid,msg:contextMessage, badParams}= urlApiCmd?.validate(params) ?? {valid:false, msg:'no validation function defined'};
//     if (isEmpty(params) && urlApiCmd && needsParams)  {
//         return {status, helpType:COMMAND_HELP, contextMessage, cmd};
//     }
//
//     if (!valid) return {status, helpType:INVALID_PARAMS, contextMessage, cmd, params, badParams};
//
//     return {status:WebApiStat.EXECUTE_API_CMD, helpType:NONE, cmd, params, execute:urlApiCmd.execute};
// }
//
// function validAPIURL(url) {
//     const {searchParams, search,pathname} = new URL(url);
//     const pathLower= pathname.toLocaleLowerCase();
//     if (pathLower.endsWith(`/${API_STR}`) || pathLower.endsWith(`/${API_STR}/`)) return true;
//
//     if (search.startsWith(`?${API_STR}`)) {
//         const values= searchParams.getAll(API_STR);
//         if (values.length>0 && values[0]==='') return true;
//     }
// }

function validAPIURL(url) {
    const {search,pathname} = new URL(url);
    const pathLower= pathname.toLocaleLowerCase();
    if (pathLower.endsWith(`/${API_STR}`) || pathLower.endsWith(`/${API_STR}/`)) return true;

    if (search.startsWith(`?${API_STR}`)) {
        return true;
        // const values= searchParams.getAll(API_STR);
        // if (values.length>0 && values[0]==='') return true;
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
    
    if (urlApiCmd?.parameters) {
        const pKeys= Object.keys(urlApiCmd.parameters);
        const normalizedParams= Object.entries(params)
            .reduce((obj, [k,v]) => {
                const lowerK= k.toLowerCase();
                const foundKey= pKeys.find( (aKey) => aKey.toLowerCase()===lowerK);
                obj[foundKey||k]= v;
                return obj;
            }, {});
        return normalizedParams;
    }
    else {
        return params;
    }
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
    const sp= params ? Object.entries(params).reduce( (str,[k,v]) => (str+ `&${k}=${v}`) ,'') : '';
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
    if (!pKeys.every( (p) => paramList.includes(p))) {
        return pKeys.filter( (p) => !paramList.includes(p));
    }
    return undefined;
}
