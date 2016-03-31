/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import {omit,get} from 'lodash';

/**
 * Reducer for 'fieldGroup' key
 */

//        fieldKey : 'string',           // required, no default
//        value : 'string',             // default ""
//        valid : 'boolean'            // default true
//        message : 'string',         // default ""
//        visible : 'boolean',        // field is visible, default true
//        mounted : 'boolean/,    // field is mounted, default false
//        asyncUpdatePromise : 'boolean' field is in a async update, default false
//    }
//
//});

const INIT_FIELD_GROUP= 'FieldGroupCntlr/initFieldGroup';
const MOUNT_COMPONENT= 'FieldGroupCntlr/mountComponent';
const MOUNT_FIELD_GROUP= 'FieldGroupCntlr/mountFieldGroup';
const VALUE_CHANGE= 'FieldGroupCntlr/valueChange';


const FIELD_GROUP_KEY= 'fieldGroup';

const defaultReducer= (state) => state;


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
 */
export function dispatchInitFieldGroup(groupKey,keepState=false, 
                                       initValues=null, reducerFunc= defaultReducer) {

    flux.process({type: INIT_FIELD_GROUP, payload: {groupKey,reducerFunc,initValues, keepState}});
}



/**
 * 
 * @param groupKey
 * @param mounted
 * @param keepState
 * @param initValues
 * @param reducerFunc
 */
export function dispatchMountFieldGroup(groupKey, mounted, keepState= false, 
                                        initValues=null, reducerFunc= defaultReducer) {
    flux.process({type: MOUNT_FIELD_GROUP, payload: {groupKey, mounted, initValues, reducerFunc, keepState} });
}


/**
 * 
 * @param groupKey
 * @param fieldKey
 * @param mounted
 * @param value
 * @param fieldState
 */
export function dispatchMountComponent(groupKey,fieldKey,mounted,value,fieldState) {
        flux.process({
            type: MOUNT_COMPONENT, payload: { groupKey, fieldKey, mounted, value, fieldState }
        });
}

/**
 * 
 * @param payload
 */
export function dispatchValueChange(payload) {
    flux.process({type: VALUE_CHANGE, payload});
}

//======================================== Action Creators =============================
//======================================== Action Creators =============================
//======================================== Action Creators =============================


/**
 *
 * @param rawAction
 * @return {function()}
 */
const valueChangeActionCreator= function(rawAction) {
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
 * main reducer
 * @param {object} state
 * @param {object} action
 * @return {object} return state
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
        // default:
        //     retState= fireOnAllMountedGroups(state,action);
        //     break;
    }
    return retState;
}


// todo: determine if I need to readd this

// const fireOnAllMountedGroups= function(state,action) {
//     var {type}= action;
//                                                           //todo: I really need to think about this next line
//     if (!type.startsWith('ImagePlotCntlr')) return state; //todo: determine what category of actions is allowed through
//
//     var changes;
//     var retState= state;
//
//     Object.keys(state).forEach((groupKey)=> {
//         var fg= state[groupKey];
//         if (fg.mounted) {
//             if (!changes) changes= {};
//             var fields= fireFieldsReducer(fg,action);
//             changes[groupKey]= Object.assign({},fg,{fields});
//         }
//     });
//
//     if (changes) {
//         retState= Object.assign({},state,changes);
//     }
//     return retState;
// };

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

const initFieldGroup= function(state,action) {
    var {groupKey, reducerFunc, keepState, initValues}= action.payload;

    const mounted= get(state, [groupKey,'mounted'],false);
    
    var fields= reducerFunc ? reducerFunc(null, action) : {};

    if (initValues) {
        fields= addInitValues(fields,initValues);
    }


    var fg= constructFieldGroup(groupKey,fields,reducerFunc,keepState);
    fg.mounted= mounted;
    return createState(state,groupKey,fg);
};

const updateFieldGroupMount= function(state,action) {
    var {groupKey, mounted, initValues}= action.payload;
    if (!groupKey) return state;

    var retState= state;

    if (mounted) {
        if (isFieldGroupDefined(state,groupKey)) {
            fg= findAndCloneFieldGroup(state, groupKey, {mounted:true});

            if (initValues) {
                fg.fields= addInitValues(fg.fields,initValues);
            }

            retState= createState(state,groupKey,fg);
        }
        else {
            retState= initFieldGroup(state,action);
            retState[groupKey].mounted= true;
            retState[groupKey].fields= fireFieldsReducer(retState[groupKey], action);
        }
    }
    else {
        if (isFieldGroupDefined(state,groupKey)) {
            var fg= findAndCloneFieldGroup(state, groupKey, {mounted:false});
            if (!fg.keepState) fg.fields= null;
            retState= createState(state,groupKey,fg);
        }
    }
    return retState;
};

/**
 * @param {object} fg the field group
 * @param {object} action - the action to fire
 * @return {object} the maps of fields
 * fire the reducer for field group if it has been defined
 */
const fireFieldsReducer= function(fg, action) {
    return  fg.reducerFunc ? fg.reducerFunc(fg.fields, action) : fg.fields;
};

const valueChange= function(state,action) {
    var {fieldKey, groupKey,message='', valid=true}= action.payload;

    if (!getFieldGroup(state,groupKey)) {
        state = initFieldGroup(state,action);
        state[groupKey].mounted= true;
    }

    var fg= findAndCloneFieldGroup(state, groupKey);

    fg.fields[fieldKey]=  Object.assign({},
                                  fg.fields[fieldKey],
                                  action.payload,
                                  {message, valid});

    fg.fields= fireFieldsReducer(fg, action);

    return createState(state,groupKey,fg);
};






const updateMount= function(state, action) {
    var {fieldKey,mounted,fieldState={},groupKey,valid=true}= action.payload;
    if (!getFieldGroup(state,groupKey)) return state;

    var fg= findAndCloneFieldGroup(state,groupKey);

    if (mounted) {
        var omitPayload= omit(action.payload, ['fieldState','groupKey']);
        fg.fields[fieldKey]= Object.assign({},fg.fields[fieldKey],
                                           fieldState, omitPayload,{valid});
    }
    else {
        fg.fields[fieldKey]= Object.assign({},fg.fields[fieldKey],{mounted});
    }

    return createState(state,groupKey,fg);
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

const constructFieldGroup= function(groupKey,fields, reducerFunc, keepState) {
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
             mounted : false,
             fieldGroupValid : false
    };
};


//============ end private utilities ===========
//============ end private utilities ===========


//============ EXPORTS ===========
//============ EXPORTS ===========

var FieldGroupCntlr = {reducer, FIELD_GROUP_KEY,
                       INIT_FIELD_GROUP, MOUNT_COMPONENT,
                       MOUNT_FIELD_GROUP, VALUE_CHANGE,
                       valueChangeActionCreator};
export default FieldGroupCntlr;

//============ EXPORTS ===========
//============ EXPORTS ===========

