/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


/*jshint browserify:true*/
/*jshint esnext:true*/
/*jshint curly:false*/

"use strict";

export class ExtensionResult {
    constructor() {
    }

    setExtValue(key,value) {
        this[key]= value;
    }
}

