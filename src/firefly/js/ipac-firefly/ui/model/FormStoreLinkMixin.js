/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Created by roby on 3/5/15.
 */
/*jshint browserify:true*/
/*jshint esnext:true*/
"use strict";

import React from 'react/addons';


var FormStoreLinkMixin=  {
        propTypes: {
            dispatcher : React.PropTypes.object.isRequired,
            fieldKey : React.PropTypes.string.isRequired,
            formModel : React.PropTypes.object.isRequired,
            initialState : React.PropTypes.object,
            labelWidth : React.PropTypes.number
        },

        getInitialState() {
            var fieldState = this.props.initialState || this.props.formModel[this.props.fieldKey];
            return {
                fieldState : fieldState
            };
        },


        componentWillMount() {
            this.getMessage= this.getFromStore.bind(this,"message","");
            this.isValid= this.getFromStore.bind(this,"valid",true);
            this.isVisible= this.getFromStore.bind(this,"visible",true);
            this.getValue= this.getFromStore.bind(this,"value","");
            this.getTip= this.getFromStore.bind(this,"tooltip","");
            this.getLabel= this.getFromStore.bind(this,"label",null);
            this.getLabelWidth= this.getFromStore.bind(this,"labelWidth",undefined);
            this.getValidator= this.getFromStore.bind(this,"validator",function() {
                return {valid:true,message:""};
            });
        },


        getFromStore(key,defValue) {
            if (!this.state.fieldState) {
                return defValue;
            }
            var fieldState= this.state.fieldState;
            return fieldState[key] !== undefined ? fieldState[key] : defValue;
        },

        componentDidMount() {
            this.props.formModel.on('change:'+this.props.fieldKey,this.updateFieldState);
            var payload= {
                evType : 'mountComponent',
                fieldKey : this.props.fieldKey,
                mounted : true,
                value : this.getValue()
            };
            if (this.props.initialState) {
                payload.fieldState= this.props.initialState;
            }
            this.props.dispatcher.dispatch(payload);
        },

        componentWillUnmount() {
            this.props.formModel.off('change:'+this.props.fieldKey,this.updateFieldState);
            this.props.dispatcher.dispatch({
                                               evType : 'mountComponent',
                                               fieldKey : this.props.fieldKey,
                                               mounted : false,
                                               value : this.getValue()
                                           });
        },

        updateFieldState() {
            var key= this.props.fieldKey;
            this.setState( {fieldState : this.props.formModel[key]});
        }
};

export default FormStoreLinkMixin;
