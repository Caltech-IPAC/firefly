/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {get, filter, omitBy, isNil} from 'lodash';

import {LO_VIEW, LO_MODE, SHOW_DROPDOWN, SET_LAYOUT_MODE, getLayouInfo, dispatchUpdateLayoutInfo} from '../LayoutCntlr.js';
import {clone} from '../../util/WebUtil.js';
import {TBL_RESULTS_ADDED, TABLE_NEW, TABLE_REMOVE} from '../../tables/TablesCntlr.js';
import ImagePlotCntlr from '../../visualize/ImagePlotCntlr.js';
import {isMetaDataTable, isCatalogTable} from '../../metaConvert/converterUtils.js';
import {META_VIEWER_ID} from '../../visualize/ui/TriViewImageSection.jsx';
import {REPLACE_IMAGES, DEFAULT_FITS_VIEWER_ID, getViewerPlotIds, getMultiViewRoot} from '../../visualize/MultiViewCntlr.js';

/**
 * this manager manages what main components get display on the screen.
 * These main components are image plots, charts, tables, dropdown panel, etc.
 * This manager implements the default firefly viewer's requirements.
 * Because it may differs between applications, it is okay to have a custom layout manager if needed.
 * @param [title] title to display.
 * @param views defaults to tri-view if not given.
 * @param callback
 */
export function* layoutManager({title, views='tables | images | xyPlots'}) {
    views = LO_VIEW.get(views) || LO_VIEW.none;

    while (true) {
        const action = yield take([
            ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.DELETE_PLOT_VIEW, REPLACE_IMAGES,
            TBL_RESULTS_ADDED, TABLE_REMOVE, TABLE_NEW,
            SHOW_DROPDOWN, SET_LAYOUT_MODE
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

        const ids = getViewerPlotIds(getMultiViewRoot(), DEFAULT_FITS_VIEWER_ID);
        images = clone(images, {showFits: ids && ids.length > 0});

        // special cases which could affect the layout..
        switch (action.type) {
            case SHOW_DROPDOWN :
            case SET_LAYOUT_MODE :
                ignore = handleLayoutChanges(action);
                break;

            case TABLE_NEW :
                [showImages, images] = handleNewTable(action, images, showImages);
                break;

            case REPLACE_IMAGES :
            case ImagePlotCntlr.PLOT_IMAGE :
                [showImages, images, ignore] = handleNewImage(action, images);
                break;
        }

        if (ignore) continue;  // ignores, don't update layout.

        const count = filter([showTables, showXyPlots, showImages]).length;

        // change mode when new UI elements are added or removed from results
        switch (action.type) {
            case TBL_RESULTS_ADDED:
            case REPLACE_IMAGES :
            case ImagePlotCntlr.PLOT_IMAGE :
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
        }

        // calculate dropDown when new UI elements are added or removed from results
        switch (action.type) {
            case TBL_RESULTS_ADDED:
            case REPLACE_IMAGES :
            case ImagePlotCntlr.PLOT_IMAGE :
                dropDown = {visible: count === 0};
                break;
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

function handleNewTable(action, images, showImages) {
    // check for catalog or meta images
    const {tbl_id} = action.payload;
    const isMeta = isMetaDataTable(tbl_id);
    if (isMeta || isCatalogTable(tbl_id)) {
        if (!get(images, 'showFits')) {
            images = clone(images, {selectedTab: 'coverage', showCoverage: true});
            showImages = true;
        }
    }
    if (isMeta) {
        images = clone(images, {selectedTab: 'meta', showMeta: true});
        showImages = true;
    }
    return [showImages, images];
}

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
    };

    return [true, images, ignore];
}
