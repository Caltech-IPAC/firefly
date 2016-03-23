/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import {flux} from '../Firefly.js';
import {logError} from '../util/WebUtil.js';
import FieldGroupCntlr, {dispatchValueChange} from './FieldGroupCntlr.js';


const includeForValidation= (f,includeUnmounted) => f.valid !== undefined && (f.mounted||includeUnmounted);


function validateResolvedSingle(groupKey,includeUnmounted, doneCallback) {
    var flds= getGroupFields(groupKey);
    var valid = Object.keys(flds).every( (key) => includeForValidation(flds[key]) ? flds[key].valid : true);
    doneCallback(valid);
}


/**
 * 
 * @param groupKey
 * @param includeUnmounted
 * @param doneCallback
 * @return Promise with the valid state true/false --- todo: the return value does not work
 */
var validateSingle= function(groupKey, includeUnmounted, doneCallback) {
    var fields= getGroupFields(groupKey);

    //====== clear out all functions
    Object.keys(fields).forEach( (key) => {
        if (typeof fields[key].value === 'function') {
            var newValue= fields[key].value();
            if (typeof newValue=== 'object' &&       // check to see if return is an object with {value:string,valid:boolean}
                Object.keys(newValue).length===2 &&
                newValue.hasOwnProperty('valid') &&
                newValue.hasOwnProperty('value') ) {
                dispatchValueChange({fieldKey:key,groupKey,valid:newValue.valid,value:newValue.value});
            }
            else {
                dispatchValueChange({fieldKey:key,groupKey,valid:true,value:newValue});
            }
        }
    });


    //===============

    fields= getGroupFields(groupKey); // need a new copy

    if (!fields) return Promise.resolve(true);
    return Promise.all( Object.keys(fields).map( (key) => Promise.resolve(fields[key].value),this ) )
        .then( (allResults) =>
        {
            window.setTimeout(validateResolvedSingle(groupKey,includeUnmounted, doneCallback));
        }
    )
        .catch( (e) => logError(e));
};

var validateGroup= function() {
    //todo
};

/**
 * 
 * @param groupKey
 * @param includeUnmounted
 * @param doneCallback
 */
export var validateFieldGroup= function(groupKey, includeUnmounted, doneCallback) {
    if (Array.isArray(groupKey)) {
        validateGroup(groupKey,includeUnmounted, doneCallback);
    }
    else {
        validateSingle(groupKey,includeUnmounted, doneCallback);
    }
};


/**
 * 
 * @param {string} groupKey the group key for the fieldgroup
 * @param includeUnmounted if true, get the results for any fields that are not showing
 * @return {*}
 */
export function getFieldGroupResults(groupKey,includeUnmounted=false) {
    var fields= getGroupFields(groupKey);
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
const getGroupState= function(groupKey) {
    var fieldGroupMap= flux.getState()[FieldGroupCntlr.FIELD_GROUP_KEY];
    return fieldGroupMap[groupKey] ? fieldGroupMap[groupKey] : null;
};

/**
 * Get the group fields for a key
 *
 * @param {string} groupKey
 * @return {object}
 */
const getGroupFields= function(groupKey) {
    var groupState= getGroupState(groupKey);
    return groupState?groupState.fields:null;
};

const getFldValue= function(fields, fldName, defval=undefined) {
    return (fields? get(fields, [fldName, 'value']) : defval);
};


/**
 *
 * @param {string} groupKey
 * @param {function} stateUpdaterFunc this is a functions takes a field object as a parameter and should update the
 *                   react components state
 * @return {function} a function that will unbind the store, should be called on componentWillUnmount
 */
const bindToStore= function(groupKey, stateUpdaterFunc) {
    var storeListenerRemove= flux.addListener( () => {
        stateUpdaterFunc(getGroupFields(groupKey));
    });
    return storeListenerRemove;
};







var FieldGroupUtils= {getGroupState, getGroupFields, getFldValue, bindToStore };

export default FieldGroupUtils;

