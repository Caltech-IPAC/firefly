/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import './Banner.css';

import FFTOOLS_ICO from 'html/images/fftools-logo-offset-small-75x75.png';

function menu(menuComp) {
    if (menuComp) {
        return (
            <div id='menu-bar' style={{height: '30px', display: 'inline-block', whiteSpace: 'nowrap', position:'relative'}}>
                {menuComp}
            </div>
        );
    } else {
        return '';
    }
}



function appIcon(icoSrc) {
    const src = icoSrc || FFTOOLS_ICO;
    return (
            <img src={src} className='gwt-Image' style={{width: '75px', height: '75px'}}/>
    );
}

function visPreview(visPreviewComp) {
    if (visPreviewComp) {
        return (
            <div id='visPreview'>
                {visPreviewComp}
            </div>
        );
    }
}

function appTitle(title) {
    if (title) {
        return (
            <div id='appTitle' className='banner__Title'>
                {title}
            </div>
        );
    }
}

export const Banner = React.createClass({

    propTypes: {
        menu        : React.PropTypes.object,
        readout     : React.PropTypes.object,
        appIcon     : React.PropTypes.string,
        visPreview  : React.PropTypes.object,
        appTitle    : React.PropTypes.string
    },

    render() {
        const {menu, readout, appIcon, visPreview, appTitle} = this.props;
        const iconSrc = appIcon || FFTOOLS_ICO;

        return (
            <div className='banner__main'>
                <div className='banner__left'>
                    <img src={iconSrc} className='gwt-Image'/>
                </div>
                <div className='banner__middle'>
                    <div className='banner__middle--readout'>
                        <div className='banner__middle--title'>{appTitle}</div>
                        {readout}
                    </div>
                    <div className='banner__middle--menu'>
                        {menu}
                    </div>
                </div>
                <div  className='banner__right'>
                    {visPreview}
                </div>
            </div>
        );
    }
});
