import {get, isArray, isObject} from 'lodash';
import {REINIT_APP} from '../core/AppDataCntlr';
import {flux} from '../core/ReduxFlux';
import {download} from '../util/fetch';
import {encodeUrl, getRootURL} from '../util/WebUtil';
import {dpdtMessage, DPtypes} from './DataProductsType';


export const DATA_PRODUCTS_KEY= 'dataProducts';
const PREFIX= 'DataProductCntlr';
export const DATA_PRODUCT_ID_PREFIX= 'DPC';
export const INIT_DATA_PRODUCTS= `${PREFIX}.InitDataProducts`;
export const UPDATE_DATA_PRODUCTS= `${PREFIX}.UpdateDataProducts`;
export const UPDATE_ACTIVE_KEY= `${PREFIX}.UpdateActiveKey`;
export const ACTIVATE_MENU_ITEM= `${PREFIX}.ActivateMenuItem`;
export const ACTIVATE_FILE_MENU_ITEM= `${PREFIX}.ActivateFileMenuItem`;
export const SET_SEARCH_PARAMS= `${PREFIX}.SetSearchParams`;
export const SET_SERVICE_DESC_ACTIVATE_STATUS= `${PREFIX}.SetServiceDescActiveStatus`;

export function dataProductRoot() { return flux.getState()[DATA_PRODUCTS_KEY]; }

function reducers() {
    return {
        [DATA_PRODUCTS_KEY]: reducer,
    };
}

function actionCreators() {
    return {
        [ACTIVATE_MENU_ITEM]: activateMenuItemActionCreator,
        [ACTIVATE_FILE_MENU_ITEM]: activateFileMenuItemActionCreator
    };
}


const ACTIVATE_REQUIRED= [DPtypes.IMAGE,DPtypes.TABLE,DPtypes.CHART,DPtypes.ANALYZE];

/**
 * @param {Action} rawAction
 * @returns {Function}
 */
function activateMenuItemActionCreator(rawAction) {
    return (dispatcher, getState) => {
        const {dpId,menuKey}= rawAction.payload;
        const menu= get(getDataProducts(getState()[DATA_PRODUCTS_KEY],dpId),'menu');
        if (!menu) return;
        const menuItem= menu.find( (m) => m.menuKey===menuKey);
        if (!menuItem) return;
        if (menuItem.displayType===DPtypes.EXTRACT) doExtract(menuItem);
        else dispatcher(rawAction);
    };
}

function activateFileMenuItemActionCreator(rawAction) {
    return (dispatcher) => {
        dispatcher(rawAction);
    };
}

function doExtract(menuItem) {
    menuItem?.activate();
}

export function doDownload(url) {
    const serviceURL = getRootURL() + 'servlet/Download';
    const tmpUrl= url.toLowerCase();
    if (tmpUrl.startsWith('http') || tmpUrl.startsWith('ftp')) {
        const params={externalURL: url};
        download(encodeUrl(serviceURL, params));
    }
    else if (tmpUrl.startsWith('${')) {
        const serviceURL = getRootURL() + 'servlet/Download';
        const params={file: url};
        download(encodeUrl(serviceURL, params));
    }
}




/**
 *
 * @typedef {Object} Viewer
 * @prop {string} dpId
 * @prop {DataProductsDisplayType} dataProducts
 * @prop {Object} serviceDescriptorsActivateStatus
 * @prop {Object} activeFileMenuKeys - key serialized request, value - activeFileMenuKey, only used with containerType:WRAPPER
 * @prop {Object} activeMenuKeys - key serialized request, value - activeFileMenuKey, last active menu key, only used with containerType:WRAPPER
 * @prop {Array.<{activeMenuLookupKey:string,menuKey:string,params:Object}>} serviceParamsAry
 *
 * @global
 * @public
 */


function initState() {

    return [
        {
            dpId: 'TEMPLATE',
            dataProducts: {},
            activeFileMenuKeys: {},
            activeMenuKeys: {},
            serviceDescriptorsActivateStatus:{},
            activateParams: {
                imageViewerId: DATA_PRODUCT_ID_PREFIX+'-image-0',
                tableGroupViewerId: DATA_PRODUCT_ID_PREFIX+'-table-0',
                chartViewerId: DATA_PRODUCT_ID_PREFIX+'-chart-0'
            },
            serviceParamsAry: []
        }
    ];
}

export default {
    reducers, actionCreators,
};

/**
 * Create and init a new data products id if it does not exist, otherwise no nothing.
 * @param dpId
 */
export function dispatchInitDataProducts(dpId) {
    flux.process({type: INIT_DATA_PRODUCTS, payload: {dpId} });
}


/**
 *
 * @param dpId
 * @param {Object} dataProducts
 */
export function dispatchUpdateDataProducts(dpId, dataProducts) {
    flux.process({type: UPDATE_DATA_PRODUCTS, payload: {dpId,dataProducts} });
}

/**
 * Update the activeMenuKeys and/or the one or more of the activeFileMenuKey's
 * @param {Object} p
 * @param  p.dpId - data products id
 * @param {Object} [p.activeMenuKeyChanges] - change the activeMenuItemKey
 *     this is a map of a cacheKey and the related activeFileMenuKey
 * @param {Object} [p.activeFileMenuKeyChanges] - changes to the activeFileMenuKey's,
 *     this is a map of a cacheKey and the related activeFileMenuKey
 */
export function dispatchUpdateActiveKey({dpId, activeMenuKeyChanges, activeFileMenuKeyChanges}) {
    flux.process({type: UPDATE_ACTIVE_KEY, payload: {dpId,activeMenuKeyChanges, activeFileMenuKeyChanges} });
}

export function dispatchActivateMenuItem(dpId, menuKey) {
    flux.process({type: ACTIVATE_MENU_ITEM, payload: {dpId,menuKey} });
}

/**
 *
 * @param {Object} obj
 * @param {string} obj.dpId
 * @param {string} [obj.menuKey]
 * @param {string} [obj.activeMenuLookupKey]
 * @param {Object} [obj.autoActiveStatus]
 * @param {Object|undefined} obj.params
 */
export function dispatchSetSearchParams({dpId,activeMenuLookupKey,menuKey,params,autoActiveStatus}) {
    flux.process({type: SET_SEARCH_PARAMS, payload: {dpId,activeMenuLookupKey,menuKey,params,autoActiveStatus} });
}

/**
 * @param p
 * @param {String} p.dpId
 * @param {DataProductsFileMenu} p.fileMenu
 * @param {String|undefined} [p.newActiveFileMenuKey]
 * @param {String} [p.currentMenuKey]
 * @param {Array.<DataProductsDisplayType>|undefined} [p.menu]
 */
export function dispatchActivateFileMenuItem({dpId,fileMenu, newActiveFileMenuKey, menu, currentMenuKey}) {
    flux.process({type: ACTIVATE_FILE_MENU_ITEM, payload: {dpId,fileMenu, newActiveFileMenuKey, currentMenuKey, menu} });
}


export const isInitDataProducts= (dpRoot,dpId) => Boolean(dpRoot.find( (dpContainer) => dpContainer.dpId===dpId));

export const getDataProducts= (dpRoot,dpId) => createOrFind(dpRoot,dpId).dataProducts;
export const getActivateParams= (dpRoot,dpId) => createOrFind(dataProductRoot(),dpId).activateParams;
export const getServiceParamsAry= (dpRoot,dpId) => createOrFind(dpRoot,dpId).serviceParamsAry;

export const getActiveFileMenuKey= (dpId,fileMenu) =>
    createOrFind(dataProductRoot(),dpId).activeFileMenuKeys[fileMenu.activeItemLookupKey];


export const getActiveFileMenuKeyByKey= (dpId,key) => createOrFind(dataProductRoot(),dpId).activeFileMenuKeys[key];


export const getActiveMenuKey= (dpId,activeMenuLookupKey) =>
    createOrFind(dataProductRoot(),dpId).activeMenuKeys[activeMenuLookupKey];

export const getCurrentActiveKeyID= (dpId) => createOrFind(dataProductRoot(),dpId).currentActiveKeyID ?? '';

/**
 *
 * @param dpId
 * @param internalServiceDescriptorID
 * @return {Boolean|undefined}
 */
export const isServiceDescriptorActivated= (dpId, internalServiceDescriptorID) =>
    createOrFind(dataProductRoot(),dpId).serviceDescriptorsActivateStatus[internalServiceDescriptorID];

export function getSearchParams(serviceParamsAry,activeMenuLookupKey,menuKey)  {
    return serviceParamsAry?.find( (obj) => obj.activeMenuLookupKey===activeMenuLookupKey && obj.menuKey===menuKey)?.params;
}


function reducer(state=initState(), action={}) {

    if (!action.payload || !action.type) return state;
    let retState= state;

    switch (action.type) {
        case INIT_DATA_PRODUCTS:
            retState= initDataProducts(state,action);
            break;
        case UPDATE_DATA_PRODUCTS:
            retState= updateDataProducts(state,action);
            break;
        case UPDATE_ACTIVE_KEY:
            retState= updateActiveKey(state,action);
            break;
        case ACTIVATE_MENU_ITEM:
            retState= activateMenuItem(state,action);
            break;
        case ACTIVATE_FILE_MENU_ITEM:
            retState= changeActiveFileMenuItem(state,action);
            break;
        case SET_SEARCH_PARAMS:
            retState= setSearchParams(state,action);
            break;
        case SET_SERVICE_DESC_ACTIVATE_STATUS:
            retState= setServiceDescActivateStatus(state,action);
            break;
        case REINIT_APP:
            retState= initState();
            break;
        default:
            break;

    }
    return retState;
}

let activateCnt=0;

const makeNewDPData= (dpId) => {
    activateCnt++;
    return { dpId,
        dataProducts: {},
        activeFileMenuKeys: {},
        activeMenuKeys: {},
        serviceDescriptorsActivateStatus:{},
        activateParams: {
            imageViewerId:`${DATA_PRODUCT_ID_PREFIX}-image-${activateCnt}`,
            tableGroupViewerId:`${DATA_PRODUCT_ID_PREFIX}-table-${activateCnt}`,
            chartViewerId:`${DATA_PRODUCT_ID_PREFIX}-chart-${activateCnt}`,
            dpId,
        },
        serviceParamsAry: []
    };
};


function setSearchParams(state,action) {
    const {dpId,activeMenuLookupKey,menuKey,params, autoActiveStatus}= action.payload;
    let serviceParamsAry;
    const dpData= state.find( (dpContainer) => dpContainer.dpId===dpId );
    if (!dpData) return state;

    if (autoActiveStatus) {
        dpData.serviceDescriptorsActivateStatus= {...dpData.serviceDescriptorsActivateStatus, ...autoActiveStatus};
    }

    if (activeMenuLookupKey) {
        if (dpData.serviceParamsAry?.find( (obj) => obj.activeMenuLookupKey===activeMenuLookupKey && obj.menuKey===menuKey)) {
            serviceParamsAry= dpData.serviceParamsAry.map( (obj) =>
                obj.activeMenuLookupKey===activeMenuLookupKey &&
                obj.menuKey===menuKey ? {dpId,activeMenuLookupKey,menuKey,params} : obj);
        }
        else {
            serviceParamsAry= [...(dpData.serviceParamsAry??[]), {dpId,activeMenuLookupKey,menuKey,params}];
        }
    }
    return insertOrReplace(state,{...dpData,serviceParamsAry});
}

function createOrFindAndCopy(state,dpId) {
    const dp= state.find( (dpContainer) => dpContainer.dpId===dpId );
    return dp ? {...dp} : makeNewDPData(dpId);
}

function createOrFind(state,dpId) {
    return state.find( (dpContainer) => dpContainer.dpId===dpId ) || makeNewDPData(dpId);
}


function insertOrReplace(state,dpData) {
    const {dpId}= dpData;
    const found= Boolean(state.find( (dpContainer) => dpContainer.dpId===dpId ));
    return found ? state.map( (d) => d.dpId===dpId ? dpData : d) : [...state,dpData];
}

function initDataProducts(state,action) {
    const {dpId}= action.payload;
    const found= Boolean(state.find( (dpContainer) => dpContainer.dpId===dpId ));
    return found ? state : [...state,makeNewDPData(dpId)];
}

function updateDataProducts(state,action) {
    const {dpId, dataProducts}= action.payload;
    const dpData = createOrFindAndCopy(state,dpId);
    dpData.dataProducts= dataProducts;
    return insertOrReplace(state,dpData);
}

function updateActiveKey(state,action) {
    const {dpId, activeMenuKeyChanges, activeFileMenuKeyChanges}= action.payload;
    const dpData= createOrFindAndCopy(state,dpId);
    if (isObject(activeMenuKeyChanges)) {
        dpData.activeMenuKeys= {...dpData.activeMenuKeys,...activeMenuKeyChanges};
        dpData.currentActiveKeyID= Object.keys(activeMenuKeyChanges)?.[0];
    }
    if (isObject(activeFileMenuKeyChanges)) dpData.activeFileMenuKeys= {...dpData.activeFileMenuKeys,...activeFileMenuKeyChanges};
    return insertOrReplace(state,dpData);
}

function setServiceDescActivateStatus(state,action) {
    const {internalServiceDescriptorID,isActivated,dpId}= action.payload;
    if (!internalServiceDescriptorID) return state;
    const dpData= createOrFindAndCopy(state,dpId);
    dpData.serviceDescriptorsActivateStatus=
        {...dpData.serviceDescriptorsActivateStatus, [internalServiceDescriptorID]:isActivated };
    return insertOrReplace(state,dpData);
}


function activateMenuItem(state,action) {
    const {dpId,menuKey}= action.payload;
    const dpData= createOrFind(state,dpId);
    const {dataProducts,activeMenuKeys}= dpData;
    const {menu, activeMenuKey, activeMenuLookupKey}= dataProducts;

    if (!menu) return state;
    const menuItem= menu.find( (m) => m.menuKey===menuKey);
    if (!menuItem) return state;

    const {fileMenu}= menuItem;
    let aMenuItem;

    if (menuItem.menuKey===activeMenuKey) return state;

    if (fileMenu && isArray(fileMenu.menu)) {
        const activeFileMenuKey= getActiveFileMenuKey(dpId,fileMenu);
        aMenuItem= fileMenu.menu.find( (m) => m.menuKey===activeFileMenuKey);
        if (!aMenuItem) aMenuItem= fileMenu.menu[0];
    }
    else {
        aMenuItem= menuItem;
    }
    dpData.activeMenuKeys= {...activeMenuKeys, [activeMenuLookupKey]:menuKey};

    const {displayType}= aMenuItem;

    const requiresActivate= ACTIVATE_REQUIRED.includes(displayType);
    if (requiresActivate && !aMenuItem.activate) {
        dpData.dataProducts= {
            displayType: 'message', 'message': `Data Product (${displayType}) not supported (activate required)`,
            menu, menuKey, fileMenu, activeMenuLookupKey
        };
    }
    else {
        switch (displayType) {
            case DPtypes.IMAGE:
            case DPtypes.TABLE:
            case DPtypes.CHART:
            case DPtypes.CHOICE_CTI:
                dpData.dataProducts= {...aMenuItem, menuKey, menu, fileMenu, activeMenuKey:menuKey, activeMenuLookupKey};
                break;
            case DPtypes.PNG:
            case DPtypes.TXT:
            case DPtypes.DOWNLOAD:
            case DPtypes.DOWNLOAD_MENU_ITEM:
            case DPtypes.MESSAGE:
                dpData.dataProducts= {...aMenuItem, menuKey, menu, fileMenu, activeMenuKey:menuKey, activeMenuLookupKey};
                break;
            case DPtypes.ANALYZE:
                dpData.dataProducts= {...aMenuItem, menuKey, menu, activeMenuKey:menuKey, activeMenuLookupKey};
                break;
        }

    }
    return insertOrReplace(state,dpData);
}


const FILE_MENU_REQUIRED= [DPtypes.IMAGE,DPtypes.TABLE,DPtypes.CHOICE_CTI,DPtypes.CHART,DPtypes.MESSAGE,DPtypes.DOWNLOAD_MENU_ITEM];

export function changeActiveFileMenuItem(state,action) {

    const {dpId,fileMenu, menu:menuParameter, currentMenuKey}= action.payload;
    let {newActiveFileMenuKey}= action.payload;

    const dpData= createOrFind(state,dpId);
    const {dataProducts, activeFileMenuKeys}= dpData;
    const menu= menuParameter || dataProducts.menu || [];
    const menuKey= currentMenuKey || getActiveMenuKey(dpId, dpData.dataProducts.activeMenuLookupKey);

    const selectedMenuDataProduct= menu.find( (dt) => dt.menuKey===menuKey) || {};

    const currentActiveFileMenuKey= getActiveFileMenuKey(dpId,fileMenu);
    if (newActiveFileMenuKey===currentActiveFileMenuKey) return state;
    if (!newActiveFileMenuKey) newActiveFileMenuKey= currentActiveFileMenuKey;


    const actIdx= fileMenu.menu.findIndex( (m) => m.menuKey===newActiveFileMenuKey);
    const fileMenuItem= fileMenu.menu[actIdx<0? fileMenu.initialDefaultIndex: actIdx];

    const {activate,displayType}= fileMenuItem;

    dpData.activeFileMenuKeys={...activeFileMenuKeys, [fileMenu.activeItemLookupKey]:fileMenuItem.menuKey};

    if (!FILE_MENU_REQUIRED.includes(displayType)) {
        dpData.dataProducts= dpdtMessage(`Data product (${displayType}) not supported`,menu, {fileMenu,menuKey, activeMenuKey:menuKey});
        return insertOrReplace(state,dpData);
    }
    if (!activate && (displayType!==DPtypes.MESSAGE && displayType!==DPtypes.DOWNLOAD_MENU_ITEM)) {
        dpData.dataProducts= dpdtMessage('Data product not supported, no activate available',menu, {fileMenu,menuKey, activeMenuKey:menuKey});
        return insertOrReplace(state,dpData);
    }

    const newFileMenu= {...fileMenu, activeFileMenuKey:newActiveFileMenuKey};
    const newDisplayProduct= { ...selectedMenuDataProduct, ...fileMenuItem,
        fileMenu:newFileMenu, activeMenuKey:menuKey, menuKey};
    const newMenu= menu && menu.map( (menuItem) => (menuItem.menuKey!==menuKey) ? menuItem : newDisplayProduct );
    dpData.dataProducts= {...newDisplayProduct, menu:newMenu};
    return insertOrReplace(state,dpData);
}
