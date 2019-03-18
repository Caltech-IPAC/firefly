/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useContext, useState, useEffect, useRef} from 'react';
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

function buildViewProps(fieldState,props) {
    const {message= '', valid= true, visible= true, value= '', displayValue= '',
        tooltip= '', validator= defValidatorFunc, ...rest}= fieldState;
    const propsClean= Object.keys(props).reduce( (obj,k)=> {
        if (isDefined(props[k])) obj[k]=props[k];
        return obj;
    },{} );

    const tmpProps= Object.assign({ message, valid, visible, value, displayValue,
            tooltip, validator, key:fieldState.fieldKey},
        rest, propsClean);

    return omit(tmpProps, STORE_OMIT_LIST);
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
 */



/**
 *
 * Hook to connect a field to the FieldGroup Store. Pass the props object, make sure it includes the required props
 * to connect to the store (fieldKey is the only requirement, see below). The hooks returns qn object with the new
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
    const {fieldKey, forceReinit, confirmInitialValue= defaultConfirmInitValue, initialState}= props;
    const groupKey= props.groupKey || gkFromCtx;

    function getInitialState() {
        const storeField= get(FieldGroupUtils.getGroupFields(groupKey), [fieldKey]);
        const initS= forceReinit ? (initialState ||  storeField || {}) : (storeField || initialState || {});
        return {...initS, value: confirmInitialValue(initS.value,props,initS)};
    }
    const [fieldState, setFieldState] = useState(getInitialState());


    useEffect(() => {
        const {prevFieldKey, prevGroupKey}= infoRef.current;
        if (prevFieldKey!==fieldKey || prevGroupKey !== groupKey) {  // called the first time or when fieldKey or groupKey change
            let value= fieldState.value;
            if (prevFieldKey && prevGroupKey) { // if field and group key changed, whole thing must reinit
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
        viewProps: buildViewProps(fieldState,props)
    };
};




