/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import ExternalAccessUtils from './ExternalAccessUtils.js';
import {POINT} from '../visualize/PlotCmdExtension.js';
import {dispatchChangePointSelection} from '../visualize/ImagePlotCntlr.js';

const EXTENSION_ADD= 'ExternalAccessCntlr/extensionAdd';
const EXTENSION_REMOVE= 'ExternalAccessCntlr/extensionRemove';
const EXTENSION_ACTIVATE= 'ExternalAccessCntlr/extensionActivate';
const CHANNEL_ACTIVATE= 'ExternalAccessCntlr/channelActivate';

const EXTERNAL_ACCESS_KEY= 'externalAccess';
import {flux} from '../Firefly.js';

const ALL_MPW= 'AllMpw';

const initState= {
    extensionList : []
};

export function extensionRoot() { return flux.getState()[EXTERNAL_ACCESS_KEY]; }



export function dispatchExtensionAdd(extension) {
    flux.process({type: ExternalAccessCntlr.EXTENSION_ADD, payload: {extension}});
}

export function dispatchExtensionRemove(extensionId) {
    flux.process({type: ExternalAccessCntlr.EXTENSION_REMOVE, payload: {id: extensionId}});
}

export function dispatchExtensionActivate(extension, resultData) {
    flux.process({type: ExternalAccessCntlr.EXTENSION_ACTIVATE, payload: {extension, resultData}});
}

export function dispatchChannelActivate(channelId) {
    flux.process({type: ExternalAccessCntlr.CHANNEL_ACTIVATE, payload: {channelId}});
}



const extensionActivateActionCreator= function(rawAction) {
    return (dispatcher) => {

        if (rawAction.payload) {
            var {payload : {extension, resultData}}= rawAction;
            if (extension && resultData) {
                ExternalAccessUtils.doExtensionActivate(extension,resultData);
            }
        }
        dispatcher(rawAction);
    };
};


function extensionAddActionCreator(rawAction) {
    return (dispatcher) => {

        if (rawAction.payload) {
            var {payload : {extension}}= rawAction;
            if (extension.extType===POINT) {
                dispatchChangePointSelection('ExtensionSystem', true);
            }
        }
        dispatcher(rawAction);
    };
};


function reducers() {
    return {
        [EXTERNAL_ACCESS_KEY]: reducer,
    };
}


function actionCreators() {
    return {
        [EXTENSION_ACTIVATE]: extensionActivateActionCreator,
        [EXTENSION_ADD]: extensionAddActionCreator
    };
}

function reducer(state=initState, action={}) {
    if (!action.payload || !action.type) return state;

    var retState= state;
    switch (action.type) {
        case EXTENSION_ADD  :
            retState= addExtension(state,action);
            break;
        case EXTENSION_REMOVE  :
            retState= removeExtension(state,action);
            break;
        case EXTENSION_ACTIVATE  :
            retState= state;// todo something
            break;

        case CHANNEL_ACTIVATE  :
            retState= updateChannel(state,action);
            break;

    }
    return retState;
}

const addExtension= function(state, action) {
    var {extension}= action.payload;
    const {extensionList}= state;
    var newAry;
    if (extensionList.find( (e) => e.id===extension.id)) {
        newAry= extensionList.map( (e) => e.id===extension.id ? extension : e);
    }
    else {
        newAry= [...state.extensionList, extension];
    }
    return Object.assign({}, state, {extensionList:newAry});
};

const removeExtension= function(state, action) {
    var {id}= action.payload;
    var newAry= state.extensionList.filter((extension) => {return (extension.id !== id);});
    return Object.assign({}, state, {extensionList:newAry});
};

const updateChannel= function(state, action) {
    var {channelId}= action.payload;
    return Object.assign({}, state, {remoteChannel:channelId});
};



//============ EXPORTS ===========
//============ EXPORTS ===========

var ExternalAccessCntlr = {
    reducers, actionCreators, extensionActivateActionCreator, EXTENSION_ADD, EXTENSION_REMOVE,
    EXTENSION_ACTIVATE, CHANNEL_ACTIVATE, EXTERNAL_ACCESS_KEY,
    ALL_MPW
    };
export default ExternalAccessCntlr;

//============ EXPORTS ===========
//============ EXPORTS ===========
