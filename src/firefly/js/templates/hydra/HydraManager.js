/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take, fork} from 'redux-saga/effects';

import {
    SHOW_DROPDOWN, SET_LAYOUT_MODE, getLayouInfo,
    dispatchSetLayoutInfo, dropDownManager, getResultCounts
} from '../../core/LayoutCntlr.js';
import {removeChartsInGroup} from '../../charts/ChartsCntlr.js';
import {TABLE_SEARCH, TBL_RESULTS_ADDED, TABLE_REMOVE
} from '../../tables/TablesCntlr.js';
import {visRoot, dispatchDeletePlotView} from '../../visualize/ImagePlotCntlr.js';
import {removeTablesFromGroup, getAllTableGroupIds, smartMerge} from '../../tables/TableUtil.js';
import {getSearchByName, getSearchInfo, getSelectedMenuItem} from '../../core/AppDataCntlr.js';
import ImagePlotCntlr from '../../visualize/ImagePlotCntlr.js';
import {CHART_ADD} from '../../charts/ChartsCntlr.js';
import {REPLACE_VIEWER_ITEMS} from '../../visualize/MultiViewCntlr.js';
import {deleteAllDrawLayers} from '../../visualize/DrawLayerCntlr';

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
            // case TBL_RESULTS_ADDED:
            // case TABLE_LOADED:
            // case TBL_RESULTS_ACTIVE:
            //     const tbl = getTblById(action.payload.tbl_id);
            //     if (tbl?.request?.META_INFO?.[MetaConst.UPLOAD_TABLE]) break; //to not change layoutInfo if it is an upload table
            //     //intentional fallthrough
            // case CHART_ADD:
            case TABLE_SEARCH:
                newLayoutInfo = handleNewSearch(newLayoutInfo, action);
                break;

            default:
        }

        if (newLayoutInfo !== layoutInfo) {
            dispatchSetLayoutInfo(newLayoutInfo);
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

    if (!getResultCounts()?.haveResults) {
        // if no results, setup layout with init values;
        layoutInfo = { ...layoutInfo, showTables:true, showXyPlots:true, showImages:true, currentSearch: getSearchInfo()?.activeSearch};
    }
    //reset image object for every new search
    layoutInfo = { ...layoutInfo, images: undefined};

    // below logic applies only to searches that uses Hydra's SearchInfo
    // ignore if selectedMenuItem is not one of SearchInfo
    const selectMenuItem = getSelectedMenuItem();
    const isSearchCmdSelected = !!getSearchByName(selectMenuItem);
    if (! isSearchCmdSelected) return layoutInfo;

    if (currentSearch && currentSearch !== selectMenuItem ) {
        cleanup();
        // remove all charts
        removeChartsInGroup();
        layoutInfo = { ...layoutInfo, showTables:true, showXyPlots:true, showImages:true};
    }

    layoutInfo = { ...layoutInfo, currentSearch:selectMenuItem};
    return layoutInfo;
}


/**
 * remove the previous results
 */
export function cleanup() {

    // remove all tables
    const groups = getAllTableGroupIds() || [];
    groups.forEach((k) => removeTablesFromGroup(k));
    // remove all plots
    visRoot().plotViewAry.forEach( (pv) => dispatchDeletePlotView({plotId:pv.plotId}));
    // remove all drawing layers
    deleteAllDrawLayers();

}
