/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {HistogramPlotly} from './HistogramPlotly.jsx';
import React from 'react';

export function  Histogram(props) {
    const HistogramInstance =  HistogramPlotly;
    return <HistogramInstance {...props}/>;
}

