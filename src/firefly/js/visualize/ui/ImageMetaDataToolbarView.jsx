/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Sheet, Stack} from '@mui/joy';
import {isEmpty, isEqual, omit} from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import {SD_CUTOUT_KEY} from '../../metaConvert/vo/ServDescProducts';
import {getTblInfo} from '../../tables/TableUtil.js';
import {getComponentState} from '../../core/ComponentCntlr.js';
import {showCutoutSizeDialog} from '../../ui/CutoutSizeDialog.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {getObsCoreOption} from '../../ui/tap/TableSearchHelpers';
import {makeFoVString} from '../ZoomUtil.js';
import {ToolbarButton, ToolbarHorizontalSeparator} from '../../ui/ToolbarButton.jsx';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {
    dispatchChangeViewerLayout, getViewer, getMultiViewRoot,
    GRID_FULL, GRID_RELATED, SINGLE, GRID, getLayoutDetails
} from '../MultiViewCntlr.js';
import {DisplayTypeButtonGroup, ThreeColor} from './Buttons.jsx';
import {showColorBandChooserPopup} from './ColorBandChooserPopup.jsx';
import {ImagePager} from './ImagePager.jsx';
import {VisMiniToolbar} from 'firefly/visualize/ui/VisMiniToolbar.jsx';

import ContentCutRoundedIcon from '@mui/icons-material/ContentCutRounded';


export function ImageMetaDataToolbarView({viewerId, viewerPlotIds=[], layoutType, factoryKey, serDef,
                                             enableCutout, pixelBasedCutout,
                                          activeTable, makeDataProductsConverter, makeDropDown}) {

    const converter= makeDataProductsConverter(activeTable,factoryKey) || {};
    const {canGrid, hasRelatedBands, converterId, maxPlots, threeColor, dataProductsComponentKey}= converter ?? {};
    const cutoutValue= useStoreConnector( () => getComponentState(dataProductsComponentKey)[SD_CUTOUT_KEY]) ?? getObsCoreOption('cutoutDefSizeDeg') ?? .01;

    if (!converter) return <div/>;
    let cSize='';
    if (dataProductsComponentKey&&enableCutout) {
        if (pixelBasedCutout) {
            cSize= cutoutValue+'';
        }
        else {
            cSize= makeFoVString(Number(cutoutValue));
        }

    }


    const layoutDetail= getLayoutDetails(getMultiViewRoot(), viewerId, activeTable?.tbl_id);
    const viewer= getViewer(getMultiViewRoot(), viewerId);

    // single mode stuff

    const showThreeColorButton= threeColor && viewer?.layout===GRID &&
        layoutDetail!==GRID_FULL && !(viewerPlotIds[0].includes(GRID_FULL.toLowerCase()));
    const showPager= activeTable && canGrid && layoutType===GRID && layoutDetail===GRID_FULL;
    const showMultiImageOps= canGrid || hasRelatedBands;


    let metaControls= true;
    if (!makeDropDown && !showMultiImageOps && !canGrid && !hasRelatedBands && !showThreeColorButton &&
        !(layoutType===SINGLE && viewerPlotIds.length>1) && !showPager) {
        metaControls= false;
    }

    const gridConfig=[];
    const gridValue= layoutType===SINGLE ? 'one' : layoutType===GRID && layoutDetail!==GRID_RELATED ? 'gridFull' : 'gridRelated';

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
                    {enableCutout &&
                        <ToolbarButton
                            icon={<ContentCutRoundedIcon/>}
                            text={`${cSize}`} onClick={() => showCutoutSizeDialog(cutoutValue,pixelBasedCutout,dataProductsComponentKey)}/>
                    }
                    {metaControls &&
                        <Stack direction='row' spacing={1} alignItems='center' whiteSpace='nowrap'>
                            {showMultiImageOps && <DisplayTypeButtonGroup {...{value:gridValue, config:gridConfig }}/>}
                            {showThreeColorButton &&
                                <ThreeColor tip='Create three color image'
                                            onClick={() => showThreeColorOps(viewerId,converter,activeTable,converterId)}/>
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
    dlAry : PropTypes.arrayOf(PropTypes.object),
    activePlotId : PropTypes.string,
    viewerId : PropTypes.string.isRequired,
    layoutType : PropTypes.string.isRequired,
    viewerPlotIds : PropTypes.arrayOf(PropTypes.string).isRequired,
    activeTable: PropTypes.object,
    makeDataProductsConverter: PropTypes.func,
    makeDropDown: PropTypes.func,
    serDef: PropTypes.object,
    enableCutout: PropTypes.bool,
    pixelBasedCutout: PropTypes.bool,
    factoryKey: PropTypes.string
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


