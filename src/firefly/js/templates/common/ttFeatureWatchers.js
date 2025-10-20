import React, {useEffect, useMemo, useState} from 'react';
import {cloneDeep, once} from 'lodash';
import {dispatchAddTableTypeWatcherDef} from '../../core/MasterSaga.js';
import {MetaConst} from '../../data/MetaConst';
import {dispatchTableUiUpdate, TABLE_LOADED} from '../../tables/TablesCntlr.js';
import {getActiveTableId, getMetaEntry, getTableUiByTblId, getTblById} from '../../tables/TableUtil.js';
import {DownloadButton, DownloadOptionPanel} from '../../ui/DownloadDialog.jsx';
import {getDataServiceOption, getDataServiceOptionByTable, getDataServiceOptionsFallback,
} from '../../ui/tap/DataServicesOptions';
import {findTableCenterColumns, hasDataLinkSvcDesc, hasObsCoreLikeDataProducts, isDatalinkTable, isDataProductsTable
} from '../../voAnalyzer/TableAnalysis.js';
import {getCatalogWatcherDef} from '../../visualize/saga/CatalogWatcher.js';
import {getUrlLinkWatcherDef} from '../../visualize/saga/UrlLinkWatcher.js';
import {getActiveRowToImageDef} from '../../visualize/saga/ActiveRowToImageWatcher.js';
import {getMocWatcherDef} from '../../visualize/saga/MOCWatcher.js';
import {useFieldGroupValue, useStoreConnector} from 'firefly/ui/SimpleComponent';
import {findCutoutTarget, getCutoutSize, ROW_POSITION, tblIdToKey,} from 'firefly/ui/tap/Cutout';
import {getTableModel} from 'firefly/voAnalyzer/VoCoreUtils';
import {fetchSemanticList} from 'firefly/metaConvert/vo/DatalinkFetch';
import {checkForDatalinkServDesc} from 'firefly/ui/dynamic/ServiceDefTools';
import {CheckboxGroupInputField, SelectAllCheckbox} from 'firefly/ui/CheckboxGroupInputField';
import {FormControl, FormLabel, Stack, Typography} from '@mui/joy';
import {ToolbarButton} from 'firefly/ui/ToolbarButton';
import {FieldGroup} from 'firefly/ui/FieldGroup';
import {getFieldVal} from 'firefly/fieldGroup/FieldGroupUtils';
import {makeFoVString} from 'firefly/visualize/ZoomUtil';

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
    testTable : isObsCoreish,
    actions: [TABLE_LOADED]
}));

function isObsCoreish(tableOrId) {
    return hasObsCoreLikeDataProducts(tableOrId) || hasDataLinkSvcDesc(tableOrId);
}

function watchForObsCoreTable(tbl_id, action, cancelSelf) {
    if (action) return;
    const {leftButtons=[]} = getTableUiByTblId(tbl_id);
    if (leftButtons.some((lb) => lb.prepareDownloadBtn)) return;
    setupObsCorePackaging(tbl_id);
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

    if (!isDataProductsTable(tbl_id)) return;

    const dlProps = getDataServiceOptionByTable('obsCoreDownloadProps', table, {}) || {};

    const {tbl_ui_id, leftButtons=[]}= getTableUiByTblId(tbl_id) ?? {} ;
    const prepareDownloadFunc = () => <PrepareDownload {...dlProps} />;
    prepareDownloadFunc.prepareDownloadBtn = true;
    leftButtons.unshift(prepareDownloadFunc);
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

function ProductTypesBlock({tbl_id, dynamicOptions, cutoutValue}) {
    const [getProductTypes] = useFieldGroupValue('productTypes', tbl_id);
    const productTypes = getProductTypes();

    const isCutoutSelected = productTypes?.split(',').map((val) => val.trim()).includes('#cutout') ?? false;

    const defaultValue = dynamicOptions.find((o) => o.value === '#this')?.value ?? '';

    return (<Stack spacing={2}>
            <FormControl>
                <Stack direction='row' alignItems='center' spacing={1}>
                    <FormLabel>Products to Download:</FormLabel>
                    <SelectAllCheckbox
                        fieldKey='productTypes'
                        groupKey={tbl_id}
                        label='Select all'
                        options={dynamicOptions}
                    />
                </Stack>
                <Stack spacing={1} mt={1}>
                    <CheckboxGroupInputField
                        fieldKey='productTypes'
                        groupKey={tbl_id}
                        options={dynamicOptions}
                        //initialState={{ value: dynamicOptions.map((o) => o.value).toString() }} //this is if we want to keep default all  vals selected
                        initialState={{value: defaultValue}}
                        alignment='horizontal'
                    />
                    {isCutoutSelected && (
                        <Typography level='body-sm' sx={{ ml: 1 }}>
                            Note: the current cutout size is {makeFoVString(Number(cutoutValue))}
                            (you may change this via the cutout dialog).
                        </Typography>
                    )}
                </Stack>
            </FormControl>
        </Stack>);
}

function validateProductSelection(formInputs, groupKey) {
    const productTypes = getFieldVal(groupKey, 'productTypes');
    const hasProductBlock = !!productTypes || formInputs?.productTypes !== undefined;

    //if product types (checkboxes) exist but no selection was made
    if (hasProductBlock && (!productTypes?.trim())) {
        return {valid: false, message: 'Please select at least one product type to download.'};
    }
    return {valid: true};
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
        '#calibration': 'Calibration',
        '#preview': 'Preview',
        '#package': 'Package',
        '#weight': 'Weight'
    }), []);


    //dynamically generate options from semList
    const dynamicOptions = useMemo(() => {
        if (semList.length > 0) {
            return semList.map((value) => ({
                label: labelMap[value] || value.replace(/^#/, ''),
                value
            }));
        }
        return [];
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

    const keysToWatch = cutoutValue + '|' + position.centerColValues.ra + '|' + position.centerColValues.dec;

    return (
        <>
            {loading && <ToolbarButton enabled={false} variant={'soft'} color='warning' text={downloadType === 'script' ? 'Generate Download Script' : 'Prepare Download'}/>}
            {!loading &&
                <FieldGroup groupKey={tbl_id}>
                    <Stack>
                        <DownloadButton key={keysToWatch}
                            buttonText = {downloadType === 'script' ? 'Generate Download Script' : 'Prepare Download'}>
                            <DownloadOptionPanel {...{
                                updateSearchRequest,
                                groupKey: tbl_id,
                                tbl_id,
                                downloadType,
                                validateOnSubmit: validateProductSelection,
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
                                    Title: fileName ? fileName+ `_${baseFileName}` : `${baseFileName}`
                                }}}>
                                {!isDatalink && dynamicOptions?.length > 0 &&
                                    (<ProductTypesBlock tbl_id={tbl_id} dynamicOptions={dynamicOptions} cutoutValue={cutoutValue}/>
                                )}
                            </DownloadOptionPanel>
                        </DownloadButton>
                    </Stack>
                </FieldGroup>
            }
        </>
    );
});

PrepareDownload.Props = {
    tbl_id: String,
};
