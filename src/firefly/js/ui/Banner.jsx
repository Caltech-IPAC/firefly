/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import {SimpleComponent} from './SimpleComponent.jsx';
import {getUserInfo} from '../core/AppDataCntlr.js';
import {logout} from '../rpc/CoreServices.js';
import './Banner.css';


export class Banner extends PureComponent {

    constructor(props) {
        super(props);
    }

    render() {
        const {menu, readout, appIcon, visPreview, appTitle, additionalTitleStyle = {marginLeft:'10px'}, showUserInfo=false} = this.props;

        return (
            <div className='banner__main'>
                <div className='banner__left'>
                    {appIcon ? <img src={appIcon}/> : <div style={{width: 75}}/>}
                </div>
                <div className='banner__middle'>
                    <div className='banner__middle--readout'>
                        <div className='banner__middle--title' style={additionalTitleStyle}>{appTitle}</div>
                        {readout}
                    </div>
                    <div className='banner__middle--menu'>
                        {menu}
                    </div>
                </div>
                <div className='banner__right'>
                    {visPreview}
                </div>
                {showUserInfo && <UserInfo />}
            </div>
        );
    }
}

Banner.propTypes= {
    menu: PropTypes.object,
    readout: PropTypes.object,
    appIcon: PropTypes.string,
    visPreview: PropTypes.object,
    appTitle: PropTypes.string,
    additionalTitleStyle: PropTypes.object,
    showUserInfo: PropTypes.bool,
};



export class UserInfo extends SimpleComponent {

    getNextState(np) {
        return getUserInfo() || {};
    }

    render() {
        const {loginName='Guest', firstName, lastName, login_url} = this.state || {};
        const isGuest = loginName === 'Guest';
        const onLogin = () => login_url && (window.location = login_url);

        return (
            <div className='banner__user-info'>
                <span>{loginName}</span>
                {!isGuest && <div className='banner__user-info--links' onClick={logout}>Logout</div>}
                {isGuest && <div className='banner__user-info--links' onClick={onLogin}>Login</div>}
            </div>
        );
    }
}
