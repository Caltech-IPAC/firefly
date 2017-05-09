/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {PureComponent} from 'react';
import PropTypes from 'prop-types';
import './Banner.css';


export class Banner extends PureComponent {

    constructor(props) {
        super(props);
    }

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
}

Banner.propTypes= {
    menu: PropTypes.object,
    readout: PropTypes.object,
    appIcon: PropTypes.string,
    visPreview: PropTypes.object,
    appTitle: PropTypes.string,
    additionalTitleStyle: PropTypes.object
};


