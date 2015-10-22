/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {flux} from '../Firefly.js';

const APP_LOAD = 'app-data/APP_LOAD';
const APP_UPDATE = 'app-data/APP_UPDATE';
const SHOW_DIALOG = 'app-data/SHOW_DIALOG';
const HIDE_DIALOG = 'app-data/HIDE_DIALOG';
const HIDE_ALL_DIALOGS = 'app-data/HIDE_ALL_DIALOGS';

const APP_DATA_KEY= 'appData';


function getInitState() {
    return {
        isReady : false
    };
}

function reducer(state=getInitState(), action={}) {

    switch (action.type) {
        case (APP_LOAD)  :
            // adding a dummy sideEffect to test feature
            action.sideEffect( (dispatch) => fetchAppData(dispatch, 'fftools_v1.0.1 Beta WITH SIDE-EFFECT added.', 4) );
            return getInitState();

        case (APP_UPDATE)  :
            return Object.assign({}, state, action.payload);

        case (SHOW_DIALOG)  :
            return showDialogChange(state,action);

        case (HIDE_DIALOG)  :
            return hideDialogChange(state,action);

        case (HIDE_ALL_DIALOGS)  :
            return hideAllDialogsChange(state,action);


        default:
            return state;
    }

}


const showDialogChange= function(state,action) {
    if (!action.payload) return state;
    var {dialogId}= action.payload;
    if (!dialogId) return state;

    state= Object.assign({},state);

    if (!state.dialogs) state.dialogs= {};

    if (!state.dialogs[dialogId]) {
       state.dialogs[dialogId]= {visible:false};
    }

    if (!state.dialogs[dialogId].visible) {
        state.dialogs[dialogId].visible= true;
    }
    return state;
};

const hideDialogChange= function(state,action) {
    if (!action.payload) return state;
    var {dialogId}= action.payload;
    if (!dialogId || !state.dialogs || !state.dialogs[dialogId]) return state;

    if (state.dialogs[dialogId].visible) {
        state= Object.assign({},state);
        state.dialogs[dialogId].visible= false;
    }
    return state;
};

const hideAllDialogsChange= function(state) {
    if (!state.dialogs) return state;
    Object.keys(state.dialogs).forEach( (dialog) => { dialog.visible=false; } );
    return Object.assign({}, state);
};





function loadAppData() {

    return function (dispatch) {
        dispatch({ type : APP_LOAD });
        fetchAppData(dispatch, 'fftools_v1.0.1 Beta', 2);
    };
}

/**
 *
 * @param appData {Object} The partial object to merge with the appData branch under root
 * @returns {{type: string, payload: object}}
 */
function updateAppData(appData) {
    return { type : APP_UPDATE, payload: appData };
}

function fetchAppData(dispatch, version, waitSec) {

    setTimeout(function () {
        var mockData = {
            isReady: true,
            props: {
                version
            }
        };
        dispatch( updateAppData(mockData) );
    }, waitSec * 1000);

    //ServerApi.loadAppData()
    //    .then((data) => {
    //        dispatch(updateAppData(data));
    //    })
    //    .catch((errorMessage) => {
    //        var data = {
    //            isReady: false,
    //            errorMessage: errorMessage
    //        };
    //        dispatch(updateAppData(data));
    //    });
}

const isDialogVisible= function(dialogKey) {
    var dialogs= flux.getState()[APP_DATA_KEY].dialogs;
    return (dialogs && dialogs[dialogKey] && dialogs[dialogKey].visible) ? true : false;
};


const showDialog= function(dialogId) {
    flux.process({type: SHOW_DIALOG, payload: {dialogId}});
};

const hideDialog= function(dialogId) {
    flux.process({type: HIDE_DIALOG, payload: {dialogId}});
};

const hideAllDialogs= function() {
    flux.process({type: HIDE_ALL_DIALOGS, payload: {}});
};


var AppDataCntlr= {
    APP_LOAD,
    APP_UPDATE,
    SHOW_DIALOG,
    HIDE_DIALOG,
    APP_DATA_KEY,
    reducer,
    loadAppData,
    updateAppData,
    isDialogVisible,
    showDialog,
    hideDialog,
    hideAllDialogs
};

export default AppDataCntlr;

