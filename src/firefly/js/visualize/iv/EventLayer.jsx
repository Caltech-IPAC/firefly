/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import ReactDOM from 'react-dom';
import sCompare from 'react-addons-shallow-compare';
import {makeScreenPt} from '../Point.js';
import {MouseState}  from '../VisMouseSync.js';
import {Matrix} from 'transformation-matrix-js';

const style={left:0,top:0,right:0, bottom:0,position:'absolute'};

export class EventLayer extends Component {
    constructor(props) {
        super(props);
        this.mouseDown= false;
        this.docMouseMoveCallback= null;
        this.docMouseUpCallback= null;

        this.fireEvent= this.fireEvent.bind(this);
        this.fireDocEvent= this.fireDocEvent.bind(this);
        this.onClick= this.onClick.bind(this);
        this.onDoubleClick= this.onDoubleClick.bind(this);
        this.onMouseDown= this.onMouseDown.bind(this);
        this.onMouseMove= this.onMouseMove.bind(this);
        this.onDocumentMouseMove= this.onDocumentMouseMove.bind(this);
        this.onDocumentMouseUp= this.onDocumentMouseUp.bind(this);
        this.onMouseLeave= this.onMouseLeave.bind(this);
        this.onMouseEnter= this.onMouseEnter.bind(this);
        this.onTouchCancel= this.onTouchCancel.bind(this);
        this.onTouchEnd= this.onTouchEnd.bind(this);
        this.onTouchMove= this.onTouchMove.bind(this);
        this.onTouchStart= this.onTouchStart.bind(this);
    }

    componentDidMount() {
        this.docMouseMoveCallback= (ev)=> this.onDocumentMouseMove(ev);
        this.docMouseUpCallback= (ev)=> this.onDocumentMouseUp(ev);
    }

    componentWillUnmount() {
        // just in case that they were added
        document.removeEventListener('mousemove', this.docMouseMoveCallback);
        document.removeEventListener('mouseup', this.docMouseUpCallback);
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    fireEvent(ev,transform, plotId,mouseState) {
        ev.preventDefault();
        ev.stopPropagation();
        const {screenX, screenY, offsetX, offsetY}= ev.nativeEvent;

        const trans= Matrix.from(transform).inverse();
        const tmpScreenPt= trans.applyToPoint(offsetX,offsetY);
        const spt= makeScreenPt(tmpScreenPt.x,tmpScreenPt.y);
        this.props.eventCallback(plotId,mouseState,spt,screenX,screenY);
    }

    fireDocEvent(nativeEv,transform, plotId,mouseState) {
        nativeEv.preventDefault();
        nativeEv.stopPropagation();
        const {screenX, screenY, pageX:x, pageY:y}= nativeEv;
        const e= ReactDOM.findDOMNode(this);
        const rect = e.getBoundingClientRect();
        const {left, top}= rect;
        const compOffX= x-left;
        const compOffY= y-top;
        const trans= Matrix.from(transform).inverse();
        const tmpScreenPt= trans.applyToPoint(compOffX, compOffY);
        const spt= makeScreenPt(tmpScreenPt.x,tmpScreenPt.y);
        this.props.eventCallback(plotId,mouseState,spt,screenX,screenY);
    }




    onClick(ev) {
        this.mouseDown= false;
        const {transform,plotId}= this.props;
        this.fireEvent(ev,transform,plotId,MouseState.CLICK);
    }

    onDoubleClick(ev) {
        this.mouseDown= false;
        const {transform,plotId}= this.props;
        this.fireEvent(ev,transform,plotId,MouseState.DOUBLE_CLICK);
    }

    onMouseDown(ev) {
        this.mouseDown= true;
        const {transform,plotId}= this.props;
        this.fireEvent(ev,transform,plotId,MouseState.DOWN);


        document.addEventListener('mousemove', this.docMouseMoveCallback);
        document.addEventListener('mouseup', this.docMouseUpCallback);

    }


    onMouseMove(ev) {
        if (!this.mouseDown) {
            const {transform,plotId}= this.props;
            this.fireEvent(ev,transform,plotId,this.mouseDown?MouseState.DRAG_COMPONENT : MouseState.MOVE);
        }
    }

    onDocumentMouseMove(nativeEv) {
        if (this.mouseDown) {
            const {transform,plotId}= this.props;
            this.fireDocEvent(nativeEv,transform,plotId,MouseState.DRAG);
        }
    }

    onDocumentMouseUp(nativeEv) {
        this.mouseDown= false;
        const {transform,plotId}= this.props;
        this.fireDocEvent(nativeEv,transform,plotId,MouseState.UP);
        document.removeEventListener('mousemove', this.docMouseMoveCallback);
        document.removeEventListener('mouseup', this.docMouseUpCallback);
    }

    onMouseLeave(ev) {
        const {transform,plotId}= this.props;
        this.fireEvent(ev,transform,plotId,MouseState.EXIT);
    }

    onMouseEnter(ev) {
        const {transform,plotId}= this.props;
        this.fireEvent(ev,transform,plotId,MouseState.ENTER);
    }



    onTouchCancel(ev) {
        this.mouseDown= false;
        const {transform,plotId}= this.props;
        this.fireEvent(ev,transform,plotId,MouseState.UP);
    }

    onTouchEnd(ev) {
        this.mouseDown= false;
        const {transform,plotId}= this.props;
        this.fireEvent(ev,transform,plotId,MouseState.UP);
    }

    onTouchMove(ev) {
        const {transform,plotId}= this.props;
        this.fireEvent(ev,transform,plotId,MouseState.DRAG);
    }

    onTouchStart(ev) {
        this.mouseDown= true;
        const {transform,plotId}= this.props;
        this.fireEvent(ev,transform,plotId,MouseState.DOWN);
    }


    render() {
        return (
            <div className='event-layer'
                 style={style}
                 onClick={this.onClick}
                 onDoubleClick={this.onDoubleClick}
                 onMouseDownCapture={this.onMouseDown}
                 onMouseEnter={this.onMouseEnter}
                 onMouseLeave={this.onMouseLeave}
                 onMouseMoveCapture={this.onMouseMove}
                 onTouchCancel={this.onTouchCancel}
                 onTouchEnd={this.onTouchEnd}
                 onTouchMove={this.onTouchMove}
                 onTouchStart={this.onTouchStart}
            />
        );
    }


}



EventLayer.propTypes= {
    eventCallback : PropTypes.func.isRequired,
    plotId : PropTypes.string,
    transform : PropTypes.object.isRequired
};


