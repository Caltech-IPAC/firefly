/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import {primePlot} from '../visualize/PlotViewUtil.js';
import {getBootstrapRegistry} from '../core/BootstrapRegistry';
import {RangeValues} from '../visualize/RangeValues.js';



// NOTE
// NOTE 
//----------------------------------------------------------------
// Anything that is exported here becomes part of the lowlevel API
// It should have a jsdoc
//----------------------------------------------------------------
// NOTE 
// NOTE

/**
 * @public
 *
 */
export {RangeValues} from '../visualize/RangeValues.js';
export {WPConst, WebPlotRequest, findInvalidWPRKeys, confirmPlotRequest} from '../visualize/WebPlotRequest.js';
export {RequestType} from '../visualize/RequestType';
export {ExpandType, visRoot} from '../visualize/ImagePlotCntlr.js';

export {CysConverter} from '../visualize/CsysConverter.js';
export {CCUtil} from '../visualize/CsysConverter.js';
export {startCoverageWatcher} from '../visualize/saga/CoverageWatcher.js';
export {getSelectedRegion} from '../drawingLayers/RegionPlot.js';

export {extensionAdd, extensionRemove} from '../core/ExternalAccessUtils.js';
export {makeWorldPt, makeScreenPt, makeImagePt, parsePt, parseAnyPt, parseWorldPt } from '../visualize/Point.js';
export {CoordinateSys} from '../visualize/CoordSys.js';

export {IMAGE, NewPlotMode} from '../visualize/MultiViewCntlr';


/**
 * Get plot object with the given plot id, when plotId is not included, active plot is returned.
 * @param {string} [plotId] the plotId, optional
 * @returns {WebPlot}
 * @function getPrimePlot
 * @public
 * @memberof firefly.util.image
 *
 */
export function getPrimePlot(plotId) {
    return primePlot(visRoot(), plotId);
}



var isInit= false;
/**
 * Set a defaults object on for a draw layer type.
 * The following draw layers are supported: 'ACTIVE_TARGET_TYPE', 'CATALOG_TYPE'
 * @param {string} drawLayerTypeId
 * @param {DrawingDef} defaults
 * @public
 * @function setDrawLayerDefaults
 * @memberof firefly.util.image
 *
 * @example
 * firefly.util.image.setDrawLayerDefaults('ACTIVE_TARGET_TYPE', {symbol:'x', color:'pink', size:15});
 * or
 * firefly util.image.setDrawLayerDefaults('CATALOG_TYPE', {symbol:'cross', color:'red'});
 *
 * @see DrawingDef
 * @see DrawSymbol
 */
export function setDrawLayerDefaults(drawLayerTypeId, defaults) {
    getBootstrapRegistry().setDrawLayerDefaults(drawLayerTypeId, defaults);
}

/**
 * @summary  initialize the auto readout. Must be call once at the begging to get the popup readout running.
 * @param {object} ReadoutComponent - either a PopupMouseReadoutMinimal or PopupMouseReadoutFull
 * @param {object} props - a list of the properties
 * @public
 * @deprecated
 * @function initAutoReadout
 * @memberof firefly.util.image
 */
export function initAutoReadout() {}


/**
 *
 * @param stretchType the type of stretch may be 'Percent', 'Absolute', 'Sigma'
 * @param lowerValue lower value of stretch, based on stretchType
 * @param upperValue upper value of stretch, based on stretchType
 * @param algorithm the stretch algorithm to use, may be 'Linear', 'Log', 'LogLog', 'Equal', 'Squared', 'Sqrt'
 * @public
 * @function serializeSimpleRangeValues
 * @memberof firefly.util.image
 */
export function serializeSimpleRangeValues(stretchType,lowerValue,upperValue,algorithm) {
    const rv= RangeValues.makeSimple(stretchType,lowerValue,upperValue,algorithm);
    return rv.toJSON();
}


