/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/

"use strict";

import {getRootURL, getAbsoluteLeft, getAbsoluteTop} from 'ipac-firefly/util/BrowserUtil.js';
import _ from 'underscore';
import Enum from 'enum';
import React from 'react/addons';
import {getPopupPosition, humanStart, humanMove, humanStop } from './PopupPanelHelper.js';


export const LayoutType= new Enum(['CENTER', 'TOP_CENTER', 'NONE']);

export var PopupPanel= React.createClass(
{


    resizeCallback : null,
    mouseCtx: null,
    titleBarRef: null,
    moveCallback : null,
    buttonUpCallback : null,

    propTypes: {
        layoutPosition : React.PropTypes.object.isRequired,
        title : React.PropTypes.string
    },

    onClick: function(ev) {
        this.doClose();
    },



    updateOffsets() {
        var e= React.findDOMNode(this);

        var results= getPopupPosition(e,LayoutType.TOP_CENTER);
        e.style.left= results.left;
        e.style.top= results.top;
        e.style.visibility="visible";

    },




    componentWillUnmount() {
        window.removeEventListener("resize", this.resizeCallback);
        document.removeEventListener("mousemove", this.moveCallback);
        document.removeEventListener("mouseup", this.buttonUpCallback);
    },


    componentDidMount() {
        this.moveCallback= (ev)=> this.dialogMove(ev)
        this.buttonUpCallback= (ev)=> this.dialogMoveEnd(ev)
        this.resizeCallback= _.debounce(() => { this.updateOffsets(); },150);
        var e= React.findDOMNode(this);
        this.updateOffsets();
        //_.defer(function() {
        //    this.computeDir(e);
        //}.bind(this));
        window.addEventListener("resize", this.resizeCallback);
        document.addEventListener("mousemove", this.moveCallback);
        document.addEventListener("mouseup", this.buttonUpCallback);
    },

    doClose() {
        this.props.closeCallback();
        console.log("close dialog")
    },

    dialogMoveStart(ev)  {
        var e= React.findDOMNode(this);
        var titleBar= React.findDOMNode(this.titleBarRef);
        this.mouseCtx= humanStart(ev,e,titleBar);
    },

    dialogMove(ev)  {
        var titleBar= React.findDOMNode(this.titleBarRef);
        humanMove(ev,this.mouseCtx,titleBar);
    },

    dialogMoveEnd(ev)  {
        this.mouseCtx= humanStop(ev,this.mouseCtx);
        this.mouseCtx= null;
    },


    renderAsTopHeader() {

        var s= {position : 'absolute',
            //width : "100px",
            //height : "100px",
            //background : "white",
            visibility : 'hidden'
            //left : "40px",
            //right : "170px"
        };



        var title= this.props.title||"";
        return (
                <div style={s} className={'popup-pane-shadow'}
                     onMouseDownCapture={this.dialogMoveStart}
                     onTouchStart={this.dialogMoveStart}
                     onTouchMove={this.dialogMove}
                     onTouchEnd={this.dialogMoveEnd} >
                    <div className={'standard-border'}>
                        <div style={{position:'relative', height:'14px', width:'100%'}}
                             className={'title-bar title-color popup-title-horizontal-background'}>
                            <div ref={(c) => this.titleBarRef=c}
                                 style= {{position:'absolute', left:'0px', top:'0px',width:'100%', padding: '3px 0 3px 10px'}}
                                 onMouseDownCapture={this.dialogMoveStart}
                                 onTouchStart={this.dialogMoveStart}
                                 onTouchMove={this.dialogMove}
                                 onTouchEnd={this.dialogMoveEnd}
                                 className={'title-label'} >
                                {title}
                            </div>
                            <image className={'popup-header'}
                                   src= {getRootURL()+'images/blue_delete_10x10.gif'}
                                   style= {{position:'absolute', right:'0px', top:'0px'}}
                                   onClick={this.doClose} />

                        </div>
                        <div style={{display:"table"}}>
                            {this.props.children}
                            <button type="button" onClick={this.onClick}>close</button>
                        </div>
                    </div>
                </div>

        );

    },


    render() {

        /*jshint ignore:start */
        return  this.renderAsTopHeader();
        /*jshint ignore:end */
    }

});

