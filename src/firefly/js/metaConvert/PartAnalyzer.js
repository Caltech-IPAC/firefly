/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isArray,isEmpty} from 'lodash';
import {getSSATitle, isSSATable} from '../voAnalyzer/TableAnalysis.js';
import {isFitsTableDataTypeNumeric} from '../visualize/FitsHeaderUtil.js';
import {getObsCoreData} from '../voAnalyzer/VoDataLinkServDef';
import {dpdtChartTable, dpdtImage, dpdtTable, DPtypes, SHOW_CHART, SHOW_TABLE} from './DataProductsType';
import {FileAnalysisType, Format, TableDataType, UIEntry, UIRender} from '../data/FileAnalysis';
import {RequestType} from '../visualize/RequestType.js';
import {TitleOptions} from '../visualize/WebPlotRequest';
import {createChartTableActivate, createChartSingleRowArrayActivate, createTableExtraction, getExtractionText
} from './TableDataProductUtils.js';
import {createSingleImageActivate, createSingleImageExtraction} from './ImageDataProductsUtil';

/**
 *
 * @param {Object} p
 * @param {FileAnalysisPart} p.part
 * @param {WebPlotRequest} p.request
 * @param {TableModel} p.table
 * @param {number} p.row
 * @param {String} p.fileFormat (see Format object)
 * @param {DatalinkData} p.dlData
 * @param {String} p.source
 * @param {String} p.originalTitle
 * @param {ActivateParams} p.activateParams
 * @param {DataProductsFactoryOptions} p.options
 * @param {String} p.title
 * @return {{tableResult: DataProductsDisplayType|Array.<DataProductsDisplayType>|undefined, imageResult: DataProductsDisplayType|undefined}}
 */
export function analyzePart({part, request, table, row, fileFormat, dlData, originalTitle,
                                source, activateParams, options={}, title}) {

    const {type,desc, fileLocationIndex}= part;
    const titleToUse= title ?? desc;
    const aTypes= findAvailableTypesForAnalysisPart(part, fileFormat);
    if (isEmpty(aTypes)) return {imageResult:false, tableResult:false};

    const imageResult= aTypes.includes(DPtypes.IMAGE) && type===FileAnalysisType.Image &&
        analyzeImageResult({part, request, table, row, fileFormat, dlData, source,
            title:desc,activateParams,hduIdx:fileLocationIndex, originalTitle});

    const tableResult= aTypes.includes(DPtypes.TABLE) &&
        analyzeChartTableResult(table, row, part, fileFormat, source,titleToUse??desc,activateParams,options, originalTitle, dlData);

    return {imageResult, tableResult};
}



/**
 * Determine what type of displays are available for this part
 * @param part
 * @param fileFormat
 * @return {Array.<String>} return an array of DPTypes string const
 */
function findAvailableTypesForAnalysisPart(part, fileFormat) {
    const {type}= part;
    const naxis= getIntHeaderFromAnalysis('NAXIS',part,0);
    if (type===FileAnalysisType.HeaderOnly || type===FileAnalysisType.Unknown) return [];
    if (type!==FileAnalysisType.Image &&  fileFormat!=='FITS' &&  is1DImage(part) || type===FileAnalysisType.Table ) return [DPtypes.TABLE];
    if (type===FileAnalysisType.Image && naxis===1) return [DPtypes.TABLE];
    return (imageCouldBeTable(part)) ? [DPtypes.IMAGE,DPtypes.TABLE] : [DPtypes.IMAGE];
}


function imageCouldBeTable(part) {
    const naxis= getIntHeaderFromAnalysis('NAXIS',part,0);
    if (naxis<1) return false;
    if (naxis===1) return true;
    const naxisAry= [];
    for(let i=0; i<naxis;i++) naxisAry[i]= getIntHeaderFromAnalysis(`NAXIS${i+1}`,part,0);
    if (naxis===2) return naxisAry[1]<=30;
    if (naxis===3) return naxisAry[1]===1;
    else {
        let couldBeTable= true;
        for(let i=1; (i<naxis);i++) if (naxisAry[i]>1) couldBeTable= false;
        return couldBeTable;
    }
}

function is1DImage(part) {
    const naxis= getIntHeaderFromAnalysis('NAXIS',part,0);
    if (naxis<1) return false;
    if (naxis===1) return true;
    const naxisAry= [];
    for(let i=0; i<naxis;i++) naxisAry[i]= getIntHeaderFromAnalysis(`NAXIS${i+1}`,part,0);
    let is1DImage= true;
    for(let i=1; (i<naxis);i++) if (naxisAry[i]>1) is1DImage= false;
    return is1DImage;
}


const C_COL1= ['index','wave'];
const C_COL2= ['flux','data','data1','data2'];

const TS_C_COL1= ['mjd'];
const TS_C_COL2= [ 'psfflux', /psf.*flux/, 'mag','flux',];
const TS_UCD_COL1= ['time.epoch;obs.exposure'];
const TS_UCD_COL2= ['phot.flux'];

const SPACITAL_C_COL1= ['ra','lon', 'c_ra', 'ra1'];
const SPACITAL_C_COL2= ['dec','lat','c_dec','dec1'];

/**
 * Return a object that specifieds the xy chart columns, the column names, and the column units
 * Each property in the object is optional
 * xCol and yCol is return it an appropriate one if found
 * if table is a Fits image rendered as a table the colNames is always returned, colUnits is return if specified in the part
 * if the table is a FITS table or some other type of table then the colNames and colUnits is never returned.
 * @param title
 * @param {FileAnalysisPart} part
 * @param fileFormat
 * @param {TableModel} table
 * @return {ChartInfo}
 */
function getTableChartColInfo(title, part, fileFormat,table) {
    const {chartParamsAry}= part;
    let showChartTitle= Boolean(title);
    let tableDataType= TableDataType.NotSpecified;
    if (isImageAsTable(part,fileFormat)) {
        const ssa= isSSATable(table);
        let colNames= [];
        const {tableColumnNames:overrideColNames, tableColumnUnits=[]}= part;
        const colCnt= getImageAsTableColCount(part,fileFormat);
        if (isArray(overrideColNames) && overrideColNames.length===colCnt) {
            colNames= overrideColNames;
        }
        if (ssa && colCnt===1) {
            colNames.push('wavelength');
            colNames.push('amplitude');
        }
        else {
            for(let i=0; i<colCnt; i++) colNames.push(i===0?'naxis1_idx': `naxis1_data_${(i-1)}`);
            if (colCnt===2) colNames[1]= !title || title.startsWith('(')? 'pixel value':title;
        }
        const colUnits= colNames.length===tableColumnUnits.length ? tableColumnUnits : undefined;
        const naxis3= getIntHeaderFromAnalysis('NAXIS3',part,0);


        const xAxis= colNames[0];
        const yAxis= colNames[1];
        const hasChart= (xAxis && yAxis) || chartParamsAry;
        const chartInfo= {hasChart, showChartTitle,tableDataType, connectPoints:false, xAxis:colNames[0], yAxis:colNames[1], chartParamsAry:undefined, useChartChooser:false};

        if (colCnt===1) {
            chartInfo.useChartChooser=true;
            chartInfo.xAxis=undefined;
            chartInfo.yAxis=undefined;
            chartInfo.hasChart=true;
        }
        return {chartInfo,
            imageAsTableInfo:{hasChart, colNames,colUnits, cubePlanes: naxis3}, colNames,colUnits, cubePlanes: naxis3};
    }
    else {
        const noChartResult= {chartInfo:{hasChart:false, showChartTitle}, imageAsTableInfo:{}};
        const colInfo= getColumnNames(part,fileFormat);
        const tabColNames= colInfo.map(({name}) => name);
        
        if (!tabColNames || tabColNames.length<2) return noChartResult;


        let xCol= tabColNames.find( (c) => C_COL1.includes(c.toLowerCase()));
        let yCol= tabColNames.find( (c) => C_COL2.includes(c.toLowerCase()));
        let connectPoints= true;

        if (part.tableDataType===TableDataType.Spectrum) {
            tableDataType= TableDataType.Spectrum;
            if (isSSATable(table)) {
                showChartTitle= Boolean(getSSATitle(table,table.highlightedRow));
            }
        }
        else if (isTimeSeries(colInfo)) {
            const ts= getTimeSeriesColumns(colInfo);
            xCol= ts.xCol;
            yCol= ts.yCol;
            connectPoints= false;
            tableDataType= TableDataType.LightCurve;
        }

        const rowsTotal= getRowCnt(part,fileFormat);
        if (rowsTotal<1) return noChartResult;

        const xAxis= xCol;
        const yAxis= yCol;
        const hasChart= Boolean((xAxis && yAxis) || chartParamsAry ||
            tableDataType!==TableDataType.NotSpecified || part.details?.tableMeta?.utype);
        const chartInfo= {hasChart, showChartTitle, tableDataType, connectPoints, xAxis:xCol, yAxis:yCol, chartParamsAry, useChartChooser:true};

        return {chartInfo, imageAsTableInfo:{}};
    }
}

function getTimeSeriesColumns(colInfo) {
    const xCol= findMatchingUCDColumn(colInfo,TS_UCD_COL1) ?? findMatchingColumn(colInfo,TS_C_COL1);
    const yCol= findMatchingUCDColumn(colInfo,TS_UCD_COL2) ?? findMatchingColumn(colInfo,TS_C_COL2);
    return {xCol:xCol?.name,yCol:yCol?.name};
}

function isTimeSeries(colInfo) {
    const {xCol,yCol}= getTimeSeriesColumns(colInfo);
    return Boolean(xCol && yCol);
}

function findMatchingColumn(colInfo, testList) {
    const bestMatcher= testList.find( (s) => colInfo.find((ci) => ci.name?.toLowerCase().match(s)?.[0]));
    if (!bestMatcher) return;
    return colInfo.find( (ci) => ci?.name.toLowerCase().match(bestMatcher)?.[0]) ;
}

function findMatchingUCDColumn(colInfo, testList) {
    const bestMatcher= testList.find( (s) => colInfo.find((ci) => ci.UCD?.toLowerCase().match(s)?.[0]));
    if (!bestMatcher) return;
    return colInfo.find( (ci) => ci.UCD?.toLowerCase().match(bestMatcher)?.[0]) ;
}

/**
 *
 * @param {String} title
 * @param {String} originalTitle
 * @param {FileAnalysisPart} part
 * @param fileFormat
 * @param {TableModel} table
 * @return {string}
 */
function getTableDropTitleStr(title,originalTitle,part,fileFormat,table) {
    if (!title) title='';
    if (part.interpretedData) return title;
    let endTitle;
    if (fileFormat===Format.FITS) {
        const tOrCStr= 'table or chart';
        if (isImageAsTable(part,fileFormat)) {
            const twoD= getImageAsTableColCount(part,fileFormat)>2;
            const imageAsStr=  twoD ? '2D image - show as ': '1D image - show as ';
            endTitle= `HDU #${part.index} (${imageAsStr}${tOrCStr}${twoD? ' or image':''}) ${title}`;
        }
        else {
            endTitle= `HDU #${part.index} (${tOrCStr}) ${title}`;
        }
    }
    else {
        endTitle= isSSATable(table) ? getSSATitle(table,table.highlightedRow)  : `Part #${part.index} ${title}`;
    }
    return endTitle;
}


/**
 *
 * @param {TableModel} table
 * @param {Number} row
 * @param {FileAnalysisPart} part
 * @param {String} fileFormat
 * @param {String} source - server key to access the file
 * @param {String} title
 * @param {ActivateParams} activateParams
 * @param {DataProductsFactoryOptions} options
 * @param {String} originalTitle
 * @param {DatalinkData} dlData
 * @return {DataProductsDisplayType|undefined}
 */
function analyzeChartTableResult(table, row, part, fileFormat, source, title, activateParams,
                                 options={}, originalTitle, dlData) {
    const {uiEntry,uiRender,interpretedData=false, fileLocationIndex:tbl_index=0}= part;
    const partFormat= part.convertedFileFormat||fileFormat;
    if (uiEntry===UIEntry.UseSpecified) {
        if (uiRender!==UIRender.Chart) return undefined;
    }
    const {tbl_id,chartId}= getIds(options,title);

    const ddTitleStr= getTableDropTitleStr(title,originalTitle, part,partFormat, table);
    const dropDownText= originalTitle ? `${originalTitle} - ${ddTitleStr}` : ddTitleStr;
    const {chartInfo, imageAsTableInfo}= getTableChartColInfo(title, part, partFormat, table);

    let titleInfo=title || `table_${part.index}`;
    if (isSSATable(table)) titleInfo= getSSATitle(table,row)??'spectrum';
    

    if (chartInfo.hasChart) {
        const chartTableDefOption= getChartTableDefaultOption(part,chartInfo,partFormat);
        if (isSingleRowChart(part,partFormat,chartInfo)) { // a common case- single row table with array columns
            return makeSingleRowChartTableResult({ddTitleStr,source,activateParams,chartInfo,tbl_index, imageAsTableInfo,
                                                       chartTableDefOption, interpretedData, dropDownText});
        }
        else {
            return makeChartTableResult({
                part, source, chartTableDefOption, ddTitleStr, titleInfo,tbl_index, tbl_id,
                chartId, activateParams, dropDownText, chartInfo,imageAsTableInfo,
                statefulTabComponentKey: options.statefulTabComponentKey
            });
        }
    }
    else {
        return makeTableOnlyResult({ddTitleStr,tbl_id,source,titleInfo,tbl_index,imageAsTableInfo,
            activateParams, dropDownText, statefulTabComponentKey: options.statefulTabComponentKey});
    }
}


function getIds(options,title) {
    const tbl_id=  options?.tableIdList?.find( (e) => e.description===title)?.tbl_id ?? options?.tableIdBase;
    const chartId=  options?.chartIdList?.find( (e) => e.description===title)?.chartId ?? options?.chartIdBase;
    return {tbl_id,chartId};
}

function makeSingleRowChartTableResult({ddTitleStr,source,activateParams,chartInfo,tbl_index:paIdx, imageAsTableInfo,
                                           chartTableDefOption, interpretedData, dropDownText}) {
    const titleStr= 'Row 1 Chart';
    const activate= createChartSingleRowArrayActivate(source,titleStr,activateParams,chartInfo,0,paIdx);
    const extraction= createTableExtraction(source,titleStr,paIdx, imageAsTableInfo,
        0, chartInfo.tableDataType);
    const extractionText = getExtractionText(chartInfo.tableDataType);
    return dpdtChartTable(ddTitleStr, activate,extraction, undefined,
        {extractionText, paIdx, chartTableDefOption, interpretedData, dropDownText});
}

function makeChartTableResult({part, source, chartTableDefOption, ddTitleStr, titleInfo,tbl_index, tbl_id,
                                  chartId, activateParams, dropDownText, chartInfo,imageAsTableInfo,
                                  statefulTabComponentKey}) {
    const {interpretedData=false}= part;
    const {tableDataType} = chartInfo;
    const extractionText = getExtractionText(tableDataType);
    if (!imageAsTableInfo?.cubePlanes) {
        const extraction = createTableExtraction(source, titleInfo, tbl_index, imageAsTableInfo, 0, tableDataType);
        const activate = createChartTableActivate({
            chartAndTable: true, source, titleInfo, activateParams, chartInfo, tbl_index,
            imageAsTableInfo, tbl_id, chartId, statefulTabComponentKey });
        return dpdtChartTable(ddTitleStr, activate, extraction, undefined,
            {extractionText, paIdx: tbl_index, chartTableDefOption, interpretedData, dropDownText});
    } else {
        const retAry = [];
        for (let i = 0; i < imageAsTableInfo.cubePlanes; i++) {
            const {activate, extraction, dropDownTextIdx} = getCubeTableChartParams( {
                    i, titleInfo, dropDownText, source, tbl_index, imageAsTableInfo, activateParams,
                    chartId, chartInfo});
            retAry.push(
                dpdtChartTable(ddTitleStr + ` (Plane: ${i})`, activate, extraction, undefined,
                    {
                        extractionText, paIdx: tbl_index, cubeIdx: i, chartTableDefOption,
                        interpretedData, dropDownText: dropDownTextIdx
                    })
            );
        }
        return retAry;
    }
}

function makeTableOnlyResult({ddTitleStr,tbl_id,source,titleInfo,tbl_index,imageAsTableInfo,
                                 activateParams, dropDownText, statefulTabComponentKey}) {
    const {cubePlanes=0}= imageAsTableInfo;
    const extractionText = 'Pin Table';
    if (cubePlanes===0) {
        const extraction= createTableExtraction(source,titleInfo,tbl_index, imageAsTableInfo, 0);
        const activate= createChartTableActivate({source, titleInfo,activateParams,
            tbl_index, imageAsTableInfo, tbl_id, statefulTabComponentKey});
        return dpdtTable(ddTitleStr, activate, extraction, undefined,
            {extractionText, paIdx:tbl_index,dropDownText});
    }
    else {
        const retAry= [];
        for(let i=0;i<cubePlanes;i++) {
            const {activate,extraction,dropDownTextIdx}=
                getCubeTableChartParams( {i,titleInfo,dropDownText,source, tbl_index,
                    imageAsTableInfo,activateParams,tbl_id});
            retAry.push(
                dpdtTable(ddTitleStr+ ` (Plane: ${i})`, activate, extraction, undefined,
                    {extractionText, paIdx:tbl_index,dropDownText,dropDownTextIdx})
            );
        }
        return retAry;
    }
}

function getCubeTableChartParams({i,titleInfo,dropDownText,source, tbl_index,imageAsTableInfo, activateParams,
                                     tbl_id,chartId,chartInfo={}}) {
    const title= titleInfo+ ` (Plane: ${i})`;
    const {tableDataType=''}= chartInfo;
    const dropDownTextIdx= dropDownText+ ` (Plane: ${i})`;
    const extraction= createTableExtraction(source, titleInfo, tbl_index, imageAsTableInfo, i, tableDataType);

    const activateObj= {source, titleInfo:title, activateParams, tbl_index, cubePlane:i, imageAsTableInfo, tbl_id};
    if (chartInfo.hasChart) {
        activateObj.chartAndTable= true;
        activateObj.chartInfo= chartInfo;
        activateObj.chartId= chartId;
    }
    const activate= createChartTableActivate(activateObj);
    return {activate,extraction,dropDownTextIdx};
}


const isSingleRowChart= (part,partFormat,chartInfo) =>
  getRowCnt(part,partFormat)===1 && chartInfo.xAxis && chartInfo.yAxis;

function getChartTableDefaultOption(part, chartInfo, partFormat) {
    const {tableDataType}= chartInfo;
    const {chartTableDefOption}= part;
    if (chartTableDefOption===SHOW_CHART  || chartTableDefOption===SHOW_TABLE) return chartTableDefOption;
    // part.chartTableDefOption is auto, so determine what it should be
    if (part.type===FileAnalysisType.Image && getImageAsTableColCount(part,partFormat)===1) return SHOW_CHART;
    if (isSingleRowChart(part,partFormat,chartInfo)) return SHOW_TABLE;
    if (tableDataType===TableDataType.Spectrum || tableDataType===TableDataType.LightCurve) return SHOW_CHART;
    const imageAsTableColCnt= isImageAsTable(part,partFormat) ? getImageAsTableColCount(part,partFormat) : 0;
    if (imageAsTableColCnt===2) return SHOW_CHART;
    return SHOW_TABLE;
}

function analyzeImageResult({part, request, table, row, dlData, source,title='', activateParams, hduIdx}) {
    const {interpretedData=false,uiEntry,uiRender}= part;
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

    const ddTitleStr= (interpretedData || uiEntry===UIEntry.UseSpecified || source) ?
                           `${title} (image)` :  `HDU #${hduIdx||0} (image) ${title}`;
    const dropDownText= dlData ? `${ddTitleStr} (${dlData.semantics})` : ddTitleStr;

    const sourceObsCoreData= dlData ? dlData.sourceObsCoreData : getObsCoreData(table,row);

    return dpdtImage({name:ddTitleStr, dlData, dropDownText,
        activate: createSingleImageActivate(newReq,imageViewerId,table?.tbl_id,row),
        extraction: createSingleImageExtraction(newReq, sourceObsCoreData, dlData ),
        request:newReq, override, interpretedData});
}


export const getIntHeaderFromAnalysis= (header, part, def) => {
    const resultStr= getTableHeaderFromAnalysis(header,part);
    if (!resultStr) return def;
    const num= Number(resultStr);
    return isNaN(num) ? def : num;
};

function getHeadersThatMatch(header, part) {
    return part.details?.tableData?.data
        .filter( (row) => row[1].includes(header))
        .map( (row) => row[2]) ?? [];
}

function getHeadersThatStartsWithFromAnalysis(header, part) {
    return part.details?.tableData?.data
        .filter( (row) => row[1].startsWith(header))
        .map( (row) => row[2]) ?? [];
}

export function getTableHeaderFromAnalysis(header, part) {
    const data=part.details?.tableData?.data;
    if (!data) return undefined;
    const foundRow= part.details.tableData.data.find( (row) => row[1].toLowerCase()===header.toLowerCase());
    return foundRow && foundRow[2];
}


const tabNumericDataTypes= ['double', 'real', 'float', 'int', 'long', 'd', 'r', 'f', 'i', 'l'];

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
    const UCD='';
    if (fileFormat===Format.FITS) {
        const naxis= getIntHeaderFromAnalysis('NAXIS',part);
        const {tableColumnNames}= part;
        if (naxis===1) {
            if (isArray(tableColumnNames) && tableColumnNames.length===2) {
                return tableColumnNames.map( (name) => ({name,UCD}));
            }
            else {
                return [{name:'index',UCD}, {name:'data',UCD}];
            }
        }
        else {
            const ttNamesAry= getHeadersThatStartsWithFromAnalysis('TTYPE',part);
            if (ttNamesAry.length) {
                const ttFormAry= getHeadersThatStartsWithFromAnalysis('TFORM',part);
                const tableColumnNames= // if we can tell - then  get all numeric columns else all columns
                    ttFormAry.length===ttNamesAry.length
                    ? ttNamesAry.filter( (n,idx) => isFitsTableDataTypeNumeric(ttFormAry[idx][ttFormAry[idx].length-1]))
                    : ttNamesAry;
                return tableColumnNames.map( (name,idx) => {
                    const matchAry=getHeadersThatMatch('TUCD'+idx, part) ?? '';
                    return ({name,UCD: matchAry[0] || ''});
                });
            }
            const naxis2= getIntHeaderFromAnalysis('NAXIS2',part,0);
            if (naxis2<=30) {
                if (isArray(tableColumnNames) && tableColumnNames.length===naxis2+1) {
                    return tableColumnNames.map( (name) => ({name,UCD}));
                }
                const dynHeaderNames= ['index'];
                for(let i=0;i<naxis2;i++) dynHeaderNames.push('data'+i);
                return dynHeaderNames.map( (name) => ({name,UCD}));
            }
        }
    }
    else if (fileFormat===Format.CSV || fileFormat===Format.TSV) { // return all columns, we can't tell the type
        return part.details.tableData.data.map((row) => ({name:row[0],UCD}) );
    }
    else if (fileFormat===Format.IPACTABLE) { // return all numeric columns and those we can't tell
        return part.details.tableData.data
            .filter( ([,type]) => tabNumericDataTypes.includes(type) || type==='null')
            .map( ([name]) => ({name,UCD}) );
    }
    else { // return all numeric columns
        return part.details.tableData.data
            .filter( ([,type]) => tabNumericDataTypes.includes(type))
            .map( ([name,,,,UCD]) => ({name,UCD}));
    }
}

function isImageAsTable(part, fileFormat) { return getImageAsTableColCount(part,fileFormat) > 0; }

function getImageAsTableColCount(part, fileFormat) {
    if (part.type!==FileAnalysisType.Image) return 0;
    if (fileFormat !== 'FITS') return 0;
    if (is1DImage(part)) return 2;
    const naxis = getIntHeaderFromAnalysis('NAXIS', part);
    const naxis2= getIntHeaderFromAnalysis('NAXIS2',part,0);
    if (naxis===3) return naxis2;
    if (naxis!==2) return 0;
    if (naxis2>30) return 0;
    return naxis2+1;
}

function getRowCnt(part, fileFormat) {
    if (part.totalTableRows>0) return part.totalTableRows;
    if (fileFormat==='FITS') {
        const naxis2= getIntHeaderFromAnalysis('NAXIS2',part);
        const naxis1= getIntHeaderFromAnalysis('NAXIS1',part);
        const ttNamesAry= getHeadersThatStartsWithFromAnalysis('TTYPE',part);
        if (ttNamesAry.length) {
            return isNaN(naxis2) ? -1 : naxis2;
        }
        else {
            return isNaN(naxis1) ? -1 : naxis1;
        }
    }
}
