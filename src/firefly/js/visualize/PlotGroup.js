/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

//============ EXPORTS ===========
//============ EXPORTS ===========

export default {makePlotGroup};

//============ EXPORTS ===========
//============ EXPORTS ===========

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
 * @param {VisRoot} visRoot - root of the visualization object in storet
 * @param {string} plotGroupId
 * @return {object} the plot group object
 */
export function getPlotGroupById(visRoot,plotGroupId) {
    if (!plotGroupId) return null;
    return visRoot.plotGroupAry.find( (pg) => pg.plotGroupId===plotGroupId);
}
