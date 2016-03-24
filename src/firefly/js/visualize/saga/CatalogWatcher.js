/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {take} from 'redux-saga/effects';
import {isEmpty} from 'lodash';
import {TABLE_NEW,TABLE_SELECT,TABLE_HIGHLIGHT,TABLE_REMOVE,TABLE_SPACE_PATH} from '../../tables/TablesCntlr.js';
import {dispatchCreateDrawLayer,dispatchAttachLayerToPlot,dispatchDestroyDrawLayer, dispatchModifyCustomField} from '../DrawLayerCntlr.js';
import ImagePlotCntlr, {visRoot} from '../ImagePlotCntlr.js';
import {findTblById,doFetchTable} from '../../tables/TableUtil.js';
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

    const tableSpace= flux.getState()[TABLE_SPACE_PATH];
    if (!isEmpty(tableSpace)) {
        Object.keys(tableSpace).forEach( (tbl_id) => handleCatalogUpdate(tbl_id));
    }


    while (true) {
        const action= yield take([TABLE_NEW,TABLE_SELECT,TABLE_HIGHLIGHT, TABLE_REMOVE, ImagePlotCntlr.PLOT_IMAGE]);
        switch (action.type) {
            case TABLE_NEW:
                handleCatalogUpdate(action.payload.tbl_id);
                break;
            
            case TABLE_SELECT:
                console.log('todo: select:',action); //todo - next ticket
                break;
            
            case TABLE_HIGHLIGHT:
                const {tbl_id, highlightedRow}= action.payload;
                dispatchModifyCustomField(tbl_id, {highlightedRow});
                break;
                
            case TABLE_REMOVE:
                dispatchDestroyDrawLayer(action.payload.tbl_id);
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

    
    const {tableMeta,totalRows,tableData, request, highlightedRow}= sourceTable;
    


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


    const req = Object.assign({}, sourceTable.request, params);
    req.tbl_id = tbl_id;

    doFetchTable(req).then(
        (tableModel) => {
            if (tableModel.tableData && tableModel.tableData.data) {
                updateDrawingLayer(tbl_id,
                    tableModel.tableData, tableModel.tableMeta,
                    request, highlightedRow, columns);
            }
        }
    ).catch(
        (reason) => {
            logError(`Failed to catalog plot data: ${reason}`, reason);
        }
    );
}

function updateDrawingLayer(tbl_id, tableData, tableMeta, tableRequest, highlightedRow, columns) {

    const plotIdAry= visRoot().plotViewAry
        .filter( (pv) => pv.options.acceptAutoLayers)
        .map( (pv) => pv.plotId);

    const dl= getDrawLayerById(dlRoot(),tbl_id);
    if (dl) { // update drawing layer
        dispatchModifyCustomField(tbl_id, {tableData, tableMeta, tableRequest, highlightedRow, columns});
    }
    else { // new new
        dispatchCreateDrawLayer(Catalog.TYPE_ID,
            {catalogId:tbl_id, tableData, tableMeta, tableRequest, highlightedRow, columns});
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


