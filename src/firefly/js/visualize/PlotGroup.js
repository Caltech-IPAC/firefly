/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import ImagePlotCntlr from './ImagePlotCntlr.js';



//============ EXPORTS ===========
//============ EXPORTS ===========

export default {makePlotGroup};

//============ EXPORTS ===========
//============ EXPORTS ===========

//======================================== Exported Functions =============================
//======================================== Exported Functions =============================
//======================================== Exported Functions =============================

/**
 *
 * @param plotGroupId
 * @param {boolean} groupLocked
 * @return {{plotGroupId: *, lockRelated: boolean, enableSelecting: boolean, allSelected: boolean}}
 */
function makePlotGroup(plotGroupId,groupLocked) {
    return {
        plotGroupId,
        lockRelated  : groupLocked,
        enableSelecting :false,    //todo
        allSelected :false    //todo
    };
}

/**
 * get the plot view with the id
 * @param {string} plotGroupId
 * @param visRoot - root of the visualization object in store
 * @return {object} the plot group object
 */
export function getPlotGroupById(visRoot,plotGroupId) {
    if (!plotGroupId) return null;
    return visRoot.plotGroupAry.find( (pg) => pg.plotGroupId===plotGroupId);
}




//======================================== Private ======================================
//======================================== Private ======================================
//======================================== Private ======================================
//======================================== Private ======================================
