import React, {useEffect, useMemo, useState} from 'react';
import {cloneDeep, once} from 'lodash';
import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {MetaConst} from '../../data/MetaConst';
import {dispatchTableUiUpdate, TABLE_LOADED} from '../../tables/TablesCntlr.js';
import {getActiveTableId, getMetaEntry, getTableUiByTblId, getTblById} from '../../tables/TableUtil.js';
import {DownloadButton, DownloadOptionPanel} from '../../ui/DownloadDialog.jsx';
import {getDataServiceOption, getDataServiceOptionByTable, getDataServiceOptionsFallback,
} from '../../ui/tap/DataServicesOptions';
import {findTableCenterColumns, hasObsCoreLikeDataProducts, isDatalinkTable} from '../../voAnalyzer/TableAnalysis.js';
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
import {ToolbarButton} from 'firefly/ui/ToolbarButton';

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
    const hostname = sRequest?.source || sRequest?.serviceUrl
        ? new URL(sRequest.source || sRequest.serviceUrl).hostname
        : null;
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

export const PrepareDownload = React.memo(({table_id, tbl_title, viewerId, showFileStructure=false,
                                               downloadType='package', dataSource, fileName}) => {
    const tbl_id = table_id || getActiveTableId();
    const tblTitle = tbl_title || (getTblById(tbl_id)?.title ?? 'unknown');
    const baseFileName = tblTitle.replace(/\s+/g, '').replace(/[^a-zA-Z0-9_.-]/g, '_');
    const isDatalink = isDatalinkTable(tbl_id);

    const [semList, setSemList] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (isDatalink) setLoading(false);
        else {
            fetchSemanticList(tbl_id).then( (result) => {
                setSemList(result);
                setLoading(false);
            });
        }
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

    const cutoutValue = useStoreConnector(() => getCutoutSize(viewerId ?? undefined));

    const dataProductsComponentKey= tblIdToKey(tbl_id);

    const generateDownloadFileName= getDataServiceOptionByTable('generateDownloadFileName', tbl_id, false);

    const cutoutTargetVals = useStoreConnector(() => findCutoutTarget(viewerId ?? dataProductsComponentKey, undefined, tblModel, tblModel.highlightedRow));
    const centerCols = findTableCenterColumns(tbl_id);
    //this will be null if no datalink service descriptor is found, else it will return the access url and input params from the service descriptor
    const isDatalinkSerDesc = useStoreConnector(() => checkForDatalinkServDesc(tblModel));
    if (!tblModel?.totalRows) return;

    let ra = cutoutTargetVals?.positionWP?.x;
    let dec = cutoutTargetVals?.positionWP?.y;

    if (cutoutTargetVals?.foundType === ROW_POSITION && !isDatalink) { //if datalink table (extracted products table), then use the ra/dec directly from cutoutTargetVals
        //server side should use center cols to get ra/dec from the file if user selects this option
        ra = null;
        dec = null;
    }
    const position = {
        centerColNames: { lonCol: centerCols?.lonCol, latCol: centerCols?.latCol },
        centerColValues: { ra, dec }
    };

    return (
        <>
            {loading && <ToolbarButton enabled={false} variant={'soft'} color='warning' text={downloadType === 'script' ? 'Generate Download Script' : 'Prepare Download'}/>}
            {!loading &&
                <Stack>
                    <DownloadButton
                        buttonText = {downloadType === 'script' ? 'Generate Download Script' : 'Prepare Download'}>
                        <DownloadOptionPanel {...{
                            updateSearchRequest,
                            groupKey: tbl_id,
                            tbl_id,
                            downloadType,
                            dlParams: {
                                FileGroupProcessor:'ObsCorePackager',
                                worker: downloadType === 'script' ? 'DownloadScriptWorker' : 'PackagingWorker',
                                dlCutout: 'orig',
                                position,
                                cutoutValue,
                                generateDownloadFileName,
                                datalinkServiceDescriptor: isDatalinkSerDesc,
                                viewerId,
                                DataSource: dataSource,
                                help_id:'table.obsCorePackage',
                                BaseFileName: fileName ? fileName+ `_${baseFileName}` : `${baseFileName}`
                            }}}>
                            {!isDatalink && <Stack spacing={1}>
                                <CheckboxGroupInputField
                                    fieldKey='productTypes'
                                        options={dynamicOptions}
                                        initialState={{value: '*'}}
                                        label='Products to Download: ' />
                            </Stack>}
                        </DownloadOptionPanel>
                    </DownloadButton>
                </Stack>
            }
        </>
    );
});

PrepareDownload.Props = {
    tbl_id: String,
};
