import {doRawDataWork} from '../visualize/rawData/ManageRawDataThread.js';
import {getGlobalObj} from '../util/WebUtil.js';
import {RawDataThreadActions} from './WorkerThreadActions.js';


const rdActionList= Object.keys(RawDataThreadActions);

getGlobalObj().onmessage= (event) => {
    const action= event.data;
    const {callKey,type}= action;
    try {
        if (rdActionList.includes(type)) handleRawDataActions(action);
    }
    catch (error) {
        postMessage({error,callKey, success:false});
    }
};


function handleRawDataActions(action) {
    const {callKey}= action;
    doRawDataWork(action)
        .then( ({data,transferable}) => postMessage({success:true, ...data, callKey}, transferable) )
        .catch( (error) => postMessage({error,callKey, success:false}) );
}