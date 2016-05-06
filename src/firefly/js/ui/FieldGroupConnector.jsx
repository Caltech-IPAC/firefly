import React, {Component,PropTypes} from 'react';
import {get,omit} from 'lodash';
import {dispatchMountComponent,dispatchValueChange} from '../fieldGroup/FieldGroupCntlr.js';
import FieldGroupUtils, {getFieldGroupState} from '../fieldGroup/FieldGroupUtils.js';
import {flux} from '../Firefly.js';
import sCompare from 'react-addons-shallow-compare';

const defaultConfirmInitValue= (v) => v;

/**
 * Wraps a react component to connect to the field group store.
 *
 * @param FieldComponent the component to be wrapped
 * @param {function} getComponentProps a function that will be called for the properties for the FieldComponent
 *                          when it is rendered.
 *                          getComponentProps(params, fireValueChange)
 *                             - params is is a combination of all values in properties, fieldStore, and State
 *                             - fireValueChange is the function to call when the value has changed
 *                                     - Parameter: a object with new values to put into the fieldGroupStore
 *                                           - object should include a a 'value' key. It may include other keys such
 *                                             as 'validator', 'valid', or anything else you want to put in.
 * @param {object} connectorPropTypes - the prop type of the connector.
 *                             The connector already has propType however, this is a way to add more
 * @param {object} connectorDefProps - the default properties of the connector
 * @param {function} confirmInitialValue - call just before mount, 
 *                          confirmInitialValue(value, props, fieldState)
 *                             - value is the current value 
 *                             - props is the props to the connector
 *                             - fieldState is the current fieldState
 *                              return a new value
 *                          the current default value is pass and it can be overridden here
 * @return {FGConnector} return the wrapped component
 */
export function fieldGroupConnector(FieldComponent,
                                    getComponentProps=()=>({}),
                                    connectorPropTypes=undefined,
                                    connectorDefProps=undefined,
                                    confirmInitialValue= defaultConfirmInitValue ) {
    class FGConnector extends Component {

        constructor(props, context) {
            super(props, context);
            this.fireValueChange = this.fireValueChange.bind(this);
            const {fieldKey} = props;
            const groupKey= getGroupKey(props,context);

            var fieldState;
            if (props.forceReinit) {
                fieldState = props.initialState || FieldGroupUtils.getGroupFields(groupKey)[fieldKey];
            }
            else {
                fieldState = FieldGroupUtils.getGroupFields(groupKey)[fieldKey] || props.initialState;
            }
            this.state = {fieldState};
        }

        shouldComponentUpdate(np, ns) {
            return sCompare(this, np, ns);
        }

        fireValueChange(payload) {
            // if (!payload.groupKey) payload.groupKey = this.props.groupKey || this.context.groupKey;
            
            const {fieldKey}= this.props;
            const modPayload= Object.assign({},payload, {fieldKey,groupKey:getGroupKey(this.props,this.context)});
            dispatchValueChange(modPayload);
        }

        componentWillReceiveProps(nextProps, context) {
            const {fieldKey}= nextProps;
            if (this.props.fieldKey!==fieldKey) {
                const groupKey= getGroupKey(this.props,this.context);
                const fieldState= get(FieldGroupUtils.getGroupFields(groupKey),fieldKey);
                this.storeUnmount(this.props.fieldKey,groupKey);
                this.reinit(nextProps,fieldState);
            }
            else {
                this.updateFieldState(nextProps, context);
            }
        }

        updateFieldState(props, context) {
            var {fieldKey}= props;
            var groupState = getFieldGroupState(getGroupKey(props,context));
            if (get(groupState,'mounted') && get(groupState,['fields',fieldKey])) {
                if (this.iAmMounted && this.state.fieldState !== groupState.fields[fieldKey]) {
                    this.setState({fieldState: groupState.fields[fieldKey]});
                }
            }
        }


        storeUnmount(fieldKey,groupKey) {
            if (this.storeListenerRemove) this.storeListenerRemove();
            this.storeListenerRemove= null;
            dispatchMountComponent(
                groupKey, fieldKey, false, get(this.state, 'fieldState.value')
            );
        }


        reinit(props,fieldState) {
            var value= get(fieldState, 'value');
            if (props.forceReinit) {
                value= props.initialState.value;
            }
            if (this.storeListenerRemove) this.storeListenerRemove();
            this.storeListenerRemove = flux.addListener(()=> this.updateFieldState(props,this.context));
            dispatchMountComponent(
                getGroupKey(props,this.context),
                props.fieldKey,
                true,
                confirmInitialValue(value,props,fieldState),
                props.initialState
            );
        }

        componentDidMount() {
            this.reinit(this.props,this.state.fieldState);
            this.iAmMounted= true;
        }

        componentWillUnmount() {
            this.storeUnmount(this.props.fieldKey, getGroupKey(this.props,this.context));
            this.iAmMounted= false;
        }

        render() {
            const groupKey= getGroupKey(this.props,this.context);
            const {fieldKey}= this.props;
            const paramValues= getParamValues(this.state.fieldState,this.props);
            return (
                <FieldComponent fieldKey={fieldKey} groupKey={groupKey} 
                    {...getComponentProps(paramValues, this.fireValueChange)}
                />
            );
        }
    }

    FGConnector.contextTypes = { groupKey: PropTypes.string };

    FGConnector.propTypes = {
        fieldKey: PropTypes.string,
        groupKey: PropTypes.string, // usually comes from context but this is a fallback
        initialState: PropTypes.object,
        labelWidth: PropTypes.number,
        forceReinit: PropTypes.bool
    };

    if (connectorPropTypes) {
        Object.assign(FGConnector.propTypes, connectorDefProps);
    }
    if (connectorDefProps) {
        FGConnector.defaultProps = connectorDefProps;
    }

    FGConnector.displayName = `FGConnector(${getDisplayName(FieldComponent)})`;

    return FGConnector;

}




function getParamValues(fieldState,props) {


    const omitProps= omit(props, ['fieldKey', 'groupKey', 'initialState']);
    
    // create one object from all three parameters. Some reserved parameters need 
    // to be defined with defaults.

    return Object.assign( {}, fieldState, omitProps, {
        message:  get(fieldState,'message',''),
        valid: get(fieldState,'valid',true),
        visible: get(fieldState,'visible',true),
        value: get(fieldState,'value',''),
        displayValue: get(fieldState,'displayValue',''),
        tooltip: get(fieldState,'tooltip',''),
        validator: get(fieldState,'validator',() => ({valid:true,message:''}))
    });
}


function getDisplayName(Component) {
  return Component.displayName || Component.name || 'Component';
}


const getGroupKey= (props,context)=> props.groupKey || get(context,'groupKey');

