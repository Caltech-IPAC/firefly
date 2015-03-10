/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
/*jshint browserify:true*/
"use strict";
var AmpersandState = require('ampersand-state');
var icepick= require("icepick");
var Promise= require("es6-promise").Promise;
var _= require("underscore");



//var Field= exports.Fields = AmpersandState.extend({
//    props: {
//        fieldKey : 'string',           // required, no default
//        value : 'string',             // default ""
//        validator : 'object',         // default none
//        valid : 'boolean'            // default true
//        message : 'string',         // default ""
//        visible : 'boolean',        // field is visible, default true
//        mounted : 'boolean/,    // field is mounted, default false
//        asyncUpdate : 'boolean' field is in a async update, default false
//    }
//
//});

/*
 * Payload Types:
 *    valueChange
 *           evType: valueChange
 *           newValue: the new value as a string
 *           fieldState: the fieldState object
 *
 *    valueChange
 *           evType: mountComponent
 *           fieldKey : 'string',
 *           mounted:  'boolean'
 *
 *    updateAllValues
 *           evType: valueChange
 *           requestObj: key/value pair object
 *
 *    validateForm
 *           evType: validateForm
 *
 *    validateAndSubmitForm
 *           evType: validateAndSubmitForm
 *           submitFunction : function to call with all active form values
 *
 */


var FormModel= exports.FormModel= AmpersandState.extend(
{
    extraProperties : 'allow',
    dispatcher : null,
    dispatchToken : null,

    props: {
        //formData :  {type : 'object', default : null }
    },

    session : {
        //formValidatorAry : {type : 'array', default : [] }
    },

    initialize : function() {
        _.each(this.attributes, function(obj) {
            icepick.freeze(obj);
        }, this);
    },

    initDispatcher : function(dispatcher) {
        this.dispatcher= dispatcher;
        this.dispatchToken= this.dispatcher.register(this.dispatchCallback.bind(this) );
    },

    releaseDispatcher : function() {
        if (this.dispatchToken) {
            this.dispatcher.unregister(this.dispatchToken);
        }
    },

    dispatchCallback : function(payload) {
        switch (payload.evType) {
            case 'valueChange' :
                this.updateData(payload);
                break;
            case 'mountComponent' :
                this.updateMount(payload.fieldKey,payload.mounted,payload.value,payload.fieldState);
                break;
            case 'validateAndSubmitForm' :
                break;
            case 'validateForm' :
                break;
        }
    },

    updateData : function(payload) {
        //var validateState= {valid:true, message:""};
        //
        //if (fieldState.validator) {
        //    validateState= fieldState.validator(newValue);
        //}

        this.set(payload.fieldKey, icepick.assign(this[payload.fieldKey], payload.fieldState,
                                     {
                                       message :payload.message||"",
                                       valid : (payload.hasOwnProperty('valid') ? payload.valid :true),
                                       value : payload.newValue,
                                       asyncUpdate : payload.asyncUpdate||false
                                     }));

    },

    updateMount : function(fieldKey,mounted,value,fieldState) {
        if (mounted && !this[fieldKey]) {
            var old= fieldState||{};
            this.set(fieldKey, icepick.assign(old,
                                              { fieldKey : fieldKey,
                                                  mounted : mounted,
                                                  value : value
                                              } ));
            icepick.freeze(this[fieldKey]);
        }
        else {
            this[fieldKey]= icepick.assign(this[fieldKey],
                                           {
                                               mounted : mounted,
                                               value : value
                                           });
        }
    },


    /**
     *
     * @return Promise with the valid state true/false
     */
    validateForm : function() {
        return Promise.all(
                _.keys(this.attributes).map(function(fieldKey) {
                    return this.makeValidationPromise(fieldKey);
                },this)
        ).then(
                function(syncKeys){
                    var valid= syncKeys.every(
                            function(fieldKey) {
                                return this[fieldKey].valid !== undefined ?
                                                 this[fieldKey].valid : true;
                            },this);
                    return valid;
                }.bind(this));

    },

    /**
     * make a promise for this field key to guarantee that all async validation has completed
     * @param fieldKey the field key to convert to non async
     * @return Promise
     */
    makeValidationPromise : function(fieldKey) {
        var retval;
        if (this[fieldKey].mounted && this[fieldKey].asyncUpdate) {
            retval= new Promise( function(resolve) {
                var changeFunc= function() {
                    if (!this[fieldKey].asyncUpdate) {
                        this.off('change:'+fieldKey,changeFunc);
                        resolve(fieldKey);
                    }
                };
                this.on('change:'+fieldKey,changeFunc,this);
            }.bind(this));
        }
        else {
            retval= Promise.resolve(fieldKey);
        }
        return retval;
    },

    //makeValidationPromiseOLD : function(fieldKey) {
    //    return new Promise(
    //            function(resolve,reject) {
    //                if (this[fieldKey].valid && this[fieldKey].mounted) {
    //                    if (this[fieldKey].asyncUpdate) {
    //                        var changeFunc= function() {
    //                            this.off('change:'+fieldKey,changeFunc);
    //                            if (!this[fieldKey].asyncUpdate) {
    //                                resolve(fieldKey);
    //                            }
    //                        };
    //                        this.on('change:'+fieldKey,changeFunc,this);
    //                    }
    //                    else {
    //                        resolve(fieldKey);
    //                    }
    //                }
    //                else {
    //                    resolve(fieldKey);
    //                }
    //            }.bind(this));
    //},





    //todo: make a promise here
    getResults : function() {
        // do a promise here
        //this.fields.map




    }

});


exports.makeInputFormModel= function() {
    return new FormModel();
};