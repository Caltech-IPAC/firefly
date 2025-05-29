/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Sheet, Stack} from '@mui/joy';
import {isEmpty, isEqual, omit} from 'lodash';
import React from 'react';
import PropTypes, {arrayOf, bool, func, string, object, element, any} from 'prop-types';
import {getTblInfo} from '../../tables/TableUtil.js';
import {CutoutButton, showCutoutSizeDialog, SHOWING_CUTOUT, SHOWING_FULL} from '../../ui/CutoutSizeDialog.jsx';
import {ViewerScroll} from '../iv/ExpandedTools';
import {ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {
    dispatchChangeViewerLayout, getViewer, getMultiViewRoot,
    GRID_FULL, GRID_RELATED, SINGLE, GRID, getLayoutDetails
} from '../MultiViewCntlr.js';
import {DisplayTypeButtonGroup, ThreeColor} from './Buttons.jsx';
import {showColorBandChooserPopup} from './ColorBandChooserPopup.jsx';
import {ImagePager} from './ImagePager.jsx';
import {VisMiniToolbar} from 'firefly/visualize/ui/VisMiniToolbar.jsx';

export function ImageMetaDataToolbarView({viewerId, viewerPlotIds=[], layoutType, factoryKey, serDef,
                                             enableCutout, pixelBasedCutout,enableCutoutFullSwitching,
                                             cutoutToFullWarning, containerElement,
                                          activeTable, makeDataProductsConverter, makeDropDown}) {

    const converter= makeDataProductsConverter(activeTable,factoryKey) || {};
    const {canGrid, hasRelatedBands, converterId, maxPlots, threeColor, dataProductsComponentKey}= converter ?? {};

    if (!converter) return <div/>;

    const viewer= getViewer(getMultiViewRoot(), viewerId) ?? {scroll:false};
    const layoutDetail= getLayoutDetails(getMultiViewRoot(), viewerId, activeTable?.tbl_id);




    // single mode stuff

    const showThreeColorButton= threeColor && viewer.layout===GRID &&
        layoutDetail!==GRID_FULL && !(viewerPlotIds[0].includes(GRID_FULL.toLowerCase()));
    const showPager= activeTable && canGrid && layoutType===GRID && layoutDetail===GRID_FULL;
    const showMultiImageOps= canGrid || hasRelatedBands;

    const {width,height}= containerElement?.getBoundingClientRect() ?? {width:0,height:0};
    const showScroll= showMultiImageOps && (width>height || viewer.scroll);


    let metaControls= true;
    if (!makeDropDown && !showMultiImageOps && !canGrid && !hasRelatedBands && !showThreeColorButton &&
        !(layoutType===SINGLE && viewerPlotIds.length>1) && !showPager) {
        metaControls= false;
    }


    const cutoutMode= enableCutout
        ? SHOWING_CUTOUT :
        enableCutoutFullSwitching
            ? SHOWING_FULL : undefined;

    const gridValue= layoutType===SINGLE
        ? 'one' :
        layoutType===GRID && layoutDetail!==GRID_RELATED
            ? 'gridFull' : 'gridRelated';

    const gridConfig=[];
    if (showMultiImageOps) {
        gridConfig.push(
            { value:'one', title:'Show single image at full size',
                onClick: () => dispatchChangeViewerLayout(viewerId,SINGLE, undefined, activeTable?.tbl_id)
            }
        );
    }
    if (canGrid) {
        gridConfig.push(
            { value:'gridFull', title:'Tile all images in the search result table',
                onClick: () => dispatchChangeViewerLayout(viewerId,GRID,GRID_FULL,activeTable?.tbl_id)
            }
        );
    }

    if (hasRelatedBands) {
        gridConfig.push(
            { value:'gridRelated', title:'Tile all data products associated with the highlighted table row',
                onClick: () =>
                    dispatchChangeViewerLayout(viewerId,GRID,GRID_RELATED,activeTable?.tbl_id)
            }
        );
    }


    return (
        <Sheet>
            <Stack direction='row' alignItems='center'
                   sx={{ flexWrap:'wrap', justifyContent:'space-between', minHeight: 32}}>
                <Stack direction='row' alignItems='center' divider={<ToolbarHorizontalSeparator/>}
                       sx={{ pl: 1/2, flexWrap:'wrap'}}>
                    {makeDropDown ? makeDropDown() : false}
                    {cutoutMode && <CutoutButton {...{dataProductsComponentKey,activeTable, serDef,pixelBasedCutout,
                        enableCutoutFullSwitching, cutoutToFullWarning, cutoutMode }}/> }
                    {metaControls &&
                        <Stack direction='row' spacing={1} alignItems='center' whiteSpace='nowrap'>
                            {showMultiImageOps && <DisplayTypeButtonGroup {...{value:gridValue, config:gridConfig }}/>}
                            {showThreeColorButton &&
                                <ThreeColor tip='Create three color image'
                                            onClick={() => showThreeColorOps(viewerId,converter,activeTable,converterId)}/>
                            }
                            {showScroll &&
                                <>
                                    <ToolbarHorizontalSeparator/>
                                    <ViewerScroll {...{viewerId,checked:viewer.scroll,count:viewerPlotIds.length}}/>
                                </>
                            }
                        </Stack> }
                    {showPager && <ImagePager pageSize={maxPlots} tbl_id={activeTable.tbl_id}/>}
                </Stack>
                <VisMiniToolbar viewerId={viewerId}/>
            </Stack>
        </Sheet>
    );
}

ImageMetaDataToolbarView.propTypes= {
    dlAry : arrayOf(object),
    activePlotId : string,
    viewerId : string.isRequired,
    layoutType : string.isRequired,
    viewerPlotIds : arrayOf(PropTypes.string).isRequired,
    activeTable: object,
    makeDataProductsConverter: func,
    makeDropDown: func,
    serDef: object,
    enableCutout: bool,
    pixelBasedCutout: bool,
    factoryKey: string,
    cutoutToFullWarning: string,
    enableCutoutFullSwitching: bool,
    containerElement: any,
};



async function showThreeColorOps(viewerId,converter, table, converterId) {
    const viewer= getViewer(getMultiViewRoot(), viewerId);
    if (!viewer && !converter) return;

    const tableState= getTblInfo(table);
    const {highlightedRow}= tableState;

    const workingBandData= omit(viewer.customData[converterId],'threeColorVisible');
    const originalBandData= await converter.describeThreeColor?.(table,highlightedRow,converter.options);

    // if something has changed the go back to original data from describeThreeColor
    // todo: we probably need to improve this test- by titles? by title and Length? only by title that are used?
    const bandData= isEqual(Object.keys(workingBandData),Object.keys(originalBandData)) ?
        workingBandData : originalBandData;

    if (isEmpty(bandData)) {
        showInfoPopup('Three color is not supported');
        return;
    }
    showColorBandChooserPopup(viewer.viewerId,bandData,converterId);
}


