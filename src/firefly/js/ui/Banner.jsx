/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {memo} from 'react';
import PropTypes from 'prop-types';
import {SimpleComponent} from './SimpleComponent.jsx';
import {getUserInfo} from '../core/AppDataCntlr.js';
import {logout} from '../rpc/CoreServices.js';
import {getVersionInfoStr, showFullVersionInfoDialog} from '../ui/DropDownContainer.jsx';
import './Banner.css';

const getVersionTipStr= (appTitle) => `${appTitle?appTitle+' ':''}Version:\n${getVersionInfoStr(true)}`;

export const Banner = memo( ({menu, readout, appIcon, visPreview, appTitle, additionalTitleStyle = {},
                                 showUserInfo=false, enableVersionDialog= false }) => {
    return (
        <div className='banner__main'>
            <div className='banner__left'>
                {appIcon ?
                    <img src={appIcon}
                         onClick={() => enableVersionDialog && showFullVersionInfoDialog(appTitle) }
                         title={enableVersionDialog ? getVersionTipStr(appTitle) : ''}/> :
                    <div style={{width: 75}}/>}
            </div>
            <div className='banner__middle'>
                <div className='banner__middle--readout'>
                    <div className='banner__middle--title' style={{marginLeft:'10px', ...additionalTitleStyle}}>{appTitle}</div>
                    {readout}
                </div>
                <div className='banner__middle--menu'>
                    {menu}
                </div>
            </div>
            <div className='banner__right'>
                {visPreview}
            </div>
            {showUserInfo && <UserInfo/>}
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



class UserInfo extends SimpleComponent {

    getNextState(np) {
        return getUserInfo() || {};
    }

    render() {
        const {loginName='Guest', firstName, lastName, login_url, logout_url} = this.state || {};
        const isGuest = loginName === 'Guest';
        const onLogin = () => login_url && (window.location = login_url);
        const onLogout = () => {
            if (logout_url) {
                window.location = logout_url;
            }
            logout();
        };

        const displayName = firstName || lastName ? `${firstName} ${lastName}` : loginName;

        return (
            <div className='banner__user-info'>
                <span>{displayName}</span>
                {!isGuest && <div className='banner__user-info--links' onClick={onLogout}>Logout</div>}
                {isGuest && <div className='banner__user-info--links' onClick={onLogin}>Login</div>}
            </div>
        );
    }
}
