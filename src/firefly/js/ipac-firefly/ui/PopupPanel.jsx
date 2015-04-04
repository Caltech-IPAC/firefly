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

    resizeCallback : null,


    componentWillUnmount() {
        window.removeEventListener("resize", this.resizeCallback);
    },


    componentDidMount() {
        this.resizeCallback= _.debounce(() => { this.updateOffsets(); },150);
        var e= React.findDOMNode(this);
        this.updateOffsets();
        //_.defer(function() {
        //    this.computeDir(e);
        //}.bind(this));
        window.addEventListener("resize", this.resizeCallback);
    },

    doClose() {
        this.props.closeCallback();
        console.log("close dialog")
    },

    mouseCtx: null,

    dialogMoveStart(ev)  {
        var e= React.findDOMNode(this);
        //this.mouseCtx= humanStart(ev,e);
    },
    dialogMove(ev)  {
        //humanMove(ev,this.mouseCtx);

    },
    dialogMoveEnd(ev)  {
        //this.mouseCtx= humanEnd(ev,this.mouseCtx);
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
                     onMouseDown={this.dialogMoveStart}
                     onMouseMove={this.dialogMove}
                     onMouseUp={this.dialogMoveEnd}
                     onTouchStart={this.dialogMoveStart}
                     onTouchMove={this.dialogMove}
                     onTouchEnd={this.dialogMoveEnd} >
                    <div className={'standard-border'}>
                        <div style={{position:'relative', height:'14px', width:'100%'}}
                             className={'title-bar title-color popup-title-horizontal-background'}>
                            <div style= {{position:'absolute', left:'10px', top:'3px',}}
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

