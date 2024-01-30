
import {DialogTitle, Divider, Drawer, IconButton, ModalClose, Stack} from '@mui/joy';
import React from 'react';
import {modifyURLToFull} from '../util/WebUtil.js';
import {SideBarMenu} from './Menu.jsx';
import MenuRoundedIcon from '@mui/icons-material/MenuRounded';



export function AppConfigOptions({appTitle, appIcon, sx}) {
    const [open, setOpen] = React.useState(false);

    return (
        <Stack sx={{alignSelf:'center'}} >
            <IconButton variant='outlined'  onClick={() => setOpen(true)}>
                <MenuRoundedIcon/>
            </IconButton>
            <Drawer open={open} onClose={() => setOpen(false)} sx={{ '--Drawer-horizontalSize': '15vw' }} >
                <ModalClose />
                <DialogTitle>
                    <Stack direction='row' spacing={1} alignItems='center'>
                        <img src={appIcon?.startsWith('data') ? appIcon : modifyURLToFull(appIcon)}/>
                        {appTitle}
                    </Stack>
                </DialogTitle>
                <Divider orientation='horizontal' sx={{my:1}}/>
                <SideBarMenu setOpen={setOpen}/>
            </Drawer>
        </Stack>
    );
}