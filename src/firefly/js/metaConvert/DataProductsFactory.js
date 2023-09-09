/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isArray, once} from 'lodash';
import {MetaConst} from '../data/MetaConst.js';
import {getMetaEntry} from '../tables/TableUtil';
import {hasObsCoreLikeDataProducts, hasServiceDescriptors} from '../util/VOAnalyzer.js';
import {Band} from '../visualize/Band';
import {SINGLE} from '../visualize/MultiViewCntlr';
import {makeAnalysisGetGridDataProduct, makeAnalysisGetSingleDataProduct} from './AnalysisUtils.js';
import {DEFAULT_DATA_PRODUCTS_COMPONENT_KEY} from './DataProductsCntlr.js';
import {dpdtImage} from './DataProductsType';
import {
    createGridImagesActivate, createRelatedDataGridActivate, createSingleImageActivate, createSingleImageExtraction
} from './ImageDataProductsUtil.js';
import {findADataSourceColumn, makeRequestForUnknown, makeRequestSimpleMoving} from './DefaultConverter.js';
import {makeAtlasPlotRequest} from './missions/AtlasRequestList.js';
import {makeShaPlotRequest, makeShaViewCreate} from './missions/ShaRequestList.js';
import {make2MassPlotRequest} from './missions/TwoMassRequestList.js';
import {makeWisePlotRequest, makeWiseViewCreate} from './missions/WiseRequestList.js';
import {makeZtfPlotRequest, makeZtfViewCreate} from './missions/ZtfRequestList.js';
import {
    getObsCoreDataProduct, getObsCoreGridDataProduct, getObsCoreRelatedDataProduct, makeObsCoreConverter
} from './vo/ObsCoreConverter.js';
import {
    getServiceDescGridDataProduct, getServiceDescRelatedDataProduct, getServiceDescSingleDataProduct,
    makeServDescriptorConverter
} from './vo/ServDescConverter.js';

export const DEFAULT_CONVERTER_ID= 'DEFAULT_CONVERTER';


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
    return async (table, row, activateParams) => {
        const {imageViewerId}= activateParams;
        const retVal= makeReq(table, row, true);
        const r= retVal?.single;
        const activate= createSingleImageActivate(r,imageViewerId,table.tbl_id,row);
        const extraction= createSingleImageExtraction(r);
        return  dpdtImage({name:'Image', activate, extraction});
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
    return async (table, plotRows, activateParams) => {
        const {imageViewerId}= activateParams;

        const reqAry= plotRows
            .map( (pR) => makeReq(table,pR.row,true)?.single)
            .filter( (r) => r);

        const activate= createGridImagesActivate(reqAry,imageViewerId,table.tbl_id,plotRows);
        const extraction= createSingleImageExtraction(reqAry);
        return  dpdtImage({name:'Image Grid', activate, extraction});
    };
}


/**
 *
 * @param makeReq
 * @return {Function}
 */
function getRelatedDataProductWrapper(makeReq) {
    return async (table, row, threeColorOps, highlightPlotId, activateParams) => {
        const {imageViewerId}= activateParams;
        const retVal= makeReq(table, row, false,true,threeColorOps);
        if (!retVal) return {};
        const activate= createRelatedDataGridActivate(retVal,imageViewerId,table.tbl_id, highlightPlotId ?? retVal.highlightPlotId);
        const extraction= retVal.standard ? createSingleImageExtraction(retVal.standard) : undefined;
        return  dpdtImage({name:'Images', activate, extraction} );
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
 * @prop {number} maxPlots total number of images that can be created at a time, i.e. page size
 * @prop {string} initialLayout - one of SINGLE, GRID, GRID_RELATED, GRID_FULL
 *
 * @prop {function(table:TableModel,row:String,activateParams:ActivateParams):function} getSingleDataProduct
 *  pass table,rowNum,activate params return a activate function
 *  required
 *
 * @prop {function(table:TableModel,plotRows:Array<{plotId:String,row:number,highlight:boolean}>,activateParams:ActivateParams):function} getGridDataProduct
 * pass(table, array of plot rows, and activateParams) return activate function.
 * Only required if canGrid is true
 *
 * @prop {function(table:TableModel,plotRows:Array<Object>,threeColorOps:Object, highlightPlotId:string,activateParams:ActivateParams):function} getRelatedDataProduct
 * pass (table, row, threeColorOps, highlightPlotId, activateParams) , return activate function.
 * Only required if hasRelatedBands is true

 @prop {Function} describeThreeColor
 *
 */


/**
 * @return {Array.<DataProductsConvertType>}
 */
function initConverterTemplates() {
    /**
     * @type {Array.<DataProductsConvertType>}
     */
    return [
        {
            converterId: 'wise',
            tableMatches: (table) => matchById(table, 'wise'),
            create: makeWiseViewCreate,
            describeThreeColor: undefined,
            getSingleDataProduct: getSingleDataProductWrapper(makeWisePlotRequest),
            getGridDataProduct: getGridDataProductWrapper(makeWisePlotRequest),
            getRelatedDataProduct: getRelatedDataProductWrapper(makeWisePlotRequest),
        },
        {
            converterId: 'sha',
            tableMatches: (table) => matchById(table, 'sha'),
            create: makeShaViewCreate,
            getSingleDataProduct: (table, row, activateParams, options) =>
                makeAnalysisGetSingleDataProduct(makeShaPlotRequest)(table, row, activateParams, options, 'spectrum'),
            getRelatedDataProduct: () => Promise.reject('related data products not supported'),
            getGridDataProduct: getGridDataProductWrapper(makeShaPlotRequest),
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
            describeThreeColor: async () => ({
                J: {color: Band.RED, title: 'J'},
                H: {color: Band.GREEN, title: 'H'},
                K: {color: Band.BLUE, title: 'K'}
            })
        },
        {
            converterId: 'ztf',
            tableMatches: (table) => matchById(table, 'ztf'),
            create: makeZtfViewCreate,
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
            getRelatedDataProduct: getObsCoreRelatedDataProduct,
        },
        {
            converterId: 'ServiceDescriptors',
            tableMatches: hasServiceDescriptors,
            create: makeServDescriptorConverter,
            threeColor: false,
            hasRelatedBands: false,
            canGrid: false,
            maxPlots: 1,
            getSingleDataProduct: getServiceDescSingleDataProduct,
            getGridDataProduct: getServiceDescGridDataProduct,
            getRelatedDataProduct: getServiceDescRelatedDataProduct,
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
}


export const getDefaultFactoryOptions= once(() => ({
    dataProductsComponentKey: DEFAULT_DATA_PRODUCTS_COMPONENT_KEY,
    allowImageRelatedGrid: false,
    allowServiceDefGrid: false,
    singleViewImageOnly:false,
    singleViewTableOnly:false,
    dataLinkInitialLayout: 'single', //determine how a datalink obscore table trys to show the data layout, must be 'single', 'gridRelated', 'gridFull';
    activateServiceDef: false,
    threeColor: undefined,
    maxPlots: undefined,
    canGrid: undefined, // some specific factories might have parameters that override this parameter (e.g. allowServiceDefGrid)
    initialLayout: undefined, //todo - an datalink use this?
    paramNameKeys: [],
    ucdKeys: [],
    utypeKeys: [],
}));


const ALL_TEMPLATES= {};
const FACTORY_OPTIONS= {};

/**
 * Return the list of all templates (DataProductsConvertType). This template list is unique by factory key. A converter is created from a template
 * entry. Each template has a 'create' funtion. Each template list can have its own set of options
 * @param factoryKey
 * @return {*}
 */
function getConverterTemplates(factoryKey='DEFAULT_FACTORY')  {
    if (!ALL_TEMPLATES[factoryKey]) ALL_TEMPLATES[factoryKey]= initConverterTemplates();
    return ALL_TEMPLATES[factoryKey];
}

/**
 * Each list of factory templates can have a unique set of options. This options will come from a MultiProductViewer when
 * it starts the DataProductsWatcher.
 * @param options
 * @param factoryKey
 */
export function setFactoryTemplateOptions(factoryKey='DEFAULT_FACTORY', options)  {
    FACTORY_OPTIONS[factoryKey]= options;
}

export function getFactoryTemplateOptions(factoryKey='DEFAULT_FACTORY') {
    return FACTORY_OPTIONS[factoryKey] ?? getDefaultFactoryOptions();
}

export function removeAllButSingleConverter(keepId,factoryKey)  {
    const originTemp= getConverterTemplates(factoryKey);
    ALL_TEMPLATES[factoryKey]= originTemp.filter( ({converterId}) => converterId===keepId);
}

/**
 * get a DataProductsConvertType for a table. The converter is determined by the type of table. Each converter has
 * a table matches funtion. If a match occurs then the create function is called with that table and the results
 * are returned.
 * @param {TableModel} table
 * @param {string} factoryKey - which factory to use
 * @return {DataProductsConvertType}
 */
export function makeDataProductsConverter(table, factoryKey= undefined) {
    const t= getConverterTemplates(factoryKey).find( (template) => template.tableMatches(table) );
    if (!t) return;
    const options= getFactoryTemplateOptions(factoryKey);
    // most options are specific to a factory but these below are common to all
    const pT= {
        ...t,
        canGrid: options.canGrid ?? t.canGrid ?? false,
        maxPlots: options.maxPlots ?? t.maxPlots ?? 1,
        initialLayout: options.initialLayout ?? t.initialLayout ?? SINGLE,
        threeColor: options.threeColor ?? t.threeColor ?? false,
    };
    const retObj= t.create(table,pT, options);
    return {options, ...retObj};
        
}


function findTableMetaEntry(table,ids) {
    const testIdAry= isArray(ids) ? ids : [ids];
    const id= testIdAry.find( (key) => getMetaEntry(table,key));
    if (!id) return;
    return getMetaEntry(table,id);
}
