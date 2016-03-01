/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import ReactDOM from 'react-dom';
import Histogram from '../visualize/Histogram.jsx';

export class ReactJavaInterface {
    constructor() {}

    createHistogram(params, divName) {
        var parsedParams = JSON.parse(params);
        // when going through GWT, all parameters are converted to a string - to make it possible to pass JSON
        // non-string parameters should be converted back
        var data = (typeof parsedParams.data === 'string') ? JSON.parse(parsedParams.data) : parsedParams.data;
        var height = (parsedParams.height) ? Number(parsedParams.height) : parsedParams.height;
        var el;

        el = (<div><Histogram data={data}
                             desc={parsedParams.descr}
                             binColor={parsedParams.binColor}
                             height={height}
                             logs={parsedParams.logs}
                             reversed={parsedParams.reversed}/></div>);

        if (el) {
            ReactDOM.render(el, document.getElementById(divName));
        }
    }

}