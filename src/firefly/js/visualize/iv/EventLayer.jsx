/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {memo, useRef, useEffect} from 'react';
import {func,string,object} from 'prop-types';
import {makeScreenPt} from '../Point.js';
import {MouseState}  from '../VisMouseSync.js';
import {Matrix} from '../../externalSource/transformation-matrix-js/matrix';

const style={left:0,top:0,right:0, bottom:0,position:'absolute'};

function fireEvent(ev,transform, plotId,mouseState, eventCallback, doPreventDefault= true, doStopPropagation= true) {
    if (doPreventDefault) ev.preventDefault();
    if (doStopPropagation) ev.stopPropagation();
    const nativeEvent= ev.nativeEvent ? ev.nativeEvent : ev;
    const {screenX, screenY, offsetX, offsetY}= nativeEvent;
    const trans= Matrix.from(transform).inverse();
    const tmpScreenPt= trans.applyToPoint(offsetX,offsetY);
    const spt= makeScreenPt(tmpScreenPt.x,tmpScreenPt.y);
    eventCallback(plotId,mouseState,spt,screenX,screenY,nativeEvent);
}

function fireDocEvent(element, nativeEv,transform, plotId,mouseState, eventCallback) {
    nativeEv.preventDefault();
    nativeEv.stopPropagation();
    if (!element) return;
    const {screenX, screenY, pageX:x, pageY:y}= nativeEv;
    const {left, top}= element.getBoundingClientRect();
    const compOffX= x-left-window.scrollX;
    const compOffY= y-top-window.scrollY;
    const trans= Matrix.from(transform).inverse();
    const tmpScreenPt= trans.applyToPoint(compOffX, compOffY);
    const spt= makeScreenPt(tmpScreenPt.x,tmpScreenPt.y);
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


export const EventLayer = memo( ({transform,plotId, eventCallback}) => {
    const {current:eRef}= useRef({
        mouseDown:false, element: undefined, mouseMoveDocListener: undefined, mouseUpDocListener: undefined});

    useEffect(() => () => clearDocListeners(eRef) ,[]); // make sure clearDocListeners is when component goes away

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

    const onMouseDown= (ev) => {
        eRef.mouseDown= true;
        fireEvent(ev,transform,plotId,MouseState.DOWN, eventCallback,true,false);
        addDocListeners(eRef,onDocumentMouseMove,onDocumentMouseUp);
    };

    const onMouseMove= (ev) => !eRef.mouseDown && fireEvent(ev,transform,plotId,MouseState.MOVE, eventCallback, true, false);
    const onMouseLeave= (ev) => fireEvent(ev,transform,plotId,MouseState.EXIT, eventCallback);
    const onMouseEnter= (ev) => fireEvent(ev,transform,plotId,MouseState.ENTER, eventCallback);

    const onTouchCancel= (ev) => {
        eRef.mouseDown= false;
        fireEvent(ev,transform,plotId,MouseState.UP, eventCallback,true,false);
    };

    const onTouchEnd= (ev) => {
        eRef.mouseDown= false;
        fireEvent(ev,transform,plotId,MouseState.UP, eventCallback);
    };

    const onTouchMove= (ev) => fireEvent(ev,transform,plotId,MouseState.DRAG, eventCallback);

    const onTouchStart= (ev) => {
        eRef.mouseDown= true;
        fireEvent(ev,transform,plotId,MouseState.DOWN, eventCallback,true,false);
    };

    const onWheel= (ev) => {
        if (!ev.deltaY) return;
        fireEvent(ev,transform,plotId,ev.deltaY>0 ? MouseState.WHEEL_UP : MouseState.WHEEL_DOWN, eventCallback, false);
    };

    useEffect( () => {
        eRef.element?.addEventListener('wheel', onWheel, {passive:false});
        return () => eRef.element?.removeEventListener('wheel', onWheel);
    }, [eRef.element, transform, plotId, eventCallback]);

    return (
        <div {...{className:'event-layer', style, ref:(c) => eRef.element=c,
            onClick, onDoubleClick, onMouseDownCapture:onMouseDown, onMouseEnter, onMouseLeave,
            onMouseMoveCapture:onMouseMove, onTouchCancel, onTouchEnd, onTouchMove, onTouchStart }} />
    );
});

EventLayer.propTypes= {
    eventCallback : func.isRequired,
    plotId : string,
    transform : object.isRequired
};
