/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {flux} from '../Firefly.js';
import ImagePlotCntlr from './ImagePlotCntlr.js';



//============ EXPORTS ===========
//============ EXPORTS ===========

export default {makePlotGroup, getPlotGroupById};

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
 * @return {object} the plot group object
 */
function getPlotGroupById(plotGroupId) {
    if (!plotGroupId) return null;
    return flux.getState()[ImagePlotCntlr.IMAGE_PLOT_KEY].plotGroupAry.find( (pg) => pg.plotGroupId===plotGroupId);
}




//======================================== Private ======================================
//======================================== Private ======================================
//======================================== Private ======================================
//======================================== Private ======================================
