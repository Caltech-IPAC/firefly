
/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Created by roby on 4/13/15.
 */
/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/

'use strict';

import { Store } from 'flummox';


export class FormStore extends Store {
    constructor(flux) {
        super();

        this.state = {
            fields : {}
        };

        const actions= flux.getActions('FormUpdateActions');

        this.register(actions.valueChange, this.updateData);
        this.register(actions.mountComponent, this.updateMount);
        this.register(actions.validateForm, this.validateForm);
    }


    updateData(data) {
        var {fieldKey,newValue,fieldState,asyncUpdate,valid,message,displayString}= data;
        var fields= this.state.fields;
        var oldState= fields[fieldKey];
        var changes= {
            message :message||"",
            valid : (valid||true),
            value : newValue,
            displayString : displayString,
            asyncUpdate : asyncUpdate||false
        };

        var newFieldState= icepick.assign(oldState, fieldState, changes);
        fields[fieldKey]= newFieldState;
        this.setState({fields: icepick.assign(fields)});
    }

    updateMount(data) {
        var {fieldKey,mounted,value,fieldState}= data;
        var fields= this.state.fields;
        if (mounted && !fields[fieldKey]) {
            var old= fieldState||{};
            fields[fieldKey]= icepick.assign(old,
                                         { fieldKey : fieldKey,
                                           mounted : mounted,
                                           value : value
                                         });
        }
        else {
            fields[fieldKey]= icepick.assign(fields[fieldKey], {
                                                     mounted : mounted,
                                                     value : value
                                                 });
        }
        this.setState({fields: icepick.assign(fields)});
    }


    /**
     *
     * @return Promise with the valid state true/false
     */
    validateForm() {
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

    }

    /**
     * make a promise for this field key to guarantee that all async validation has completed
     * @param fieldKey the field key to convert to non async
     * @return Promise
     */
    makeValidationPromise(fieldKey) {
        var retval;
        var fields= this.state.fields;
        if (fields[fieldKey].mounted && fields[fieldKey].asyncUpdate) {
            retval= new Promise( function(resolve) {
                var changeFunc= function() {
                    if (!fields[fieldKey].asyncUpdate) {
                        resolve(fieldKey);
                    }
                };
            }.bind(this));
        }
        else {
            retval= Promise.resolve(fieldKey);
        }
        return retval;
    }

    //todo: make a promise here
    getResults() {
        // do a promise here
        //this.fields.map
    }



}
