/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {filter, isEmpty, get} from 'lodash';

import {LO_VIEW, SHOW_DROPDOWN, SET_LAYOUT_MODE, getLayouInfo, dispatchUpdateLayoutInfo, dropDownHandler} from '../../core/LayoutCntlr.js';
import {findGroupByTblId, getTblIdsByGroup, smartMerge} from '../../tables/TableUtil.js';
import {TBL_RESULTS_ADDED, TABLE_LOADED, TABLE_REMOVE, TBL_RESULTS_ACTIVE} from '../../tables/TablesCntlr.js';
import {CHART_ADD, CHART_REMOVE} from '../../charts/ChartsCntlr.js';

import ImagePlotCntlr from '../../visualize/ImagePlotCntlr.js';
import {isMetaDataTable, isCatalogTable} from '../../metaConvert/converterUtils.js';
import {META_VIEWER_ID} from '../../visualize/ui/TriViewImageSection.jsx';
import {REPLACE_VIEWER_ITEMS, DEFAULT_FITS_VIEWER_ID, getViewerItemIds, getMultiViewRoot} from '../../visualize/MultiViewCntlr.js';
import {visRoot} from '../../visualize/ImagePlotCntlr.js';

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

    while (true) {
        const action = yield take([
            ImagePlotCntlr.PLOT_IMAGE_START, ImagePlotCntlr.PLOT_IMAGE,
            ImagePlotCntlr.DELETE_PLOT_VIEW, REPLACE_VIEWER_ITEMS,
            TABLE_REMOVE, TABLE_LOADED, TBL_RESULTS_ADDED, TBL_RESULTS_ACTIVE,
            CHART_ADD, CHART_REMOVE,
            SHOW_DROPDOWN, SET_LAYOUT_MODE
        ]);

        /**
         * This is the current state of the layout store.  Action handlers should return newLayoutInfo if state changes
         * If state has changed, it will be dispacthed into the flux.
         * @type {LayoutInfo} layoutInfo
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
            case ImagePlotCntlr.PLOT_IMAGE_START:
            case ImagePlotCntlr.PLOT_IMAGE :
            case REPLACE_VIEWER_ITEMS:
                newLayoutInfo = handleNewImage(newLayoutInfo, action);
                break;
            case ImagePlotCntlr.DELETE_PLOT_VIEW:
                newLayoutInfo = handlePlotDelete(newLayoutInfo, action);
            case TABLE_LOADED:
            case TBL_RESULTS_ADDED:
                newLayoutInfo = handleNewTable(newLayoutInfo, action);
                break;
            case TBL_RESULTS_ACTIVE:
                newLayoutInfo = handleActiveTableChange(newLayoutInfo, action);
                break;
        }

        newLayoutInfo = onAnyAction(newLayoutInfo, action, views);
        newLayoutInfo = dropDownHandler(newLayoutInfo, action);     // handles dropdown behaviors

        if (newLayoutInfo !== layoutInfo) {
            dispatchUpdateLayoutInfo(newLayoutInfo);

        }
    }
}

function onAnyAction(layoutInfo, action, views) {
    var {mode, hasXyPlots, hasTables, showImages, showXyPlots, showTables, autoExpand} = layoutInfo;
    var {expanded=LO_VIEW.none, standard=views} = mode || {};

    console.log('showTables:' + showTables + '  hasTables:' + hasTables);
    // enforce views settings
    showImages =  showImages && views.has(LO_VIEW.images);
    showXyPlots = hasXyPlots && views.has(LO_VIEW.xyPlots);
    showTables =  hasTables && views.has(LO_VIEW.tables) && showTables;

    const count = filter([showTables, showXyPlots, showImages]).length;
    const closeable = count > 1;

    switch (action.type) {
        case TABLE_REMOVE:
        case TBL_RESULTS_ADDED:
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
    

function handleNewTable(layoutInfo, action) {
    const {tbl_id} = action.payload;
    var {images, showImages} = layoutInfo;
    var {coverageLockedOn, showFits, showMeta, showCoverage, selectedTab, metaDataTableId} = images || {};

    const isMeta = isMetaDataTable(tbl_id);
    if (isMeta || isCatalogTable(tbl_id)) {
        if (!showFits) {
            // only show coverage if there are not images or coverage is showing
            showFits= shouldShowFits();
            coverageLockedOn= !showFits||coverageLockedOn;
            selectedTab = 'coverage';
            showCoverage = coverageLockedOn;
            showImages = true;
        }
    }
    if (isMeta) {
        showImages = true;
        selectedTab = 'meta';
        showMeta = true;
        metaDataTableId = tbl_id;
    }
    return smartMerge(layoutInfo, {showTables: true, showImages, images: {coverageLockedOn, showFits, showMeta, showCoverage, selectedTab, metaDataTableId}});
}


function handleActiveTableChange (layoutInfo, action) {
    const {tbl_id} = action.payload;
    var {images, showImages} = layoutInfo;
    var {coverageLockedOn, showCoverage, showMeta, metaDataTableId} = images;

    const showFits= shouldShowFits();
    showImages= showFits||coverageLockedOn;

    if (!tbl_id) {
        images = {showMeta: false, showCoverage: false, showFits, metaDataTableId: null};
        return smartMerge(layoutInfo, {images, showImages:showFits, coverageLockedOn:false});
    }

    const tblGroup= findGroupByTblId(tbl_id);
    if (!tblGroup) return smartMerge(layoutInfo, {showImages});

    const tblList= getTblIdsByGroup(tblGroup);
    if (isEmpty(tblList)) return smartMerge(layoutInfo, {showImages});

    // check for catalog or meta images
    const anyHasCatalog= hasCatalogTable(tblList);
    const anyHasMeta= hasMetaTable(tblList);

    if (coverageLockedOn) {
        coverageLockedOn= anyHasCatalog || anyHasMeta;
    }

    if (anyHasCatalog) {
        showCoverage = coverageLockedOn;
        showImages = true;
    } else {
        showCoverage = false;
    }

    if (anyHasMeta) {
        metaDataTableId = isMetaDataTable(tbl_id) ? tbl_id : findFirstMetaTable(tblList);
        showMeta = true;
        showImages = true;
    } else {
        metaDataTableId = null;
        showMeta = false;
    }
    return smartMerge(layoutInfo, {images: {coverageLockedOn, showCoverage, showMeta, metaDataTableId}, showImages, coverageLockedOn});
}

const hasCatalogTable= (tblList) => tblList.some( (id) => isCatalogTable(id) );
const hasMetaTable= (tblList) => tblList.some( (id) => isMetaDataTable(id) );
const findFirstMetaTable= (tblList) => tblList.find( (id) => isMetaDataTable(id) );
const shouldShowFits= () => !isEmpty(getViewerItemIds(getMultiViewRoot(), DEFAULT_FITS_VIEWER_ID));


function handlePlotDelete(layoutInfo, action) {
    var {images, showImages} = layoutInfo;
    var {coverageLockedOn} = images || {};
    const showFits = shouldShowFits();
    if (!get(visRoot(), 'plotViewAry.length', 0)) {
        coverageLockedOn = false;
        showImages = false;
    }
    return smartMerge(layoutInfo, {showImages, images:{coverageLockedOn, showFits}});
}

function handleNewImage(layoutInfo, action) {
    var {images} = layoutInfo;
    var {selectedTab, showMeta, showFits} = images;
    var coverageLockedOn = false;

    const {viewerId, plotGroupId} = action.payload || {};
    if (viewerId === META_VIEWER_ID) {
        // select image meta tab when new images are added.
        selectedTab = 'meta';
        showMeta = true;
    } else if (viewerId === DEFAULT_FITS_VIEWER_ID) {
        // select image tab when new images are added.
        selectedTab = 'fits';
        showFits = true;
    } else {
        coverageLockedOn = true;
    }
    return smartMerge(layoutInfo, {showImages: true, images: {coverageLockedOn, selectedTab, showMeta, showFits}});
}
