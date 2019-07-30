/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {FixedPtControl} from './CatalogUI.jsx';
import {primePlot} from '../visualize/PlotViewUtil';
import {PlotAttribute} from '../visualize/WebPlot.js';


export const getUIComponent = (drawLayer,pv) => <ActiveTargetUI drawLayer={drawLayer} pv={pv}/>;

function ActiveTargetUI({pv}) {

    const plot= primePlot(pv);
    return (
        <div>
            <FixedPtControl wp={plot.attributes[PlotAttribute.FIXED_TARGET]} pv={pv}/>
        </div>
    );
}

ActiveTargetUI.propTypes= {
    drawLayer     : PropTypes.object.isRequired,
    pv            : PropTypes.object.isRequired
};

