/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import ReactDOM from 'react-dom';
import {PopupStoreConnection} from './PopupStoreConnection.jsx';


const DIALOG_DIV= 'dialogRootDiv';
const TMP_ROOT='TMP-';
const DEFAULT_ZINDEX= 200;

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

function computeZIndex(element ) {

    let zIndex, testZ;
    for(let e=element; (e); e= e.parentElement ) {
        testZ= Number(window.getComputedStyle(e).getPropertyValue('z-index'));
        zIndex= testZ || 2;
    }
    return zIndex;
}



/**
 * @param {string} dialogId
 * @param {object} dialog
 * @param {Element} overElement
 */
function defineDialog(dialogId, dialog, overElement) {
    if (!divElement) init();
    const idx= dialogs.findIndex((d) => d.dialogId===dialogId);
    const rootZindex= overElement ? computeZIndex(overElement): DEFAULT_ZINDEX;
    const newD= {
        dialogId,
        rootZindex,
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





class DialogRootComponent extends PureComponent {

    constructor(props) { super(props); }

    render() {
        const {dialogs,tmpPopups,requestOnTop}= this.props;
        const dialogAry = dialogs
            .filter( (d) => d.rootZindex===DEFAULT_ZINDEX)
            .map( (d) =>
                React.cloneElement(d.component,
                    {
                        key:d.dialogId,
                        zIndex:d.zIndex,
                        requestOnTop
                    })
            );
        const otherDialogAry = dialogs
            .filter( (d) => d.rootZindex<DEFAULT_ZINDEX)
            .map( (d) => (
                <div key= {d.dialogId} style={{position:'relative', zIndex:d.rootZindex}} className='rootStyle'>
                    {React.cloneElement(d.component, { key:d.dialogId, zIndex:d.zIndex, requestOnTop })}
                </div>
            ));
        const tmpPopupAry = tmpPopups.map( (p) => React.cloneElement(p.component,{key:p.dialogId}));
        return (
            <div>
                {otherDialogAry}
                <div style={{position:'relative', zIndex:DEFAULT_ZINDEX}} className='rootStyle'>
                    {dialogAry}
                    <div style={{position:'relative', zIndex:10}}>
                        {tmpPopupAry}
                    </div>
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



