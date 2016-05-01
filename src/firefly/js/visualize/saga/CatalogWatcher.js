/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {isEmpty, get} from 'lodash';
import {TABLE_NEW_LOADED,TABLE_SELECT,TABLE_HIGHLIGHT,TABLE_REMOVE,TABLE_UPDATE, TABLE_RESULTS_PATH} from '../../tables/TablesCntlr.js';
import {dispatchCreateDrawLayer,dispatchAttachLayerToPlot,dispatchDestroyDrawLayer, dispatchModifyCustomField} from '../DrawLayerCntlr.js';
import ImagePlotCntlr, {visRoot} from '../ImagePlotCntlr.js';
import {findTblById,doFetchTable} from '../../tables/TableUtil.js';
import {serializeDecimateInfo} from '../../tables/Decimate.js';
import {getDrawLayerById} from '../PlotViewUtil.js';
import {dlRoot} from '../DrawLayerCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import Catalog from '../../drawingLayers/Catalog.js';
import {CoordinateSys} from '../CoordSys.js';
import {logError} from '../../util/WebUtil.js';
import {flux} from '../../Firefly.js';


/**
 * this saga does the following:
 * <ul>
 *     <li>Waits until first fits image is plotted
 *     <li>loads all the table that are catalogs
 *     <li>Then loops:
 *     <ul>
 *         <li>waits for a table new table, update, highlight or select change and then updates the drawing layer
 *         <li>waits fora new plot and adds any catalog
 *     </ul>
 * </ul>
 */
export function* watchCatalogs() {


    yield take(ImagePlotCntlr.PLOT_IMAGE);

    const tableSpace= get(flux.getState(), TABLE_RESULTS_PATH);
    if (!isEmpty(tableSpace)) {
        Object.keys(tableSpace).forEach( (tbl_ui_id) => handleCatalogUpdate(get(tableSpace, [tbl_ui_id, 'tbl_id'])) );
    }


    while (true) {
        const action= yield take([TABLE_NEW_LOADED,TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_UPDATE,
                                  TABLE_REMOVE, ImagePlotCntlr.PLOT_IMAGE]);
        const {tbl_id}= action.payload;
        switch (action.type) {
            case TABLE_NEW_LOADED:
                handleCatalogUpdate(tbl_id);
                break;
            
            case TABLE_SELECT:
                dispatchModifyCustomField(tbl_id, {selectInfo:action.payload.selectInfo});
                break;
            
            case TABLE_HIGHLIGHT:
            case TABLE_UPDATE:
                dispatchModifyCustomField(tbl_id, {highlightedRow:action.payload.highlightedRow});
                break;
                
            case TABLE_REMOVE:
                dispatchDestroyDrawLayer(tbl_id);
                break;

            case ImagePlotCntlr.PLOT_IMAGE:
                attachToAllCatalogs(action.payload.plotId);
                break;
        }
    }
}


const isCName = (name) => (c) => c.name===name;

function handleCatalogUpdate(tbl_id) {
    const sourceTable= findTblById(tbl_id);

    
    const {tableMeta,totalRows,tableData, request, highlightedRow,selectInfo}= sourceTable;
    


    if (!totalRows ||
        !tableMeta[MetaConst.CATALOG_OVERLAY_TYPE] ||
        !tableMeta[MetaConst.CATALOG_COORD_COLS]) {
        return; 
    }

    const s = tableMeta[MetaConst.CATALOG_COORD_COLS].split(';');
    if (s.length!== 3) return;
    const columns= {
        lonCol: s[0],
        latCol: s[1],
        csys : CoordinateSys.parse(s[2])
    };

    if (!tableData.columns.find( isCName(columns.lonCol)) && !tableData.columns.find(isCName(columns.latCol))) {
        return;
    }

    const params= {
        startIdx : 0,
        pageSize : 1000000,
        inclCols : `${columns.lonCol},${columns.latCol}`
    };

    var dataTooBigForSelection= false;
    if (totalRows>5000) {
        params.decimate=  serializeDecimateInfo(columns.lonCol, columns.latCol, 10000);
        dataTooBigForSelection= true;
    }

    const req = Object.assign({}, sourceTable.request, params);
    req.tbl_id = tbl_id;

    doFetchTable(req).then(
        (tableModel) => {
            if (tableModel.tableData && tableModel.tableData.data) {
                updateDrawingLayer(tbl_id,
                    tableModel.tableData, tableModel.tableMeta,
                    request, highlightedRow, selectInfo, columns, dataTooBigForSelection);
            }
        }
    ).catch(
        (reason) => {
            logError(`Failed to catalog plot data: ${reason}`, reason);
        }
    );
}

function updateDrawingLayer(tbl_id, tableData, tableMeta, tableRequest, 
                            highlightedRow, selectInfo, columns, dataTooBigForSelection) {

    const plotIdAry= visRoot().plotViewAry
        .filter( (pv) => pv.options.acceptAutoLayers)
        .map( (pv) => pv.plotId);

    const dl= getDrawLayerById(dlRoot(),tbl_id);
    if (dl) { // update drawing layer
        dispatchModifyCustomField(tbl_id, {tableData, tableMeta, tableRequest, 
                                           highlightedRow, selectInfo, columns,
                                           dataTooBigForSelection});
    }
    else { // new drawing layer
        dispatchCreateDrawLayer(Catalog.TYPE_ID,
            {catalogId:tbl_id, tableData, tableMeta, tableRequest, highlightedRow, 
                                selectInfo, columns, dataTooBigForSelection});
        dispatchAttachLayerToPlot(tbl_id, plotIdAry);
    }
}


function attachToAllCatalogs(plotId) {
    dlRoot().drawLayerAry.forEach( (dl) => {
        if (dl.drawLayerTypeId===Catalog.TYPE_ID) {
            dispatchAttachLayerToPlot(dl.drawLayerId, plotId);
        }
    });
}


