/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import React, {useEffect} from 'react';
import {string, number, func, bool} from 'prop-types';
import {isNil, xor} from 'lodash';
import {useFieldGroupValue, useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {getTblById} from '../../tables/TableUtil.js';
import {MultiImageViewer} from './MultiImageViewer.jsx';
import {dispatchReplaceViewerItems, getMultiViewRoot, getViewer, IMAGE, NewPlotMode,} from '../MultiViewCntlr.js';
import {
    dispatchChangeActivePlotView, dispatchDeletePlotView,
    dispatchPlotImage,
    dispatchWcsMatch,
    visRoot,
    WcsMatchType
} from '../ImagePlotCntlr.js';
import {SORT_ASC, SortInfo, UNSORTED} from '../../tables/SortInfo.js';
import {CloseButton} from 'firefly/ui/CloseButton';
import {VisMiniToolbar} from './VisMiniToolbar.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {RadioGroupInputField} from '../../ui/RadioGroupInputField.jsx';
import {getActivePlotView, getPlotViewAry, getPlotViewById} from '../PlotViewUtil.js';
import {isImageDataRequestedEqual} from '../WebPlotRequest.js';
import {ValidationField} from '../../ui/ValidationField.jsx';
import Validate from '../../util/Validate.js';


const MAX_IMAGE_CNT= 7;
const IMAGE_CNT_KEY= 'imageCount';
const CUTOUT_SIZE= 'cutoutSize';


export function ImageTableRowViewer({viewerId, makeRequestFromRow, defaultCutoutSizeAS, tbl_id,
                                       defaultImageCnt= 5, imageExpandedMode, insideFlex=true,
                                        closeExpanded, maxImageCnt= MAX_IMAGE_CNT}) {


    const table= useStoreConnector(() => getTblById(tbl_id));
    const imageCnt= useFieldGroupValue(IMAGE_CNT_KEY,viewerId)[0]() ?? defaultImageCnt;
    const cutoutSize= useFieldGroupValue(CUTOUT_SIZE,viewerId)[0]() ?? defaultCutoutSizeAS;
    const {wcsMatchType, activePlotId}= useStoreConnector(() =>
        ({wcsMatchType: visRoot().wcsMatchType, activePlotId:visRoot().activePlotId}) );


    useEffect(() => {
        if (!table || table.isFetching || !makeRequestFromRow) return;
        layoutImages(viewerId, Number(cutoutSize), Number(imageCnt), table, makeRequestFromRow);
    }, [imageCnt, table, cutoutSize]);


    if (imageExpandedMode) {
        return (
            <MultiImageViewer
                {...{viewerId, Toolbar, insideFlex, closeFunc:closeExpanded, defaultImageCnt, maxImageCnt, tableId:tbl_id,
                    makeRequestFromRow,defaultCutoutSizeAS,
                    wcsMatchType, activePlotId,
                    showWhenExpanded:true, defaultDecoration:false}}/>
        );
    }
    return (
        <MultiImageViewer
            {...{viewerId, Toolbar, insideFlex, defaultImageCnt, maxImageCnt, tableId:tbl_id,
                makeRequestFromRow,defaultCutoutSizeAS,
                wcsMatchType, activePlotId,
                forceRowSize:1, canReceiveNewPlots: NewPlotMode.create_replace.key}}
        />
    );
}

ImageTableRowViewer.propTypes= {
    viewerId: string.isRequired,
    tbl_id: string.isRequired,
    defaultCutoutSizeAS: number,
    defaultImageCnt: number,
    maxImageCnt: number,
    imageExpandedMode: bool,
    insideFlex: bool,
    makeRequestFromRow: func.isRequired,
    closeExpanded: func,
};

const toolsStyle= {
    display:'flex',
    flexDirection:'row',
    flexWrap:'nowrap',
    alignItems: 'center',
    justifyContent:'space-between',
    height: 30
};

const tStyle= { display:'inline-block', whiteSpace: 'nowrap', minWidth: '3em', paddingLeft : 5, marginTop: -1 };
const closeButtonStyle= { display: 'inline-block', padding: '1px 12px 0 1px' };

function Toolbar({viewerId, tableId:tbl_id, closeFunc=null, maxImageCnt, defaultImageCnt, makeRequestFromRow,
                     wcsMatchType, activePlotId,
                     defaultCutoutSizeAS=500, minCutoutSize=50, maxCutoutSize=1000}) {


    const wcsMatch= (
        <div style={{alignSelf:'center', padding: '0 10px 0 25px', display:'flex', alignItems:'center'}}>
            <div style={{display:'inline-block'}}>
                <input style={{margin: 0}}
                       type='checkbox'
                       checked={wcsMatchType===WcsMatchType.Standard}
                       onChange={(ev) => doWcsMatch(ev.target.checked, activePlotId) }
                />
            </div>
            <div style={tStyle}>WCS Match</div>
        </div>
    );

    const options= [];
    for(let i= 1; (i<=maxImageCnt); i+=2) {
        options.push({label: i+'', value: i+''});
    }

    return (
        <FieldGroup groupKey={viewerId} keepState={true}>
            <div style={{...toolsStyle, marginBottom: closeFunc ? 3 : 0}}>
                {closeFunc &&<CloseButton style={closeButtonStyle} onClick={closeFunc}/>}
                <div style={{whiteSpace: 'nowrap', paddingLeft: 7, display:'flex', alignItems:'center'}}>
                    <div>Image Count:</div>
                    <div style={{display:'inline-block', paddingLeft:7}}>
                        <RadioGroupInputField {...{
                            options, fieldKey:IMAGE_CNT_KEY,
                            labelWidth:0, tooltip:'Choose number of images to show',
                            initialState:{value: defaultImageCnt+''} }} />
                    </div>
                </div>
                {defaultCutoutSizeAS && <ValidationField fieldKey={CUTOUT_SIZE}
                                 initialState= {{value: defaultCutoutSizeAS}}
                                 validator= {(v) =>  Validate.floatRange(minCutoutSize,maxCutoutSize,1,'cutout size',v,false)}
                                 tooltip='enter cutout size for the images'
                                 labelWidth={100}
                                 label= 'Cutout Size (arcsec):' />}
                <SortDirFeedback table={getTblById(tbl_id)}/>
                {wcsMatch}
                <VisMiniToolbar style={{width:350}}/>
            </div>
        </FieldGroup>
    );
}


function SortDirFeedback({table})  {
    if (!table?.request?.sortInfo) return <div/>;
    const sInfo = SortInfo.parse(table.request.sortInfo);
    if (sInfo.direction === UNSORTED) return <div/>;
    const dirStr= sInfo.direction===SORT_ASC ? 'ascending' : 'descending';
    return (
        <div>
            {`Sorted by column: ${sInfo.sortColumns.join(',')}  ${dirStr}`};
        </div>
    );
}


Toolbar.propTypes= {
    viewerId : string.isRequired,
    tableId: string.isRequired,
    closeFunc : func,
    defaultImageCnt: number,
    maxImageCnt: number,
    makeRequestFromRow: func.isRequired,
    defaultCutoutSizeAS: number,
    minCutoutSize: number,
    maxCutoutSize: number,
};


const wcsMatchTarget= (doWcsStandard, plotId) =>
    dispatchWcsMatch({matchType:doWcsStandard?WcsMatchType.Target:false, plotId});

const doWcsMatch= (doWcsStandard, plotId) =>
    dispatchWcsMatch({matchType:doWcsStandard?WcsMatchType.Standard:false, plotId});

//
// function changeSize(viewerId, value) {
//     value = Number(value);
//     dispatchChangeViewerLayout(viewerId, value === 1 ? SINGLE : GRID, {count: value});
// }
//


function layoutImages(viewerId, cutoutSize, imageCnt, table, makeRequestFromRow) {

    if (!table || isNil(table.highlightedRow) || (table?.totalRows??0) < 1) return;
    const viewer= getViewer(getMultiViewRoot(),viewerId);
    if (!viewer) return;

    const hasPlotsWhenBeginning = getPlotViewAry(visRoot())?.length > 0;
    const newPlotIdAry = makePlotIds(viewerId, table.highlightedRow, table.totalRows, imageCnt);

    const vr = visRoot();
    newPlotIdAry.forEach((plotId) => {
        const rowNum = getPlotIdRowNum(viewerId,plotId);
        const wpRequest= makeRequestFromRow(table.tbl_id, rowNum, cutoutSize); //todo - needs documentation
        if (!wpRequest) return;

        const pv = getPlotViewById(vr, plotId);
        if (!pv || !isImageDataRequestedEqual(pv?.request, wpRequest)) {
            wpRequest.setRotateNorth(true);
            dispatchPlotImage({
                plotId, wpRequest,
                setNewPlotAsActive: false,
                holdWcsMatch: true,
                pvOptions: {userCanDeletePlots: false}
            });
        }
    });

    if (xor(viewer.itemIdAry,newPlotIdAry).length>0) {
        dispatchReplaceViewerItems(viewerId, newPlotIdAry, IMAGE );
    }

    const newActivePlotId = makePlotId(viewerId,table.highlightedRow);
    if (getActivePlotView(visRoot())?.plotId !== newActivePlotId) {
        dispatchChangeActivePlotView(newActivePlotId);
    }

    if (!visRoot().wcsMatchType && hasPlotsWhenBeginning) { //todo: test to see if this is necessary
        dispatchWcsMatch({matchType: WcsMatchType.Standard, plotId: newActivePlotId});
    }

    const {mpwWcsPrimId} = visRoot();
    const root= plotIdRoot(viewerId);

    const keepPlotIdAry = makePlotIds(viewerId, table.highlightedRow, table.totalRows, MAX_IMAGE_CNT);
    getPlotViewAry(visRoot())
        .filter(({plotId}) => plotId.startsWith(root))
        .filter(({plotId}) => plotId !== mpwWcsPrimId)
        .filter(({plotId}) => !keepPlotIdAry.includes(plotId))
        .forEach(({plotId}) => dispatchDeletePlotView({plotId, holdWcsMatch: true}));
}

function makePlotIds(viewerId, highlightedRow, totalRows, totalPlots)  {
    const beforeCnt= totalPlots%2===0 ? totalPlots/2-1 : (totalPlots-1)/2;
    const afterCnt= totalPlots%2===0 ? totalPlots/2    : (totalPlots-1)/2;
    let endRow= Math.min(totalRows-1, highlightedRow+afterCnt);
    let startRow= Math.max(0,highlightedRow-beforeCnt);
    if (startRow===0) endRow= Math.min(totalRows-1, totalPlots-1);
    if (endRow===totalRows-1) startRow= Math.max(0, totalRows-totalPlots);

    const plotIds= [];
    for(let i= startRow; i<=endRow; i++) plotIds.push(makePlotId(viewerId,i));
    return plotIds;
}

const plotIdRoot= (viewerId) => 'image--'+viewerId;

const makePlotId= (viewerId, rowIdx) => plotIdRoot(viewerId)+'-'+rowIdx;

const getPlotIdRowNum= (viewerId, plotId) => Number(plotId.substring(plotIdRoot(viewerId).length));
