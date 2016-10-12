/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {get, filter, omitBy, isNil, isEmpty} from 'lodash';

import {LO_VIEW, LO_MODE, SHOW_DROPDOWN, SET_LAYOUT_MODE, getLayouInfo, dispatchUpdateLayoutInfo} from '../../core/LayoutCntlr.js';
import {clone} from '../../util/WebUtil.js';
import {findGroupByTblId, getTblIdsByGroup,getActiveTableId} from '../../tables/TableUtil.js';
import {TBL_RESULTS_ADDED, TABLE_LOADED, TABLE_REMOVE, TBL_RESULTS_ACTIVE} from '../../tables/TablesCntlr.js';
import {CHART_ADD, CHART_REMOVE} from '../../charts/ChartsCntlr.js';
import ImagePlotCntlr from '../../visualize/ImagePlotCntlr.js';
import {isMetaDataTable, isCatalogTable} from '../../metaConvert/converterUtils.js';
import {META_VIEWER_ID} from '../../visualize/ui/TriViewImageSection.jsx';
import {REPLACE_VIEWER_ITEMS, DEFAULT_FITS_VIEWER_ID, getViewerItemIds, getMultiViewRoot} from '../../visualize/MultiViewCntlr.js';

/**
 * this manager manages what main components get display on the screen.
 * These main components are image plots, charts, tables, dropdown panel, etc.
 * This manager implements the default firefly viewer's requirements.
 * Because it may differs between applications, it is okay to have a custom layout manager if needed.
 * @param {object} p
 * @param [p.title] title to display. p
 * @param p.views defaults to tri-view if not given.
 */
export function* layoutManager({title, views='tables | images | xyPlots'}) {
    views = LO_VIEW.get(views) || LO_VIEW.none;
    var coverageLockedOn= false;

    while (true) {
        const action = yield take([
            ImagePlotCntlr.PLOT_IMAGE_START, ImagePlotCntlr.PLOT_IMAGE,
            ImagePlotCntlr.DELETE_PLOT_VIEW, REPLACE_VIEWER_ITEMS,
            TBL_RESULTS_ADDED, TABLE_REMOVE, TABLE_LOADED, CHART_ADD, CHART_REMOVE,
            SHOW_DROPDOWN, SET_LAYOUT_MODE,
            TBL_RESULTS_ACTIVE
        ]);

        var {hasImages, hasTables, hasXyPlots, mode, dropDown={}, ...others} = getLayouInfo();
        // eslint-disable-next-line
        var {images, tables, xyPlots} = others;     //images, tables, and xyPlots are additional states relevant only to them.
        var {expanded, standard} = mode || {};
        var searchDesc = '';
        var closeable = true, ignore = false;
        standard = standard || views;

        var showImages = hasImages && views.has(LO_VIEW.images);
        const showXyPlots = hasXyPlots && views.has(LO_VIEW.xyPlots);
        const showTables = hasTables && views.has(LO_VIEW.tables);

        const ids = getViewerItemIds(getMultiViewRoot(), DEFAULT_FITS_VIEWER_ID);
        images = clone(images, {showFits: ids && ids.length > 0});

        // special cases which could affect the layout..
        switch (action.type) {
            case SHOW_DROPDOWN :
            case SET_LAYOUT_MODE :
                ignore = handleLayoutChanges(action);
                break;

            case TABLE_LOADED :
                [showImages, images, coverageLockedOn] = handleNewTable(action, images, showImages, showTables, coverageLockedOn);
                break;

            case REPLACE_VIEWER_ITEMS :
            case ImagePlotCntlr.PLOT_IMAGE :
            case ImagePlotCntlr.PLOT_IMAGE_START :
                [showImages, images, ignore] = handleNewImage(action, images);
                break;
            case TBL_RESULTS_ACTIVE:
                [showImages, images, coverageLockedOn] = handleActiveTableChange(action.payload.tbl_id, images, coverageLockedOn);
                break;
            case TABLE_REMOVE:
                [showImages, images, coverageLockedOn] = handleActiveTableChange(getActiveTableId(findGroupByTblId(action.payload.tbl_id)), images, coverageLockedOn);
                break;

        }

        if (ignore) continue;  // ignores, don't update layout.

        const count = filter([showTables, showXyPlots, showImages]).length;

        // change mode when new UI elements are added or removed from results
        switch (action.type) {
            case CHART_ADD:
            case TBL_RESULTS_ADDED:
            case TABLE_LOADED:
            case REPLACE_VIEWER_ITEMS :
            case ImagePlotCntlr.PLOT_IMAGE :
            case ImagePlotCntlr.PLOT_IMAGE_START :
            case CHART_REMOVE:
            case TABLE_REMOVE:
            case ImagePlotCntlr.DELETE_PLOT_VIEW:
                if (count === 1) {
                    // set mode into expanded view when there is only 1 component visible.
                    closeable = false;
                    expanded =  showImages ? LO_VIEW.images :
                        showXyPlots ? LO_VIEW.xyPlots :
                            showTables ? LO_VIEW.tables :  expanded;
                } else {
                    expanded = LO_VIEW.none;
                }
                mode = {expanded, standard, closeable};
                break;
        }


        // calculate dropDown when new UI elements are added or removed from results
        switch (action.type) {
            case CHART_ADD:
            case TBL_RESULTS_ADDED:
            case TABLE_LOADED:
            case REPLACE_VIEWER_ITEMS :
            case ImagePlotCntlr.PLOT_IMAGE :
            case ImagePlotCntlr.PLOT_IMAGE_START :
                dropDown = {visible: count === 0};
                break;
            case SHOW_DROPDOWN:
            case CHART_REMOVE:
            case TABLE_REMOVE:
            case ImagePlotCntlr.DELETE_PLOT_VIEW:
                if (!get(dropDown, 'visible', false)) {
                    dropDown = {visible: count === 0};
                }
                break;
        }

        dispatchUpdateLayoutInfo(omitBy({title, views, mode, searchDesc, dropDown, showTables, showImages, showXyPlots, images}, isNil));
    }
}

function handleLayoutChanges(action) {
    if ((action.type === SHOW_DROPDOWN && get(action, 'payload.visible', true)) ||
        (action.type === SET_LAYOUT_MODE && get(action, 'payload.mode') === LO_MODE.expanded)) {
        return true;
    } else {
        return false;
    }
}

function handleNewTable(action, images, showImages, showTables, coverageLockedOn) {
    // check for catalog or meta images
    if (!showTables) return [showImages, images, coverageLockedOn];        // ignores this if table is not visible
    const {tbl_id} = action.payload;
    const isMeta = isMetaDataTable(tbl_id);
    if (isMeta || isCatalogTable(tbl_id)) {
        if (!get(images, 'showFits')) {
                          // only show coverage if there are not images or coverage is showing
            const showFits= shouldShowFits();
            coverageLockedOn= !showFits||coverageLockedOn;
            images = clone(images, {selectedTab: 'coverage', showCoverage: coverageLockedOn});
            showImages = true;
        }
    }
    if (isMeta) {
        images = clone(images, {selectedTab: 'meta', showMeta: true, metaDataTableId: tbl_id});
        showImages = true;
    }
    return [showImages, images, coverageLockedOn];
}

function handleActiveTableChange (tbl_id, images, coverageLockedOn) {
    // check for catalog or meta images

    const showFits= shouldShowFits();

    var showImages= showFits||coverageLockedOn;

    if (!tbl_id) {
        images = clone(images, {showMeta: false, showCoverage: false, showFits, metaDataTableId: null});
        return [showFits, images, false];
    }

    const tblGroup= findGroupByTblId(tbl_id);
    if (!tblGroup) return [showImages, images, coverageLockedOn];
    const tblList= getTblIdsByGroup(tblGroup);
    if (isEmpty(tblList)) return [showImages, images, coverageLockedOn];

    const anyHasCatalog= hasCatalogTable(tblList);
    const anyHasMeta= hasMetaTable(tblList);

    if (coverageLockedOn) {
        coverageLockedOn= anyHasCatalog || anyHasMeta;
        showImages= showFits || coverageLockedOn;
    }

    if (!anyHasCatalog && !anyHasMeta) {
        coverageLockedOn= false;
        showImages= showFits || coverageLockedOn;
        images = clone(images, {showMeta: false, showCoverage: false, showFits, metaDataTableId: null});
        return [showFits, images, coverageLockedOn];
    }



    if (hasCatalogTable(tblList)) {
        images = clone(images, {showCoverage: coverageLockedOn, showFits});
        showImages = true;
    }

    if (hasMetaTable(tblList)) {
        const metaTableId= isMetaDataTable(tbl_id) ? tbl_id : findFirstMetaTable(tblList);
        images = clone(images, {showMeta: true, showFits, metaDataTableId:metaTableId});
        showImages = true;
    }
    else {
        images = clone(images, {showMeta: false, showFits, metaTableId:null});
    }
    return [showImages, images, coverageLockedOn];
}

const hasCatalogTable= (tblList) => tblList.some( (id) => isCatalogTable(id) );
const hasMetaTable= (tblList) => tblList.some( (id) => isMetaDataTable(id) );
const findFirstMetaTable= (tblList) => tblList.find( (id) => isMetaDataTable(id) );
const shouldShowFits= () => !isEmpty(getViewerItemIds(getMultiViewRoot(), DEFAULT_FITS_VIEWER_ID));



function handleNewImage(action, images) {
    var ignore = false;
    const {viewerId, plotGroupId} = action.payload || {};
    if (viewerId === META_VIEWER_ID) {
        // select image meta tab when new images are added.
        images = clone(images, {selectedTab: 'meta', showMeta: true});
    } else if (viewerId === DEFAULT_FITS_VIEWER_ID) {
        // select image tab when new images are added.
        images = clone(images, {selectedTab: 'fits', showFits: true});
    } else {
        ignore = true;
    }

    return [true, images, ignore];
}
