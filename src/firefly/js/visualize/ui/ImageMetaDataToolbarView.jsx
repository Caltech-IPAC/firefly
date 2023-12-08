/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Divider, Stack} from '@mui/joy';
import {isEmpty, isEqual, omit} from 'lodash';
import React from 'react';
import PropTypes from 'prop-types';
import {getTblInfo} from '../../tables/TableUtil.js';
import {showInfoPopup} from '../../ui/PopupUtil.jsx';
import {
    dispatchChangeViewerLayout, getViewer, getMultiViewRoot,
    GRID_FULL, GRID_RELATED, SINGLE, GRID, getLayoutDetails
} from '../MultiViewCntlr.js';
import {showColorBandChooserPopup} from './ColorBandChooserPopup.jsx';
import {ImagePager} from './ImagePager.jsx';
import {VisMiniToolbar} from 'firefly/visualize/ui/VisMiniToolbar.jsx';
import {ToolbarButton} from '../../ui/ToolbarButton.jsx';

import ONE from 'html/images/icons-2014/Images-One.png';
import GRID_GROUP from 'html/images/icons-2014/Images-plus-related.png';
import FULL_GRID from 'html/images/icons-2014/Images-Tiled-full.png';
import THREE_COLOR from 'html/images/icons-2014/28x28_FITS_Modify3Image.png';




export function ImageMetaDataToolbarView({viewerId, viewerPlotIds=[], layoutType, factoryKey,
                                          activeTable, makeDataProductsConverter, makeDropDown}) {

    const converter= makeDataProductsConverter(activeTable,factoryKey) || {};
    if (!converter) return <div/>;

    const {canGrid, hasRelatedBands, converterId, maxPlots, threeColor}= converter;

    const layoutDetail= getLayoutDetails(getMultiViewRoot(), viewerId, activeTable?.tbl_id);

    // single mode stuff

    const showThreeColorButton= threeColor && layoutDetail!==GRID_FULL && !(viewerPlotIds[0].includes(GRID_FULL.toLowerCase()));
    const showPager= activeTable && canGrid && layoutType===GRID && layoutDetail===GRID_FULL;
    const showMultiImageOps= canGrid || hasRelatedBands;


    let metaControls= true;
    if (!makeDropDown && !showMultiImageOps && !canGrid && !hasRelatedBands && !showThreeColorButton &&
        !(layoutType===SINGLE && viewerPlotIds.length>1) && !showPager) {
        metaControls= false;
    }


    return (
        <Stack direction='row' alignItems='center' style={{ flexWrap:'nowrap', justifyContent:'space-between', height: 30}}>
            {makeDropDown && makeDropDown()}
            {makeDropDown && <Divider orientation='vertical' sx={{mx:1}}/> }
            {metaControls && <Stack direction='row' alignItems='center' whiteSpace='nowrap'>
                {showMultiImageOps && <ToolbarButton icon={ONE} tip={'Show single image at full size'}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               horizontal={true}
                               onClick={() => dispatchChangeViewerLayout(viewerId,SINGLE, undefined, activeTable?.tbl_id)}/>}

                {canGrid && <ToolbarButton icon={FULL_GRID} tip={'Tile all images in the search result table'}
                               horizontal={true}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               onClick={() => dispatchChangeViewerLayout(viewerId,GRID,GRID_FULL,activeTable?.tbl_id)}/>}

                {hasRelatedBands  &&
                            <ToolbarButton icon={GRID_GROUP} tip={'Tile all data products associated with the highlighted table row'}
                               horizontal={true}
                               imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                               style={{marginLeft: 20}}
                               onClick={() => dispatchChangeViewerLayout(viewerId,GRID,GRID_RELATED,activeTable?.tbl_id)}/>
                }
                {showThreeColorButton &&
                             <ToolbarButton icon={THREE_COLOR} tip={'Create three color image'}
                                         horizontal={true}
                                         imageStyle={{width:24,height:24, flex: '0 0 auto'}}
                                         onClick={() => showThreeColorOps(viewerId,converter,activeTable,converterId)}/>
                }
            </Stack> }
            {showPager && <ImagePager pageSize={maxPlots} tbl_id={activeTable.tbl_id} style={{marginLeft:10}}/>}
            <Divider orientation='vertical' sx={{mx:1}}/>
            <VisMiniToolbar viewerId={viewerId}/>
        </Stack>
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


