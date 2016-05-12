/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import ReactDOM from 'react-dom';
import sCompare from 'react-addons-shallow-compare';
import {PopupStoreConnection} from './PopupStoreConnection.jsx';


const DIALOG_DIV= 'dialogRootDiv';
const TMP_ROOT='TMP-';

export default {defineDialog, showTmpPopup};

var dialogs= [];
var tmpPopups= [];
var tmpCount=0;
var divElement;



function requestOnTop(key) {
    var topKey= dialogs.sort( (d1,d2) => d2.zIndex-d1.zIndex)[0].dialogId;
    if (topKey!==key) {
        dialogs= sortZIndex(dialogs,key);
        reRender(dialogs,tmpPopups,requestOnTop);
    }
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
        dialogs= [...dialogs,newD];
    }
    else {
        dialogs[idx]= newD;
    }
    dialogs= sortZIndex(dialogs,dialogId);
    reRender(dialogs,tmpPopups,requestOnTop);
}


/**
 * @param popup {object}
 */
function showTmpPopup(popup) {
    if (!divElement) init();
    tmpCount++;
    const id= TMP_ROOT+tmpCount;
    tmpPopups= [...tmpPopups, {dialogId:id, component:popup}];
    reRender(dialogs,tmpPopups,requestOnTop);
    return () => {
        if (tmpPopups.some( (p) => (p.dialogId==id))) {
            tmpPopups= tmpPopups.filter( (p) => p.dialogId!=id);
            reRender(dialogs,tmpPopups,requestOnTop);
        }
    };
}



function init() {
    divElement= document.createElement('div');
    document.body.appendChild(divElement);
    divElement.id= DIALOG_DIV;
    divElement.style.position= 'absolute';
    divElement.style.left= '0';
    divElement.style.top= '0';
}





class DialogRootComponent extends Component {

    constructor(props) { super(props); }

    shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    render() {
        var {dialogs,tmpPopups,requestOnTop}= this.props;
        var dialogAry = dialogs.map( (d) =>
                                React.cloneElement(d.component,
                                    {
                                        key:d.dialogId,
                                        zIndex:d.zIndex,
                                        requestOnTop
                                    }));
        var tmpPopupAry = tmpPopups.map( (p) => React.cloneElement(p.component,{key:p.dialogId}));
        return (
                <div style={{position:'relative', zIndex:200}} className='rootStyle'>
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
    tmpPopups : PropTypes.array,
    requestOnTop : PropTypes.func
};


/**
 *
 * @param dialogs
 * @param tmpPopups
 * @param requestOnTop
 */
function reRender(dialogs,tmpPopups,requestOnTop) {
    ReactDOM.render(<DialogRootComponent dialogs={dialogs} tmpPopups={tmpPopups} requestOnTop={requestOnTop}/>, divElement);
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



