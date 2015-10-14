/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import FieldGroupStore from '../FieldGroupStore.js';

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
    var fields= FieldGroupStore.getGroupFields(groupKey);
    if (!fields) return true;
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
                        throw new Error('could not find fieldKey from promise results');
                    }
                    var f = fields[fieldKey];
                    return (f.valid !== undefined && f.mounted) ? f.valid : true;
                });
            doneCallback(valid);
        }
    ).catch(e => console.log(e));
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
 * @param groupkey  the group key for the fieldgroup
 * @return {{}}
 */
var getResults= function(groupKey) {
    var fields= FieldGroupStore.getGroupFields(groupKey);
    var request= {};
    Object.keys(fields).forEach(function(fieldKey) {
        request[fieldKey] = fields[fieldKey].value;
    },this);
    return request;
};

var FieldGroupUtils= {validate, getResults};

export default FieldGroupUtils;




