


import Enum from 'enum';
import {flux} from '../Firefly.js';
import {getPlotViewById} from './PlotViewUtil.js';
import {visRoot} from './ImagePlotCntlr.js';
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

const VIS_MOUSE_KEY= 'visMouseState';


export const MouseState= new Enum(['NONE', 'ENTER', 'EXIT', 'DOWN', 'UP',
                                   'DRAG_COMPONENT', 'DRAG', 'MOVE', 'CLICK',
                                   'DOUBLE_CLICK']);

export function currMouseState() { return flux.getState()[VIS_MOUSE_KEY]; }

//============ EXPORTS ===========
//============ EXPORTS ===========

export default {reducer, MouseState, VIS_MOUSE_KEY, MOUSE_STATE_CHANGE
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
        default:
            break;
    }
    return retState;
}


function processMouseStateChange(state,action) {

    var {plotId, mouseState, modifiers, screenPt, imagePt, worldPt}= action.payload;
    return Object.assign({}, state, {plotId, mouseState, modifiers, screenPt, imagePt, worldPt});
}




//============ Action Creators ===========
//============ Action Creators ===========
//============ Action Creators ===========
export function dispatchMouseStateChange(payload) {
    flux.process({type: MOUSE_STATE_CHANGE, payload});
}




/**
 *
 * @param {string} plotId
 * @param {Enum} mouseState
 * @param {Object} screenPt
 * @param {number} screenX
 * @param {number} screenY
 * @param shiftDown
 * @param controlDown
 * @param metaDown
 * @return {{plotId: string, mouseState: Enum, screenPt: object, imagePt: object, worldPt: object, screenX: number, screenY: number}}
 */
export function makeMouseStatePayload(plotId,mouseState,screenPt,screenX,screenY,
                               {shiftDown,controlDown,metaDown}= {}) {
    var payload={mouseState,screenPt,screenX,screenY, shiftDown,controlDown,metaDown};
    if (plotId) {
        var pv= getPlotViewById(visRoot(),plotId);
        if (pv && pv.primaryPlot) {
            payload.plotId= plotId;
            var cc= CsysConverter.make(pv.primaryPlot);
            payload.imagePt= cc.getImageCoords(screenPt);
            payload.worldPt= cc.getWorldCoords(screenPt);
        }

    }
    return payload;
}
//======================================== Private ======================================
//======================================== Private ======================================
//======================================== Private ======================================


const initState= function() {

    return {
        mouseState : MouseState.NONE,
        plotId : null,
        screenPt : null,
        imagePt : null,
        worldPt : null,
        modifiers: {}
    };

};


