/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * @global
 * @public
 * @typedef {Object} PlotGroup
 *
 * @prop {string} plotGroupId
 * @prop {boolean} overlayColorLock
 * @prop {boolean} enableSelecting
 * @prop {boolean} allSelected
 * @prop {boolean} rotateNorthLockSticky
 * @prop {boolean} flipYSticky
 * @prop {RangeValues} defaultRangeValues
 */

/**
 * @param plotGroupId
 * @param {boolean} overlayColorLock
 * @return {PlotGroup}
 */
export const makePlotGroup= (plotGroupId,overlayColorLock) =>
    ({ plotGroupId, overlayColorLock, enableSelecting :false, allSelected :false,
        rotateNorthLockSticky:false, flipYSticky : false, defaultRangeValues: undefined,
    });

/**
 * get the plot view with the id
 * @param {VisRoot|Array.<PlotGroup>} visRootOrGroupAry - root of the visualization object in store
 * @param {string} plotGroupId
 * @return {object} the plot group object
 */
export function getPlotGroupById (visRootOrGroupAry,plotGroupId) {
    const pgAry= visRootOrGroupAry.plotGroupAry ? visRootOrGroupAry.plotGroupAry : visRootOrGroupAry;
    return pgAry.find((pg) => pg.plotGroupId===plotGroupId);
}
