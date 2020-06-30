/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

/**
 * @param plotGroupId
 * @param {boolean} overlayColorLock
 * @return {{plotGroupId: *, overlayColorLock: boolean, enableSelecting: boolean, allSelected: boolean}}
 */
export const makePlotGroup= (plotGroupId,overlayColorLock) =>
    ({ plotGroupId, overlayColorLock, enableSelecting :false, allSelected :false });

/**
 * get the plot view with the id
 * @param {VisRoot} visRoot - root of the visualization object in storet
 * @param {string} plotGroupId
 * @return {object} the plot group object
 */
export const getPlotGroupById= (visRoot,plotGroupId) => visRoot.plotGroupAry.find((pg) => pg.plotGroupId===plotGroupId);
