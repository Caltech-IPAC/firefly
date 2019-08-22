/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */


import {get, isEmpty, isArray} from 'lodash';
import {makeWisePlotRequest} from './WiseRequestList.js';
import {make2MassPlotRequest} from './TwoMassRequestList.js';
import {makeAtlasPlotRequest} from './AtlasRequestList.js';
import {makeLsstSdssPlotRequest, makeLsstWisePlotRequest} from './LsstSdssRequestList.js';
import {WebPlotRequest, TitleOptions} from '../visualize/WebPlotRequest.js';
import {ZoomType} from '../visualize/ZoomType.js';
import {Band} from '../visualize/Band';
import {getCellValue} from '../tables/TableUtil.js';
import {makeWorldPt, parseWorldPt} from '../visualize/Point.js';
import {MetaConst} from '../data/MetaConst.js';
import {CoordinateSys} from '../visualize/CoordSys.js';
import {hasObsCoreLikeDataProducts} from '../util/VOAnalyzer.js';
import {dispatchUpdateCustom} from '../visualize/MultiViewCntlr.js';
import {makeObsCoreConverter, getObsCoreSingleDataProduct, getObsCoreGridDataProduct} from './ObsCoreConverter.js';
import {
    createGridImagesActivate,
    createRelatedDataGridActivate,
    createSingleImageActivate
} from './ImageDataProductsUtil';

const FILE= 'FILE';


function matchById(table,id)  {
    if (!id) return false;
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
 * @typedef {Object} DataProductsDisplayType
 * @prop {string} displayType
 * @prop {Function} activate
 * @prop {Object} menu
 *
 */

/**
 * @global
 * @public
 * @typedef {Object} ActivateParams
 * @prop {string} imageViewerId
 * @prop {string} chartViewerId
 * @prop {string} tableGroupViewerId
 * @prop {string} converterId
 *
 */


/**
 * Returns a callback function or a Promise<DataProductsDisplayType>.
 * callback: function(table:TableModel, row:String,activeParams:{imageViewerId:String,chartViewId:String,tableViewId:String,converterId:String})
 * @param makeReq
 * @return {function | promise}
 */
function getSingleDataProductWrapper(makeReq) {
    return (table, row, activateParams) => {
        const {imageViewerId, converterId}= activateParams;
        const retVal= makeReq(table, row, true);
        const r= get(retVal,'single');
        const activate= createSingleImageActivate(r,imageViewerId,converterId,table.tbl_id,row);
        return Promise.resolve( { displayType:'images', activate, menu:undefined });
    };
}


/**
 *
 * Returns a callback function or a Promise<DataProductsDisplayType>.
 * callback: function(table:TableModel, plotRows:Array.<Object>,activeParams:{imageViewerId:String,chartViewId:String,tableViewId:String,converterId:String})
 * @param makeReq
 * @return {function | promise}
 */
function getGridDataProductWrapper(makeReq) {
    return (table, plotRows, activateParams) => {
        const {imageViewerId, converterId}= activateParams;

        const reqAry= plotRows.map( (pR) => makeReq(table,pR.row,true))
            .filter( (r) => (r && r.single))
            .map( (result) => result.single);

        const activate= createGridImagesActivate(reqAry,imageViewerId,converterId,table.tbl_id,plotRows);
        return Promise.resolve( { displayType:'images', activate, menu:undefined });
    };
}


/**
 *
 * @param makeReq
 * @return {Function}
 */
function getRelatedDataProductWrapper(makeReq) {
    return (table, row, threeColorOps, highlightPlotId, activateParams) => {
        const {imageViewerId, converterId}= activateParams;
        const retVal= makeReq(table, row, false,true,threeColorOps);
        if (retVal) {
            const activate= createRelatedDataGridActivate(retVal,imageViewerId,converterId,table.tbl_id, highlightPlotId);
            return Promise.resolve( { displayType:'images', activate, menu:undefined });
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


/**
 * @type {Array.<DataProductsConvertType>}
 */
export const converterTemplates = [
    {
        converterId : 'wise',
        tableMatches: (table) => matchById(table,'wise'),
        create : simpleCreate,
        threeColor : true,
        hasRelatedBands : true,
        canGrid : true,
        maxPlots : 12,
        getSingleDataProduct: getSingleDataProductWrapper(makeWisePlotRequest),
        getGridDataProduct: getGridDataProductWrapper(makeWisePlotRequest),
        getRelatedDataProduct: getRelatedDataProductWrapper(makeWisePlotRequest),
        threeColorBands : {
            b1 : {color : Band.RED, title: 'Band 1'},
            b2 : {color : Band.GREEN, title: 'Band 2'},
            b3 : {color : null, title: 'Band 3'},
            b4 : {color : Band.BLUE, title: 'Band 4'}
        },
    },
    {
        converterId : 'atlas',
        tableMatches: (table) => matchById(table,'atlas'),
        create : simpleCreate,
        hasRelatedBands : true,
        canGrid : true,
        maxPlots : 5,
        getSingleDataProduct: getSingleDataProductWrapper(makeAtlasPlotRequest),
        getGridDataProduct: getGridDataProductWrapper(makeAtlasPlotRequest),
        getRelatedDataProduct: getRelatedDataProductWrapper(makeAtlasPlotRequest),
    },
    {
        converterId : 'twomass',
        tableMatches: (table) => matchById(table,'twomass'),
        create : simpleCreate,
        threeColor : true,
        hasRelatedBands : true,
        canGrid : true,
        maxPlots : 12,
        getSingleDataProduct: getSingleDataProductWrapper(make2MassPlotRequest),
        getGridDataProduct: getGridDataProductWrapper(make2MassPlotRequest),
        getRelatedDataProduct: getRelatedDataProductWrapper(make2MassPlotRequest),
        threeColorBands : {
            J : {color : Band.RED, title: 'J'},
            H : {color : Band.GREEN, title: 'H'},
            K : {color : Band.BLUE, title: 'K'}
        }
    },
    {
        converterId : 'lsst_sdss',
        tableMatches: (table) => matchById(table,'lsst_sdss'),
        create : simpleCreate,
        threeColor : true,
        hasRelatedBands : true,
        canGrid : true,
        maxPlots : 12,
        getSingleDataProduct: getSingleDataProductWrapper(makeLsstSdssPlotRequest),
        getGridDataProduct: getGridDataProductWrapper(makeLsstSdssPlotRequest),
        getRelatedDataProduct: getRelatedDataProductWrapper(makeLsstSdssPlotRequest),
        threeColorBands : {
            u : {color : null, title: 'u'},
            g : {color : Band.RED, title: 'g'},
            r : {color : Band.GREEN, title: 'r'},
            i : {color : null,  title: 'i'},
            z : {color : Band.BLUE, title: 'z'}
        }
    },
    {
        converterId : 'lsst_wise',
        tableMatches: (table) => matchById(table,'lsst_wise'),
        create : simpleCreate,
        threeColor : true,
        hasRelatedBands : true,
        canGrid : true,
        maxPlots : 12,
        getSingleDataProduct: getSingleDataProductWrapper(makeLsstWisePlotRequest),
        getGridDataProduct: getGridDataProductWrapper(makeLsstWisePlotRequest),
        getRelatedDataProduct: getRelatedDataProductWrapper(makeLsstWisePlotRequest),
        threeColorBands : {
            b1 : {color : Band.RED, title: 'Band 1'},
            b2 : {color : Band.GREEN, title: 'Band 2'},
            b3 : {color : null, title: 'Band 3'},
            b4 : {color : Band.BLUE, title: 'Band 4'}
        }
    },
    {
        converterId : 'ObsCore',
        tableMatches: hasObsCoreLikeDataProducts,
        create : makeObsCoreConverter,
        threeColor : false,
        hasRelatedBands : false,
        canGrid : true,
        maxPlots : 8,
        getSingleDataProduct: getObsCoreSingleDataProduct,
        getGridDataProduct: getObsCoreGridDataProduct,
        getRelatedDataProduct: () => Promise.reject('related data products not supported')
    },
    {
        converterId : 'UNKNOWN',
        tableMatches: (table) => !isEmpty(Object.keys(findADataSourceColumn(table.tableMeta,table.tableData.columns))),
        create : simpleCreate,
        threeColor : false,
        hasRelatedBands : false,
        canGrid : true,
        maxPlots : 12,
        getSingleDataProduct: getSingleDataProductWrapper(makeRequestForUnknown),
        getGridDataProduct: () => Promise.reject('grid not supported'),
        getRelatedDataProduct: () => Promise.reject('related data products not supported')
    },
    {
        converterId : 'SimpleMoving',
        tableMatches: () => false,
        create : simpleCreate,
        threeColor : false,
        hasRelatedBands : false,
        canGrid : false,
        maxPlots : 12,
        getSingleDataProduct: getSingleDataProductWrapper(makeRequestSimpleMoving),
        getGridDataProduct: () => Promise.reject('grid not supported'),
        getRelatedDataProduct: () => Promise.reject('related data products not supported')
    }
];


export function initImage3ColorDisplayManagement(viewerId) {
     const customEntry= converterTemplates.reduce( (newObj, template) => {
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
    const t= converterTemplates.find( (template) => template.tableMatches(table) );
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


export function addTemplate(template) { converterTemplates.unshift(template); }

export function addTemplateToEnd(template) { converterTemplates.push(template); }


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

    const dataSource= findADataSourceColumn(meta,table.tableData.columns);
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


    const dataSource= findADataSourceColumn(meta, tableData.columns);

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
const dataSourceUpper= MetaConst.DATA_SOURCE.toUpperCase();

function findADataSourceColumn(meta,columns) {
    const dsCol= Object.keys(meta).find( (key) => key.toUpperCase()===dataSourceUpper);
    let guesses= meta[dsCol] ? [meta[dsCol],...defDataSourceGuesses] : defDataSourceGuesses;
    guesses= guesses.map( (g) => g.toUpperCase());
    return columns.find( (c) => guesses.includes(c.name.toUpperCase())) || {};
}

function findTableMetaEntry(table,ids) {
    const testIdAry= isArray(ids) ? ids.map( (id) => id.toUpperCase()) : [ids.toUpperCase()];
    const foundKey= Object.keys(table.tableMeta)
        .find( (key) => testIdAry
            .find( (t) => t===key.toUpperCase()));
    return foundKey ? table.tableMeta[foundKey] : undefined;
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
        r = WebPlotRequest.makeFilePlotRequest(source, 'Fits Image');
    }
    else {
        r = WebPlotRequest.makeURLPlotRequest(source, 'Fits Image');
    }
    r.setZoomType(ZoomType.FULL_SCREEN);
    r.setTitleOptions(TitleOptions.FILE_NAME);
    r.setPlotId(source);
    if (positionWP) r.setOverlayPosition(positionWP);

    return r;
}
