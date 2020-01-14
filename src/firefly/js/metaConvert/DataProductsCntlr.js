import {flux} from '../Firefly';
import {REINIT_APP} from '../core/AppDataCntlr';
import {isArray, isObject,get} from 'lodash';
import {dpdtMessage, DPtypes} from './DataProductsType';
import {download, downloadSimple, encodeUrl} from '../util/WebUtil';
import {getRootURL} from '../util/BrowserUtil';


export const DATA_PRODUCTS_KEY= 'dataProducts';
const PREFIX= 'DataProductCntlr';
export const UPDATE_DATA_PRODUCTS= `${PREFIX}.UpdateDataProducts`;
export const UPDATE_ACTIVE_KEY= `${PREFIX}.UpdateActiveKey`;
export const ACTIVATE_MENU_ITEM= `${PREFIX}.ActivateMenuItem`;
export const ACTIVATE_FILE_MENU_ITEM= `${PREFIX}.ActivateFileMenuItem`;

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
        if (menuItem.displayType===DPtypes.DOWNLOAD) doDownload(menuItem.url);
        else dispatcher(rawAction);
    };
}

function activateFileMenuItemActionCreator(rawAction) {
    return (dispatcher) => {
        const {fileMenu, newActiveFileMenuKey}= rawAction.payload;
        let doDispatch= true;
        if (fileMenu && fileMenu.menu && newActiveFileMenuKey) {
            const menuItem= fileMenu.menu.find( (m) => m.menuKey===newActiveFileMenuKey);
            if (menuItem && menuItem.displayType===DPtypes.DOWNLOAD) {
                doDownload(menuItem.url);
                doDispatch= false;
            }
        }
        if (doDispatch) dispatcher(rawAction);
    };
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
 * @prop {Object} activeFileMenuKeys - key serialized request, value - activeFileMenuKey, only used with containerType:WRAPPER
 * @prop {Object} activeMenuKeys - key serialized request, value - activeFileMenuKey, last active menu key, only used with containerType:WRAPPER
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
        }
    ];
}

export default {
    reducers, actionCreators,
};


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



export function getDataProducts(dpRoot,dpId) {
    return createOrFind(dpRoot,dpId).dataProducts;
}

export const getActiveFileMenuKey= (dpId,fileMenu) =>
    createOrFind(dataProductRoot(),dpId).activeFileMenuKeys[fileMenu.activeItemLookupKey];


export const getActiveFileMenuKeyByKey= (dpId,key) =>
    createOrFind(dataProductRoot(),dpId).activeFileMenuKeys[key];


export const getActiveMenuKey= (dpId,activeMenuLookupKey) =>
    createOrFind(dataProductRoot(),dpId).activeMenuKeys[activeMenuLookupKey];



function reducer(state=initState(), action={}) {

    if (!action.payload || !action.type) return state;
    let retState= state;

    switch (action.type) {
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
        case REINIT_APP:
            retState= initState();
            break;
        default:
            break;

    }
    return retState;
}

const makeNewDPData= (dpId) =>  ({ dpId, dataProducts: {}, activeFileMenuKeys: {}, activeMenuKeys: {} });

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


function updateDataProducts(state,action) {
    const {dpId, dataProducts}= action.payload;
    const dpData = createOrFindAndCopy(state,dpId);
    dpData.dataProducts= dataProducts;
    return insertOrReplace(state,dpData);
}

function updateActiveKey(state,action) {
    const {dpId, activeMenuKeyChanges, activeFileMenuKeyChanges}= action.payload;
    const dpData= createOrFindAndCopy(state,dpId);
    if (isObject(activeMenuKeyChanges)) dpData.activeMenuKeys= {...dpData.activeMenuKeys,...activeMenuKeyChanges};
    if (isObject(activeFileMenuKeyChanges)) dpData.activeFileMenuKeys= {...dpData.activeFileMenuKeys,...activeFileMenuKeyChanges};
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
    dpData.activeMenuKeys= {...activeMenuKeys, [activeMenuLookupKey]:aMenuItem.menuKey};

    const {activate,url, displayType}= aMenuItem;

    const requiresActivate= ACTIVATE_REQUIRED.includes(displayType);
    if (requiresActivate && !activate) {
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
            case DPtypes.CHART_TABLE:
                dpData.dataProducts= {displayType, menuKey, menu, fileMenu, activeMenuKey:menuKey, activeMenuLookupKey, activate};
                break;
            case DPtypes.PNG:
                dpData.dataProducts= {displayType, url, menuKey, menu, activeMenuKey:menuKey, activeMenuLookupKey};
                break;
            case DPtypes.DOWNLOAD:
                dpData.dataProducts= {displayType, url, menuKey, menu, activeMenuKey:menuKey, activeMenuLookupKey};
                break;
            case DPtypes.ANALYZE:
                dpData.dataProducts= {displayType, menuKey, menu, activeMenuKey:menuKey, activate, activeMenuLookupKey};
        }

    }
    return insertOrReplace(state,dpData);
}


const FILE_MENU_REQUIRED= [DPtypes.IMAGE,DPtypes.TABLE,DPtypes.CHART_TABLE,DPtypes.CHART,DPtypes.MESSAGE];

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
    const fileMenuItem= fileMenu.menu[actIdx<0?0:actIdx];

    const {activate,displayType,message}= fileMenuItem;

    dpData.activeFileMenuKeys={...activeFileMenuKeys, [fileMenu.activeItemLookupKey]:fileMenuItem.menuKey};

    if (!FILE_MENU_REQUIRED.includes(displayType)) {
        dpData.dataProducts= dpdtMessage(`Data product (${displayType}) not supported`,menu, {fileMenu,menuKey, activeMenuKey:menuKey});
        return insertOrReplace(state,dpData);
    }
    if (!activate && displayType!==DPtypes.MESSAGE) {
        dpData.dataProducts= dpdtMessage('Data product not supported, no activate available',menu, {fileMenu,menuKey, activeMenuKey:menuKey});
        return insertOrReplace(state,dpData);
    }

    const newFileMenu= {...fileMenu, activeFileMenuKey:newActiveFileMenuKey};
    const newDisplayProduct= { ...selectedMenuDataProduct, displayType, activate, message, fileMenu:newFileMenu, activeMenuKey:menuKey};
    const newMenu= menu && menu.map( (menuItem) => (menuItem.menuKey!==menuKey) ? menuItem : newDisplayProduct );
    dpData.dataProducts= {...newDisplayProduct, menu:newMenu};
    return insertOrReplace(state,dpData);
}
