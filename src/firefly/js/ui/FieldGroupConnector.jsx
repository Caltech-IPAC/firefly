/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {useContext, useState, useEffect, useRef} from 'react';
import PropTypes from 'prop-types';
import {get, isUndefined, omit} from 'lodash';
import {dispatchMountComponent,dispatchValueChange} from '../fieldGroup/FieldGroupCntlr.js';
import FieldGroupUtils, {callValidator, canValidate, getFieldGroupState} from '../fieldGroup/FieldGroupUtils.js';
import {flux} from '../core/ReduxFlux.js';
import {FieldGroupCtx, ForceValidCtx} from './FieldGroup';
import {isDefined} from '../util/WebUtil';

const defaultConfirmValue= (v) => v;
const defValidatorFunc= () => ({valid:true,message:''});



const STORE_OMIT_LIST= ['fieldKey', 'groupKey', 'initialState', 'fireReducer',
    'confirmValue', 'confirmValueOnInit', 'forceReinit', 'mounted'];

function buildViewProps(fieldState,props,fieldKey,groupKey,value='') {
    const {message= '', valid= true, visible= true, value:ignoreValue, displayValue= '',
        tooltip= '', validator= defValidatorFunc, ...rest}= fieldState;
    const propsClean= Object.keys(props).reduce( (obj,k)=> {
        if (isDefined(props[k]) && k!=='value') obj[k]=props[k];
        return obj;
    },{} );

    const tmpProps= { message, valid, visible, value, displayValue,
            tooltip, validator, key:`${groupKey}-${fieldKey}`, ...rest, ...propsClean};

    return omit(tmpProps, STORE_OMIT_LIST);
}

function showValueWarning(groupKey, fieldKey, value, ignoring) {
    if (!ignoring) return;
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
    return isInit(infoRef,fieldKey,groupKey) && prevFieldKey && prevGroupKey;
}

function doGetInitialState(groupKey, initialState,  props, confirmValueOnInit= defaultConfirmValue) {
    const {fieldKey}= props;
    const {keepState=false}= getFieldGroupState(groupKey) ?? {};
    const storeField= get(FieldGroupUtils.getGroupFields(groupKey), [fieldKey]);
    const initS= !keepState ? (initialState ||  storeField || {}) : (storeField || initialState || {});
    return {...initS, value: confirmValueOnInit(initS.value,props,initialState,initS)};
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
 * @prop {function(payload:Object)} fireValueChange- Call with the value of the view changes. The parameter is an object
 * the should contains as least a value property plus anything else that is appropriate to pass
 * @prop {Object} viewProps - all the properties passed to useFieldGroupConnector with the connection properties
 * removed.  This object should contain all the properties to pass on the the view component
 * @prop {String} fieldKey - the passed fieldKey, you don't usually need to access this value
 * @prop {String} groupKey -  the passed groupKey, you don't usually need to access this value
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
    confirmValueOnInit: PropTypes.func,
    confirmValue: PropTypes.func
};

/**
 * Minimal set of properties ot use for useFieldGroupConnector
 */
export const fgMinPropTypes= {
    fieldKey: PropTypes.string.isRequired,
    groupKey: PropTypes.string,  // normally passed in context
};

/**
 * @name ConfirmValueFunc
 * Give a value, props, and state, return the same or a updated value. It must return a value. This is most often
 * used with a radio box type component when the value must be one in the list.
 * @function
 * @param {*} value
 * @param {Object} props
 * @param {Object} state
 * @returns *
 */

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
 * @param {string} [props.initialState] - optional - the initial state object, anything in the initialState,
 * should be thought of as state data, it can change over the lifetime of the component and is not controlled by props.
 * Typically only items like value (and possibly displayValue) are in the state but other items can be manage there as well.
 * @param {ConfirmValueFunc} [props.confirmValueOnInit] - optional - If defined it will be call only on init
 * @param {ConfirmValueFunc} [props.confirmValue] - optional - If defined it called every update or init.
 * @param {boolean} [props.forceReinit] - optional - if true, this field will be reinited from the properties and not from the field group,
 *                                it is almost always unnecessary, only use if you know what you are doing and even then make sure.
 * @return {ConnectorInterface}
 *
 */
export const useFieldGroupConnector= (props) => {
    const infoRef = useRef({prevFieldKey:undefined, prevGroupKey:undefined});
    const context= useContext(FieldGroupCtx);
    const {forceValid}= useContext(ForceValidCtx) ;
    const {fieldKey,confirmValue,confirmValueOnInit}= props;
    let {initialState}= props;
    const groupKey= props.groupKey || context.groupKey;
    const doingInit= isInit(infoRef,fieldKey,groupKey);

    if (doingInit) {// validation checks
        validateKeys(fieldKey,groupKey);
        if (isDefined(props.value)) {
            showValueWarning(groupKey, fieldKey, props.value, (initialState && isDefined(initialState.value)));
            initialState= {value:props.value, ...initialState};
        }
    }

    const getInitialState= () => doGetInitialState(groupKey, initialState,  props, (confirmValueOnInit||confirmValue));

    const [fieldState, setFieldState] = useState(() => getInitialState());

    const fireValueChange= (payload) => dispatchValueChange({...payload, fieldKey,groupKey});
    const value= confirmValue ? confirmValue(fieldState.value,props,fieldState) : fieldState.value;

    const effectChangeAry= [fieldKey, groupKey, fieldState];
    if (confirmValue) effectChangeAry.push(value); // only need to watch value in this case


    useEffect(() => {
        if (doingInit) {  // called the first time or when fieldKey or groupKey change
            let value= fieldState.value;
            let newInitState= initialState;
            if (hasNewKeys(infoRef,fieldKey,groupKey)) { // if field and group key changed, whole thing must reinit
                const {prevFieldKey, prevGroupKey}= infoRef.current;
                dispatchMountComponent( prevGroupKey, prevFieldKey, false );
                const initFieldState= getInitialState();
                setFieldState(initFieldState);
                value= initFieldState.value;
                newInitState= initFieldState;
            }
            else {
                const initFieldState= getInitialState();
                const groupState = getFieldGroupState(groupKey) || {};
                const s = groupState?.fields?.[fieldKey];
                const {keepState= false}= groupState;
                if (s) {
                    const rest= omit(s, ['fieldKey', 'groupKey', 'mounted']);
                    newInitState= keepState  ? {...initFieldState, ...rest} : {...rest, ...initFieldState};
                    setFieldState(newInitState);
                }
            }
            dispatchMountComponent( groupKey, fieldKey, true, value, newInitState );
            infoRef.current={prevFieldKey: fieldKey, prevGroupKey: groupKey};
        }
        else if (confirmValue) {// in this case the value might have been updated during the render
            if (fieldState.value!==value) fireValueChange({value}); // put value back in sync
        }
        return flux.addListener(()=> {
            const gState = getFieldGroupState(groupKey);
            if (!gState || !gState.mounted || !get(gState,['fields',fieldKey])) return;
            if (fieldState !== gState.fields[fieldKey]) setFieldState(gState.fields[fieldKey]);
        });
    }, effectChangeAry);

    useEffect(() => {  // only run on dismount
        return () => dispatchMountComponent( groupKey, fieldKey, false);
    }, []);

    useEffect(() => {
        if (doingInit || isUndefined(forceValid)) return;
        if (forceValid) {
            fireValueChange({valid:true});
        }
        else {
            const f= {...fieldState};
            f.validator= f.validator ?? props.validator;
            if (canValidate(f)) fireValueChange({...callValidator(f)});
        }
    }, [forceValid]);

    return {
        fireValueChange, viewProps: buildViewProps(fieldState,props,fieldKey,groupKey, value), fieldKey, groupKey
    };
};
