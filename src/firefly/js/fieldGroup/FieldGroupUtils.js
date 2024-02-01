/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isFunction, hasIn, isBoolean, isEmpty, pick, isUndefined} from 'lodash';
import shallowequal from 'shallowequal';
import {flux} from '../core/ReduxFlux.js';
import {smartMerge} from '../tables/TableUtil.js';
import {isDefined} from '../util/WebUtil.js';
import {FIELD_GROUP_KEY,dispatchValueChange,dispatchMultiValueChange} from './FieldGroupCntlr.js';
import {Logger} from '../util/Logger.js';

const logger = Logger('FieldGroupUtils');

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
                dispatchValueChange({valid:true,fieldKey:key,groupKey,...newValue});
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
                f= {...f, message: 'Value is required', valid: false};
            }
            f= (f.validator && f.valid) ? {...f, ...callValidator(f)} : f;
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
        .catch( (e) => logger.error(e));
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
 * @param [includeUnmounted]
 * @return {Promise} promise of a boolean true if valid
 */
export var validateFieldGroup= function(groupKey, includeUnmounted= false) {
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
    return Object.keys(fields)
        .filter((fieldKey) => (isUndefined(fields[fieldKey].mounted)||fields[fieldKey].mounted||includeUnmounted))
        .reduce((request, key) => {
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
export const getFieldGroupState= (groupKey) => flux.getState()[FIELD_GROUP_KEY]?.[groupKey];

export const isFieldGroupMounted = (groupKey) => getFieldGroupState(groupKey)?.isMounted;

export const getFieldVal= (groupKey, fldName, defval) => getFldValue(getGroupFields(groupKey), fldName, defval);

export function getFieldsForKeys(groupKey,fldNameKeyAry) {
    if (!groupKey || !fldNameKeyAry?.length) return [];
    const fields= getGroupFields(groupKey) ?? {};
    return fldNameKeyAry.map( (key) => ({...(fields[key] ?? {})}));
}


const getFldValue= (fields, fldName, defval=undefined) => fields?.[fldName]?.value ?? defval;

export const getMetaState= (groupKey) => getFieldGroupState(groupKey)?.metaState;

export const getField= (groupKey, fldName) => getGroupFields(groupKey)?.[fldName];

export function makeFieldsObject(groupKey,fldNameAry) {
    return fldNameAry.reduce( (obj,key) => {
        obj[key]= getField(groupKey,key);
        return obj;
    },{});
}

/**
 * set any value in the field object
 * @param {String} groupKey
 * @param {String} fieldKey
 * @param {Object} fieldUpdates
 */
export function setField(groupKey,fieldKey,fieldUpdates) {
    if (!fieldUpdates) return;
    const cField= getField(groupKey,fieldKey) ?? {};
    if (!fieldUpdates) return;

    const forComparison= {displayValue:'',...fieldUpdates};
    const originObj= pick({displayValue:'',...cField},Object.keys(forComparison));
    if (shallowequal(originObj,forComparison)) return;
    dispatchValueChange({...cField, ...fieldUpdates, groupKey, fieldKey});
}

/**
 * set the field value and optionally send an object with other fieldUpdates (such as {valid:false,message:'opps'}
 * @param {String} groupKey
 * @param {String} fieldKey
 * @param value
 * @param {Object} fieldUpdates
 */
export function setFieldValue(groupKey,fieldKey,value=undefined,fieldUpdates) {
    const sendUpdates = {...fieldUpdates};
    const valueUpdates = {displayValue: '', value, valid: true};
    setField(groupKey,fieldKey,{...valueUpdates, ...sendUpdates});
}

/**
 * Get the group fields for a key
 * @param {string} groupKey
 * @return {object}
 */
export const getGroupFields= (groupKey) => getFieldGroupState(groupKey)?.fields ?? {};


/**
 *
 * @param {string} groupKey
 * @param {function} stateUpdaterFunc this is a functions takes a field object as a parameter and should update the
 *                   react components state
 * @return {function} a function that will unbind the store, should be called on componentWillUnmount
 */
function bindToStore(groupKey, stateUpdaterFunc) {
    return flux.addListener( () => {
        const fields= getGroupFields(groupKey);
        if (isEmpty(fields)) return;
        stateUpdaterFunc(getGroupFields(groupKey));
    } );
}

export function canValidate(f) {
    return Boolean(f.validator && !isFunction(f.value) && !f.value?.then);
}

export function callValidator(f) {
    return canValidate(f) ? f.validator(f.value) : {valid: true, message: ''};
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
        if (canValidate(f)) {
            newfields[key]= smartMerge(f,callValidator(f));
            if (newfields[key] !== f) hasChanged = true;
        } else {
            newfields[key] = f;
        }
    } );
    return hasChanged ? newfields : fields;
}


const FieldGroupUtils= {getGroupFields, getFldValue, bindToStore };
export default FieldGroupUtils;
