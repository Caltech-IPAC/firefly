/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component,PropTypes} from 'react';
import ReactDOM from 'react-dom';
import sCompare from 'react-addons-shallow-compare';
import {makeScreenPt} from '../Point.js';
import {MouseState}  from '../VisMouseCntlr.js';
import {getAbsoluteLeft, getAbsoluteTop} from '../../util/BrowserUtil.js';





export var EventLayer= React.createClass(
{

    mouseDown: false,
    docMouseMoveCallback: null,
    docMouseUpCallback: null,


    propTypes: {
        width : PropTypes.number.isRequired,
        height : PropTypes.number.isRequired,
        eventCallback : PropTypes.func.isRequired,
        plotId : PropTypes.string,
        viewPort : PropTypes.object
    },

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); },

    fireEvent(ev,plotId,viewPort,mouseState) {
        var spt;
        ev.preventDefault();
        ev.stopPropagation();
        var {x:viewPortX,y:viewPortY} = viewPort;
        var {screenX, screenY, offsetX, offsetY}= ev.nativeEvent;
        if (ev.clientX && ev.clientY && offsetX && offsetY) {
            spt= makeScreenPt( viewPortX+offsetX, viewPortY+offsetY);
        }
        this.props.eventCallback(plotId,mouseState,spt,screenX,screenY);
    },

    fireDocEvent(nativeEv,plotId,viewPort,mouseState) {
        var spt;
        nativeEv.preventDefault();
        nativeEv.stopPropagation();
        var {x:viewPortX,y:viewPortY} = viewPort;
        var {screenX, screenY, x, y}= nativeEv;
        var e= ReactDOM.findDOMNode(this);
        var compOffX= x-getAbsoluteLeft(e)+window.scrollX;
        var compOffY= y-getAbsoluteTop(e)+window.scrollY;
        spt= makeScreenPt( viewPortX+compOffX, viewPortY+compOffY);
        this.props.eventCallback(plotId,mouseState,spt,screenX,screenY);
    },





    componentDidMount() {
        this.docMouseMoveCallback= (ev)=> this.onDocumentMouseMove(ev);
        this.docMouseUpCallback= (ev)=> this.onDocumentMouseUp(ev);
    },

    componentWillUnmount() {
        // just in case that they were added
        document.removeEventListener('mousemove', this.docMouseMoveCallback);
        document.removeEventListener('mouseup', this.docMouseUpCallback);
    },



    onClick(ev) {
        this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,MouseState.CLICK);
    },

    onDoubleClick(ev) {
        this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,MouseState.DOUBLE_CLICK);
    },

    onMouseDown(ev) {
        this.mouseDown= true;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,MouseState.DOWN);


        document.addEventListener('mousemove', this.docMouseMoveCallback);
        document.addEventListener('mouseup', this.docMouseUpCallback);

    },

    onDocumentMouseMove(nativeEv) {
        if (this.mouseDown) {
            var {viewPort,plotId}= this.props;
            this.fireDocEvent(nativeEv,plotId,viewPort,MouseState.DRAG);
        }
    },

    onDocumentMouseUp(nativeEv) {
        this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireDocEvent(nativeEv,plotId,viewPort,MouseState.UP);
        document.removeEventListener('mousemove', this.docMouseMoveCallback);
        document.removeEventListener('mouseup', this.docMouseUpCallback);
    },

    onMouseLeave(ev) {
        //this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,MouseState.EXIT);
    },

    onMouseEnter(ev) {
        //this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,MouseState.ENTER);
    },


    onMouseMove(ev) {
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,this.mouseDown?MouseState.DRAG_COMPONENT : MouseState.MOVE);
    },


    onTouchCancel(ev) {
        this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,MouseState.UP);
    },

    onTouchEnd(ev) {
        this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,MouseState.UP);
    },

    onTouchMove(ev) {
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,MouseState.DRAG);
    },

    onTouchStart(ev) {
        this.mouseDown= true;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,MouseState.DOWN);
    },

    render() {
        var {width,height}= this.props;
        var style={left:0,top:0,width,height, position:'absolute'};
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
                 >
            </div>
        );
    }


});

