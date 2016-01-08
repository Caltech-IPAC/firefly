/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import sCompare from 'react-addons-shallow-compare';
import {visRoot} from '../ImagePlotCntlr.js';
import {flux} from '../../Firefly.js';


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
            <div style={oS}><div style={{position:'absolute', color:'white'}}>thumbnail here</div></div>
            <div style={oS}><div style={{position:'absolute', color:'white'}}>magnifier here</div></div>
        </div>
    );
}
