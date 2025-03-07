import React, {useEffect, useMemo, useState} from 'react';
import {cloneDeep, once} from 'lodash';
import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {MetaConst} from '../../data/MetaConst';
import {dispatchTableUiUpdate, TABLE_LOADED} from '../../tables/TablesCntlr.js';
import {getActiveTableId, getMetaEntry, getTableUiByTblId, getTblById} from '../../tables/TableUtil.js';
import {DownloadButton, DownloadOptionPanel, ScriptTypeOptions} from '../../ui/DownloadDialog.jsx';
import {getDataServiceOption, getDataServiceOptionByTable, getDataServiceOptionsFallback,
} from '../../ui/tap/DataServicesOptions';
import {findTableCenterColumns, hasObsCoreLikeDataProducts} from '../../voAnalyzer/TableAnalysis.js';
import {getCatalogWatcherDef} from '../../visualize/saga/CatalogWatcher.js';
import {getUrlLinkWatcherDef} from '../../visualize/saga/UrlLinkWatcher.js';
import {getActiveRowToImageDef } from '../../visualize/saga/ActiveRowToImageWatcher.js';
import {getMocWatcherDef} from '../../visualize/saga/MOCWatcher.js';
import {useStoreConnector} from 'firefly/ui/SimpleComponent';
import {findCutoutTarget, getCutoutSize, ROW_POSITION, tblIdToKey,
} from 'firefly/ui/tap/Cutout';
import {getTableModel} from 'firefly/voAnalyzer/VoCoreUtils';
import {fetchSemanticList} from 'firefly/metaConvert/vo/DatalinkFetch';
import {checkForDatalinkServDesc} from 'firefly/ui/dynamic/ServiceDefTools';
import {CheckboxGroupInputField} from 'firefly/ui/CheckboxGroupInputField';
import {Stack} from '@mui/joy';

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

    const [semList, setSemList] = useState([]);

    // Fetch semList on mount when tbl_id changes
    useEffect(() => {
        fetchSemanticList(tbl_id).then(setSemList);
    }, [tbl_id]);

    const labelMap = useMemo(() => ({
        '#this': 'Primary Product',
        '#cutout': 'Cutouts',
        '#counterpart': 'Counterpart',
        '#noise': 'Noise',
        '#auxiliary': 'Auxiliary',
        '#progenitor': 'Progenitor',
        '#thumbnail': 'Thumbnail',
        '#preview': 'Preview',
        '#package': 'Package',
        '#weight': 'Weight'
    }), []);

    //dynamically generate options from semList
    const dynamicOptions = useMemo(() => {
        let options;
        if (semList.length > 0) {
            //use semList if available
            options = semList.map((value) => ({
                label: labelMap[value] || value.replace(/^#/, ''), //strip "#" if using raw value as fallback for a cleaner label in the UI
                value
            }));
        } else {
            options = []; //fallback only to "All data" only below
        }

        //ensure '*' ("All data") is always included, even as a fallback when semList is empty
        options.push({ label: 'All data', value: '*' });
        return options;
    }, [semList, labelMap]);

    const tblModel = getTableModel(tbl_id);
    const cutoutValue = useStoreConnector( () => getCutoutSize());

    const dataProductsComponentKey= tblIdToKey(tbl_id);
    const cutoutTargetVals = findCutoutTarget(dataProductsComponentKey, undefined, tblModel, tblModel.highlightedRow);
    const centerCols = findTableCenterColumns(tbl_id);

    //this will be null if no datalink service descriptor is found, else it will return the access url and input params from the service descriptor
    const datalinkServDesc = useStoreConnector(() => checkForDatalinkServDesc(tblModel));

    if (!tblModel?.totalRows) return;
    let ra = cutoutTargetVals?.positionWP?.x;
    let dec = cutoutTargetVals?.positionWP?.y;

    if (cutoutTargetVals.foundType === ROW_POSITION) {
        //server side should use center cols to get ra/dec from the file if user selects this option
        ra = null;
        dec = null;
    }

    const position = {
        centerColNames: { lonCol: centerCols?.lonCol, latCol: centerCols?.latCol },
        centerColValues: { ra, dec }
    };

    return (
        <div>
            <DownloadButton>
                <DownloadOptionPanel {...{
                    updateSearchRequest,
                    showZipStructure: false, //flattened files, except for datalink (with more than one valid file)
                    dlParams: {
                        FileGroupProcessor:'ObsCorePackager',
                        dlCutout: 'orig',
                        position,
                        cutoutValue,
                        datalinkServiceDescriptor: datalinkServDesc,
                        TitlePrefix: tblTitle,
                        help_id:'table.obsCorePackage',
                        BaseFileName:`${baseFileName}`}}}>
                    <Stack spacing={1}>
                        <CheckboxGroupInputField
                            fieldKey='productTypes'
                            options={dynamicOptions}
                            initialState={{value: '*'}}
                            label='Products to Download: ' />
                    </Stack>
                </DownloadOptionPanel>
            </DownloadButton>
        </div>
    );
};
