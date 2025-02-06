import React from 'react';
import {cloneDeep, once} from 'lodash';
import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {MetaConst} from '../../data/MetaConst';
import {dispatchTableUiUpdate, TABLE_LOADED} from '../../tables/TablesCntlr.js';
import {getActiveTableId, getMetaEntry, getTableUiByTblId, getTblById} from '../../tables/TableUtil.js';
import {DownloadButton, DownloadOptionPanel} from '../../ui/DownloadDialog.jsx';
import {
    getDataServiceOption, getDataServiceOptionByTable, getDataServiceOptions, getDataServiceOptionsFallback,
} from '../../ui/tap/DataServicesOptions';
import {hasObsCoreLikeDataProducts} from '../../voAnalyzer/TableAnalysis.js';
import {getCatalogWatcherDef} from '../../visualize/saga/CatalogWatcher.js';
import {getUrlLinkWatcherDef} from '../../visualize/saga/UrlLinkWatcher.js';
import {getActiveRowToImageDef } from '../../visualize/saga/ActiveRowToImageWatcher.js';
import {getMocWatcherDef} from '../../visualize/saga/MOCWatcher.js';

export const getAllStartIds= ()=> [
    getMocWatcherDef().id,
    getCatalogWatcherDef().id,
    getUrlLinkWatcherDef().id,
    getActiveRowToImageDef().id,
    getObsCoreWatcherDef().id,
];

export function startTTFeatureWatchers(startIds=[
    getMocWatcherDef().id, getCatalogWatcherDef().id, getUrlLinkWatcherDef().id, getActiveRowToImageDef().id]) {
    startIds.includes(getMocWatcherDef().id) && dispatchAddTableTypeWatcherDef(getMocWatcherDef());
    startIds.includes(getCatalogWatcherDef().id) && dispatchAddTableTypeWatcherDef(getCatalogWatcherDef());
    startIds.includes(getUrlLinkWatcherDef().id) && dispatchAddTableTypeWatcherDef(getUrlLinkWatcherDef());
    startIds.includes(getActiveRowToImageDef().id) && dispatchAddTableTypeWatcherDef(getActiveRowToImageDef());
    startIds.includes(getObsCoreWatcherDef().id) && dispatchAddTableTypeWatcherDef(getObsCoreWatcherDef());
}



/** @type {TableWatcherDef} */
export const getObsCoreWatcherDef= once(() => ({
    id : 'ObsCorePackage',
    watcher : watchForObsCoreTable,
    testTable : hasObsCoreLikeDataProducts,
    actions: [TABLE_LOADED]
}));


const addedTblIds=[];

function watchForObsCoreTable(tbl_id, action, cancelSelf) {
    if (action) return;
    if (addedTblIds.includes(tbl_id)) return;
    setupObsCorePackaging(tbl_id);
    addedTblIds.push(tbl_id);
    cancelSelf();
}


function setupObsCorePackaging(tbl_id) {
    const table= getTblById(tbl_id);
    if (!table) return;

    const {request}=table;
    let enabled;
    if (request.QUERY && request.serviceUrl) { // if known TAP service request
        enabled= getDataServiceOptionByTable('enableObsCoreDownload',table);
    }
    else {
        enabled= getDataServiceOption('enableObsCoreDownload');
    }
    if (!enabled) return;

    const {tbl_ui_id, leftButtons=[]}= getTableUiByTblId(tbl_id) ?? {} ;
    leftButtons.unshift(() => <PrepareDownload/>);
    dispatchTableUiUpdate({ tbl_ui_id, leftButtons});
}

function updateSearchRequest( tbl_id='', dlParams='', sRequest=null) {
    const hostname= new URL(sRequest.source ? sRequest.source : sRequest.serviceUrl)?.hostname;
    const serviceId= getMetaEntry(tbl_id,MetaConst.DATA_SERVICE_ID);
    const ops= getDataServiceOptionsFallback(serviceId, hostname) ?? {};
    const template= ops.productTitleTemplate;
    const useSourceUrlFileName= ops.packagerUsesSourceUrlFileName;
    const templateColNames= template && getColNameFromTemplate(template);
    const searchRequest = cloneDeep( sRequest);
    searchRequest.template = template;
    searchRequest.templateColNames = templateColNames?.toString();
    searchRequest.useSourceUrlFileName= useSourceUrlFileName;
    return searchRequest;
}

function getColNameFromTemplate(template) {
    return template.match(/\${[\w -.]+}/g)?.map( (s) => s.substring(2,s.length-1));
}

const PrepareDownload = React.memo(() => {
    const tblTitle = getTblById(getActiveTableId())?.title ?? 'unknown';
    const baseFileName = tblTitle.replace(/\s+/g, '').replace(/[^a-zA-Z0-9_.-]/g, '_');
    return (
        <div>
            <DownloadButton>
                <DownloadOptionPanel {...{
                    updateSearchRequest,
                    showZipStructure: false, //flattened files, except for datalink (with more than one valid file)
                    dlParams: {
                        FileGroupProcessor:'ObsCorePackager',
                        dlCutout: 'orig',
                        TitlePrefix: tblTitle,
                        help_id:'table.obsCorePackage',
                        BaseFileName:`${baseFileName}`}}}/>
            </DownloadButton>
        </div>
    );
});
