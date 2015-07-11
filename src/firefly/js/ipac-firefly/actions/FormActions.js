

import {application, NetworkMode} from '../core/Application.js';





class FormActions {

    constructor() {
        this.generateActions(
            'initState', 'mountComponent', 'validateForm'
        );
        //'initState', 'valueChange', 'mountComponent'
    }

    valueChange(data) {
        this.dispatch(data);
        if (data.asyncUpdate) {
            var action= this;
            data.asyncUpdate.then((payload) => {
                action.dispatch(payload);
            }).catch(e => console.log(e));
        }
    }


}

export default application.alt.createActions(FormActions);