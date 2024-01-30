
import {DialogTitle, Divider, Drawer, ModalClose, Stack, Typography} from '@mui/joy';
import {bool, number, oneOfType, string} from 'prop-types';
import React, {useState} from 'react';
import {dispatchHideDialog, isDialogVisible, SIDE_BAR_ID} from '../core/ComponentCntlr.js';
import {modifyURLToFull} from '../util/WebUtil.js';
import {useColorMode} from './FireflyRoot.jsx';
import {ListBoxInputFieldView} from './ListBoxInputField.jsx';
import {SideBarMenu} from './Menu.jsx';
import {AccordionPanelView} from './panel/AccordionPanel.jsx';
import {useStoreConnector} from './SimpleComponent.jsx';
import {VersionInfo} from './VersionInfo.jsx';



export function AppConfigDrawer({appTitle, appIcon, drawerWidth= '22rem', allowMenuHide= true, useSideBarMenu=true, children}) {
    const visible= useStoreConnector(() => isDialogVisible(SIDE_BAR_ID));
    const closeSideBar= () => dispatchHideDialog(SIDE_BAR_ID);
    return (
        <Drawer open={visible} onClose={() => closeSideBar()} sx={{ '--Drawer-horizontalSize': drawerWidth }} >
            <ModalClose/>
            <DialogTitle>
                <Stack direction='row' spacing={1} alignItems='center'>
                    <img style={{maxWidth:'50px'}} src={appIcon?.startsWith('data') ? appIcon : modifyURLToFull(appIcon)}/>
                    <div>{appTitle}</div>
                </Stack>
            </DialogTitle>
            {useSideBarMenu && <Divider orientation='horizontal' sx={{mb:1}}/>}
            <Stack justifyContent='space-between' height={1}>
                {useSideBarMenu && <SideBarMenu closeSideBar={closeSideBar} allowMenuHide={allowMenuHide}/>}
                <Stack>
                    {React.Children.map(children,(c) => React.cloneElement(c, {closeSideBar}))}
                    <Divider orientation='horizontal' sx={{my:1}}/>
                    <SideBarColorModeUI closeSideBar={closeSideBar}/>
                    <Divider orientation='horizontal' sx={{mt:1}}/>
                    <Stack width={1} alignItems='center'>
                        <VersionInfo asButton={false} includeBuiltOnDate={true} includeBuildType={false}
                                     onClick={closeSideBar}/>
                    </Stack>
                </Stack>
            </Stack>
        </Drawer>
    );
}

AppConfigDrawer.propTypes = {
    appTitle: string,
    appIcon: string,
    drawerWidth: oneOfType([string,number]),
    allowMenuHide: bool,
    useSideBarMenu: bool
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