/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {memo, useRef, useEffect} from 'react';
import PropTypes from 'prop-types';
import {makeScreenPt} from '../Point.js';
import {MouseState}  from '../VisMouseSync.js';
import {Matrix} from '../../externalSource/transformation-matrix-js/matrix';

const style={left:0,top:0,right:0, bottom:0,position:'absolute'};

function fireEvent(ev,transform, plotId,mouseState, eventCallback, doPreventDefault= true) {
    if (doPreventDefault) ev.preventDefault();
    ev.stopPropagation();
    const {screenX, screenY, offsetX, offsetY}= ev.nativeEvent;
    const trans= Matrix.from(transform).inverse();
    const tmpScreenPt= trans.applyToPoint(offsetX,offsetY);
    const spt= makeScreenPt(tmpScreenPt.x,tmpScreenPt.y);
    eventCallback(plotId,mouseState,spt,screenX,screenY);
}

function fireDocEvent(element, nativeEv,transform, plotId,mouseState, eventCallback) {
    nativeEv.preventDefault();
    nativeEv.stopPropagation();
    const {screenX, screenY, pageX:x, pageY:y}= nativeEv;
    const {left, top}= element.getBoundingClientRect();
    const compOffX= x-left;
    const compOffY= y-top;
    const trans= Matrix.from(transform).inverse();
    const tmpScreenPt= trans.applyToPoint(compOffX, compOffY);
    const spt= makeScreenPt(tmpScreenPt.x,tmpScreenPt.y);
    eventCallback(plotId,mouseState,spt,screenX,screenY);
}

export const EventLayer = memo( ({transform,plotId, eventCallback}) => {
    const {current:eRef}= useRef({
        mouseDown:false, element: undefined, mouseMoveDocListener: undefined, mouseUpDocListener: undefined});

    const clearDocListeners= () =>{
        eRef.mouseMoveDocListener && document.removeEventListener('mousemove', eRef.mouseMoveDocListener);
        eRef.mouseUpDocListener && document.removeEventListener('mouseup', eRef.mouseUpDocListener);
        eRef.mouseMoveDocListener= undefined;
        eRef.mouseUpDocListener= undefined;
    };

    const addDocListeners= () =>{
        eRef.mouseMoveDocListener= onDocumentMouseMove;
        eRef.mouseUpDocListener= onDocumentMouseUp;
        document.addEventListener('mousemove', eRef.mouseMoveDocListener);
        document.addEventListener('mouseup', eRef.mouseUpDocListener);
    };

    useEffect(() => clearDocListeners ,[]);

    const onDocumentMouseMove= (nativeEv) =>
        eRef.mouseDown && fireDocEvent(eRef.element, nativeEv,transform,plotId,MouseState.DRAG, eventCallback);

    const onDocumentMouseUp= (nativeEv) => {
        eRef.mouseDown= false;
        clearDocListeners();
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
        fireEvent(ev,transform,plotId,MouseState.DOWN, eventCallback);
        addDocListeners();
    };

    const onMouseMove= (ev) => !eRef.mouseDown && fireEvent(ev,transform,plotId,MouseState.MOVE, eventCallback);
    const onMouseLeave= (ev) => fireEvent(ev,transform,plotId,MouseState.EXIT, eventCallback);
    const onMouseEnter= (ev) => fireEvent(ev,transform,plotId,MouseState.ENTER, eventCallback);

    const onTouchCancel= (ev) => {
        eRef.mouseDown= false;
        fireEvent(ev,transform,plotId,MouseState.UP, eventCallback);
    };

    const onTouchEnd= (ev) => {
        eRef.mouseDown= false;
        fireEvent(ev,transform,plotId,MouseState.UP, eventCallback);
    };

    const onTouchMove= (ev) => fireEvent(ev,transform,plotId,MouseState.DRAG, eventCallback);

    const onTouchStart= (ev) => {
        eRef.mouseDown= true;
        fireEvent(ev,transform,plotId,MouseState.DOWN, eventCallback);
    };

    const onWheel= (ev) => {
        if (!ev.deltaY) return;
        fireEvent(ev,transform,plotId,ev.deltaY>0 ? MouseState.WHEEL_UP : MouseState.WHEEL_DOWN, eventCallback, false);
    };

    return (
        <div className='event-layer' style={style} ref={(c) => eRef.element=c}
             onClick={onClick} onDoubleClick={onDoubleClick} onMouseDownCapture={onMouseDown}
             onMouseEnter={onMouseEnter} onMouseLeave={onMouseLeave}
             onMouseMoveCapture={onMouseMove} onTouchCancel={onTouchCancel}
             onTouchEnd={onTouchEnd} onTouchMove={onTouchMove} onTouchStart={onTouchStart} onWheel={onWheel}
        />
    );
});

EventLayer.propTypes= {
    eventCallback : PropTypes.func.isRequired,
    plotId : PropTypes.string,
    transform : PropTypes.object.isRequired
};
