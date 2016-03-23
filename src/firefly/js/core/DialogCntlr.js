/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {get} from 'lodash';
export const DIALOG_KEY = 'dialog';

/*---------------------------- ACTIONS -----------------------------*/

const SHOW_DIALOG = `DialogCntlr.showDialog`;
const HIDE_DIALOG = `DialogCntlr.hideDialog`;
const HIDE_ALL_DIALOGS = `DialogCntlr.hideAllDialogs`;



export default {
    SHOW_DIALOG,
    HIDE_DIALOG,
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


//============================ Utilities ============================
//============================ Utilities ============================
//============================ Utilities ============================

export function isDialogVisible(dialogKey) {
    return get(flux.getState()[DIALOG_KEY], [dialogKey,'visible'],false);
}

export function getDialogOwner(dialogKey) {
    const d= get(flux.getState()[DIALOG_KEY], dialogKey, {});
    return (d.visible && d.ownerId) ? d.ownerId : null;
}

//======================================== Reducer =============================
//======================================== Reducer =============================
//======================================== Reducer =============================


function reducer(state={}, action={}) {
    if (!action.payload || !action.type) return state;
    switch (action.type) {

        case SHOW_DIALOG  :
            return showDialogChange(state,action);

        case HIDE_DIALOG  :
            return hideDialogChange(state,action);

        case HIDE_ALL_DIALOGS  :
            return hideAllDialogsChange(state,action);

        default:
            return state;
    }
}


//============ private functions =================================
//============ private functions =================================
//============ private functions =================================

const showDialogChange= function(state,action) {
    var {dialogId,ownerId}= action.payload;

    if (!state[dialogId] || !state[dialogId].visible || ownerId!==state[dialogId].ownerId) {
        state= Object.assign({},state, {[dialogId]: {visible: true, ownerId}});
    }
    return state
};

const hideDialogChange= function(state,action) {
    var {dialogId}= action.payload;
    if (!dialogId ||  !state[dialogId]) return state;

    if (state[dialogId] && state[dialogId].visible) {
        state= Object.assign({},state, {[dialogId]: {visible: false}});
    }
    return state;
};

const hideAllDialogsChange= function(state) {
    const keys= Object.keys(state);
    if (keys.every( (key) => !state[key].visible)) return state;
    return keys.reduce( (obj,key) =>
                             {
                               obj[key]= {visible:false};
                               return obj;
                             },{} );
};


