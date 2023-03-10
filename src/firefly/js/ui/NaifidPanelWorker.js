/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

//-----------------
import {ServerParams} from '../data/ServerParams';
import {getCmdSrvSyncURL, toBoolean} from '../util/WebUtil';
import {fetchUrl} from '../util/fetch';


function makeResolverPromise(objName, naifIdFormat) {
    let ignoreSearchResults= null;
    let aborted= false;

    const aborter= function() {
        aborted= true;
        if (ignoreSearchResults!==null) ignoreSearchResults();
    };

    const workerPromise= new Promise(
        function(resolve, reject) {
            setTimeout( ()=> {
                if (aborted) {
                    reject();
                }
                else {
                    const {p, rejectFunc}= makeSearchPromise(objName, naifIdFormat);
                    ignoreSearchResults= rejectFunc;
                    resolve(p);
                }
            }, 200);
        }
    );

    return {p:workerPromise, aborter};
}



function makeSearchPromise(objName, naifIdFormat) {
    let rejectFunc= null;
    let url= `${getCmdSrvSyncURL()}?objName=${objName}&cmd=${ServerParams.RESOLVE_NAIFID}`;
    if (naifIdFormat) url += `&naifIdFormat=${naifIdFormat}`;
    const searchPromise= new Promise(
        function(resolve, reject) {
            let fetchOptions = {};
            // AbortController might not be available in older browsers
            if (typeof AbortController !== 'undefined') {
                // fetch will be aborted after timeout
                const fetchTimeoutMs = 7000;
                const controller = new AbortController();
                const signal = controller.signal;
                setTimeout(() => {
                    controller.abort();
                }, fetchTimeoutMs);
                fetchOptions = {signal};
            }

            fetchUrl(url, fetchOptions).then( (response) => {
                response.json().then((value) => {
                    resolve(value);
                });
            }).catch( (error) => {
                return reject(error);
            });
        });

    const abortPromise= new Promise(function(resolve,reject) {
        rejectFunc= reject;
    });
    return {p:Promise.race([searchPromise,abortPromise]), rejectFunc};
}


export function resolveNaifidObj(object, naifIdFormat){
    let result = resolveObject(object, naifIdFormat);
    return result;
}




function resolveObject(objName, naifIdFormat) {
    if (!objName) {
        return {
            showHelp: true,
            valid : true,
            feedback: ''
        };
    }

    let {p}= makeResolverPromise(objName, naifIdFormat);
    p= p.then( (results) =>
        {
            if (results) {
                if (toBoolean(results[0].success)) {
                    return{
                        data: results[0].data,
                        showHelp: false,
                        valid: true,
                    };
                }
                else {
                    return {
                        showHelp: false,
                        feedback: `Could not resolve: ${objName}`,
                        valid: false
                    };
                }
            }
            else {
                return {
                    showHelp: false,
                    feedback: `Could not resolve: ${objName}`,
                    valid: false
                };
            }
        }
    ).catch((e) => {
        let feedback = `Could not resolve: ${objName}`;
        if (e.name === 'AbortError') {
            feedback += '. Unresponsive service.';
        } else {
            feedback += '. Unexpected error.';
            if (e) console.error(e);
        }
        return {
            showHelp: false,
            feedback,
            valid: false,
            wpt: null
        };
    });
    return {p};

}



