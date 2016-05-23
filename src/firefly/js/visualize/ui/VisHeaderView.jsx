/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {ThumbnailView} from './ThumbnailView.jsx';
import {MagnifiedView} from './MagnifiedView.jsx';
import {getActivePlotView, getPlotViewById} from '../PlotViewUtil.js';
import {MouseReadout} from './MouseReadout.jsx';
import InputFieldLabel from '../../ui/InputFieldLabel.jsx';


/**
 *
 * @param visRoot visualization store root
 * @param {object} currMouseState  the current state of the mouse
 * @return {XML}
 */
export function VisHeaderView({visRoot,currMouseState, readout}) {

    var rS= {
        width: 700,
        minWidth:660,
        height: 32,
        minHeight:32,
        display: 'inline-block',
        position: 'relative',
        verticalAlign: 'top',
        cursor:'pointer',
        whiteSpace : 'nowrap',
        overflow : 'hidden'
    };

    var pv= getActivePlotView(visRoot);
    var mousePv= getPlotViewById(visRoot,currMouseState.plotId);

    return (
        <div style={{display:'inline-block', float:'right', whiteSpace:'nowrap'}}>
            <div style={rS}>
                <div style={{position:'absolute', color:'white'}}>
                    <MouseReadout  readout={readout}/>
                </div>
            </div>

            <ThumbnailView plotView={pv}/>
            <MagnifiedView plotView={mousePv} size={70} mouseState={currMouseState} />

        </div>
    );
}

VisHeaderView.propTypes= {
    visRoot : React.PropTypes.object.isRequired,
    currMouseState :React.PropTypes.object,
    readout:  React.PropTypes.object.isRequire
};