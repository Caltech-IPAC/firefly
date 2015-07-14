/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*jshint browserify:true*/
"use strict";



import FormActions from '../actions/FormActions.js'

var Promise= require("es6-promise").Promise;



//var Field= exports.Fields = AmpersandState.extend({
//    props: {
//        fieldKey : 'string',           // required, no default
//        value : 'string',             // default ""
//        validator : 'object',         // default none
//        valid : 'boolean'            // default true
//        message : 'string',         // default ""
//        visible : 'boolean',        // field is visible, default true
//        mounted : 'boolean/,    // field is mounted, default false
//        asyncUpdatePromise : 'boolean' field is in a async update, default false
//    }
//
//});

class InputFormBaseStore {

    constructor() {
        this.fields= {},
        this.formValid= false,
        this.formKey= 'UNKNOWN'

        this.bindListeners({
            updateData: FormActions.valueChange,
            updateMount: FormActions.mountComponent,
            validateForm : FormActions.validateForm
        });
        this.exportPublicMethods({
             getResults: this.getResults.bind(this),
           });
    }

    initialize()  {
        var fields= this.fields;

        Object.keys(fields).forEach((key)=> {
            icepick.freeze(fields[key]);
        });
    }

    updateData(payload) {
        if (!payload.formKey || payload.formKey!==this.formKey) return;
        this.formValid= false;

        //var validateState= {valid:true, message:""};
        //
        //if (fieldState.validator) {
        //    validateState= fieldState.validator(newValue);
        //}
        var fields= this.fields;

        fields[payload.fieldKey]=  Object.assign({},fields[payload.fieldKey], payload.fieldState,
            {
                message :payload.message||"",
                valid : (payload.hasOwnProperty('valid') ? payload.valid :true),
                value : payload.newValue,
                asyncUpdatePromise : payload.asyncUpdatePromise||false,
                displayValue : payload.displayValue,
                extraData: payload.extraData
            });
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
        if (!payload.formKey || payload.formKey!==this.formKey) return;
        this.formValid= false;

        var {fieldKey,mounted,value,fieldState,displayValue,extraData}= payload;
        var fields= this.fields;

        if (mounted && !fields[fieldKey]) {
            var old= fieldState||{};
            fields[fieldKey]= Object.assign({},old, {
                fieldKey,
                mounted,
                value,
                displayValue,
                extraData
            } );
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
    validateForm() {
        var fields= this.fields;
        var store= this;
        return Promise.all( Object.keys(fields).map( this.makeValidationPromise,this ) )
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
                    store.setState({formValid: valid});
                    store.getInstance().getEventEmitter().emit('formValid');
                }
            ).catch(e => console.log(e));
    }


    /**
     * make a promise for this field key to guarantee that all async validation has completed
     * @param fieldKey the field key to convert to non async
     * @return Promise
     */
    makeValidationPromise(fieldKey) {
        var retval;
        var fields= this.fields;
        if (fields[fieldKey].mounted && fields[fieldKey].asyncUpdatePromise) {
            retval= fields[fieldKey].asyncUpdatePromise;
        }
        else {
            retval= Promise.resolve(fieldKey);
        }
        return retval;
    }

    /**
     * Export this method.
     *
     * @return {{}}
     */
    getResults() {
        var fields= this.fields;
        var request= {};
        Object.keys(fields).forEach(function(fieldKey) {
            request[fieldKey] = fields[fieldKey].value;
        },this);
        return request;
    }

}

export default InputFormBaseStore;

