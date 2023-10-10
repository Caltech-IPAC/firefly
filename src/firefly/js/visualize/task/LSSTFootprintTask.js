/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {get,  isArray, isUndefined, isEmpty} from 'lodash';
import {findTableCenterColumns} from '../../voAnalyzer/TableAnalysis.js';
import {Style} from '../draw/DrawingDef.js';
import {primePlot, getDrawLayerById} from '../PlotViewUtil.js';
import {dispatchCreateDrawLayer, getDlAry, dispatchAttachLayerToPlot, dispatchModifyCustomField,
                        dispatchDestroyDrawLayer} from '../DrawLayerCntlr.js';
import {clone} from '../../util/WebUtil.js';
import {ImageLineBasedObj} from '../draw/ImageLineBasedObj.js';
import ImagePlotCntlr, {visRoot, dispatchPlotImage} from '../ImagePlotCntlr.js';
import {makeTblRequest, cloneRequest, MAX_ROW} from '../../tables/TableRequestUtil.js';
import {dispatchAddActionWatcher} from '../../core/MasterSaga.js';
import {getAViewFromMultiView, getMultiViewRoot, IMAGE} from '../MultiViewCntlr.js';
import WebPlotRequest from '../WebPlotRequest.js';
import {dispatchTableSearch, dispatchTableRemove, dispatchTableUpdate, TABLE_LOADED, TABLE_SELECT,
        TABLE_HIGHLIGHT, TABLE_UPDATE, TABLE_REMOVE, TABLE_SORT, TABLE_FILTER} from '../../tables/TablesCntlr.js';
import {getTblById, doFetchTable, getColumnIdx, getColumn} from '../../tables/TableUtil.js';
import LSSTFootprint from '../../drawingLayers/ImageLineBasedFootprint';
import {convertAngle, isAngleUnit} from '../VisUtil.js';
import {logger} from '../../util/Logger.js';


export const isLsstFootprintTable = (tableModel) => {
    const lsstKeys = ['contains_lsst_footprints', 'contains_lsst_measurements'];
    const {tableMeta} = tableModel || {};

    if (tableMeta) {                                        // not from analysis summary table
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
const footprintid = 0;
const spans = 5;
const peaks = 6;
const corner1_x = 1;
const corner1_y = 2;
const corner2_x = 3;
const corner2_y = 4;
const table_rowidx = 'ROW_IDX';
const table_rownum = 'ROW_NUM';
let   ra_col = '';
let   dec_col = '';
const footprintColumnNamesDefault = 'id;footprint_corner1_x;footprint_corner1_y;' +
                                    'footprint_corner2_x;footprint_corner2_y;spans;peaks';

// column set
const hiddenColumnsDisplay =[spans, peaks, corner1_x, corner1_y, corner2_x, corner2_y];
const tblIdxCols = [table_rowidx, table_rownum];

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
            logger.error('no lsst drawlayer id specified');
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
            logger.error('no lsst image for footprint overlay');
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


function assignLSSTFootprintColumnNames(tableModel) {
    const {FootPrintColumnNames=footprintColumnNamesDefault} = tableModel.tableMeta || {};

    return FootPrintColumnNames.split(';');
}

function getPixelSys(tableModel) {
      const {pixelsys='zero-based'} = tableModel.tableMeta || {};
      return pixelsys;
}

function hideTableColumns(hiddenColumns, tbl) {
    const {tbl_id, tableData} = tbl;
    const {columns} = tableData;

    if (!columns) return;

    const hCols = hiddenColumnsDisplay.reduce((prev, hIdx) => {
        if (hIdx >= 0 && hIdx < hiddenColumns.length) {
            prev.push(hiddenColumns[hIdx]);
        }
        return prev;
    }, ['flags', 'footprint']);

    const newColumns = columns.reduce((prev, col) => {
        const newCol = clone(col);
        if (hCols.includes(col.name)) {
            newCol.visibility = 'hidden';
        }
        prev.push(newCol);
        return prev;
    }, []);

    const newTbl = {tbl_id, tableData: {columns: newColumns}};

    dispatchTableUpdate(newTbl);
}

function getFootprintDataFromTable(tableModel) {
    const {data} = tableModel.tableData || {};

    if (!data)  {
        return Promise.resolve(null);
    }

    const centerCols = findTableCenterColumns(tableModel);
    const hiddenColumns = assignLSSTFootprintColumnNames(tableModel);

    hideTableColumns(hiddenColumns, tableModel);

    let centerSys = null;

    // valid world system and ra&dec columns will take the priority as the footprint center in world coordinate system
    // invalid world system and valid ra & dec column will take the priority as the footprint center in pixel system
    // if no valid ra & dec column, then the footprint center is set based on 4 corners of the footprint
    if (centerCols) {
        if (centerCols.lonCol && centerCols.latCol) {
            ra_col = centerCols.lonCol;
            dec_col = centerCols.latCol;
            centerSys = centerCols.csys;
        }
    }

    const pixelsys = getPixelSys(tableModel);
    const allColumns = hiddenColumns.concat(tblIdxCols);
    if (ra_col && dec_col) {
        allColumns.push(ra_col, dec_col);
    }

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
            const footprintData = {pixelsys, feet: {}};
            let   failCol = false;
            const colsIdxMap = allColumns.reduce((prev, colName) => {
                prev[colName] = getColumnIdx(footprintTblModel, colName);
                if (prev[colName] < 0) {
                    failCol = true;
                }
                return prev;
            }, {});

            const worldUnit = ra_col&&dec_col ? get(getColumn(footprintTblModel, ra_col), 'units', null) : null;

            if (!failCol && data) {
                data.reduce((prev, oneFootprint) => {
                    const id = oneFootprint[colsIdxMap[hiddenColumns[footprintid]]];
                    const c1_x = parseInt(oneFootprint[colsIdxMap[hiddenColumns[corner1_x]]]);
                    const c1_y = parseInt(oneFootprint[colsIdxMap[hiddenColumns[corner1_y]]]);
                    const c2_x = parseInt(oneFootprint[colsIdxMap[hiddenColumns[corner2_x]]]);
                    const c2_y = parseInt(oneFootprint[colsIdxMap[hiddenColumns[corner2_y]]]);
                    const spansStr = oneFootprint[colsIdxMap[hiddenColumns[spans]]];
                    const peaksStr = oneFootprint[colsIdxMap[hiddenColumns[peaks]]];
                    let   ra, dec;
                    if (ra_col && dec_col) {
                        ra = Number(oneFootprint[colsIdxMap[ra_col]]);
                        dec = Number(oneFootprint[colsIdxMap[dec_col]]);

                        if (worldUnit && isAngleUnit(worldUnit)) {
                            ra = convertAngle(worldUnit, 'deg', ra);
                            dec = convertAngle(worldUnit, 'deg', dec);
                        }
                    }

                    // skip no spans case
                    if (c1_x >= 0 && c1_y >= 0 && c2_x >= 0 && c2_y >= 0 && spansStr && peaksStr) {
                        const corners = [[c1_x, c1_y], [c2_x, c1_y], [c2_x, c2_y], [c1_x, c2_y]];
                        const spanSet = everyOtherData(spansStr, 3);
                        const peakSet = everyOtherData(peaksStr, 2, 'float');

                        prev[id] = {corners, spans: spanSet, peaks: peakSet, rowIdx: oneFootprint[colsIdxMap[table_rowidx]],
                                    rowNum: oneFootprint[colsIdxMap[table_rownum]],
                                    ra, dec, centerSys};
                    }
                    return prev;
                }, footprintData.feet);

                return footprintData;
            }
            return null;
        }
    ).catch(
        (reason) => {
            logger.error(`Failed to lsst footprint data: ${reason}`, reason);
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
    const {request} = action.payload;
    const tbl_id = action.payload.tbl_id || request.tbl_id;
    const {plotId, drawLayerId, footprintTableId} = params || {};

    if (tbl_id !== footprintTableId) return;

    switch(action.type) {
        case TABLE_SORT:
        case TABLE_FILTER:
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
        const footprintTableId = getTableId(drawLayerId);   // table Id is uniquely derived from drawLayerId
        const tbl = getTblById(footprintTableId);

        const loadTable = () => {
            const tblReq = makeTblRequest('userCatalogFromFile', title,
                {filePath: footprintFileOnServer},
                {
                    tbl_id: footprintTableId,
                    removable: true,
                    pageSize: 50
                });

            if (!isUndefined(tbl_index)) {
                tblReq.tbl_index = tbl_index;
            }

            const loadFootprintTableWatcher = (action, canself) => {
                const {tbl_id} = action.payload;
                if (tbl_id !== footprintTableId) return;

                const tableModel = getTblById(tbl_id);
                if (get(tableModel, ['tableData', 'data'])) {
                    dispatchAddActionWatcher({
                        actions: [TABLE_SELECT, TABLE_HIGHLIGHT, TABLE_REMOVE, TABLE_SORT, TABLE_FILTER],
                        callback: footprintTableWatcher,
                        params: {plotId, drawLayerId, footprintTableId}
                    });
                    resolve(tableModel);
                    if (canself) {
                        canself();
                    }
                }
            };

            dispatchTableSearch(tblReq);
            dispatchAddActionWatcher({
                actions: [TABLE_LOADED, TABLE_UPDATE],
                callback: loadFootprintTableWatcher,
                removable: true
            });
        };

        if (tbl) {
            dispatchTableRemove(footprintTableId);
        }
        loadTable();

    });
}

function handleFootprintUpdate(tbl_id, drawLayerId, plotId) {
    const sourceTable = getTblById(tbl_id);

    getFootprintDataFromTable(sourceTable).then((fpData) => {
        initFootprint(fpData, drawLayerId, tbl_id, plotId, null);
    }).catch (
        (reason) => {
            logger.error(reason);
        }
    );
}

