/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {isArray,isEmpty} from 'lodash';
import {getProdTypeGuess, getSSATitle, isSSATable} from '../voAnalyzer/TableAnalysis.js';
import {isFitsTableDataTypeNumeric} from '../visualize/FitsHeaderUtil.js';
import {dpdtChartTable, dpdtImage, dpdtTable, DPtypes, SHOW_CHART, SHOW_TABLE, AUTO} from './DataProductsType';
import {FileAnalysisType, Format, UIEntry, UIRender} from '../data/FileAnalysis';
import {RequestType} from '../visualize/RequestType.js';
import {TitleOptions} from '../visualize/WebPlotRequest';
import {createChartTableActivate, createChartSingleRowArrayActivate, createTableExtraction} from './TableDataProductUtils.js';
import {createSingleImageActivate, createSingleImageExtraction} from './ImageDataProductsUtil';

/**
 *
 * @param {FileAnalysisPart} part
 * @param {WebPlotRequest} request
 * @param {TableModel} table
 * @param {number} row
 * @param {String} fileFormat (see Format object)
 * @param {String} dataTypeHint  stuff like 'spectrum', 'image', 'cube', etc
 * @param serverCacheFileKey
 * @param {ActivateParams} activateParams
 * @param {DataProductsFactoryOptions} options
 * @return {{tableResult: DataProductsDisplayType|Array.<DataProductsDisplayType>|undefined, imageResult: DataProductsDisplayType|undefined}}
 */
export function analyzePart(part, request, table, row, fileFormat, dataTypeHint, serverCacheFileKey, activateParams, options) {

    const {type,desc, fileLocationIndex}= part;
    const aTypes= findAvailableTypesForAnalysisPart(part, fileFormat);
    if (isEmpty(aTypes)) return {imageResult:false, tableResult:false};

    const fileOnServer= (part.convertedFileName) ? part.convertedFileName : serverCacheFileKey;

    const imageResult= aTypes.includes(DPtypes.IMAGE) && type===FileAnalysisType.Image &&
        analyzeImageResult(part, request, table, row, fileFormat, part.convertedFileName,desc,activateParams,fileLocationIndex);

    const tableResult= aTypes.includes(DPtypes.TABLE) &&
        analyzeChartTableResult(false, table, row, part, fileFormat, fileOnServer,desc,dataTypeHint,activateParams,fileLocationIndex, options);

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
    if (type===FileAnalysisType.Image && naxis===1) {
        part.chartTableDefOption=SHOW_CHART;
        return [DPtypes.TABLE];
    }

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
 * @param table
 * @return {ChartInfo}
 */
function getTableChartColInfo(title, part, fileFormat,table) {
    if (isImageAsTable(part,fileFormat)) {
        const ssa= isSSATable(table);
        let cNames= [];
        const {tableColumnNames:overrideColNames, tableColumnUnits=[]}= part;
        const colCnt= getImageAsTableColCount(part,fileFormat);
        if (isArray(overrideColNames) && overrideColNames.length===colCnt) {
            cNames= overrideColNames;
        }
        if (ssa && colCnt===1) {
            cNames.push('wavelength');
            cNames.push('amplitude');
        }
        else {
            for(let i=0; i<colCnt; i++) cNames.push(i===0?'naxis1_idx': `naxis1_data_${(i-1)}`);
            if (colCnt===2) cNames[1]= !title || title.startsWith('(')? 'pixel value':title;
        }
        const cUnits= cNames.length===tableColumnUnits.length ? tableColumnUnits : undefined;
        const naxis3= getIntHeaderFromAnalysis('NAXIS3',part,0);
        return {xCol:cNames[0],yCol:cNames[1],cNames,cUnits, cubePlanes: naxis3, connectPoints:false};
    }
    else {
        const tabColNames= getColumnNames(part,fileFormat);
        if (!tabColNames || tabColNames.length<2) return {};

        if (part.details?.tableMeta?.utype) return {useChartChooser:true};

        let xCol= tabColNames.find( (c) => C_COL1.includes(c.toLowerCase()));
        let yCol= tabColNames.find( (c) => C_COL2.includes(c.toLowerCase()));
        let connectPoints= true;
        if (!xCol || !yCol) {
            xCol= findMatchingColumn(tabColNames,TS_C_COL1);
            yCol= findMatchingColumn(tabColNames,TS_C_COL2);
            connectPoints= false;
        }
        const rowsTotal= getRowCnt(part,fileFormat);
        if (rowsTotal<1) return {};
        return {xCol,yCol, connectPoints, useChartChooser:true};
    }
}

function findMatchingColumn(tabColNames, testList) {
    const bestMatcher= testList.find( (s) => tabColNames.find((c) => c.toLowerCase().match(s)?.[0]));
    if (!bestMatcher) return;
    return tabColNames.find( (c) => c.toLowerCase().match(bestMatcher)?.[0]) ;
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
    if (fileFormat===Format.FITS) {
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
 * @param {TableModel} table
 * @param {Number} row
 * @param {FileAnalysisPart} part
 * @param {String} fileFormat
 * @param {String} fileOnServer - server key to access the file
 * @param {String} title
 * @param {String} dataTypeHint  stuff like 'spectrum', 'image', 'cube', etc
 * @param {ActivateParams} activateParams
 * @param {number} tbl_index
 * @param {DataProductsFactoryOptions} options
 * @return {DataProductsDisplayType|undefined}
 */
function analyzeChartTableResult(tableOnly, table, row, part, fileFormat, fileOnServer, title, dataTypeHint='', activateParams, tbl_index=0, options) {
    const {uiEntry,uiRender,chartParamsAry, interpretedData=false, defaultPart:requestDefault= false}= part;
    const partFormat= part.convertedFileFormat||fileFormat;
    if (uiEntry===UIEntry.UseSpecified) {
        if (tableOnly && uiRender!==UIRender.Table) return undefined;
        else if (uiRender!==UIRender.Chart) return undefined;
    }

    let ddTitleStr= getTableDropTitleStr(title,part,partFormat,tableOnly);
    const {xCol,yCol,cNames,cUnits,cubePlanes=0,connectPoints,useChartChooser}= getTableChartColInfo(title, part, partFormat,table);

    //define title for table and chart
    const titleInfo={titleStr:title, showChartTitle:true};
    if (isSSATable(table)) {
        titleInfo.titleStr= getSSATitle(table,row)??'spectrum';
        ddTitleStr= titleInfo.titleStr;
        titleInfo.showChartTitle=false;
    }
    else if (!title){
        titleInfo.titleStr=`table_${part.index}`;
        titleInfo.showChartTitle=false;
    }

    const tbl_id=  options?.tableIdList?.find( (e) => e.description===title)?.tbl_id ?? options?.tableIdBase;
    const chartId=  options?.chartIdList?.find( (e) => e.description===title)?.chartId ?? options?.chartIdBase;
    const noChartParams= (!xCol || !yCol) && !chartParamsAry && !useChartChooser;
    if (tableOnly || noChartParams) {
        if (cubePlanes===0) {
            const extraction= createTableExtraction(fileOnServer,titleInfo,tbl_index, cNames, cUnits, 0, dataTypeHint);
            const activate= createChartTableActivate({source:fileOnServer,titleInfo,activateParams, tbl_index, dataTypeHint, cNames, cUnits,
                    tbl_id});
            return dpdtTable(ddTitleStr, activate, extraction, undefined,
                {extractionText: 'Pin Table', paIdx:tbl_index,requestDefault});
        }
        else {
            const retAry= [];
            for(let i=0;i<cubePlanes;i++) {
                const tInfoIdx= {titleStr:titleInfo.titleStr+ ` (Plane: ${i})` , ...titleInfo};
                const extraction= createTableExtraction(fileOnServer,tInfoIdx,tbl_index, cNames, cUnits, i, dataTypeHint);
                const activate= createChartTableActivate({source:fileOnServer,titleInfo:tInfoIdx,activateParams, tbl_index, dataTypeHint, cNames, cUnits,
                            tbl_id, cubePlane:i});
                retAry.push(
                    dpdtTable(ddTitleStr+ ` (Plane: ${i})`, activate, extraction, undefined,
                        {extractionText: 'Pin Table', paIdx:tbl_index,requestDefault})
                );
            }
            return retAry;
        }
    }
    else {

        let {chartTableDefOption}= part;
        if (getRowCnt(part,partFormat)===1 && xCol && yCol) {
            if (chartTableDefOption===AUTO) chartTableDefOption= SHOW_TABLE;
            return dpdtChartTable(ddTitleStr,
                createChartSingleRowArrayActivate(fileOnServer,'Row 1 Chart',activateParams,xCol,yCol,0,tbl_index, dataTypeHint),
                createTableExtraction(fileOnServer,'Row 1 Chart',tbl_index, cNames, cUnits, 0, dataTypeHint),
                undefined, {extractionText: 'Pin Table', paIdx:tbl_index, chartTableDefOption, interpretedData, requestDefault});
        }
        else {
            let dataTypeHintOverride= dataTypeHint;
            if (getPartProdGuess(part,table,row).toLowerCase().startsWith('spec')) {
                chartTableDefOption= SHOW_CHART;
                dataTypeHintOverride= 'spectrum';
            }
            const imageAsTableColCnt= isImageAsTable(part,partFormat) ? getImageAsTableColCount(part,partFormat) : 0;
            const chartInfo= {xAxis:xCol, yAxis:yCol, chartParamsAry, useChartChooser};
            if (dataTypeHintOverride==='spectrum') {
                chartTableDefOption= SHOW_CHART;
            }
            else if (chartTableDefOption===AUTO) {
                chartTableDefOption= imageAsTableColCnt===2 ? SHOW_CHART : SHOW_TABLE;
            }
            if (cubePlanes===0) {
                const extraction= createTableExtraction(fileOnServer, titleInfo, tbl_index, cNames, cUnits, 0, dataTypeHint);
                const activate= createChartTableActivate({
                            chartAndTable: true, source: fileOnServer, titleInfo, activateParams, chartInfo,
                            tbl_index, dataTypeHint: dataTypeHintOverride,
                            colNames: cNames, colUnits: cUnits, connectPoints, tbl_id, chartId });
                return dpdtChartTable(ddTitleStr, activate, extraction, undefined,
                    { extractionText: 'Pin Table', paIdx: tbl_index, chartTableDefOption, interpretedData, requestDefault });
            }
            else {
                const retAry= [];
                for(let i=0;i<cubePlanes;i++) {
                    const tInfoIdx= {titleStr:titleInfo.titleStr+ ` (Plane: ${i})` , ...titleInfo};
                    const extraction= createTableExtraction(fileOnServer, titleInfo, tbl_index, cNames, cUnits, i, dataTypeHint);
                    const activate= createChartTableActivate({
                                chartAndTable: true, source: fileOnServer, titleInfo:tInfoIdx, activateParams, chartInfo,
                                tbl_index, dataTypeHint: dataTypeHintOverride,
                                cubePlane:i,
                                colNames: cNames, colUnits: cUnits, connectPoints, tbl_id, chartId });
                    retAry.push(
                        dpdtChartTable(ddTitleStr+ ` (Plane: ${i})`, activate, extraction, undefined,
                            {
                                extractionText: 'Pin Table', paIdx: tbl_index, cubeIdx:i, chartTableDefOption,
                                interpretedData, requestDefault
                            })
                    );
                }
                return retAry;
            }
        }
    }
}

function getPartProdGuess(part,table,row) {
    const utype= part?.details?.tableMeta?.utype ?? part?.details?.tableMeta?.UTYPE;
    if (utype) return utype;
    const foundUtypes= part?.details?.resources?.map( (r) => r.utype)
        .filter( (utype='') => utype.toLowerCase().includes('spec'));
    if (foundUtypes?.length) return foundUtypes?.[0];
    return getProdTypeGuess(table,row);
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

    return dpdtImage({name:ddTitleStr,
        activate: createSingleImageActivate(newReq,imageViewerId,table?.tbl_id,row),
        extraction: createSingleImageExtraction(newReq),
        request:newReq, override, interpretedData, requestDefault:Boolean(defaultPart)});
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
    if (fileFormat===Format.FITS) {
        const naxis= getIntHeaderFromAnalysis('NAXIS',part);
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
            const ttNamesAry= getHeadersThatStartsWithFromAnalysis('TTYPE',part);
            if (ttNamesAry.length) {
                const ttFormAry= getHeadersThatStartsWithFromAnalysis('TFORM',part);

                return ttFormAry.length===ttNamesAry.length ?    // return if we can tell - then all numeric columns else all columns
                    ttNamesAry.filter( (n,idx) => isFitsTableDataTypeNumeric(ttFormAry[idx][ttFormAry[idx].length-1])) : ttNamesAry;
            }
            const naxis2= getIntHeaderFromAnalysis('NAXIS2',part,0);
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
