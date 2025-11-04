/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import Enum from 'enum';
import {get, set, isEmpty, cloneDeep, isString} from 'lodash';
import {makeDrawingDef, TextLocation, Style} from '../visualize/draw/DrawingDef.js';
import DrawLayer, {DataTypes, ColorChangeType}  from '../visualize/draw/DrawLayer.js';
import {makeFactoryDef} from '../visualize/draw/DrawLayerFactory.js';
import {primePlot, getDrawLayerById, getPlotViewIdListInOverlayGroup, getFoV} from '../visualize/PlotViewUtil.js';
import DrawLayerCntlr, {DRAWING_LAYER_KEY, dispatchUpdateDrawLayer, dlRoot} from '../visualize/DrawLayerCntlr.js';
import MocObj, {createDrawObjsInMoc, setMocDisplayOrder, MocGroup} from '../visualize/draw/MocObj.js';
import {getUIComponent} from './HiPSMOCUI.jsx';
import ImagePlotCntlr, {visRoot} from '../visualize/ImagePlotCntlr.js';
import {getMetaEntry, getTblById} from '../tables/TableUtil.js';
import {makeTblRequest} from '../tables/TableRequestUtil.js';
import {MAX_ROW} from '../tables/TableRequestUtil.js';
import { dispatchAddWorkingTask, getAppOptions} from '../core/AppDataCntlr.js';
import {doFetchTable} from '../tables/TableUtil';
import {logger} from '../util/Logger.js';
import {dispatchModifyCustomField, getDlAry} from '../visualize/DrawLayerCntlr';
import {cloneRequest} from '../tables/TableRequestUtil';
import {dispatchAddActionWatcher} from '../core/MasterSaga';
import {MetaConst} from '../data/MetaConst';
import {getNextColor} from '../visualize/draw/DrawingDef';
import {rateOpacity, toRGBA, toRGBAString} from '../util/Color.js';
import {CoordinateSys} from '../visualize/CoordSys.js';

const ID= 'MOC_PLOT';
const TYPE_ID= 'MOC_PLOT_TYPE';
const MocPrefix = 'MOC - ';
const MIN_AUTO_FILL_DEPTH= 6;
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
        obj.abortUpdate();
    });
}

function getVisiblePlotIdsByDrawlayerId(id, getState) {
    return getDrawLayerById(getState()[DRAWING_LAYER_KEY], id)?.visiblePlotIdAry ?? [];
}

function loadMocFitsWatcher(action, cancelSelf, params, dispatch, getState) {
    const {id, mocFitsInfo}= params;
    let autoUsesOnlyOutline= false;
    if (action.payload.drawLayerId === id && (action.payload.visible)) {
        const {fitsPath,tbl_id,tablePreloaded} = mocFitsInfo || {};

        const dl = getDrawLayerById(getDlAry(), tbl_id);
        if (!dl) return;
        const preloadedTbl= tablePreloaded && getTblById(tbl_id);
        let filterObj= {};
        let maxDepth= undefined;
        if (dl.maxFetchDepth) {
            maxDepth= 4*(4**(dl.maxFetchDepth+1));
            filterObj= {filters : `"${mocFitsInfo.uniqColName}" < ${maxDepth}`};
        }
        if (!dl.mocTable) { // moc table is not yet loaded
            let tReq;
            if (preloadedTbl){ //load by getting the full version of a already loaded table

                      // in this case we may have 1 test row loaded. test to see if it is greater than the filter
                if (preloadedTbl.tableData.data[0][0] > maxDepth)  filterObj= {}; //abort filtering

                autoUsesOnlyOutline= (preloadedTbl.tableData.data[0][0]> 4*(4**(MIN_AUTO_FILL_DEPTH)));


                tReq= cloneRequest(preloadedTbl.request,
                    { startIdx : 0, pageSize : MAX_ROW, inclCols: mocFitsInfo.uniqColName, ...filterObj});
            }
            else if (fitsPath) {       // load by getting file on server

                tReq = makeTblRequest('userCatalogFromFile', 'Table Upload',
                    {
                        filePath: fitsPath, sourceFrom: 'isLocal',
                        ...filterObj,
                    },
                    {tbl_id: mocFitsInfo.tbl_id, pageSize: MAX_ROW, inclCols: mocFitsInfo.uniqColName});
            }
            if (!tReq) return;

            const {plotIdAry=[]}= action.payload;
            const fetchPromise= doFetchTable(tReq);
            plotIdAry.forEach( (plotId) => dispatchAddWorkingTask(plotId, fetchPromise));
            fetchPromise.then(
                (tableModel) => {
                    if (tableModel.tableData) {
                        dispatchModifyCustomField(tbl_id, {mocTable:tableModel,autoUsesOnlyOutline});
                        const visiblePlotIdAry =getVisiblePlotIdsByDrawlayerId(id, getState);
                        visiblePlotIdAry.forEach((pId) => {
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
    const defStyle= getAppOptions().hips.mocDefaultStyle ?? 'AUTO';
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
        case 'auto':
            drawingDef.style= Style.AUTO;
            break;
        case 'outline':
        case 'destination_outline':
        default:
            drawingDef.style= Style.DESTINATION_OUTLINE;
            break;
    }

    const dl = DrawLayer.makeDrawLayer( id, TYPE_ID, get(initPayload, 'title', MocPrefix +id.replace('_moc', '')),
                                        options, drawingDef, actionTypes);

    dl.requestedStyle= drawingDef.style;

    dl.mocFitsInfo = mocFitsInfo;
    dl.mocTable= undefined;
    dl.rootTitle= dl.title;
    dl.mocGroupDefColorId= mocGroupDefColorId;
    dl.maxFetchDepth= initPayload.maxFetchDepth;

    dispatchAddActionWatcher({
        callback:loadMocFitsWatcher,
        params: {id: dl.drawLayerId, mocFitsInfo},
        actions:[DrawLayerCntlr.CHANGE_VISIBILITY, DrawLayerCntlr.ATTACH_LAYER_TO_PLOT]
    });

    return dl;
}

class UpdateStatus {
    constructor(maxChunk=5000) {
        this.done = false;               // when total is the same as processed
        this.maxChunk = maxChunk;        // set once
        // when setInterval starts
        this.newMocObj = null;           // set when new setInterval starts
        this.totalTiles = 0;             // set once when visible cells are calculated
        this.processedTiles = [];        // keep update in each setInterval execution
        this.updateCanceler = null;      // set when new setInterval starts
        this.storedSidePoints = {};
    }

    startUpdate() {
        this.done = false;
        this.newMocObj = null;
        this.totalTiles = 0;
        this.processedTiles = [];
    }

    abortUpdate() {
        if (this.updateCanceler) {
            this.updateCanceler();
            this.updateCanceler = null;
        }
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
    const {visiblePlotIdAry=[], mocFitsInfo, autoUsesOnlyOutline=false} = drawLayer;

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
            const {fillStyle, targetPlotId, mocTable, autoUsesOnlyOutline=false} = action.payload.changes;

            if (fillStyle && targetPlotId) {

                const [r,g,b,alpha]= toRGBA(drawLayer.drawingDef.color);
                const {mocStyle={},drawingDef} = drawLayer;
                const requestedStyle= Style.get(fillStyle);
                const savedAlpha= requestedStyle!==Style.AUTO ? alpha : drawLayer.savedAlpha;
                const newDrawingDef= {...drawingDef};

                let newStyle;
                const newMocObj = {...drawLayer.mocObj};
                if (requestedStyle===Style.AUTO) {
                    const {style: s, color:newColor} = getAutoDrawStyle(primePlot(visRoot(),targetPlotId), drawingDef.color, autoUsesOnlyOutline);
                    newDrawingDef.color= newColor;
                    newMocObj.color= newColor;
                    newStyle= s;
                }
                else {
                    if (drawLayer.requestedStyle===Style.AUTO) newDrawingDef.color= toRGBAString([r,g,b,savedAlpha]);
                    newStyle= requestedStyle;
                }

                set(mocStyle, [targetPlotId], newStyle);
                return {mocStyle, drawingDef:newDrawingDef, savedAlpha, requestedStyle, mocObj:newMocObj};
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
                return {mocTable, mocObj, mocCsys, title: getTitle(drawLayer, visiblePlotIdAry), autoUsesOnlyOutline};
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
            const [r,g,b,savedAlpha]= toRGBA(color);
            if (!newMocObj) return;
            if (drawLayer.mocGroupDefColorId) defColors[drawLayer.mocGroupDefColorId]= color;
            if (drawLayer.requestedStyle===Style.AUTO) {
                const {color:newColor} = getAutoDrawStyle(primePlot(visRoot(),plotId ?? plotIdAry?.[0]), color, autoUsesOnlyOutline);
                newMocObj.color = newColor;
            }
            else {
                if (newMocObj.color===color) return;
                newMocObj.color = color;
            }
            const overrideDrawingDef= {...action.payload.drawingDef, color: newMocObj.color};
            return {mocObj: newMocObj, savedAlpha, drawingDef:overrideDrawingDef};
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
    const {mocObj, drawingDef, mocFitsInfo} = dl;

    return mocObj ? cloneDeep(mocObj) : MocObj.make(moc_nuniq_nums, drawingDef, mocCsys, mocFitsInfo);
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

/**
 * update MOC draw data at specific intervals
 * @param {DrawLayer} inDrawLayer
 * @param {String} plotId
 */
function updateMocData(inDrawLayer, plotId) {
    const {updateStatusAry, mocObj} = inDrawLayer;
    const plot = primePlot(visRoot(), plotId);
    if (!plot?.viewDim) return;
    const updateStatus = updateStatusAry[plotId];
    const dl= {...inDrawLayer};
    dl.drawingDef= {...dl.drawingDef};

    const inStyle= dl.requestedStyle ?? dl?.mocStyle?.[plotId] ?? dl.drawingDef?.style ?? Style.DESTINATION_OUTLINE;
    let style;
    if (inStyle===Style.AUTO) {
        const {style:s,color}=  getAutoDrawStyle(plot,dl.drawingDef.color,dl.autoUsesOnlyOutline);
        style= s;
        dl.drawingDef.color= color;
        mocObj.color= color;
    }
    else {
        style= inStyle;
    }


     if (isEmpty(updateStatus.newMocObj)) {    // find visible cells first
        const newMocObj = {...mocObj};

        newMocObj.mocGroup = MocGroup.copy(mocObj.mocGroup, plot);
        newMocObj.mocGroup.collectVisibleTilesFromMoc(plot, updateStatus.storedSidePoints);
        newMocObj.style = style;
        updateStatus.newMocObj = newMocObj;
    } else if (updateStatus.newMocObj.mocGroup.isInCollection()) {
         const {mocGroup} = updateStatus.newMocObj;
         // mocGroup.collectVisibleTilesFromMoc(plot,updateStatus.storedSidePoints, 20);
         mocGroup.collectVisibleTilesFromMoc(plot,updateStatus.storedSidePoints);

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
                 completeAsyncUpdate(dl, updateStatusAry, plotId, LayerUpdateMethod.byTrueAry);
             }
         }
     }
}

function getAutoDrawStyle(plot, color,autoUsesOnlyOutline=false) {
    const [r,g,b]= toRGBA(color);
    const fov= getFoV(plot);
    if (!fov) return {style:Style.FILL, color:toRGBAString([r,g,b,1])};
    if (autoUsesOnlyOutline || fov<30) return {style:Style.DESTINATION_OUTLINE, color:toRGBAString([r,g,b,1])};
    const alpha= getAlpha(fov);
    return {style:Style.FILL, color: toRGBAString([r,g,b,alpha])};
}

function getAlpha(fov, {minFov=25, maxFov=70, minAlpha=.01, maxAlpha=.7}={}) {
    const t = Math.min(Math.max((fov - minFov) / (maxFov - minFov), 0), 1); // normalized fov in [0,1]
    return minAlpha + t * (maxAlpha - minAlpha);
}

/**
 * start producing draw data at specific intervals
 * @param drawLayerId
 * @param plotId
 * @returns {Function}
 */
function makeUpdateDeferred(drawLayerId, plotId) {
    let id= undefined;
    let pResolve;
    const completedPromise= new Promise( (resolve) => pResolve = resolve );
    const dl = getDrawLayerById(dlRoot(), drawLayerId);

    const shutdown= () => {
        if (id) window.clearInterval(id);
        pResolve?.();
    };

    if (!dl) return {cancel:() => undefined};
    const {updateStatusAry} = dl;

    updateStatusAry[plotId].startUpdate();

    id = window.setInterval( () => {
        const updateDl= getDrawLayerById(dlRoot(), drawLayerId);
        if (updateDl?.plotIdAry.includes(plotId) && updateDl?.visiblePlotIdAry.includes(plotId)) {
            updateMocData(updateDl, plotId);
        }
        else {
            updateStatusAry[plotId].abortUpdate();
            shutdown();
        }
    }, 0);

    return {completedPromise,cancel:shutdown};
}

/**
 * dispatch update drawlayer until all draw data is produced
 * @param drawObjAry
 * @param drawLayer
 * @param plotId
 */
function updateDrawLayer(drawObjAry, drawLayer, plotId) {
    const drawData = {...drawLayer.drawData, data:{...drawLayer.drawData.data, [plotId]:drawObjAry}};
    const newDrawLayer = {...drawLayer, drawData};
    dispatchUpdateDrawLayer(newDrawLayer);
    return newDrawLayer;
}


/**
 * complete the async drow object computation and update the draw layer
 * @param dl - the droaw layer, if not defined then just end the async task
 * @param updateStatusAry
 * @param pId
 * @param updateMethod
 */
function completeAsyncUpdate(dl, updateStatusAry, pId, updateMethod = LayerUpdateMethod.none) {
    if (dl && (updateMethod===LayerUpdateMethod.byTrueAry || updateMethod===LayerUpdateMethod.byEmptyAry)) {
        const drawObjAry= LayerUpdateMethod.byTrueAry ? updateStatusAry[pId].processedTiles : [];
        updateDrawLayer(drawObjAry, dl, pId);
    }
    updateStatusAry[pId].abortUpdate();

}


function abortLastAsyncUpdateIfRunning(updateStatusAry, pId) {
    completeAsyncUpdate(undefined,updateStatusAry, pId);
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
        mocRedraw(drawLayer, action);
    }
}

function mocRedraw(drawLayer,action) {
    const {plotId, plotIdAry} = action.payload;
    const {visiblePlotIdAry, updateStatusAry={}} = drawLayer;

    const pIdAry= plotIdAry ?? action.type === ImagePlotCntlr.CHANGE_CENTER_OF_PROJECTION
        ? getPlotViewIdListInOverlayGroup(visRoot(), plotId)
        : plotId ? [plotId] : [];


    pIdAry.forEach((pId) => {
        if (visiblePlotIdAry.includes(pId) && updateStatusAry[pId]) {
            abortLastAsyncUpdateIfRunning(updateStatusAry,pId);
            const {cancel,completedPromise}= makeUpdateDeferred(drawLayer.drawLayerId, pId);
            updateStatusAry[pId].setCanceler(cancel);
            if (completedPromise) dispatchAddWorkingTask(plotId, completedPromise);
        }
    });

}
