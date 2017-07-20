/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take, fork} from 'redux-saga/effects';
import {filter} from 'lodash';

import {LO_VIEW, SHOW_DROPDOWN, SET_LAYOUT_MODE, getLayouInfo, dispatchUpdateLayoutInfo, dropDownManager} from '../../core/LayoutCntlr.js';
import {smartMerge} from '../../tables/TableUtil.js';
import {TBL_RESULTS_ADDED, TABLE_LOADED, TABLE_REMOVE, TBL_RESULTS_ACTIVE, TBL_RESULTS_REMOVE} from '../../tables/TablesCntlr.js';
import {CHART_ADD, CHART_REMOVE} from '../../charts/ChartsCntlr.js';

import ImagePlotCntlr from '../../visualize/ImagePlotCntlr.js';
import {REPLACE_VIEWER_ITEMS} from '../../visualize/MultiViewCntlr.js';
import {resetChartSelectOptions} from '../../ui/ChartSelectDropdown.jsx';

/**
 * this manager manages what main components get display on the screen.
 * These main components are image plots, charts, tables, dropdown panel, etc.
 * This manager implements the default firefly viewer's requirements.
 * Because it may differs between applications, it is okay to have a custom layout manager if needed.
 * @param {object} p
 * @param {string} [p.title] title to display
 * @param {string} [p.views] defaults to tri-view if not given.
 */
export function* layoutManager({title, views='tables | images | xyPlots'}) {
    views = LO_VIEW.get(views) || LO_VIEW.none;

    yield fork(dropDownManager);        // start the dropdown manager
    while (true) {
        const action = yield take([
            ImagePlotCntlr.PLOT_IMAGE_START, ImagePlotCntlr.PLOT_IMAGE,
            ImagePlotCntlr.DELETE_PLOT_VIEW, REPLACE_VIEWER_ITEMS,
            TABLE_REMOVE, TABLE_LOADED, TBL_RESULTS_ADDED, TBL_RESULTS_ACTIVE, TBL_RESULTS_REMOVE,
            CHART_ADD, CHART_REMOVE,
            SHOW_DROPDOWN, SET_LAYOUT_MODE
        ]);

        /**
         * This is the current state of the layout store.  Action handlers should return newLayoutInfo if state changes
         * If state has changed, it will be dispatched into the flux.
         * @type {LayoutInfo}
         * @prop {boolean}  layoutInfo.showTables  show tables panel
         * @prop {boolean}  layoutInfo.showXyPlots show charts panel
         * @prop {boolean}  layoutInfo.showImages  show images panel
         * @prop {string}   layoutInfo.searchDesc  optional string describing search criteria used to generate this result.
         * @prop {boolean}  layoutInfo.autoExpand  this is true when manager think it should be expanded, ie. single view
         * @prop {Object}   layoutInfo.images      images specific states
         * @prop {string}   layoutInfo.images.metaTableId  tbl_id of the image meta table
         * @prop {string}   layoutInfo.images.selectedTab  selected tab of the images tabpanel
         * @prop {string}   layoutInfo.images.showCoverage  show images coverage tab
         * @prop {string}   layoutInfo.images.showFits  show images fits data tab
         * @prop {string}   layoutInfo.images.showMeta  show images image metea tab
         * @prop {string}   layoutInfo.images.coverageLockedOn
         */
        var layoutInfo = getLayouInfo();
        var newLayoutInfo = layoutInfo;

        switch (action.type) {
            case TBL_RESULTS_ACTIVE:
                newLayoutInfo = handleActiveTableChange(newLayoutInfo, action);
                break;
        }

        newLayoutInfo = onAnyAction(newLayoutInfo, action, views);
        // newLayoutInfo = dropDownHandler(newLayoutInfo, action);     // replaced with manager up above

        if (newLayoutInfo !== layoutInfo) {
            dispatchUpdateLayoutInfo(newLayoutInfo);

        }
    }
}

function onAnyAction(layoutInfo, action, views) {
    var {mode, hasXyPlots, hasTables, showImages, showXyPlots, showTables, autoExpand} = layoutInfo;
    var {expanded=LO_VIEW.none, standard=views} = mode || {};

    // enforce views settings
    showImages =  showImages && views.has(LO_VIEW.images);
    showXyPlots = hasXyPlots && views.has(LO_VIEW.xyPlots);
    showTables =  hasTables && views.has(LO_VIEW.tables);

    const count = filter([showTables, showXyPlots, showImages]).length;
    const closeable = count > 1;

    switch (action.type) {
        case TABLE_REMOVE:
        case TBL_RESULTS_ADDED:
        case TBL_RESULTS_REMOVE:
        case REPLACE_VIEWER_ITEMS:
        case ImagePlotCntlr.PLOT_IMAGE_START :
        case ImagePlotCntlr.DELETE_PLOT_VIEW:
        case CHART_ADD:
        case CHART_REMOVE:
        {
            // handle autoExpand feature
            if (autoExpand) {
                if (count > 1) {
                    autoExpand = false;
                    expanded = LO_VIEW.none;
                }
            } else if (expanded === LO_VIEW.none && count === 1) {
                // set mode into expanded view when there is only 1 component visible.
                autoExpand = true;
                autoExpand = true;
                expanded =  showImages ? LO_VIEW.images :
                    showXyPlots ? LO_VIEW.xyPlots :
                        showTables ? LO_VIEW.tables :  expanded;
            }
            mode = {expanded, standard, closeable};
            break;
        }
    }
    return smartMerge(layoutInfo, {showTables, showImages, showXyPlots, autoExpand, mode: {expanded, standard, closeable}});
}


function handleActiveTableChange (layoutInfo, action) {
    resetChartSelectOptions();
    return layoutInfo;
}
