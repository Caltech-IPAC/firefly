/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {get, isEmpty, isArray} from 'lodash';
import {RangeValues, SIGMA, STRETCH_LINEAR} from '../visualize/RangeValues.js';
import {makeWiseViewCreate, makeWisePlotRequest} from './WiseRequestList.js';
import {make2MassPlotRequest} from './TwoMassRequestList.js';
import {makeAtlasPlotRequest} from './AtlasRequestList.js';
import {makeZtfPlotRequest} from './ZtfRequestList.js';
import {WebPlotRequest, TitleOptions} from '../visualize/WebPlotRequest.js';
import {ZoomType} from '../visualize/ZoomType.js';
import {Band} from '../visualize/Band';
import {getCellValue} from '../tables/TableUtil.js';
import {makeWorldPt, parseWorldPt} from '../visualize/Point.js';
import {MetaConst} from '../data/MetaConst.js';
import {CoordinateSys} from '../visualize/CoordSys.js';
import {hasObsCoreLikeDataProducts, hasServiceDescriptors} from '../util/VOAnalyzer.js';
import {
    makeObsCoreConverter,
    getObsCoreGridDataProduct,
    getObsCoreDataProduct
} from './ObsCoreConverter.js';
import {
    createGridImagesActivate,
    createRelatedDataGridActivate,
    createSingleImageActivate, createSingleImageExtraction
} from './ImageDataProductsUtil';
import {makeAnalysisGetGridDataProduct, makeAnalysisGetSingleDataProduct} from './MultiProductFileAnalyzer';
import {dpdtImage} from './DataProductsType';
import {dispatchUpdateCustom, GRID, GRID_FULL, GRID_RELATED} from '../visualize/MultiViewCntlr';
import {getDataSourceColumn} from '../util/VOAnalyzer';
import {getColumn, getMetaEntry} from '../tables/TableUtil';
import {getAppOptions} from '../core/AppDataCntlr';
import {getServiceDescSingleDataProduct} from './ServDescConverter.js';

const FILE= 'FILE';
const DEFAULT_CONVERTER_ID= 'DEFAULT_CONVERTER';


function matchById(table,id)  {
    if (!id || !table || table.isFetching) return false;
    const value= findTableMetaEntry(table, [MetaConst.IMAGE_SOURCE_ID, MetaConst.DATASET_CONVERTER]);
    if (!value) return false;
    return value.toUpperCase()===id.toUpperCase();
}

/**
 * @param {TableModel} table
 * @param {DataProductsConvertType} converterTemplate
 * @return {DataProductsConvertType}
 */
const simpleCreate= (table, converterTemplate) => converterTemplate;



/**
 * @global
 * @public
 * @typedef {Object} ActivateParams
 * @prop {string} imageViewerId
 * @prop {string} chartViewerId
 * @prop {string} tableGroupViewerId
 * @prop {string} dpId - data product id
 *
 */


/**
 * Returns a callback function or a Promise<DataProductsDisplayType>.
 * callback: function(table:TableModel, row:String,activeParams:ActivateParams)
 * @param makeReq
 * @return {function | promise}
 */
function getSingleDataProductWrapper(makeReq) {
    return (table, row, activateParams) => {
        const {imageViewerId}= activateParams;
        const retVal= makeReq(table, row, true);
        const r= get(retVal,'single');
        const activate= createSingleImageActivate(r,imageViewerId,table.tbl_id,row);
        const extraction= createSingleImageExtraction(r);
        return Promise.resolve( dpdtImage('Image', activate, extraction, undefined, {extractionText:'Pin Image'}));
    };
}


/**
 *
 * Returns a callback function or a Promise<DataProductsDisplayType>.
 * callback: function(table:TableModel, plotRows:Array.<Object>,activeParams:ActivateParams)
 * @param makeReq
 * @return {function | promise}
 */
function getGridDataProductWrapper(makeReq) {
    return (table, plotRows, activateParams) => {
        const {imageViewerId}= activateParams;

        const reqAry= plotRows
            .map( (pR) => get(makeReq(table,pR.row,true),'single'))
            .filter( (r) => r);

        const activate= createGridImagesActivate(reqAry,imageViewerId,table.tbl_id,plotRows);
        return Promise.resolve( dpdtImage('Image Grid', activate));
    };
}


/**
 *
 * @param makeReq
 * @return {Function}
 */
function getRelatedDataProductWrapper(makeReq) {
    return (table, row, threeColorOps, highlightPlotId, activateParams) => {
        const {imageViewerId}= activateParams;
        const retVal= makeReq(table, row, false,true,threeColorOps);
        if (retVal) {
            const activate= createRelatedDataGridActivate(retVal,imageViewerId,table.tbl_id, highlightPlotId ?? retVal.highlightPlotId);
            const extraction= retVal.standard ? createSingleImageExtraction(retVal.standard) : undefined;
            return Promise.resolve( dpdtImage('Images', activate, extraction,undefined, {extractionText:'Pin Image'} ));
        }
        else {
            return Promise.resolve( {});
        }
    };
}





/**
 * @global
 * @public
 * @typedef {Object} DataProductsConvertType
 *
 * @prop {function} tableMatches, function to test if the table fits the template form: tableMatches(TableModel): boolean
 * @prop {function(table:TableModel, template:DataProductsConvertType):DataProductsConvertType} create - create the converter based on this template. The converter is created by a function that is
 *               passed a table and the converter template. It then may return a new converter template
 *               that could be optionally customized to the table.
 *               form: create(TableModel,DataProductsConvertType): DataProductsConvertType
 * @prop {boolean} threeColor supports three color images
 * @prop {boolean} hasRelatedBands supports groups of related images
 * @prop {boolean} canGrid support grids of images
 * @prop {number} maxPlot total number of images that can be created at a time, i.e. page size
 * @prop {string} initialLayout - one of SINGLE, GRID, GRID_RELATED, GRID_FULL
 *
 * @prop {function(table:TableModel,row:String,activateParams:ActivateParams):function} getSingleDataProduct
 *  pass table,rowNum,activate params return a activate function
 *  required
 *
 * @prop {function(table:TableModel,plotRows:Array<{plotId:String,row:number,highlight:boolean}>,activateParams:ActivateParams):function} getGridDataProduct
 * pass(table, array of plotrows, and activateParams) return activate function.
 * Only required if canGrid is true
 *
 * @prop {function(table:TableModel,plotRows:Array<Object>,threeColorOps:Object, highlightPlotId:string,activateParams:ActivateParams):function}
 * getRelatedDataProduct, pass (table, row, threeColorOps, highlightPlotId, activateParams) , return activate function.
 * Only required if hasRelatedBands is true

 * @prop {Object} threeColorBands definition of the three color plot request
 *
 */




function initConverterTemplates() {
    /**
     * @type {Array.<DataProductsConvertType>}
     */
    const originalConverterTemplates = [
        {
            converterId: 'wise',
            tableMatches: (table) => matchById(table, 'wise'),
            create: makeWiseViewCreate,
            getSingleDataProduct: getSingleDataProductWrapper(makeWisePlotRequest),
            getGridDataProduct: getGridDataProductWrapper(makeWisePlotRequest),
            getRelatedDataProduct: getRelatedDataProductWrapper(makeWisePlotRequest),
        },
        {
            converterId: 'atlas',
            tableMatches: (table) => matchById(table, 'atlas'),
            create: simpleCreate,
            hasRelatedBands: true,
            canGrid: true,
            maxPlots: 5,
            getSingleDataProduct: getSingleDataProductWrapper(makeAtlasPlotRequest),
            getGridDataProduct: getGridDataProductWrapper(makeAtlasPlotRequest),
            getRelatedDataProduct: getRelatedDataProductWrapper(makeAtlasPlotRequest),
        },
        {
            converterId: 'twomass',
            tableMatches: (table) => matchById(table, 'twomass'),
            create: simpleCreate,
            threeColor: true,
            hasRelatedBands: true,
            canGrid: true,
            maxPlots: 12,
            getSingleDataProduct: getSingleDataProductWrapper(make2MassPlotRequest),
            getGridDataProduct: getGridDataProductWrapper(make2MassPlotRequest),
            getRelatedDataProduct: getRelatedDataProductWrapper(make2MassPlotRequest),
            threeColorBands: {
                J: {color: Band.RED, title: 'J'},
                H: {color: Band.GREEN, title: 'H'},
                K: {color: Band.BLUE, title: 'K'}
            }
        },
        {
            converterId: 'ztf',
            tableMatches: (table) => matchById(table, 'ztf'),
            create: simpleCreate,
            hasRelatedBands: false,
            canGrid: true,
            maxPlots: 12,
            getSingleDataProduct: getSingleDataProductWrapper(makeZtfPlotRequest),
            getGridDataProduct: getGridDataProductWrapper(makeZtfPlotRequest),
            getRelatedDataProduct: getRelatedDataProductWrapper(makeZtfPlotRequest),
        },
        {
            converterId: 'ObsCore',
            tableMatches: hasObsCoreLikeDataProducts,
            create: makeObsCoreConverter,
            threeColor: false,
            hasRelatedBands: false,
            canGrid: true,
            maxPlots: 8,
            getSingleDataProduct: getObsCoreDataProduct,
            getGridDataProduct: getObsCoreGridDataProduct,
            getRelatedDataProduct: () => Promise.reject('related data products not supported')
        },
        {
            converterId: 'ServiceDescriptors',
            tableMatches: hasServiceDescriptors,
            create: simpleCreate,
            threeColor: false,
            hasRelatedBands: false,
            canGrid: false,
            maxPlots: 1,
            getSingleDataProduct: getServiceDescSingleDataProduct,
            getGridDataProduct: null,
            getRelatedDataProduct: () => Promise.reject('related data products not supported')
        },
        {
            converterId: 'SimpleMoving',
            tableMatches: () => false,
            create: simpleCreate,
            threeColor: false,
            hasRelatedBands: false,
            canGrid: false,
            maxPlots: 12,
            getSingleDataProduct: getSingleDataProductWrapper(makeRequestSimpleMoving),
            getGridDataProduct: () => Promise.reject('grid not supported'),
            getRelatedDataProduct: () => Promise.reject('related data products not supported')
        },
        {                            // this one should be last, it is the fallback
            converterId: DEFAULT_CONVERTER_ID,
            tableMatches: findADataSourceColumn,
            create: simpleCreate,
            threeColor: false,
            hasRelatedBands: false,
            canGrid: true,
            maxPlots: 3,
            getSingleDataProduct: makeAnalysisGetSingleDataProduct(makeRequestForUnknown),
            getGridDataProduct: makeAnalysisGetGridDataProduct(makeRequestForUnknown),
            getRelatedDataProduct: () => Promise.reject('related data products not supported')
        }
    ];


    const {factoryOverride} = getAppOptions()?.dataProducts ?? {};
    let converterTemplates = originalConverterTemplates;
    if (isArray(factoryOverride)) {
        converterTemplates = originalConverterTemplates.map((t) => {
            const overTemp = factoryOverride.find((tTmp) => tTmp.converterId === t.converterId);
            return overTemp ? {...t, ...overTemp} : t;
        });
    }
    return converterTemplates;
}





export const {getConverterTemplates, addTemplate, addTemplateToEnd, removeTemplate, removeAllButDefaultConverter}= (() => {
    let converterTemplates;

    const getConverterTemplates= () => {
        if (!converterTemplates) converterTemplates= initConverterTemplates();
        return converterTemplates;
    };
    const addTemplate= (template) => {
        getConverterTemplates();
        converterTemplates.unshift(template);
    };
    const addTemplateToEnd= (template) => {
        getConverterTemplates();
        converterTemplates.push(template);
    };
    const removeTemplate= (id) => {
        const originTemp= getConverterTemplates();
        converterTemplates= originTemp.filter( ({converterId}) => converterId!==id);
    };
    const removeAllButDefaultConverter= () => {
        const originTemp= getConverterTemplates();
        converterTemplates= originTemp.filter( ({converterId}) => converterId===DEFAULT_CONVERTER_ID);
    };

    return {getConverterTemplates,addTemplate,addTemplateToEnd,removeTemplate,removeAllButDefaultConverter};
})();



export function initImage3ColorDisplayManagement(viewerId) {
     const customEntry= getConverterTemplates().reduce( (newObj, template) => {
        if (!template.threeColor) return newObj;
        newObj[template.converterId]= {...template.threeColorBands, threeColorVisible:false};
        return newObj;
    }, {});
    dispatchUpdateCustom(viewerId, customEntry);
}



/**
 *
 * @param {TableModel} table
 * @return {DataProductsConvertType}
 */
export function defaultMakeDataProductsConverter(table) {
    const t= getConverterTemplates().find( (template) => template.tableMatches(table) );
    return t && t.create(table,t);
}
/**
 *
 * @param table
 * @param {TableModel} table
 * @return {DataProductsConvertType}
 */
let overrideMakeDataProductsConverter;

/**
 * get a convert factory for a table
 * @param {TableModel} table
 * @return {DataProductsConvertType}
 */
export const makeDataProductsConverter= (table) =>
    (overrideMakeDataProductsConverter && overrideMakeDataProductsConverter(table)) || defaultMakeDataProductsConverter(table);

export const setOverrideDataProductsConverterFactory = (f) => overrideMakeDataProductsConverter= f;

/**
 *  Support data the we don't know about
 * @param table
 * @param row
 * @param includeSingle
 * @param includeStandard
 * @return {{}}
 */
function makeRequestForUnknown(table, row, includeSingle, includeStandard) {

    const {tableMeta:meta}= table;

    const dataSource= findADataSourceColumn(table);
    if (!dataSource) return {};


    let positionWP= null;

    let sAry= meta[MetaConst.POSITION_COORD_COLS] && meta[MetaConst.POSITION_COORD_COLS].split(';');
    if (!sAry) sAry= meta[MetaConst.CENTER_COLUMN] && meta[MetaConst.CENTER_COLUMN].split(';');

    if (!isEmpty(sAry)) {
        const lon= Number(getCellValue(table,row,sAry[0]));
        const lat= Number(getCellValue(table,row,sAry[1]));
        const csys= CoordinateSys.parse(sAry[2]);
        positionWP= makeWorldPt(lon,lat,csys);
    }
    else if (meta[MetaConst.POSITION_COORD]) {
        positionWP= parseWorldPt(meta[MetaConst.POSITION_COORD]);
    }


    const retval= {};
    if (includeSingle) {
        retval.single= makeRequest(table,dataSource.name,positionWP, row);
    }
    
    if (includeStandard) {
        retval.standard= [makeRequest(table,dataSource.name,positionWP, row)];
        retval.highlightPlotId= retval.standard[0].getPlotId();
    }
    
    return retval;

}


function makeRequestSimpleMoving(table, row, includeSingle, includeStandard) {

    const {tableMeta:meta, tableData}= table;


    const dataSource= findADataSourceColumn(table);

    if (!dataSource) return {};


    const sAry= meta[MetaConst.POSITION_COORD_COLS].split(';');
    if (!sAry || sAry.length!== 3) return [];

    let positionWP= null;
    if (!isEmpty(sAry)) {
        const lon= Number(getCellValue(table,row,sAry[0]));
        const lat= Number(getCellValue(table,row,sAry[1]));
        const csys= CoordinateSys.parse(sAry[2]);
        positionWP= makeWorldPt(lon,lat,csys);
    }

    const retval= {};
    if (includeSingle) {
        retval.single= makeMovingRequest(table,row,dataSource.name,positionWP,'simple-moving-single-'+(row %24));
    }

    if (includeStandard) {
        retval.standard= [makeMovingRequest(table,row,dataSource.name,positionWP,'simple-moving-single')];
        retval.highlightPlotId= retval.standard[0].getPlotId();
    }

    return retval;

}


const defDataSourceGuesses= [ 'FILE', 'FITS', 'DATA', 'SOURCE', 'URL' ];

function findADataSourceColumn(table) {
    if (!table || table.isFetching) return false;
    const columns= get(table,'tableData.columns');
    if (!columns) return false;
    const dsCol= getDataSourceColumn(table);
    if (dsCol) return getColumn(table,dsCol);
    if (dsCol===false) return false;
    // if dsCol is undefined then start guessing
    const guesses= defDataSourceGuesses.map( (g) => g.toUpperCase());
    return columns.find( (c) => guesses.includes(c.name.toUpperCase()));
}

function findTableMetaEntry(table,ids) {
    const testIdAry= isArray(ids) ? ids : [ids];
    const id= testIdAry.find( (key) => getMetaEntry(table,key));
    if (!id) return;
    return getMetaEntry(table,id);
}



/**
 *
 * @param table
 * @param row
 * @param dataSource
 * @param positionWP
 * @param plotId
 * @return {*}
 */
function makeMovingRequest(table, row, dataSource, positionWP, plotId) {
    const url= getCellValue(table,row,dataSource);
    const r = WebPlotRequest.makeURLPlotRequest(url, 'Fits Image');
    r.setTitleOptions(TitleOptions.FILE_NAME);
    r.setZoomType(ZoomType.TO_WIDTH_HEIGHT);
    r.setPlotId(plotId);
    r.setInitialRangeValues(RangeValues.make2To10SigmaLinear());
    r.setOverlayPosition(positionWP);
    return r;

}


/**
 *
 * @param table
 * @param dataSource
 * @param positionWP
 * @param row
 * @return {*}
 */
function makeRequest(table, dataSource, positionWP, row) {
    if (!table || !dataSource) return null;

    let r;
    const source= getCellValue(table, row, dataSource);
    if (dataSource.toLocaleUpperCase() === FILE) {
        r = WebPlotRequest.makeFilePlotRequest(source, 'DataProduct');
    }
    else {
        r = WebPlotRequest.makeURLPlotRequest(source, 'DataProduct');
    }
    r.setZoomType(ZoomType.FULL_SCREEN);
    r.setTitleOptions(TitleOptions.FILE_NAME);
    r.setPlotId(source);
    if (positionWP) r.setOverlayPosition(positionWP);

    return r;
}

