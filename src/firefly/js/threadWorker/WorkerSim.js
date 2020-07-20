import {doRawDataWork} from '../visualize/rawData/ManageRawDataThread.js';
import {RawDataThreadActions} from './WorkerThreadActions.js';



const rdActionList= Object.keys(RawDataThreadActions);

export const WorkerSim= {
    postMessage: (data) => {
        const action= data;
        const {callKey,type}= action;
        try {
            if (rdActionList.includes(type)) handleRawDataActions(action, WorkerSim.onmessage);
        }
        catch (error) {
            WorkerSim.onmessage({data:{error,callKey, success:false}});
        }

    },

    terminate: () => undefined

};

function handleRawDataActions(action, onmessage) {
    const {callKey}= action;
    doRawDataWork(action)
        .then( ({data,transferable}) => onmessage({data:{success:true, ...data, callKey}}, transferable) )
        .catch( (error) => onmessage({data:{error,callKey, success:false}}) );
}
