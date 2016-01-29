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
export function VisHeaderView({visRoot,currMouseState}) {

    var rS= {
        border: '1px solid white',
        width: 500,
        height: 32,
        display: 'inline-block',
        position: 'relative',
        verticalAlign: 'top'
    };

    var pv= getActivePlotView(visRoot);
    var mousePv= getPlotViewById(visRoot,currMouseState.plotId);
    return (
        <div>
            <div style={rS}>
                <div style={{position:'absolute', color:'white'}}>
                    <MouseReadout plotView={mousePv} mouseState={currMouseState} />
                </div>
            </div>

            <ThumbnailView plotView={pv}/>
            <MagnifiedView plotView={mousePv} size={70} mouseState={currMouseState} />

        </div>
    );
}

VisHeaderView.propTypes= {
    visRoot : React.PropTypes.object.isRequired,
    currMouseState :React.PropTypes.object
};
