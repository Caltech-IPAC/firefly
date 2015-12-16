/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {logError} from '../util/WebUtil.js';
import FieldGroupCntlr from './FieldGroupCntlr.js';

/**
 * make a promise for this field key to guarantee that all async validation has completed
 * @param fields the fields
 * @param fieldKey the field key to convert to non async
 * @return Promise
 */
var makeValidationPromise= function(fields,fieldKey) {
    if (fields[fieldKey].mounted && fields[fieldKey].asyncUpdatePromise) {
        return fields[fieldKey].asyncUpdatePromise;
    }
    else {
        return Promise.resolve(fieldKey);
    }
};

/**
 *
 * @return Promise with the valid state true/false
 */
var validateSingle= function(groupKey, doneCallback) {
    var fields= getGroupFields(groupKey);
    if (!fields) return Promise.resolve(true);
    return Promise.all( Object.keys(fields).map( (fieldKey) => makeValidationPromise(fields,fieldKey),this ) )
        .then( (allResults) =>
        {
            var valid = allResults.every(
                (result) => {
                    var fieldKey;
                    if (typeof result==='string') {
                        fieldKey= result;
                    }
                    else if (typeof result==='object' && result.fieldKey){
                        fieldKey= result.fieldKey;
                    }
                    else {
                        throw new Error('could not find fieldKey from promise results');
                    }
                    var f = fields[fieldKey];
                    return (f.valid !== undefined && f.mounted) ? f.valid : true;
                });
            doneCallback(valid);
        }
    ).catch( (e) => logError(e));
};

//var validateGroup= function(groupKeyAry, doneCallback) {
//   //todo
//};
var validateGroup= function() {
    //todo
};

var validate= function(groupKey, doneCallback) {
    if (Array.isArray(groupKey)) {
        validateGroup(groupKey,doneCallback);
    }
    else {
        validateSingle(groupKey,doneCallback);
    }
};


/**
 *
 * @param {string} groupKey the group key for the fieldgroup
 * @return {{}}
 */
var getResults= function(groupKey) {
    var fields= getGroupFields(groupKey);
    return Object.keys(fields).
        filter((fieldKey) => fields[fieldKey].mounted).
        reduce((request, key) => {
            request[key] = fields[key].value;
            return request;
        }, {});
};


/**
 * Get the group state for a key
 *
 * @param {string} groupKey
 * @return {object}
 */
const getGroupState= function(groupKey) {
    var fieldGroupMap= flux.getState()[FieldGroupCntlr.FIELD_GROUP_KEY].fieldGroupMap;
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

const defaultReducer= (state) => state;

/**
 *
 * @param {string} groupKey
 * @param {function} reducerFunc
 * @param {boolean} keepState
 */
const initFieldGroup= function(groupKey,reducerFunc= defaultReducer,keepState=false) {
    flux.process({type: FieldGroupCntlr.INIT_FIELD_GROUP, payload: {groupKey,reducerFunc,keepState}});
};

/**
 *
 * @param {string} groupKey
 * @param {function} reducerFunc
 * @param {boolean} keepState
 */
const mountFieldGroup= function(groupKey, reducerFunc= defaultReducer, keepState= false) {
    flux.process({type: FieldGroupCntlr.MOUNT_FIELD_GROUP,
                  payload: {groupKey, reducerFunc, keepState, mounted:true}
    });
};
/**
 *
 * @param groupKey
 */
const unmountFieldGroup= function(groupKey) {
    flux.process({type: FieldGroupCntlr.MOUNT_FIELD_GROUP,
        payload: {groupKey, mounted:false}
    });
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







var FieldGroupUtils= {validate, getResults, getGroupState, getGroupFields,
                      initFieldGroup,mountFieldGroup,unmountFieldGroup, bindToStore };

export default FieldGroupUtils;

