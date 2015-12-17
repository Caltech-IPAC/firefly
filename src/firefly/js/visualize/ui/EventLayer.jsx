/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import ReactDOM from 'react-dom';
import PureRenderMixin from 'react-addons-pure-render-mixin';
import PlotViewUtil from '../PlotViewUtil.js';
import {makeScreenPt} from '../Point.js';
import VisMouseCntlr  from '../VisMouseCntlr.js';
import {getAbsoluteLeft, getAbsoluteTop} from '../../util/BrowserUtil.js';





var EventLayer= React.createClass(
{

    mouseDown: false,
    docMouseMoveCallback: null,
    docMouseUpCallback: null,

    mixins : [PureRenderMixin],

    propTypes: {
        plotId : React.PropTypes.string.isRequired,
        width : React.PropTypes.number.isRequired,
        height : React.PropTypes.number.isRequired,
        eventCallback : React.PropTypes.func.isRequired
    },

    fireEvent(ev,plotId,mouseState) {
        var spt;
        ev.preventDefault();
        ev.stopPropagation();
        var plot= PlotViewUtil.getPrimaryPlot(plotId);
        var {x:viewPortX,y:viewPortY} = plot.viewPort;
        var {screenX, screenY, offsetX, offsetY}= ev.nativeEvent;
        if (ev.clientX && ev.clientY && offsetX && offsetY) {
            //spt= makeScreenPt( viewPortX+ev.clientX, viewPortY+ev.clientY);
            spt= makeScreenPt( viewPortX+offsetX, viewPortY+offsetY);
        }
        //var e= ReactDOM.findDOMNode(this);
        //var pc= CysConverter.make(plot);
        //VisMouseCntlr.fireMouseEvent(plotId,mouseState, spt);
        //var ip= pc.getImageCoords(spt);
        //var wpt= pc.getWorldCoords(spt);
        //console.log(`fire: ${mouseState.key}: x:${spt.x}, y:${spt.y}, ix:${ip.x}, iy:${ip.y}, wx:${wpt.getLon()}, wy:${wpt.getLat()}`);
        //console.log(`      clientX: ${ev.clientX}, clientY: ${ev.clientY}, screenX: ${ev.screenX}, screenY: ${ev.screenY}`);
        //console.log('fire: '+mouseState.key);
        //console.log(`component: ${mouseState.key} screenX: ${screenX}, screenY: ${screenY}, offsetX: ${offsetX}, offsetY: ${offsetY}`);
        //if (mouseState==VisMouseCntlr.MouseState.DRAG) {
            this.props.eventCallback(plotId,mouseState,spt,screenX,screenY);
        //}
    },

    fireDocEvent(nativeEv,plotId,mouseState) {
        var spt;
        nativeEv.preventDefault();
        nativeEv.stopPropagation();
        var plot= PlotViewUtil.getPrimaryPlot(plotId);
        var {x:viewPortX,y:viewPortY} = plot.viewPort;
        //var {screenX, screenY, offsetX, offsetY}= nativeEv;
        var {screenX, screenY, x, y}= nativeEv;
        if (screenX && screenY) {
            //spt= makeScreenPt( viewPortX+offsetX, viewPortY+offsetY);
        }
        var e= ReactDOM.findDOMNode(this);
        var compOffX= x-getAbsoluteLeft(e)+window.scrollX;
        var compOffY= y-getAbsoluteTop(e)+window.scrollY;
        spt= makeScreenPt( viewPortX+compOffX, viewPortY+compOffY);
        //console.log(`document: ${mouseState.key} screenX: ${screenX}, screenY: ${screenY}, offsetX: ${compOffX}, offsetY: ${compOffY}`);
        //this.props.eventCallback(plotId,mouseState,spt,,);
        this.props.eventCallback(plotId,mouseState,spt,screenX,screenY);
    },





    componentWillMount() {
        this.docMouseMoveCallback= (ev)=> this.onDocumentMouseMove(ev);
        this.docMouseUpCallback= (ev)=> this.onDocumentMouseUp(ev);
    },



    onClick(ev) {
        this.mouseDown= false;
        this.fireEvent(ev,this.props.plotId,VisMouseCntlr.MouseState.CLICK);
    },

    onDoubleClick(ev) {
        this.mouseDown= false;
        this.fireEvent(ev,this.props.plotId,VisMouseCntlr.MouseState.DOUBLE_CLICK);
    },

    onMouseUp() {
        //this.mouseDown= false;
        //this.fireEvent(ev,this.props.plotId,VisMouseCntlr.MouseState.UP);
        //
        //
        //document.removeEventListener('mousemove', this.docMouseMoveCallback);
        //document.removeEventListener('mouseup', this.docMouseUpCallback);

    },

    onMouseDown(ev) {
        this.mouseDown= true;
        this.fireEvent(ev,this.props.plotId,VisMouseCntlr.MouseState.DOWN);


        document.addEventListener('mousemove', this.docMouseMoveCallback);
        document.addEventListener('mouseup', this.docMouseUpCallback);

    },

    onDocumentMouseMove(nativeEv) {
        if (this.mouseDown) {
            this.fireDocEvent(nativeEv,this.props.plotId,VisMouseCntlr.MouseState.DRAG);
        }
    },

    onDocumentMouseUp(nativeEv) {
        this.mouseDown= false;
        this.fireDocEvent(nativeEv,this.props.plotId,VisMouseCntlr.MouseState.UP);
        document.removeEventListener('mousemove', this.docMouseMoveCallback);
        document.removeEventListener('mouseup', this.docMouseUpCallback);
    },




    onMouseOut() { }, //do nothing
    onMouseOver() { },//do nothing

    onMouseLeave(ev) {
        //this.mouseDown= false;
        this.fireEvent(ev,this.props.plotId,VisMouseCntlr.MouseState.EXIT);
    },

    onMouseEnter(ev) {
        //this.mouseDown= false;
        this.fireEvent(ev,this.props.plotId,VisMouseCntlr.MouseState.ENTER);
    },


    onMouseMove(ev) {
        this.fireEvent(ev,this.props.plotId,this.mouseDown?VisMouseCntlr.MouseState.DRAG_COMPONENT : VisMouseCntlr.MouseState.MOVE);
    },


    onTouchCancel(ev) {
        this.mouseDown= false;
        this.fireEvent(ev,this.props.plotId,VisMouseCntlr.MouseState.UP);
    },

    onTouchEnd(ev) {
        this.mouseDown= false;
        this.fireEvent(ev,this.props.plotId,VisMouseCntlr.MouseState.UP);
    },

    onTouchMove(ev) {
        this.fireEvent(ev,this.props.plotId,VisMouseCntlr.MouseState.DRAG);
    },

    onTouchStart(ev) {
        this.mouseDown= true;
        this.fireEvent(ev,this.props.plotId,VisMouseCntlr.MouseState.DOWN);
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
