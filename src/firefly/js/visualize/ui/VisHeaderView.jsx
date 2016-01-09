/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {ThumbnailView} from './ThumbnailView.jsx';
import {getActivePlotView} from '../PlotViewUtil.js';


export function VisHeaderView({visRoot}) {


    var rS= {
        border: '1px solid white',
        width: 500,
        height: 32,
        display: 'inline-block',
        position: 'relative',
        verticalAlign: 'top'

    };

    var oS= {
        border: '1px solid white',
        width: 70,
        height: 70,
        display: 'inline-block',
        position: 'relative'
    };

    return (
        <div>
            <div style={rS}><div style={{position:'absolute', color:'white'}}>mouse readout here</div></div>
            <ThumbnailView plotView={getActivePlotView(visRoot)}/>
            <div style={oS}><div style={{position:'absolute', color:'white'}}>magnifier here</div></div>
        </div>
    );
}

VisHeaderView.propTypes= {
    visRoot : React.PropTypes.object.isRequired
};
