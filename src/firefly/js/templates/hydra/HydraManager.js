/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take, fork} from 'redux-saga/effects';

import {SHOW_DROPDOWN, SET_LAYOUT_MODE, getLayouInfo,
        dispatchUpdateLayoutInfo, dropDownManager} from '../../core/LayoutCntlr.js';
import {removeChartsInGroup} from '../../charts/ChartsCntlr.js';
import {TABLE_SEARCH, TBL_RESULTS_ADDED, TABLE_REMOVE} from '../../tables/TablesCntlr.js';
import {visRoot, dispatchDeletePlotView} from '../../visualize/ImagePlotCntlr.js';
import {removeTablesFromGroup, getAllTableGroupIds, smartMerge} from '../../tables/TableUtil.js';
import {getSearchInfo} from '../../core/AppDataCntlr.js';
import ImagePlotCntlr from '../../visualize/ImagePlotCntlr.js';
import {deleteAllDrawLayers} from '../../visualize/PlotViewUtil.js';
import {CHART_ADD} from '../../charts/ChartsCntlr.js';
import {REPLACE_VIEWER_ITEMS} from '../../visualize/MultiViewCntlr.js';


/**
 * Configurable part of this template
 * @typedef {object} Config
 * @prop {Search[]} searches  list of searches.
 * @prop {Catalog}  catalogs  additional catalogs(non-public).
 */

/**
 * Search information.
 * @typedef {object} Catalog
 * @prop {string} url  url of the additional catalog
 */

/**
 * This event manager is custom made for HydraViewer.
 */
export function* hydraManager() {

    yield fork(dropDownManager);        // start the dropdown manager
    while (true) {
        const action = yield take([
            SHOW_DROPDOWN, SET_LAYOUT_MODE, CHART_ADD,
            TABLE_SEARCH, TBL_RESULTS_ADDED, TABLE_REMOVE,
            ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_IMAGE_START, ImagePlotCntlr.DELETE_PLOT_VIEW, REPLACE_VIEWER_ITEMS
        ]);

        /**
         * This is the current state of the layout store.  Action handlers should return newLayoutInfo if state changes
         * If state has changed, it will be dispatched into the flux.
         * @type {LayoutInfo}
         * @prop {string}   layoutInfo.currentSearch  the current selected search
         *
         */
        var layoutInfo = getLayouInfo();
        var newLayoutInfo = layoutInfo;
        const {hasImages:showImages, hasXyPlots:showXyPlots, hasTables:showTables} = layoutInfo;
        newLayoutInfo = smartMerge(layoutInfo, {showImages, showXyPlots, showTables});

        switch (action.type) {
            case TABLE_SEARCH:
                newLayoutInfo = handleNewSearch(newLayoutInfo, action);
                break;

            default:
        }

        if (newLayoutInfo !== layoutInfo) {
            dispatchUpdateLayoutInfo(newLayoutInfo);
        }
    }
}

/**
 * handle logic when a new search is initiated.
 * should go and clean up all previous state.
 * @param {object} layoutInfo layoutInfo
 * @param {string} action
 * @returns {Object} the new layoutInfo
 */
function handleNewSearch(layoutInfo, action) {

    const {currentSearch} = layoutInfo;
    const {activeSearch} = getSearchInfo();
    var {showTables=true, showXyPlots=true, showImages=true, images={}} = layoutInfo;

    if (currentSearch && currentSearch !== activeSearch) {
        cleanup();
        // remove all charts
        removeChartsInGroup();
        showTables=showXyPlots=showImages=true;
        images= {};
    }

    layoutInfo = Object.assign({}, layoutInfo, {showTables, showXyPlots, showImages, currentSearch:activeSearch, images});
    return layoutInfo;
}


export function cleanup() {

    // remove all tables
    const groups = getAllTableGroupIds() || [];
    groups.forEach((k) => removeTablesFromGroup(k));
    // remove all plots
    visRoot().plotViewAry.forEach( (pv) => dispatchDeletePlotView({plotId:pv.plotId}));
    // remove all drawing layers
    deleteAllDrawLayers();

}
