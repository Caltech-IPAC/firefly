/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import PendingActionsOutlinedIcon from '@mui/icons-material/PendingActionsOutlined.js';
import {
    Badge, Button, Chip, CircularProgress, Divider, IconButton, ListItemDecorator, Sheet,
    Stack, Tab, TabList, Tabs, Tooltip, Typography
} from '@mui/joy';
import {tabClasses} from '@mui/joy/Tab';
import {debounce} from 'lodash';
import React, {forwardRef, memo, useCallback, useContext, useEffect, useRef, useState} from 'react';
import shallowequal from 'shallowequal';
import {
    COMMAND, dispatchAddPreference, dispatchSetMenu,
    getMenu, getPreference, getSelectedMenuItem, getUserInfo
} from '../core/AppDataCntlr.js';
import {getBackgroundInfo, isActive} from '../core/background/BackgroundUtil.js';
import {flux} from '../core/ReduxFlux.js';
import {dispatchHideDropDown, dispatchShowDropDown, getLayouInfo, getResultCounts} from '../core/LayoutCntlr.js';
import QuizOutlinedIcon from '@mui/icons-material/QuizOutlined';
import {AppPropertiesCtx} from './AppPropertiesCtx.jsx';
import {useStoreConnector} from './SimpleComponent.jsx';
import InsightsIcon from '@mui/icons-material/Insights';
import {ToolbarButton} from './ToolbarButton.jsx';
import {logout} from 'firefly/rpc/CoreServices';
import {useColorMode} from 'firefly/ui/FireflyRoot.jsx';

const UploadCmd= 'FileUploadDropDownCmd';
const TapCmd= 'TAPSearch';
const ResultCmd= 'result';
const MENU_PREF_ROOT='menu-visibility-';


function menuHandleAction (menuItem) {

    if (menuItem.type === COMMAND) {
        flux.process({ type: menuItem.action, payload: (menuItem.payload ?? {}) });
    } else {
        dispatchShowDropDown( {view: menuItem.action, menuItem});
    }
}

function onClickHandler(clickHandler,menuItem) {
    clickHandler ??= menuHandleAction;
    clickHandler(menuItem);
}

export const menuTabsBorderSx = (theme) => ({
    borderWidth: 1,
    borderStyle: 'solid',
    borderColor: theme.vars.palette.neutral.outlinedBorder //if we need blue accent, can use primary.outlinedBorder or primary.outlinedActiveBg (lighter)
});


export function Menu() {
    const [ready,setReady]= useState(false);
    const [,setWindowWidth]= useState(window?.innerWidth??1000);
    const {appTitle, showUserInfo} = useContext(AppPropertiesCtx);
    const menu= useStoreConnector(() => getMenu());
    const {menuItems=[], showBgMonitor=true} = menu;
    const layoutInfo= getLayouInfo() ?? {};
    const {dropDown={}}=  layoutInfo;
    const selected= getSelectedMenuItem(menu,dropDown);

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

    return (<AdjustableMenu
            {...{menuTabItems,helpItem,selected,dropDown, showBgMonitor,showUserInfo}}/>
    );
}


function AdjustableMenu({menuTabItems, helpItem, selected, dropDown, showBgMonitor,showUserInfo}) {
    const [maskTestRender,setMaskTestRender]= useState(false);
    const [tabCount,setTabCount]= useState(-1); // use for forcing re-renders, don't start tracking until we know the tab bar element
    const {current:tbarElement}= useRef({element:undefined});
    const {current:tabRenderedInfo}= useRef({tabWidths:{}});
    const {current:lastButtonSize}= useRef({size:'lg'});
    const showHelp= Boolean(helpItem);

    useEffect(() => {
        // force a rerender the first time size is small
        if (lastButtonSize.size!=='sm' && maskTestRender) setMaskTestRender(false);
        if (lastButtonSize.size==='sm' && !maskTestRender) setMaskTestRender(true);
    } );

    useEffect(() => {
        if (!tbarElement.element) return;
        setTabCount(menuTabItems.length); // force a rerender if number of tabs change
    }, [menuTabItems.length]);

    const setTabBarElement= useCallback((ts) => {
        tbarElement.element= ts;
        if (ts && tabCount===-1) setTabCount(menuTabItems.length); // force a rerender when we know the html element of the tab bar
    },[]);

    const setElement= useCallback( (key,e) => {
        if (!e) return;
        const {tabWidths}= tabRenderedInfo;
        tabWidths[key]= Math.trunc(e.getBoundingClientRect()?.width ?? 0);
        Object.keys(tabWidths).forEach( (key) => {
            if (Number(key)>=menuTabItems.length) tabWidths[key]= undefined;
        });
    }, [menuTabItems.length]);

    const selectedIdx= menuTabItems?.findIndex( ({action}) => action===selected);
    const {size,displayMask}= getTabDisplayInfo(selectedIdx, tabRenderedInfo.tabWidths,tbarElement.element,
        lastButtonSize.size, showBgMonitor,showHelp,showUserInfo);

    lastButtonSize.size= size;

    const divider= <Divider {...{orientation:'vertical', sx:{ ':first-of-type':  {ml:0}, mx:0.5, mt: 1.25, mb:3/4} }}/>;

    return (
        <Stack direction='row' justifyContent={'space-between'} alignItems='flex-end' ref={(c) => setTabBarElement(c) }>
            <MenuTabBar {...{menuTabItems,size,selected,dropDown, displayMask, setElement}}/>
            <Stack {...{direction:'row', alignItems:'center', alignSelf:'center', divider}} >
                {size==='lg' && showBgMonitor  && <React.Fragment/>}
                {showBgMonitor && <BgMonitorButton size={size}/> }
                {showHelp && <AppHelpButton {...{ menuItem:helpItem,size}}/>}
                {showUserInfo && <UserInfo/>}
            </Stack>
        </Stack>
    );
}

function MenuItemButton({menuItem, icon, size='lg', clickHandler, isWorking=false, badgeCount=0, sx}) {
    const variant= 'plain';
    const color= 'neutral';

    const startDecorator= isWorking ? <CircularProgress {...{sx:{'--CircularProgress-size':'12px'}, size:'sm' }}/> : undefined;

    const item=(
        icon ?
            (<IconButton {...{ className: 'ff-MenuItem', size, color, variant,
                onClick: () => onClickHandler(clickHandler,menuItem)}}>
                {icon}
            </IconButton>) :
            (<Button {...{startDecorator, className: 'ff-MenuItem', size, color, variant,
                sx:{whiteSpace:'nowrap', ...sx},
                onClick: () => onClickHandler(clickHandler,menuItem) }}>
                {menuItem.label}
            </Button>)
    );
    return !badgeCount ? item : <Badge {...{badgeContent:badgeCount}}> {item} </Badge>;
}



function tabDivider(size, placeAtEnd=true) {
    return {
        '&[aria-selected="false"]': {            // add pipes after non-selected tabs
            [`&::${placeAtEnd ? 'after' : 'before'}`]: {
                content: '""',
                display: 'block',
                position: 'absolute',
                height: 0.6,
                width: 1,
                [placeAtEnd ? 'right' : 'left']: -3,
                zIndex: 1,                      //zIndex necessary so the hover does not cover pipe
                [placeAtEnd ? 'borderRight': 'borderLeft']: '1px solid var(--joy-palette-divider)',
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
        height: 1,
        backgroundColor: 'transparent'
    };
}


function MenuTabBar({menuTabItems=[], size, selected, dropDown, displayMask, setElement}) {
    const tabSelected= selected || ResultCmd;
    const variant='soft';
    const color='primary';


    const tabItems= [
        <ResultsTab {...{key:'results-tab', size, color, variant, ref: (c) => setElement('results-tab ',c)}}/>,
        ...menuTabItems
            .map(({action,label,title}, idx) =>
            {
                const tab= (
                    <Tab {...{ key: idx, value:action, disableIndicator:true, color, variant,
                        ref: (c) => setElement(idx+'',c),
                        sx: (theme) => ({ ...setupTabCss(theme,size) }) }} >
                        {label}
                    </Tab>);
                const tip= getTip(title,action);
                return tip ? <Tooltip key={idx} title={tip}>{tab}</Tooltip> : tab;
            }
            )
            .filter((item,idx) => displayMask ? displayMask[idx] : true)
    ];

    return (
        <Tabs {...{size, value:tabSelected,
            sx: {minHeight: '2.5rem', height:1, mt:0, backgroundColor: 'transparent'}, onChange: (ev,action) => doTabChange(action,menuTabItems) }} >
            <TabList {...{
                sx: (theme) => ( {
                    boxShadow: 'none', //hide the default underline created by TabList as inset box-shadow
                    paddingBottom:0,
                    height: 1,
                    transform: 'translate(0px, 1px)', //to overlap banner's bottom border to create bleeding effect; 0.25 * (py of banner)
                    [`& .${tabClasses.root}`]: {
                        ...tabDivider(size),
                        '&[aria-selected="true"]': { // apply to selected tab
                            background: theme.vars.palette.background.surface,
                            fontWeight: 'md',
                            zIndex: 2,
                            ...menuTabsBorderSx(theme),
                            borderBottomColor: 'transparent', //don't show border at bottom to let active tab's background bleed into tab panel
                            // boxShadow: '0px -2px 8px -2px rgba(0 0 0 / 0.24)', //theme.shadow.md
                        }
                    }
                })
            }}>
                {tabItems}
            </TabList>
        </Tabs>
    );
}

function itemVisible(menuItem) {
     const {visible, primary,type} = menuItem;
     if (type==='COMMAND') return true;
    return visible ?? primary;
}

function updateMenu(appTitle, menu) {
    const pref= menu.menuItems
        .map( (mi)  => [mi.action, Boolean(mi.visible ?? mi.primary)] );
    dispatchAddPreference(MENU_PREF_ROOT+appTitle, Object.fromEntries(pref));
    dispatchSetMenu(menu);
}

function getTabBarRealWidth(tabBarElement) {
    const {left: tabBarLeft, width: tabBarWidth} = tabBarElement.getBoundingClientRect() ?? {width: 0, left: 0};
    const tabBarRealWidth = (window.innerWidth < (tabBarLeft + tabBarWidth)) ? window.innerWidth - tabBarLeft : tabBarWidth;
    return tabBarRealWidth;
}


function getTabDisplayInfo(selectedIdx,tabWidths,tabBarElement,lastButtonSize,showBgMonitor,showHelp,showUserInfo) {
    const size= getButtonSize(tabWidths,tabBarElement,lastButtonSize,showBgMonitor,showHelp,showUserInfo);
    const displayMask= size==='sm' && lastButtonSize==='sm' ?
        getButtonDisplayMask(tabWidths, tabBarElement, selectedIdx, showBgMonitor,showHelp,showUserInfo) : undefined;
    return {size,displayMask};
}

const sumAry= (ary) => ary.reduce( (total,num) => num ? total+num : total,0);

function getButtonSize(tabWidths,tabBarElement,lastButtonSize,showBgMonitor,showHelp,showUserInfo) {
    if (!tabBarElement) return 'lg';

    const tabBarRealWidth =  getTabBarRealWidth(tabBarElement);
    const sumTabWidth= sumAry(Object.values(tabWidths));

    const bgMon= showBgMonitor ? lastButtonSize==='lg'?175:50 : 0;
    const bTotal= sumTabWidth+ bgMon + (showHelp?50:0) + (showUserInfo?50:0);

    if (lastButtonSize==='lg') {
        if (bTotal < tabBarRealWidth) return 'lg';
        if ((bTotal*.87) < tabBarRealWidth) return 'md';
    }
    else if (lastButtonSize==='md') {
        if ( (bTotal*1.30) < tabBarRealWidth) return 'lg';
        if ( (bTotal) < tabBarRealWidth) return 'md';
    }
    else {
        if ( (bTotal*1.3) < tabBarRealWidth) return 'lg';
        if ( (bTotal*1.18) < tabBarRealWidth) return 'md';
    }
    return 'sm';
}


function getButtonDisplayMask(tabWidths,tabBarElement,selectedTabIdx, showBgMonitor,showHelp,showUserInfo) {
    const usedWith= (showBgMonitor?50:0) + (showHelp?50:0) + (showUserInfo?50:0);
    const tabBarRealWidth =  getTabBarRealWidth(tabBarElement) - usedWith;
    const sizeAry= Object.values(tabWidths);
    const maskAry= sizeAry.map( () => true);

    for(let i=sizeAry.length; (i>0); i--) {
        const tstAry= sizeAry.filter( (item,idx) => maskAry[idx]);
        if (i-1!==selectedTabIdx && sumAry(tstAry)>tabBarRealWidth) maskAry[i-1]=false;
    }
    return maskAry;
}

function AppHelpButton({menuItem,sx,size='lg'}) {
    const {isDarkMode} = useColorMode();
    menuItem.payload = {...menuItem.payload, isDarkMode};
    const onClick = useCallback(() => {
        menuHandleAction(menuItem);
    }, []);

    return (
        <IconButton {...{sx, size, variant:'plain', color:'neutral', onClick}}>
            <QuizOutlinedIcon/>
        </IconButton>
    );
}


const UserInfo= memo(() => {
    const userInfo = useStoreConnector(() => getUserInfo() ?? {});

    const {loginName='Guest', firstName='', lastName='', login_url, logout_url} = userInfo;
    const isGuest = loginName === 'Guest';
    const onLogin = () => login_url && (window.location = login_url);
    const onLogout = () => {
        if (logout_url) window.location = logout_url;
        void logout();
    };

    const fn= (firstName && firstName.trim().toLowerCase()!=='null') ? firstName : '';
    const ln= (lastName && lastName.trim().toLowerCase()!=='null') ? lastName : '';
    const displayName = (fn || ln) ? `${fn} ${ln}` : loginName;

    return (
        <Stack spacing={1/4}
               px='0.75rem' //0.75rem px is used by BG monitor button
               justifyContent='center'
               alignItems='center'>
            <Typography level='body-xs' title={displayName}>{displayName}</Typography>
            {!isGuest && <Chip onClick={onLogout}>Logout</Chip>}
            {isGuest && <Chip onClick={onLogin}>Login</Chip>}
        </Stack>
    );
});



export function SideBarMenu({closeSideBar, allowMenuHide}) {
    const {appTitle} = useContext(AppPropertiesCtx);
    const menu= useStoreConnector(() => getMenu());
    const {haveResults}= useStoreConnector(getCounts);
    const uploadItem= menu.menuItems?.find(({action}) => action===UploadCmd);
    const menuItems= menu.menuItems?.filter(({type,action}) => type !== COMMAND && action!==UploadCmd);
    const {dropDown={visible:false}}= getLayouInfo() ?? {};
    const selected= getSelectedMenuItem(menu,dropDown);
    const categoryList= menuItems ? [...new Set(menuItems.map( (mi) => mi.category ?? ''))] : [];

    return (
        <SideBarView {...{menu,appTitle, closeSideBar,haveResults,selected,dropDown,
            uploadItem,menuItems,categoryList, allowMenuHide}}/>
    );
}


function SideBarView({menu,appTitle,closeSideBar,haveResults,selected,dropDown,
                         uploadItem,menuItems,categoryList,allowMenuHide}) {
    const noCatItems= menuItems?.filter( ({category}) => !category) ?? [];
    const itemLayoutSx = {alignItems:'center', justifyContent:'space-between', '.MuiChip-root': {ml: 1.5, height:'1.5rem'}};
    const itemMinWidth = '13.5rem';
    const groupSpacingY = 0.5;
    const showReset= allowMenuHide && tabsUpdated(menu);

    return (
        <Stack mt={1/2}>
            <Divider orientation='horizontal'/>
            <Sheet variant='soft'>
                <Stack direction='row' sx={{py: '.25rem', px:'.75rem',...itemLayoutSx}}>
                    <Typography level='h4' sx={{minWidth: itemMinWidth}}>Tab Selection</Typography>
                    {showReset &&
                        <Tooltip title={'Reset visibility of all the tabs to default'}>
                            <Chip
                                sx={{mr:1/2}}
                                variant='outlined'
                                onClick={() => {
                                    const newMI= menu.menuItems.map( (mi) => ({...mi, visible: undefined}) );
                                    const selected= newMI.find( (m) => m.action===menu.selected && m.primary)?.action;
                                    if (!selected) dispatchHideDropDown();
                                    updateMenu(appTitle, {...menu, selected, menuItems: newMI});
                                }}>
                                Reset All
                            </Chip>
                        </Tooltip>
                    }
                </Stack>
            </Sheet>

            <Stack
                   sx={{pt:1, pl: '0.75rem', pr: '1rem', //0.75 rem is the left padding used by accordions
                       '& .ff-toolbar-button' : {minWidth: itemMinWidth, justifyContent:'flex-start', fontWeight: 'initial'}
                   }}>

                <Stack spacing={2} >
                    {/* No-category Group */}
                    <Stack spacing={groupSpacingY}>
                        {/* Results */}
                        <ResultsTip>
                            <ToolbarButton {...{
                                pressed: !selected && !dropDown.visible,
                                text: (
                                    <Stack direction='row' spacing={.5} alignItems='center'>
                                        <InsightsIcon color='success'/>
                                        <Stack direction='row' spacing={1} alignItems='baseline'>
                                            <Typography color='success' /*sx={{color:'unset'}}*/>Results</Typography>
                                            {!haveResults && <Typography level='body-xs'>(No results yet)</Typography>}
                                        </Stack>
                                    </Stack>
                                ),
                                onClick: () => {
                                    dispatchHideDropDown();
                                    closeSideBar();
                                },
                                sx:(theme) =>( {
                                        '& .ff-toolbar-button': {color:theme.vars.palette.success.solidBg },}
                                )
                            }} />
                        </ResultsTip>

                        {/* Upload */}
                        {uploadItem &&
                            <SideBarItem {...{key:'UPLOAD', item:uploadItem,selected,
                                menu,closeSideBar,allowMenuHide, sx:itemLayoutSx}}/>
                        }

                        {/* Other no-category items, if any */}
                        {Boolean(noCatItems?.length) &&
                            menuItems
                                .filter( ({category}) => !category )
                                .map( (item) => (<SideBarItem {...{key:item.label, item,selected,menu,closeSideBar, allowMenuHide, sx:itemLayoutSx}}/>) )
                        }
                    </Stack>

                    {/* Category Groups */}
                    {categoryList.map((cat) => (
                        <Stack key={cat} spacing={groupSpacingY}>
                            {Boolean(cat) && <Typography key={cat} level='title-sm'>{cat}</Typography>}
                            {
                                menuItems
                                    .filter( ({category}) => category===cat )
                                    .map( (item) => (<SideBarItem {...{key:item.label, item,selected,menu,closeSideBar, allowMenuHide, sx:itemLayoutSx}}/>) )
                            }
                        </Stack>
                    ))}
                </Stack>
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

function getTip(title, action) {
    if (title) return title;
    if (action===UploadCmd) return  'Upload catalogs, tables, FITS, MOCs, or UWS job file';
    if (action===TapCmd) return 'General TAP search for any TAP server';
    return undefined;
}

function doTabChange(action,menuTabItems) {
    if (action===ResultCmd) {
        dispatchHideDropDown();
    }
    else {
        const clickItem= menuTabItems?.find( (i) => i.action===action) ;
        if (clickItem) onClickHandler(clickItem.clickHandler,clickItem);
    }
}


const workingIndicator= (
    <CircularProgress
        color='success'
        sx={{
            '--CircularProgress-size': '18px',
            '--CircularProgress-progressThickness': '2px',
            '--CircularProgress-trackThickness': '2px',
            '--CircularProgress-circulation': '1.2s linear 0s infinite normal none running',
        }}
        style= {{
            '--CircularProgress-percent': '35', // for some reason joyUI put this in style and this is the only way to override
        }}
    />);


const ResultsTab= forwardRef(({size, color, variant},ref) =>{
    const {tableLoadingCnt, imageLoadingCnt}= useStoreConnector(getCounts);
    const loading= (tableLoadingCnt+imageLoadingCnt)>0;


    const tab= (
        <Tab {...{color, variant, value:ResultCmd, disableIndicator:true, ref,
            sx: (theme) => {
                return ({
                    ...setupTabCss(theme,size),
                    ...tabDivider(size,false), //add a divider at start too
                    color: theme.vars.palette.success.plainColor,
                    '&[aria-selected="true"]': { // apply this to the selected tab
                        color: theme.vars.palette.success.plainColor,
                    }
                });
            } }}>
            <ListItemDecorator>
                {loading ? workingIndicator: <InsightsIcon/>}
            </ListItemDecorator>
            Results
        </Tab>
    );

    return <ResultsTip>{tab}</ResultsTip>;
});

function ResultsTip({useBadge=false,children}) {
    const {haveResults,tableCnt,tableLoadingCnt, imageCnt, imageLoadingCnt, bgTableCnt, pinChartCnt}= useStoreConnector(getCounts);
    const badgeCnt=useBadge && tableCnt+imageCnt+pinChartCnt;
    if (!haveResults) return children;
    const ttWrap= (
        <Tooltip
            followCursor={true}
            title={(
                <Stack>
                    {imageCnt>0 && <Typography>{`${imageCnt} image${imageCnt>1?'s':''}`}</Typography>}
                    {imageLoadingCnt>0 && <Typography>{`${imageLoadingCnt} image${imageLoadingCnt>1?'s':''} still loading`}</Typography>}
                    {tableCnt>0 && <Typography>{`${tableCnt} table${tableCnt>1?'s':''}`}</Typography>}
                    {tableLoadingCnt>0 && <Typography>{`${tableLoadingCnt} table${tableLoadingCnt>1?'s':''} still loading`}</Typography>}
                    {pinChartCnt>0 && <Typography>{`${pinChartCnt} pinned chart${pinChartCnt>1?'s':''}`}</Typography>}
                    {bgTableCnt>0 && <Typography>{`${bgTableCnt} table${bgTableCnt>1?'s':''} in Background Monitor`}</Typography>}
                </Stack> )}>
            {children}
        </Tooltip>
    );
    return !badgeCnt ? ttWrap :
        <Badge {...{badgeContent:badgeCnt, sx:{'& .MuiBadge-badge': {top:12, right:11, zIndex:2}}  }}>
            {ttWrap}
        </Badge>;
}


function SideBarItem({item,selected,menu,closeSideBar,allowMenuHide,icon,sx}) {
    const {appTitle} = useContext(AppPropertiesCtx);

    const onClick= (menuItem) => {
        const newMenuItems= menu.menuItems.map( (mi) => mi===menuItem ? {...mi, visible:true} : mi);
        updateMenu(appTitle, {...menu, menuItems:newMenuItems});
        onClickHandler(menuItem.clickHandler, menuItem);
        closeSideBar();
    };

    if (!item) return <div>missing</div>;
    const {title,action,label,visible,primary}= item;
    return (
        <Stack direction='row' sx={sx}>
            <ToolbarButton tip={getTip(title,action)} icon={icon} text={label}
                           pressed={selected===action} onClick= {() => onClick(item)}  />
            {(allowMenuHide && (visible ?? primary)) &&
                <Chip {...{
                    className: 'hideTab',
                    onClick:() => {
                        const newMenuItems= menu.menuItems.map( (mi) => mi===item ? {...mi, visible:false} : mi);
                        const current= selected===action;
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
    const results= getResultCounts();
    if (prev && shallowequal(prev,results)) return prev;
    return results;
}

const showBgMonAction = { type:'COMMAND',
    action: 'background.bgMonitorShow',
    label: 'Background Monitor',
    desc: 'Watch and retrieve background tasks for packaging and catalogs'
};


function BgMonitorButton ({sx,size}) {
    const {jobs={}} = useStoreConnector(() => getBackgroundInfo());

    const monitoredJobs = Object.values(jobs).filter( (info) => info?.jobInfo?.monitored );
    const isWorking = monitoredJobs.some( (info) => isActive(info) );

    const buttonSize= size==='lg' ? 'sm' : size==='md' ? 'lg' : 'md';

    return (
        <MenuItemButton {...{ sx, size:buttonSize, menuItem:showBgMonAction, isWorking,
            icon:size!=='lg' ? <PendingActionsOutlinedIcon/> : undefined,
            badgeCount: monitoredJobs.length }}/>);
}
