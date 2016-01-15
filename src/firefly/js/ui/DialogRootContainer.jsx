/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import AppDataCntlr from '../core/AppDataCntlr.js';
import sCompare from 'react-addons-shallow-compare';
import {flux} from '../Firefly.js';



export default {defineDialog};

var dialogs= {};
const DIALOG_DIV= 'dialogRootDiv';
var divElement;

var init= function() {
    divElement= document.createElement('div');
    document.body.appendChild(divElement);
    divElement.id= DIALOG_DIV;
};

function DialogRootComponent({dialogs}) {
    var dialogAry = Object.keys(dialogs).map( (k) => React.cloneElement(dialogs[k],{key:k}));
    return  <div> {dialogAry} </div>;
}

DialogRootComponent.propTypes = {
    dialogs : PropTypes.object
};


class PopupStoreConnection extends Component {

    constructor(props)  {
        super(props);
        var visible= AppDataCntlr.isDialogVisible(this.props.dialogId);
        this.state = { visible};
    }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    componentWillUnmount() {
        if (this.storeListenerRemove) this.storeListenerRemove();
    }

    componentDidMount() {
        this.storeListenerRemove= flux.addListener(() => this.updateVisibility());
    }

    updateVisibility() {
        var {visible}= this.state;
        var newVisible= AppDataCntlr.isDialogVisible(this.props.dialogId);
        if (newVisible !== visible) {
            this.setState( {visible : newVisible} );
        }
    }

    render() {
        var {visible}= this.state;
        if (!visible) return false;
        var {dialogId,popupPanel}= this.props;
        return  React.cloneElement(popupPanel,
            {
                visible,
                closeCallback : () => AppDataCntlr.hideDialog(dialogId)
            });
    }

}

PopupStoreConnection.propTypes= {
    popupPanel : PropTypes.object.isRequired,
    dialogId   : PropTypes.string.isRequired
};




/**
 * @param dialogId {string}
 * @param dialog {object}
 */
function defineDialog(dialogId, dialog) {
    if (!divElement) init();
    dialogs[dialogId]= <PopupStoreConnection popupPanel={dialog} dialogId={dialogId}/>;
    ReactDOM.render(<DialogRootComponent dialogs={dialogs}/>, divElement);
}

