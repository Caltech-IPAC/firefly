/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Band} from './Band.js';
import BrowserCache from '../util/BrowserCache.js';
import {RangeValues} from './RangeValues.js';
import PlotState from './PlotState.js';

/**
 * @param {string} cacheKey
 * @param (PlotState} state
 */
var putCacheColorPref= function(cacheKey, state) {
    if (!cacheKey) return;
    var colorTableId= state.getColorTableId();
    var pref= {colorTableId};

    state.getBands().forEach(
        (band)=> {
            pref[band.key]= state.getRangeValues(band).toJSON();
        });

    BrowserCache.put(cacheKey,pref);
};



/**
 * @param {string} cacheKey
 * @return {object} the color preference
 */
var getCacheColorPref= function(cacheKey) {
    if (!cacheKey) return null;
    var pref= BrowserCache.get(cacheKey);
    if (!pref) return null;

    Band.enums.forEach( (band) => {
        if (pref[band.key]) {
            pref[band.key]= RangeValues.parse(pref[band.key]);
        }
    });
    return pref;
};


export const PlotPref= {putCacheColorPref, getCacheColorPref};

