/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {primePlot} from '../visualize/PlotViewUtil';
import {PlotAttribute} from '../visualize/PlotAttribute.js';
import {FixedPtControl} from './FixedPtControl.jsx';


export const getUIComponent = (drawLayer,pv) => <ActiveTargetUI drawLayer={drawLayer} pv={pv}/>;

function ActiveTargetUI({pv}) {
    const plot= primePlot(pv);
    return <FixedPtControl wp={plot.attributes[PlotAttribute.FIXED_TARGET]} pv={pv}/> ;
}

ActiveTargetUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

