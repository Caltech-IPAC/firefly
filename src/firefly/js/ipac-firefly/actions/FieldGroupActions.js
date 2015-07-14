/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {application, NetworkMode} from '../core/Application.js';





class FieldGroupActions {

    constructor() {
        this.generateActions(
            'initFieldGroup', 'mountComponent', 'mountFieldGroup', 'validateFieldGroup', 'validateMultiFieldGroup'
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

export default application.alt.createActions(FieldGroupActions);
