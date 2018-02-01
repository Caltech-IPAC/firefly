/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import sizeMe from 'react-sizeme';

//Some of the docs from size me are included here
const config = {
    monitorWidth: true,
    monitorHeight: true,
    monitorPosition: false,

    // The maximum frequency, in milliseconds, at which size changes should be
    // recalculated when changes in your Component's rendered size are being
    // detected. This should not be set to lower than 16.
    refreshRate: 100,

    // The mode in which refreshing should occur.  Valid values are "debounce"
    // and "throttle".  "throttle" will eagerly measure your component and then
    // wait for the refreshRate to pass before doing a new measurement on size
    // changes. "debounce" will wait for a minimum of the refreshRate before
    // it does a measurement check on your component.  "debounce" can be useful
    // in cases where your component is animated into the DOM.
    // NOTE: When using "debounce" mode you may want to consider disabling the
    // placeholder as this adds an extra delay in the rendering time of your
    // component.
    refreshMode: 'debounce',   // 'throttle' or 'debounce'

    // By default we render a "placeholder" component initially so we can try
    // and "prefetch" the expected size for your component.  This is to avoid
    // any unnecessary deep tree renders.  If you feel this is not an issue
    // for your component case and you would like to get an eager render of
    // your component then disable the placeholder using this config option.
    // NOTE: You can set this globally. See the docs on first render.
    noPlaceholder: false
};

export const wrapResizer = sizeMe(config);
