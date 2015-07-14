/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*jshint browserify:true*/
"use strict";



import FieldGroupActions from '../actions/FieldGroupActions.js'
import ImagePlotActions from '../actions/ImagePlotsActions.js'
import {application} from '../core/Application.js';

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
           });
    }


    initFieldGroup(payload) {
        if (!payload) return;
        var {groupKey, reducerFunc, validatorFunc, keepState}= payload;

        var field= null;
        if (this.fieldGroupMap[groupKey]) {
            fields= this.fieldGroupMap[groupKey].fields;
        }

        var fields= reducerFunc ? reducerFunc(null, FormActions.INIT_FIELD_GROUP) : {};

        this.fieldGroupMap[groupKey]= {
            fields : [],
            reducerFunc,
            validatorFunc,
            keepState,
            mounted : false,
            fieldGroupValid: false
        };
    }

    updateFieldGroupMount(payload) {
        if (!payload) return;
        var {groupKey, mounted}= payload;

        if (payload.mounted) {
            initFieldGroup(payload);
        }
        else {
            if (groupKey && this.fieldGroupMap[groupKey]) {
                var fg= this.fieldGroupMap[groupKey];
                fg.mounted= mounted;
                if (!fg.keepState) fg.fields= null;
            }
        }
    }


    updateData(payload) {
        if (!payload.groupKey || this.fieldGroupMap[payload.groupKey]) return;
        var fg= this.fieldGroupMap[payload.groupkey];
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


        fireReducer(fg, FieldGroupActions.VALUE_CHANGE);
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
        if (!payload.groupKey || this.fieldGroupMap[payload.groupKey]) return;
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


    allPlotUpdate() {
        fireAll(ImagePlotActions.ANY_CHANGE);
    }

    fireAll(actionConst) {
        //TODO: finish this, go through a loop and get all that are active

        Object.keys(fields).forEach((groupKey)=> {
            var fg= this.fieldGroupMap[groupKey];
            if (fg.mounted) fireReducer(fg);
        });
    }

    /**
     * @param fg  fieldgroup
     */
    fireReducer(fg, acitonConst) {
        if (fg.reducerFunc) fg.fields = fg.reducerFunc(fields, acitonConst);
    }

}

export default application.alt.createStore(FieldGroupStore, 'FieldGroupStore' );

