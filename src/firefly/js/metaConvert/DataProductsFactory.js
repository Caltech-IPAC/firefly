/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {isArray, isUndefined, once} from 'lodash';
import {MetaConst} from '../data/MetaConst.js';
import {getMetaEntry, getObjectMetaEntry} from '../tables/TableUtil';
import {getDataServiceOptionByTable} from '../ui/tap/DataServicesOptions';
import {toBoolean} from '../util/WebUtil';
import {Band} from '../visualize/Band';
import {SINGLE} from '../visualize/MultiViewCntlr';
import {hasObsCoreLikeDataProducts, isDatalinkTable} from '../voAnalyzer/TableAnalysis.js';
import {hasServiceDescriptors} from '../voAnalyzer/VoDataLinkServDef.js';
import {makeAnalysisGetGridDataProduct, makeAnalysisGetSingleDataProduct} from './AnalysisUtils.js';
import {DEFAULT_CONVERTER_ID, DEFAULT_DATA_PRODUCTS_COMPONENT_KEY, NO_LIMIT} from './DataProductConst';
import {dpdtImage} from './DataProductsType';
import {findADataSourceColumn, makeRequestForUnknown} from './DefaultConverter.js';
import {
    createGridImagesActivate, createRelatedDataGridActivate, createSingleImageActivate, createSingleImageExtraction
} from './ImageDataProductsUtil.js';
import {makeAtlasPlotRequest} from './missions/AtlasRequestList.js';
import {makeShaPlotRequest, makeShaViewCreate} from './missions/ShaRequestList.js';
import {make2MassPlotRequest} from './missions/TwoMassRequestList.js';
import {makeWisePlotRequest, makeWiseViewCreate} from './missions/WiseRequestList.js';
import {makeZtfPlotRequest, makeZtfViewCreate} from './missions/ZtfRequestList.js';
import {getDatalinkStandAlineDataProduct, makeDatalinkStaneAloneConverter} from './vo/DataLinkStandAloneConverter';
import {
    getObsCoreDataProduct, getObsCoreGridDataProduct, getObsCoreRelatedDataProduct, makeObsCoreConverter,
    OBSCORE_DEF_MAX_PLOTS
} from './vo/ObsCoreConverter.js';
import {
    getServiceDescGridDataProduct, getServiceDescRelatedDataProduct, getServiceDescSingleDataProduct,
    makeServDescriptorConverter
} from './vo/ServDescConverter.js';


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
            threeColor: false,
        },
        {
            converterId: 'sha',
            tableMatches: (table) => matchById(table, 'sha'),
            create: makeShaViewCreate,
            getSingleDataProduct: (table, row, activateParams, options) =>
                makeAnalysisGetSingleDataProduct(makeShaPlotRequest)(table, row, activateParams, options, 'spectrum'),
            getRelatedDataProduct: () => Promise.reject('related data products not supported'),
            getGridDataProduct: getGridDataProductWrapper(makeShaPlotRequest),
            threeColor: false,
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
            threeColor: false,
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
            threeColor: false,
        },
        {
            converterId: 'DataLinkStandaloneTable',
            tableMatches: isDatalinkTable,
            create: makeDatalinkStaneAloneConverter,
            threeColor: false,
            hasRelatedBands: false,
            canGrid: false,
            maxPlots: 1,
            getSingleDataProduct: getDatalinkStandAlineDataProduct,
            getGridDataProduct: () => Promise.reject('grid not supported'),
            getRelatedDataProduct: () => Promise.reject('related data products not supported')
        },
        {
            converterId: 'ObsCore',
            tableMatches: hasObsCoreLikeDataProducts,
            create: makeObsCoreConverter,
            threeColor: undefined,
            hasRelatedBands: undefined,
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
            threeColor: undefined,
            hasRelatedBands: undefined,
            canGrid: undefined,
            maxPlots: OBSCORE_DEF_MAX_PLOTS,
            getSingleDataProduct: getServiceDescSingleDataProduct,
            getGridDataProduct: getServiceDescGridDataProduct,
            getRelatedDataProduct: getServiceDescRelatedDataProduct,
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


/**
 * @typedef {Object} DataProductsFactoryOptions
 *
 * @prop {string} [limitViewerDisplay] - can be 'IMAGE_ONLY', 'TABLE_ONLY', defaults to 'NO_LIMIT';
 * @prop {boolean} [activateServiceDef] - if possible then call the service def without giving option for user input
 * @prop {boolean} [threeColor] - if true, then for images in related grid, show the threeColor option
 * @prop {number} [maxPlots] - maximum number of plots in grid mode, this will override the factory default
 * @prop {boolean} [canGrid] - some specific factories might have parameters that override this parameter
 * @prop {string} [tableIdBase] - any tbl_id will use this string for its base
 * @prop {string} [chartIdBase] - any chartId will use this string for its base
 * @prop {string} [tableIdList] - an array of table object that will be used instead of generating a table id. form: {description,tbl_id}
 * @prop {string} [chartIdList] - an array of table ids that will be used instead of generating a chart id: {description,chartId}
 * @prop {string} [initialLayout] - one of 'single', 'gridRelated', 'gridFull'
 * @prop {boolean} hasRelatedBands
 * @prop {boolean} datalinkDisableMoreDrop - if true then use only the first this and don't show the more dropdown
 * @prop {string} [dataProductsComponentKey] - this is the key use when calling getComponentState() to get a key,value object.
 *                The values in this object will override one or more parameters to a service descriptor.
 *                The following are used with this prop by service descriptors to build the url to include input from the UI.
 *                see- ServDescProducts.js getComponentInputs()
 * @prop {object} datalinkTblRequestOptions = add addition options to table request
 * @prop {Array.<string>} [paramNameKeys] - name of the parameters to put in the url from the getComponentState() return object
 * @prop {Array.<string>} [ucdKeys] - same as above but can be specified by UCD
 * @prop {Array.<string>} [utypeKeys] - same as above but can be specified by utype
   @prop {String} statefulTabComponentKey
 */

/**
 * @return {DataProductsFactoryOptions}
 */
export const getDefaultFactoryOptions= once(() => ({
    dataProductsComponentKey: DEFAULT_DATA_PRODUCTS_COMPONENT_KEY,
    limitViewerDisplay: NO_LIMIT,
    activateServiceDef: undefined,
    threeColor: undefined,
    maxPlots: undefined,
    canGrid: undefined, // some specific factories might have parameters that override this parameter
    initialLayout: undefined,
    tableIdBase: undefined,
    chartIdBase: undefined,
    tableIdList: [], // list of ids
    chartIdList: [],// list of ids
    relatedGridImageOrder: undefined,
    datalinkTblRequestOptions: {},
    paramNameKeys: [], // experimental - might be used with obscure cutout services, for not unnecessary, I will probably remove
    ucdKeys: [], // experimental - might be used with obscure cutout services, for not unnecessary, I will probably remove
    utypeKeys: [], // experimental - might be used with obscure cutout services, for not unnecessary, I will probably remove
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
 * @param factoryKey
 * @param options
 */
export function setFactoryTemplateOptions(factoryKey='DEFAULT_FACTORY', options)  {
    FACTORY_OPTIONS[factoryKey]= {...getDefaultFactoryOptions(), ...options};
}

export function getFactoryTemplateOptions(factoryKey) {
    const options=  FACTORY_OPTIONS[factoryKey??'DEFAULT_FACTORY'] ?? getDefaultFactoryOptions();
    if (factoryKey && options.dataProductsComponentKey===DEFAULT_DATA_PRODUCTS_COMPONENT_KEY) {
        options.dataProductsComponentKey= factoryKey;
    }
    return options;
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
    const inTemplate= getConverterTemplates(factoryKey).find( (template) => template.tableMatches(table) );
    if (!inTemplate) return;
    const defaultsObs= { maxPlots: 1, initialLayout: SINGLE};
    const t= {...defaultsObs,...inTemplate};
    const options= getFactoryTemplateOptions(factoryKey);
    // most options are specific to a factory but these below are common to all

    const metaOptions= getObjectMetaEntry(table, MetaConst.DATA_PRODUCTS_FACTORY_OPTIONS, {});
    const dataServiceOptions= getDataServiceOptionByTable(MetaConst.DATA_PRODUCTS_FACTORY_OPTIONS, table, {});
    const combinedOps= {dataProductsComponentKey: factoryKey, ...options, ...dataServiceOptions, ...metaOptions};

    const pT= {
        ...t,
        canGrid: asBooleanOrUndefined(combinedOps.canGrid ?? t.canGrid),
        maxPlots: Number(combinedOps.maxPlots ?? t.maxPlots),
        initialLayout: combinedOps.initialLayout ?? t.initialLayout,
        threeColor: asBooleanOrUndefined(combinedOps.threeColor ?? t.threeColor),
        hasRelatedBands: asBooleanOrUndefined(combinedOps.hasRelatedBands?? t.hasRelatedBands),
        dataProductsComponentKey: combinedOps.dataProductsComponentKey,
        relatedGridImageOrder: combinedOps.relatedGridImageOrder,
    };
    const retObj= t.create(table,pT, combinedOps);
    return {options:combinedOps, ...retObj};
}

const asBooleanOrUndefined= (v) => isUndefined(v) ? undefined : toBoolean(v);

function findTableMetaEntry(table,ids) {
    const testIdAry= isArray(ids) ? ids : [ids];
    const id= testIdAry.find( (key) => getMetaEntry(table,key));
    if (!id) return;
    return getMetaEntry(table,id);
}
