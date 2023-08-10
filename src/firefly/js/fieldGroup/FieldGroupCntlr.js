/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../core/ReduxFlux.js';
import {isEqual, isUndefined} from 'lodash';
import {revalidateFields} from './FieldGroupUtils.js';
import {REINIT_APP} from '../core/AppDataCntlr.js';
import {isDefined} from 'firefly/util/WebUtil.js';

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
 * @prop {boolean} keepState
 * @prop {boolean} mounted field is mounted
 * @prop {String} wrapperGroupKey
 * @prop {boolean} fieldGroupValid
 * @prop {Object} metaState
 *
 * @global
 * @public
 */

export const INIT_FIELD_GROUP= 'FieldGroupCntlr/initFieldGroup';
export const MOUNT_COMPONENT= 'FieldGroupCntlr/mountComponent';
export const MOUNT_FIELD_GROUP= 'FieldGroupCntlr/mountFieldGroup';
export const VALUE_CHANGE= 'FieldGroupCntlr/valueChange';
export const META_STATE_CHANGE= 'FieldGroupCntlr/metaStateChange';
export const MULTI_VALUE_CHANGE= 'FieldGroupCntlr/multiValueChange';
export const RESTORE_DEFAULTS= 'FieldGroupCntlr/restoreDefaults';
export const CHILD_GROUP_CHANGE= 'FieldGroupCntlr/childGroupChange';
export const FORCE_FIELD_GROUP_REDUCER= 'FieldGroupCntlr/forceFieldGroupReducer';


const actionPrefix= 'FieldGroupCntlr';
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
 * @param {string} groupKey
 * @param {boolean} mounted - true will mount (initialize) the field group, false will unmount the field group. All
 *                            values are clear when a field group unmount unless keepState is set to true
 * @param {boolean} [keepState] - if mounted parameter is true,
 *                                then keepState defaults to false, true will set the field group hold it state even
 *                                after if is unmounted
 *                                if mounted parameter is false, then keepState defaults to the field groups value set
 *                                when mounted. If specified then it will override the previous setting.
 * @param {Function} [reducerFunc]
 * @param {string} [wrapperGroupKey]
 */
export function dispatchMountFieldGroup(groupKey, mounted, keepState, reducerFunc= undefined, wrapperGroupKey) {
    flux.process({type: MOUNT_FIELD_GROUP, payload: {groupKey, mounted, reducerFunc, keepState, wrapperGroupKey} });
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
 * @param {boolean} payload.valid - true if valid, default to true
 */
export function dispatchValueChange(payload) {
    flux.process({type: VALUE_CHANGE, payload});
}

/**
 *
 * @param {Object} payload
 * @param {String} payload.groupKey group key
 * @param {Object} payload.metaState a generated state object
 */
export function dispatchMetaStateChange(payload) {
    flux.process({type: META_STATE_CHANGE, payload});
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

/*
 * @param groupKey
 * @param relatedAction
 */
export function dispatchForceFieldGroupReducer(groupKey,relatedAction={type:FORCE_FIELD_GROUP_REDUCER,payload:{}}) {
    flux.process({type: FORCE_FIELD_GROUP_REDUCER, payload:{groupKey,relatedAction}});
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
                    dispatcher({ type: VALUE_CHANGE, payload: {...payload, fieldKey, groupKey} });
            }).catch((e) => console.log(e));
        }
    };
}



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
}

/**
 * main reducer
 * @param {Object} state
 * @param {Action} action
 * @return {Object} return state
 */
function reducer(state={}, action={}) {
    if (action.type===REINIT_APP) return {};
    if (!action.payload || !action.type || !action.type.startsWith(actionPrefix)) return state;
    switch (action.type) {
        case MOUNT_COMPONENT: return updateMount(state, action);
        case MOUNT_FIELD_GROUP: return updateFieldGroupMount(state, action);
        case VALUE_CHANGE: return valueChange(state, action);
        case META_STATE_CHANGE: return updateMetaState(state, action);
        case MULTI_VALUE_CHANGE: return multiValueChange(state, action);
        case RESTORE_DEFAULTS: return restoreDefaults(state, action);
        case FORCE_FIELD_GROUP_REDUCER: return forceFieldGroupReducer(state,action);
    }
    return state;
}


function forceFieldGroupReducer(state,action) {
    const {relatedAction,groupKey}= action.payload;
    const fg= state[groupKey];
    return {...state,[groupKey]:{...fg,fields:fireFieldsReducer(fg,relatedAction)}};
}

/**
 * @param {Object} state
 * @param {Action} action
 */
function initFieldGroup(state,action) {
    const {groupKey, reducerFunc, keepState=false, wrapperGroupKey}= action.payload;

    const mounted= state?.[groupKey]?.mounted ?? false;

    const fields= reducerFunc ? reducerFunc(undefined, action) : {};
    const fg= constructFieldGroup(groupKey,fields,reducerFunc,keepState, wrapperGroupKey);
    fg.mounted= mounted;
    fg.initFields= Object.keys(fg.fields).map( (key) => fg.fields[key]);
    return {...state,[groupKey]:fg};
}

function updateFieldGroupMount(state,action) {
    const {groupKey, mounted, reducerFunc, wrapperGroupKey, keepState}= action.payload;
    if (!groupKey) return state;

    let retState= state;

    if (mounted) {
        if (isFieldGroupDefined(state,groupKey)) {
            const fg= findAndCloneFieldGroup(state, groupKey, {mounted:true});
            if (wrapperGroupKey) fg.wrapperGroupKey= wrapperGroupKey;
            if (reducerFunc) fg.reducerFunc= reducerFunc;
            if (!isUndefined(keepState)) fg.keepState= keepState;
            retState= {...state,[groupKey]:fg};
        }
        else {
            retState= initFieldGroup(state,action);
            retState[groupKey].mounted= true;
        }
        retState[groupKey].fields= fireFieldsReducer(retState[groupKey], action);
    }
    else {
        if (isFieldGroupDefined(state,groupKey)) {
            const fg= findAndCloneFieldGroup(state, groupKey, {mounted:false});
            const doKeepState= isUndefined(keepState) ? fg.keepState : keepState;
            if (!doKeepState) {
                fg.fields= null;
                fg.metaState={};
            }
            retState= {...state,[groupKey]:fg};
        }
    }
    return retState;
}

/**
 * @param {object} fg the field group
 * @param {Action} action - the action to fire
 * @return {Object} the fields
 * fire the reducer for field group if it has been defined
 */
function fireFieldsReducer(fg, action) {
    if (!fg.reducerFunc) return fg.fields;
    const newFields = fg.reducerFunc(revalidateFields(fg.fields), action);
    return smartReplace(fg.fields,newFields);
}

/**
 * Returns old fields unless some attribute has changed.
 * @param oldFields
 * @param newFields
 * @returns {*}
 */
function smartReplace(oldFields, newFields) {
    if (!oldFields || !newFields || oldFields === newFields) return newFields;
    const newFieldsOptimized = {};
    let hasChanged = false;
    Object.entries(newFields).forEach(([k,nf]) => {
        const of = oldFields[k];
        if (!isEqual(of, nf)) {
            newFieldsOptimized[k] = nf;
            hasChanged = true;
        } else {
            newFieldsOptimized[k] = of;
        }
    });
    if (!hasChanged && (Object.keys(oldFields).length === Object.keys(newFields).length)) {
        return oldFields;
    } else {
        return newFieldsOptimized;
    }
}

function valueChange(state,action) {
    const {fieldKey, groupKey,message='', valid=true, fireReducer=true, displayValue}= action.payload;

    if (!getFieldGroup(state,groupKey)) {
        state = initFieldGroup(state,action);
        state[groupKey].mounted= true;
    }

    const fg= findAndCloneFieldGroup(state, groupKey);

    const addToInit= !fg.fields[fieldKey];
    fg.fields[fieldKey]=  { ...fg.fields[fieldKey], ...action.payload, message, valid};
    if (isDefined(displayValue)) fg.fields[fieldKey].displayValue= displayValue;

    if (fireReducer) fg.fields= fireFieldsReducer(fg, action);
    if (addToInit) fg.initFields= [...fg.initFields,fg.fields[fieldKey]];

    let mods= {[groupKey]:fg};
   //============== Experimental parent group get notified
    if (fireReducer) {
        const modAddition= updateWrapperGroup(state,fg,groupKey,action);
        mods= {...mods,...modAddition};
    }
    //==============

    return {...state,...mods};
}

function updateMetaState(state,action) {
    const {groupKey, metaState={}}= action.payload;
    return {...state,[groupKey]:findAndCloneFieldGroup(state, groupKey,{metaState})};
}

function updateWrapperGroup(state, fg, groupKey, action) {
    if (!state?.[fg.wrapperGroupKey]?.mounted) return {};

    const wrapperFg= findAndCloneFieldGroup(state, fg.wrapperGroupKey);
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

    fieldAry.forEach( (f) => state= valueChange(state,{type:VALUE_CHANGE, payload:{...f,groupKey,fireReducer:false}} ));

    const fg= findAndCloneFieldGroup(state, groupKey);
    if (!fg) return state;
    fg.fields= fireFieldsReducer(fg, action);

    let mods= {[groupKey]:fg};
    //============== Experimental parent group get notified
    const modAddition= updateWrapperGroup(state,fg,groupKey,action);
    mods= {...mods,...modAddition};
    //==============

    return {...state,...mods};
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



function updateMount(state, action) {
    const {fieldKey,mounted,initFieldState={},value,groupKey, ...rest}= action.payload;
    let fg= getFieldGroup(state,groupKey);
    if (!fg || (!mounted && !fg.fields)) return state;

    fg= findAndCloneFieldGroup(state,groupKey);

    if (mounted) {
        const addToInit= !fg.fields[fieldKey];
        fg.fields[fieldKey]= {groupKey, fieldKey, valid:true,nullAllowed:true,
                              ...initFieldState, ...fg.fields[fieldKey], ...rest, mounted:true};
        if (!isUndefined(value)) fg.fields[fieldKey].value= value;
        if (addToInit) fg.initFields= [...fg.initFields,fg.fields[fieldKey]];
    }
    else {
        fg.fields[fieldKey]= {...fg.fields[fieldKey],mounted:false};
    }

    return {...state,[groupKey]:fg};
}

// function updateReducer(state, action) {
//     const {groupKey,reducerFunc}= action.payload;
//     const fg= findAndCloneFieldGroup(state,groupKey,{reducerFunc});
//     return {...state, [groupKey]: {...fg, fields: fireFieldsReducer(fg, action)}};
// }

//============ private utilities =================================
//============ private utilities =================================
//============ private utilities =================================

/**
 * @param {object} state
 * @param {string} groupKey
 * @param {object} newValues any value replacements
 */
function findAndCloneFieldGroup(state,groupKey, newValues={}) {
    const fg= getFieldGroup(state,groupKey);
    if (!fg) return undefined;
    return {...fg,...newValues, fields:{...fg.fields}};
}

const isFieldGroupDefined= (state,groupKey) => Boolean(groupKey && state[groupKey]?.fields);

/**
 *
 *
 * @param {object} state
 * @param {string} groupKey
 * @return {object} retState
 */
const getFieldGroup= (state,groupKey) => !groupKey ? undefined : state[groupKey];

/**
 *
 * @param groupKey
 * @param fields
 * @param reducerFunc
 * @param keepState
 * @param wrapperGroupKey
 * @return {FieldGroup}
 *
 */
function constructFieldGroup(groupKey,fields={}, reducerFunc, keepState, wrapperGroupKey) {
    Object.keys(fields).forEach( (key) => {
        if (isUndefined(fields[key].valid)) fields[key].valid= true;
    });

    return { groupKey,
             fields,
             reducerFunc,
             keepState,
             metaState: {},
             wrapperGroupKey,
             mounted : false,
             fieldGroupValid : false
    };
}
