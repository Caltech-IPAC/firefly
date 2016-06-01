/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {get, isEmpty, filter, omitBy, isNil} from 'lodash';
import Enum from 'enum';
import {flux} from '../Firefly.js';
import {TBL_RESULTS_ADDED, TABLE_NEW, TABLE_REMOVE} from '../tables/TablesCntlr.js';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import {smartMerge} from '../tables/TableUtil.js';
import {getDropDownNames} from '../ui/Menu.jsx';
import {isMetaDataTable, isCatalogTable} from '../metaConvert/converterUtils.js';
import {META_VIEWER_ID, FITS_VIEWER_ID, COVERAGE_VIEWER_ID} from '../visualize/ui/TriViewImageSection.jsx';
import {ADD_IMAGES, REPLACE_IMAGES, REMOVE_IMAGES} from '../visualize/MultiViewCntlr.js';


export const LAYOUT_PATH = 'layout';

// this enum is flaggable, therefore you can use any combination of the 3, i.e. 'tables | images'.
export const LO_VIEW = new Enum(['none', 'tables', 'images', 'xyPlots'], { ignoreCase: true });
export const LO_MODE = new Enum(['expanded', 'standard']);

/*---------------------------- Actions ----------------------------*/

export const SET_LAYOUT         = `${LAYOUT_PATH}.setLayout`;
export const SET_LAYOUT_MODE    = `${LAYOUT_PATH}.setLayoutMode`;
export const SHOW_DROPDOWN      = `${LAYOUT_PATH}.showDropDown`;


/*---------------------------- Reducers ----------------------------*/

export function reducer(state={}, action={}) {
    var {mode, view} = action.payload || {};

    switch (action.type) {
        case SET_LAYOUT :
            return smartMerge(state, action.payload);

        case SET_LAYOUT_MODE :
            return smartMerge(state, {mode: {[mode]: view}});

        case SHOW_DROPDOWN :
            const {visible = true} = action.payload;
            view =  view || get(state, 'dropDown.view') || getDropDownNames()[0];
            return smartMerge(state, {dropDown: {visible, view}});

        default:
            return state;
    }

}



/*---------------------------- DISPATCHERS -----------------------------*/

/**
 * set the layout mode of the application.  see LO_MODE and LO_VIEW enums for options.
 * @param mode standard or expanded
 * @param view see LO_VIEW for options.
 */
export function dispatchSetLayoutMode(mode=LO_MODE.standard, view) {
    flux.process({type: SET_LAYOUT_MODE, payload: {mode, view}});
}

/**
 * set the layout info of the application.  data will be merged.
 * @param layoutInfo data to be updated
 */
export function dispatchUpdateLayoutInfo(layoutInfo) {
    flux.process({type: SET_LAYOUT, payload: {...layoutInfo}});
}

/**
 * show the drop down container
 * @param view name of the component to display in the drop-down container
 */
export function dispatchShowDropDown({view}={}) {
    flux.process({type: SHOW_DROPDOWN, payload: {visible: true, view}});
}

/**
 * hide the drop down container
 */
export function dispatchHideDropDown() {
    flux.process({type: SHOW_DROPDOWN, payload: {visible: false}});
}


/*------------------------- Util functions -------------------------*/
export function getExpandedMode() {
    return get(flux.getState(), ['layout','mode','expanded']);
}

export function getStandardMode() {
    return get(flux.getState(), ['layout','mode','standard']);
}

export function getDropDownInfo() {
    return get(flux.getState(), 'layout.dropDown', {visible: false});
}

export function getLayouInfo() {
    const layout = get(flux.getState(), 'layout', {});
    const hasImages = get(flux.getState(), 'allPlots.plotViewAry.length') > 0;
    const hasTables = !isEmpty(get(flux.getState(), 'table_space.results.main.tables', {}));
    const hasXyPlots = hasTables;
    return Object.assign({}, layout, {hasImages, hasTables, hasXyPlots});
}


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

        var {hasImages, hasTables, hasXyPlots, mode, ...others} = getLayouInfo();
        // eslint-disable-next-line
        var {images, tables, xyPlots} = others;     //images, tables, and xyPlots are additional states relevant only to them.
        var {expanded, standard} = mode || {};
        var searchDesc = '';
        var closeable = true;
        standard = standard || views;

        var showImages = hasImages && views.has(LO_VIEW.images);
        const showXyPlots = hasXyPlots && views.has(LO_VIEW.xyPlots);
        const showTables = hasTables && views.has(LO_VIEW.tables);

        // special cases which could affect the layout..
        const {viewerId} = action.payload || {};
        switch (action.type) {
            case SHOW_DROPDOWN :
                if (get(action, 'payload.visible', true)) continue;         // ignore show action
                break;

            case SET_LAYOUT_MODE :
                if (get(action, 'payload.mode') === LO_MODE.expanded) continue;     // ignore expanded mode action.
                break;

            case TABLE_NEW :
                // check for catalog or meta images
                const {tbl_id} = action.payload;
                if (isCatalogTable(tbl_id)) {
                    [showImages, images] = selectCoverageIf(images, showImages);
                } else if (isMetaDataTable(tbl_id)) {
                    [showImages, images] = selectMeta(images);
                };
                break;

            case REPLACE_IMAGES :
                // select image meta tab when new images are added.
                if (viewerId === META_VIEWER_ID) {
                    [showImages, images] = selectMeta(images);
                } else {
                    continue;   // ignores others viewerId..
                };
                break;

            case ImagePlotCntlr.PLOT_IMAGE :
                // select image tab when new images are added.
                const {plotGroupId} = action.payload || {};
                if (viewerId === FITS_VIEWER_ID ||
                    (!viewerId && plotGroupId === 'remoteGroup') ) {    // only way to pick up external viewer api images
                    [showImages, images] = selectFits(images);
                } else {
                    continue;   // ignores others viewerId..
                };
                break;
        }

        const count = filter([showTables, showXyPlots, showImages]).length;
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
        const dropDown = {visible: count === 0};

        dispatchUpdateLayoutInfo(omitBy({title, views, mode, searchDesc, dropDown, showTables, showImages, showXyPlots, images}, isNil));
    }
}

function selectFits(images) {
    return [true, Object.assign({}, images || {}, {selectedTab: 'fits', showFits: true})];
}

function selectMeta(images) {
    return [true, Object.assign({}, images || {}, {selectedTab: 'meta', showMeta: true})];
}

function selectCoverageIf(images, showImages) {
    if (!showImages) {
        return [true, Object.assign({}, images || {}, {selectedTab: 'coverage', showCoverage: true})];
    } else {
        return [showImages, images];
    }
}
