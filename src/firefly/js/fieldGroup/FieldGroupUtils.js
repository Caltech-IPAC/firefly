/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get,isFunction,hasIn,isBoolean} from 'lodash';
import {flux} from '../core/ReduxFlux.js';
import {logError,clone} from '../util/WebUtil.js';
import {smartMerge} from '../tables/TableUtil.js';
import {FIELD_GROUP_KEY,dispatchValueChange,dispatchMultiValueChange} from './FieldGroupCntlr.js';


const includeForValidation= (f,includeUnmounted) => f.valid !== undefined && (f.mounted||includeUnmounted);


function validateResolvedSingle(groupKey,includeUnmounted) {
    const flds= getGroupFields(groupKey);
    const valid = Object.keys(flds).every( (key) =>
                     includeForValidation(flds[key],includeUnmounted) ? flds[key].valid : true);
    return valid;
}


/**
 * 
 * @param groupKey
 * @param includeUnmounted
 * @return Promise with the valid state true/false --- todo: the return value does not work
 */
function validateSingle(groupKey, includeUnmounted) {
    let fields= getGroupFields(groupKey);
    if (!fields) return Promise.resolve(true);

    //====== clear out all functions
    Object.keys(fields).forEach( (key) => {
        if (isFunction(fields[key].value)) {
            const newValue= fields[key].value();
            if (typeof newValue=== 'object' && // check to see if return is an object that includes {value: any} and not a promise
                !newValue.then &&
                newValue.hasOwnProperty('value') ) {
                dispatchValueChange(Object.assign({valid:true,fieldKey:key,groupKey},newValue));
            }
            else {
                dispatchValueChange({fieldKey:key,groupKey,valid:true,value:newValue});
            }
        }
    });


    //====== Validate any plain value fields

    fields= getGroupFields(groupKey); // need a new copy

    const newFieldAry= Object.keys(fields)
        .map( (k) => fields[k])
        .filter( (f) => !isFunction(f.value) && !hasIn(f.value,'then'))
        .map( (f) => {
            if (!f.nullAllowed && !f.value && f.valid && !isBoolean(f.value) && f.mounted) {
                f= clone(f, {message: 'Value is required', valid: false});
            }
            f= (f.validator && f.valid) ? clone(f, f.validator(f.value)) :f;
            return f;
        });

    dispatchMultiValueChange(groupKey,newFieldAry);


    //===============
    return Promise.all( Object.keys(fields).map( (key) => Promise.resolve(fields[key].value),this ) )
        .then( () =>
        {
            return new Promise( (resolve, reject) => 
                window.setTimeout( () => resolve(validateResolvedSingle(groupKey,includeUnmounted))
            ));
        }
    )
        .catch( (e) => logError(e));
}

/**
 *
 * @param groupKeyAry
 * @param includeUnmounted
 */
function validateGroup(groupKeyAry, includeUnmounted) {
    return Promise.all(groupKeyAry.map( (groupKey) => validateSingle(groupKey,includeUnmounted)))
        .then( (validAry) => {
            return validAry.every( (v) => v);
        });
}

/**
 * 
 * @param groupKey
 * @param includeUnmounted
 * @return {Promise} promise of a boolean true if valid
 */
export var validateFieldGroup= function(groupKey, includeUnmounted) {
    if (Array.isArray(groupKey)) {
        return validateGroup(groupKey,includeUnmounted);
    }
    else {
        return validateSingle(groupKey,includeUnmounted);
    }
};


/**
 * 
 * @param {string} groupKey the group key for the fieldGroup
 * @param includeUnmounted if true, get the results for any fields that are not showing
 * @return {*}
 */
export function getFieldGroupResults(groupKey,includeUnmounted=false) {
    const fields= getGroupFields(groupKey);
    if (!fields) return null;
    return Object.keys(fields).
        filter((fieldKey) => (fields[fieldKey].mounted||includeUnmounted)).
        reduce((request, key) => {
            request[key] = fields[key].value;
            return request;
        }, {});
}


/**
 * Get the group state for a key
 *
 * @param {string} groupKey
 * @return {object}
 */
export function getFieldGroupState(groupKey) {
    const fieldGroupMap= flux.getState()[FIELD_GROUP_KEY];
    return fieldGroupMap[groupKey] ? fieldGroupMap[groupKey] : null;
}

export function getFieldVal(groupKey, fldName, defval=undefined) {
    return get(getGroupFields(groupKey), [fldName, 'value'], defval);
}

export function getField(groupKey, fldName) {
    return get(getGroupFields(groupKey), fldName);
}

export function getReducerFunc(groupKey) {
    const groupState= getFieldGroupState(groupKey);
    return get(groupState, 'reducerFunc');
}

/**
 * Get the group fields for a key
 *
 * @param {string} groupKey
 * @return {object}
 */
function getGroupFields(groupKey) {
    const groupState= getFieldGroupState(groupKey);
    return groupState?groupState.fields:null;
}

function getFldValue(fields, fldName, defval=undefined) {
    return (fields? get(fields, [fldName, 'value'], defval) : defval);
}

/**
 *
 * @param {string} groupKey
 * @param {function} stateUpdaterFunc this is a functions takes a field object as a parameter and should update the
 *                   react components state
 * @return {function} a function that will unbind the store, should be called on componentWillUnmount
 */
function bindToStore(groupKey, stateUpdaterFunc) {
    const storeListenerRemove= flux.addListener( () => {
        stateUpdaterFunc(getGroupFields(groupKey));
    });
    return storeListenerRemove;
}


/**
 * Revalidate fields. If no change happened in a field state, its reference does not change
 * @param fields
 */
export function revalidateFields(fields) {
    const newfields= {};   //clone(fields);
    let hasChanged = false;
    Object.keys(fields).forEach( (key) => {
        const f= fields[key];
        if (f.validator && !isFunction(f.value) && f.value && !f.value.then) {
            // const {valid,message} = f.validator(f.value);
            // newfields[key]= (valid!==f.valid || message!==f.message) ? clone(f,{valid,message}) : f;
            newfields[key]= smartMerge(f,f.validator(f.value));
            if (newfields[key] !== f) hasChanged = true;
        } else {
            newfields[key] = f;
        }
    } );
    return hasChanged ? newfields : fields;
}





const FieldGroupUtils= {getGroupFields, getFldValue, bindToStore };

export default FieldGroupUtils;

