/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/

"use strict";

import Enum from "enum";
import React from 'react/addons';


const LayoutType= new Enum(["CENTER", "NONE"]);

var PopupPanel= React.createClass(
{
    propTypes: {
        layoutPosition : React.PropTypes.object.isRequired,
    },

    onClick: function(ev) {
        this.props.closeCallback();
    },



    updateOffsets(e) {
        var pos= this.computePosition(this.state.dir);

        var left= window.innerWidth/2 - e.offsetWidth/2;
        var top= window.innerHeight/2 - e.offsetHeight/2;
        e.style.left= left +"px";
        e.style.top= top+"px";
        e.style.visibility="visible";

    },

    resizeCallback : null,


    componentWillUnmount() {
        window.removeEventListener("resize", this.resizeCallback);
    },


    componentDidMount() {
        this.resizeCallback= () =>  {
            var e= React.findDOMNode(this);
            updateOffsets(e);
        };
        var e= React.findDOMNode(this);
        this.updateOffsets(e)
        _.defer(function() {
            this.computeDir(e);
        }.bind(this));
        window.addEventListener("resize", this.resizeCallback);
    },



            render: function() {

                var s= {position : "absolute",
                    width : "100px",
                    height : "100px",
                    background : "white",
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

