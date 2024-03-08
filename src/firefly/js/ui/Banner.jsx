/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import MenuRoundedIcon from '@mui/icons-material/MenuRounded.js';
import {Box, IconButton, Sheet, Stack, Typography} from '@mui/joy';
import React, {memo, useContext} from 'react';
import PropTypes from 'prop-types';
import {dispatchShowDialog, SIDE_BAR_ID} from '../core/ComponentCntlr.js';
import {AppPropertiesCtx} from './AppPropertiesCtx.jsx';
import {getVersionInfoStr, showFullVersionInfoDialog} from 'firefly/ui/VersionInfo.jsx';
import './Banner.css';
import {menuTabsBorderSx} from 'firefly/ui/Menu';

const getVersionTipStr= (appTitle) => `${appTitle?appTitle+' ':''}Version:\n${getVersionInfoStr(true,true)}`;

// todo - evaluate if we want soft/primary as our banner color
const variant='soft';
// const variant='solid';
// const variant='outlined';
const color='primary';

export const Banner = memo( ({menu, enableVersionDialog= false, appTitle:titleProp, showTitleOnBanner=false}) => {
    const ctx = useContext(AppPropertiesCtx);
    const {appIcon, bannerMiddleStyle, bannerLeftStyle} = ctx;
    const appTitle=  titleProp || ctx.appTitle;
    return (
        <Sheet {...{
            className:'banner__main', variant, color, sx: (theme) => ({
                width:1, position:'relative',
                ...menuTabsBorderSx(theme)
            })
        }}>
            <Stack {...{direction:'row', height:40, alignItems:'center', px:1, py: .5,
                spacing: 0.5}}>
                <AppConfigButton/>

                <Box sx={{flexGrow:0, cursor: enableVersionDialog ? 'pointer' : 'inherit'}}
                     style={bannerLeftStyle}>
                    {appIcon ?
                        <img src={appIcon} alt={`${appTitle} icon`}
                             onClick={() => enableVersionDialog && showFullVersionInfoDialog(appTitle) }
                             title={enableVersionDialog ? getVersionTipStr(appTitle) : ''}/> :
                        <Box {...{width: 75}}/>}
                </Box>

                <Stack {...{flexGrow:1, direction:'row',  style:bannerMiddleStyle }}>
                    {showTitleOnBanner && makeBannerTitle(appTitle)}
                    <Stack {...{ flexGrow: 0, width: 1}}>
                        {menu}
                    </Stack>
                </Stack>
            </Stack>
        </Sheet>
    );
});

Banner.propTypes= {
    menu: PropTypes.object,
    readout: PropTypes.object,
    appIcon: PropTypes.string,
    visPreview: PropTypes.object,
    appTitle: PropTypes.oneOfType([
        PropTypes.string,
        PropTypes.element
    ]),
    additionalTitleStyle: PropTypes.object,
    showUserInfo: PropTypes.bool,
    enableVersionDialog: PropTypes.bool,
    showTitleOnBanner: PropTypes.bool
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