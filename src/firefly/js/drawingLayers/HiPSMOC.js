/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import Enum from 'enum';
import {debounce, get, set, isEmpty, cloneDeep, isString} from 'lodash';
import {makeDrawingDef, TextLocation, Style} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {primePlot, getDrawLayerById, getPlotViewIdListInOverlayGroup} from '../visualize/PlotViewUtil.js';
import {visRoot} from '../visualize/ImagePlotCntlr.js';
import DrawLayerCntlr, {DRAWING_LAYER_KEY, dispatchUpdateDrawLayer, dlRoot} from '../visualize/DrawLayerCntlr.js';
import MocObj, {createDrawObjsInMoc, setMocDisplayOrder, MocGroup} from '../visualize/draw/MocObj.js';
import {getUIComponent} from './HiPSMOCUI.jsx';
import ImagePlotCntlr from '../visualize/ImagePlotCntlr.js';
import {getMetaEntry, getTblById} from '../tables/TableUtil.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {MAX_ROW} from '../tables/TableRequestUtil.js';
import {dispatchAddTaskCount, dispatchRemoveTaskCount, getAppOptions, makeTaskId} from '../core/AppDataCntlr.js';
import {doFetchTable} from '../tables/TableUtil';
import {logger} from '../util/Logger.js';
import {dispatchModifyCustomField, getDlAry} from '../visualize/DrawLayerCntlr';
import {cloneRequest} from '../tables/TableRequestUtil';
import {dispatchAddActionWatcher} from '../core/MasterSaga';
import {MetaConst} from '../data/MetaConst';
import {getNextColor} from '../visualize/draw/DrawingDef';
import {rateOpacity} from '../util/Color.js';
import {CoordinateSys} from '../visualize/CoordSys.js';

const ID= 'MOC_PLOT';
const TYPE_ID= 'MOC_PLOT_TYPE';
const MocPrefix = 'MOC - ';
const factoryDef= makeFactoryDef(TYPE_ID, creator, null, getLayerChanges, onDetach, getUIComponent, null, asyncComputeDrawData);
export default {factoryDef, TYPE_ID};

let idCnt=0;
const colorList = ['green', 'cyan', 'magenta', 'orange', 'lime', 'red', 'blue', 'yellow'];
const colorN = colorList.length;
const LayerUpdateMethod = new Enum(['byEmptyAry', 'byTrueAry', 'none']);

const defColors={};


function onDetach(dl,action) {
    if (!dl?.updateStatusAry) return;
    Object.entries(dl.updateStatusAry).forEach( ([plotId,obj]) => {
        removeTask(plotId, obj.updateTaskId);
        obj.abortUpdate();
    });
}

function getVisiblePlotIdsByDrawlayerId(id, getState) {
    return getDrawLayerById(getState()[DRAWING_LAYER_KEY], id)?.visiblePlotIdAry ?? [];
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
            if (preloadedTbl){ //load by getting the full version of a already loaded table
                tReq= cloneRequest(preloadedTbl.request,
                    { startIdx : 0, pageSize : MAX_ROW, inclCols: mocFitsInfo.uniqColName });
            }
            else if (fitsPath) {       // load by getting file on server
                tReq = makeTblRequest('userCatalogFromFile', 'Table Upload',
                    {filePath: fitsPath, sourceFrom: 'isLocal'},
                    {tbl_id: mocFitsInfo.tbl_id, pageSize: MAX_ROW, inclCols: mocFitsInfo.uniqColName});
            }
            if (!tReq) return;

            const {plotIdAry=[]}= action.payload;
            plotIdAry.forEach( (plotId) => addTask(plotId, 'fetchMOC'));
            doFetchTable(tReq).then(
                (tableModel) => {
                    if (tableModel.tableData) {
                        dispatchModifyCustomField(tbl_id, {mocTable:tableModel});
                        const visiblePlotIdAry =getVisiblePlotIdsByDrawlayerId(id, getState);
                        visiblePlotIdAry.forEach((pId) => {
                            dispatch({type: ImagePlotCntlr.ANY_REPLOT, payload: {plotId: pId}});
                        });
                        plotIdAry?.forEach( (plotId) => removeTask(plotId, 'fetchMOC'));
                    }
                }
            ).catch(
                (reason) => {
                    logger.error(`Failed to MOC table: ${reason}`, reason);
                    plotIdAry.forEach( (plotId) => removeTask(plotId, 'fetchMOC'));
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
                                     {textLoc: TextLocation.CENTER,
                                      canUseOptimization: true});
    idCnt++;
    const options= {
        canUseMouse:true,
        canHighlight:true,
        canUserChangeColor: ColorChangeType.DYNAMIC,
        hasPerPlotData: true,
        destroyWhenAllDetached: true,
        layersPanelLayoutId: initPayload.layersPanelLayoutId,
    };

    // const actionTypes = [DrawLayerCntlr.REGION_SELECT, TABLE_LOADED];
    const actionTypes = [DrawLayerCntlr.REGION_SELECT];
    const {mocFitsInfo={},color= getNextColor(), mocGroupDefColorId}= initPayload || {};
    const {tbl_id, tablePreloaded} = mocFitsInfo;
    const id =  tbl_id || (initPayload?.drawLayerId ?? `${ID}-${idCnt}`);


    if (color && mocGroupDefColorId) { // if mocGroupDefColorId is defined, treet color as fallback they might be replaced by a user pref
        if (!defColors[mocGroupDefColorId]) defColors[mocGroupDefColorId]= color;
    }

    const preloadedTbl= tablePreloaded && getTblById(tbl_id);
    drawingDef.color = preloadedTbl?.tableMeta?.[MetaConst.DEFAULT_COLOR] ?? defColors[mocGroupDefColorId] ?? color;
    const defStyle= getAppOptions().hips.mocDefaultStyle ?? 'outline';
    const inStyleStr= getMetaEntry(preloadedTbl, MetaConst.MOC_DEFAULT_STYLE, defStyle).toLowerCase();
    switch (inStyleStr) {
        case 'moc tile outline':
        case 'tile outline':
        case 'tile_outline':
            drawingDef.style= Style.STANDARD;
            break;
        case 'fill':
            drawingDef.style= Style.FILL;
            break;
        case 'outline':
        case 'destination_outline':
        default:
            drawingDef.style= Style.DESTINATION_OUTLINE;
            break;
    }

    const dl = DrawLayer.makeDrawLayer( id, TYPE_ID, get(initPayload, 'title', MocPrefix +id.replace('_moc', '')),
                                        options, drawingDef, actionTypes);

    dl.mocFitsInfo = mocFitsInfo;
    dl.mocTable= undefined;
    dl.rootTitle= dl.title;
    dl.mocGroupDefColorId= mocGroupDefColorId;

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
        this.updateTaskId = makeTaskId('moc');
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
            const tObj = getTitle(drawLayer, pIdAry, visible && !drawLayer.mocTable);
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
                const style = Style.get(fillStyle);

                set(mocStyle, [targetPlotId], style);
                return Object.assign({}, {mocStyle});
            }
            if (mocTable) {
                const getMocNuniqs = () => {
                    const {data} = get(mocTable, ['tableData']) || {};
                    return data.map((row) => row[0]);
                };
                const mocTiles = getMocNuniqs(mocTable);
                const mocCsys= getMetaEntry(mocTable,'COORDSYS')?.trim().toUpperCase().startsWith('G') ?
                    CoordinateSys.GALACTIC : CoordinateSys.EQ_J2000;
                const mocObj = createMocObj(drawLayer, mocTiles, mocCsys);
                return {mocTable, mocObj, mocCsys, title: getTitle(drawLayer, visiblePlotIdAry)};
            }
            break;

        case DrawLayerCntlr.CHANGE_VISIBILITY:
            if (action.payload.visible) {
                const pIdAry = plotIdAry ? plotIdAry :[plotId];
                return Object.assign({}, {title: getTitle(drawLayer, pIdAry, action.payload.visible && !drawLayer.mocTable)});
            }
            break;

        case DrawLayerCntlr.CHANGE_DRAWING_DEF:   // from color change
            const {color} = action.payload.drawingDef || {};
            const newMocObj = createMocObj(drawLayer, undefined, undefined);

            if (newMocObj && newMocObj.color!==color) {
                newMocObj.color = color;
                if (drawLayer.mocGroupDefColorId) defColors[drawLayer.mocGroupDefColorId]= color;
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
 * @param mocCsys
 * @returns {Object}
 */
function createMocObj(dl, moc_nuniq_nums = [], mocCsys= undefined) {
    const {mocObj, drawingDef} = dl;

    return mocObj ? cloneDeep(mocObj) : MocObj.make(moc_nuniq_nums, drawingDef, mocCsys);
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
        setTimeout( () => dispatchAddTaskCount(plotId, taskId) ,0);
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
    if (!plot?.viewDim) return;
    const updateStatus = updateStatusAry[plotId];

     if (isEmpty(updateStatus.newMocObj)) {    // find visible cells first
        const newMocObj = {...mocObj};

        newMocObj.mocGroup = MocGroup.copy(mocObj.mocGroup, plot);
        newMocObj.mocGroup.collectVisibleTilesFromMoc(plot, updateStatus.storedSidePoints);
        newMocObj.style = dl?.mocStyle?.[plotId] ?? dl.drawingDef?.style ?? Style.DESTINATION_OUTLINE;
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
             const moreObjs = createDrawObjsInMoc(updateStatus.newMocObj, plot, dl.mocCsys,
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
 * @param drawLayerId
 * @param plotId
 * @returns {Function}
 */
function makeUpdateDeferred(drawLayerId, plotId) {
    const dl = getDrawLayerById(dlRoot(), drawLayerId);
    if (!dl) return () => window.clearInterval(id);
    const {updateStatusAry} = dl;

    updateStatusAry[plotId].startUpdate();
    const {updateTaskId}= updateStatusAry[plotId];
    addTask(plotId, updateTaskId);

    const id = window.setInterval( () => {
        const updateDl= getDrawLayerById(dlRoot(), drawLayerId);
        if (updateDl?.plotIdAry.includes(plotId) && updateDl?.visiblePlotIdAry.includes(plotId)) {
            updateMocData(updateDl, plotId);
        }
        else {
            removeTask(plotId, updateTaskId);
            updateStatusAry[plotId].abortUpdate();
            window.clearInterval(id);
        }
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

    const newDrawLayer = {...drawLayer, drawData: dd};
    dispatchUpdateDrawLayer(newDrawLayer);
    return newDrawLayer;
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

        const newDl= updateDrawLayer(changeMocDrawingStyle(drawLayer,
                mocStyle?.[targetPlotId] ?? drawLayer.drawingDef?.style ?? Style.STANDARD,
                                    targetPlotId),
            drawLayer, targetPlotId);
        mocRedraw(newDl,action);
    } else if (action.type === ImagePlotCntlr.ANY_REPLOT) {
        mocRedraw(drawLayer,action);
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
        mocRedrawDebounce(drawLayer, action);
    }
}

function mocRedraw(drawLayer,action) {
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
            updateStatusAry[pId].setCanceler(makeUpdateDeferred(drawLayer.drawLayerId, pId));
        }
    });

}

const mocRedrawDebounce= debounce(mocRedraw,60);
