/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {take} from 'redux-saga/effects';
import {makeDrawingDef, TextLocation, Style} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {primePlot, getDrawLayerById, getPlotViewIdListInGroup} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import DrawLayerCntlr, {DRAWING_LAYER_KEY, dispatchUpdateDrawLayer} from '../visualize/DrawLayerCntlr.js';
import {get, set, isEmpty, cloneDeep, isString} from 'lodash';
import {clone} from '../util/WebUtil.js';
import MocObj, {createDrawObjsInMoc, setMocDisplayOrder, MocGroup} from '../visualize/draw/MocObj.js';
import {getUIComponent} from './HiPSMOCUI.jsx';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import {dispatchAddSaga} from '../core/MasterSaga.js';
import {getTblById} from '../tables/TableUtil.js';
import {dispatchTableFetch, TABLE_LOADED} from '../tables/TablesCntlr.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {MAX_ROW} from '../tables/TableRequestUtil.js';
import {dispatchAddTaskCount, dispatchRemoveTaskCount, makeTaskId } from '../core/AppDataCntlr.js';

const ID= 'MOC_PLOT';
const TYPE_ID= 'MOC_PLOT_TYPE';
const MocPrefix = 'MOC - ';
const factoryDef= makeFactoryDef(TYPE_ID, creator, null, getLayerChanges, null, getUIComponent, asyncComputeDrawData);
export default {factoryDef, TYPE_ID};

let idCnt=0;
const colorList = ['green', 'cyan', 'magenta', 'orange', 'lime', 'red', 'blue', 'yellow'];
const colorN = colorList.length;


function getVisiblePlotIdsByDrawlayerId(id, getState) {
    const dl = getDrawLayerById(getState()[DRAWING_LAYER_KEY], id);
    const {visiblePlotIdAry = []} = dl;

    return visiblePlotIdAry;
}


function* loadMocFitsSaga({id, mocFitsInfo}, dispatch, getState) {
    while (true) {
        const action = yield take([DrawLayerCntlr.CHANGE_VISIBILITY, TABLE_LOADED]);

        switch (action.type) {
            case DrawLayerCntlr.CHANGE_VISIBILITY:
                if (action.payload.drawLayerId === id && action.payload.visible) {
                    const {fitsPath} = mocFitsInfo || {};

                    if (mocFitsInfo.tbl_id) {
                        const mocTable = getTblById(mocFitsInfo.tbl_id);
                        if (!mocTable && fitsPath) {       // moc fits not loaded yet
                            const tReq = makeTblRequest('userCatalogFromFile', 'Table Upload',
                                {filePath: fitsPath, sourceFrom: 'isLocal'},
                                {tbl_id: mocFitsInfo.tbl_id, pageSize: MAX_ROW});
                            dispatchTableFetch(tReq, 0);  // change to dispatchTableFetch later
                        } else {
                            const vPlotIds =getVisiblePlotIdsByDrawlayerId(id, getState);

                            vPlotIds.forEach((pId) => {
                                dispatch({type: ImagePlotCntlr.ANY_REPLOT, payload: {plotId: pId}});
                            });
                        }
                    }
                }
                break;
            case TABLE_LOADED:
                const {tbl_id} = action.payload;
                if (tbl_id === mocFitsInfo.tbl_id) {
                    const visiblePlotIdAry =getVisiblePlotIdsByDrawlayerId(id, getState);

                    visiblePlotIdAry .forEach((pId) => {
                        dispatch({type: ImagePlotCntlr.ANY_REPLOT, payload: {plotId: pId}});
                    });
                }
                break;
            default:
                break;
        }
    }

}


/**
 * create region plot layer
 * @param initPayload moc_nuniq_nums, highlightedCell, selectMode
 * @return {DrawLayer}
 */
function creator(initPayload) {

    const drawingDef= makeDrawingDef(colorList[idCnt%colorN], {style: Style.STANDARD});
    drawingDef.textLoc = TextLocation.CENTER;

    idCnt++;
    const options= {
        canUseMouse:true,
        canHighlight:true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        hasPerPlotData: true,
        destroyWhenAllDetached: true
    };

    const actionTypes = [DrawLayerCntlr.REGION_SELECT, TABLE_LOADED];
    const mocFitsInfo = get(initPayload, 'mocFitsInfo') || {};
    const {tbl_id} = mocFitsInfo;
    const id =  tbl_id || get(initPayload, 'drawLayerId', `${ID}-${idCnt}`);
    const dl = DrawLayer.makeDrawLayer( id, TYPE_ID, get(initPayload, 'title', MocPrefix +id.replace('_moc', '')),
                                        options, drawingDef, actionTypes);

    dl.mocFitsInfo = mocFitsInfo;
    dispatchAddSaga(loadMocFitsSaga, {id: dl.drawLayerId, mocFitsInfo});

    return dl;
}

class UpdateStatus {
    constructor(maxChunk=500) {
        this.done = false;               // when total is the same as processed
        this.maxChunk = maxChunk;        // set once
        // when setInterval starts
        this.newMocObj = null;           // set when new setInterval starts
        this.totalTiles = 0;             // set once when visible cells are calculated
        this.processedTiles = [];        // keep update in each setInterval execution
        this.updateCanceler = null;      // set when new setInterval starts
        this.storedSidePoints = {};
        this.updateTaskId = '';
    }

    startUpdate() {
        this.done = false;
        this.newMocObj = null;
        this.totalTiles = 0;
        this.processedTiles = [];
        this.updateTaskId = makeTaskId();
    }

    abortUpdate() {
        if (this.updateCanceler) {
            this.updateCanceler();
            this.updateCanceler = null;
        }
        this.updateTaskId = '';
        this.done = false;
        if (this.newMocObj) {
            this.newMocObj.mocGroup = null;
        }
        this.newMocObj = null;
        const total = Object.keys(this.storedSidePoints).reduce((prev, order) => {
            prev += Object.keys(get(this.storedSidePoints, [order], {})).length;
            return prev;
        }, 0);
        if (total > 8000) {
            this.storedSidePoints = {};
        }
        this.totalTiles = 0;
        this.processedTiles = [];
    }

    setCanceler(canceler) {
        this.updateCanceler = canceler;
    }
}

function getTitle(dl, pIdAry, isLoading=false) {
    const {drawLayerId, title} = dl;
    const mTitle = MocPrefix + drawLayerId.replace('_moc', '');

    const tObj = isString(title) ? {} : Object.assign({}, title);
    pIdAry.forEach((pId) => tObj[pId] = mTitle + (isLoading ? ' -- is loading' : ''));

    return tObj;
}


function showMessage(text, bShow = false) {
    const isShow = bShow;

    if (isShow) {
        console.log(text);
    }
}
/**
 * state update on the drawlayer change
 * @param drawLayer
 * @param action
 * @returns {*}
 */
function getLayerChanges(drawLayer, action) {
    const {drawLayerId, plotId, plotIdAry} = action.payload;
    const {visiblePlotIdAry=[], mocFitsInfo} = drawLayer;

    if (drawLayerId && drawLayerId !== drawLayer.drawLayerId) return null;

    switch (action.type) {
        case DrawLayerCntlr.ATTACH_LAYER_TO_PLOT:
            if (!plotIdAry && !plotId) return null;

            const pIdAry = plotIdAry ? plotIdAry :[plotId];
            const tObj = getTitle(drawLayer, pIdAry);
            const updateStatusAry = pIdAry.reduce((prev, pId) => {
                    if (!prev[pId]) {
                        prev[pId] = new UpdateStatus();
                    }
                    return prev;
            }, drawLayer.updateStatusAry || {});

            return {title: tObj, updateStatusAry};

        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            const {fillStyle, targetPlotId} = action.payload.changes;

            if (fillStyle && targetPlotId) {
                const {mocStyle={}} = drawLayer;
                const style = fillStyle.includes('outline') ? Style.STANDARD : Style.FILL;

                set(mocStyle, [targetPlotId], style);
                return Object.assign({}, {mocStyle});
            }
            break;

        case TABLE_LOADED:
            const {tbl_id} = action.payload;
            const getMocNuniqs = (mocTable) => {
                const {data} = get(mocTable, ['tableData']) || {};
                return data.map((row) => row[0]);
            };

            if (tbl_id === mocFitsInfo.tbl_id)  {
                const mocTable = getTblById(tbl_id);
                if (mocTable) {                       // get nuniq set from moc table after table is loaded
                    const mocTiles = getMocNuniqs(mocTable);
                    const mocObj = createMocObj(drawLayer, mocTiles);
                    return Object.assign({}, {mocObj, title: getTitle(drawLayer, visiblePlotIdAry)});
                }
            }
            break;

        case DrawLayerCntlr.CHANGE_VISIBILITY:
            if (action.payload.visible) {
                if (!getTblById(mocFitsInfo.tbl_id)) {     // moc table is not loaded yet
                    const pIdAry = plotIdAry ? plotIdAry :[plotId];

                    return Object.assign({}, {title: getTitle(drawLayer, pIdAry, true)});
                }
            }
            break;
        default:
            return null;
    }
    return null;
}

/**
 * create MocObj base on cell nuniq numbers and the coordinate systems
 * @param dl
 * @param moc_nuniq_nums
 * @returns {Object}
 */
function createMocObj(dl, moc_nuniq_nums = []) {
    const {mocObj, drawingDef} = dl;

    return mocObj ? cloneDeep(mocObj) : MocObj.make(moc_nuniq_nums, drawingDef);
}


function changeMocDrawingStyle(dl, style, plotId) {
    const dObjs = get(dl.drawData, [DataTypes.DATA, plotId], []);

    return dObjs.map((oneObj) => Object.assign({}, oneObj, {style}));
}

function removeTask(plotId, taskId) {
    if (plotId && taskId) {
        setTimeout( () => dispatchRemoveTaskCount(plotId, taskId) ,0);
    }
}

function addTask(plotId, taskId) {
    if (plotId && taskId) {
        setTimeout( () => dispatchAddTaskCount(plotId, taskId, true) ,0);
    }
}


/**
 * update MOC draw data at specific intervals
 * @param dl
 * @param plotId
 */
function updateMocData(dl, plotId) {
    const {updateStatusAry, mocObj} = dl;
    const plot = primePlot(visRoot(), plotId);
    const updateStatus = updateStatusAry[plotId];

     if (isEmpty(updateStatus.newMocObj)) {    // find visible cells first
        const newMocObj = clone(mocObj);
        newMocObj.mocGroup = MocGroup.make(null, mocObj.mocGroup, plot);
        newMocObj.mocGroup.collectVisibleTilesFromMoc(plot, updateStatus.storedSidePoints);
        newMocObj.style = get(dl, ['mocStyle', plotId], Style.STANDARD);
        updateStatus.newMocObj = newMocObj;
    } else if (updateStatus.newMocObj.mocGroup.isInCollection()) {
         const {mocGroup} = updateStatus.newMocObj;
         mocGroup.collectVisibleTilesFromMoc(plot,updateStatus.storedSidePoints, 20);

         if (!mocGroup.isInCollection()) {
             setMocDisplayOrder(updateStatus.newMocObj);
             updateStatus.totalTiles = get(updateStatus.newMocObj, ['allCells'], []).length;
         }
     } else {
         if (updateStatus.processedTiles.length < updateStatus.totalTiles) {   // form drawObj
             const startIdx = updateStatus.processedTiles.length;
             const endIdx = updateStatus.processedTiles.length + updateStatus.maxChunk - 1;
             const moreObjs = createDrawObjsInMoc(updateStatus.newMocObj, plot,
                 startIdx, endIdx, updateStatus.storedSidePoints);  // handle max chunk
             updateStatus.processedTiles.push(...moreObjs);
             if (updateStatus.processedTiles.length >= updateStatus.totalTiles) {
                 abortUpdate(dl, updateStatusAry, plotId, true);
             }
         }
     }
}

/**
 * start producing draw data at specific intervals
 * @param dl
 * @param plotId
 * @returns {Function}
 */
function makeUpdateDeferred(dl, plotId) {
    const {updateStatusAry} = dl;

    updateStatusAry[plotId].startUpdate();
    addTask(plotId, updateStatusAry[plotId].updateTaskId);

    const id = window.setInterval( () => {
        updateMocData(dl, plotId);
    }, 0);

    return () => window.clearInterval(id);
}

/**
 * dispatch update drawlayer until all draw data is produced
 * @param drawObjAry
 * @param drawLayer
 * @param plotId
 */
function updateDrawLayer(drawObjAry, drawLayer, plotId) {
    const dd = Object.assign({}, drawLayer.drawData);
    set(dd[DataTypes.DATA], [plotId], drawObjAry);

    const newDrawLayer = clone(drawLayer, {drawData: dd});
    dispatchUpdateDrawLayer(newDrawLayer);
}


/**
 * abort the draw data update
 * @param dl
 * @param updateStatusAry
 * @param pId
 * @param updateLayer
 */
function abortUpdate(dl, updateStatusAry, pId, updateLayer=false) {
    if (updateLayer) {
        const {processedTiles} = updateStatusAry[pId];
        updateDrawLayer(processedTiles, dl, pId);
    }
    removeTask(pId, updateStatusAry[pId].updateTaskId);
    updateStatusAry[pId].abortUpdate();

}

/**
 * produce the draw data in async style
 * @param drawLayer
 * @param action
 */
function asyncComputeDrawData(drawLayer, action) {
    const forAction = [ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION, ImagePlotCntlr.ANY_REPLOT,
                       DrawLayerCntlr.MODIFY_CUSTOM_FIELD];
    if (!forAction.includes(action.type)) return;

    if (action.type === DrawLayerCntlr.MODIFY_CUSTOM_FIELD) {
        const {fillStyle, targetPlotId} = action.payload.changes;
        if (!fillStyle || !targetPlotId) return;

        const {mocStyle} = drawLayer;
        updateDrawLayer(changeMocDrawingStyle(drawLayer, get(mocStyle, [targetPlotId], Style.STANDARD), targetPlotId),
                        drawLayer, targetPlotId);
    } else {
        const {plotId, plotIdAry} = action.payload;
        const {visiblePlotIdAry, updateStatusAry} = drawLayer;

        let pIdAry = [];
        if (plotIdAry) {
            pIdAry = plotIdAry;
        } else if (action.type === ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION ) {
            if (plotId) {
                pIdAry = getPlotViewIdListInGroup(visRoot(), plotId);
            }
        } else if (plotId) {
            pIdAry = [plotId];
        }

        pIdAry.forEach((pId) => {
            if (visiblePlotIdAry.includes(pId) && get(updateStatusAry, pId)) {
                abortUpdate(drawLayer, updateStatusAry, pId);
                updateStatusAry[pId].setCanceler(makeUpdateDeferred(drawLayer, pId));
            }
        });
    }
}