/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import React, {memo, useEffect, useRef, useState} from 'react';
import PropTypes, {bool, elementType, func, object, oneOfType, shape, string} from 'prop-types';
import {createRoot} from 'react-dom/client';
import {Dropdown, IconButton, Menu, MenuButton, Sheet, Tooltip} from '@mui/joy';
import ArrowDropDownIcon from '@mui/icons-material/ArrowDropDown';

import {set} from 'lodash';
import {dispatchHideDialog, isDialogVisible} from '../core/ComponentCntlr';
import {flux} from '../core/ReduxFlux';
import {FireflyRoot} from './FireflyRoot.jsx';


const DIALOG_DIV= 'dialogRootDiv';
const TMP_ROOT='TMP-';
const DEFAULT_ZINDEX= 200;

export default {defineDialog, showTmpPopup};

let dialogs= [];
let tmpPopups= [];
let tmpCount=0;
let divElement;
let divElementRoot;

function init() {
    divElement= createDiv({id: DIALOG_DIV});
    divElementRoot= createRoot(divElement);
}

/*
 * Extend JoyUI Dropdown component to provide ease of use.
 * This set focus to the popup panel on mount.  This allow any click to hide it.
 * It also show tooltip when dropdown is closed.
 * @param button         defaults to ArrowDropDownIcon
 * @param title          tooltips for this dropdown
 * @param onOpenChange   called when dropdown open/close state changes
 * @param onFocusChange  called when focus state changes.  focus is true when mouse in hover over button, or when dropdown is opened.
 * @param slotProps
 * @param useIconButton     defaults to true.  more convenience than setting button.slots.root
 */
export function DropDown({button, title, onOpenChange, onFocusChange, slotProps, useIconButton=true, children, ...props}) {
    const [open, setOpen] = useState(false);
    const [focus, setFocus] = useState();

    const dropdownEl = useRef(null);
    useEffect(() => {
        dropdownEl.current?.focus();
    }, [dropdownEl.current]);

    useEffect(() => {
        focus !== undefined && onFocusChange?.(focus);
    }, [focus]);

    button ||= <ArrowDropDownIcon/>;

    const onChange = (_, open) => {
        onOpenChange?.(open);
        setOpen(open);
        setFocus(open);
    };

    const root = useIconButton ? IconButton : undefined;
    return (
        <Dropdown onOpenChange={onChange} {...props}>
            <Tooltip onMouseEnter={() => setFocus(true)}
                     onMouseLeave={() => setFocus(open)}
                     title={!open && title} {...slotProps?.tooltip}>
                <MenuButton {...slotProps?.button} slots={{ root, ...slotProps?.button?.slots }}>{button}</MenuButton>
            </Tooltip>
            <Menu ref={dropdownEl} {...slotProps?.menu}>{children}</Menu>
        </Dropdown>
    );
}

DropDown.propTypes = {
    button: object,
    title: oneOfType([string, elementType]),
    onOpenChange: func,
    onFocusChange: func,
    useIconButton: bool,
    slotProps: shape({
        button: object,
        menu: object,
        tooltip: object,
    })
};

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

function createDiv({id, appendTo=document.body, style = {}}) {
    const el= document.createElement('div');
    appendTo.appendChild(el);
    el.id= id;
    el.style.width= '0';
    el.style.height= '0';
    el.style.position = 'absolute';
    el.style.left= '0';
    el.style.top= '0';
    el.style.zIndex= DEFAULT_ZINDEX;
    Object.entries(style).forEach(([k,v]) => set(el.style, [k], v));
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
        <Sheet>
            {otherDialogAry}
            <div style={{position:'relative', zIndex:DEFAULT_ZINDEX}} className='rootStyle'>
                {dialogAry}
                <div style={{position:'relative', zIndex:10}}>
                    {tmpPopupAry}
                </div>
            </div>
        </Sheet>
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
    divElementRoot.render(
        <FireflyRoot>
            <DialogRootComponent dialogs={dialogs} tmpPopups={tmpPopups} requestOnTop={requestOnTop}/>
        </FireflyRoot>
    );
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
