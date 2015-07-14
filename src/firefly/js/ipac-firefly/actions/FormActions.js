

import {application, NetworkMode} from '../core/Application.js';





class FormActions {

    constructor() {
        this.generateActions(
            'initFieldGroup', 'initState', 'mountComponent', 'mountFieldGroup', 'validateForm'
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

export default application.alt.createActions(FormActions);