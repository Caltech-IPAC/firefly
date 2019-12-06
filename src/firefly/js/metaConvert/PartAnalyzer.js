import {dpdtChart, dpdtChartTable, dpdtTable, DPtypes} from './DataProductsType';
import {FileAnalysisType} from '../data/FileAnalysis';
import {createChartActivate, createTableActivate} from './converterUtils';





export function analyzePart(part, fileFormat, serverCacheFileKey, activateParams) {

    const {type,desc, index}= part;
    const availableTypes= findAvailableTypesForAnalysisPart(part, fileFormat);

    const chartResult= availableTypes.includes(DPtypes.CHART) && analyzeChartResult(part, fileFormat, serverCacheFileKey,desc,activateParams,index);
    const tableResult=
        availableTypes.includes(DPtypes.TABLE) &&
        (!chartResult || chartResult.displayType!==DPtypes.CHART_TABLE) &&
        analyzeTableResult(part, fileFormat, serverCacheFileKey,desc,activateParams,index,chartResult);

    return {
        isImage: availableTypes.includes(DPtypes.IMAGE) && type===FileAnalysisType.Image,
        tableResult,
        chartResult
    };
}

// todo
// todo - add intelligence here
//     - how can this be done on an application specific basis?
//     - probably needs to be an injection point
// todo
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

// todo
// todo - add intelligence here
//      - how can this be done on an application specific basis?
//      - probably needs to be an injection point
// todo
/**
 * Determine what type of displays are available for this part
 * @param part
 * @param fileFormat
 * @return {Array.<DPtypes>}
 */
function findAvailableTypesForAnalysisPart(part, fileFormat) {
    const {type}= part;
    if (type===FileAnalysisType.HeaderOnly || type===FileAnalysisType.Unknown) return [];
    if (type===FileAnalysisType.Image) return [DPtypes.IMAGE];
    return [DPtypes.CHART,DPtypes.TABLE];
}




const C_COL1= ['WAVE'];
const C_COL2= ['FLUX'];


// todo
// todo - add intelligence here
//      - how can this be done on an application specific basis?
//      - probably needs to be an injection point
// todo
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
    if (part.type!==FileAnalysisType.Table) return;
    if (part.details.totalRows<3) return;
    const cNames= getColumnNames(part,fileFormat);
    if (!cNames || cNames.length<2) return;
    const xCol= cNames.find( (c) => C_COL1.includes(c));
    const yCol= cNames.find( (c) => C_COL2.includes(c));
    if (!xCol || !yCol) return;

    //note - based on analysis this could also just call a dpdtChart() function

    return dpdtChartTable('Chart or Table: ' +title,
        createChartActivate(serverCacheFileKey,title,activateParams,xCol,yCol,tbl_index,false),
        undefined, {paIdx:tbl_index});
}


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
function analyzeTableResult(part, fileFormat, serverCacheFileKey,title, activateParams, tbl_index) {
    return dpdtTable('Table: ' +title, createTableActivate(serverCacheFileKey,title,activateParams,tbl_index,true),
        undefined, {paIdx:tbl_index});
}





//todo
//todo - add intelligence here
//     - currently only supports fits / needs to support VO
//     - maybe cvs and tsv?
//todo
function getColumnNames(part, fileFormat) {
    if (fileFormat==='FITS') {
        return part.details.tableData.data
            .filter( (row) => row[1].includes('TTYPE'))
            .map( (row) => row[2]);
    }
    else { //todo support more file formats
        return;
    }
}
