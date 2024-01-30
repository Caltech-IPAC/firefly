/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Badge, Box, Button, Chip, CircularProgress, Divider, IconButton, Stack, Tooltip, Typography} from '@mui/joy';
import React, {useCallback, useEffect} from 'react';
import PropTypes from 'prop-types';
import {filter,pick} from 'lodash';

import {COMMAND, dispatchSetMenu, getMenu} from '../core/AppDataCntlr.js';
import {getBackgroundInfo} from '../core/background/BackgroundUtil.js';
import {flux} from '../core/ReduxFlux.js';
import {dispatchHideDropDown, dispatchShowDropDown, getLayouInfo} from '../core/LayoutCntlr.js';
import {BgMonitorButton} from '../core/background/BgMonitorButton.jsx';
import QuestionMarkIcon from '@mui/icons-material/QuestionMark';
import {getTblIdsByGroup} from '../tables/TableUtil.js';
import {isDefined} from '../util/WebUtil.js';
import {DEFAULT_FITS_VIEWER_ID, getMultiViewRoot, getViewer} from '../visualize/MultiViewCntlr.js';
import {useStoreConnector} from './SimpleComponent.jsx';
import InsightsIcon from '@mui/icons-material/Insights';
import {ToolbarButton} from './ToolbarButton.jsx';

export function menuHandleAction (menuItem) {
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

function onClick(clickHandler,menuItem) {
    clickHandler ??= menuHandleAction;
    clickHandler(menuItem);
};



/**
 * Create the html for a menu item
 * @param p
 * @param p.menuItem
 * @param p.isSelected
 * @param p.clickHandler     onClick handler.  Defaults to #handleAction
 * @param p.isWorking
 * @param p.badgeCount
 * @param p.size
 * @param p.sx
 * @returns {JSX.Element}
 */
function MenuItem({menuItem, isSelected, size, clickHandler, isWorking=false, badgeCount=0, sx}) {
    return menuItem.type==='COMMAND' ?
        <MenuItemButton {...{menuItem, size, clickHandler, isWorking, badgeCount, sx}}/> :
        <MenuItemTab {...{menuItem, isSelected, size, clickHandler, isWorking, badgeCount, sx}}/>;
}


export function MenuItemButton({menuItem, size='lg', clickHandler, isWorking=false, badgeCount=0, sx}) {
    const variant= 'plain';
    const color= 'neutral';

    const startDecorator= isWorking ? <CircularProgress {...{sx:{'--CircularProgress-size':'12px'}, size:'sm' }}/> : undefined;

    const item=(
        <Button {...{ className: 'ff-MenuItem', sx, size, color, variant,
            onClick: () => onClick(clickHandler,menuItem) , startDecorator,}}>
            {menuItem.label}
        </Button>
    );
    return !badgeCount ? item : <Badge {...{badgeContent:badgeCount}}> {item} </Badge>;
}

function MenuItemTab({menuItem, isSelected, size='lg', clickHandler, isWorking=false, badgeCount=0, sx}) {
    const variant= 'soft';
    const color= 'primary';

    const startDecorator= isWorking ? <CircularProgress {...{sx:{'--CircularProgress-size':'12px'}, size:'sm' }}/> : undefined;

    const item=(
        <Stack direction={'row'} alignItems={'center'}>
            <Button {...{
                className: 'ff-MenuItem',
                size, color, variant, startDecorator,
                onClick: () => onClick(clickHandler,menuItem),
                sx: (theme) => {
                    const bC= `1px solid ${theme.vars.palette.primary.solidBg}`;
                    return ({
                        borderTop: isSelected ? bC : undefined,
                        borderLeft: isSelected ? bC : undefined,
                        borderRight: isSelected ? bC : undefined,
                        borderBottomRightRadius: 0,
                        borderBottomLeftRadius: 0,
                        background: isSelected ? theme.vars.palette.background.surface : undefined,
                        color: isSelected ? theme.vars.palette.text.primary : undefined,
                        ':hover': {
                            color: isSelected ? theme.vars.palette.primary.solidBg : undefined,
                            background: isSelected ? theme.vars.palette.background.surface : undefined,
                        },
                        ...sx
                    });
                },
                }}>
                {menuItem.label}
            </Button>
            <Box sx={{height:.6}}>
                <Divider orientation='vertical' sx={{height:1}}/>
            </Box>
        </Stack>
    );

    return !badgeCount ? item : <Badge {...{badgeContent:badgeCount}}> {item} </Badge>;
}

function itemVisible(menuItem) {
     const {visible, primary,type} = menuItem;
     if (type==='COMMAND') return true;
    return visible ?? primary;
}

function getSelected(menu,dropDown) {
    if (!menu || !dropDown?.visible) return '';
    if (menu.selected) return menu.selected;
    return menu.menuItems.find(({action}) => (action===dropDown?.view))?.action ?? menu.menuItems[0].action;
}

export function Menu() {
    const menu= useStoreConnector(() => getMenu());
    const {menuItems=[], showBgMonitor=true} = menu;
    const layoutInfo= getLayouInfo() ?? {};
    const {dropDown={}}=  layoutInfo;
    const selected= getSelected(menu,dropDown);

    useEffect(() => { // make sure the selected item is visible
        if (!selected) return;
        const selectedItem= menuItems.find(({action}) => (action===selected));
        if (!selectedItem) return;
        const isVisible=  selectedItem.visible ?? selectedItem.primary;
        if (!isVisible && selected === selectedItem.action) {
            const newMenuItems= menuItems.map( (mi) => mi===selectedItem ? {...mi, visible:true} : mi);
            dispatchSetMenu({...menu, menuItems:newMenuItems, selected});
        }
    }, [selected]);

    if (!menuItems?.length) return <div/>;

    const items = menuItems
        .filter(({action,type}) => (action!=='app_data.helpLoad' && type!=='COMMAND'))
        .filter(itemVisible)
        .map((item, idx) =>
            item.type==='COMMAND' ?
                <MenuItem key={idx} menuItem={item} isSelected={item.action === selected}/> :
                <MenuItemTab key={idx} menuItem={item} isSelected={item.action === selected}/>
        );

    const helpItem= menuItems.find(({action,type}) => (action==='app_data.helpLoad' && type==='COMMAND'));

    return (
        <Stack direction='row' justifyContent={'space-between'}>
            <Stack direction='row' spacing={2}>
                <Stack direction='row'>
                    {<ResultsTab {...{isSelected:!dropDown.visible}}/>}
                    {items}
                </Stack>
            </Stack>
            <Stack direction='row'>
                {showBgMonitor && <Divider orientation='vertical' sx={{mx:1}} />}
                {showBgMonitor && <BgMonitorButton/> }
                {Boolean(helpItem) && <Divider orientation='vertical' sx={{mx:1}} />}
                {Boolean(helpItem) && <AppHelpButton {...{ menuItem:helpItem, }}/>}
            </Stack>
        </Stack>
    );
}

function AppHelpButton({menuItem,sx}) {

    const onClick = useCallback(() => {
        menuHandleAction(menuItem);
    }, []);

    return (
        <IconButton {...{sx, variant:'plain', color:'neutral', onClick}}>
            <QuestionMarkIcon/>
        </IconButton>
    );
}

function ResultsTab({sx,isSelected}) {
    const {haveResults,tableCnt,imageCnt, bgTableCnt, pinChartCnt}= useStoreConnector(getCounts);
    const variant= haveResults?'solid' : 'soft';
    const color= 'success';
    const size= 'lg';

    const b= (
            <Stack direction={'row'} alignItems={'center'}>
            <Button {...{
                sx: (theme) => {
                    const bC= `1px solid ${theme.vars.palette.success.solidBg}`;
                    return ({
                        borderBottomRightRadius: 0,
                        borderBottomLeftRadius: 0,
                        background: isSelected ? theme.vars.palette.background.surface : undefined,
                        borderTop: isSelected ? bC : undefined,
                        borderLeft: isSelected ? bC : undefined,
                        borderRight: isSelected ? bC : undefined,
                        color: isSelected ? theme.vars.palette.text.primary : undefined,
                        ':hover': {
                            color: isSelected ? theme.vars.palette.success.solidBg : undefined,
                            background: isSelected ? theme.vars.palette.background.surface : undefined,
                        },
                        ...sx
                    });
                },
                size, color, variant,
                startDecorator:  <InsightsIcon/>,
                onClick:() => dispatchHideDropDown(),
            }}>
                Results
            </Button>
            <Box sx={{height:.6}}>
                <Divider orientation='vertical' sx={{height:1}}/>
            </Box>
        </Stack>
        );
    const badgeCnt=tableCnt+imageCnt+pinChartCnt;
    if (!badgeCnt) return b;
    return (
        <Tooltip title={(
            <Stack>
                {imageCnt>0 && <Typography>{`${imageCnt} image${imageCnt>1?'s':''} loaded`}</Typography>}
                {tableCnt>0 &&<Typography>{`${tableCnt} table${tableCnt>1?'s':''} loaded`}</Typography>}
                {pinChartCnt>0 &&<Typography>{`${pinChartCnt} pinned chart${pinChartCnt>1?'s':''}`}</Typography>}
                {bgTableCnt>0 &&<Typography>{`${bgTableCnt} table${bgTableCnt>1?'s':''} available in Background Monitor`}</Typography>}
            </Stack> )}>
            <Badge {...{badgeContent:badgeCnt, sx:{'& .MuiBadge-badge': {top:12, right:11}}  }}>
                {b}
            </Badge>
        </Tooltip>
    );

}


export function SideBarMenu({setOpen}) {

    const menu= useStoreConnector(() => getMenu());
    const {haveResults}= useStoreConnector(getCounts);
    const uploadItem= menu.menuItems?.find(({action}) => action==='FileUploadDropDownCmd');
    const menuItems= menu.menuItems?.filter(({type,action}) => type !== COMMAND && action!=='FileUploadDropDownCmd');
    const {dropDown={visible:false}}= getLayouInfo() ?? {};
    const selected= getSelected(menu,dropDown);


    const catList= [...new Set(menuItems.map( (mi) => mi.category ?? ''))];

    return (
        <Stack spacing={1} ml={1}>
            <Typography level='h3'>Choose Option</Typography>
            <Stack spacing={2} sx={{ml:3, '& .ff-toolbar-button' : {minWidth:'13rem', justifyContent:'flex-start'}}}>
                <Stack>
                    <ToolbarButton {...{
                        hasCheckBox:true,
                        checkBoxOn:!selected && !dropDown.visible,
                        // text:`Results${haveResults?'': '  (No results yet)'}`,
                        text: (
                            <Stack direction='row' spacing={1} alignItems='baseline'>
                                <Typography sx={{color:'unset'}}>Results</Typography>
                                {!haveResults && <Typography level='body-xs'>(No results yet)</Typography>}
                            </Stack>
                        ),
                        // icon:<InsightsIcon/>,
                        onClick: () => {
                            dispatchHideDropDown();
                            setOpen?.(false);
                        },
                        sx:(theme) =>(
                            {'& .ff-toolbar-button': {color:theme.vars.palette.success.solidBg},}
                        )
                    }} />
                    {uploadItem && <SideBarItem {...{key:'UPLOAD', item:uploadItem,selected,menu,setOpen}}/>}
                    {menuItems.filter( ({category}) => !category)
                        .map( (item) => (<SideBarItem {...{key:item.label, item,selected,menu,setOpen}}/>) )}
                </Stack>

                {catList.map( (cat) => {
                    return (
                        <Stack key={cat}>
                            {Boolean(cat) && <Typography key={cat} level='title-md'>{cat}</Typography>}
                            {
                                menuItems
                                    .filter( ({category}) => category===cat)
                                    .map( (item) => (<SideBarItem {...{key:item.label, item,selected,menu,setOpen}}/>) )
                            }
                        </Stack>
                    ); })
                }
            </Stack>
        </Stack>
    );
}


function SideBarItem({item,selected,menu,setOpen}) {

    const onClick= (menuItem) => {
        const newMenuItems= menu.menuItems.map( (mi) => mi===menuItem ? {...mi, visible:true} : mi);
        dispatchSetMenu({...menu, menuItems:newMenuItems});
        menuHandleAction(menuItem);
        setOpen?.(false);
    };

    if (!item) return <div>missing</div>;
    return (
        <Stack direction={'row'}>
            <ToolbarButton
                checkBoxOn={selected===item.action}
                hasCheckBox={true}
                onClick= {() => onClick(item)}
                text={item.label} />
            {(item.visible ?? item.primary) &&
                <Chip
                    sx={{height:'1.5rem'}}
                    onClick={() => {
                        const newMenuItems= menu.menuItems.map( (mi) => mi===item ? {...mi, visible:false} : mi);
                        const current= selected===item.action;
                        dispatchSetMenu({...menu, menuItems:newMenuItems,
                            selected:  current ? undefined : selected
                        });
                if (current) dispatchHideDropDown();
            }}>
                Hide Tab
                </Chip>}
        </Stack>
    );
}






export const PINNED_VIEWER_ID = 'PINNED_CHARTS_VIEWER';

function getCounts(prev={}) {
    const haveResults = filter(pick(getLayouInfo(), ['showTables', 'showXyPlots', 'showImages'])).length>0;
    const tableCnt= getTblIdsByGroup('main')?.length ?? 0;
    const imageCnt= getViewer(getMultiViewRoot(), DEFAULT_FITS_VIEWER_ID)?.itemIdAry?.length ?? 0;
    const pinChartCnt= getViewer(getMultiViewRoot(), PINNED_VIEWER_ID)?.itemIdAry?.length ?? 0;
    const {jobs={}}= getBackgroundInfo() ?? {};


    const bgTableCnt= Object.values(jobs)
        .filter((job) => job.jobInfo?.monitored && job.jobInfo?.type !== 'PACKAGE')?.length ?? 0;


    if (prev?.tableCnt===tableCnt && prev?.imageCnt===imageCnt &&
        bgTableCnt===prev.bgTableCnt && pinChartCnt===prev.pinChartCnt) return prev;
    return {haveResults,tableCnt,imageCnt,pinChartCnt,bgTableCnt};
}


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
