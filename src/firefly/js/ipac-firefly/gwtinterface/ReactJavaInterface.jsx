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
        if (parsedParams.data) {
            el = <div><Histogram data={parsedParams.data}/></div>;
        } else {
            if (parsedParams.source) {
                el = <div><Histogram source={parsedParams.source} desc={parsedParams.descr}/></div>;
            }
        }
        if (el) {
            React.render(el, document.getElementById(divName));
        }
        /* jshint ignore:end */
    }

}