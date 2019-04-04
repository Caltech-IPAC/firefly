/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {useContext, useState, useEffect, useRef} from 'react';
import PropTypes from 'prop-types';
import {get,omit} from 'lodash';
import {dispatchMountComponent,dispatchValueChange} from '../fieldGroup/FieldGroupCntlr.js';
import FieldGroupUtils, {getFieldGroupState} from '../fieldGroup/FieldGroupUtils.js';
import {flux} from '../Firefly.js';
import {GroupKeyCtx} from './FieldGroup';
import {isDefined} from '../util/WebUtil';

const defaultConfirmInitValue= (v) => v;
const defValidatorFunc= () => ({valid:true,message:''});



const STORE_OMIT_LIST= ['fieldKey', 'groupKey', 'initialState', 'fireReducer',
    'confirmInitialValue', 'forceReinit', 'mounted'];

function buildViewProps(fieldState,props,fieldKey,groupKey) {
    const {message= '', valid= true, visible= true, value= '', displayValue= '',
        tooltip= '', validator= defValidatorFunc, ...rest}= fieldState;
    const propsClean= Object.keys(props).reduce( (obj,k)=> {
        if (isDefined(props[k]) && k!=='value') obj[k]=props[k];
        return obj;
    },{} );

    const tmpProps= Object.assign({ message, valid, visible, value, displayValue,
            tooltip, validator, key:`${groupKey}-${fieldKey}`},
        rest, propsClean);

    return omit(tmpProps, STORE_OMIT_LIST);
}

function showValueWarning(groupKey, fieldKey, value, ignoring) {
    const extra= ignoring ? 'value property is being ignored, since initialState.value is defined' : '';
    console.warn(
        `useFieldGroupConnector: fieldKey: ${fieldKey}, groupKey: ${groupKey}: value should not be passed in props\n`,
        `Only initial value should be passed as in the initialState property, ${extra}\n`,
        `i.e. initialState= {value: '${value}'} `);
}

function validateKeys(fieldKey, groupKey) {
    if (!fieldKey) throw Error('useFieldGroupConnector: fieldKey is required for useFieldGroupConnector.');
    if (!groupKey) throw Error('useFieldGroupConnector: groupKey is required for useFieldGroupConnector, groupKey is usually passed though context but may be a prop.');
}

function isInit(infoRef,fieldKey,groupKey) {
    const {prevFieldKey, prevGroupKey}= infoRef.current;
    return (prevFieldKey!==fieldKey || prevGroupKey !== groupKey);
}

function hasNewKeys(infoRef,fieldKey,groupKey) {
    const {prevFieldKey, prevGroupKey}= infoRef.current;
    return isInit(infoRef,fieldKey,groupKey) && prevFieldKey && prevGroupKey
}

function doGetInitialState(groupKey, initialState,  props) {
    const {fieldKey, forceReinit, confirmInitialValue= defaultConfirmInitValue}= props;
    const storeField= get(FieldGroupUtils.getGroupFields(groupKey), [fieldKey]);
    const initS= forceReinit ? (initialState ||  storeField || {}) : (storeField || initialState || {});
    return {...initS, value: confirmInitialValue(initS.value,props,initS)};
}


/**
 * @global
 * @public
 * @typedef {Object} ConnectorInterface
 *
 * @summary Return value of useFieldGroupConnector. This object contains the view components properties (viewProps) and the
 * fireValueChange. Simple pass the viewProps to the component and call fireValueChange when the view component has a
 * value change call.
 *
 * @prop {Function} fireValueChange- Call the anytime the value of the view changes. The parameter is an object
 * the should contains as least a value properties plus anything else that is appropriate to pass
 * @prop {Object} viewProps - all the properties passed to useFieldGroupConnector with the connection properties
 * removed.  This object should contain all the properties to pass on the the view component
 * @prop {fieldKey} return the passed fieldKey, you don't usually need to access this value
 * @prop {groupKey} return the passed groupKey, you don't usually need to access this value
 */

/**
 * useFieldGroupConnector parameters expressed as propTypes
 */
export const fgConnectPropsTypes= {
    fieldKey: PropTypes.string.isRequired,
    groupKey: PropTypes.string, // normally passed in context
    forceReinit: PropTypes.bool,
    initialState: PropTypes.shape({ // not all fields use everything in initialState, most of it is optional
        value: PropTypes.any, // this is the most common one, it is the initial value for the field.
        message: PropTypes.string,
        validator: PropTypes.func,
        displayValue: PropTypes.string,
        tooltip:  PropTypes.string,
        label:  PropTypes.string,
    }),
    confirmInitialValue: PropTypes.func
};

/**
 * Minimal set of properties ot use for useFieldGroupConnector
 */
export const fgMinPropTypes= {
    fieldKey: PropTypes.string.isRequired,
    groupKey: PropTypes.string,  // normally passed in context
};

/**
 *
 * Hook to connect a field to the FieldGroup Store. Pass the props object, make sure it includes the required props
 * to connect to the store (fieldKey is the only requirement, see below). The hooks returns an object with the new
 * props that you should be able to pass directly to the view.
 *
 * the props object parameter can contain any that should be kept in the store. The parameters below are special.
 * fieldKey is required.
 *
 * @param {Object} props
 * @param {string} props.fieldKey - required, a unique id for this field (unique within group)
 * @param {string} [props.groupKey] - optional - a unique group id, normally this is not use because it is passed in the context
 * @param {string} [props.forceReinit] - optional - if true, this field will be reinited from the properties and not from the field group
 * @param {string} [props.initialState] - optional - the initial state object
 * @param {function} [props.confirmInitialValue] - optional - on the first render the value from the properties is passed and a value is returned
 * @return {ConnectorInterface}
 *
 */
export const useFieldGroupConnector= (props) => {
    const infoRef = useRef({prevFieldKey:undefined, prevGroupKey:undefined});
    const gkFromCtx= useContext(GroupKeyCtx);
    const {fieldKey}= props;
    let {initialState}= props;
    const groupKey= props.groupKey || gkFromCtx;
    const doingInit= isInit(infoRef,fieldKey,groupKey);

    if (doingInit) {// validation checks
        validateKeys(fieldKey,groupKey);
        if (isDefined(props.value)) {
            showValueWarning(groupKey, fieldKey, props.value, (initialState && isDefined(initialState.value)));
            initialState= {value:props.value, ...initialState};
        }
    }

    const getInitialState= () => doingInit ? doGetInitialState(groupKey, initialState,  props) : undefined;

    const [fieldState, setFieldState] = useState(getInitialState());


    useEffect(() => {
        if (doingInit) {  // called the first time or when fieldKey or groupKey change
            let value= fieldState.value;
            if (hasNewKeys(infoRef,fieldKey,groupKey)) { // if field and group key changed, whole thing must reinit
                const {prevFieldKey, prevGroupKey}= infoRef.current;
                dispatchMountComponent( prevGroupKey, prevFieldKey, false );
                const initFieldState= getInitialState();
                setFieldState(initFieldState);
                value= initFieldState.value;
            }
            dispatchMountComponent( groupKey, fieldKey, true, value, initialState );
            infoRef.current={prevFieldKey: fieldKey, prevGroupKey: groupKey};
        }
        return flux.addListener(()=> {
            const gState = getFieldGroupState(groupKey);
            if (!gState || !gState.mounted || !get(gState,['fields',fieldKey])) return;
            if (fieldState !== gState.fields[fieldKey]) setFieldState(gState.fields[fieldKey]);
        });
    }, [fieldKey, groupKey, fieldState]);

    return {
        fireValueChange: (payload) => dispatchValueChange({...payload, fieldKey,groupKey}),
        viewProps: buildViewProps(fieldState,props,fieldKey,groupKey),
        fieldKey, groupKey
    };
};
