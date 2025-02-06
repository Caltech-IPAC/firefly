/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Sheet, Stack, Typography} from '@mui/joy';
import {isEmpty, isEqual, omit} from 'lodash';
import React from 'react';
import PropTypes, {arrayOf, bool, func, string, object} from 'prop-types';
import {getTblInfo} from '../../tables/TableUtil.js';
import {showCutoutSizeDialog} from '../../ui/CutoutSizeDialog.jsx';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {
    findCutoutTarget, getCutoutErrorStr, getCutoutSize, getCutoutTargetType,
    ROW_POSITION, SEARCH_POSITION, USER_ENTERED_POSITION } from '../../ui/tap/Cutout';
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
import ReadMoreRoundedIcon from '@mui/icons-material/ReadMoreRounded';
import CrisisAlertRoundedIcon from '@mui/icons-material/CrisisAlertRounded';
import AccessibilityRoundedIcon from '@mui/icons-material/AccessibilityRounded';

const SHOWING_FULL='SHOWING_FULL';
const SHOWING_CUTOUT='SHOWING_CUTOUT';

export function ImageMetaDataToolbarView({viewerId, viewerPlotIds=[], layoutType, factoryKey, serDef,
                                             enableCutout, pixelBasedCutout,enableCutoutFullSwitching,
                                             cutoutToFullWarning,
                                          activeTable, makeDataProductsConverter, makeDropDown}) {

    const converter= makeDataProductsConverter(activeTable,factoryKey) || {};
    const {canGrid, hasRelatedBands, converterId, maxPlots, threeColor, dataProductsComponentKey}= converter ?? {};

    if (!converter) return <div/>;

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
    enableCutoutFullSwitching: bool
};

function CutoutButton({dataProductsComponentKey,activeTable, serDef,pixelBasedCutout,
                          enableCutoutFullSwitching, cutoutToFullWarning, cutoutMode}) {
    const cutoutValue= useStoreConnector( () => getCutoutSize(dataProductsComponentKey));
    const {positionWP:cutoutCenterWP, requestedType,foundType}= findCutoutTarget(dataProductsComponentKey,serDef,activeTable,activeTable?.highlightedRow);
    const cutoutTypeError= requestedType!==foundType;
    const cSize= dataProductsComponentKey ? pixelBasedCutout ? cutoutValue+'' : makeFoVString(Number(cutoutValue)) : '';

    if (!dataProductsComponentKey || !activeTable) return;

    if (cutoutMode===SHOWING_FULL) {
        return (
            <ToolbarButton
                icon={<ContentCutRoundedIcon/>} text={'Off'}
                title='Showing original image (with all extensions), click here to show a cutout'
                onClick={() =>
                    showCutoutSizeDialog({showingCutout:false,cutoutDefSizeDeg:cutoutValue,pixelBasedCutout,
                        tbl_id:activeTable?.tbl_id, cutoutCenterWP, serDef,
                        dataProductsComponentKey,enableCutoutFullSwitching})}/>
            );
    }
    else if (cutoutMode===SHOWING_CUTOUT) {
        return (
            <ToolbarButton
                icon={
                    <CutoutIcon
                        type={getCutoutTargetType(dataProductsComponentKey,activeTable?.tbl_id, serDef)}
                        showTypeError={cutoutTypeError}
                    />}
                text={`${cSize}`}
                title={
                    <Stack>
                        <Typography>
                            Showing cutout of image, click here to modify cutout or show original image (with all extensions)
                        </Typography>
                        {
                            cutoutTypeError &&
                            <Typography color='warning'>
                                {getCutoutErrorStr(foundType,requestedType)}
                            </Typography>

                        }
                    </Stack>
                }
                onClick={() =>
                    showCutoutSizeDialog({
                        showingCutout:true,cutoutDefSizeDeg:cutoutValue,pixelBasedCutout,
                        tbl_id:activeTable?.tbl_id, cutoutCenterWP, serDef,
                        dataProductsComponentKey,enableCutoutFullSwitching, cutoutToFullWarning})}
            />
        );
    }

}


const CutoutIcon= ({type, showTypeError}) => {
    if (!type) return <ContentCutRoundedIcon/>;

    let typeIcon;
    const sx={position:'absolute', transform: 'scale(.9)', top:3, left:15};
    switch (type) {
        case USER_ENTERED_POSITION:
            typeIcon= <AccessibilityRoundedIcon {...{color: showTypeError?'danger':undefined ,sx:{...sx}}}/>;
            break;
        case SEARCH_POSITION:
            typeIcon= <CrisisAlertRoundedIcon {...{color: showTypeError?'danger':undefined ,sx:{...sx, transform:'scale(.8)', top:sx.top+1}}}/>;
            break;
        case ROW_POSITION:
            typeIcon= <ReadMoreRoundedIcon {...{color: showTypeError?'danger':undefined ,sx:{...sx,transform:'scale(1.1)', left:sx.left-2}}}/>;
            break;
    }
    const icon= (
        <Box sx={{width:24,height:24, position:'relative'}}>
            <ContentCutRoundedIcon sx={{position:'absolute', top:4, left:-1}}/>
            {typeIcon}
        </Box>
    );
    return icon;
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


