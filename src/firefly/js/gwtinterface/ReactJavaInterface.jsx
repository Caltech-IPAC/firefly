/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react/addons';
import Histogram from '../visualize/Histogram.jsx';

export class ReactJavaInterface {
    constructor() {}

    createHistogram(params, divName) {
        var parsedParams = JSON.parse(params);
        // when going through GWT, all parameters are converted to a string - to make it possible to pass JSON
        // non-string parameters should be converted back
        var data = (typeof parsedParams.data === 'string') ? JSON.parse(parsedParams.data) : parsedParams.data;
        var height = (parsedParams.height) ? Number(parsedParams.height) : parsedParams.height;
        /* jshint ignore:start */
        var el;
        //{
        el = <div><Histogram data={data}
                             source={parsedParams.source}
                             desc={parsedParams.descr}
                             binColor={parsedParams.binColor}
                             height={height}
                             logs={parsedParams.logs}
                             reversed={parsedParams.reversed}/></div>;
        //}
        if (el) {
            React.render(el, document.getElementById(divName));
        }
        /* jshint ignore:end */
    }

}