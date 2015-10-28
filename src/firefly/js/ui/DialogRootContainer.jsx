/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * Created by roby on 9/3/15.
 */
import React from 'react/addons';
import AppDataCntlr from '../core/AppDataCntlr.js';
import {flux} from '../Firefly.js';
//import _ from 'underscore';




var dialogs= {};
const DIALOG_DIV= 'dialogRootDiv';
var initComplete= false;
var divElement;

var init= function() {
    divElement= document.createElement('div');
    document.body.appendChild(divElement);
    divElement.id= DIALOG_DIV;
    initComplete= true;
};

var DialogRootComponent = React.createClass(
{
    render() {
        var {dialogs}= this.props;
        var dialogAry = Object.keys(dialogs).map( (k) => dialogs[k]);
        return  (
            <div>
                {dialogAry}
            </div>
        );
    }
});


var PopupStoreConnection = React.createClass(
{

    propTypes: {
        popupPanel   : React.PropTypes.object.isRequired,
        dialogId   : React.PropTypes.string.isRequired
    },

    componentWillMount() { },

    componentWillUnmount() {
        if (this.storeListenerRemove) this.storeListenerRemove();
    },



    componentDidMount() {
        this.storeListenerRemove= flux.addListener( this.changeDialogState);
    },

    changeDialogState() {
        this.updateVisibility();
    },

    updateVisibility() {
        var newVisible= AppDataCntlr.isDialogVisible(this.props.dialogId);
        if (newVisible !== this.state.visible) {
            this.setState( {visible : newVisible} );
        }
    },

    getInitialState() {
        return { visible: false };
    },

    closeCallback() {
        AppDataCntlr.hideDialog(this.props.dialogId);
    },


    render() {
        if (this.state.visible) {
            return  React.cloneElement(this.props.popupPanel, { visible: this.state.visible,
                                                                dialogId : this.props.dialogId,
                                                                closeCallback : this.closeCallback});
            //key : DIALOG_DIV + this.props.dialogId})
        }
        else {
            return false;
        }
    }

});


/**
 * @param dialogId {string}
 * @param dialog {object}
 */
var defineDialog= function(dialogId, dialog) {
    if (!initComplete) init();
    dialogs[dialogId]= <PopupStoreConnection popupPanel={dialog} dialogId={dialogId}/>;
    React.render(<DialogRootComponent dialogs={dialogs}/>, divElement);
};


var DialogRootContainer= {defineDialog};
export default DialogRootContainer;
