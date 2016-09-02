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
        <div id='app-icon' align='left' style={{width: '100%', height: '100%', float: 'left'}}>
            <img src={src} className='gwt-Image' style={{width: '75px', height: '75px'}}></img>
        </div>
    );
}

function visPreview(visPreviewComp) {
    if (visPreviewComp) {
        return (
            <div id='visPreview' style={{height: '100%', top: 0, right : 0, position: 'absolute'}}>
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

        return (
            <div className='banner__wrap'>
                {appIcon(this.props.appIcon)}
                <div style={{position: 'absolute', left: 75, right: 3, minWidth: 820}}>
                    <div id='readout' style={{height: '45px', width: '100%', position : 'relative'}}>
                        {appTitle(this.props.appTitle)}
                        {visPreview(this.props.visPreview)}
                    </div>
                    {menu(this.props.menu)}
                </div>
            </div>
        );
    }
});
