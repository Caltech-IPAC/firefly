/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

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

const valueChangeActionCreator= function(rawAction) {
    return (dispatcher) => {
        dispatcher(rawAction);
        if (rawAction.payload.asyncUpdatePromise) {
            rawAction.payload.asyncUpdatePromise.then((payload) => {
                dispatcher({type: VALUE_CHANGE, payload});
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
function reducer(state={fieldGroupMap: {}}, action={}) {

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
        default:
            fireOnAllMountedGroups(state,action);
            break;
    }
    return retState;
}



const fireOnAllMountedGroups= function(state,action) {
    var changes;
    var retState= state;

    Object.keys(state.fieldGroupMap).forEach((groupKey)=> {
        var fg= state.fieldGroupMap[groupKey];
        if (fg.mounted) {
            if (!changes) changes= {};
            var fields= fireReducer(fg,action);
            changes[groupKey]= Object.assign({},fg,{fields});
        }
    });

    if (changes) {
        retState= {};
        retState.fieldGroupMap= Object.assign({},state.fieldGroupMap,changes);

    }
    return retState;
};


const initFieldGroup= function(state,action) {
    var {groupKey, reducerFunc, keepState}= action.payload;

    var fields= reducerFunc ? reducerFunc(null, action) : {};

    var fg= constructFieldGroup(fields,reducerFunc,keepState);
    return createState(state.fieldGroupMap,groupKey,fg);
};

const updateFieldGroupMount= function(state,action) {
    var {groupKey, mounted}= action.payload;
    if (!groupKey) return state;

    var retState= state;

    if (mounted) {
        if (isFieldGroupDefined(state,groupKey)) {
            fg= findAndCloneFieldGroup(state, groupKey, {mounted:true});
            retState= createState(state.fieldGroupMap,groupKey,fg);
        }
        else {
            retState= this.initFieldGroup(state,action);
            retState.fieldGroupMap[groupKey].mounted= true;
        }
    }
    else {
        if (isFieldGroupDefined(state,groupKey)) {
            var fg= findAndCloneFieldGroup(state, groupKey, {mounted:false});
            if (!fg.keepState) fg.fields= null;
            retState= createState(state.fieldGroupMap,groupKey,fg);
        }
    }
    return retState;
};

/**
 * @param {object} fg the field group
 * @param {object} action - the action to fire
 * fire the reducer for field group if it has been defined
 */
const fireReducer= function(fg, action) {
    return  fg.reducerFunc ? fg.reducerFunc(fg.fields, action) : Object.assign({},fg.fields);
};

const valueChange= function(state,action) {
    var {fieldKey,newValue,displayValue,extraData,groupKey}= action.payload;

    if (!getFieldGroup(state,groupKey)) return state;

    var fg= findAndCloneFieldGroup(state, groupKey);

    fg.fields[fieldKey]=  cloneField(fg.fields[fieldKey],
        {
            message: action.payload.message||'',
            valid : (action.payload.hasOwnProperty('valid') ? action.payload.valid :true),
            value : newValue,
            asyncUpdatePromise : action.payload.asyncUpdatePromise||false,
            displayValue,
            extraData
        });


    fg.fields= fireReducer(fg, action);

    return createState(state.fieldGroupMap,groupKey,fg);



    //fields[payload.fieldKey]=
    //    {
    //        ...fields[payload.fieldKey],
    //        message :payload.message||"",
    //        valid : (payload.hasOwnProperty('valid') ? payload.valid :true),
    //        value : payload.newValue,
    //        asyncUpdatePromise : payload.asyncUpdatePromise||false,
    //        displayValue : payload.displayValue,
    //        extraData: payload.extraData
    //    };

};






const updateMount= function(state, action) {
    var {fieldKey,mounted,value,fieldState,displayValue,extraData,groupKey}= action.payload;
    if (!getFieldGroup(state,groupKey)) return state;

    var fg= findAndCloneFieldGroup(state,groupKey);
    var fields= fg.fields;
    var newField;

    if (mounted && !fields[fieldKey]) {
        var old= fieldState||{};
        newField= Object.assign({},old, { fieldKey, mounted, value, displayValue, extraData } );
    }
    else {
        newField= cloneField(fields[fieldKey],{ mounted, value, displayValue });
    }
    fields[fieldKey]= newField;


    if (typeof fields[fieldKey].valid === 'undefined') {
        newField.valid= true;
    }
    return createState(state.fieldGroupMap,groupKey,fg);
};


//============ private utilities =================================
//============ private utilities =================================
//============ private utilities =================================

const createState= function(oldState,groupKey,fieldGroup) {

    var retState= {fieldGroupMap : Object.assign({},oldState.fieldGroupMap)};
    retState.fieldGroupMap[groupKey]= fieldGroup;
    return retState;
};


function cloneField(field, newKeys={}) { return Object.assign({},field,newKeys); }

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
    return groupKey && state.fieldGroupMap[groupKey] && state.fieldGroupMap[groupKey].fields;
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
    return state.fieldGroupMap[groupKey];
}

const constructFieldGroup= function(fields, reducerFunc, keepState) {
    fields= fields || {};

    Object.keys(fields).forEach( (key) => {
        if (typeof fields[key].valid === 'undefined') {
            fields[key].valid= true;
        }
    });

    return { fields,
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

