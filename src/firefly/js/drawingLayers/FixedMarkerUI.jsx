/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {dispatchRecenter} from '../visualize/ImagePlotCntlr';


export const getUIComponent = (drawLayer,pv) => <FixedMarkerUI drawLayer={drawLayer} pv={pv}/>;

function FixedMarkerUI({drawLayer,pv}) {


    const center= () => {
        const {worldPt:wp} = drawLayer;
        if (!wp) return;
        dispatchRecenter({plotId:pv.plotId, centerPt:wp});
    };
    return (

        <div>
            <button style={{padding: '0 3px'}} type='button' onClick={center}>Center Here</button>
        </div>
    );
}

FixedMarkerUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

