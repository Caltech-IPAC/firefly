/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get} from 'lodash';
import {getAppOptions} from '../../core/AppDataCntlr.js';
import {HistogramPlotly} from './HistogramPlotly.jsx';
import {HistogramHighcharts} from './HistogramHighcharts.jsx';
import React from 'react';

export function  Histogram(props) {
    if (get(getAppOptions(), 'charts.chartEngine') !== 'plotly') {
        return <HistogramHighcharts {...props}/>;
    } else {
        return <HistogramPlotly {...props} />;
    }
}

