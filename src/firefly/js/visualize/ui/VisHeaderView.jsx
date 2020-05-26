/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {ThumbnailView} from './ThumbnailView.jsx';
import {MagnifiedView} from './MagnifiedView.jsx';
import {getActivePlotView, getPlotViewById} from '../PlotViewUtil.js';
import {MouseReadout} from './MouseReadout.jsx';
import {HiPSMouseReadout} from './HiPSMouseReadout';
import {STANDARD_READOUT, HIPS_STANDARD_READOUT} from '../MouseReadoutCntlr.js';


const readoutUI = {
    [STANDARD_READOUT] : MouseReadout,
    [HIPS_STANDARD_READOUT] : HiPSMouseReadout
};

const rS= {
    width: 700,
    minWidth:660,
    height: 34,
    minHeight:34,
    display: 'inline-block',
    position: 'relative',
    verticalAlign: 'top',
    whiteSpace : 'nowrap',
    overflow : 'hidden'
};

/**
 *
 * @param props
 * @param props.readout
 * @param props.readoutData
 * @param props.showHealpixPixel
 */
export function VisHeaderView({readout, readoutData, showHealpixPixel=false, style={}}) {

    const ActiveReadoutUI= readoutData && readoutUI[readoutData.readoutType];
    if (!ActiveReadoutUI) return (<div style={rS}/>);

    return (
        <div style={{display:'inline-block', float:'right', whiteSpace:'nowrap'}}>
            <div style={{...rS, ...style}}>
                <div style={{position:'absolute', color:'white'}}>
                    {ActiveReadoutUI && <ActiveReadoutUI readout={readout} readoutData={readoutData} showHealpixPixel={showHealpixPixel}/>}
                </div>
            </div>
        </div>
    );
}

VisHeaderView.propTypes= {
    readout:  PropTypes.object.isRequired,
    readoutData:  PropTypes.object,
    showHealpixPixel : PropTypes.bool,
    style: PropTypes.number
};


/**
 * @param {Object} p React props object.
 * @param {Object} p.visRoot
 * @param {Object} p.currMouseState
 * @returns {HTML}
 * @constructor
 */
export function VisPreview({visRoot,currMouseState}) {
    const pv= getActivePlotView(visRoot);
    const mousePv= getPlotViewById(visRoot,currMouseState.plotId);
    return (
        <div>
            <ThumbnailView plotView={pv}/>
            <MagnifiedView plotView={mousePv} size={70} mouseState={currMouseState} />
        </div>
    );
}

VisPreview.propTypes= {
    visRoot : PropTypes.object.isRequired,
    currMouseState :PropTypes.object,
};
