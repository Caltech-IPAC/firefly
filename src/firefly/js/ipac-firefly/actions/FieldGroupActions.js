/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

//import {alt} from '../core/Application.js';

import alt from '../core/AppAlt.js';




class FieldGroupActions {

    constructor() {
        this.generateActions(
            'initFieldGroup', 'mountComponent', 'mountFieldGroup'
        );
        //'initState', 'valueChange', 'mountComponent'
    }

    valueChange(data) {
        this.dispatch(data);
        if (data.asyncUpdatePromise) {
            var action= this;
            data.asyncUpdatePromise.then((payload) => {
                action.dispatch(payload);
            }).catch(e => console.log(e));
        }
    }


}

export default alt.createActions(FieldGroupActions);
