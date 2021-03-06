/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import Enum from 'enum';
import {makeDrawingDef, TextLocation, Style} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {primePlot, getDrawLayerById, getPlotViewIdListInOverlayGroup} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import DrawLayerCntlr, {DRAWING_LAYER_KEY, dispatchUpdateDrawLayer} from '../visualize/DrawLayerCntlr.js';
import {get, set, isEmpty, cloneDeep, isString} from 'lodash';
import {clone} from '../util/WebUtil.js';
import MocObj, {createDrawObjsInMoc, setMocDisplayOrder, MocGroup} from '../visualize/draw/MocObj.js';
import {getUIComponent} from './HiPSMOCUI.jsx';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import {getTblById} from '../tables/TableUtil.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {MAX_ROW} from '../tables/TableRequestUtil.js';
import {dispatchAddTaskCount, dispatchRemoveTaskCount, makeTaskId } from '../core/AppDataCntlr.js';
import {doFetchTable} from '../tables/TableUtil';
import {logger} from '../util/Logger.js';
import {dispatchModifyCustomField, getDlAry} from '../visualize/DrawLayerCntlr';
import {cloneRequest} from '../tables/TableRequestUtil';
import {dispatchAddActionWatcher} from '../core/MasterSaga';
import {MetaConst} from '../data/MetaConst';
import {getNextColor} from '../visualize/draw/DrawingDef';
import {rateOpacity} from '../util/Color.js';

const ID= 'MOC_PLOT';
const TYPE_ID= 'MOC_PLOT_TYPE';
const MocPrefix = 'MOC - ';
const factoryDef= makeFactoryDef(TYPE_ID, creator, null, getLayerChanges, null, getUIComponent, null, asyncComputeDrawData);
export default {factoryDef, TYPE_ID};

let idCnt=0;
const colorList = ['green', 'cyan', 'magenta', 'orange', 'lime', 'red', 'blue', 'yellow'];
const colorN = colorList.length;
const LayerUpdateMethod = new Enum(['byEmptyAry', 'byTrueAry', 'none']);


function getVisiblePlotIdsByDrawlayerId(id, getState) {
    const dl = getDrawLayerById(getState()[DRAWING_LAYER_KEY], id);
    const {visiblePlotIdAry = []} = dl;

    return visiblePlotIdAry;
}

function loadMocFitsWatcher(action, cancelSelf, params, dispatch, getState) {
    const {id, mocFitsInfo}= params;
    if (action.payload.drawLayerId === id && (action.payload.visible)) {
        const {fitsPath,tbl_id,tablePreloaded} = mocFitsInfo || {};

        const dl = getDrawLayerById(getDlAry(), tbl_id);
        if (!dl) return;
        const preloadedTbl= tablePreloaded && getTblById(tbl_id);

        if (!dl.mocTable) { // moc table is not yet loaded
            let tReq;
            if (fitsPath) {       // load by getting file on server
                tReq = makeTblRequest('userCatalogFromFile', 'Table Upload',
                    {filePath: fitsPath, sourceFrom: 'isLocal'},
                    {tbl_id: mocFitsInfo.tbl_id, pageSize: MAX_ROW, inclCols: mocFitsInfo.uniqColName});
            }
            else if (preloadedTbl){ //load by getting the full version of a already loaded table
                tReq= cloneRequest(preloadedTbl.request,
                    { startIdx : 0, pageSize : MAX_ROW, inclCols: mocFitsInfo.uniqColName });
            }
            if (!tReq) return;

            doFetchTable(tReq).then(
                (tableModel) => {
                    if (tableModel.tableData) {
                        dispatchModifyCustomField(tbl_id, {mocTable:tableModel});
                        const visiblePlotIdAry =getVisiblePlotIdsByDrawlayerId(id, getState);
                        visiblePlotIdAry .forEach((pId) => {
                            dispatch({type: ImagePlotCntlr.ANY_REPLOT, payload: {plotId: pId}});
                        });
                    }
                }
            ).catch(
                (reason) => {
                    logger.error(`Failed to MOC table: ${reason}`, reason);
                }
            );

        } else {
            const vPlotIds =getVisiblePlotIdsByDrawlayerId(id, getState);
            vPlotIds.forEach((pId) => {
                dispatch({type: ImagePlotCntlr.ANY_REPLOT, payload: {plotId: pId}});
            });
        }
    }
}


/**
 * create region plot layer
 * @param initPayload moc_nuniq_nums, highlightedCell, selectMode
 * @return {DrawLayer}
 */
function creator(initPayload) {


    const drawingDef= makeDrawingDef(colorList[idCnt%colorN],
                                     {style: Style.STANDARD,
                                      textLoc: TextLocation.CENTER,
                                      canUseOptimization: true});


    idCnt++;
    const options= {
        canUseMouse:true,
        canHighlight:true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        hasPerPlotData: true,
        destroyWhenAllDetached: true
    };

    // const actionTypes = [DrawLayerCntlr.REGION_SELECT, TABLE_LOADED];
    const actionTypes = [DrawLayerCntlr.REGION_SELECT];
    const {mocFitsInfo={},color= getNextColor()}= initPayload || {};
    const {tbl_id, tablePreloaded} = mocFitsInfo;
    const id =  tbl_id || get(initPayload, 'drawLayerId', `${ID}-${idCnt}`);



    const preloadedTbl= tablePreloaded && getTblById(tbl_id);
    drawingDef.color = get(preloadedTbl, ['tableMeta',MetaConst.DEFAULT_COLOR], color);
    const style = preloadedTbl?.tableMeta?.[MetaConst.MOC_DEFAULT_STYLE] ?? 'outline';
    drawingDef.style = style === 'outline' ? Style.STANDARD : Style.FILL;



    const dl = DrawLayer.makeDrawLayer( id, TYPE_ID, get(initPayload, 'title', MocPrefix +id.replace('_moc', '')),
                                        options, drawingDef, actionTypes);

    dl.mocFitsInfo = mocFitsInfo;
    dl.mocTable= undefined;
    dl.rootTitle= dl.title;

    dispatchAddActionWatcher({
        callback:loadMocFitsWatcher,
        params: {id: dl.drawLayerId, mocFitsInfo},
        actions:[DrawLayerCntlr.CHANGE_VISIBILITY, DrawLayerCntlr.ATTACH_LAYER_TO_PLOT]
    });

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
    const {title, rootTitle} = dl;

    const tObj = isString(title) ? {} : Object.assign({}, title);
    pIdAry.forEach((pId) => tObj[pId] = rootTitle + (isLoading ? ' -- is loading' : ''));

    return tObj;
}


function showMessage(text, bShow = false) {
    if (bShow) {
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

            const {visible} = action.payload;
            const pIdAry = plotIdAry ? plotIdAry :[plotId];
            const tObj = getTitle(drawLayer, pIdAry, visible);
            const updateStatusAry = pIdAry.reduce((prev, pId) => {
                    if (!prev[pId]) {
                        prev[pId] = new UpdateStatus();
                    }
                    return prev;
            }, drawLayer.updateStatusAry || {});

            return {title: tObj, updateStatusAry};

        case DrawLayerCntlr.MODIFY_CUSTOM_FIELD:
            const {fillStyle, targetPlotId, mocTable} = action.payload.changes;

            if (fillStyle && targetPlotId) {
                const {mocStyle={}} = drawLayer;
                const style = fillStyle.includes('outline') ? Style.STANDARD : Style.FILL;

                set(mocStyle, [targetPlotId], style);
                return Object.assign({}, {mocStyle});
            }
            if (mocTable) {
                const getMocNuniqs = () => {
                    const {data} = get(mocTable, ['tableData']) || {};
                    return data.map((row) => row[0]);
                };
                const mocTiles = getMocNuniqs(mocTable);
                const mocObj = createMocObj(drawLayer, mocTiles);
                return {mocTable, mocObj, title: getTitle(drawLayer, visiblePlotIdAry)};
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

        case DrawLayerCntlr.CHANGE_DRAWING_DEF:   // from color change
            const {color} = action.payload.drawingDef || {};
            const newMocObj = createMocObj(drawLayer);

            if (newMocObj && newMocObj.color != color) {
                newMocObj.color = color;
                return {mocObj: newMocObj};
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

function changeMocDrawingColor(dl, pId) {

    const color = dl?.mocObj?.color ?? dl.drawingDef.color;
    const fillColor = rateOpacity(color, MocObj.PTILE_OPACITY_RATIO);

    const dObjs = get(dl.drawData, [DataTypes.DATA, pId],[]);
    return dObjs.map((oneObj) => {
        if (oneObj.fillColor && oneObj.fillColor != fillColor) {
            return {...oneObj, fillColor};
        } else {
            return oneObj;
        }
    });
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
    if (!plot) return;
    const updateStatus = updateStatusAry[plotId];

     if (isEmpty(updateStatus.newMocObj)) {    // find visible cells first
        const newMocObj = clone(mocObj);

        newMocObj.mocGroup = MocGroup.make(null, mocObj.mocGroup, plot);
        newMocObj.mocGroup.collectVisibleTilesFromMoc(plot, updateStatus.storedSidePoints);
        newMocObj.style = dl?.mocStyle?.[plotId] ?? dl.drawingDef?.style ?? style.STANDARD;
        updateStatus.newMocObj = newMocObj;
    } else if (updateStatus.newMocObj.mocGroup.isInCollection()) {
         const {mocGroup} = updateStatus.newMocObj;
         mocGroup.collectVisibleTilesFromMoc(plot,updateStatus.storedSidePoints, 20);

         if (!mocGroup.isInCollection()) {
             setMocDisplayOrder(updateStatus.newMocObj);
             updateStatus.totalTiles = get(updateStatus.newMocObj, ['allCells'], []).length;
         }
     } else {
         if (updateStatus.processedTiles.length < updateStatus.totalTiles || (updateStatus.totalTiles === 0)) {   // form drawObj
             const startIdx = updateStatus.processedTiles.length;
             const endIdx = updateStatus.processedTiles.length + updateStatus.maxChunk - 1;
             const moreObjs = createDrawObjsInMoc(updateStatus.newMocObj, plot,
                 startIdx, endIdx, updateStatus.storedSidePoints);  // handle max chunk
             updateStatus.processedTiles.push(...moreObjs);
             if (updateStatus.processedTiles.length >= updateStatus.totalTiles) {
                 abortUpdate(dl, updateStatusAry, plotId, LayerUpdateMethod.byTrueAry);
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
 * @param updateMethod
 */
function abortUpdate(dl, updateStatusAry, pId, updateMethod = LayerUpdateMethod.none) {
    //console.log('update method = ' + updateMethod.key);
    if (updateMethod === LayerUpdateMethod.byTrueAry) {
        const {processedTiles} = updateStatusAry[pId];
        updateDrawLayer(processedTiles, dl, pId);
    } else if (updateMethod === LayerUpdateMethod.byEmptyAry) {
        updateDrawLayer([], dl, pId);
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
                       DrawLayerCntlr.MODIFY_CUSTOM_FIELD, DrawLayerCntlr.CHANGE_DRAWING_DEF];
    if (!forAction.includes(action.type) || !drawLayer.mocObj) return;

    const {mocStyle={}} = drawLayer;
    if (action.type === DrawLayerCntlr.MODIFY_CUSTOM_FIELD) {
        const {fillStyle, targetPlotId} = action.payload.changes;
        if (!fillStyle || !targetPlotId) return;

        updateDrawLayer(changeMocDrawingStyle(drawLayer,
                                mocStyle?.[targetPlotId] ?? drawLayer.drawingDef?.style ?? Style.STANDARD,
                                    targetPlotId),
            drawLayer, targetPlotId);
    } else if (action.type === DrawLayerCntlr.CHANGE_DRAWING_DEF) {
        const {plotIdAry} = drawLayer;
        const dd = {...drawLayer.drawData};

        plotIdAry.forEach((pId) => {
            const newObjs = changeMocDrawingColor(drawLayer, pId);
            set(dd[DataTypes.DATA], [pId], newObjs);
        });

        const newDrawLayer = {...drawLayer, drawData: dd};
        dispatchUpdateDrawLayer(newDrawLayer);
    } else {
        const {plotId, plotIdAry} = action.payload;
        const {visiblePlotIdAry, updateStatusAry} = drawLayer;

        let pIdAry = [];
        if (plotIdAry) {
            pIdAry = plotIdAry;
        } else if (action.type === ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION ) {
            if (plotId) {
                pIdAry = getPlotViewIdListInOverlayGroup(visRoot(), plotId);
            }
        } else if (plotId) {
            pIdAry = [plotId];
        }

        pIdAry.forEach((pId) => {
            if (visiblePlotIdAry.includes(pId) && get(updateStatusAry, pId)) {
                const updateMethod = LayerUpdateMethod.none;

                abortUpdate(drawLayer, updateStatusAry, pId, updateMethod);
                updateStatusAry[pId].setCanceler(makeUpdateDeferred(drawLayer, pId));
            }
        });
    }
}