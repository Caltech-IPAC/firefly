/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Badge, Button, CircularProgress, Divider, IconButton, Stack} from '@mui/joy';
import React, {useCallback} from 'react';
import PropTypes from 'prop-types';

import {COMMAND, getMenu} from '../core/AppDataCntlr.js';
import {flux} from '../core/ReduxFlux.js';
import {dispatchShowDropDown, getLayouInfo} from '../core/LayoutCntlr.js';
import {BgMonitorButton} from '../core/background/BgMonitorButton.jsx';
// import QuestionMarkIcon from '@mui/icons-material/QuestionMark';
import QuizOutlinedIcon from '@mui/icons-material/QuizOutlined';

function handleAction (menuItem) {

    // set whether search menu should be shown
    if (menuItem.type === COMMAND) {
        flux.process({
            type: menuItem.action,
            payload: (menuItem.payload ?? {})
        });
    } else {
        dispatchShowDropDown( {view: menuItem.action});
    }
}

/**
 * Create the html for a menu item
 * @param p
 * @param p.menuItem
 * @param p.isSelected
 * @param p.clickHandler     onClick handler.  Defaults to #handleAction
 * @param p.isWorking
 * @param p.badgeCount
 * @param p.size
 * @returns {JSX.Element}
 */
export function MenuItem({menuItem, isSelected, size='lg', clickHandler, isWorking=false, badgeCount=0, sx}) {
    const onClick = () => {
        clickHandler ??= handleAction;
        clickHandler(menuItem);
    };

    // const variant= menuItem.type==='COMMAND' ? 'plain' : isSelected?'solid' : 'soft';
    const variant= menuItem.type==='COMMAND' ? 'plain' : 'soft';
    const color= menuItem.type==='COMMAND' ? 'neutral' : 'primary';

    const startDecorator= isWorking ? <CircularProgress {...{sx:{'--CircularProgress-size':'12px'}, size:'sm' }}/> : undefined;

    const item=(
        <Button {...{
            className: 'ff-MenuItem',
            sx: (theme) => {
                return ({
                    borderBottomRightRadius: menuItem.type !== 'COMMAND' ? 0 : undefined,
                    borderBottomLeftRadius: menuItem.type !== 'COMMAND' ? 0 : undefined,
                    background: isSelected ? theme.vars.palette.background.surface : undefined,
                    color: isSelected ? theme.vars.palette.text.primary : undefined,
                    ':hover': {
                        color: isSelected ? theme.vars.palette.primary.solidBg : undefined,
                        background: isSelected ? theme.vars.palette.background.surface : undefined,
                    },
                    ...sx
                });
            },
            size, color, variant, onClick, startDecorator}}>
            {menuItem.label}
        </Button>
    );

    return !badgeCount ? item : <Badge {...{badgeContent:badgeCount}}> {item} </Badge>;

}

export function Menu({menu={}}) {
    const {menuItems, showBgMonitor=true} = menu;
    if (!menuItems.length) return <div/>;

    let selected= menu.selected;

    const selectedItem= menuItems.find(({action}) => (action===selected));
    const {dropDown={}}= getLayouInfo() ?? {};
    if (!selectedItem && dropDown.visible) {
        selected= menuItems.find(({action}) => (action===dropDown.view))?.action ?? menuItems[0].action;
    }

    const items = menuItems
        .filter(({action,type}) => (action!=='app_data.helpLoad' && type!=='COMMAND'))
        .map((item, idx) => <MenuItem key={idx} menuItem={item} isSelected={item.action === selected}/>);


    const helpItem= menuItems.find(({action,type}) => (action==='app_data.helpLoad' && type==='COMMAND'));

    const hasRight= Boolean(showBgMonitor || helpItem);

    return (
        <Stack direction='row' justifyContent={'space-between'}>
            <Stack direction='row'> {items} </Stack>
            <Stack direction='row'>
                {showBgMonitor && <Divider orientation='vertical' sx={{mx:1}} />}
                {showBgMonitor && <BgMonitorButton/> }
                {Boolean(helpItem) && <Divider orientation='vertical' sx={{mx:1}} />}
                {Boolean(helpItem) && <AppHelpButton {...{ menuItem:helpItem, }}/>}
            </Stack>
        </Stack>
    );
}

export function AppHelpButton({menuItem,sx}) {

    const onClick = useCallback(() => {
        handleAction(menuItem);
    }, []);

    return (
        <IconButton {...{sx, size:'lg', variant:'plain', color:'neutral', onClick}}>
            <QuizOutlinedIcon/>
        </IconButton>
    );
}




Menu.propTypes = {
    menu   : PropTypes.object.isRequired
};

/**
 * returns an array menuItems
 * @returns {*}
 */
export function getMenuItems() {
    return getMenu()?.menuItems;
}

/**
 * returns an array of drop down actions from menu items
 * @returns {*}
 */
export function getDropDownNames() {
    const menuItems = getMenuItems();
    if (!Array.isArray(menuItems)) return [];
    return menuItems.filter((mi) => mi.type !== COMMAND)
                    .map((mi) => mi.action);
}
