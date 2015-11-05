


import Enum from 'enum';
import {flux} from '../Firefly.js';
import PlotView from './PlotView.js';
import CsysConverter from './CsysConverter.js';
import {makeScreenPt} from './Point.js';


/**
 * action should contain:
 * {string} plotId
 * {MouseState} mouseState
 * modifiers
 * currViewPortPt
 * currScreenPt
 * currImagePt
 * currWorldPt
 *
 * @type {string}
 */
const MOUSE_STATE_CHANGE= 'VisMouseCntlr/MouseStateChange';


const ADD_MOUSE_GRAB= 'VisMouseCntlr/AddMouseGrab';
const REMOVE_MOUSE_GRAB= 'VisMouseCntlr/AddMouseGrab';
const ADD_MOUSE_PERSISTENT= 'VisMouseCntlr/AddMousePersistent';
const REMOVE_MOUSE_PERSISTENT= 'VisMouseCntlr/RemoveMousePersistent';

const VIS_MOUSE_KEY= 'visMouseState';


const MouseState= new Enum(['NONE', 'ENTER', 'EXIT', 'DOWN', 'UP', 'DRAG_COMPONENT', 'DRAG', 'MOVE', 'CLICK','DOUBLE_CLICK']);

//============ EXPORTS ===========
//============ EXPORTS ===========

export default {reducer, MouseState, fireMouseEvent,
                VIS_MOUSE_KEY,
                MOUSE_STATE_CHANGE, ADD_MOUSE_GRAB,
                REMOVE_MOUSE_GRAB, ADD_MOUSE_PERSISTENT,
                REMOVE_MOUSE_PERSISTENT
};


//============ EXPORTS ===========
//============ EXPORTS ===========





//======================================== Exported Functions =============================
//======================================== Exported Functions =============================
//======================================== Exported Functions =============================



function reducer(state=initState(), action={}) {

    if (!action.payload || !action.type) return state;

    var retState= state;
    switch (action.type) {
        case MOUSE_STATE_CHANGE:
            retState= processMouseStateChange(state,action);
            break;
        case ADD_MOUSE_GRAB:
        case REMOVE_MOUSE_GRAB:
        case ADD_MOUSE_PERSISTENT:
        case REMOVE_MOUSE_PERSISTENT:
            retState= processAddRemove(state,action);
            break;
        default:
            break;
    }
    return retState;
}


function processMouseStateChange(state,action) {

    var {plotId, mouseState, modifiers, currWorldSpacePt,
        currScreenPt, currImagePt, currWorldPt}= action;
    return Object.assign({}, state, {plotId, mouseState, modifiers, currWorldSpacePt,
                                     currScreenPt, currImagePt, currWorldPt});
}


function processAddRemove(state,action) {

    var persistentMouseActionAry;
    var grabMouseActionStack ;
    var {actionConst}= action.payload;
    var retState= state;
    switch (action.type) {
        case ADD_MOUSE_GRAB:
            grabMouseActionStack= [actionConst, ...state.grabMouseActionStack];
            retState= Object.assign({}, state,grabMouseActionStack);
            break;
        case REMOVE_MOUSE_GRAB:
            grabMouseActionStack= state.grabMouseActionStack.filter( (a) => actionConst!=a);
            retState= Object.assign({}, state,grabMouseActionStack);
            //todo
            break;
        case ADD_MOUSE_PERSISTENT:
            persistentMouseActionAry = [...state.persistentMouseActionAry, actionConst];
            retState=  Object.assign({}, state,persistentMouseActionAry);
            break;
        case REMOVE_MOUSE_PERSISTENT:
            persistentMouseActionAry= state.persistentMouseActionAry.filter( (a) => actionConst!=a);
            retState=  Object.assign({}, state,persistentMouseActionAry);
            //todo
            break;
    }
    return retState;

}





//============ Action Creators ===========
//============ Action Creators ===========
//============ Action Creators ===========

function fireMouseEvent(plotId, mouseState, currScreenPt) {

    var plot=PlotView.getPrimaryPlot(plotId);
    var currViewPortPt;
    var currImagePt;
    var currWorldPt;

    if (currScreenPt && plot) {
        var cc= CsysConverter.make(plot);
        currViewPortPt= cc.getViewPortCoords(currScreenPt);
        currImagePt= cc.getImageCoords(currScreenPt);
        currWorldPt= cc.getWorldCoords(currImagePt);
    }

    flux.process({type: MOUSE_STATE_CHANGE,
                  payload: {plotId, mouseState, currScreenPt, currViewPortPt,
                            currImagePt, currWorldPt}});

}


function fireAddMousePersistentAction(actionConst) {
    flux.process({type: ADD_MOUSE_PERSISTENT,
        payload: {actionConst}});

}

function fireRemoveMousePersistentAction(actionConst) {
    flux.process({type: REMOVE_MOUSE_PERSISTENT,
        payload: {actionConst}});

}


function fireAddMouseGrabAction(actionConst) {
    flux.process({type: ADD_MOUSE_GRAB,
        payload: {actionConst}});

}

function fireRemoveMouseGrabAction(actionConst) {
    flux.process({type: REMOVE_MOUSE_GRAB,
        payload: {actionConst}});

}


//======================================== Private ======================================
//======================================== Private ======================================
//======================================== Private ======================================


const initState= function() {

    return {
        currMouseState : MouseState.NONE,
        currPlotId : null,
        currWorldSpacePt : null,
        currScreenPt : null,
        currImagePt : null,
        currWorldPt : null,
        persistentMouseActionAry : [],
        grabMouseActionStack : []
    };

};


