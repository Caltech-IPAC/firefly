/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Badge, Box, Button, Checkbox, Divider, IconButton, Stack, Tooltip} from '@mui/joy';
import {isString} from 'lodash';
import React, {useRef, useEffect, forwardRef, useCallback, useImperativeHandle} from 'react';
import {bool, element, func, node, number, object, oneOfType, shape, string} from 'prop-types';
import {dispatchHideDialog} from '../core/ComponentCntlr.js';
import {DROP_DOWN_KEY} from './DropDownToolbarButton.jsx';
import ArrowDropDownRoundedIcon from '@mui/icons-material/ArrowDropDownRounded';
import BrowserInfo, {Platform} from 'firefly/util/BrowserInfo.js';


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

function makeImage(icon,style={},className='') {
    if (!icon) return;
    if (isString(icon)) {
        return <img className={`old-ff-icon-img${className?' ':''}${className}`} src={icon} style={{...style}}/>;
    }
    else if (!className) {
        return icon;
    }
    else {
        const cName= icon.props?.className ?? '';
        const newCname= cName ? `${cName} ${className}` : className;
        return React.cloneElement(icon, { className:newCname} );
    }
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
export const ToolbarButton = forwardRef((props,fRef) => {
    const {
        icon,text='',badgeCount=0,badgeAlert=false, enabled=true, visible=true,
        imageStyle={}, iconButtonSize, shortcutKey='', color='neutral', variant='plain',
        disableHiding, active, sx, CheckboxOnIcon, CheckboxOffIcon, value,
        useDropDownIndicator= false, hasCheckBox=false, checkBoxOn=false, pressed=false,
        component, slotProps={}, dropPosition={}, dropDownCB, onClick} = props;

    const tip= props.tip || props.title || '';
    const buttonPressed= pressed || active;
    const {current:divElementRef}= useRef({divElement:undefined});
    useImperativeHandle(fRef, () => divElementRef.divElement);
    const setupRef  = useCallback((c) => divElementRef.divElement= c, [divElementRef]);

    const handleClick= useCallback((ev) => {
        onClick?.(divElementRef.divElement,ev);
        dropDownCB ? dropDownCB(divElementRef.divElement) : dispatchHideDialog(DROP_DOWN_KEY);
    },[onClick,dropDownCB,divElementRef.divElement]);

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


    const image= makeImage(icon,imageStyle,allowInput);
    const iSize= iconButtonSize ? {'--IconButton-size': iconButtonSize} : {};

    // const image= icon ? <img src={icon} style={imageStyle} className={allowInput} /> : undefined;
    const useIconButton= icon && !text;
    // const dropDownIndicator= useDropDownIndicator ? makeImage(DROP_DOWN_ICON,undefined,allowInput) : undefined;
    const dropDownIndicator= useDropDownIndicator ? <ArrowDropDownRoundedIcon sx={{transform:'scale(1.75)'}}/> : undefined;

    // <ArrowDropDownRoundedIcon viewBox='8 8 10 10' sx={{position:'absolute', transform:'scale(1.5)', width:10,height:10, left:0, bottom:0}}/>



    const b=  (
        <Tooltip followCursor={true} title={tip} {...slotProps?.tooltip}>
            <Stack {...{direction:'row', sx, value, alignItems:'center', ref:setupRef, position:'relative' }} {...slotProps?.root}>
                <TbCheckBox {...{hasCheckBox, CheckboxOnIcon, CheckboxOffIcon, checkBoxOn, onClick:handleClick}}/>
                {useIconButton ?
                    (<IconButton {...{
                        sx: (theme) => (
                             {minHeight:'unset', minWidth:'unset', p:1/4, backgroundColor:'transparent',
                                 '& svg' : {
                                     color: enabled?
                                         theme.vars.palette[color]?.plainColor :
                                         theme.vars.palette[color]?.softDisabledColor,
                                 },
                                 opacity: enabled ? '1' : '0.3',
                                 ...makeBorder(active,theme,color),
                                 ...iSize,
                                 ['&[aria-pressed="true"]']: {
                                     ...theme.variants.outlinedActive.neutral,
                                     borderColor: theme.vars.palette.neutral.outlinedHoverBorder,
                                 },
                             }),

                        className:'ff-toolbar-iconbutton ' + allowInput,
                        value,
                        component,
                        variant:'soft', color:'neutral' ,
                        'aria-pressed': buttonPressed ? 'true' : 'false',
                        'aria-label':tip, onClick:handleClick, disabled:!enabled}}>
                        {image}
                    </IconButton>) :
                    <Button {...{color, variant, value,
                        'aria-label':tip, disabled:!enabled, onClick:handleClick,
                        size:'md',
                        className:'ff-toolbar-button ' + allowInput,
                        startDecorator: image,
                        component,
                        endDecorator: dropDownIndicator,
                        'aria-pressed':buttonPressed ? 'true' : 'false',
                        sx:(theme) => ({whiteSpace:'nowrap', py:1/4, minHeight: 'unset',
                            color: enabled? undefined : theme.vars.palette.neutral?.softDisabledColor,
                            ...makeFontSettings(theme),
                            ...makeBorder(active,theme,color),
                            ['&[aria-pressed="true"]']: {
                               ...theme.variants.outlinedActive.neutral,
                               borderColor: theme.vars.palette.neutral.outlinedHoverBorder,
                             },
                        }),
                        ...slotProps?.button
                    }}>
                        {makeTextLabel(text,shortcutKey)}
                    </Button>
                }
                {useIconButton && useDropDownIndicator &&
                    <DropDownIndicator {...{dropPosition,enabled,onClick:handleClick,className:allowInput}}/>}
            </Stack>
        </Tooltip>
    );

    return (!badgeCount&&!badgeAlert) ? b : <Badge {...{badgeContent:badgeAlert?'!':badgeCount,
        color:badgeAlert?'danger':undefined,
        sx:{'& .MuiBadge-badge': {top:'.7rem', right:'.4rem'}}}}> {b} </Badge>;
} );

ToolbarButton.propTypes= {
    icon : oneOfType([element,string]),
    text : oneOfType([element,string]),
    tip : string,
    value: string,
    title: string,
    pressed: bool,
    shortcutHelp : bool,
    badgeCount : number,
    badgeAlert: bool,
    enabled : bool,
    visible : bool,
    imageStyle : object,
    lastTextItem : bool,
    useDropDownIndicator: bool,
    hasCheckBox: bool,
    checkBoxOn: bool,
    CheckboxOnIcon:  element,
    CheckboxOffIcon: element,
    onClick : func,
    dropDownCB : func,
    disableHiding: bool,
    shortcutKey: string,
    color: string,
    iconButtonSize : string,
    slotProps: shape({
        root: object,     // because there are already too many props, this is used specifically to pass custom props to top level component
        tooltip: object,
        button: object,
    }),
    active: bool,
    sx: oneOfType([object,func]),
    component: string,
    dropPosition: object,
    variant: string,
};

const DropDownIndicator= ({dropPosition,enabled, onClick,className=''}) => (
    <Box {...{ className:`ff-toolbar-dropdown${className?' ':''}${className}`, onClick,
        sx: (theme) => ({
            minHeight:'unset', minWidth:'unset',backgroundColor:'transparent',
            padding:0, position:'absolute', bottom:'0px', left:'3px', ...dropPosition,
            opacity: enabled ? '1' : '0.3',
            '& svg' : {
                color: enabled?
                    theme.vars.palette.neutral?.plainColor :
                    theme.vars.palette.neutral?.softDisabledColor,
            },
        })
    }}>
        <ArrowDropDownRoundedIcon viewBox='8 8 10 10' sx={{position:'absolute', transform:'scale(1.5)', width:10,height:10, left:0, bottom:0}}/>
    </Box>
);

function TbCheckBox({hasCheckBox, CheckboxOnIcon, CheckboxOffIcon, checkBoxOn, onClick}) {
    if (!hasCheckBox) return undefined;
    if (CheckboxOnIcon && CheckboxOffIcon) {
        return (
            <Box onClick={onClick}>
                {checkBoxOn ? CheckboxOnIcon : CheckboxOffIcon}
            </Box>);
    }
    return (<Checkbox {...{variant:'plain', checked:checkBoxOn, onClick,
        sx:{ '.MuiCheckbox-checkbox': { background:'transparent' } }
    }}/>);
}

function makeBorder(active,theme,color) {
    const borderC= active ? theme.vars.palette[color]?.softActiveBg : 'transparent';
    return { border: `1px solid ${borderC}` };
}

function makeFontSettings(theme) {
    return {
        fontSize:theme.fontSize.md,
        fontWeight:theme.fontWeight.md,
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


export function ToolbarHorizontalSeparator() {
    return <Divider orientation='vertical' sx={{mx:1,my:1/2}}/>;
}
ToolbarHorizontalSeparator.propTypes= { style:object, top : number };

export function DropDownVerticalSeparator({useLine=false, sx={}}) {
    return useLine?
        <Divider orientation='horizontal' sx={{my:1, ...sx}}/> :
        <Box sx={sx} />;
}
DropDownVerticalSeparator.propTypes= {
    useLine: bool,
    style: object
};
