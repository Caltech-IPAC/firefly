/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/

"use strict";

import { Actions } from 'flummox';


export class FormUpdateActions extends Actions {
    valueChange(fieldKey,newValue,fieldState,asyncUpdate,valid,message) {
        return {fieldKey,newValue,fieldState,asyncUpdate,valid,message}
    }

    mountComponent(fieldKey,mounted,value,fieldState) {
        return {fieldKey,mounted,value,fieldState};
    }

    validateForm() {
    }



}
