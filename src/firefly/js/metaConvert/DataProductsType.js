

/**
 * @global
 * @public
 * @typedef {Object} DataProductsDisplayType
 * @prop {string} displayType one of 'image', 'message', 'promise', 'table', 'png', 'download, 'xyplot', 'analyze'
 * @prop {String} menuKey - unique key of this item
 * @prop {Function} [activate] - function to plot 'image', 'table', 'xyplot', require for those
 * @prop {String} [url] - required if display type is 'png' or 'download'
 * @prop {String} [message] - required it type is 'message' or 'promise'
 * @prop {String} [name]
 * @prop {boolean} [isWorkingState] - if defined this means we are in a transitive/loading state. expect regular updates
 * @prop {Promise} [promise] - required it type is 'promise'
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
    MESSAGE: 'message',
    PROMISE: 'promise',
    IMAGE: 'image',
    TABLE: 'table',
    CHART: 'xyplot',
    CHART_TABLE: 'chartTable',
    DOWNLOAD: 'download',
    PNG: 'png',
    ANALYZE: 'analyze',
};



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

/**
 *
 * @param {String} message
 * @param {WebPlotRequest} [request]
 * @param {Object} [extra]
 * @param {String} [rootMessage] will default to message
 * @return {DataProductsDisplayType}
 */
export function dpdtWorkingMessage(message,request, extra={}, rootMessage=undefined) {
    return {displayType:DPtypes.MESSAGE, message, rootMessage:rootMessage||message, menuKey:'working-0',
        isWorkingState:true, menu:undefined, ...extra};
}

/**
 *
 * @param {String} message
 * @param {Promise} promise
 * @param {WebPlotRequest} [request]
 * @param {Object} [extra]
 * @param {String} [rootMessage] will default to message
 * @return {DataProductsDisplayType}
 */
export function dpdtWorkingPromise(message,promise,request=undefined, extra={}, rootMessage=undefined) {
    return {
        displayType:DPtypes.PROMISE,
        promise,
        message,
        request,
        rootMessage:rootMessage||message, menuKey:'working-promise-0',
        isWorkingState:true,
        menu:undefined, ...extra
    };
}


/**
 *
 * @param {String} message
 * @param {String} titleStr download title str
 * @param {String} url download url
 * @return {DataProductsDisplayType}
 */
export const dpdtMessageWithDownload= (message,titleStr, url) => {
    const singleDownload= Boolean(titleStr && url);
    return dpdtMessage(message,singleDownload ?[dpdtDownload(titleStr,url)] : undefined,{singleDownload} );
};

/**
 *
 * @param {string} name
 * @param {Function} activate
 * @param {number|string} menuKey
 * @param {Object} extra - all values in this object are added to the DataProjectType Object
 * @return {DataProductsDisplayType}
 */
export function dpdtImage(name, activate, menuKey='image-0', extra={}) {
    return { displayType:DPtypes.IMAGE, name, activate, menuKey, ...extra};
}

/**
 *
 * @param {string} name
 * @param {Function} activate
 * @param {number|string} menuKey
 * @param {Object} extra - all values in this object are added to the DataProjectType Object
 * @return {DataProductsDisplayType}
 */
export function dpdtTable(name, activate, menuKey='table-0', extra={}) {
    return { displayType:DPtypes.TABLE, name, activate, menuKey, ...extra};
}

/**
 *
 * @param {string} name
 * @param {Function} activate
 * @param {number|string} menuKey
 * @param {Object} extra - all values in this object are added to the DataProjectType Object
 * @return {DataProductsDisplayType}
 */
export function dpdtChart(name, activate, menuKey='chart-0', extra={}) {
    return { displayType:DPtypes.CHART, name, activate, menuKey, ...extra};
}

/**
 *
 * @param {string} name
 * @param {Function} activate
 * @param {number|string} menuKey
 * @param {Object} extra - all values in this object are added to the DataProjectType Object
 * @return {DataProductsDisplayType}
 */
export function dpdtChartTable(name, activate, menuKey='chart-table-0', extra={}) {
    return { displayType:DPtypes.CHART_TABLE, name, activate, menuKey, ...extra};
}

/**
 *
 * @param {string} name
 * @param {Function} activate
 * @param {String} url
 * @param {number|string} menuKey
 * @param {Object} extra - all values in this object are added to the DataProjectType Object
 * @return {DataProductsDisplayType}
 */
export function dpdtAnalyze(name, activate, url, menuKey='analyze-0', extra={}) {
    return { displayType:DPtypes.ANALYZE, name, url, activate, menuKey, ...extra};
}

/**
 *
 * @param {string} name
 * @param {string} url
 * @param {number|string} menuKey
 * @param {Object} extra - all values in this object are added to the DataProjectType Object
 * @return {DataProductsDisplayType}
 */
export function dpdtDownload(name, url, menuKey='download-0', extra={}) {
    return { displayType:DPtypes.DOWNLOAD, name, url, menuKey, ...extra};
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
 * @return {DataProductsDisplayType}
 */
export function dpdtFromMenu(menu,activeIdx,activeMenuLookupKey) {
    return {...menu[activeIdx], activeMenuLookupKey, menu:menu.length>1?menu:[]};
}
