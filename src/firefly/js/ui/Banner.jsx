/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import MenuRoundedIcon from '@mui/icons-material/MenuRounded.js';
import {Box, Chip, Divider, IconButton, Sheet, Stack, Typography} from '@mui/joy';
import React, {memo, useContext} from 'react';
import PropTypes from 'prop-types';
import {dispatchShowDialog, SIDE_BAR_ID} from '../core/ComponentCntlr.js';
import {AppPropertiesCtx} from './AppPropertiesCtx.jsx';
import {useStoreConnector} from './SimpleComponent.jsx';
import {getUserInfo} from '../core/AppDataCntlr.js';
import {logout} from '../rpc/CoreServices.js';
import {getVersionInfoStr, showFullVersionInfoDialog} from 'firefly/ui/VersionInfo.jsx';
import './Banner.css';

const getVersionTipStr= (appTitle) => `${appTitle?appTitle+' ':''}Version:\n${getVersionInfoStr(true,true)}`;

// todo - evaluate if we want soft/primary as our banner color
const variant='soft';
// const variant='solid';
// const variant='outlined';
const color='primary';

export const Banner = memo( ({menu, enableVersionDialog= false, appTitle:titleProp, showTitleOnBanner=false}) => {
    const ctx = useContext(AppPropertiesCtx);
    const {showUserInfo, appIcon, bannerMiddleStyle, bannerLeftStyle} = ctx;
    const appTitle=  titleProp || ctx.appTitle;
    return (
        <Sheet {...{
            className:'banner__main', variant, color, sx:{width:1, position:'relative' } }}>
            <Stack {...{direction:'row', height:40, alignItems:'flex-end', pl:1}}>
                <AppConfigButton/>
                <Box sx={{ alignSelf:'center', flexGrow:0, cursor: enableVersionDialog ? 'pointer' : 'inherit'}}
                     style={bannerLeftStyle}>
                    {appIcon ?
                        <img src={appIcon}
                             onClick={() => enableVersionDialog && showFullVersionInfoDialog(appTitle) }
                             title={enableVersionDialog ? getVersionTipStr(appTitle) : ''}/> :
                        <Box {...{width: 75}}/>}
                </Box>
                <Stack {...{flexGrow:1, direction:'row',alignItems:'flex-end',  style:bannerMiddleStyle   }}>
                    {showTitleOnBanner && makeBannerTitle(appTitle)}
                    <Stack {...{ flexGrow: 0, width: 1, pr:1}}>
                        {menu}
                    </Stack>
                </Stack>
                <div className='banner__right'>
                    {showUserInfo && <UserInfo/>}
                </div>
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
};

function AppConfigButton({sx}) {
    return (
        <Stack sx={{alignSelf:'center', ...sx}} >
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

const UserInfo= memo(() => {
    const userInfo = useStoreConnector(() => getUserInfo() ?? {});

    const {loginName='Guest', firstName='', lastName='', login_url, logout_url} = userInfo;
    const isGuest = loginName === 'Guest';
    const onLogin = () => login_url && (window.location = login_url);
    const onLogout = () => {
        if (logout_url) window.location = logout_url;
        logout();
    };

    const fn= (firstName && firstName.trim().toLowerCase()!=='null') ? firstName : '';
    const ln= (lastName && lastName.trim().toLowerCase()!=='null') ? lastName : '';

    const displayName = (fn || ln) ? `${fn} ${ln}` : loginName;

    return (
        <Stack pr={2} pb={1/4} direction='row'>
            <Divider orientation='vertical' sx={{mx:1}}/>
            <Stack spacing={1/4}>
                <Typography level='body-xs' title={displayName}>{displayName}</Typography>
                {!isGuest && <Chip onClick={onLogout}>Logout</Chip>}
                {isGuest && <Chip onClick={onLogin}>Login</Chip>}
            </Stack>
        </Stack>
    );
});
