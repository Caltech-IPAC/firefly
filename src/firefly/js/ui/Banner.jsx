/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import './Banner.css';



function menu(menuComp) {
    if (menuComp) {
        return (
            <div id='menu-bar' style={{height: '30px', width: '100%', whiteSpace: 'nowrap'}}>
                {menuComp}
            </div>
        );
    } else {
        return '';
    }
}

function appIcon(icoSrc) {
    const src = icoSrc || 'http://localhost:8080/fftools/images/fftools-logo-offset-small-75x75.png';
    return (
        <div id='app-icon' align='left' style={{width: '100%', height: '100%', float: 'left'}}>
            <img src={src} className='gwt-Image' style={{width: '75px', height: '75px'}}></img>
        </div>
    );
}

function altAppIcon(icoSrc) {
    if (icoSrc) {
        return (
            <div id='altAppIcon' style={{width: '100%', height: '100%'}}>
                <img src={icoSrc} className='gwt-Image' style={{width: '75px', height: '75px'}}></img>
            </div>
        );
    } else {
        return '';
    }
}

function visPreview(visPreviewComp) {
    if (visPreviewComp) {
        return (
            <div id='visPreview' style={{height: '100%', float: 'right'}}>
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

const Banner = React.createClass({

    propTypes: {
        menu        : React.PropTypes.object,
        appIcon     : React.PropTypes.string,
        altAppIcon  : React.PropTypes.string,
        visPreview  : React.PropTypes.object,
        appTitle    : React.PropTypes.string
    },

    render() {

        return (
            <div id='container' style={{width: '100%', height: '75px', background: 'url(images/ipac_bar.jpg)'}}>
                <div style={{height: '75px', width: '75px', float: 'left'}}>

                    {appIcon(this.props.appIcon)}

                    <div style={{position: 'absolute', left: '75px', right: '158px', minWidth: '820px'}}>
                        <div id='readout' style={{height: '45px', width: '100%'}}>

                            {appTitle(this.props.appTitle)}
                            {visPreview(this.props.visPreview)}

                        </div>

                        {menu(this.props.menu)}

                    </div>
                    <div style={{height: '75px', width: '148px', right: '10px', position: 'absolute'}}>
                        <div>
                            <div style={{width: '100%', height: '100%', padding: '0px', margin: '0px'}}>

                                {altAppIcon(this.props.altAppIcon)}

                            </div>
                            <div aria-hidden='true' style={{width: '100%', height: '100%', padding: '0px', margin: '0px', display: 'none'}}>

                                {visPreview(this.props.visPreview)}

                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
});

export default Banner;
