/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import {dispatchHideDialog,isDialogVisible} from '../core/ComponentCntlr.js';
import sCompare from 'react-addons-shallow-compare';
import {flux} from '../Firefly.js';


export class PopupStoreConnection extends Component {

    constructor(props)  {
        super(props);
        var visible= isDialogVisible(props.dialogId);
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
        var newVisible= isDialogVisible(this.props.dialogId);
        if (newVisible !== visible) {
            this.setState( {visible : newVisible} );
        }
    }

    render() {
        var {visible}= this.state;
        if (!visible) return false;
        var {dialogId,popupPanel,requestOnTop,zIndex}= this.props;
        return  React.cloneElement(popupPanel,
            {
                visible, requestOnTop, dialogId, zIndex,
                requestToClose : () => dispatchHideDialog(dialogId)
            });
    }

}

PopupStoreConnection.propTypes= {
    popupPanel : PropTypes.object.isRequired,
    dialogId   : PropTypes.string.isRequired,
    requestOnTop : PropTypes.func,
    zIndex : PropTypes.number.isRequired
};


