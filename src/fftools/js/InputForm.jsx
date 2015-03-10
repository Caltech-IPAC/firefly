/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Created by roby on 2/19/15.
 */
/*jshint browserify:true*/
"use strict";

var React= require('react/addons');
var _= require('underscore');


var InputForm = module.exports = React.createClass(
{

    propTypes: {
        dispatcher : React.PropTypes.object.isRequired,
        formModel : React.PropTypes.object.isRequired

    },


    componentWillMount : function() {
        //this.setState( {formData : this.props.formModel.formData});
        //this.props.formModel.on('change:formData',this.updateState);
        this.activeInputs = []; // We create a map of traversed inputs
        this.registerInputs(this.props.children); // We register inputs from the children
    },

    componentWillUnmount : function () {
        //this.props.formModel.off('change:formData',this.updateState);
    },

    updateState : function() {
    },


    registerInputs: function (children) {

        React.Children.forEach(children, function (child) {
            if (child.hasOwnProperty('isFormInput')  ) {
                child.attachToForm = this.attachToForm;
                child.detachFromForm = this.detachFromForm;
            }

            if (child.props.children) {
                this.registerInputs(child.props.children);
            }
        }.bind(this));
    },

    attachToForm: function (component) {
        if (this.activeInputs.indexOf(component.props.fieldKey)===-1) {
            this.activeInputs.push(component.props.fieldKey);
        }
    },

    detachFromForm: function (component) {
        var idx= this.activeInputs.indexOf(component.props.fieldKey);
        if (idx>0) {
            this.activeInputs.splice(idx, 1);
        }
    },

    count : 1,

    render: function() {
        /*jshint ignore:start */
        this.count++;
        return (
                <div>
                     {React.Children.map(this.props.children,function(inChild,idx) {
                         return React.addons.cloneWithProps(inChild);
                     })}
                </div>
        );
        /*jshint ignore:end */
    }


});
