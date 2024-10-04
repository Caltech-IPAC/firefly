

/**
 * @global
 * @public
 * @typedef {Object} DataProductsDisplayType
 *
 * This object is managed by the store. @see DataProductCntlr.js
 * It contains the parameters that the MultiProductViewer uses to choose what to display.
 * Flow
 *    - DataProductWatcher watches data products tables.
 *    - DataProducts tables evaluated using DataProductsFactory to find the correct DataProductsConvertType entry
 *    - functions from DataProductsConvertType object are called to generate this object
 *    - this object is dispatched to the controller using dispatchUpdateDataProducts
 *    - the MultiProductViewer is updated based on this object to show the correct display
 *          - note: The MultiProductViewer must show the correct viewer component and the activate function loads the correct data
 *                  - MultiProductViewer looks at the displayType to render the correct UI
 *                  - a useEffect calls the activate function that load the tables or images
 *                  - activate functions are only necessary to tables, charts, and images
 *                  - displayTypes such as 'promise', 'message' show feedback to the user (with possibly a working indicator)
 *                  - displayTypes such as 'png' or 'download' will cause the UI to show the png or download the item
 *
 *
 * @prop {string} displayType one of 'image', 'message', 'promise', 'table', 'png', 'download, 'xyplot', 'analyze'
 * @prop {String} [name]
 * @prop {String} menuKey - unique key of this item
 * @prop {Function} [activate] - function to plot 'image', 'table', 'xyplot', require for those
 * @prop {Function} [imageActivate] (only used with CHOICE_CTI) function to plot 'image', used when there is already an activate for a table
 * @prop {Function} [analysisActivateFunc] this is set on already activated menu item with a service descriptor to reset and reenter inputs, it is then uses as the activate
 * @prop {String} [url] - required if display type is 'png' or 'download'
 * @prop {WebPlotRequest} [request] - webPlotRequest - only carried for limited uses
 *
 * @prop {String} [message] - (used with type message) required it type is 'message' or 'promise'
 * @prop {boolean} [isWorkingState]- (used with type message) if defined this means we are in a transitive/loading state. expect regular updates
 * @prop {boolean} complexMessage - (used with type message) - use with message display type. indicates it is a complex message with more functionality
 * @prop {Array.<String>} (used with type message, only when complexMessage is true) detailMsgAry
 * @prop {String} resetMenuKey (used with type message)
 * @prop {boolean} singleDownload - (used with type message) (menu with dpdtDownload as first item in array is required) (also set to true with DPtypes.DOWNLOAD_MENU_ITEM) give a message with a file to download
 *
 * @prop {Promise} [promise] - (used with type promise) required it type is 'promise'
 *
 * @prop {String} activeMenuLookupKey
 * @prop {boolean} requestDefault
 * @prop {String} serviceDefRef
 * @prop {String} sRegion - use for hips backing service descriptor input
 * @prop {String} semantics - (carried but not used)
 * @prop {String} size - (carried but not used)
 * @prop {String} badUrl - an url that is not working, for just showing to the user
 * @prop {Array.<DataProductsDisplayType>|undefined} menu - if defined, then menu to display
 *
 */


/**
 * @global
 * @public
 * @typedef {Object} DataProductsFileMenu
 * @prop {Object} fileAnalysis
 * @prop {String} activeItemLookUpKey
 * @prop {Array.<DataProductsDisplayType>|undefined} menu - if defined, then menu to display
 *
 */


export const DPtypes= {
    ERROR: 'error',
    MESSAGE: 'message',
    SEND_TO_BROWSER: 'send-to-browser',
    PROMISE: 'promise',
    IMAGE: 'image',
    IMAGE_SNGLE_AXIS: 'image-single-axis',
    TABLE: 'table',
    CHART: 'xyplot',
    CHOICE_CTI: 'chartTable',
    DOWNLOAD: 'download',
    DOWNLOAD_MENU_ITEM: 'download-menu-item',
    PNG: 'png',
    ANALYZE: 'analyze',
    UNSUPPORTED: 'unsupported',
};


export const SHOW_CHART='showChart';
export const SHOW_TABLE='showTable';
export const SHOW_IMAGE='showImage';
export const AUTO='auto';

/**
 *
 * @param {String} message
 * @param {Array.<DataProductsDisplayType>|undefined} [menu] - if defined, then menu to display
 * @param {Object} extra - all values in this object are added to the DataProjectType Object
 * @return {DataProductsDisplayType}
 */
export function dpdtMessage(message, menu= undefined, extra={}) {
    return {displayType:DPtypes.MESSAGE, message, menu, menuKey:'message-0', ...extra};
}

export function dpdtSimpleMsg(message) {
    return {displayType:DPtypes.MESSAGE, message, menuKey:'message-0'};
}

export function dpdtUploadError(url,e) {
    return {displayType:DPtypes.ERROR, error:e, url};
}

/**
 *
 * @param {String} message
 * @param {String} menuKey
 * @return {DataProductsDisplayType}
 */
export function dpdtWorkingMessage(message,menuKey) {
    return {displayType:DPtypes.MESSAGE, message, rootMessage:message,
        isWorkingState:true, menu:undefined, menuKey, };
}

/**
 *
 * @param {String} message
 * @param {Promise} promise
 * @param {WebPlotRequest} [request]
 * @return {DataProductsDisplayType}
 */
export function dpdtWorkingPromise(message,promise,request=undefined) {
    return {
        displayType:DPtypes.PROMISE,
        promise,
        message,
        request,
        rootMessage:message,
        menuKey:'working-promise-0',
        isWorkingState:true,
        menu:undefined,
    };
}

export const dpdtSendToBrowser= (url, serDefParams) => {
    return {displayType:DPtypes.SEND_TO_BROWSER, url, serDefParams};
};

/**
 *
 * @param {String} message
 * @param {String} titleStr download title str
 * @param {String} url download url
 * @param {String} [fileType]
 * @return {DataProductsDisplayType}
 */
export const dpdtMessageWithDownload= (message,titleStr, url,fileType=undefined) => {
    const singleDownload= Boolean(titleStr && url);
    return dpdtMessage(message,singleDownload ?[dpdtDownload(titleStr,url,'download-0',fileType)] : undefined,{singleDownload} );
};

export const dpdtMessageWithError= (message,detailMsgAry) => {
    return dpdtMessage(message,undefined,{complexMessage:true, detailMsgAry} );
};

/**
 *
 * @param {string} name
 * @param {Function} activate
 * @param {Function} extraction
 * @param {number|string} menuKey
 * @param {Object} extra - all values in this object are added to the DataProjectType Object
 * @return {DataProductsDisplayType}
 */
/**
 *
 * @param {Object} p
 * @param p.name
 * @param p.activate
 * @param [p.extraction]
 * @param [p.menuKey]
 * @param [p.extractionText]
 * @param [p.request]
 * @param [p.override]
 * @param [p.interpretedData]
 * @param [p.requestDefault]
 * @param [p.url]
 * @param [p.semantics]
 * @param [p.size]
 * @return {DataProductsDisplayType}
 */
export function dpdtImage({name, activate, extraction, menuKey='image-0', extractionText='Pin Image',
                              request, override, interpretedData, requestDefault, enableCutout, pixelBasedCutout,
                              url, semantics,size, serDef }) {
    return { displayType:DPtypes.IMAGE, name, activate, extraction, menuKey, extractionText, enableCutout, pixelBasedCutout,
        request, override, interpretedData, requestDefault,url, semantics,size,serDef};
}

/**
 *
 * @param {string} name
 * @param {Function} activate
 * @param {Function} extraction
 * @param {number|string} menuKey
 * @param {Object} extra - all values in this object are added to the DataProjectType Object
 * @return {DataProductsDisplayType}
 */
export function dpdtTable(name, activate, extraction, menuKey='table-0', extra={}) {
    return { displayType:DPtypes.TABLE, name, activate, extraction, menuKey, ...extra};
}


/**
 *
 * @param {string} name
 * @param {Function} activate
 * @param {Function} extraction
 * @param {number|string} menuKey
 * @param {Object} extra - all values in this object are added to the DataProjectType Object
 * @return {DataProductsDisplayType}
 */
export function dpdtChartTable(name, activate, extraction, menuKey='chart-table-0', extra={}) {
    return { displayType:DPtypes.CHOICE_CTI, name, activate, extraction, menuKey, ...extra};
}

/**
 *
 * @param {object} p
 * @param {String} p.name
 * @param {function} p.activate
 * @param {String} p.url
 * @param {ServiceDescriptorDef} p.serDef
 * @param {String} p.menuKey
 * @param {String} p.semantics
 * @param {number} [p.size]
 * @param {String} p.activeMenuLookupKey
 * @param {WebPlotRequest} p.request
 * @param {String} [p.sRegion]
 * @param {String} [p.prodTypeHint]
 * @param {String} [p.serviceDefRef]
 * @param {boolean} [p.allowsInput]
 * @param {String} [p.standardID]
 * @param {String} [p.ID]
 * @return {DataProductsDisplayType}
 */
export function dpdtAnalyze({
                             name,
                             activate,
                             url,
                             serDef= undefined,
                             menuKey='analyze-0',
                             semantics,
                             size,
                             activeMenuLookupKey,
                             request,
                             sRegion,
                             prodTypeHint= 'unknown',
                             serviceDefRef,
                             allowsInput= false,
                             standardID,
                             ID }) {
    return { displayType:DPtypes.ANALYZE,
        name, url, activate, serDef, menuKey, semantics,
        size, activeMenuLookupKey, request, sRegion, prodTypeHint,
        serviceDefRef, allowsInput, standardID, ID,
    };
}

/**
 *
 * @param {string} name
 * @param {string} url
 * @param {number|string} menuKey
 * @param {string} fileType type of file eg- tar or gz
 * @param {Object} extra - all values in this object are added to the DataProjectType Object
 * @return {DataProductsDisplayType}
 */
export function dpdtDownload(name, url, menuKey='download-0', fileType, extra={}) {
    return { displayType:DPtypes.DOWNLOAD, name, url, menuKey, fileType, ...extra};
}

export function dpdtDownloadMenuItem(name, url, menuKey='download-0', fileType, extra={}) {
    return { displayType:DPtypes.DOWNLOAD_MENU_ITEM, name, url, menuKey, singleDownload: true, fileType, ...extra};
}

/**
 *
 * @param {string} name
 * @param {string} url
 * @param {number|string} menuKey
 * @param {Object} extra - all values in this object are added to the DataProjectType Object
 * @return {DataProductsDisplayType}
 */
export function dpdtPNG(name, url, menuKey='png-0', extra={}) {
    return { displayType:DPtypes.PNG, name, url, menuKey, ...extra};
}

/**
 *
 * @param {Array.<DataProductsDisplayType>} menu
 * @param {number} activeIdx
 * @param {String} activeMenuLookupKey
 * @param {boolean} keepSingleMenu
 * @return {DataProductsDisplayType}
 */
export function dpdtFromMenu(menu,activeIdx,activeMenuLookupKey, keepSingleMenu=false) {
    return {...menu[activeIdx], activeMenuLookupKey, menu: (menu.length>1||keepSingleMenu)?menu:[]};
}
