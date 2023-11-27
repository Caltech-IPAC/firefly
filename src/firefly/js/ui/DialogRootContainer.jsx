/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {memo, useEffect, useState, PureComponent} from 'react';
import PropTypes from 'prop-types';
import {createRoot} from 'react-dom/client';
import {get, set} from 'lodash';
import {dispatchHideDialog, isDialogVisible} from '../core/ComponentCntlr';
import {flux} from '../core/ReduxFlux';


const DIALOG_DIV= 'dialogRootDiv';
const DROPDOWN_DIV_ROOT= 'dropDownPlane-root';
const TMP_ROOT='TMP-';
const DEFAULT_ZINDEX= 200;

export default {defineDialog, showTmpPopup};

let dialogs= [];
let tmpPopups= [];
let tmpCount=0;
let divElement;
let divElementRoot;

const reactRoots= new Map();

function init() {
    divElement= createDiv({id: DIALOG_DIV});
    divElementRoot= createRoot(divElement);
}

/**
 * locDir is a 2-digit number to indicate the location and direction of the drop-down.
 *   location is the first digit starting from 1-top-left to 4-bottom-left clockwise.
 *   direction is the 2nd digit used to denote direction.  It follows the same convention as above.
 *
 * example:  drop-down at bottom-right, spanning left.   34
 *
 * @param p parameters object
 * @param p.id
 * @param p.content     the content to display
 * @param p.style       overrideable style
 * @param p.atElRef     the element reference used to apply locDir to.
 * @param p.locDir      location and direction of the drop-down.  see desc for more info
 * @param p.wrapperStyle style to apply to dropdown wrapper div, ex. zIndex
 * @param p.boxEl       the container's element.  Used to deactivate the dropdown.
 */
export function showDropDown({id='',content, style={}, atElRef, locDir, wrapperStyle, boxEl}) {
    const planeId= getddDiv(id);
    const ddDiv = document.getElementById(planeId) || createDiv({id: planeId, wrapperStyle});
    const root= createRoot(ddDiv);
    reactRoots.set(divElement,root);

    const rootZindex= atElRef && computeZIndex(atElRef);
    if (rootZindex) ddDiv.style.zIndex= rootZindex;
    root.render( <DropDown {...{id, content, style, atElRef, locDir, boxEl}}/>);
    return ddDiv;
}

export function isDropDownShowing(id) {
    return document.getElementById(getddDiv(id));
}

export function hideDropDown(id='') {
    const ddDiv = document.getElementById(getddDiv(id));
    if (ddDiv) {
        reactRoots.get(ddDiv)?.unmount();
        ddDiv.parentNode.removeChild(ddDiv);
    }
}

const getddDiv= (id) => id ? id+ '-dropdownPlane' : DROPDOWN_DIV_ROOT;

function DropDown ({id, content, style={}, locDir, atElRef, boxEl}) {

    const hide = () => hideDropDown(id);

    // Effect to to hide DropDown when clicked elsewhere
    useEffect(() => {
        const box = boxEl || document;
        box.addEventListener('click', hide);
        return () => box.removeEventListener('click', hide);;
    }, []);

    if (!get(atElRef, 'isConnected', true)) hide();                                            // referenced element is no longer visible.. hide drop-down.
    const {x:o_x, y:o_y, width:o_width, height:o_height} = document.documentElement.getBoundingClientRect();    // outer box
    const {x=0, y=0, width=0, height=0} = atElRef ? atElRef.getBoundingClientRect() : {};                       // inner box


    const [loc, dir] = [Math.floor(locDir/10), locDir%10];
    const top    = (y-o_y) + (loc === 3 || loc === 4 ? height : 0);
    const bottom = ((o_height-o_y) - y) - (loc === 3 || loc === 4 ? height : 0);
    const left   = (x-o_x) + (loc === 2 || loc === 3 ? width : 0);
    const right  = ((o_width-o_x) - x) - (loc === 2 || loc === 3 ? width : 0);

    let pos;
    switch (dir) {
        case 1:
            pos = {bottom, right}; break;
        case 2:
            pos = {bottom, left}; break;
        case 3:
            pos = {top, left}; break;
        case 4:
            pos = {top, right}; break;
    }

    const myStyle = Object.assign({ backgroundColor: '#FBFBFB',
            ...pos,
            padding: 3,
            boxShadow: '#c1c1c1 1px 1px 5px 0px',
            borderRadius: '0 3px',
            border: '1px solid #c1c1c1',
            position: 'absolute'},
        style);
    const stopEvent = (e) => {
        e.stopPropagation();
        e.nativeEvent && e.nativeEvent.stopImmediatePropagation();
    };

    return (
        <div className='rootStyle' style={myStyle} onClick={stopEvent}>
            {content}
        </div>
    );
}


function requestOnTop(key) {
    const topKey= dialogs.sort( (d1,d2) => d2.zIndex-d1.zIndex)[0].dialogId;
    if (topKey===key) return;
    dialogs= sortZIndex(dialogs,key);
    reRender(dialogs,tmpPopups,requestOnTop);
}

function computeZIndex(element ) {
    let zIndex, testZ;
    for(let e=element; (e); e= e.parentElement ) {
        testZ= Number(window.getComputedStyle(e).getPropertyValue('z-index'));
        if (testZ) zIndex= testZ;
    }
    if (!zIndex) return 12;
    return (zIndex<DEFAULT_ZINDEX-10) ? zIndex+10 : DEFAULT_ZINDEX;
}


const PopupStoreConnection= memo(({dialogId,popupPanel,requestOnTop,zIndex}) => {
    const [visible, setVisible]= useState(false);
    useEffect( () => {   // can't useStoreConnector, the flux listener is not added fast enough
        setVisible(isDialogVisible(dialogId));
        return flux.addListener(() => setVisible(isDialogVisible(dialogId)));
    }, [] );
    if (!visible) return false;
    return React.cloneElement(popupPanel,
        { visible, requestOnTop, dialogId, zIndex, requestToClose : () => dispatchHideDialog(dialogId) });
});

PopupStoreConnection.propTypes= {
    popupPanel : PropTypes.object.isRequired,
    dialogId   : PropTypes.string.isRequired,
    requestOnTop : PropTypes.func,
    zIndex : PropTypes.number.isRequired
};

/**
 * @param {string} dialogId
 * @param {object} dialog
 * @param {Element} [overElement]
 */
function defineDialog(dialogId, dialog, overElement) {
    if (!divElement) init();
    const idx= dialogs.findIndex((d) => d.dialogId===dialogId);
    const newD= {
        dialogId,
        rootZindex: overElement ? computeZIndex(overElement): DEFAULT_ZINDEX,
        component: <PopupStoreConnection popupPanel={dialog} dialogId={dialogId} zIndex={1}/>
    };
    if (idx < 0) dialogs= [...dialogs,newD];
    else dialogs[idx]= newD;
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
        if (tmpPopups.some( (p) => (p.dialogId===id))) {
            tmpPopups= tmpPopups.filter( (p) => p.dialogId!==id);
            reRender(dialogs,tmpPopups,requestOnTop);
        }
    };
}

function createDiv({id, appendTo=document.body, wrapperStyle={}}) {
    const el= document.createElement('div');
    appendTo.appendChild(el);
    el.id= id;
    el.style.width= '0';
    el.style.height= '0';
    el.style.position = 'absolute';
    el.style.left= '0';
    el.style.top= '0';
    Object.entries(wrapperStyle).forEach(([k,v]) => set(el.style, [k], v));
    return el;
}


const DialogRootComponent= memo(({dialogs,tmpPopups,requestOnTop}) =>{
    const dialogAry = dialogs
        .filter( (d) => d.rootZindex===DEFAULT_ZINDEX)
        .map( (d) => React.cloneElement(d.component, { key:d.dialogId, zIndex:d.zIndex, requestOnTop }) );
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
});

DialogRootComponent.propTypes = {
    dialogs : PropTypes.array,
    tmpPopups : PropTypes.array,
    requestOnTop : PropTypes.func
};


/**
 * @param dialogs
 * @param tmpPopups
 * @param requestOnTop
 */
function reRender(dialogs,tmpPopups,requestOnTop) {
    divElementRoot.render(<DialogRootComponent dialogs={dialogs} tmpPopups={tmpPopups} requestOnTop={requestOnTop}/>);
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
    let max= inDialogAry.reduce((prev,d) => (d.zIndex &&d.zIndex>prev) ? d.zIndex : prev, 1);
    if (max<inDialogAry.length) max= inDialogAry.length+1;
    const retval= inDialogAry.map( (d,idx) => Object.assign({},d,{zIndex:d.dialogId===topId ? max+1 : (d.zIndex||idx+1) }));

    const min= retval.reduce((prev,d) => (d.zIndex &&d.zIndex<prev) ? d.zIndex : prev, Number.MAX_SAFE_INTEGER);
    if (retval.length>1 && min>10000) {
        inDialogAry.forEach( (d) => d.zIndex-=7000);
    }
    return retval;
}
