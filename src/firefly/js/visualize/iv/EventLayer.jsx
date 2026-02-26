/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {memo, useRef, useEffect} from 'react';
import {func,string,object} from 'prop-types';
import {checkProps} from '../../ui/SimpleComponent';
import {CCUtil} from '../CsysConverter';
import {visRoot} from '../ImagePlotCntlr';
import {primePlot} from '../PlotViewUtil';
import {makeScreenPt} from '../Point.js';
import {MouseState}  from '../VisMouseSync.js';
import {Matrix} from '../../externalSource/transformation-matrix-js/matrix';
import {computeSimpleDistance} from '../VisUtil';
import {isHiPS} from '../WebPlot';


function fireEvent(inEvent, transform, plotId, mouseState, eventCallback, doPreventDefault= true, doStopPropagation= true) {
    if (doPreventDefault) inEvent.preventDefault();
    if (doStopPropagation) inEvent.stopPropagation();
    const ev= inEvent.nativeEvent ?? inEvent;
    const {screenX, screenY}= ev.touches?.[0]
        ?ev.touches?.[0]
        :ev;
    let {offsetX, offsetY}= ev;
    if (ev.touches?.[0]) {
        const rect= ev.target.getBoundingClientRect();
        offsetX = ev.touches?.[0].clientX - rect.left;
        offsetY = ev.touches?.[0].clientY - rect.top;
    }

    const spt= createScreenPt(transform,offsetX,offsetY);
    eventCallback(plotId,mouseState,spt,screenX,screenY,ev);
}


function firePinchEvent(inEvent, transform, plotId, originalCenterPt, mouseState, eventCallback) {
    const ev= inEvent.nativeEvent ?? inEvent;
    ev.preventDefault();
    ev.stopPropagation();
    const plot= primePlot(visRoot(), plotId);
    const spt= CCUtil.getScreenCoords(plot,originalCenterPt);
    eventCallback(plotId,mouseState,spt,spt.x,spt.y,ev);
}

function createScreenPt(transform,x,y) {
    const trans= Matrix.from(transform).inverse();
    const tmpScreenPt= trans.applyToPoint(x,y);
    return makeScreenPt(tmpScreenPt.x,tmpScreenPt.y);
}



function fireDocEvent(element, nativeEv,transform, plotId,mouseState, eventCallback) {
    nativeEv.preventDefault();
    nativeEv.stopPropagation();
    if (!element) return;
    const {screenX, screenY, pageX:x, pageY:y}= nativeEv;
    const {left, top}= element.getBoundingClientRect();
    const compOffX= x-left-window.scrollX;
    const compOffY= y-top-window.scrollY;

    const spt= createScreenPt(transform,compOffX,compOffY);
    eventCallback(plotId,mouseState,spt,screenX,screenY,nativeEv);
}

function clearDocListeners(eRef) {
    eRef.mouseMoveDocListener && document.removeEventListener('mousemove', eRef.mouseMoveDocListener);
    eRef.mouseUpDocListener && document.removeEventListener('mouseup', eRef.mouseUpDocListener);
    eRef.mouseMoveDocListener= undefined;
    eRef.mouseUpDocListener= undefined;
}

function addDocListeners (eRef,onDocumentMouseMove,onDocumentMouseUp) {
    eRef.mouseMoveDocListener= onDocumentMouseMove;
    eRef.mouseUpDocListener= onDocumentMouseUp;
    document.addEventListener('mousemove', eRef.mouseMoveDocListener);
    document.addEventListener('mouseup', eRef.mouseUpDocListener);
}

const screenPtFromTouch= (touchEntry) => makeScreenPt(touchEntry.clientX,touchEntry.clientY);

function makeTouchCenterPt(ev,transform,plotId) {
    if (ev.touches.length!==2) return;
    const touchPt0= screenPtFromTouch(ev.touches[0]);
    const touchPt1= screenPtFromTouch(ev.touches[1]);
    const x= (touchPt0.x + touchPt1.x) /2;
    const y= (touchPt0.y + touchPt1.y) /2;
    const rect= ev.target.getBoundingClientRect();
    const offsetX= x - rect.left;
    const offsetY= y - rect.top;
    const spt= createScreenPt(transform,offsetX,offsetY);
    const plot= primePlot(visRoot(), plotId);
    return {touchPt0, touchPt1,
        centerPt: isHiPS(plot) ? CCUtil.getWorldCoords(plot,spt) : CCUtil.getImageCoords(plot,spt)
    };
}

function startTouch(ev,transform,plotId) {
    const {touchPt0, touchPt1, centerPt:originalCenterPt}= makeTouchCenterPt(ev,transform,plotId);
    return { touchPt0, touchPt1, originalCenterPt};
}

function getTouchPinchDir(ev, touchPt0,touchPt1,moveTouchPt0, moveTouchPt1) {
    if (ev.touches.length!==2) return;
    const distMove= computeSimpleDistance(moveTouchPt0, moveTouchPt1);
    const distStart= computeSimpleDistance(touchPt0, touchPt1);
    const change= distMove - distStart;
    if (Math.abs(change)>.01) {
        return change < 0 ? MouseState.PINCH_IN : MouseState.PINCH_OUT;
    }
}



export const EventLayer = memo( (props) => {
    checkProps(props,EventLayer);
    const {transform, plotId, eventCallback}= props;
    const {current:eRef}= useRef({
        mouseDown:false, touch: undefined, element: undefined,
        mouseMoveDocListener: undefined, mouseUpDocListener: undefined,
    });

         //clear Doc Listeners is when component goes away
    useEffect(() => () => clearDocListeners(eRef) ,[]); // eslint-disable-line react-hooks/exhaustive-deps

    const onDocumentMouseMove= (nativeEv) =>
        eRef.mouseDown && fireDocEvent(eRef.element, nativeEv,transform,plotId,MouseState.DRAG, eventCallback);

    const onDocumentMouseUp= (nativeEv) => {
        eRef.mouseDown= false;
        clearDocListeners(eRef);
        fireDocEvent(eRef.element, nativeEv,transform,plotId,MouseState.UP, eventCallback);
    };

    const onClick= (ev) => {
        eRef.mouseDown= false;
        fireEvent(ev,transform,plotId,MouseState.CLICK, eventCallback);
    };

    const onDoubleClick= (ev) => {
        eRef.mouseDown= false;
        fireEvent(ev,transform,plotId,MouseState.DOUBLE_CLICK, eventCallback);
    };

    const onMouseDownCapture= (ev) => {
        eRef.mouseDown= true;
        fireEvent(ev,transform,plotId,MouseState.DOWN, eventCallback,true,false);
        addDocListeners(eRef,onDocumentMouseMove,onDocumentMouseUp);
    };

    const onMouseMoveCapture= (ev) => !eRef.mouseDown && fireEvent(ev,transform,plotId,MouseState.MOVE, eventCallback, true, false);
    const onMouseLeave= (ev) => fireEvent(ev,transform,plotId,MouseState.EXIT, eventCallback);
    const onMouseEnter= (ev) => fireEvent(ev,transform,plotId,MouseState.ENTER, eventCallback);


    useEffect( () => {

        if (!eRef?.element) return;

        const onWheel= (ev) => {
            if (!ev.deltaY) return;
            fireEvent(ev,transform,plotId,ev.deltaY>0 ? MouseState.WHEEL_UP : MouseState.WHEEL_DOWN, eventCallback, false);
        };
        const onTouchCancelOrEnd= (ev) => {
            eRef.mouseDown= false;
            eRef.touch= undefined;
            fireEvent(ev,transform,plotId,MouseState.UP, eventCallback);
        };

        const onTouchStart= (ev) => {
            if (ev.touches.length===2) {
                eRef.touch= startTouch(ev,transform,plotId);
            }
            else if (ev.touches.length===1){
                eRef.mouseDown= true;
                eRef.touch=undefined;
                fireEvent(ev,transform,plotId,MouseState.DOWN, eventCallback);
            }
        };
        
        const onTouchMove= (ev) => {
            if (ev.touches.length===2) {
                const {touchPt0, touchPt1, originalCenterPt } = eRef.touch ?? {};
                if (!touchPt0 || !touchPt1) {
                    eRef.touch= startTouch(ev,transform,plotId);
                    return;
                }
                const moveTouchPt0= screenPtFromTouch(ev.touches[0]);
                const moveTouchPt1= screenPtFromTouch(ev.touches[1]);
                eRef.touch= { touchPt0: moveTouchPt0, touchPt1: moveTouchPt1, originalCenterPt };
                const ms= getTouchPinchDir(ev,touchPt0,touchPt1, moveTouchPt0, moveTouchPt1);
                if (ms) firePinchEvent(ev,transform,plotId,originalCenterPt, ms,eventCallback);
            } else if (ev.touches.length===1){
                fireEvent(ev,transform,plotId,MouseState.DRAG, eventCallback);
                eRef.touch=undefined;
            }
        };

        const nonPassiveEvents= [
            ['wheel', onWheel], ['touchmove', onTouchMove], ['touchstart', onTouchStart],
            ['touchend', onTouchCancelOrEnd], ['touchcancel', onTouchCancelOrEnd],
        ];

        nonPassiveEvents.forEach( ([ev,cb]) => eRef?.element?.addEventListener(ev, cb, {passive:false}));
        return () => {
            nonPassiveEvents.forEach( ([ev,cb]) => eRef?.element?.removeEventListener(ev, cb));
        };
    }, [transform, plotId, eventCallback]); // eslint-disable-line react-hooks/exhaustive-deps

    return (
        <div {...{
            className:'event-layer',
            style: {left:0,top:0,right:0, bottom:0,position:'absolute'},
            ref:(c) => eRef.element=c,
            onClick, onDoubleClick, onMouseDownCapture, onMouseEnter, onMouseLeave, onMouseMoveCapture
        } }/> );
});

EventLayer.propTypes= {
    eventCallback : func.isRequired,
    plotId : string,
    transform : object.isRequired
};