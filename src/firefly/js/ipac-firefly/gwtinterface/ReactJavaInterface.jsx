/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

'use strict';

import React from 'react/addons';
import Histogram from 'ipac-firefly/visualize/Histogram.jsx';

export class ReactJavaInterface {
    constructor() {}

    createHistogram(params, divName) {
        var parsedParams = JSON.parse(params);
        /* jshint ignore:start */
        var el;
        {
            el = <div><Histogram data={parsedParams.data}
                                 source={parsedParams.source}
                                 desc={parsedParams.descr}
                                 binColor={parsedParams.binColor}
                                 height={parsedParams.height}
                                 logs={parsedParams.logs}
                                 reversed={parsedParams.reversed}/></div>;
        }
        if (el) {
            React.render(el, document.getElementById(divName));
        }
        /* jshint ignore:end */
    }

}