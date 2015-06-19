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
import formActions from '../../actions/FormActions.js'






var FormStoreLinkMixin=  {

        storeListenerRemove : null,

        propTypes: {
            fieldKey : React.PropTypes.string.isRequired,
            formStore : React.PropTypes.object.isRequired,
            initialState : React.PropTypes.object,
            labelWidth : React.PropTypes.number
        },

        getInitialState() {
            var {formStore,fieldKey} = this.props;
            var fieldState = this.props.initialState || formStore.getState().fields[fieldKey];
            return {
                fieldState : fieldState
            };
        },



        componentWillMount() {
            this.getMessage= this.getFromStore.bind(this,"message","");
            this.isValid= this.getFromStore.bind(this,"valid",true);
            this.isVisible= this.getFromStore.bind(this,"visible",true);
            this.getValue= this.getFromStore.bind(this,"value","");
            this.getDisplayValue= this.getFromStore.bind(this,'displayValue','');
            this.getTip= this.getFromStore.bind(this,"tooltip","");
            this.getLabel= this.getFromStore.bind(this,"label",null);
            this.getLabelWidth= this.getFromStore.bind(this,"labelWidth",undefined);
            this.getExtraData= this.getFromStore.bind(this,"extraData",{});
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

        getFormKey() {
            return this.props.formStore.getState().formKey;
        },

        componentDidMount() {
            this.storeListenerRemove= this.props.formStore.listen(this.updateFieldState);
            formActions.mountComponent( {
                formKey: this.getFormKey(),
                fieldKey : this.props.fieldKey,
                mounted : true,
                value: this.getValue(),
                fieldState: this.props.initialState,
            } );
        },

        componentWillUnmount() {
            if (this.storeListenerRemove) this.storeListenerRemove();
            formActions.mountComponent( {
                formKey: this.getFormKey(),
                fieldKey : this.props.fieldKey,
                mounted : false,
                value: this.getValue()
            } );

            //formActions.mountComponent(this.getFormKey(),this.props.fieldKey,false,this.getValue(),this.props.initialState)
        },

        updateFieldState() {
            var {fieldKey, formStore}= this.props;
            if (fieldKey===this.props.fieldKey && this.state.fieldState!==formStore.getState().fields[fieldKey]) {
                this.setState( {fieldState : formStore.getState().fields[fieldKey]});
            }
        }
}



export default FormStoreLinkMixin;
