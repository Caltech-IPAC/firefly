/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isEmpty} from 'lodash';
import {dispatchAddActionWatcher} from 'firefly/core/MasterSaga.js';
import ImagePlotCntlr, {dispatchChangePrimePlot} from 'firefly/visualize/ImagePlotCntlr.js';
import {
    dispatchTableHighlight,
    TABLE_HIGHLIGHT,
    TABLE_LOADED,
    TABLE_REMOVE,
    TABLE_SELECT,
    TABLE_UPDATE
} from 'firefly/tables/TablesCntlr.js';
import {getCellValue, getColumnIdx, getMetaEntry, getTblById} from 'firefly/tables/TableUtil.js';
import {MetaConst} from 'firefly/data/MetaConst.js';
import {
    convertHDUIdxToImageIdx, getHDU, getHDUIndex, getImageCubeIdx, isImageCube, primePlot
} from 'firefly/visualize/PlotViewUtil.js';
import {makeImagePt, parseImagePt} from 'firefly/visualize/Point.js';
import {
    dispatchAttachLayerToPlot,
    dispatchCreateDrawLayer,
    dispatchDestroyDrawLayer
} from 'firefly/visualize/DrawLayerCntlr.js';
import SearchTarget from 'firefly/drawingLayers/SearchTarget.js';
import {findPlotViewUsingFitsPathMeta} from 'firefly/visualize/saga/CatalogWatcher.js';

let idCnt=0;

export function addZAxisExtractionWatcher(tbl_id) {
    idCnt++;
    dispatchAddActionWatcher( {
        id: `table-zaxis-watcher-${idCnt}--`+tbl_id,
        callback:zAxisExtractionTableWatcher,
        params:{tbl_id},
        actions:[TABLE_LOADED,TABLE_REMOVE,TABLE_SELECT,TABLE_HIGHLIGHT,TABLE_UPDATE]
    });
    dispatchAddActionWatcher( {
        id: 'plot-zaxis-watcher-${idCnt}--'+tbl_id,
        callback:zAxisExtractionPlotWatcher,
        params:{tbl_id},
        actions:[ImagePlotCntlr.CHANGE_PRIME_PLOT, TABLE_REMOVE]
    });
}


function zAxisExtractionPlotWatcher(action,cancelSelf,{tbl_id}) {
    if (action.type===TABLE_REMOVE) {
        if (tbl_id===action.payload.tbl_id) cancelSelf();
        return;
    }
    if (!isTargetCube(tbl_id)) return {tbl_id};
    const {table,plot}= getInfo(tbl_id);
    const cubeIdx= getImageCubeIdx(plot);
    if (cubeIdx!==table.highlightedRow) dispatchTableHighlight(tbl_id,cubeIdx,table.request);
    return {tbl_id};
}



function zAxisExtractionTableWatcher(action,cancelSelf,{tbl_id, drawLayerId=undefined, color, firstLoadComplete}) {
    if (action.payload.tbl_id!==tbl_id) return {tbl_id, drawLayerId, color};
    if (action.type===TABLE_REMOVE) {
        cancelSelf();
        drawLayerId && dispatchDestroyDrawLayer(drawLayerId);
        return;
    }
    const {type}= action;
    if ((type===TABLE_UPDATE || type===TABLE_LOADED) && !firstLoadComplete) {
        firstLoadComplete= true;
        const {table,plot}= getInfo(tbl_id);
        const cubeIdx= getImageCubeIdx(plot);
        dispatchTableHighlight(tbl_id,cubeIdx,table.request)
        return {tbl_id, drawLayerId, color, firstLoadComplete};
    }
    if (!isTargetCube(tbl_id)) return {tbl_id, drawLayerId, color, firstLoadComplete};
    const {table,pv,plot}= getInfo(tbl_id);

    if (!drawLayerId) {
        idCnt++;
        drawLayerId= `zaxis-drill-point-${idCnt}--`+tbl_id;
        const imPt= parseImagePt(getMetaEntry(table,MetaConst.FITS_IM_PT));
        const newDL = dispatchCreateDrawLayer(SearchTarget.TYPE_ID,
            {
                drawLayerId,
                searchTargetPoint: makeImagePt(Math.trunc(imPt.x)+.5, Math.trunc(imPt.y)+.5),
                titlePrefix: 'Extraction Point ',
                canUserDelete: true,
            });
        dispatchAttachLayerToPlot(newDL.drawLayerId, [plot.plotId], false);
    }

    const plane= getCellValue(table,table.highlightedRow,'plane');
    const primeIdx= convertHDUIdxToImageIdx(pv,getHDUIndex(pv,plot),plane-1);
    if (pv.primeIdx!==primeIdx) dispatchChangePrimePlot({plotId:plot.plotId,primeIdx});
    return {tbl_id, drawLayerId, color, firstLoadComplete};
}


function isTargetCube(tbl_id) {
    const {table,pv,plot,extractionType,hdus,hduNum}= getInfo(tbl_id);
    if (!pv || !plot|| !table || extractionType!=='z-axis') return;
    if (!isImageCube(plot)) return false;
    const imPt= parseImagePt(getMetaEntry(table,MetaConst.FITS_IM_PT));
    if (isEmpty(hdus) || !imPt || !Object.values(hdus).includes(hduNum)) return false;
    if (getColumnIdx(table, 'plane') <0)  return false;
    return true;
}

function getInfo(tbl_id) {
    const table= getTblById(tbl_id);
    const pv= findPlotViewUsingFitsPathMeta(table);
    if (!pv || !table || !getMetaEntry(table, MetaConst.FITS_EXTRACTION_TYPE)) return {};
    const plot= primePlot(pv);

    return {
        table,
        pv,
        plot,
        extractionType: getMetaEntry(table, MetaConst.FITS_EXTRACTION_TYPE),
        hdus: getHDUs(table),
        hduNum: getHDU(plot)
    };

}

function getHDUs(table) {
    const hduPairs= getMetaEntry(table,MetaConst.FITS_IMAGE_HDU);
    return hduPairs.split(';').reduce( (res,str) => {
        const [k,v]= str.split('=');
        res[k]=Number(v);
        return res;
    },{});
}