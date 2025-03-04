import React from 'react';
import {cloneDeep, once} from 'lodash';
import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {MetaConst} from '../../data/MetaConst';
import {dispatchTableUiUpdate, TABLE_LOADED} from '../../tables/TablesCntlr.js';
import {getActiveTableId, getMetaEntry, getTableUiByTblId, getTblById, getTblInfo} from '../../tables/TableUtil.js';
import {DownloadButton, DownloadOptionPanel} from '../../ui/DownloadDialog.jsx';
import {getDataServiceOption, getDataServiceOptionByTable, getDataServiceOptionsFallback,
} from '../../ui/tap/DataServicesOptions';
import {findTableCenterColumns, hasObsCoreLikeDataProducts} from '../../voAnalyzer/TableAnalysis.js';
import {getCatalogWatcherDef} from '../../visualize/saga/CatalogWatcher.js';
import {getUrlLinkWatcherDef} from '../../visualize/saga/UrlLinkWatcher.js';
import {getActiveRowToImageDef } from '../../visualize/saga/ActiveRowToImageWatcher.js';
import {getMocWatcherDef} from '../../visualize/saga/MOCWatcher.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent';
import {
    findCutoutTarget, getCutoutSize, getCutoutTargetType, ROW_POSITION, SEARCH_POSITION, tblIdToKey,
    USER_ENTERED_POSITION
} from 'firefly/ui/tap/Cutout';
import {getTableModel} from 'firefly/voAnalyzer/VoCoreUtils';

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

const PrepareDownload = () => {
    const tbl_id = getActiveTableId();
    const tblTitle = getTblById(tbl_id)?.title ?? 'unknown';
    const baseFileName = tblTitle.replace(/\s+/g, '').replace(/[^a-zA-Z0-9_.-]/g, '_');

    const tblModel = getTableModel(tbl_id);
    const cutoutValue = useStoreConnector( () => getCutoutSize());

    // const tblInfo = getTblInfo(tblModel);
    // const cutoutTargetVals = findCutoutTarget('', {}, tblModel, tblInfo?.highlightedRow);
    // const ra = cutoutTargetVals?.positionWP?.x;
    // const dec = cutoutTargetVals?.positionWP.y;
    ////////// todo
    ////////// todo
    const dataProductsComponentKey= tblIdToKey(tbl_id);
    const cutoutTargetVals = findCutoutTarget(dataProductsComponentKey, undefined, tblModel, tblModel.highlightedRow);
    const passRaDec= cutoutTargetVals.foundType===SEARCH_POSITION || cutoutTargetVals.foundType===USER_ENTERED_POSITION;
    const ra = cutoutTargetVals?.positionWP?.x;
    const dec = cutoutTargetVals?.positionWP.y;
    const {lonCol,latCol}= findTableCenterColumns(tbl_id);

    ////////// todo
    ////////// todo

    return (
        <div>
            <DownloadButton>
                <DownloadOptionPanel {...{
                    updateSearchRequest,
                    showZipStructure: false, //flattened files, except for datalink (with more than one valid file)
                    dlParams: {
                        FileGroupProcessor:'ObsCorePackager',
                        dlCutout: 'orig',
                        cutoutPosition: passRaDec ? ra + ',' + dec : '',
                        centerCols: cutoutTargetVals.foundType===ROW_POSITION ? lonCol + ',' + latCol : '',
                        cutoutValue,
                        TitlePrefix: tblTitle,
                        help_id:'table.obsCorePackage',
                        BaseFileName:`${baseFileName}`}}}/>
            </DownloadButton>
        </div>
    );
};
