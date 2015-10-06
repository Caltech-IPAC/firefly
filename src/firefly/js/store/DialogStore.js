/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import alt from '../core/AppAlt.js';
import DialogActions from '../actions/DialogActions.js'



class DialogStore {

    constructor() {
        this.dialogVisibleStatus = {};
        this.bindListeners({
            showDialog: DialogActions.showDialog,
            hideDialog: DialogActions.hideDialog
        });
    }

    showDialog(payload) {
        if (payload && payload.dialogId) this.updateStatus(payload.dialogId,true);
    }

    hideDialog(payload) {
        if (payload && payload.dialogId) this.updateStatus(payload.dialogId,false);
    }

    updateStatus(dialogId, visible) {
        if (dialogId in this.dialogVisibleStatus) {
            if (visible!=this.dialogVisibleStatus[dialogId]) {
                //this.dialogVisibleStatus= {  ...this.dialogVisibleStatus, {dialogId : visible}}
                this.dialogVisibleStatus= Object.assign({},this.dialogVisibleStatus, {[dialogId] : visible})
            }
        }
        else if (visible){
            this.dialogVisibleStatus[dialogId]= true;
            //this.dialogVisibleStatus= {  ...this.dialogVisibleStatus, {dialogId : true}}
            this.dialogVisibleStatus= Object.assign({},this.dialogVisibleStatus, {[dialogId] : visible})
        }
    }
}

export default alt.createStore(DialogStore, 'DialogStore' );
