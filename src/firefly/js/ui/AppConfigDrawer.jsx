
import {DialogTitle, Divider, Drawer, IconButton, ModalClose, Stack, Typography} from '@mui/joy';
import {object, bool, number, oneOfType, string} from 'prop-types';
import React, {useContext, useState} from 'react';
import {dispatchHideDialog, isDialogVisible, SIDE_BAR_ID} from '../core/ComponentCntlr.js';
import {modifyURLToFull} from '../util/WebUtil.js';
import {AppPropertiesCtx} from './AppPropertiesCtx.jsx';
import {useColorMode} from './FireflyRoot.jsx';
import {ListBoxInputFieldView} from './ListBoxInputField.jsx';
import {SideBarMenu} from './Menu.jsx';
import {AccordionPanelView} from './panel/AccordionPanel.jsx';
import {useStoreConnector} from './SimpleComponent.jsx';
import {VersionInfo} from './VersionInfo.jsx';



export function AppConfigDrawer({containerElement, drawerWidth= '20rem', allowMenuHide= true, useSideBarMenu=true, children}) {

    const {appTitle, appIcon} = useContext(AppPropertiesCtx);
    const visible= useStoreConnector(() => isDialogVisible(SIDE_BAR_ID));
    const closeSideBar= () => dispatchHideDialog(SIDE_BAR_ID);
    const drawerTitleSx = {m: '0.5rem', height: '3rem'};

    return (
        <Drawer container={containerElement} open={visible} onClose={() => closeSideBar()} sx={{ '--Drawer-horizontalSize': drawerWidth }} >
            <ModalClose sx={{transform: 'translateY(-50%)', top: `calc(${drawerTitleSx.height} / 2 + ${drawerTitleSx.m})`}}/>
            <DialogTitle level='h4' sx={(theme) => ({m: drawerTitleSx.m, color: theme.vars.palette.text.tertiary, fontWeight: theme.vars.fontWeight.md})}>
                <Stack direction='row' spacing={.5} alignItems='center'>
                    <IconButton onClick={() => closeSideBar()}>
                        <img style={{height: drawerTitleSx.height}} src={appIcon?.startsWith('data') ? appIcon : modifyURLToFull(appIcon)} />
                    </IconButton>
                    <span>{appTitle}</span>
                </Stack>
            </DialogTitle>
            <Divider orientation='horizontal'/>

            {/* Drawer content --- */}
            <Stack justifyContent='space-between' height={1}>
                {useSideBarMenu && <SideBarMenu closeSideBar={closeSideBar} allowMenuHide={allowMenuHide}/>}
                <Stack divider={<Divider orientation='horizontal'/>}>
                    {React.Children.map(children,(c) => React.cloneElement(c, {closeSideBar}))}
                    <SideBarColorModeUI closeSideBar={closeSideBar}/>
                    <Stack p='0.75rem' //0.75 rem is the left padding used by accordions
                           alignItems='center'>
                        <VersionInfo asButton={false} includeBuiltOnDate={true} includeBuildType={false}
                                     onClick={closeSideBar}/>
                    </Stack>
                </Stack>
            </Stack>
        </Drawer>
    );
}

AppConfigDrawer.propTypes = {
    drawerWidth: oneOfType([string,number]),
    allowMenuHide: bool,
    useSideBarMenu: bool,
    containerElement: object
};


function SideBarColorModeUI({closeSideBar}) {
    const { mode, setMode } = useColorMode();
    const [ panelOpen, setPanelOpen ] = useState(true);

    return (
        <AccordionPanelView
            header={<Typography level='h4'>Appearance</Typography>}
            expanded={panelOpen}
            onChange={(v) => setPanelOpen(v)} >
            <ListBoxInputFieldView {...{
                onChange: (ev, newValue) => {
                    setMode(newValue);
                    closeSideBar();
                },
                slotProps:{label: {sx: {width: '7rem'}}},
                value:mode,
                sx:{mt:1},
                label:'Theme',
                tooltip:'set dark mode, light mode, or lock mode to the system',
                options:[
                    {value: 'dark', label:'Dark'},
                    {value: 'light', label:'Light'},
                    {value: 'system', label:'System'},
                ]
            }}/>
        </AccordionPanelView>
    );


}