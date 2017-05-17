/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';

import {SimpleComponent} from './SimpleComponent.jsx';
import {getUserInfo} from '../core/AppDataCntlr.js';

export class UserInfo extends SimpleComponent {

    getNextState(np) {
        return getUserInfo();
    }

    render() {
        const {loginName, firstName, lastName, institute} = this.props;
        const isGuest = loginName === 'Guest';
        const logout = () => {
            
        }
        return (
            <div>
                <div className='banner__userInfo'>
                    {loginName}
                </div>
                {!isGuest} <div className='banner__logout' onClick={logout}>Logout</div>
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
    showUserInfo: PropTypes.boolean
};


