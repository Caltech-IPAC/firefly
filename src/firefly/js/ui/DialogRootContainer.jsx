/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import {dispatchHideDialog,isDialogVisible} from '../core/ComponentCntlr.js';
import sCompare from 'react-addons-shallow-compare';
import {flux} from '../Firefly.js';



export default {defineDialog, showTmpPopup};

var dialogs= [];
var tmpPopups= [];
const DIALOG_DIV= 'dialogRootDiv';
const TMP_ROOT='TMP=';
var tmpCount=0;
var divElement;

var init= function() {
    divElement= document.createElement('div');
    document.body.appendChild(divElement);
    divElement.id= DIALOG_DIV;
};


class DialogRootComponent extends Component {

    constructor(props) {
        super(props);
        this.requestOnTop= this.requestOnTop.bind(this);
        this.state = {oneTopKey:null};
    }
    
    componentWillReceiveProps(nextProps, context) {
        this.setState({dialogs:null,onTopKey:null});
    }

    requestOnTop(key) {
        if (this.state.onTopKey!==key) {
            dialogs= sortZIndex(dialogs,key);
            this.setState({dialogs,onTopKey:key});
        }
    }

    render() {
        var {dialogs,tmpPopups}= this.props;
        if (this.state.dialogs) dialogs= this.state.dialogs;
        var dialogAry = dialogs.map( (d) =>
                                React.cloneElement(d.component,
                                    {
                                        key:d.dialogId,
                                        zIndex:d.zIndex,
                                        requestOnTop:this.requestOnTop
                                    }));
        var tmpPopupAry = tmpPopups.map( (p) => React.cloneElement(p.component,{key:p.dialogId}));
        return (
                <div style={{position:'relative', zIndex:200}}>
                    {dialogAry}
                    <div style={{position:'relative', zIndex:10}}>
                        {tmpPopupAry}
                    </div>
                </div>

            );

    }
}

DialogRootComponent.propTypes = {
    dialogs : PropTypes.array,
    tmpPopups : PropTypes.array
};


class PopupStoreConnection extends Component {

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


function reRender(dialogs,tmpPopups) {
    ReactDOM.render(<DialogRootComponent dialogs={dialogs} tmpPopups={tmpPopups}/>, divElement);
}


/**
 * @param dialogId {string}
 * @param dialog {object}
 */
function defineDialog(dialogId, dialog) {
    if (!divElement) init();
    const idx= dialogs.findIndex((d) => d.dialogId===dialogId);
    const newD= {
        dialogId,
        component: <PopupStoreConnection popupPanel={dialog} dialogId={dialogId} zIndex={1}/>
    };
    if (idx < 0) {
        dialogs.push(newD);
    }
    else {
        dialogs[idx]= newD;
    }
    dialogs= sortZIndex(dialogs,dialogId);
    reRender(dialogs,tmpPopups);
}


/**
 * Set the zindex for the active one is on top while maintaining the order of the others.
 * @param inDialogAry
 * @param topId
 * @return {*}
 *
 *
 * This is probably not the best way to do this.  I take the active id and make make it so it is bigger
 * than any of the others. Over time my counter will get too high so occasionally I reduce all the zIndexes too
 * something more reasonable.
 */
function sortZIndex(inDialogAry, topId) {
    var max= inDialogAry.reduce((prev,d) => (d.zIndex &&d.zIndex>prev) ? d.zIndex : prev, 1);
    if (max<inDialogAry.length) max= inDialogAry.length+1;
    var retval= inDialogAry.map( (d,idx) => Object.assign({},d,{zIndex:d.dialogId===topId ? max+1 : (d.zIndex||idx+1) }));

    var min= retval.reduce((prev,d) => (d.zIndex &&d.zIndex<prev) ? d.zIndex : prev, Number.MAX_SAFE_INTEGER);
    if (retval.length>1 && min>10000) {
        inDialogAry.forEach( (d) => d.zIndex-=7000);
    }
    return retval;
}



/**
 * @param popup {object}
 */
function showTmpPopup(popup) {
    if (!divElement) init();
    tmpCount++;
    const id= TMP_ROOT+tmpCount;
    tmpPopups.push( {dialogId:id, component:popup});
    reRender(dialogs,tmpPopups);
    return () => {
        if (tmpPopups.some( (p) => (p.dialogId==id))) {
            tmpPopups= tmpPopups.filter( (p) => p.dialogId!=id);
            reRender(dialogs,tmpPopups);
        }
    };
}
