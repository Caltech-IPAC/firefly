/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {take} from 'redux-saga/effects';
import {omit,get} from 'lodash';
import {clone} from '../util/WebUtil.js';
import {revalidateFields} from './FieldGroupUtils.js';
import {smartMerge} from '../tables/TableUtil.js';

/**
 * Reducer for 'fieldGroup' key
 */

/**
 * @typedef {Object.<String,FieldGroup>} FieldGroupStore
 *
 * @global
 * @public
 */

/**
 * @typedef {Object} FieldGroupField
 *
 * @prop {String} fieldKey the field id, must be unique in the group
 * @prop {String} groupKey the group id, must be unique
 * @prop {String|Promise|*} value the value, can be anything including promise, typically a string
 * @prop {boolean} valid the group id, must be unique
 * @prop {Function} validator
 * @prop {boolean} mounted field is mounted
 * @prop {boolean} nullAllowed, default to true
 *
 * @global
 * @public
 */

/**
 * @typedef {Object} FieldGroup
 *
 * @prop {String} groupKey
 * @prop {FieldGroupField[]} fields
 * @prop {Function} reducerFunc
 * @props {String[]} actionTypes any action types (beyond the FieldGroup action types) that the reducer should allow though
 * @prop {boolean} keepState
 * @prop {boolean} mounted field is mounted
 * @prop {String} wrapperGroupKey
 * @prop {boolean} fieldGroupValid
 *
 * @global
 * @public
 */





export const INIT_FIELD_GROUP= 'FieldGroupCntlr/initFieldGroup';
export const MOUNT_COMPONENT= 'FieldGroupCntlr/mountComponent';
export const MOUNT_FIELD_GROUP= 'FieldGroupCntlr/mountFieldGroup';
export const VALUE_CHANGE= 'FieldGroupCntlr/valueChange';
export const MULTI_VALUE_CHANGE= 'FieldGroupCntlr/multiValueChange';
export const RESTORE_DEFAULTS= 'FieldGroupCntlr/restoreDefaults';
export const CHILD_GROUP_CHANGE= 'FieldGroupCntlr/childGroupChange';
export const RELATED_ACTION= 'FieldGroupCntlr/relatedAction';


export const FIELD_GROUP_KEY= 'fieldGroup';

export default {
    reducers () {return {[FIELD_GROUP_KEY]: reducer};},

    actionCreators() {
        return {
            [VALUE_CHANGE] : valueChangeActionCreator,
            [MULTI_VALUE_CHANGE]: multiValueChangeActionCreator
        };
    },
    VALUE_CHANGE, CHILD_GROUP_CHANGE,
    MOUNT_FIELD_GROUP
};


//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================
//======================================== Dispatch Functions =============================

/**
 * This will init a field group. In general, you should not need to use this.  Mount does the same thing and is automatic
 * through the FieldGroupConnector. Have a good reason if you are using this action
 * 
 * @param groupKey
 * @param keepState
 * @param initValues
 * @param reducerFunc
 * @param actionTypes any action types (beyond the FieldGroup action types) that the reducer should allow though
 */
export function dispatchInitFieldGroup(groupKey,keepState=false, 
                                       initValues=null, 
                                       reducerFunc= null,
                                       actionTypes=[]) {

    flux.process({type: INIT_FIELD_GROUP, payload: {groupKey,reducerFunc, actionTypes, 
                                                    initValues, keepState}});
}



/**
 *
 * @param groupKey
 * @param mounted
 * @param keepState
 * @param initValues
 * @param reducerFunc
 * @param actionTypes any action types (beyond the FieldGroup action types) that the reducer should allow though
 * @param wrapperGroupKey
 */
export function dispatchMountFieldGroup(groupKey, mounted, keepState= false, 
                                        initValues=null, reducerFunc= null,
                                        actionTypes=[], wrapperGroupKey, forceUnmount = false) {
    flux.process({type: MOUNT_FIELD_GROUP, payload: {groupKey, mounted, initValues, reducerFunc, 
                                                     actionTypes, keepState, wrapperGroupKey, forceUnmount} });
}


/**
 * 
 * @param groupKey
 * @param fieldKey
 * @param mounted
 * @param value
 * @param initFieldState
 */
export function dispatchMountComponent(groupKey,fieldKey,mounted,value,initFieldState) {
        flux.process({
            type: MOUNT_COMPONENT, payload: { groupKey, fieldKey, mounted, value, initFieldState }
        });
}

/**
 * the required parameter are below, anything else passed is put in the field group as well
 *
 * @param {Object} payload
 * @param {String} payload.fieldKey the field Key
 * @param {String} payload.groupKey group key
 * @param {String} payload.value value can be anything including a promise or function
 * @param {String} payload.valid - true if valid, default to true
 */
export function dispatchValueChange(payload) {
    flux.process({type: VALUE_CHANGE, payload});
}


/**
 * Update mutiliple fields
 * @param {String} groupKey
 * @param {FieldGroupField[]} fieldAry
 */
export function dispatchMultiValueChange(groupKey, fieldAry) {
    flux.process({type: MULTI_VALUE_CHANGE, payload:{groupKey, fieldAry}});
}

/**
 * Restore defaults
 * @param {String} groupKey
 */
export function dispatchRestoreDefaults(groupKey) {
    flux.process({type: RESTORE_DEFAULTS, payload:{groupKey}});
}



//======================================== Action Creators =============================
//======================================== Action Creators =============================
//======================================== Action Creators =============================


/**
 *
 * @param {Action} rawAction
 * @return {Function}
 */
function valueChangeActionCreator(rawAction) {
    return (dispatcher) => {
        const {value}= rawAction.payload;
        dispatcher(rawAction);
        if (value && value.then) {
            const {fieldKey,groupKey}= rawAction.payload;
            value.then((payload) => {
                    dispatcher({
                        type: VALUE_CHANGE,
                        payload: Object.assign({}, payload, {fieldKey, groupKey})
                    });
            }).catch((e) => console.log(e));
        }
    };
};



/**
 *
 * @param {Action} rawAction
 * @return {Function}
 */
function multiValueChangeActionCreator(rawAction) {
    return (dispatcher) => {
        const {groupKey, fieldAry}= rawAction.payload;
        dispatcher(rawAction);
        fieldAry
            .filter((f) => f.value && f.value.then)
            .forEach((f) => {
                f.value.then((payload) => {
                        dispatcher({
                            type: VALUE_CHANGE,
                            payload: Object.assign({}, payload, {fieldKey: f.fieldKey, groupKey})
                        });
                    })
                    .catch((e) => console.log(e));
            });
    };
};

//======================================== Saga =============================
//======================================== Saga =============================
//======================================== Saga =============================


export function* watchForRelatedActions(params, dispatcher, getState ) {
    var action=  yield take();
    var state;
    while(true) {
        state= getState()[FIELD_GROUP_KEY];
        Object.keys(state).forEach((groupKey)=> {
            var fg= state[groupKey];
            if (fg.mounted && fg.actionTypes.includes(action.type)) {
                dispatcher({type:RELATED_ACTION, payload:{fieldGroup:fg, originalAction:action}});
            }
        });

        action=  yield take();
    }
}





//======================================== Reducer =============================
//======================================== Reducer =============================
//======================================== Reducer =============================


/**
 * main reducer
 * @param {Object} state
 * @param {Action} action
 * @return {Object} return state
 */
function reducer(state={}, action={}) {

    if (!action.payload || !action.type) return state;

    var retState= state;
    switch (action.type) {
        case INIT_FIELD_GROUP  :
            retState= initFieldGroup(state, action);
            break;
        case MOUNT_COMPONENT  :
            retState= updateMount(state, action);
            break;
        case MOUNT_FIELD_GROUP  :
            retState= updateFieldGroupMount(state, action);
            break;
        case VALUE_CHANGE  :
            retState= valueChange(state, action);
            break;
        case MULTI_VALUE_CHANGE:
            retState= multiValueChange(state, action);
            break;
        case RESTORE_DEFAULTS:
            retState= restoreDefaults(state, action);
            break;
        case RELATED_ACTION:
            retState= relatedAction(state,action);
            break;
    }
    return retState;
}


/**
 *
 * @param {Object} state
 * @param {Action} action
 * @return {*}
 */
function relatedAction(state,action) {
    const {originalAction,fieldGroup}= action.payload;
    const newFg= Object.assign({},fieldGroup,{fields:fireFieldsReducer(fieldGroup,originalAction)});
    return Object.assign({},state,{[newFg.groupKey]:newFg});
}



function addInitValues(fields,initValues) {
    fields= Object.assign({},fields);
    return Object.keys(initValues).reduce( (obj,key)=> {
        if (!fields[key]) fields[key]= {};
        fields[key].value= initValues[key];
        fields[key].fieldKey= key;
        fields[key].mounted= true;
        return fields;
    },fields);
}


/**
 *
 * @param {Object} state
 * @param {Action} action
 */
function initFieldGroup(state,action) {
    var {groupKey, reducerFunc, actionTypes=[], keepState, initValues,wrapperGroupKey}= action.payload;

    const mounted= get(state, [groupKey,'mounted'],false);

    var fields= reducerFunc ? reducerFunc(null, action) : {};

    if (initValues) {
        fields= addInitValues(fields,initValues);
    }


    var fg= constructFieldGroup(groupKey,fields,reducerFunc,actionTypes, keepState, wrapperGroupKey);
    fg.mounted= mounted;
    fg.initFields= Object.keys(fg.fields).map( (key) => fg.fields[key]);
    return clone(state,{[groupKey]:fg});
}

const updateFieldGroupMount= function(state,action) {
    var {groupKey, mounted, initValues, wrapperGroupKey, forceUnmount}= action.payload;
    if (!groupKey) return state;

    var retState= state;

    if (mounted) {
        if (isFieldGroupDefined(state,groupKey)) {
            fg= findAndCloneFieldGroup(state, groupKey, {mounted:true});
            if (wrapperGroupKey) fg.wrapperGroupKey= wrapperGroupKey;

            if (initValues) {
                fg.fields= addInitValues(fg.fields,initValues);
            }

            retState= clone(state,{[groupKey]:fg});
        }
        else {
            retState= initFieldGroup(state,action);
            retState[groupKey].mounted= true;
        }
        retState[groupKey].fields= fireFieldsReducer(retState[groupKey], action);
    }
    else {
        if (isFieldGroupDefined(state,groupKey)) {
            var fg= findAndCloneFieldGroup(state, groupKey, {mounted:false});
            if (!fg.keepState || forceUnmount) fg.fields= null;
            retState= clone(state,{[groupKey]:fg});
        }
    }
    return retState;
};

/**
 * @param {object} fg the field group
 * @param {Action} action - the action to fire
 * @return {Object} the fields
 * fire the reducer for field group if it has been defined
 */
const fireFieldsReducer= function(fg, action) {
    // return  fg.reducerFunc ? fg.reducerFunc(revalidateFields(fg.fields), action) : fg.fields;
    if (fg.reducerFunc ) {
        const newFields= fg.reducerFunc(revalidateFields(fg.fields), action);
        return smartMerge(fg.fields,newFields);
    }
    else {
        return fg.fields;
    }
};

const valueChange= function(state,action) {
    var {fieldKey, groupKey,message='', valid=true, fireReducer=true}= action.payload;

    if (!getFieldGroup(state,groupKey)) {
        state = initFieldGroup(state,action);
        state[groupKey].mounted= true;
    }

    var fg= findAndCloneFieldGroup(state, groupKey);

    const addToInit= !fg.fields[fieldKey];
    fg.fields[fieldKey]=  Object.assign({},
                                  fg.fields[fieldKey],
                                  action.payload,
                                  {message, valid});

    if (fireReducer) fg.fields= fireFieldsReducer(fg, action);
    if (addToInit) fg.initFields= [...fg.initFields,fg.fields[fieldKey]];

    var mods= {[groupKey]:fg};
   //============== Experimental parent group get notified
    if (fireReducer) {
        const modAddition= updateWrapperGroup(state,fg,groupKey,action);
        mods= clone(mods,modAddition);
    }
    //==============

    return clone(state,mods);
};

function updateWrapperGroup(state, fg, groupKey, action) {
    if (!get(state, [fg.wrapperGroupKey,'mounted'],false)) return {};

    var wrapperFg= findAndCloneFieldGroup(state, fg.wrapperGroupKey);
    const childGroups= makeChildGroups(fg.wrapperGroupKey, state);
    childGroups[groupKey]= fg.fields;
    const wrapperAction= {
        type: CHILD_GROUP_CHANGE,
        payload: { changedGroupKey: groupKey, sourceAction:action, childGroups }
    };
    wrapperFg.fields= fireFieldsReducer(wrapperFg, wrapperAction);
    return {[wrapperFg.groupKey]: wrapperFg};
}

function multiValueChange(state,action) {
    const {fieldAry,groupKey}= action.payload;

    fieldAry.forEach( (f) => state= valueChange(state,{type:VALUE_CHANGE, payload:clone(f,{groupKey,fireReducer:false})}) );

    var fg= findAndCloneFieldGroup(state, groupKey);
    fg.fields= fireFieldsReducer(fg, action);

    var mods= {[groupKey]:fg};
    //============== Experimental parent group get notified
    const modAddition= updateWrapperGroup(state,fg,groupKey,action);
    mods= clone(mods,modAddition);
    //==============

    return clone(state,mods);
}

function restoreDefaults(state,action) {
    const {groupKey}= action.payload;
    const fg= getFieldGroup(state,groupKey);
    if (!fg) return state;
    fg.initFields.forEach( (f) => {
        const {fieldKey,message,valid,value,displayValue}= f;
        state= valueChange(state,{type:VALUE_CHANGE, payload:{groupKey,fieldKey,message,valid,value,displayValue}});
    } );
    return state;
}



function makeChildGroups(wrapperGroupKey, state) {
    return Object.keys(state).reduce( (obj,key) => {
        if (state[key].wrapperGroupKey===wrapperGroupKey) {
            obj[key]= state[key].fields;
        }
        return obj;
    }, {});

}



const updateMount= function(state, action) {
    var {fieldKey,mounted,initFieldState={},groupKey}= action.payload;
    var fg= getFieldGroup(state,groupKey);
    if (!fg || (!mounted && !fg.fields)) return state;

    fg= findAndCloneFieldGroup(state,groupKey);

    if (mounted) {
        const addToInit= !fg.fields[fieldKey];
        var omitPayload= omit(action.payload, ['initFieldState','groupKey']);
        fg.fields[fieldKey]= Object.assign({valid:true,nullAllowed:true},initFieldState, fg.fields[fieldKey],
                                           omitPayload);
        if (addToInit) fg.initFields= [...fg.initFields,fg.fields[fieldKey]];
    }
    else {
        fg.fields[fieldKey]= Object.assign({},fg.fields[fieldKey],{mounted});
    }

    return clone(state,{[groupKey]:fg});
};


//============ private utilities =================================
//============ private utilities =================================
//============ private utilities =================================

const createState= function(oldState,groupKey,fieldGroup) {
    return Object.assign({},oldState, {[groupKey]:fieldGroup});
};


// function cloneField(field, newKeys={}) { return Object.assign({},field,newKeys); }

/**
 *
 *
 * @param {object} state
 * @param {string} groupKey
 * @param {object} newValues any value replacements
 */
function findAndCloneFieldGroup(state,groupKey, newValues={}) {
    var fg= getFieldGroup(state,groupKey);
    if (!fg) return undefined;
    var retFg= Object.assign({},fg,newValues);
    retFg.fields= Object.assign({},fg.fields);
    return retFg;
}

function isFieldGroupDefined(state,groupKey) {
    return groupKey && state[groupKey] && state[groupKey].fields;
}

/**
 *
 *
 * @param {object} state
 * @param {string} groupKey
 * @return {object} retState
 */
function getFieldGroup(state,groupKey) {
    if (!groupKey) return null;
    return state[groupKey];
}

/**
 *
 * @param groupKey
 * @param fields
 * @param reducerFunc
 * @param actionTypes
 * @param keepState
 * @param wrapperGroupKey
 * @return {FieldGroup}
 *
 */
function constructFieldGroup(groupKey,fields, reducerFunc, actionTypes, keepState, wrapperGroupKey) {
    fields= fields || {};

    Object.keys(fields).forEach( (key) => {
        if (typeof fields[key].valid === 'undefined') {
            fields[key].valid= true;
        }
    });

    return { groupKey,
             fields,
             reducerFunc,
             keepState,
             actionTypes,
             wrapperGroupKey,
             mounted : false,
             fieldGroupValid : false
    };
}


//============ end private utilities ===========
//============ end private utilities ===========


//============ EXPORTS ===========
//============ EXPORTS ===========


//============ EXPORTS ===========
//============ EXPORTS ===========
