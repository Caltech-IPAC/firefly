/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {useStoreConnector} from './SimpleComponent.jsx';
import {getUserInfo} from '../core/AppDataCntlr.js';
import {logout} from '../rpc/CoreServices.js';
import {getVersionInfoStr, showFullVersionInfoDialog} from 'firefly/ui/VersionInfo.jsx';
import './Banner.css';

const getVersionTipStr= (appTitle) => `${appTitle?appTitle+' ':''}Version:\n${getVersionInfoStr(true,true)}`;

export const Banner = memo( ({menu, readout, appIcon, visPreview, appTitle, additionalTitleStyle = {},
                                 showUserInfo=false, enableVersionDialog= false, showTitleOnBanner=false,
                             bannerMiddleStyle={}, bannerLeftStyle={}}) => {
    return (
        <div className='banner__main'>
            <div className='banner__left' style={{ cursor: enableVersionDialog ? 'pointer' : 'inherit', ...bannerLeftStyle}}>
                {appIcon ?
                    <img src={appIcon}
                         onClick={() => enableVersionDialog && showFullVersionInfoDialog(appTitle) }
                         title={enableVersionDialog ? getVersionTipStr(appTitle) : ''}/> :
                    <div style={{width: 75}}/>}
            </div>
            <div className='banner__middle' style={bannerMiddleStyle}>

                {showTitleOnBanner && <div className='banner__middle--title' style={{marginLeft:'10px', ...additionalTitleStyle}}>{appTitle}</div>}
                <div className='banner__middle--menu'>
                    {menu}
                </div>
            </div>
            <div className='banner__right'>
                {showUserInfo && <UserInfo/>}
            </div>
        </div>
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
        <div className='banner__user-info'>
            <span className='banner__user-info--name' title={displayName}>{displayName}</span>
            {!isGuest && <div className='banner__user-info--links' onClick={onLogout}>Logout</div>}
            {isGuest && <div className='banner__user-info--links' onClick={onLogin}>Login</div>}
        </div>
    );
});
