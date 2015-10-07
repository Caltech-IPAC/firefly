/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

//-----------------
import PositionParser from '../util/PositionParser';
import PositionFieldDef from '../data/form/PositionFieldDef';
import Point from '../visualize/Point';
import http from 'http';




// return an object with:
//     feedback, string
//     inputType
//     valid, boolean
//     aborter function
//     wpt, WorldPt
//     showHelp, boolean
//     return parse results

var makeResolverPromise= function(objName) {
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
                    var {p, rejectFunc}= makeSearchPromise(objName);
                    ignoreSearchResults= rejectFunc;
                    resolve(p);
                }
            }, 200);
        }
    );



    return {p:workerPromise, aborter};
};



var makeSearchPromise= function(objName) {
    var rejectFunc= null;
    var url= '/fftools/sticky/CmdSrv?objName='+objName+'&resolver=nedthensimbad&cmd=CmdResolveName';
    var searchPromise= new Promise(
        function(resolve, reject) {
            http.get(
                { path : url },
                (res) => {
                    res.on('data', (buf) =>  resolve(buf) );
                    res.on('error', (e) =>  reject(e) );
                    res.on('end', () => { });
                });
        }).then( buf => JSON.parse(buf) );
    var abortPromise= new Promise(function(resolve,reject) {
        rejectFunc= reject;
    });
    return {p:Promise.race([searchPromise,abortPromise]), rejectFunc};
};



export var parseTarget= function(inStr, lastResults) {
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
                    feedback= '<i>Resolving:</i> ' + posFieldDef.getObjectName();
                    resolveData= resolveObject(posFieldDef);
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


var resolveObject = function(posFieldDef) {
    var objName= posFieldDef.getObjectName();
    if (!objName) {
        return {
            showHelp: true,
            valid : true,
            feedback: ''
        };
    }

    var {p,aborter}= makeResolverPromise(objName);
    p= p.then( (results) =>
        {
            if (results) {
                if (results[0].success === 'true') {
                    var wpt = Point.parseWorldPt(results[0].data);
                    return {
                        showHelp: false,
                        feedback: posFieldDef.formatTargetForHelp(wpt),
                        valid: true,
                        wpt: wpt
                    };
                }
                else {
                    return {
                        showHelp: false,
                        feedback: 'Could not resolve: ' + objName,
                        valid: false,
                        wpt: null
                    };
                }
            }
            else {
                return {
                    showHelp: false,
                    feedback: 'Could not resolve: ' + objName,
                    valid: false,
                    wpt: null
                };
            }
        }
    ).catch(function() {
            //console.log('aborted: '+objName);
        });

    return {p,aborter};

};



