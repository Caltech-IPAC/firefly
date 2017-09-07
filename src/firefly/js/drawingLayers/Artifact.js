/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {take} from 'redux-saga/effects';
import {get} from 'lodash';
import {isPlotIdInPvNewPlotInfoAry,
         isDrawLayerVisible, getDrawLayerById, getPlotViewById} from '../visualize/PlotViewUtil.js';
import {getRelatedDataById} from '../visualize/RelatedDataUtil.js';
import {clone, logError} from '../util/WebUtil.js';
import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr.js';
import PointDataObj, {DrawSymbol} from '../visualize/draw/PointDataObj.js';
import {makeDrawingDef, getNextColor} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {ColorChangeType} from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import DrawLayerCntlr, {dlRoot, dispatchModifyCustomField} from '../visualize/DrawLayerCntlr.js';
import {makeWorldPt} from '../visualize/Point.js';
import {doFetchTable} from '../tables/TableUtil.js';
import {getUIComponent} from './CatalogUI.jsx';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {getCenterColumns} from '../tables/TableInfoUtil.js';

const TYPE_ID= 'ARTIFACT_TYPE';

const factoryDef= makeFactoryDef(TYPE_ID,creator,null, getLayerChanges,null,getUIComponent);
export default {factoryDef, TYPE_ID}; // every draw layer must default export with factoryDef and TYPE_ID

const defSymbolMap= {
    pers_arti  : {color: 'orange', symbol: DrawSymbol.CROSS, size: 3},
    glint_arti : {color: 'purple', symbol: DrawSymbol.DIAMOND, size: 3},
    diff_spikes: {color: 'orange', symbol: DrawSymbol.DOT, size: 1},
    halos : {color: 'yellow', symbol: DrawSymbol.SQUARE, size: 3},
    ghost : {color: 'pink', symbol: DrawSymbol.DIAMOND, size: 3},
    latents : {color: 'green', symbol: DrawSymbol.X, size: 3},
    unknown  : {color: 'blue', symbol: DrawSymbol.X, size: 3},
};



function* watchReplot({plotId, drawLayerId}) {
    let keepChecking= true;
    while (keepChecking) {
        const action = yield take([ImagePlotCntlr.PLOT_IMAGE, ImagePlotCntlr.PLOT_IMAGE_FAIL,
                                 ImagePlotCntlr.DELETE_PLOT_VIEW, DrawLayerCntlr.DESTROY_DRAWING_LAYER]);
        const {payload}= action;
        switch (action.type) {
            case ImagePlotCntlr.PLOT_IMAGE:
                if (isPlotIdInPvNewPlotInfoAry(payload.pvNewPlotInfoAry, plotId)) {
                    updateArtifactTable(plotId, drawLayerId);
                }
                break;
            case ImagePlotCntlr.PLOT_IMAGE_FAIL:
                if (payload.plotId===plotId) {
                    dispatchModifyCustomField(drawLayerId, {tableModel:null});
                }
                break;
            case ImagePlotCntlr.DELETE_PLOT_VIEW:
                keepChecking= (payload.plotId!==plotId);
                break;
            case DrawLayerCntlr.DESTROY_DRAWING_LAYER:
                keepChecking= (payload.drawLayerId!==drawLayerId);
                break;
        }
    }
}




function updateArtifactTable(plotId, drawLayerId) {
    const dl= getDrawLayerById(dlRoot(), drawLayerId);
    const pv= getPlotViewById(visRoot(), plotId);
    if (!dl || !pv) return;
    if (isDrawLayerVisible(dl,plotId)) {
        retrieveArtifactsTable(dl);
    }
    else {
        dispatchModifyCustomField(drawLayerId, {tableModel:null});
    }
}



function retrieveArtifactsTable(drawLayer) {
    const pv= getPlotViewById(visRoot(), drawLayer.plotId);
    const rd= getRelatedDataById(pv, drawLayer.relatedDataId);

    if (!rd) return;

    const sendReq= clone(rd.searchParams,{ startIdx : 0, pageSize : 1000000 });

    doFetchTable(sendReq).then(
        (tableModel) => {
            if (tableModel.tableData && tableModel.tableData.data) {
                dispatchModifyCustomField(drawLayer.drawLayerId, {tableModel});
            }
        }
    ).catch(
        (reason) => {
            logError(`Failed retried artifact data: ${reason}`, reason);
        }
    );
}




//---------------------------------------------------------------------
//---------------------------------------------------------------------
//--- The following are functions are used to create and
//--- operate the drawing layer
//---------------------------------------------------------------------
//---------------------------------------------------------------------

function creator(initPayload, presetDefaults) {
    const {title, color, angleInRadian=false, relatedDataId, plotId, symbol, size}= initPayload;

    const pv= getPlotViewById(visRoot(), plotId);
    const rd= getRelatedDataById(pv, relatedDataId);

    const dataKey=  get(rd, 'dataKey');

    const symbolData= defSymbolMap[dataKey] || defSymbolMap['unknown'];

    const drawingDef= Object.assign(makeDrawingDef(),
        {
            size: size || symbolData.size || 5,
            symbol: DrawSymbol.get(symbol) || symbolData.symbol || DrawSymbol.SQUARE
        },
        presetDefaults);

    const options= {
        hasPerPlotData:false,
        isPointData:true,
        canUserDelete: true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        titleMatching: true,
        destroyWhenAllDetached :true
    };

    drawingDef.color= (color || symbolData.color || getNextColor());

    const dl= DrawLayer.makeDrawLayer(relatedDataId,TYPE_ID, title, options, drawingDef, null, null);
    dl.tableModel= null;
    dl.plotId= plotId;
    dl.relatedDataId= relatedDataId;
    dl.angleInRadian= angleInRadian;

    dispatchAddSaga(watchReplot, {drawLayerId:dl.drawLayerId, plotId});

    return dl;
}


function getLayerChanges(drawLayer, action) {

    switch (action.type) {
        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            const {tableModel}= action.payload.changes;
            return tableModel ? Object.assign({tableModel}, createDrawData(drawLayer, tableModel)) :
                                {tableModel:null, drawData: null};
            break;

        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
        case DrawLayerCntlr.CHANGE_VISIBILITY:
            if (action.payload.visible && !drawLayer.tableModel) {
                retrieveArtifactsTable(drawLayer); //start the server retrieve call
            }
            break;
    }
    return null;
}



function toAngle(d, radianToDegree)  {
    const v= Number(d);
    return (!isNaN(v) && radianToDegree) ? v*180/Math.PI : v;
}

function createDrawData(drawLayer, tableModel) {
    if (!tableModel) return;

    const {tableData}= tableModel;
    if (!tableData.data.length) return;

    const columns= getCenterColumns(tableModel);
    if (columns.lonIdx<0 || columns.latIdx<0) return null;

    const {angleInRadian:rad}= drawLayer;

    const data= tableData.data.map( (d) => {
        const wp= makeWorldPt( toAngle(d[columns.lonIdx],rad), toAngle(d[columns.latIdx],rad), columns.csys);
        return PointDataObj.make(wp);
    });
    return {drawData: {data}};
}
