/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React from 'react';
import PropTypes from 'prop-types';
import {get, isEmpty, isUndefined} from 'lodash';
import {take} from 'redux-saga/effects';

import {ImageExpandedMode} from '../iv/ImageExpandedMode.jsx';
import {Tab, Tabs} from '../../ui/panel/TabPanel.jsx';
import {MultiViewStandardToolbar} from './MultiViewStandardToolbar.jsx';
import {ImageMetaDataToolbar} from './ImageMetaDataToolbar.jsx';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {watchImageMetaData} from '../saga/ImageMetaDataWatcher.js';
import {watchCoverage} from '../saga/CoverageWatcher.js';
import {dispatchAddSaga} from '../../core/MasterSaga.js';
import {DEFAULT_FITS_VIEWER_ID, REPLACE_VIEWER_ITEMS, NewPlotMode, getViewerItemIds, getMultiViewRoot} from '../MultiViewCntlr.js';
import {getTblById, findGroupByTblId, getTblIdsByGroup, smartMerge} from '../../tables/TableUtil.js';
import {LO_MODE, LO_VIEW, SET_LAYOUT, dispatchSetLayoutMode, dispatchUpdateLayoutInfo, getLayouInfo} from '../../core/LayoutCntlr.js';
import {isMetaDataTable, isCatalogTable} from '../../metaConvert/converterUtils.js';
import ImagePlotCntlr, {visRoot} from '../../visualize/ImagePlotCntlr.js';
import {TABLE_LOADED, TBL_RESULTS_ACTIVE, TBL_RESULTS_ADDED} from '../../tables/TablesCntlr.js';

export const META_VIEWER_ID = 'triViewImageMetaData';


/**
 * This component works with ImageMetaDataWatch sega which should be launch during initialization
 * @param p
 * @param p.showCoverage
 * @param p.showFits
 * @param p.showMeta
 * @param p.imageExpandedMode if true, then imageExpandedMode overrides everything else
 * @param p.closeable expanded mode should have a close button
 * @param p.metaDataTableId
 * @return {XML}
 * @constructor
 */
export function TriViewImageSection({showCoverage=false, showFits=false, selectedTab='fits',
                                     showMeta=false, imageExpandedMode=false, closeable=true,
                                     metaDataTableId}) {

    if (imageExpandedMode) {
        return  ( <ImageExpandedMode
                        key='results-plots-expanded'
                        closeFunc={closeable ? closeExpanded : null}/>
                );
    }
    const onTabSelect = (idx, id) => dispatchUpdateLayoutInfo({images:{selectedTab:id}});
    const table= getTblById(metaDataTableId);
    const metaTitle= get(table,'tableMeta.title')  || 'Image Meta Data';


    // showCoverage= true; // todo - let the application control is coverage is visible

    if (showCoverage || showFits || showMeta) {
        return (
            <Tabs onTabSelect={onTabSelect} defaultSelected={selectedTab} useFlex={true} resizable={true}>
                { showFits &&
                    <Tab name='FITS Data' removable={false} id='fits'>
                        <MultiImageViewer viewerId= {DEFAULT_FITS_VIEWER_ID}
                                          insideFlex={true}
                                          canReceiveNewPlots={NewPlotMode.create_replace.key}
                                          Toolbar={MultiViewStandardToolbar}/>
                    </Tab>
                }
                { showMeta &&
                    <Tab name={metaTitle} removable={false} id='meta'>
                        <MultiImageViewer viewerId= {META_VIEWER_ID}
                                          insideFlex={true}
                                          canReceiveNewPlots={NewPlotMode.none.key}
                                          tableId={metaDataTableId}
                                          Toolbar={ImageMetaDataToolbar}/>
                    </Tab>
                }
                { showCoverage &&
                    <Tab name='Coverage' removable={false} id='coverage'>
                        <MultiImageViewer viewerId='coverageImages'
                                          insideFlex={true}
                                          canReceiveNewPlots={NewPlotMode.replace_only.key}
                                          Toolbar={MultiViewStandardToolbar}/>
                    </Tab>
                }
            </Tabs>
        );

    }
    else {
        return <div>todo</div>;
    }
}


TriViewImageSection.propTypes= {
    showCoverage : PropTypes.bool,
    showFits : PropTypes.bool,
    showMeta : PropTypes.bool,
    imageExpandedMode : PropTypes.bool,
    closeable: PropTypes.bool,
    metaDataTableId: PropTypes.string,
    selectedTab: PropTypes.oneOf(['fits', 'meta', 'coverage'])
};

export function launchImageMetaDataSega() {
    dispatchAddSaga(watchImageMetaData,{viewerId: META_VIEWER_ID});
    dispatchAddSaga(watchCoverage, {viewerId:'coverageImages', ignoreCatalogs:true});
    dispatchAddSaga(layoutHandler);
}

/**
 * this saga manages layout info related to this component.
 * @param dispatch
 */
function* layoutHandler(dispatch) {

    while (true) {
        const action = yield take([
            ImagePlotCntlr.PLOT_IMAGE_START, ImagePlotCntlr.PLOT_IMAGE,
            ImagePlotCntlr.DELETE_PLOT_VIEW, REPLACE_VIEWER_ITEMS,
            TBL_RESULTS_ACTIVE, TABLE_LOADED, TBL_RESULTS_ADDED
        ]);

        /**
         * This is the current state of the layout store.  Action handlers should return newLayoutInfo if state changes
         * If state has changed, it will be dispatched into the flux.
         * @type {LayoutInfo}
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
                newLayoutInfo = onNewImage(newLayoutInfo, action);
                break;
            case ImagePlotCntlr.DELETE_PLOT_VIEW:
                newLayoutInfo = onPlotDelete(newLayoutInfo, action);
                break;
            case TABLE_LOADED:
            case TBL_RESULTS_ADDED:
                newLayoutInfo = handleNewTable(newLayoutInfo, action);
                break;
            case TBL_RESULTS_ACTIVE:
                newLayoutInfo = onActiveTable(newLayoutInfo, action);
                break;
        }

        if (newLayoutInfo !== layoutInfo) {
            dispatchUpdateLayoutInfo(newLayoutInfo);

        }
    }
}


/*-----------------------------------------------------------------------------------------*/



function closeExpanded() {
    dispatchSetLayoutMode(LO_MODE.expanded, LO_VIEW.none);
}
const hasCatalogTable= (tblList) => tblList.some( (id) => isCatalogTable(id) );
const hasMetaTable= (tblList) => tblList.some( (id) => isMetaDataTable(id) );
const findFirstMetaTable= (tblList) => tblList.find( (id) => isMetaDataTable(id) );
const shouldShowFits= () => !isEmpty(getViewerItemIds(getMultiViewRoot(), DEFAULT_FITS_VIEWER_ID));

function handleNewTable(layoutInfo, action) {
    const {tbl_id} = action.payload;
    var {images={}, showImages, showTables} = layoutInfo;
    var {coverageLockedOn, showFits, showMeta, showCoverage, selectedTab, metaDataTableId} = images;
    const isMeta = isMetaDataTable(tbl_id);
    
    if ((isMeta || isCatalogTable(tbl_id)) && showTables ) {
        if (!showFits) {
            // only show coverage if there are not images or coverage is showing
            showFits= shouldShowFits();
            coverageLockedOn= !showFits||coverageLockedOn;
            selectedTab = 'coverage';
            showCoverage = coverageLockedOn;
            showImages = true;
        }
    }
    if (isMeta && showTables) {
        showImages = true;
        selectedTab = 'meta';
        showMeta = true;
        metaDataTableId = tbl_id;
    }
    return smartMerge(layoutInfo, {showTables: true, showImages, images: {coverageLockedOn, showFits, showMeta, showCoverage, selectedTab, metaDataTableId}});
}

function onActiveTable (layoutInfo, action) {
    const {tbl_id} = action.payload;
    var {images={}, showImages} = layoutInfo;
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

    if (anyHasCatalog || anyHasMeta) {
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
    return smartMerge(layoutInfo, {images: {coverageLockedOn, showCoverage, showMeta, metaDataTableId}, showImages});
}

function onPlotDelete(layoutInfo, action) {
    var {images={}, showImages} = layoutInfo;
    var {coverageLockedOn} = images;
    const showFits = shouldShowFits();
    if (!get(visRoot(), 'plotViewAry.length', 0)) {
        coverageLockedOn = false;
        showImages = false;
    }
    return smartMerge(layoutInfo, {showImages, images:{coverageLockedOn, showFits}});
}

function onNewImage(layoutInfo, action) {
    var {images={}} = layoutInfo;
    var {selectedTab, showMeta, showFits, coverageLockedOn} = images;

    const {viewerId} = action.payload || {};
    if (viewerId === META_VIEWER_ID) {
        // select meta tab when new images are added to meta image group.
        selectedTab = 'meta';
        showMeta = true;
    } else if (viewerId === DEFAULT_FITS_VIEWER_ID) {
        // select fits tab when new images are added to default group.
        selectedTab = 'fits';
        showFits = true;
    } else {
        // why lock coverage here?
        coverageLockedOn = true;
    }
    return smartMerge(layoutInfo, {showImages: true, images: {coverageLockedOn, selectedTab, showMeta, showFits}});
}
