/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

"use strict";

import React from 'react/addons';
import Histogram from "ipac-firefly/visualize/Histogram.jsx";

export class ReactJavaInterface {
    constructor() {}

    createHistogram(data, divName) {
        var el = <div><Histogram data={data}/></div>;
        React.render(el, document.getElementById(divName));
    }

}