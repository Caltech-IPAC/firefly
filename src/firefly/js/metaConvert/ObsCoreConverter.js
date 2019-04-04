import {get} from 'lodash';
import {WebPlotRequest, TitleOptions} from '../visualize/WebPlotRequest.js';
import {getCellValue, doFetchTable} from '../tables/TableUtil.js';
import {getObsCoreAccessURL, getObsCoreProdType} from '../util/VOAnalyzer.js';
import {
    findTableCenterColumns,
    getDataLinkAccessUrls,
    getObsCoreProdTypeCol,
    isFormatDataLink,
    isFormatVoTable
} from '../util/VOAnalyzer';
import {makeFileRequest} from '../tables/TableRequestUtil';
import {ZoomType} from '../visualize/ZoomType.js';
import {makeWorldPt} from '../visualize/Point.js';

const GIG= 1048576 * 1024;

export function makeObsCoreConverter(table, converterTemplate) {
    const ret= {...converterTemplate};
    ret.hasRelatedBands= false;

    const propTypeCol= getObsCoreProdTypeCol(table);
    if (propTypeCol.enumVals) {
        const pTypes= propTypeCol.enumVals.split(',');
        if (pTypes.every( (s) => s.toLocaleLowerCase()==='image' || s.toLocaleLowerCase()==='cube')) {
            ret.canGrid= true;
            ret.maxPlots= 8;
            return ret;
        }
    }
    const filters= get(table, 'request.filters');
    if (filters) {
        const fList= filters.split(';');
        const pTFilter= fList.find( (f) => f.includes(propTypeCol.name) && f.includes('IN'));
        if (pTFilter) {
            const inList=  pTFilter.substring( pTFilter.indexOf('(')+1, pTFilter.indexOf(')')).split(',');
            if (inList.every( (s) => s.toLocaleLowerCase()==='\'image\'' || s.toLocaleLowerCase()==='\'cube\'')) {
                ret.canGrid= true;
                ret.maxPlots= 8;
                return ret;
            }
        }
    }

    ret.canGrid= false;
    ret.maxPlots= 1;

    return ret;
}

/**
 *  Support data the we don't know about
 * @param table
 * @param row
 * @param includeSingle
 * @param includeStandard
 * @return {{}}
 */
export function makeRequestForObsCore(table, row, includeSingle, includeStandard) {

    const dataSource= getObsCoreAccessURL(table,row);
    const prodType= (getObsCoreProdType(table,row) || '').toLocaleLowerCase();
    const canHandleProdType= prodType.includes('image') || prodType.includes('cube');
    const isVoTable= isFormatVoTable(table, row);
    const isDataLink= isFormatDataLink(table,row);

    if (!dataSource || !canHandleProdType || (isVoTable && !isDataLink)) return Promise.resolve({});

    let obsCollect= getCellValue(table,row,'obs_collection') || '';
    const iName= getCellValue(table,row,'instrument_name') || '';
    const obsId= getCellValue(table,row,'obs_id') || '';
    if (obsCollect===iName) obsCollect= '';

    const titleStr= `${obsCollect?obsCollect+', ':''}${iName?iName+', ':''}${obsId}`;

    const cen= findTableCenterColumns(table);
    const positionWP= cen && makeWorldPt(getCellValue(table,row,cen.lonCol), getCellValue(table,row,cen.latCol), cen.csys);

    if (isDataLink) {
        const tableReq= makeFileRequest('no title', dataSource, { startIdx : 0, pageSize : 1000});

        return doFetchTable(tableReq).then(
            (datalinkTable) => {
                const listOfUrls= getDataLinkAccessUrls(table, datalinkTable, 'fits', GIG);
                let idx= listOfUrls.findIndex( (item) => item.semantics.includes('this'));
                if (idx<0) idx= 0;
                return (listOfUrls.length) ? makeRequestForObsCoreItem(listOfUrls[idx].url,positionWP,titleStr,includeSingle,includeStandard) : {};
            }
        ).catch(
            (reason) => {
                console.warn(`Failed to catalog plot data: ${reason}`, reason);
            }
        );

    }
    else {
        return Promise.resolve(makeRequestForObsCoreItem(dataSource,positionWP,titleStr,includeSingle,includeStandard));
    }
}

function makeRequestForObsCoreItem(dataSource, positionWP, titleStr, includeSingle, includeStandard) {
    const retval= {};
    if (includeSingle) {
        retval.single= makeObsCoreRequest(dataSource,positionWP, titleStr);
    }

    if (includeStandard) {
        retval.standard= [makeObsCoreRequest(dataSource,positionWP, titleStr)];
        retval.highlightPlotId= retval.standard[0].getPlotId();
    }
    return retval;
}


function makeObsCoreRequest(dataSource, positionWP, titleStr) {
    if (!dataSource) return null;

    const r = WebPlotRequest.makeURLPlotRequest(dataSource, 'Fits Image');
    r.setZoomType(ZoomType.FULL_SCREEN);
    if (titleStr.length>7) {
        r.setTitleOptions(TitleOptions.NONE);
        r.setTitle(titleStr);
    }
    else {
        r.setTitleOptions(TitleOptions.FILE_NAME);
    }
    r.setPlotId(dataSource);
    if (positionWP) r.setOverlayPosition(positionWP);

    return r;
}


