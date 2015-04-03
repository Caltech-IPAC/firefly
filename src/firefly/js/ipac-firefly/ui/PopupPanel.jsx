/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/

"use strict";

import Enum from "enum";
import React from 'react/addons';
import {getPopupPosition} from './PopupPanelHelper.js';


export const LayoutType= new Enum(["CENTER", "TOP_CENTER", "NONE"]);

export var PopupPanel= React.createClass(
{
    propTypes: {
        layoutPosition : React.PropTypes.object.isRequired,
    },

    onClick: function(ev) {
        this.props.closeCallback();
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
        this.resizeCallback= () =>  {
            updateOffsets();
        };
        var e= React.findDOMNode(this);
        this.updateOffsets();
        //_.defer(function() {
        //    this.computeDir(e);
        //}.bind(this));
        window.addEventListener("resize", this.resizeCallback);
    },



    render: function() {

        var s= {position : "absolute",
            width : "100px",
            height : "100px",
            background : "white",
            visibility : "hidden"
            //left : "40px",
            //right : "170px"
        };
        /*jshint ignore:start */
        return  (
                <div style={s}>
                    {this.props.children}
                    <button type="button" onClick={this.onClick}>close</button>
                </div>
        );
        /*jshint ignore:end */
    }

});

