/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo, useRef, useEffect} from 'react';
import PropTypes from 'prop-types';
import {dispatchHideDialog} from '../core/ComponentCntlr.js';
import './ToolbarButton.css';
import {DROP_DOWN_KEY} from './DropDownToolbarButton.jsx';
import DROP_DOWN_ICON from 'html/images/dd-narrow.png';
import CHECK_BOX from 'html/images/black_check-on_10x10.gif';
import BrowserInfo, {Platform} from 'firefly/util/BrowserInfo.js';


export function makeBadge(cnt) {
    const cName= `ff-badge ${cnt<10 ? 'badge-1-digit' : 'badge-2-digit'}`;
    return <div className={cName}>{Math.trunc(cnt)}</div>;
}

const checkBoxBaseStyle= {width: 10, height: 10, paddingRight: 4, alignSelf: 'center'};

const makeCheckBox= (checkBoxOn, imageStyle= {}) => checkBoxOn ?
            <img style={{...checkBoxBaseStyle, ...imageStyle}} src={CHECK_BOX}/> : <span style={{width:14}}/>;

const makeDropDownIndicator= () => (<img src={DROP_DOWN_ICON} style={{width:11, height:6, alignSelf: 'center'}}/>);

const todoStyle= {
    position : 'absolute',
    left  : 1,
    top  : 7,
    fontSize : '10px',
    color : 'white',
    background : 'rgba(245,0,255,.8)',
    borderRadius : '5px',
    whiteSpace : 'nowrap'
};
const makeToDoTag= () => <div style={todoStyle}>${'ToDo'}</div>;


const NO_SHORT= {hasShortcut:false};

function getShortCutInfo(shortcutKey) {
    if (!shortcutKey) return NO_SHORT;
    shortcutKey= shortcutKey.trim();
    const requiresCtrl= shortcutKey.toLowerCase().startsWith('ctrl-');
    const requiresMeta= shortcutKey.toLowerCase().startsWith('meta-');
    if ((requiresCtrl || requiresMeta) && shortcutKey.length!==6) return NO_SHORT;
    else if (!requiresCtrl && !requiresMeta && shortcutKey.length!==1) return NO_SHORT;
    const testKey= (requiresCtrl || requiresMeta) ? shortcutKey[5] : shortcutKey[0];
    return {ctrl:requiresCtrl, meta:requiresMeta, key:testKey, hasShortcut:true};
}

function makeTextLabel(text,shortcutKey, allowInput='') {
    if (!text) return '';
    const {meta,key,hasShortcut}= getShortCutInfo(shortcutKey);
    if (!hasShortcut) return text;
    if (hasShortcut && meta && BrowserInfo.isPlatform(Platform.MAC)) {
        shortcutKey= String.fromCharCode(0x2318) + '-'+key;
    }
    return (
        <span className={allowInput}>
            {text}
            <span style={{fontSize : 'smaller'}}>
                {` (${shortcutKey})`}
            </span>
        </span>
    );
}

/**
 *
 * @param icon icon to display
 * @param text text to display, if icon specified, icon task precidents
 * @param tip tooltip
 * @param badgeCount if greater then 0 a badge is shown on the button
 * @param enabled if false, show faded view
 * @param dropDownCB callback for the dropdown, will pass the div element
 * @param onClick function to call on click
 * @param horizontal lay out horizontal, if false lay out vertical
 * @param bgDark layout on a dark background, if false lay out on a light background
 * @param visible if false then don't show button
 * @param active
 * @param imageStyle
 * @param tipOnCB
 * @param tipOffCB
 * @param lastTextItem
 * @param style - a style to apply
 * @param todo show a todo message
 * @return {object}
 */
export const ToolbarButton = memo((props) => {
    const {
        icon,text='',tip='',badgeCount=0,enabled=true, horizontal=true, bgDark= false, visible=true, active= false,
        imageStyle={}, lastTextItem=false, todo= false, style={}, shortcutKey='', useBorder=false,
        useDropDownIndicator= false, hasCheckBox=false, checkBoxOn=false,
        tipOnCB, tipOffCB, dropDownCB, onClick, disableHiding= false} = props;

    const {current:divElementRef}= useRef({divElement:undefined});


    const handleClick= () => {
        onClick?.(divElementRef.divElement);
        dropDownCB ? dropDownCB(divElementRef.divElement) : dispatchHideDialog(DROP_DOWN_KEY);
    };

    useEffect( () => {
        const {cnrl,meta,key,hasShortcut}= getShortCutInfo(shortcutKey);
        if (!hasShortcut) return;
        const listener= (ev) => {
            if (cnrl && !ev.ctrlKey) return;
            if (meta && !ev.metaKey) return;
            ev.key===key && handleClick();
        };
        window.document.addEventListener('keydown', listener);
        return () => window.document.removeEventListener('keydown', listener);
    });
    if (!visible) return false;


    const mouseOver= () => tipOnCB?.(tip);
    const mouseOut= () => tipOffCB?.();
    const setupRef  = (c) => divElementRef.divElement= c;

    const baseStyle= { position: 'relative', display: (horizontal) ? 'inline-flex' : 'flex'};
    let textCName= 'menuItemText';

    const cName= `ff-MenuItem ${bgDark ? 'ff-MenuItem-dark' : 'ff-MenuItem-light'}`+
        ` ${enabled ? '' : 'ff-MenuItem-disabled'} ${active ? 'ff-MenuItem-active':''}`;
    const allowInput= disableHiding?' allow-input':' normal-button-hide';

    if (horizontal && !icon) { //used for horizontal text only, this is usually a dropdown menu
        const horizontalStyle= {
            verticalAlign: 'bottom',
            fontSize: '10pt',
            position: 'relative',
            display: 'flex',
            alignItems: 'center',
            borderRadius: 5
        };
        textCName= 'ff-menuItemHText';
        const htextBorder= {border: useBorder ? '1px solid rgba(0,0,0,.2)' : 'none' };
        return (
            <div style={{display:'flex', height:'100%', flex:'0 0 auto' ,...style}} className={allowInput}>
                <div style={{ display:'inline-block', margin:'0 4px 0 4px'}} />
                <div title={tip} style={horizontalStyle} className={cName}
                     ref={setupRef} onClick={handleClick} onMouseOver={mouseOver} onMouseOut={mouseOut}>
                    <div className={textCName} style={htextBorder}>{makeTextLabel(text,shortcutKey,allowInput)}</div>
                    {useDropDownIndicator && makeDropDownIndicator()}
                    {badgeCount>0 && makeBadge(badgeCount)}
                    {todo && makeToDoTag()}
                </div>
                {lastTextItem &&
                     <div style={{ display: 'inline-block', margin: '0 4px 0 4px', height: 'calc(100% - 7px)'}} />}
            </div>
        );
    }
    else {
        if (icon&&text) {  // button in vertical style with both icon and text, least common case
            return (
                <div title={tip} style={{display: 'flex', alignItems: 'center'}} className={cName + allowInput}
                     ref={setupRef} onClick={handleClick} onMouseOver={mouseOver} onMouseOut={mouseOut}>
                    <div style= {{display:'flex', flexGrow:1, alignItems:'center'}} className={textCName}>
                        <img style={imageStyle} src={icon} className={textCName+ allowInput}/>
                        <span style={{paddingLeft:5, flexGrow:1}}>{makeTextLabel(text,shortcutKey,allowInput)}</span>
                    </div>
                    {badgeCount>0 && makeBadge(badgeCount)}
                    {todo && makeToDoTag()}
                </div>
            );
        }
        else { // this is the most common case - vertical text buttons or horizontal icons
            return (
                <div title={tip} style={{...baseStyle, flex: '0 0 auto', ...style}} className={cName+ ' '+allowInput}
                     ref={setupRef} onClick={handleClick} onMouseOver={mouseOver} onMouseOut={mouseOut}>
                    <div style={{flexGrow:1, display:'flex'}} className={textCName}>
                        {hasCheckBox && makeCheckBox(checkBoxOn,imageStyle)}
                        {icon ?
                            <img style={{flexGrow:1, ...imageStyle}} src={icon} className={allowInput}/> :
                            <div style={{flexGrow:1}} className={textCName}>{makeTextLabel(text,shortcutKey,allowInput)}</div>}
                    </div>
                    {badgeCount>0 && makeBadge(badgeCount)}
                    {todo && makeToDoTag()}
                </div>
            );
        }
    }
} );

ToolbarButton.propTypes= {
    icon : PropTypes.string,
    text : PropTypes.node,
    tip : PropTypes.string,
    shortcutKey : PropTypes.string,
    shortcutHelp : PropTypes.bool,
    useBorder : PropTypes.bool,
    badgeCount : PropTypes.number,
    enabled : PropTypes.bool,
    bgDark: PropTypes.bool,
    todo: PropTypes.bool,
    horizontal : PropTypes.bool,
    visible : PropTypes.bool,
    active : PropTypes.bool,
    imageStyle : PropTypes.object,
    lastTextItem : PropTypes.bool,
    useDropDownIndicator: PropTypes.bool,
    style : PropTypes.object,
    hasCheckBox: PropTypes.bool,
    checkBoxOn: PropTypes.bool,
    onClick : PropTypes.func,
    tipOnCB : PropTypes.func,
    tipOffCB : PropTypes.func,
    dropDownCB : PropTypes.func,
    disableHiding: PropTypes.bool
};


export function ToolbarHorizontalSeparator({top=0, style={}}) {
    const s= {top, ...style};
    return <div style={s} className='ff-horizontal-separator'/>;
}
ToolbarHorizontalSeparator.propTypes= { style:PropTypes.object, top : PropTypes.number };

export function DropDownVerticalSeparator({useLine=false}) {
    return <div className={useLine? 'ff-vertical-line-separator' : 'ff-vertical-separator'}/>;
}
DropDownVerticalSeparator.propTypes= { useLine: PropTypes.bool };
