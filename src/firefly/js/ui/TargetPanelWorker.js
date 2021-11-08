/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {PositionParsedInputType} from '../util/PositionParser.js';
import {positionValidateSoft,formatTargetForHelp, formatPosForHelp} from './PositionFieldDef.js';
import {parseWorldPt} from '../visualize/Point';
import {fetchUrl} from '../util/fetch';
import {getCmdSrvSyncURL} from '../util/WebUtil';

// return an object with:
//     feedback, string
//     inputType
//     valid, boolean
//     aborter function
//     wpt, WorldPt
//     showHelp, boolean
//     return parse results

function makeResolverPromise(objName, resolver) {
    let ignoreSearchResults= undefined;
    let aborted= false;

    const aborter= function() {
        aborted= true;
        ignoreSearchResults?.();
    };

    const workerPromise= new Promise(
        (resolve, reject) => {
            setTimeout( ()=> {
                if (aborted) {
                    reject();
                    return;
                }
                const {p, rejectFunc}= makeSearchPromise(objName, resolver);
                ignoreSearchResults= rejectFunc;
                resolve(p);
            }, 200);
        }
    );
    return {p:workerPromise, aborter};
}

const FETCH_TIMEOUT= 7000; // 7 seconds - fetch will be aborted after timeout

function makeSearchPromise(objName, resolver= 'nedthensimbad') {
    let rejectFunc= null;
    const searchPromise= new Promise(
        async (resolve, reject) => {
            const fetchOptions = {};
            rejectFunc= reject;
            if (window.AbortController) { // AbortController might not be available in older browsers
                const controller = new window.AbortController();
                fetchOptions.signal= controller.signal;
                setTimeout(() => controller.abort(), FETCH_TIMEOUT);
            }
            const url= `${getCmdSrvSyncURL()}?objName=${objName}&resolver=${resolver}&cmd=CmdResolveName`;
            try {
                const response= await fetchUrl(url, fetchOptions);
                resolve(await response.json());
            }
            catch(error) {
                reject(error);
            }
        });
    return {p:searchPromise, rejectFunc};
}

export function parseTarget(inStr, lastResults, resolver) {
    const targetInput= inStr;
    let feedback= 'valid: false';
    let showHelp= true;
    let resolveData= {aborter:undefined, resolvePromise:undefined};
    let parseError;
    let result= {valid: false};

    if (!targetInput)  return {valid:false, feedback, showHelp};
    if (lastResults?.aborter) lastResults.aborter();
    try {
        result= positionValidateSoft(targetInput);
    } catch (e) {
        result={valid: false};
        parseError = e;
    }

    const {inputType,valid, position:wpt, objName}= result;
    if (valid) {
        if (inputType===PositionParsedInputType.Position || inputType===PositionParsedInputType.DB_ID) {
            showHelp= false;
            feedback= formatPosForHelp(wpt);
        }
        else {
            if (objName) {
                showHelp= false;
                feedback= `<i>Resolving:</i>  ${objName}`;
                resolveData= resolveObject(objName, resolver);
            }
            else {
                showHelp= true;
            }
        }
    }

    const {aborter, p:resolvePromise} = resolveData;
    return {showHelp, feedback, parseError, inputType, valid, resolvePromise, wpt, aborter  };
}
function resolveObject(objNameIn, resolver) {
    const objName= encodeURIComponent(objNameIn);
    if (!objName) return { showHelp: true, valid : true, feedback: '' };

    let {p,aborter}= makeResolverPromise(objName, resolver);
    p= p.then( (results) =>
        {
            if (results?.[0]?.success === 'true') {
                const wpt = parseWorldPt(results[0].data);
                return {showHelp: false, feedback: formatTargetForHelp(wpt), valid: true, wpt };
            }
            else {
                return {showHelp: false, feedback: `Could not resolve: ${objNameIn}`, valid: false, wpt: undefined };
            }
        }
    ).catch((e) => {
        // e is undefined when a newer request came in, and promise is rejected
        if (e) {
            let feedback = `Could not resolve: ${objNameIn}`;
            if (e.name === 'AbortError') {
                feedback += '. Unresponsive service.';
            } else {
                feedback += '. Unexpected error.';
                if (e) console.error(e);
            }
            return { showHelp: false, feedback, valid: false, wpt: undefined };
        }
    });

    return {p,aborter};
}