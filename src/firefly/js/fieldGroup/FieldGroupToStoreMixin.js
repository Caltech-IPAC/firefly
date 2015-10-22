/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';
import _ from 'lodash';
import FieldGroupCntlr from './FieldGroupCntlr.js';
import FieldGroupUtils from './FieldGroupUtils.js';
import {flux} from '../Firefly.js';



var FieldGroupToStoreMixin=  {

    storeListenerRemove : null,

    propTypes: {
        fieldKey : React.PropTypes.string.isRequired,
        groupKey : React.PropTypes.string.isRequired,
        initialState : React.PropTypes.object,
        labelWidth : React.PropTypes.number
    },

    getInitialState() {
        var {groupKey,fieldKey} = this.props;
        var fieldState = this.props.initialState || FieldGroupUtils.getGroupFields(groupKey)[fieldKey];
        return { fieldState };
    },



    componentWillMount() {
        this.getMessage= this.getFromStore.bind(this,'message','');
        this.isValid= this.getFromStore.bind(this,'valid',true);
        this.isVisible= this.getFromStore.bind(this,'visible',true);
        this.getValue= this.getFromStore.bind(this,'value','');
        this.getDisplayValue= this.getFromStore.bind(this,'displayValue','');
        this.getTip= this.getFromStore.bind(this,'tooltip','');
        this.getLabel= this.getFromStore.bind(this,'label',null);
        this.getLabelWidth= this.getFromStore.bind(this,'labelWidth',undefined);
        this.getExtraData= this.getFromStore.bind(this,'extraData',{});
        this.getValidator= this.getFromStore.bind(this,'validator',function() {
            return {valid:true,message:''};
        });
    },


    fireValueChange(payload) {
        if (!payload.groupKey) payload.groupKey= this.props.groupKey;
        flux.process({type: FieldGroupCntlr.VALUE_CHANGE, payload});
    },


    getFromStore(key,defValue) {
        if (!this.state.fieldState) {
            return defValue;
        }
        var fieldState= this.state.fieldState;
        return fieldState[key] !== undefined ? fieldState[key] : defValue;
    },

    componentWillReceiveProps(nextProps) {
        this.updateFieldStateWithProps(nextProps);
    },


    componentDidMount() {
        this.storeListenerRemove= flux.addListener( ()=> this.updateFieldStateWithProps(this.props));
        flux.process({
            type: FieldGroupCntlr.MOUNT_COMPONENT,
            payload: {
                groupKey: this.props.groupKey,
                fieldKey : this.props.fieldKey,
                mounted : true,
                value: this.getValue(),
                fieldState: this.props.initialState
            }} );
        //this.storeListenerRemove= FieldGroupStore.listen( ()=> this.updateFieldStateWithProps(this.props));
        //_.defer(()=> {
        //    FieldGroupActions.mountComponent( {
        //        groupKey: this.props.groupKey,
        //        fieldKey : this.props.fieldKey,
        //        mounted : true,
        //        value: this.getValue(),
        //        fieldState: this.props.initialState
        //    } );
        //});
    },

    componentWillUnmount() {
        if (this.storeListenerRemove) this.storeListenerRemove();
        flux.process({
            type: FieldGroupCntlr.MOUNT_COMPONENT,
            payload: {
                groupKey: this.props.groupKey,
                fieldKey : this.props.fieldKey,
                mounted : false,
                value: this.getValue()
            }} );

        //formActions.mountComponent(this.getFormKey(),this.props.fieldKey,false,this.getValue(),this.props.initialState)
    },

    //updateFieldStateOnMount() {
    //    this.updateFieldStateWithProps(this.props)
    //},

    updateFieldStateWithProps(props) {
        var {groupKey, fieldKey}= props;
        var groupState= FieldGroupUtils.getGroupState(groupKey);
        if (groupState && groupState.mounted && groupState.fields[fieldKey]) {
            if (this.state.fieldState!==groupState.fields[fieldKey]) {
                this.setState( {fieldState : groupState.fields[fieldKey]});
            }
        }
    }

};



export default FieldGroupToStoreMixin;
