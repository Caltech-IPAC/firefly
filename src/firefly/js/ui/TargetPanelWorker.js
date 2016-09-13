/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

//-----------------
import PositionParser from '../util/PositionParser';
import PositionFieldDef from '../data/form/PositionFieldDef';
import Point from '../visualize/Point';
import {fetchUrl} from '../util/WebUtil.js';
import {parseWorldPt} from '../visualize/Point';


export {formatPosForTextField} from '../data/form/PositionFieldDef.js';


// return an object with:
//     feedback, string
//     inputType
//     valid, boolean
//     aborter function
//     wpt, WorldPt
//     showHelp, boolean
//     return parse results

var makeResolverPromise= function(objName, resolver) {
    var ignoreSearchResults= null;
    var aborted= false;

    var aborter= function() {
        aborted= true;
        if (ignoreSearchResults!=null) ignoreSearchResults();
    };

    var workerPromise= new Promise(
        function(resolve, reject) {
            setTimeout( ()=> {
                if (aborted) {
                    reject();
                }
                else {
                    var {p, rejectFunc}= makeSearchPromise(objName, resolver);
                    ignoreSearchResults= rejectFunc;
                    resolve(p);
                }
            }, 200);
        }
    );



    return {p:workerPromise, aborter};
};



function makeSearchPromise(objName, resolver= 'nedthensimbad') {
    var rejectFunc= null;
    var url= `sticky/CmdSrv?objName=${objName}&resolver=${resolver}&cmd=CmdResolveName`;
    var searchPromise= new Promise(
        function(resolve, reject) {
            fetchUrl(url).then( (response) => {
                response.json().then( (value) => {
                    resolve(value);
                });
            }).catch( (error) => {
                return reject(error);
            });
        });

    var abortPromise= new Promise(function(resolve,reject) {
        rejectFunc= reject;
    });
    return {p:Promise.race([searchPromise,abortPromise]), rejectFunc};
}



export var parseTarget= function(inStr, lastResults, resolver) {
    var wpt= null;
    var valid= false;
    var targetInput= inStr;
    var feedback= 'valid: false';
    //var update= true;
    var showHelp= true;
    var posFieldDef= PositionFieldDef.makePositionFieldDef();
    var resolveData;
    var resolvePromise= null;
    var aborter= null;
    if (targetInput) {
        if (lastResults && lastResults.aborter) lastResults.aborter();
        try {
            valid= posFieldDef.validateSoft(targetInput);
        } catch (e) {
            valid= false;
        }
        if (valid) {
            wpt= posFieldDef.getPosition();
            if (posFieldDef.getInputType()===PositionParser.PositionParsedInput.Position) {
                showHelp= false;
                feedback= posFieldDef.formatPosForHelp(wpt);
            }
            else {
                if (posFieldDef.getObjectName()) {
                    showHelp= false;
                    feedback= `<i>Resolving:</i>  ${posFieldDef.getObjectName()}`;
                    resolveData= resolveObject(posFieldDef, resolver);
                }
                else {
                    showHelp= true;
                }
            }
        }
    }
    if (resolveData && resolveData.p && resolveData.aborter) {
        resolvePromise= resolveData.p;
        aborter= resolveData.aborter;
    }

    return {showHelp, feedback,
            inputType: posFieldDef.getInputType(),
            valid, resolvePromise, wpt, aborter  };
};


export function getFeedback(wpt) {
    var posFieldDef= PositionFieldDef.makePositionFieldDef();
    return posFieldDef.formatTargetForHelp(wpt);
}

var resolveObject = function(posFieldDef, resolver) {
    var objName= posFieldDef.getObjectName();
    if (!objName) {
        return {
            showHelp: true,
            valid : true,
            feedback: ''
        };
    }

    var {p,aborter}= makeResolverPromise(objName, resolver);
    p= p.then( (results) =>
        {
            if (results) {
                if (results[0].success === 'true') {
                    var wpt = parseWorldPt(results[0].data);
                    return {
                        showHelp: false,
                        feedback: posFieldDef.formatTargetForHelp(wpt),
                        valid: true,
                        wpt
                    };
                }
                else {
                    return {
                        showHelp: false,
                        feedback: `Could not resolve: ${objName}`,
                        valid: false,
                        wpt: null
                    };
                }
            }
            else {
                return {
                    showHelp: false,
                    feedback: `Could not resolve: ${objName}`,
                    valid: false,
                    wpt: null
                };
            }
        }
    ).catch(function(e) {
            console.log(`aborted: ${objName}`);
            if (e) console.error(e);
        });

    return {p,aborter};

};



