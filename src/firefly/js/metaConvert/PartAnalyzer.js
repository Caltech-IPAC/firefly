/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isArray} from 'lodash';
import {dpdtChartTable, dpdtImage, dpdtTable, DPtypes, SHOW_CHART, SHOW_TABLE, AUTO} from './DataProductsType';
import {FileAnalysisType, Format, UIEntry, UIRender} from '../data/FileAnalysis';
import {RequestType} from '../visualize/RequestType.js';
import {TitleOptions} from '../visualize/WebPlotRequest';
import {createChartTableActivate, createChartSingleRowArrayActivate, createTableExtraction} from './converterUtils';
import {createSingleImageActivate, createSingleImageExtraction} from './ImageDataProductsUtil';
import {isEmpty} from 'lodash';

/**
 *
 * @param part
 * @param {WebPlotRequest} request
 * @param table
 * @param row
 * @param fileFormat
 * @param serverCacheFileKey
 * @param activateParams
 * @return {{tableResult: DataProductsDisplayType|undefined, imageResult: DataProductsDisplayType|undefined}}
 */
export function analyzePart(part, request, table, row, fileFormat, serverCacheFileKey, activateParams) {

    const {type,desc, fileLocationIndex}= part;
    const availableTypes= findAvailableTypesForAnalysisPart(part, fileFormat);
    if (isEmpty(availableTypes)) return {imageResult:false, tableResult:false};

    const fileOnServer= (part.convertedFileName) ? part.convertedFileName : serverCacheFileKey;

    const imageResult= availableTypes.includes(DPtypes.IMAGE) && type===FileAnalysisType.Image &&
            analyzeImageResult(part, request, table, row, fileFormat, part.convertedFileName,desc,activateParams,fileLocationIndex);

    let tableResult= availableTypes.includes(DPtypes.CHART) &&
                   analyzeChartTableResult(false, part, fileFormat, fileOnServer,desc,activateParams,fileLocationIndex);
    if (!tableResult) {
        tableResult= availableTypes.includes(DPtypes.TABLE) &&
            analyzeChartTableResult(true, part, fileFormat, fileOnServer,desc,activateParams,fileLocationIndex);
    }
    return {imageResult, tableResult};
}

/**
 * Determine which entry should be the default
 * @param menu
 * @param parts
 * @param fileFormat
 * @param dataTypeHint
 * @return {Array}
 */
export function chooseDefaultEntry(menu,parts,fileFormat, dataTypeHint) {
    if (!menu || !menu.length) return undefined;
    let defIndex= menu.findIndex( (m) => m.requestDefault);
    if (defIndex > -1) return defIndex;

    switch (dataTypeHint) {
        case 'timeseries':
            defIndex= menu.find( (m) => m.displayType===DPtypes.CHART);
            break;
        case 'spectrum':
            defIndex= menu.find( (m) => m.displayType===DPtypes.CHART);
            break;
    }
    return defIndex > -1 ? defIndex : 0;
}


/**
 * Determine what type of displays are available for this part
 * @param part
 * @param fileFormat
 * @return {Array.<String>} return an array of DPTypes string const
 */
function findAvailableTypesForAnalysisPart(part, fileFormat) {
    const {type}= part;
    const naxis= getIntHeader('NAXIS',part,0);
    if (type===FileAnalysisType.HeaderOnly || type===FileAnalysisType.Unknown) return [];
    if (type!==FileAnalysisType.Image &&  fileFormat!=='FITS' &&  is1DImage(part) || type===FileAnalysisType.Table ) return [DPtypes.CHART,DPtypes.TABLE];
    if (type===FileAnalysisType.Image && naxis===1) {
        part.chartTableDefOption=SHOW_CHART;
        return [DPtypes.CHART,DPtypes.TABLE];
    }

    return (imageCouldBeTable(part)) ? [DPtypes.IMAGE,DPtypes.TABLE,DPtypes.CHART] : [DPtypes.IMAGE];
}


function imageCouldBeTable(part) {
    const naxis= getIntHeader('NAXIS',part,0);
    if (naxis<1) return false;
    if (naxis===1) return true;
    const naxisAry= [];
    for(let i=0; i<naxis;i++) naxisAry[i]= getIntHeader(`NAXIS${i+1}`,part,0);
    if (naxis===2) return naxisAry[1]<=30;
    else {
        let couldBeTable= true;
        for(let i=1; (i<naxis);i++) if (naxisAry[i]>1) couldBeTable= false;
        return couldBeTable;
    }
}

function is1DImage(part) {
    const naxis= getIntHeader('NAXIS',part,0);
    if (naxis<1) return false;
    if (naxis===1) return true;
    const naxisAry= [];
    for(let i=0; i<naxis;i++) naxisAry[i]= getIntHeader(`NAXIS${i+1}`,part,0);
    let is1DImage= true;
    for(let i=1; (i<naxis);i++) if (naxisAry[i]>1) is1DImage= false;
    return is1DImage;
}


const C_COL1= ['index','wave'];
const C_COL2= ['flux','data','data1','data2'];

const TS_C_COL1= ['mjd'];
const TS_C_COL2= ['mag'];

const SPACITAL_C_COL1= ['ra','lon', 'c_ra', 'ra1'];
const SPACITAL_C_COL2= ['dec','lat','c_dec','dec1'];

/**
 * Return a object that specifieds the xy chart columns, the column names, and the column units
 * Each property in the object is optional
 * xCol and yCol is return it an appropriate one if found
 * if table is a Fits image rendered as a table the cNames is always returned, cUnits is return if specified in the part
 * if the table is a FITS table or some other type of table then the cNames and cUnits is never returned.
 * @param title
 * @param part
 * @param fileFormat
 * @return {{xCol:string,yCol:string,cNames:Array.<String>,cUnits:Array.<String>}|{}}
 */
function getTableChartColInfo(title, part, fileFormat) {
    if (isImageAsTable(part,fileFormat)) {
        let cNames= [];
        const {tableColumnNames:overrideColNames, tableColumnUnits=[]}= part;
        const colCnt= getImageAsTableColCount(part,fileFormat);
        if (isArray(overrideColNames) && overrideColNames.length===colCnt) {
            cNames= overrideColNames;
        }
        else {
            for(let i=0; i<colCnt; i++) cNames.push(i===0?'naxis1_idx': `naxis1_data_${(i-1)}`);
            if (colCnt===2) cNames[1]= !title? 'pixel value':title;
        }
        const cUnits= cNames.length===tableColumnUnits.length ? tableColumnUnits : undefined;
        return {xCol:cNames[0],yCol:cNames[1],cNames,cUnits, connectPoints:false};
    }
    else {
        const tabColNames= getColumnNames(part,fileFormat);
        if (!tabColNames || tabColNames.length<2) return {};

        if (part.details?.tableMeta?.utype) return {useChartChooser:true};

        let xCol= tabColNames.find( (c) => C_COL1.includes(c.toLowerCase()));
        let yCol= tabColNames.find( (c) => C_COL2.includes(c.toLowerCase()));
        let connectPoints= true;
        if (!xCol || !yCol) {
            xCol= tabColNames.find( (c) => TS_C_COL1.includes(c.toLowerCase()));
            yCol= tabColNames.find( (c) => TS_C_COL2.includes(c.toLowerCase()));
            connectPoints= false;
        }
        const rowsTotal= getRowCnt(part,fileFormat);
        if (rowsTotal<1) return {};
        return {xCol,yCol, connectPoints, useChartChooser:true};
    }
}

/**
 *
 * @param {String} title
 * @param {FileAnalysisPart} part
 * @param fileFormat
 * @param tableOnly
 * @return {string}
 */
function getTableDropTitleStr(title,part,fileFormat,tableOnly) {
    if (!title) title='';
    if (part.interpretedData) return title;
    if (fileFormat==='FITS') {
        const tOrCStr= tableOnly ? 'table' : 'table or chart';
        if (isImageAsTable(part,fileFormat)) {
            const twoD= getImageAsTableColCount(part,fileFormat)>2;
            const imageAsStr=  twoD ? '2D image - show as ': '1D image - show as ';
            return `HDU #${part.index} (${imageAsStr}${tOrCStr}${twoD? ' or image':''}) ${title}`;
        }
        else {
            return `HDU #${part.index} (${tOrCStr}) ${title}`;
        }
    }
    else {
        return `Part #${part.index} ${title}`;
    }
}


/**
 *
 * @param {boolean} tableOnly
 * @param {FileAnalysisPart} part
 * @param fileFormat
 * @param fileOnServer
 * @param title
 * @param activateParams
 * @param tbl_index
 * @return {DataProductsDisplayType|undefined}
 */
function analyzeChartTableResult(tableOnly, part, fileFormat, fileOnServer, title, activateParams, tbl_index=0) {
    const {uiEntry,uiRender,chartParamsAry, interpretedData=false, defaultPart:requestDefault= false}= part;
    const partFormat= part.convertedFileFormat||fileFormat;
    if (uiEntry===UIEntry.UseSpecified) {
        if (tableOnly && uiRender!==UIRender.Table) return undefined;
        else if (uiRender!==UIRender.Chart) return undefined;
    }

    const ddTitleStr= getTableDropTitleStr(title,part,partFormat,tableOnly);
    const {xCol,yCol,cNames,cUnits,connectPoints,useChartChooser}= getTableChartColInfo(title, part, partFormat);

    //define title for table and chart
    let titleInfo={titleStr:title, showChartTitle:true};
    if (!title){
        titleInfo.titleStr=`table_${part.index}`;
        titleInfo.showChartTitle=false;
    }

    if (tableOnly) {
        return dpdtTable(ddTitleStr,
            createChartTableActivate(false, fileOnServer,titleInfo,activateParams, undefined, tbl_index, cNames, cUnits),
            createTableExtraction(fileOnServer,titleInfo,tbl_index, cNames, cUnits),
            undefined, {paIdx:tbl_index,requestDefault});
    }
    else {

        if ( (!xCol || !yCol) && !chartParamsAry && !useChartChooser) return;


        let {chartTableDefOption}= part;
        if (getRowCnt(part,partFormat)===1) {
            if (!xCol || !yCol) return;
            if (chartTableDefOption===AUTO) chartTableDefOption= SHOW_TABLE;
            return dpdtChartTable(ddTitleStr,
                createChartSingleRowArrayActivate(fileOnServer,'Row 1 Chart',activateParams,xCol,yCol,0,tbl_index),
                createTableExtraction(fileOnServer,'Row 1 Chart',tbl_index, cNames, cUnits),
                undefined, {paIdx:tbl_index, chartTableDefOption, interpretedData, requestDefault});
        }
        else {
            const imageAsTableColCnt= isImageAsTable(part,partFormat) ? getImageAsTableColCount(part,partFormat) : 0;
            const chartInfo= {xAxis:xCol, yAxis:yCol, chartParamsAry, useChartChooser};
            if (chartTableDefOption===AUTO) chartTableDefOption= imageAsTableColCnt===2 ? SHOW_CHART : SHOW_TABLE;
            return dpdtChartTable(ddTitleStr,
                createChartTableActivate(true, fileOnServer,titleInfo,activateParams,chartInfo,tbl_index,cNames,cUnits,connectPoints),
                createTableExtraction(fileOnServer,titleInfo,tbl_index, cNames, cUnits),
                undefined, {paIdx:tbl_index, chartTableDefOption, interpretedData, requestDefault});
        }
    }
}

function analyzeImageResult(part, request, table, row, fileFormat, fileOnServer,title='', activateParams, hduIdx) {
    const {interpretedData=false,uiEntry,uiRender, defaultPart=false}= part;
    if (uiEntry===UIEntry.UseSpecified && uiRender!==UIRender.Image) return undefined;
    const newReq= request.makeCopy();
    let override= false;
    if (part.convertedFileName) {
        newReq.setRequestType(RequestType.FILE);
        newReq.setFileName(part.convertedFileName);
        newReq.setTitle(title);
        newReq.setTitleOptions(TitleOptions.NONE);
        override= true;
    }
    Object.entries(part.additionalImageParams ?? {}).forEach(([k,v]) => newReq.setParam(k,v));
    hduIdx>-1 && newReq.setMultiImageExts(hduIdx+'');
    const {imageViewerId}= activateParams;

    const ddTitleStr= (interpretedData || uiEntry===UIEntry.UseSpecified || fileOnServer) ?
                           `${title} (image)` :  `HDU #${hduIdx||0} (image) ${title}`;

    return dpdtImage(ddTitleStr,
        createSingleImageActivate(newReq,imageViewerId,table.tbl_id,row),
        createSingleImageExtraction(newReq),
        'image-'+0,
        {request:newReq, override, interpretedData, requestDefault:Boolean(defaultPart)});
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

function getHeader(header, part) {
    const data=part.details?.tableData?.data;
    if (!data) return undefined;
    const foundRow= part.details.tableData.data.find( (row) => row[1].toLowerCase()===header.toLowerCase());
    return foundRow && foundRow[2];
}


const tabNumericDataTypes= ['double', 'real', 'float', 'int', 'long', 'd', 'r', 'f', 'i', 'l'];
const fitNumericDataTypes= ['I', 'J', 'K', 'E', 'D', 'C', 'M'];

/**
 * Get the column name
 *   - for tables try to return only the numeric column if the information is available
 *   - for single dimension fits then make up column names
 *   - for fits images that we want to read as tables the make up the column names
 * @param part
 * @param fileFormat
 * @return {*|{length}|string[]}
 */
function getColumnNames(part, fileFormat) {
    if (fileFormat===Format.FITS) {
        const naxis= getIntHeader('NAXIS',part);
        const {tableColumnNames}= part;
        if (naxis===1) {
            if (isArray(tableColumnNames) && tableColumnNames.length===2) {
                return tableColumnNames;
            }
            else {
                return ['index', 'data'];
            }
        }
        else {
            const ttNamesAry= getHeadersThatStartsWith('TTYPE',part);
            if (ttNamesAry.length) {
                const ttFormAry= getHeadersThatStartsWith('TFORM',part);
                return ttFormAry.length===ttNamesAry.length ?    // return if we can tell - then all numeric columns else all columns
                    ttNamesAry.filter( (n,idx) => fitNumericDataTypes.includes(ttFormAry[idx])) : ttNamesAry;
            }
            const naxis2= getIntHeader('NAXIS2',part,0);
            if (naxis2<=30) {
                if (isArray(tableColumnNames) && tableColumnNames.length===naxis2+1) {
                    return tableColumnNames;
                }
                const dynHeaderNames= ['index'];
                for(let i=0;i<naxis2;i++) dynHeaderNames.push('data'+i);
                return dynHeaderNames;
            }
        }
    }
    else if (fileFormat===Format.CSV || fileFormat===Format.TSV) { // return all columns, we can't tell the type
        return part.details.tableData.data.map((row) => row[0]);
    }
    else if (fileFormat===Format.IPACTABLE) { // return all numeric columns and those we can't tell
        return part.details.tableData.data
            .filter( ([name,type]) => tabNumericDataTypes.includes(type) || type==='null')
            .map( ([name]) => name);
    }
    else { // return all numeric columns
        return part.details.tableData.data
            .filter( ([name,type]) => tabNumericDataTypes.includes(type))
            .map( ([name]) => name);
    }
}

function isImageAsTable(part, fileFormat) { return getImageAsTableColCount(part,fileFormat) > 0; }

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
