/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*jshint browserify:true*/
"use strict";



import FieldGroupActions from '../actions/FieldGroupActions.js'
import ImagePlotActions from '../actions/ImagePlotsActions.js'
import alt from '../core/AppAlt.js';

var Promise= require("es6-promise").Promise;



//var Field= exports.Fields = AmpersandState.extend({
//    props: {
//        fieldKey : 'string',           // required, no default
//        value : 'string',             // default ""
//        validatorFunc : 'function',         // default none
//        valid : 'boolean'            // default true
//        message : 'string',         // default ""
//        visible : 'boolean',        // field is visible, default true
//        mounted : 'boolean/,    // field is mounted, default false
//        asyncUpdatePromise : 'boolean' field is in a async update, default false
//    }
//
//});



export class FieldGroup {
    constructor(fields, reducerFunc,validatorFunc,keepState) {
        this.fields= fields||{};
        this.reducerFunc= reducerFunc;
        this.validatorFunc= validatorFunc;
        this.keepState= keepState;
        this.mounted = false;
        this.fieldGroupValid= false;

        Object.keys(fields).forEach( key => {
            if (typeof fields[key].valid === 'undefined') {
                fields[key].valid= true;
            }
        } );

    }
}


class FieldGroupStore {

    constructor() {
        this.fieldGroupMap= {}; // this is the whole state, change to set state when I update alt

        this.bindListeners({
            initFieldGroup: FieldGroupActions.initFieldGroup,
            updateData: FieldGroupActions.valueChange,
            updateMount: FieldGroupActions.mountComponent,
            updateFieldGroupMount: FieldGroupActions.mountFieldGroup,
            validateFieldGroup : FieldGroupActions.validateFieldGroup,
            allPlotUpdate : ImagePlotActions.anyChange
        });
        this.exportPublicMethods({
            getResults: this.getResults.bind(this),
            getGroupState: this.getGroupState.bind(this),
            getGroupFields : this.getGroupFields.bind(this)
        });
    }


    initFieldGroup(payload) {
        if (!payload) return;
        var {groupKey, reducerFunc, validatorFunc, keepState}= payload;

        var fields= null;
        if (this.fieldGroupMap[groupKey]) {
            fields= this.fieldGroupMap[groupKey].fields;
        }

        fields= reducerFunc ? reducerFunc(null, groupKey, FieldGroupActions.INIT_FIELD_GROUP) : {};

        this.fieldGroupMap[groupKey]= new FieldGroup(fields,reducerFunc,validatorFunc,keepState);
    }

    updateFieldGroupMount(payload) {
        if (!payload) return;
        var {groupKey, mounted}= payload;

        if (payload.mounted) {
            this.initFieldGroup(payload);
            this.fieldGroupMap[groupKey].mounted= true;
        }
        else {
            if (groupKey && this.fieldGroupMap[groupKey]) {
                var fg= this.fieldGroupMap[groupKey];
                fg.mounted= mounted;
                if (!fg.keepState) fg.fields= null;
            }
        }
        return false;
    }


    updateData(payload) {
        if (!payload.groupKey && !this.fieldGroupMap[payload.groupKey]) return;
        var fg= this.fieldGroupMap[payload.groupKey];
        fg.fieldGroupValid= false;

        //var validateState= {valid:true, message:""};
        //
        //if (fieldState.validatorFunc) {
        //    validateState= fieldState.validatorFunc(newValue);
        //}
        var fields= fg.fields;

        fields[payload.fieldKey]=  Object.assign({},fields[payload.fieldKey], payload.fieldState,
            {
                message :payload.message||"",
                valid : (payload.hasOwnProperty('valid') ? payload.valid :true),
                value : payload.newValue,
                asyncUpdatePromise : payload.asyncUpdatePromise||false,
                displayValue : payload.displayValue,
                extraData: payload.extraData
            });


        this.fireReducer(fg, payload.groupKey, FieldGroupActions.VALUE_CHANGE);
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

    }

    updateMount(payload) {
        if (!payload.groupKey && !this.fieldGroupMap[payload.groupKey]) return;
        var fg= this.fieldGroupMap[payload.groupKey];

        var {fieldKey,mounted,value,fieldState,displayValue,extraData}= payload;
        var fields= fg.fields;

        if (mounted && !fields[fieldKey]) {
            var old= fieldState||{};
            fields[fieldKey]= Object.assign({},old, {
                fieldKey,
                mounted,
                value,
                displayValue,
                extraData
            } );
            //Object.freeze(fields[fieldKey]);
        }
        else {
            fields[fieldKey]= Object.assign({},fields[fieldKey], {
                mounted, value, displayValue,
            });
        }
        if (typeof fields[fieldKey].valid === 'undefined') {
            fields[fieldKey].valid= true;
        }
        return false;
    }

    /**
     *
     * @return Promise with the valid state true/false
     */
    validateFieldGroup(groupKey) {
        var fg= this.fieldGroupMap[groupKey];
        var fields= fg.fields;
        var evEmmitter= this.getInstance().getEventEmitter();
        return Promise.all( Object.keys(fields).map( (fieldKey => makeValidationPromise(fields,fieldKey)),this ) )
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
                                throw(new Error('could not find fieldKey from promise results'));
                            }
                            var f = fields[fieldKey];
                            return (f.valid !== undefined && f.mounted) ? f.valid : true;
                        });
                    fg.valid= valid;
                    evEmmitter.emit('fieldGroupValid');
                }
            ).catch(e => console.log(e));
    }


    /**
     * make a promise for this field key to guarantee that all async validation has completed
     * @param fieldKey the field key to convert to non async
     * @return Promise
     */
    makeValidationPromise(fields,fieldKey) {
        if (fields[fieldKey].mounted && fields[fieldKey].asyncUpdatePromise) {
            return fields[fieldKey].asyncUpdatePromise;
        }
        else {
            return Promise.resolve(fieldKey);
        }
    }

    /**
     * Export this method.
     *
     * @return {{}}
     */
    getResults(groupKey) {
        var fg= this.fieldGroupMap[groupKey];
        var fields= fg.fields;
        var request= {};
        Object.keys(fields).forEach(function(fieldKey) {
            request[fieldKey] = fields[fieldKey].value;
        },this);
        return request;
    }

    /**
     * Export this method.
     *
     * @return {{}}
     */
    getGroupState(groupKey) {
        return this.fieldGroupMap[groupKey] ? this.fieldGroupMap[groupKey] : null;
    }
    getGroupFields(groupKey) {
        var retval= null;
        if (this.fieldGroupMap[groupKey] && this.fieldGroupMap[groupKey].fields) {
            retval= this.fieldGroupMap[groupKey].fields;
        }
        return retval;
    }

    allPlotUpdate() {
        this.fireAll(ImagePlotActions.ANY_CHANGE);
    }

    fireAll(actionConst) {
        //TODO: finish this, go through a loop and get all that are active

        Object.keys(this.fieldGroupMap).forEach((groupKey)=> {
            var fg= this.fieldGroupMap[groupKey];
            if (fg.mounted) this.fireReducer(fg,groupKey,actionConst);
        });
    }

    /**
     * @param fg  fieldgroup
     */
    fireReducer(fg, groupKey, acitonConst) {
        if (fg.reducerFunc) fg.fields = fg.reducerFunc(fg.fields, groupKey, acitonConst);
    }

}

export default alt.createStore(FieldGroupStore, 'FieldGroupStore' );

