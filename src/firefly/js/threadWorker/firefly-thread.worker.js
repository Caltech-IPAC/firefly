import PlotState from '../visualize/PlotState';
import {doRawDataWork} from '../visualize/rawData/ManageRawDataThread.js';
import {RawDataThreadActions} from './WorkerThreadActions.js';


const rdActionList= Object.keys(RawDataThreadActions);

globalThis.onmessage= (event) => {
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
    let sendStatus= () => undefined;
    if (action.payload.plotId && action.payload.plotStateSerialized) {
        sendStatus= (messageText) => {
            const plotState= PlotState.parse(action.payload.plotStateSerialized);
            postMessage({message:true,
                messageText,
                plotId:action.payload.plotId,
                requestKey:plotState.getWebPlotRequest().getRequestKey()});
        };
    }
    doRawDataWork({...action,sendStatus})
        .then( ({data,transferable}) => postMessage({success:true, ...data, callKey}, transferable) )
        .catch( (error) => postMessage({error,callKey, success:false}) );
}