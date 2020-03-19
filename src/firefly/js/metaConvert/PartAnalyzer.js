/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {dpdtChart, dpdtChartTable, dpdtImage, dpdtTable, DPtypes, SHOW_CHART, SHOW_TABLE} from './DataProductsType';
import {FileAnalysisType} from '../data/FileAnalysis';
import {createChartActivate, createChartSingleRowArrayActivate, createTableActivate} from './converterUtils';
import {createSingleImageActivate} from './ImageDataProductsUtil';




// todo
// todo - PartAnalyzer will eventually need to have some injected analysis
//      - We should do this as we better understand the need
// todo




/**
 *
 * @param part
 * @param {WebPlotRequest} request
 * @param table
 * @param row
 * @param fileFormat
 * @param serverCacheFileKey
 * @param activateParams
 * @return {{imageSingleAxis: boolean,
 *           isImage: boolean,
 *           tableResult: DataProductsDisplayType|Array.<DataProductsDisplayType>|undefined,
 *           chartResult: DataProductsDisplayType|Array.<DataProductsDisplayType>|undefined}}
 */
export function analyzePart(part, request, table, row, fileFormat, serverCacheFileKey, activateParams) {

    const {type,desc, index}= part;
    const availableTypes= findAvailableTypesForAnalysisPart(part, fileFormat);

    const imageResult= availableTypes.includes(DPtypes.IMAGE) && type===FileAnalysisType.Image &&
            analyzeImageResult(part, request, table, row, fileFormat, serverCacheFileKey,desc,activateParams,index);

    const chartResult= availableTypes.includes(DPtypes.CHART) && analyzeChartResult(part, fileFormat, serverCacheFileKey,desc,activateParams,index);
    const tableResult=
        availableTypes.includes(DPtypes.TABLE) &&
        (!chartResult || chartResult.displayType!==DPtypes.CHART_TABLE) &&
        analyzeTableResult(part, fileFormat, serverCacheFileKey,desc,activateParams,index,chartResult);

    return {
        imageResult,
        // imageSingleAxis:availableTypes.includes(DPtypes.IMAGE_SNGLE_AXIS),
        tableResult,
        chartResult
    };
}

// todo - arrangeAnalysisMenu needs to have injection point for application specific analysis
/**
 * Arrange the order of the menu array so that is show the most important stuff on the top
 * @param menu
 * @param parts
 * @param fileFormat
 * @param dataTypeHint
 * @return {Array}
 */
export function arrangeAnalysisMenu(menu,parts,fileFormat, dataTypeHint) {
    const imageEntry= menu.find( (m) => m.displayType===DPtypes.IMAGE);
    if (imageEntry) {
        menu= menu.filter( (m) => m.displayType!==DPtypes.IMAGE);
    }

    switch (dataTypeHint) {
        case 'timeseries':
            imageEntry && menu.push(imageEntry);
            break;
        case 'spectrum':
            const chartList= menu.filter( (m) => m.displayType===DPtypes.CHART);
            const noCharts= menu.filter( (m) => m.displayType!==DPtypes.CHART);
            if (chartList.length) {
                menu= [...chartList,...noCharts];
                imageEntry && menu.push(imageEntry);
            }
            else {
                imageEntry && menu.unshift(imageEntry);
            }
            break;
        default:
            imageEntry && menu.unshift(imageEntry);
            break;
    }
    return menu;
}

// todo - findAvailableTypesForAnalysisPart needs to have injection point for application specific analysis
/**
 * Determine what type of displays are available for this part
 * @param part
 * @param fileFormat
 * @return {Array.<String>} return an array of DPTypes string const
 */
function findAvailableTypesForAnalysisPart(part, fileFormat) {
    const {type}= part;
    if (type===FileAnalysisType.HeaderOnly || type===FileAnalysisType.Unknown) return [];
    if (type!==FileAnalysisType.Image || fileFormat!=='FITS' || is1DImage(part)) return [DPtypes.CHART,DPtypes.TABLE];
    return (imageCouldBeTable(part)) ? [DPtypes.IMAGE,DPtypes.TABLE,DPtypes.CHART] : [DPtypes.IMAGE];
}


function imageCouldBeTable(part) {
    const naxis= getIntHeader('NAXIS',part,0);
    if (naxis<1) return false;
    const naxisAry= [];
    for(let i=0; i<naxis;i++) naxisAry[i]= getIntHeader(`NAXIS${i+1}`,part,0);
    if (naxis===1) return true;
    else if (naxis===2) return naxisAry[1]<=30;
    else {
        let couldBeTable= true;
        for(let i=2; (i<naxis);i++) if (naxisAry[i]>1) couldBeTable= false;
        return couldBeTable;
    }
}

function is1DImage(part) {
    const naxis= getIntHeader('NAXIS',part,0);
    if (naxis<1) return false;
    const naxisAry= [];
    for(let i=0; i<naxis;i++) naxisAry[i]= getIntHeader(`NAXIS${i+1}`,part,0);
    if (naxis===1) return true;
    let is1DImage= true;
    for(let i=1; (i<naxis);i++) if (naxisAry[i]>1) is1DImage= false;
    return is1DImage;
}


const C_COL1= ['index','wave'];
const C_COL2= ['flux','data','data1','data2'];


// todo - analyzeChartResult needs to have injection point for application specific analysis
/**
 *
 * @param part
 * @param fileFormat
 * @param serverCacheFileKey
 * @param title
 * @param activateParams
 * @param tbl_index
 * @return {DataProductsDisplayType}
 */
function analyzeChartResult(part, fileFormat, serverCacheFileKey, title, activateParams, tbl_index) {
    const imageAsTableColCnt= isImageAsTable(part,fileFormat) ? getImageAsTableColCount(part,fileFormat) : 0;
    let imageAsStr= '';
    if (imageAsTableColCnt>2) imageAsStr= 'image - show as ';
    else if (imageAsTableColCnt===2) imageAsStr= 'image - 1D - show as ';
    const ddTitleStr= fileFormat==='FITS' ? `HDU #${part.index} (${imageAsStr}table or chart) ${title}` : `Part #${part.index} ${title}`;
    const cNames= imageAsTableColCnt ? [] : getColumnNames(part,fileFormat);
    const rowsTotal= getRowCnt(part,fileFormat);
    let xCol;
    let yCol;
    if (imageAsTableColCnt) {
        for(let i=0; i<imageAsTableColCnt; i++) cNames.push(i===0?'naxis1_idx': `naxis1_data_${(i-1)}`);

        xCol= cNames[0];
        yCol= cNames[1];
    }
    else {
        if (!cNames || cNames.length<2) return;
        const xCol= cNames.find( (c) => C_COL1.includes(c.toLowerCase()));
        const yCol= cNames.find( (c) => C_COL2.includes(c.toLowerCase()));
        if (rowsTotal<1 || !xCol || !yCol) return;
    }


    if (rowsTotal===1) {
        return dpdtChartTable(ddTitleStr,
            createChartSingleRowArrayActivate(serverCacheFileKey,'Row 1 Chart',activateParams,xCol,yCol,0,tbl_index),
            undefined, {paIdx:tbl_index, charTableDefOption: SHOW_TABLE});
    }
    else if (imageAsTableColCnt===2) {
        cNames[1]= 'DataLine';
        return dpdtChartTable(ddTitleStr,
            createChartActivate(serverCacheFileKey,title,activateParams,cNames[0],cNames[1],tbl_index,cNames),
            undefined, {paIdx:tbl_index, chartTableDefOption: SHOW_CHART});

    }
    else {
        return dpdtChartTable(ddTitleStr,
                createChartActivate(serverCacheFileKey,title,activateParams,xCol,yCol,tbl_index,cNames),
                undefined, {paIdx:tbl_index, chartTableDefOption: SHOW_TABLE});
    }
}



/**
 *
 * @param part
 * @param fileFormat
 * @param serverCacheFileKey
 * @param title
 * @param {ActivateParams} activateParams
 * @param tbl_index
 * @return {DataProductsDisplayType}
 */
function analyzeTableResult(part, fileFormat, serverCacheFileKey,title, activateParams, tbl_index) {
    const ddTitleStr= fileFormat==='FITS' ? `HDU #${part.index} (table) ${title}` : `Part #${part.index} ${title}`;

    return dpdtTable(ddTitleStr, createTableActivate(serverCacheFileKey,title,activateParams,tbl_index,false),
        undefined, {paIdx:tbl_index});
}

function analyzeImageResult(part, request, table, row, fileFormat, serverCacheFileKey,title, activateParams, hduIdx) {
    const newReq= request.makeCopy();
    const {imageViewerId}= activateParams;
    newReq.setMultiImageExts(hduIdx+'');
    return dpdtImage(`HDU #${hduIdx} (image) ${title}`,
        createSingleImageActivate(newReq,imageViewerId,table.tbl_id,row),'image-'+0, {request:newReq});
}


export const getIntHeader= (header, part, def) => {
    const resultStr= getHeader(header,part);
    if (!resultStr) return def;
    const num= Number(resultStr);
    return isNaN(num) ? def : num;
};

function getHeadersThatMatch(header, part) {
    return part.details.tableData.data
        .filter( (row) => row[1].includes(header))
        .map( (row) => row[2]);
}

function getHeadersThatStartsWith(header, part) {
    return part.details.tableData.data
        .filter( (row) => row[1].startsWith(header))
        .map( (row) => row[2]);
}

export function getHeader(header, part) {
    const data=part?.details?.tableData?.data;
    if (!data) return undefined;
    const foundRow= part.details.tableData.data.find( (row) => row[1].toLowerCase()===header.toLowerCase());
    return foundRow && foundRow[2];
}



function getColumnNames(part, fileFormat) {
    if (fileFormat==='FITS') {
        const naxis= getIntHeader('NAXIS',part);
        if (naxis===1) {
            return ['index', 'data'];
        }
        else {
            const ttNamesAry= getHeadersThatStartsWith('TTYPE',part);
            if (ttNamesAry.length) return ttNamesAry;
            const naxis2= getIntHeader('NAXIS2',part,0);
            if (naxis2<=30) {
                let dynHeaderNames= ['index'];
                for(let i=0;i<naxis2;i++) dynHeaderNames.push('data'+i);
                return dynHeaderNames;
            }
        }
    }
    else {
        return part.details.tableData.data.map( (row) => row[0]);
    }
}

function isImageAsTable(part, fileFormat) {
    if (part.type!==FileAnalysisType.Image) return false;
    if (fileFormat !== 'FITS') return false;
    if (is1DImage(part)) return true;
    const naxis = getIntHeader('NAXIS', part);
    if (naxis!==2) return false;
    const naxis2= getIntHeader('NAXIS2',part,0);
    if (naxis2>30) return false;
    return true;
}

function getImageAsTableColCount(part, fileFormat) {
    if (part.type!==FileAnalysisType.Image) return 0;
    if (fileFormat !== 'FITS') return 0;
    if (is1DImage(part)) return 2;
    const naxis = getIntHeader('NAXIS', part);
    if (naxis!==2) return 0;
    const naxis2= getIntHeader('NAXIS2',part,0);
    if (naxis2>30) return 0;
    return naxis2+1;
}



function getRowCnt(part, fileFormat) {
    if (part.totalTableRows>0) return part.totalTableRows;
    if (fileFormat==='FITS') {
        const naxis2= getIntHeader('NAXIS2',part);
        const naxis1= getIntHeader('NAXIS1',part);
        const ttNamesAry= getHeadersThatStartsWith('TTYPE',part);
        if (ttNamesAry.length) {
            return isNaN(naxis2) ? -1 : naxis2;
        }
        else {
            return isNaN(naxis1) ? -1 : naxis1;
        }
    }
}

