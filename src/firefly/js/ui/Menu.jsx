/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {
    Badge, Box, Button, Chip, CircularProgress, Divider, IconButton, ListItemDecorator,
    Stack, Tab, TabList, Tabs, Typography
} from '@mui/joy';
import {tabClasses} from '@mui/joy/Tab';
import {debounce} from 'lodash';
import React, {useCallback, useContext, useEffect, useState} from 'react';
import {COMMAND, dispatchAddPreference, dispatchSetMenu, getMenu, getPreference} from '../core/AppDataCntlr.js';
import {flux} from '../core/ReduxFlux.js';
import {dispatchHideDropDown, dispatchShowDropDown, getLayouInfo, getResultCounts} from '../core/LayoutCntlr.js';
import {BgMonitorButton} from '../core/background/BgMonitorButton.jsx';
import QuizOutlinedIcon from '@mui/icons-material/QuizOutlined';
import {AppPropertiesCtx} from './AppPropertiesCtx.jsx';
import {useStoreConnector} from './SimpleComponent.jsx';
import InsightsIcon from '@mui/icons-material/Insights';
import {ToolbarButton} from './ToolbarButton.jsx';
import FileUploadOutlinedIcon from '@mui/icons-material/FileUploadOutlined';

function menuHandleAction (menuItem) {
    if (menuItem.type === COMMAND) {
        flux.process({ type: menuItem.action, payload: (menuItem.payload ?? {}) });
    } else {
        dispatchShowDropDown( {view: menuItem.action});
    }
}

function onClick(clickHandler,menuItem) {
    clickHandler ??= menuHandleAction;
    clickHandler(menuItem);
}


export function Menu() {
    const [ready,setReady]= useState(false);
    const [windowWidth,setWindowWidth]= useState(window?.innerWidth??1000);
    const {appTitle} = useContext(AppPropertiesCtx);
    const menu= useStoreConnector(() => getMenu());
    const {menuItems=[], showBgMonitor=true} = menu;
    const layoutInfo= getLayouInfo() ?? {};
    const {dropDown={}}=  layoutInfo;
    const selected= getSelected(menu,dropDown);

    useEffect(() => {
        const doResize= () => setWindowWidth(window.innerWidth);
        const browserResizeCallback= debounce(doResize,20);
        window.addEventListener('resize', browserResizeCallback);
        return () => {
            window.removeEventListener('resize', browserResizeCallback);
        };
    },[]);



    useEffect(() => {
        if (!ready) setReady(true);
        const pref= getPreference(MENU_PREF_ROOT+appTitle);
        if (!pref) return;
        if (!menu.menuItems) return;
        const menuItems= menu.menuItems.map( (mi) => ({...mi, visible: pref[mi.action]}) );
        dispatchSetMenu({...menu, menuItems});
    }, []);

    useEffect(() => { // make sure the selected item is visible
        if (!selected) return;
        const selectedItem= menuItems.find(({action}) => (action===selected));
        if (!selectedItem) return;
        const isVisible=  selectedItem.visible ?? selectedItem.primary;
        if (!isVisible && selected === selectedItem.action) {
            const newMenuItems= menuItems.map( (mi) => mi===selectedItem ? {...mi, visible:true} : mi);
            updateMenu(appTitle, {...menu, menuItems:newMenuItems, selected});
        }
    }, [selected,ready]);

    if (!ready) return <div/>;

    const menuTabItems = menuItems
        ?.filter(({action,type}) => (action!=='app_data.helpLoad' && type!=='COMMAND'))
        ?.filter(itemVisible)
        ?.filter((item) => item.type!=='COMMAND');

    const helpItem= menuItems?.find(({action,type}) => (action==='app_data.helpLoad' && type==='COMMAND'));

    const bCntAdd= showBgMonitor?2:1;
    const size= getButtonSize(menuTabItems.length+bCntAdd,windowWidth);

    return (
        <Stack direction='row' justifyContent={'space-between'}>
            <MenuTabBarJoyTabBased {...{menuTabItems,size,selected,bCntAdd,windowWidth,dropDown}}/>
            <Stack direction='row'>
                {showBgMonitor && <Divider orientation='vertical' sx={{mx:1}} />}
                {showBgMonitor && <BgMonitorButton size={size}/> }
                {Boolean(helpItem) && <Divider orientation='vertical' sx={{mx:1}} />}
                {Boolean(helpItem) && <AppHelpButton {...{ menuItem:helpItem,size}}/>}
            </Stack>
        </Stack>
    );
}

export function MenuItemButton({menuItem, icon, size='lg', clickHandler, isWorking=false, badgeCount=0, sx}) {
    const variant= 'plain';
    const color= 'neutral';

    const startDecorator= isWorking ? <CircularProgress {...{sx:{'--CircularProgress-size':'12px'}, size:'sm' }}/> : undefined;

    const item=(
        icon ?
            (<IconButton {...{ className: 'ff-MenuItem', size:'lg', color, variant,
                onClick: () => onClick(clickHandler,menuItem)}}>
                {icon}
            </IconButton>) :
            (<Button {...{startDecorator, className: 'ff-MenuItem', size, color, variant,
                sx:{whiteSpace:'nowrap', ...sx},
                onClick: () => onClick(clickHandler,menuItem) }}>
                {menuItem.label}
            </Button>)
    );
    return !badgeCount ? item : <Badge {...{badgeContent:badgeCount}}> {item} </Badge>;
}



function tabDivider(activeBg,size) {
    return {
        '&[aria-selected="false"]': {            // add pipes after non-selected tabs
            '&::after': {
                content: '""',
                display: 'block',
                position: 'absolute',
                height: size==='lg'? '1.8rem' : size==='md' ? '1.25rem' : '.9rem',
                bottom: 6,
                width: 1,
                right: -3,
                zIndex: 1,                      //zIndex necessary so the hover does not cover pipe
                borderRightColor: 'divider',
                borderRightStyle: 'solid',
                borderRightWidth: '1px'
            },
        }
    };
}

function setupTabCss(theme,size) {
    return {
        ml:'1px',
        whiteSpace: 'nowrap',
        borderRadius: `${theme.radius[size]} ${theme.radius[size]} 0 0`,
        borderBottomWidth:0,
    };
}


function MenuTabBarJoyTabBased({menuTabItems, size, selected, dropDown}) {
    const resultValue= 'result';
    const tabSelected= dropDown.visible ? selected : resultValue;
    const activeBg = 'background.surface';
    const variant='soft';
    const color='primary';


    const doTabChange= (action) => {
        if (action===resultValue) {
            dispatchHideDropDown();
        }
        else {
            const clickItem= menuTabItems?.find( (i) => i.action===action) ;
            if (clickItem) onClick(clickItem.clickHandler,clickItem);
        }
    };


    const items= menuTabItems?.map((item, idx) => (
        <Tab {...{ key: idx, value:item.action, disableIndicator:true,
            sx: (theme) => ({ ...setupTabCss(theme,size) }) }} >
            {item.label}
        </Tab>
    )) ?? [];

    return (
        <Tabs {...{size, color, variant, value:tabSelected,
            sx: {alignSelf:'flex-end'}, onChange: (ev,val) => doTabChange(val) }} >
            <TabList {...{
                sx: (theme) => ( {
                    paddingBottom:0,
                    [`& .${tabClasses.root}`]: {
                        ...tabDivider(activeBg,size),
                        '&[aria-selected="true"]': {// apply this to the selected tab
                            background: theme.vars.palette.background.surface,
                            zIndex: 2,
                        }
                    }
                })

            }}>
                <Tab {...{key: resultValue, color, variant, value:resultValue, disableIndicator:true,
                    sx: (theme) => {
                        return ({
                            mb: '1px',
                            ...setupTabCss(theme,size),
                            color: theme.vars.palette.success.plainColor,
                            '&[aria-selected="true"]': { // apply this to the selected tab
                                mb: 0,
                                background: theme.vars.palette.background.surface,
                                color: theme.vars.palette.success.plainColor,
                            }
                        });
                    } }}>
                    <ListItemDecorator>
                        <InsightsIcon/>
                    </ListItemDecorator>
                    Results
                </Tab>
                {items}
            </TabList>
        </Tabs>
    );
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

const MENU_PREF_ROOT='menu-visibility-';
function updateMenu(appTitle, menu) {
    const pref= menu.menuItems
        .map( (mi)  => [mi.action, Boolean(mi.visible ?? mi.primary)] );
    dispatchAddPreference(MENU_PREF_ROOT+appTitle, Object.fromEntries(pref));
    dispatchSetMenu(menu);
}

function getButtonSize(buttonCnt,windowWidth) {
    if (buttonCnt<5 || windowWidth>1600) return 'lg';
    const offsetEst= 300;
    const lgButtonSizeEst=130;
    const mdButtonSizeEst=80;
    const width= windowWidth-offsetEst;
    if (width> buttonCnt*lgButtonSizeEst) return 'lg';
    if (width> buttonCnt*mdButtonSizeEst) return 'md';
    return 'sm';
}


function AppHelpButton({menuItem,sx,size='lg'}) {

    const onClick = useCallback(() => {
        menuHandleAction(menuItem);
    }, []);

    return (
        <IconButton {...{sx, size, variant:'plain', color:'neutral', onClick}}>
            <QuizOutlinedIcon/>
        </IconButton>
    );
}



export function SideBarMenu({closeSideBar, allowMenuHide}) {

    const {appTitle} = useContext(AppPropertiesCtx);
    const menu= useStoreConnector(() => getMenu());
    const {haveResults}= useStoreConnector(getCounts);
    const uploadItem= menu.menuItems?.find(({action}) => action==='FileUploadDropDownCmd');
    const menuItems= menu.menuItems?.filter(({type,action}) => type !== COMMAND && action!=='FileUploadDropDownCmd');
    const {dropDown={visible:false}}= getLayouInfo() ?? {};
    const selected= getSelected(menu,dropDown);


    const categoryList= menuItems ? [...new Set(menuItems.map( (mi) => mi.category ?? ''))] : [];

    return (
        <SideBarView {...{menu,appTitle, closeSideBar,haveResults,selected,dropDown,
            uploadItem,menuItems,categoryList, allowMenuHide}}/>
    );
}


function SideBarView({menu,appTitle,closeSideBar,haveResults,selected,dropDown,
                         uploadItem,menuItems,categoryList,allowMenuHide}) {
    const noCatItems= menuItems?.filter( ({category}) => !category) ?? [];
    return (
        <Stack >
            <Typography level='h4' sx={{ml:1}}>Choose Option</Typography>
            <Stack spacing={1} ml={5} mt={1}>
                <Stack style={{marginLeft:-16}} sx={{'& .ff-toolbar-button' : {minWidth:'13rem', justifyContent:'flex-start'}}}>
                    <Stack direction='row'>
                        <Typography color='success'  startDecorator={ <InsightsIcon sx={{width:20}}/> }/>
                        <Box ml={-1/2}/>
                        <ToolbarButton {...{
                            pressed: !selected && !dropDown.visible,
                            text: (
                                <Stack direction='row' spacing={1} alignItems='baseline'>
                                    <Typography sx={{color:'unset'}}>Results</Typography>
                                    {!haveResults && <Typography level='body-xs'>(No results yet)</Typography>}
                                </Stack>
                            ),
                            onClick: () => {
                                dispatchHideDropDown();
                                closeSideBar();
                            },
                            sx:(theme) =>( {
                                    '& .ff-toolbar-button': {color:theme.vars.palette.success.solidBg
                                    },}
                            )
                        }} />
                    </Stack>
                    {uploadItem &&
                        <Stack direction='row'>
                            <FileUploadOutlinedIcon/>
                            <Box ml={-1/4}/>
                            <SideBarItem {...{key:'UPLOAD', item:uploadItem,selected,
                                menu,closeSideBar,allowMenuHide}}/>
                        </Stack>
                    }
                </Stack>
                {Boolean(noCatItems?.length) &&
                    <Stack sx={{pl:1, '& .ff-toolbar-button' : {minWidth:'13rem', justifyContent:'flex-start'}}}>
                        {menuItems.filter( ({category}) => !category)
                            .map( (item) => (<SideBarItem {...{key:item.label, item,selected,menu,closeSideBar, allowMenuHide}}/>) )}
                    </Stack>
                }
                <Stack spacing={1} sx={{ml:3, '& .ff-toolbar-button' : {minWidth:'13rem', justifyContent:'flex-start'}}}>
                    {categoryList.map( (cat) => {
                        return (
                            <Stack key={cat}>
                                {Boolean(cat) && <Typography key={cat} sx={{ml:-2}} level='title-md'>{cat}</Typography>}
                                {
                                    menuItems
                                        .filter( ({category}) => category===cat)
                                        .map( (item) => (<SideBarItem {...{key:item.label, item,selected,menu,closeSideBar, allowMenuHide}}/>) )
                                }
                            </Stack>
                        ); })
                    }
                </Stack>

                {allowMenuHide && tabsUpdated(menu) && <Stack pt={1}>
                    <Chip onClick={() => {
                        const menuItems= menu.menuItems.map( (mi) => ({...mi, visible: undefined}) );
                        dispatchHideDropDown();
                        updateMenu(appTitle, {...menu, menuItems});
                    } }>
                        Reset Tabs to Default
                    </Chip>
                </Stack>}
            </Stack>
        </Stack>
    );
}

function tabsUpdated(menu) {
    if (!menu?.menuItems) return false;
    return menu.menuItems.some( (mi) => {
        return (mi.visible??mi.primary)!==mi.primary;
    });
}


function SideBarItem({item,selected,menu,closeSideBar,allowMenuHide,icon,sx, hideSx}) {
    const {appTitle} = useContext(AppPropertiesCtx);

    const onClick= (menuItem) => {
        const newMenuItems= menu.menuItems.map( (mi) => mi===menuItem ? {...mi, visible:true} : mi);
        updateMenu(appTitle, {...menu, menuItems:newMenuItems});
        menuHandleAction(menuItem);
        closeSideBar();
    };

    if (!item) return <div>missing</div>;
    return (
        <Stack direction='row' alignItems='center' spacing={1} sx={sx}>
            <ToolbarButton icon={icon} pressed={selected===item.action} onClick= {() => onClick(item)} text={item.label} />
            {(allowMenuHide && (item.visible ?? item.primary)) &&
                <Chip {...{
                    className: 'hideTab',
                    sx:{height:'1.5rem'},
                    onClick:() => {
                        const newMenuItems= menu.menuItems.map( (mi) => mi===item ? {...mi, visible:false} : mi);
                        const current= selected===item.action;
                        updateMenu(appTitle, {...menu, menuItems:newMenuItems, selected:  current ? undefined : selected });
                        if (current) dispatchHideDropDown();
                    }
                }}>
                    Hide Tab
                </Chip>
            }
        </Stack>
    );
}




function getCounts(prev={}) {
    const {haveResults,tableCnt,imageCnt,pinChartCnt,bgTableCnt}= getResultCounts();
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

