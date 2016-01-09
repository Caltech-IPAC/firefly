/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import ReactDOM from 'react-dom';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import {makeScreenPt} from '../Point.js';
import VisMouseCntlr  from '../VisMouseCntlr.js';
import {getAbsoluteLeft, getAbsoluteTop} from '../../util/BrowserUtil.js';





export var EventLayer= React.createClass(
{

    mouseDown: false,
    docMouseMoveCallback: null,
    docMouseUpCallback: null,

    mixins : [PureRenderMixin],

    propTypes: {
        width : React.PropTypes.number.isRequired,
        height : React.PropTypes.number.isRequired,
        eventCallback : React.PropTypes.func.isRequired,
        plotId : React.PropTypes.string,
        viewPort : React.PropTypes.object
    },

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





    componentWillMount() {
        this.docMouseMoveCallback= (ev)=> this.onDocumentMouseMove(ev);
        this.docMouseUpCallback= (ev)=> this.onDocumentMouseUp(ev);
    },



    onClick(ev) {
        this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,VisMouseCntlr.MouseState.CLICK);
    },

    onDoubleClick(ev) {
        this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,VisMouseCntlr.MouseState.DOUBLE_CLICK);
    },

    onMouseUp() {
    },

    onMouseDown(ev) {
        this.mouseDown= true;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,VisMouseCntlr.MouseState.DOWN);


        document.addEventListener('mousemove', this.docMouseMoveCallback);
        document.addEventListener('mouseup', this.docMouseUpCallback);

    },

    onDocumentMouseMove(nativeEv) {
        if (this.mouseDown) {
            var {viewPort,plotId}= this.props;
            this.fireDocEvent(nativeEv,plotId,viewPort,VisMouseCntlr.MouseState.DRAG);
        }
    },

    onDocumentMouseUp(nativeEv) {
        this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireDocEvent(nativeEv,plotId,viewPort,VisMouseCntlr.MouseState.UP);
        document.removeEventListener('mousemove', this.docMouseMoveCallback);
        document.removeEventListener('mouseup', this.docMouseUpCallback);
    },




    onMouseOut() { }, //do nothing
    onMouseOver() { },//do nothing

    onMouseLeave(ev) {
        //this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,VisMouseCntlr.MouseState.EXIT);
    },

    onMouseEnter(ev) {
        //this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,VisMouseCntlr.MouseState.ENTER);
    },


    onMouseMove(ev) {
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,this.mouseDown?VisMouseCntlr.MouseState.DRAG_COMPONENT : VisMouseCntlr.MouseState.MOVE);
    },


    onTouchCancel(ev) {
        this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,VisMouseCntlr.MouseState.UP);
    },

    onTouchEnd(ev) {
        this.mouseDown= false;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,VisMouseCntlr.MouseState.UP);
    },

    onTouchMove(ev) {
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,VisMouseCntlr.MouseState.DRAG);
    },

    onTouchStart(ev) {
        this.mouseDown= true;
        var {viewPort,plotId}= this.props;
        this.fireEvent(ev,plotId,viewPort,VisMouseCntlr.MouseState.DOWN);
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
                 onMouseOut={this.onMouseOut}
                 onMouseOver={this.onMouseOver}
                 onMouseUpCapture={this.onMouseUp}
                 onTouchCancel={this.onTouchCancel}
                 onTouchEnd={this.onTouchEnd}
                 onTouchMove={this.onTouchMove}
                 onTouchStart={this.onTouchStart}
                 >
            </div>
        );
    }


});

export default EventLayer;
