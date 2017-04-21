/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import './Banner.css';

export const Banner = React.createClass({

    propTypes: {
        menu: React.PropTypes.object,
        readout: React.PropTypes.object,
        appIcon: React.PropTypes.string,
        visPreview: React.PropTypes.object,
        appTitle: React.PropTypes.string,
        additionalTitleStyle: React.PropTypes.object
    },

    render() {
        const {menu, readout, appIcon, visPreview, appTitle, additionalTitleStyle = {marginLeft:'10px'}} = this.props;

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
            </div>
        );
    }
});
