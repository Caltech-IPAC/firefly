/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {HistogramPlotly} from './HistogramPlotly.jsx';
import {HistogramHighcharts} from './HistogramHighcharts.jsx';
import {isPlotly} from '../ChartUtil.js';
import React from 'react';

export function  Histogram(props) {
    const HistogramInstance =  isPlotly() ? HistogramPlotly : HistogramHighcharts;
    return <HistogramInstance {...props}/>;
}

