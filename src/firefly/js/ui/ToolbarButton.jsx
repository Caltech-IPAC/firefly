/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Badge, Button, Checkbox, Divider, IconButton, Stack, Tooltip} from '@mui/joy';
import React, {memo, useRef, useEffect} from 'react';
import PropTypes from 'prop-types';
import {dispatchHideDialog} from '../core/ComponentCntlr.js';
import {DROP_DOWN_KEY} from './DropDownToolbarButton.jsx';
import DROP_DOWN_ICON from 'html/images/dd-narrow.png';
import BrowserInfo, {Platform} from 'firefly/util/BrowserInfo.js';
import { useColorMode } from './FireflyRoot.jsx';


export function makeBadge(cnt, style={}) {
    const cName= `ff-badge ${cnt<10 ? 'badge-1-digit' : 'badge-2-digit'}`;
    return <div style={style} className={cName}>{Math.trunc(cnt)}</div>;
}


function getShortCutInfo(shortcutKey) {
    const NO_SHORT= {hasShortcut:false};
    if (!shortcutKey) return NO_SHORT;
    shortcutKey= shortcutKey.trim();
    const requiresCtrl= shortcutKey.toLowerCase().startsWith('ctrl-');
    const requiresMeta= shortcutKey.toLowerCase().startsWith('meta-');
    if ((requiresCtrl || requiresMeta) && shortcutKey.length!==6) return NO_SHORT;
    else if (!requiresCtrl && !requiresMeta && shortcutKey.length!==1) return NO_SHORT;
    const testKey= (requiresCtrl || requiresMeta) ? shortcutKey[5] : shortcutKey[0];
    return {ctrl:requiresCtrl, meta:requiresMeta, key:testKey, hasShortcut:true};
}

/**
 *
 * @param icon icon to display
 * @param text text to display, if icon specified, icon task precidents
 * @param tip tooltip
 * @param badgeCount if greater than 0 a badge is shown on the button
 * @param enabled if false, show faded view
 * @param dropDownCB callback for the dropdown, will pass the div element
 * @param onClick function to call on click
 * @param visible if false then don't show button
 * @param active
 * @param imageStyle
 * @param lastTextItem
 * @param style - a style to apply
 * @return {object}
 */
export const ToolbarButton = memo((props) => {
    const {
        icon,text='',tip='',badgeCount=0,enabled=true, visible=true,
        imageStyle={}, shortcutKey='', color='neutral', variant='plain',
        disableHiding, active, sx,
        useDropDownIndicator= false, hasCheckBox=false, checkBoxOn=false,
        dropDownCB, onClick} = props;

    const {current:divElementRef}= useRef({divElement:undefined});
    const doInvert= useColorMode()?.activeMode==='dark';

    const handleClick= (ev) => {
        onClick?.(divElementRef.divElement,ev);
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
    const allowInput= disableHiding?'allow-input':'normal-button-hide';

    const setupRef  = (c) => divElementRef.divElement= c;

    const image= icon ? <img src={icon} style={imageStyle} className={allowInput} /> : undefined;
    const dropDownIndicator= useDropDownIndicator ? <img src={DROP_DOWN_ICON}/> : undefined;


    const b=  (
        <Tooltip title={tip} sx={sx}>
            <Stack {...{direction:'row', alignItems:'center', ref:setupRef,
                   sx:doInvert ? {'img':{filter : 'invert(1)'}} : {} }}>
                {hasCheckBox && <Checkbox {...{variant:'plain', checked:checkBoxOn, onClick:handleClick}}/> }
                {(icon && !text) ?
                    (<IconButton {...{
                        sx: (theme) => (
                             {minHeight:'unset', minWidth:'unset', p:.5, backgroundColor:'transparent',
                             ...makeBorder(active,theme,color)
                             }),
                        className:'ff-toolbar-iconbutton ' + allowInput,
                        variant:'soft', color:'neutral' ,
                        'aria-label':tip, onClick:handleClick, disabled:!enabled}}>
                        {image}
                    </IconButton>) :
                    <Button {...{color, variant,
                        'aria-label':tip, disabled:!enabled, onClick:handleClick,
                        className:'ff-toolbar-button ' + allowInput,
                        startDecorator: image,
                        endDecorator: dropDownIndicator,
                        sx:(theme) => ({whiteSpace:'nowrap', py:.4, minHeight: 'unset', ...makeBorder(active,theme,color)}),
                    }}>
                        {makeTextLabel(text,shortcutKey)}
                    </Button>
                }
            </Stack>
        </Tooltip>
    );

    return !badgeCount ? b : <Badge {...{badgeContent:badgeCount}}> {b} </Badge>;
} );

ToolbarButton.propTypes= {
    icon : PropTypes.string,
    text : PropTypes.node,
    tip : PropTypes.string,
    shortcutHelp : PropTypes.bool,
    badgeCount : PropTypes.number,
    enabled : PropTypes.bool,
    visible : PropTypes.bool,
    imageStyle : PropTypes.object,
    lastTextItem : PropTypes.bool,
    useDropDownIndicator: PropTypes.bool,
    style : PropTypes.object,
    hasCheckBox: PropTypes.bool,
    checkBoxOn: PropTypes.bool,
    onClick : PropTypes.func,
    dropDownCB : PropTypes.func,
    disableHiding: PropTypes.bool,
    shortcutKey: PropTypes.string,
    color: PropTypes.string,
};


function makeBorder(active, theme,color) {
    // const color= active ? theme.vars.palette.warning.softActiveBg : 'transparent';
    const borderC= active ? theme.vars.palette[color]?.softActiveBg : 'transparent';
    // const color= active ? theme.vars.palette.primary.softActiveColor: 'transparent';
    return {
        borderTop: `1px solid ${borderC}`,
        borderLeft: `1px solid ${borderC}`,
        borderRight: `1px solid ${borderC}`,
    };
}

function makeTextLabel(text,shortcutKey) {
    const {meta,key,hasShortcut}= getShortCutInfo(shortcutKey);
    if (!hasShortcut) return text;
    if (hasShortcut && meta && BrowserInfo.isPlatform(Platform.MAC)) {
        shortcutKey= String.fromCharCode(0x2318) + '-'+key;
    }
    return (
        <span>
            {text}
            <span style={{fontSize : 'smaller'}}>
                {` (${shortcutKey})`}
            </span>
        </span>
    );
}


export function ToolbarHorizontalSeparator({ style={}}) {
    return <Divider orientation='vertical' style={style} sx={{mx:1}}/>;
}
ToolbarHorizontalSeparator.propTypes= { style:PropTypes.object, top : PropTypes.number };

export function DropDownVerticalSeparator({useLine=false, style={}}) {
    return <div style={style} className={useLine? 'ff-vertical-line-separator' : 'ff-vertical-separator'}/>;
}
DropDownVerticalSeparator.propTypes= {
    useLine: PropTypes.bool,
    style: PropTypes.object
};
