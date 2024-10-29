/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import MenuRoundedIcon from '@mui/icons-material/MenuRounded.js';
import {Box, IconButton, Sheet, Stack, Typography} from '@mui/joy';
import React, {isValidElement, memo, useContext} from 'react';
import {bool, element, node, object, shape, string} from 'prop-types';
import {dispatchShowDialog, SIDE_BAR_ID} from '../core/ComponentCntlr.js';
import {AppPropertiesCtx} from './AppPropertiesCtx.jsx';
import {getVersionInfoStr, showFullVersionInfoDialog} from 'firefly/ui/VersionInfo.jsx';
import {menuTabsBorderSx} from 'firefly/ui/Menu';

const getVersionTipStr= (appTitle) => `${appTitle?appTitle+' ':''}Version:\n${getVersionInfoStr(true,true)}`;

// todo - evaluate if we want soft/primary as our banner color
const variant='soft';
const color='primary';

export const Banner = memo( ({menu, enableVersionDialog= false, appIcon:pAppIdon, appTitle:pAppTitle, title, slotProps, sx, ...props}) => {
    const ctx = useContext(AppPropertiesCtx);
    const {appIcon=pAppIdon, appTitle=pAppTitle} = ctx;
    const appIconEl = appIcon && isValidElement(appIcon) ? React.cloneElement( appIcon, {
                                        onClick: () => enableVersionDialog && showFullVersionInfoDialog(appTitle),
                                        title: enableVersionDialog ? getVersionTipStr(appTitle) : ''
                                    }) : undefined;
    return (
        <Sheet {...{variant, color,
            sx: (theme) => ({
                width:1, position:'relative',
                ...menuTabsBorderSx(theme),
                ...sx
            }),
            ...props
        }}>
            {Boolean(menu) && <Stack {...{direction:'row', minHeight:40, alignItems:'center', px:1,spacing: 0.5}}>
                <AppConfigButton/>

                <Stack direction='row' {...slotProps?.icon} sx={{flexGrow:0, cursor: enableVersionDialog ? 'pointer' : 'inherit', ...slotProps?.icon?.sx}} >
                    {appIconEl || <Box width={75}/>}
                </Stack>

                <Stack {...{alignSelf:'flex-end', flexGrow:1, direction:'row', ...slotProps?.tabs}}>
                    {title}
                    <Stack {...{ flexGrow: 0, width: 1}}>
                        {menu}
                    </Stack>
                </Stack>
            </Stack> }
        </Sheet>
    );
});

Banner.propTypes= {
    menu: object,
    appIcon: element,
    appTitle: string,
    enableVersionDialog: bool,
    title: node,
    sx: object,
    slotProps: shape({
        icon: object,
        tabs: object
    })
};

function AppConfigButton({sx}) {
    return (
        <Stack sx={sx}>
            <IconButton variant='outlined'  onClick={() => dispatchShowDialog(SIDE_BAR_ID)}>
                <MenuRoundedIcon/>
            </IconButton>
        </Stack>
    );
}

 export function makeBannerTitle(title,subTitle) {
     if (title && subTitle) {
         return (
             <Stack pr={1}>
                 <Typography {...{level:'h3', sx:{lineHeight:1, whiteSpace:'nowrap'} }}>{title}</Typography>
                 <Typography {...{level:'title-sm', sx:{lineHeight:1, textAlign:'end'} }} >
                     {subTitle}
                 </Typography>
             </Stack>
         );
     }
     return <Typography {...{color, variant, level:'h4' }}>{title}</Typography>;
}