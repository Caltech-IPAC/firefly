/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {get} from 'lodash';
import update from 'react-addons-update';

export const DIALOG_OR_COMPONENT_KEY = 'dialogOrComponent';

const DIALOG_KEY = 'dialog';
const COMPONENT_KEY = 'component';

/*---------------------------- ACTIONS -----------------------------*/

const SHOW_DIALOG = 'ComponentCntlr.showDialog';
const HIDE_DIALOG = 'ComponentCntlr.hideDialog';
const HIDE_ALL_DIALOGS = 'ComponentCntlr.hideAllDialogs';
const COMPONENT_STATE_CHANGE = 'ComponentCntlr.componentStateChange';



export default {
    SHOW_DIALOG,
    HIDE_DIALOG,
    COMPONENT_STATE_CHANGE,
    reducer
};

//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================


export function dispatchShowDialog(dialogId,ownerId=undefined) {
    flux.process({type: SHOW_DIALOG, payload: {dialogId, ownerId}});
}

export function dispatchHideDialog(dialogId) {
    flux.process({type: HIDE_DIALOG, payload: {dialogId}});
}

export function dispatchHideAllDialogs() {
    flux.process({type: HIDE_ALL_DIALOGS, payload: {}});
}

export function dispatchComponentStateChange(componentId, componentState) {
    flux.process({type: COMPONENT_STATE_CHANGE, payload: {componentId, componentState}});
}


//============================ Utilities ============================
//============================ Utilities ============================
//============================ Utilities ============================

export function isDialogVisible(dialogKey) {
    return get(flux.getState()[DIALOG_OR_COMPONENT_KEY], [DIALOG_KEY,dialogKey,'visible'],false);
}

export function getDialogOwner(dialogKey) {
    const d= get(flux.getState()[DIALOG_OR_COMPONENT_KEY], [DIALOG_KEY,dialogKey], {});
    return (d.visible && d.ownerId) ? d.ownerId : null;
}

export function getComponentState(componentKey) {
    return get(flux.getState()[DIALOG_OR_COMPONENT_KEY], [COMPONENT_KEY,componentKey], {});
}

/*
   This controller handles small (one level deep) state of various application components,
   such as dialogs, tab and collapsible panels, which are not a part of field group

   Structure of the store:
   dialogOrComponent:
     dialog:
        [dialogId]: {
            visible
            ownerId
        }
     component:
        [componentId]: savedComponentState
 */

//======================================== Reducer =============================
//======================================== Reducer =============================
//======================================== Reducer =============================


function reducer(state={[DIALOG_KEY]: {}, [COMPONENT_KEY]: {}}, action={}) {
    if (!action.payload || !action.type) return state;
    switch (action.type) {

        case SHOW_DIALOG  :
            return showDialogChange(state,action);

        case HIDE_DIALOG  :
            return hideDialogChange(state,action);

        case HIDE_ALL_DIALOGS  :
            return hideAllDialogsChange(state,action);

        case COMPONENT_STATE_CHANGE  :
            return changeComponentState(state,action);

        default:
            return state;
    }
}


//============ private functions =================================
//============ private functions =================================
//============ private functions =================================

const showDialogChange= function(state,action) {
    var {dialogId,ownerId}= action.payload;
    const dialogsState = state[DIALOG_KEY];

    if (!dialogsState[dialogId] || !dialogsState[dialogId].visible || ownerId!==dialogsState[dialogId].ownerId) {
        state = update(state,
                        {[DIALOG_KEY] :
                            {$merge: {
                                [dialogId]: {
                                    visible: true,
                                    ownerId
                                }
                            }}});
    }
    return state;
};

const hideDialogChange= function(state,action) {
    var {dialogId}= action.payload;
    const dialogsState = state[DIALOG_KEY];

    if (!dialogId ||  !dialogsState[dialogId]) return state;

    if (dialogsState[dialogId] && dialogsState[dialogId].visible) {
        state = update(state,
                        {[DIALOG_KEY] :
                            {$merge: {[dialogId]: {visible: false}}}
                        });
    }
    return state;
};

const hideAllDialogsChange= function(state) {
    const dialogsState = state[DIALOG_KEY];
    const keys= Object.keys(dialogsState);
    if (keys.every( (key) => !dialogsState[key].visible)) return state;
    const newDialogState = keys.reduce( (obj,key) =>
                                 {
                                   obj[key]= {visible: false};
                                   return obj;
                                 },{} );
    return Object.assign({}, state, newDialogState);
};

const changeComponentState=  function(state, action) {
    const {componentId, componentState} = action.payload;
    if (!componentId) {return state;}
    if (!state[COMPONENT_KEY][componentId]) {
        state = update(state,
            {
                [COMPONENT_KEY]: {$merge: {[componentId]: componentState}}
            });
    } else {
        state = update(state,
            {
                [COMPONENT_KEY]: {[componentId]: {$merge: componentState}}
            });
    }
    return state;
};



