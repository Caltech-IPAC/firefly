/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get,  isArray, isUndefined, isEmpty} from 'lodash';
import {Style} from '../draw/DrawingDef.js';
import {primePlot, getDrawLayerById} from '../PlotViewUtil.js';
import {dispatchCreateDrawLayer, getDlAry, dispatchAttachLayerToPlot, dispatchModifyCustomField,
                        dispatchDestroyDrawLayer} from '../DrawLayerCntlr.js';
import {logError} from '../../util/WebUtil.js';
import {ImageLineBasedObj} from '../draw/ImageLineBasedObj.js';
import ImagePlotCntlr, {visRoot, dispatchPlotImage} from '../ImagePlotCntlr.js';
import {makeTblRequest, cloneRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {dispatchAddActionWatcher} from '../../core/MasterSaga.js';
import {getAViewFromMultiView, getMultiViewRoot, IMAGE} from '../MultiViewCntlr.js';
import WebPlotRequest from '../WebPlotRequest.js';
import {dispatchTableSearch, dispatchTableRemove, TABLE_LOADED, TABLE_SELECT,TABLE_HIGHLIGHT,TABLE_REMOVE,TABLE_UPDATE}
                from '../../tables/TablesCntlr.js';
import {getTblById, doFetchTable, getColumnIdx, getColumn} from '../../tables/TableUtil.js';
import LSSTFootprint from '../../drawingLayers/ImageLineBasedFootprint';
import {convertAngle} from '../VisUtil.js';


export const isLsstFootprintTable = (tableModel, fromAnalysis = false, tbl_idx = 0) => {
    const HEADER_KEY_COL = 1;
    const HEADER_VAL_COL = 2;
    const lsstKeys = ['contains_lsst_footprints', 'contains_lsst_measurements'];
    const {tableMeta} = tableModel || {};

    if (fromAnalysis && tableMeta) {      // check meta from analysis
        const metaInfo = JSON.parse(get(tableMeta, [tbl_idx]));
        const metaAry = metaInfo && get(metaInfo, ['tableData', 'data']);

        if (metaAry && isArray(metaAry)) {
            const contains = metaAry.reduce((prev, oneMeta) => {
                if (prev.length === lsstKeys.length) return prev;  // all keys are set
                if ((oneMeta.length > HEADER_VAL_COL) && lsstKeys.includes(oneMeta[HEADER_KEY_COL]) &&
                     oneMeta[HEADER_VAL_COL] === 'true' && (!prev.includes(oneMeta[HEADER_KEY_COL]))) {
                    prev.push(oneMeta[HEADER_VAL_COL]);
                }
                return prev;
            }, []);

            return (contains.length === lsstKeys.length);
        }
    } else if (tableMeta) {                                        // not from analysis summary table
        const filterRes = Object.keys(tableMeta).reduce((prev, oneKey) => {
            if (prev.length === lsstKeys.length) return prev;
            if (lsstKeys.includes(oneKey) && tableMeta[oneKey] === 'true' && !prev.includes(oneKey)) {
                prev.push(oneKey);
            }
            return prev;
        }, []);
        return (filterRes.length === lsstKeys.length);
    }
    return false;
};

// columns in footprint table
const spans = 'spans';
const peaks = 'peaks';
const corner1_x = 'footprint_corner1_x';
const corner1_y = 'footprint_corner1_y';
const corner2_x = 'footprint_corner2_x';
const corner2_y = 'footprint_corner2_y';
const footprintid = 'id';
const table_rowidx = 'ROW_IDX';
const ra_col = 'coord_ra';
const dec_col = 'coord_dec';

// column set
const hiddenColumns =[spans, peaks, corner1_x, corner1_y, corner2_x, corner2_y];
const tblIdxCols = [footprintid, table_rowidx];
const posCols = [ra_col, dec_col];

function getFileNameFromPath(filePath) {
    return filePath.split(/(\\|\/)/g).pop().replace('.', '_');
}

/**
 * create lsst footprint by checking if a footprint source catalog, an lsst image file or footprint data is send
 * - if an image file is set, load the image and then load the footprint file (table) if there is and get the footprint
 * data from the loaded file (table)
 * - if a footprint file is set, load the footpriont file (table) and then get the footprint data from the loaded table
 * - create the drawlayer per footprint data.
 * @param action
 * @returns {Function}
 */
export function imageLineBasedfootprintActionCreator(action) {
    return (dispatcher) => {
        const {footprintFile, footprintImageFile, footprintData, drawLayerId, tbl_index,
               title, style='fill', color, selectColor, highlightColor, showText, plotId} = action.payload;
        const fpParams = {title, style, color, selectColor, highlightColor, showText};
        let   imagePlotId = isArray(plotId) ? plotId[0] : plotId;
        let   tbl_id;

        if (!drawLayerId) {
            logError('no lsst drawlayer id specified');
            return;
        }

        if (!imagePlotId) {
            if (footprintImageFile) {
                imagePlotId = getFileNameFromPath(footprintImageFile);  // create a new plot
            } else {
                const pv = primePlot(visRoot());      // check the active plot
                imagePlotId = pv ? pv.plotId : null;
            }
        }

        if (!imagePlotId) {
            logError('no lsst image for footprint overlay');
            return;
        }

        const getFootprintData = (footprintFile, pId, tbl_index = 0) => {
            loadFootprintTable(footprintFile, pId, drawLayerId, tbl_index, title).then((tableModel) => {
                tbl_id = tableModel.tbl_id;
                return getFootprintDataFromTable(tableModel);
            }).then((fpData) => {
                initFootprint(fpData, drawLayerId, tbl_id, pId, fpParams);
            });
        };

        const footprintImageWatcher = (action, cancelSelf) => {
            const {plotId} = action.payload;

            if (plotId === imagePlotId) {
                if (footprintFile) {
                    getFootprintData(footprintFile, plotId, tbl_index);
                } else if (footprintData) {
                    initFootprint(footprintData, drawLayerId, tbl_id, plotId, fpParams);
                }
                if (cancelSelf) {
                    cancelSelf();
                }
            }
        };


        if (footprintImageFile) {
            dispatchAddActionWatcher({actions: [ImagePlotCntlr.PLOT_IMAGE], callback: footprintImageWatcher});
            loadFootprintImage(footprintImageFile, imagePlotId);
        } else if (footprintFile) {
            getFootprintData(footprintFile, imagePlotId, tbl_index);
        } else if (footprintData) {
            initFootprint(footprintData, drawLayerId, tbl_id, imagePlotId, fpParams);
        }
    };
}

function loadFootprintImage(imageFileOnServer, plotId) {
    const wpr = WebPlotRequest.makeFilePlotRequest(imageFileOnServer);
    const {viewerId=''} = getAViewFromMultiView(getMultiViewRoot(), IMAGE) || {};

    if (viewerId) {
        wpr.setPlotGroupId(viewerId);
        dispatchPlotImage({plotId, wpRequest: wpr, viewerId});
    }
}


function getFootprintDataFromTable(tableModel) {
    const allColumns = hiddenColumns.concat(posCols).concat(tblIdxCols);

    const inclCols = allColumns.map((col) => (`"${col}"`)).join(',');

    const params = {
        startIdx: 0,
        pageSize: MAX_ROW,
        inclCols
    };


    const req = cloneRequest(tableModel.request, params);
    req.tbl_id = `data-${tableModel.tbl_id}`;

    return doFetchTable(req).then(
        (footprintTblModel) => {

            const everyOtherData = (ary, numInGroup, type = 'integer') => {
                const numIdx = [...Array(numInGroup).keys()];
                const aryIdx = [...Array(Math.trunc(ary.length/numInGroup)).keys()];

                const newArray = aryIdx.reduce((prev, gIdx) => {
                    const newGroup = numIdx.reduce((prev, idx) => {
                        const v = ary[gIdx * numInGroup + idx];
                        prev.push((type === 'integer' ? parseInt(v) : parseFloat(v)));
                        return prev;
                    }, []);

                    prev.push(newGroup);
                    return prev;
                }, []);

                return newArray;
            };

            const {data} = get(footprintTblModel, ['tableData']);
            const footprintData = {pixelsys: 'zero-based', feet: {}};
            let   failCol = false;
            const colsIdxMap = allColumns.reduce((prev, colName) => {
                prev[colName] = getColumnIdx(footprintTblModel, colName);
                if (prev[colName] < 0) {
                    failCol = true;
                }
                return prev;
            }, {});

            const worldUnit = get(getColumn(footprintTblModel, ra_col), 'units', 'deg');

            if (!failCol && data) {
                data.reduce((prev, oneFootprint) => {
                    const id = oneFootprint[colsIdxMap[footprintid]];
                    const c1_x = parseInt(oneFootprint[colsIdxMap[corner1_x]]);
                    const c1_y = parseInt(oneFootprint[colsIdxMap[corner1_y]]);
                    const c2_x = parseInt(oneFootprint[colsIdxMap[corner2_x]]);
                    const c2_y = parseInt(oneFootprint[colsIdxMap[corner2_y]]);
                    const spansStr = oneFootprint[colsIdxMap[spans]];
                    const peaksStr = oneFootprint[colsIdxMap[peaks]];
                    const ra = convertAngle(worldUnit, 'deg', Number(oneFootprint[colsIdxMap[ra_col]]));
                    const dec = convertAngle(worldUnit, 'deg', Number(oneFootprint[colsIdxMap[dec_col]]));


                    // skip no spans case
                    if (c1_x >= 0 && c1_y >= 0 && c2_x >= 0 && c2_y >= 0 && spansStr && peaksStr && ra && dec) {
                        const corners = [[c1_x, c1_y], [c2_x, c1_y], [c2_x, c2_y], [c1_x, c2_y]];
                        const spans = everyOtherData(spansStr.replace(/\(|\)|,/g, '').split(' '), 3);
                        const peaks = everyOtherData(peaksStr.replace(/\(|\)|,/g, '').split(' '), 2, 'float');

                        prev[id] = {corners, spans, peaks, rowIdx: oneFootprint[colsIdxMap[table_rowidx]],
                                    ra, dec};
                    }
                    return prev;
                }, footprintData.feet);

                return footprintData;
            }
            return null;
        }
    ).catch(
        (reason) => {
            logError(`Failed to lsst footprint data: ${reason}`, reason);
        }
    );
}


function initFootprint(footprintData, drawLayerId, tbl_id, plotId, footprintParams) {
    const imageLineBasedFP = ImageLineBasedObj(footprintData);
    const {title, style, color, showText, selectColor, highlightColor} = footprintParams || {};
    const sourceTable = getTblById(tbl_id);
    const {highlightedRow, selectInfo, request, tableData} = sourceTable || {};

    if (!isEmpty(imageLineBasedFP)) {
        const dl = getDrawLayerById(getDlAry(), drawLayerId);
        if (!dl) {
            dispatchCreateDrawLayer(LSSTFootprint.TYPE_ID, {
                drawLayerId, title, imageLineBasedFP,
                color, showText, selectColor, highlightColor,
                highlightedRow, selectInfo, tbl_id,
                tableRequest: request,
                style: (style&&(style.toLowerCase() === 'fill')) ? Style.FILL : Style.STANDARD
            });
            if (plotId) {
                dispatchAttachLayerToPlot(drawLayerId, plotId, false);    // only one plot is attached
            }
        } else {
            dispatchModifyCustomField(drawLayerId,
                    {tableData, imageLineBasedFP,
                     title, style, color, showText, selectColor, highlightColor,
                     highlightedRow, selectInfo,  tableRequest: request, tbl_id}, plotId);
        }
    }
}

const footprintTableWatcher = (action, cancelself, params) => {
    const {tbl_id} = action.payload;
    const {plotId, drawLayerId, footprintTableId} = params || {};

    if (tbl_id !== footprintTableId) return;

    switch(action.type) {
        case TABLE_UPDATE:
             handleFootprintUpdate(tbl_id, drawLayerId, plotId);  // from filter, sort
            break;

        case TABLE_SELECT:
            dispatchModifyCustomField(drawLayerId, {selectInfo: action.payload.selectInfo}, plotId);
            break;

        case TABLE_HIGHLIGHT:
            dispatchModifyCustomField(drawLayerId, {highlightedRow:action.payload.highlightedRow}, plotId);
            break;

        case TABLE_REMOVE:
            dispatchDestroyDrawLayer(drawLayerId);
            if (cancelself) {
                cancelself();
            }
            break;

    }
};

function getTableId(dlId) {
    const tbl_id = dlId;
    return tbl_id;
}


function loadFootprintTable(footprintFileOnServer, plotId, drawLayerId, tbl_index, tableTitle) {

    return new Promise((resolve) => {
        const title = tableTitle || footprintFileOnServer.split(/(\\|\/)/g).pop();
        const hiddenColumnMeta = hiddenColumns.concat(['flags', 'footprint']).reduce( (prev, oneCol) => {
            const colKey = `col.${oneCol}.Visibility`;

            prev[colKey] = 'hidden';
            return prev;
        }, {});
        const footprintTableId = getTableId(drawLayerId);   // table Id is uniquely derived from drawLayerId
        const tbl = getTblById(footprintTableId);

        const tableRemoveWatcher = (action, canself) => {
            const {tbl_id} = action.payload;
            if (tbl_id !== footprintTableId) return;

            loadTable();
            if (canself) {
                canself();
            }

        };


        const loadTable = () => {
            const tblReq = makeTblRequest('userCatalogFromFile', title,
                {filePath: footprintFileOnServer},
                {
                    tbl_id: footprintTableId,
                    META_INFO: hiddenColumnMeta,
                    removable: true,
                    pageSize: 200
                });

            if (!isUndefined(tbl_index)) {
                tblReq.tbl_index = tbl_index;
            }

            const loadFootprintTableWatcher = (action, canself) => {
                const {tbl_id} = action.payload;
                if (tbl_id !== footprintTableId) return;

                const tableModel = getTblById(tbl_id);
                dispatchAddActionWatcher({
                    actions: [TABLE_SELECT, TABLE_HIGHLIGHT, TABLE_UPDATE, TABLE_REMOVE],
                    callback: footprintTableWatcher,
                    params: {plotId, drawLayerId, footprintTableId}
                });
                resolve(tableModel);
                if (canself) {
                    canself();
                }
            };

            dispatchAddActionWatcher({
                actions: [TABLE_LOADED],
                callback: loadFootprintTableWatcher,
                params: {plotId, drawLayerId, footprintTableId}
            });
            dispatchTableSearch(tblReq);
        };

        if (tbl) {
            dispatchAddActionWatcher({
                actions: [TABLE_REMOVE],
                callback: tableRemoveWatcher,
                params: {plotId, drawLayerId, footprintTableId}
            });
            dispatchTableRemove(footprintTableId);
        } else {
            loadTable();
        }

    });
}

function handleFootprintUpdate(tbl_id, drawLayerId, plotId) {
    const sourceTable = getTblById(tbl_id);

    getFootprintDataFromTable(sourceTable).then((fpData) => {
        initFootprint(fpData, drawLayerId, tbl_id, plotId, null);
    }).catch (
        (reason) => {
            logError(reason);
        }
    );
}

