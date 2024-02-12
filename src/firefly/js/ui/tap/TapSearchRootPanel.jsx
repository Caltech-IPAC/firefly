/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Box, Button, Stack, Switch, Typography} from '@mui/joy';
import {once} from 'lodash';
import {shape,object,bool} from 'prop-types';
import React, {useEffect, useRef, useState} from 'react';
import FieldGroupUtils, {getFieldVal, setFieldValue} from 'firefly/fieldGroup/FieldGroupUtils.js';
import {makeSearchOnce} from 'firefly/util/WebUtil.js';
import {dispatchMultiValueChange} from 'firefly/fieldGroup/FieldGroupCntlr.js';
import {getAppOptions} from 'firefly/core/AppDataCntlr.js';
import {ExtraButton, FormPanel} from 'firefly/ui/FormPanel.jsx';
import {ValidationField} from 'firefly/ui/ValidationField.jsx';
import {intValidator} from 'firefly/util/Validate.js';
import {FieldGroup} from 'firefly/ui/FieldGroup.jsx';
import {makeTblRequest, setNoCache} from 'firefly/tables/TableRequestUtil.js';
import {dispatchTableSearch} from 'firefly/tables/TablesCntlr.js';
import {showInfoPopup, showYesNoPopup} from 'firefly/ui/PopupUtil.jsx';
import {dispatchHideDialog} from 'firefly/core/ComponentCntlr.js';
import {dispatchHideDropDown} from 'firefly/core/LayoutCntlr.js';
import {MetaConst} from '../../data/MetaConst.js';
import {InputField} from '../InputField.jsx';
import {ListBoxInputFieldView} from '../ListBoxInputField.jsx';
import {
    ConstraintContext, getTapUploadSchemaEntry, getUploadServerFile, getUploadTableName,
    getHelperConstraints, getUploadConstraint, isTapUpload
} from './Constraints.js';

import {makeColsLines, tableColumnsConstraints} from 'firefly/ui/tap/TableColumnsConstraints.jsx';
import {
    getMaxrecHardLimit, tapHelpId, getTapServices,
    loadObsCoreSchemaTables, maybeQuote, defTapBrowserState, TAP_UPLOAD_SCHEMA, getAsEntryForTableName,
} from 'firefly/ui/tap/TapUtil.js';
import {AdqlUI, BasicUI} from 'firefly/ui/tap/TableSelectViewPanel.jsx';
import {useFieldGroupMetaState} from '../SimpleComponent.jsx';
import {PREF_KEY} from 'firefly/tables/TablePref.js';

export const TAP_PANEL_GROUP_KEY = 'TAP_PANEL_GROUP_KEY';
const SERVICE_TIP= 'Select a TAP service, or type to enter the URL of any other TAP service';

//-------------
//-------------
// set up the one time (once) functions to be used with the API via initArgs. initArgs is really only used the
// first time though the UI.
//-------------
//-------------

let webApiUserAddedService;
const initServiceUsingAPIOnce= makeSearchOnce(false); // call one time during first construction
const searchFromAPIOnce= makeSearchOnce(); // setup options to immediately execute the search the first time
const activateInitArgsAdqlOnce= once((tapBrowserState,initArgs,setSelectBy) => initArgs?.urlApi?.adql &&
    setTimeout(() => populateAndEditAdql(tapBrowserState,setSelectBy, initArgs.urlApi?.adql), 5));
let lastServicesShowing= false;

/** if an extra service is found from the api that is not in the list then set webApiUserAddedService */
const initApiAddedServiceOnce= once((initArgs) => {
    if (initArgs?.urlApi?.service) {
        const listedEntry= getTapServiceOptions().find( (e) => e.value===initArgs.urlApi?.service);
        if (!listedEntry) webApiUserAddedService = {label: initArgs.urlApi?.service, value: initArgs.urlApi?.service};
    }
});

/**
 * Return true if the initArgs has populated the tap fields so a valid search can be executed.
 * @param fields
 * @param initArgs
 * @param {TapBrowserState} tapBrowserState
 * @return {boolean} true if the field are valid to doa search
 */
function validateAutoSearch(fields, initArgs, tapBrowserState) {
    const {urlApi = {}} = initArgs;
    if (urlApi.adql) return true;
    if (!getAdqlQuery(tapBrowserState, false)) return false; // if we can't build a query then we are not initialized yet
    const {valid, where} = getHelperConstraints(tapBrowserState);
    if (!valid) return false;
    const notWhereArgs = ['MAXREC', 'execute', 'schema', 'service', 'table'];
    const needsWhereClause = Object.keys(urlApi).some((arg) => !notWhereArgs.includes(arg));
    return needsWhereClause ? Boolean(where) : true;
}


//----------
//----------
// end of once api functions
//----------
//----------

function getInitServiceUrl(tapBrowserState,initArgs,tapOps) {
    let {serviceUrl=tapOps[0].value} = tapBrowserState;
    initServiceUsingAPIOnce(true, () => {
        if (initArgs?.urlApi?.service) serviceUrl= initArgs.urlApi.service;
    });
    return serviceUrl;
}

export function getServiceLabel(serviceUrl) {
    const tapOps= getTapServiceOptions();
    return (serviceUrl && (tapOps.find( (e) => e.value===serviceUrl)?.labelOnly)) || '';
}

export function getServiceHiPS(serviceUrl) {
    const tapOps= getTapServices();
    return (serviceUrl && (tapOps.find( (e) => e.value===serviceUrl)?.hipsUrl)) || '';
}


export function TapSearchPanel({initArgs= {}, titleOn=true}) {
    const [getTapBrowserState,setTapBrowserState]= useFieldGroupMetaState(defTapBrowserState,TAP_PANEL_GROUP_KEY);
    const tapState= getTapBrowserState();
    if (!initArgs?.urlApi?.execute) searchFromAPIOnce(true); // if not execute then mark as done, i.e. disable any auto searching
    initApiAddedServiceOnce(initArgs);  // only look for the extra service the first time
    const tapOps= getTapServiceOptions();
    const {current:clickFuncRef} = useRef({clickFunc:undefined});
    const [selectBy, setSelectBy]= useState(() => {
        const val= getFieldVal(TAP_PANEL_GROUP_KEY,'selectBy');
        if (val) return val;
        if (initArgs?.urlApi?.adql) return 'adql';
        return initArgs?.urlApi?.selectBy || 'basic';
    });
    const [servicesShowing, setServicesShowingInternal]= useState(lastServicesShowing);
    const [obsCoreTableModel, setObsCoreTableModel] = useState();
    const [serviceUrl, setServiceUrl]= useState(() => getInitServiceUrl(tapState,initArgs,tapOps));
    activateInitArgsAdqlOnce(tapState,initArgs,setSelectBy);

    const setServicesShowing= (showing) => {
        setServicesShowingInternal((showing));
        lastServicesShowing= showing;
    };

    const obsCoreEnabled = obsCoreTableModel?.tableData?.data?.length > 0;

    const onTapServiceOptionSelect= (selectedOption) => {
        if (!selectedOption) return;
        dispatchMultiValueChange(TAP_PANEL_GROUP_KEY,
            [
                {fieldKey: 'defAdqlKey', value: ''},
                {fieldKey: 'adqlQuery', placeholder: '', value: ''}
            ]
        );
        const serviceUrl= selectedOption?.value;
        setServiceUrl(serviceUrl);
        setObsCoreTableModel(undefined);
        setTapBrowserState({...getTapBrowserState(), serviceUrl});
    };

    useEffect(() => {
        const {serviceUrl:u}= initArgs?.searchParams ?? {};
        u && u!==serviceUrl && setServiceUrl(u);
    }, [initArgs?.searchParams?.serviceUrl]);

    useEffect(() => {
        return FieldGroupUtils.bindToStore( TAP_PANEL_GROUP_KEY, (fields) => {
            const ts= getTapBrowserState();
            setObsCoreTableModel(ts.obsCoreTableModel);

            searchFromAPIOnce( // searchFromAPIOnce only matters if the urlApi.execute is true
                () => validateAutoSearch(fields,initArgs,ts),
                () => setTimeout(() => clickFuncRef.clickFunc?.(), 5));
        });
    }, []);

    useEffect(() => {
        setFieldValue(TAP_PANEL_GROUP_KEY, 'selectBy', selectBy);
    }, [selectBy]);

    const ctx= {
        setConstraintFragment: (key,value) => {
            value ?
                getTapBrowserState().constraintFragments.set(key,value) :
                getTapBrowserState().constraintFragments.delete(key);
        }
    };

    return (
        <Box width={1} height={1}>
            <ConstraintContext.Provider value={ctx}>
                <FormPanel  inputStyle = {{display: 'flex', flexDirection: 'column', backgroundColor: 'transparent', padding: 'none', border: 'none'}}
                            groupKey={TAP_PANEL_GROUP_KEY}
                            getDoOnClickFunc={(clickFunc) => clickFuncRef.clickFunc= clickFunc}
                            params={{hideOnInvalid: false}}
                            onSubmit={(request) => onTapSearchSubmit(request, serviceUrl,tapState)}
                            extraWidgets={makeExtraWidgets(initArgs,selectBy,setSelectBy, tapState)}
                            // extraWidgetsRight={makeExtraWidgetsRight(servicesShowing, setServicesShowing)}
                            buttonStyle={{justifyContent: 'left'}}
                            submitBarStyle={{padding: '2px 3px 3px'}}
                            includeUnmounted={true}
                            help_id = {tapHelpId('form')} >
                    <TapSearchPanelComponents {...{
                        servicesShowing, setServicesShowing,
                        initArgs, selectBy, setSelectBy, serviceUrl, onTapServiceOptionSelect, titleOn, tapOps, obsCoreEnabled}} />
                </FormPanel>
            </ConstraintContext.Provider>
        </Box>
    );

}

TapSearchPanel.propTypes= {
    titleOn: bool,
    initArgs: shape({
        searchParams: object,
        urlApi: object,
    }),
};


function TapSearchPanelComponents({initArgs, serviceUrl, servicesShowing, setServicesShowing, onTapServiceOptionSelect, tapOps, titleOn=true, selectBy, setSelectBy}) {

    const label= getServiceLabel(serviceUrl);
    const [obsCoreTableModel, setObsCoreTableModel] = useState();
    const hasObsCoreTable = obsCoreTableModel?.tableData?.data?.length > 0;

    const loadObsCoreTables = (requestServiceUrl) => {
        loadObsCoreSchemaTables(requestServiceUrl).then((tableModel) => {
            setObsCoreTableModel(tableModel);
        });
    };

    useEffect(() => {
        loadObsCoreTables(serviceUrl);
    }, [serviceUrl]);

    return (
        <FieldGroup groupKey={TAP_PANEL_GROUP_KEY} keepState={true} style={{flexGrow: 1, display: 'flex'}}>
            <div className='TapSearch'>
                {titleOn &&<Typography {...{level:'h3', sx:{m:1} }}> TAP Searches </Typography>}
                <Services {...{serviceUrl, servicesShowing, tapOps, onTapServiceOptionSelect}}/>
                { selectBy === 'adql' ?
                    <AdqlUI {...{serviceUrl, servicesShowing, setServicesShowing, setSelectBy}}/> :
                    <BasicUI  {...{serviceUrl, serviceLabel: label, selectBy, initArgs, obsCoreTableModel,
                        servicesShowing, setServicesShowing, hasObsCoreTable, setSelectBy}}/>
                }
            </div>
        </FieldGroup>
    );
}

function Services({serviceUrl, servicesShowing, tapOps, onTapServiceOptionSelect} ) {
    const [extraStyle,setExtraStyle] = useState({overflow:'hidden'});
    const [enterUrl,setEnterUrl]=  useState(false);

    useEffect(() => {
        if (servicesShowing) {
            setTimeout(() => setExtraStyle({overflow:'visible'}), 250);
        }
        else {
            setExtraStyle({overflow:'hidden'});
        }
    }, [servicesShowing]);

    return (
        <div
            className={servicesShowing?'TapSearch__section':'TapSearch__section TapSearch__hide'}
            style={{height: servicesShowing?62:0, justifyContent:'space-between', alignItems:'center',...extraStyle}}
            title={SERVICE_TIP}>
            <div style={{display:'flex', alignItems:'center', width:'100%'}}>
                <Typography {...{level:'h4', color:'primary', sx:{width:'14rem', mr:1} }}>
                    Select TAP Service
                </Typography>
                <Stack {...{direction:'row', spacing:2}}>
                    {enterUrl ? (
                            <InputField orientation='horizontal'
                                        placeholder='Enter TAP Url'
                                        value={serviceUrl}
                                        actOn={['enter']}
                                        tooltip='enter TAP URL'
                                        slotProps={{input:{sx:{width:'40rem'}}}}
                                        onChange={(val) => onTapServiceOptionSelect(val)}
                            />
                    ) : (
                        <ListBoxInputFieldView {...{
                            sx:{'& .MuiSelect-root':{width:'40rem'}},
                            options:tapOps, value:serviceUrl,
                            placeholder:'Choose TAP Service...',
                            startDecorator:!tapOps.length ? <Button loading={true}/> : undefined,
                            onChange:(ev, value) => {
                                onTapServiceOptionSelect({value});
                            },
                            renderValue:
                                ({value}) =>
                                    (<ServiceOpRender {...{ ops: tapOps, value}}/>),
                            decorator:
                                (label,value) => (<ServiceOpRender {...{ ops: tapOps, value}}/>),
                        }} /> )}

                    <Switch {...{ size:'sm', endDecorator: enterUrl? 'enter URL' : 'Use TAP List', checked:enterUrl,
                        onChange: (ev) => {
                            setEnterUrl(ev.target.checked);
                        },
                        }} />

                </Stack>
            </div>
        </div>
    );
}


function ServiceOpRender({ops, value, sx}) {
    const op = ops.find((t) => t.value === value);
    if (!op) return 'none';
    return (
        <Stack {...{alignItems:'flex-start', sx}}>
            <Stack {...{direction:'row', spacing:1, alignItems:'center'}}>
                <Typography level='title-lg'>
                    {`${op.labelOnly}: `}
                </Typography>
                <Typography level='body-lg' >
                    {op.value}
                </Typography>
            </Stack>
        </Stack>
    );
}



function makeExtraWidgets(initArgs, selectBy, setSelectBy, tapBrowserState) {
    const extraWidgets = [
        (<ValidationField orientation='horizontal' fieldKey='maxrec' key='maxrec' groupKey={TAP_PANEL_GROUP_KEY}
                         tooltip='Maximum number of rows to return (via MAXREC)' label= 'Row Limit:'
                         initialState= {{
                             value: Number(initArgs?.urlApi?.MAXREC) || Number(getAppOptions().tap?.defaultMaxrec ?? 50000),
                             validator: intValidator(0, getMaxrecHardLimit(), 'Maximum number of rows'),
                         }}
                         wrapperStyle={{marginLeft: 30}}
                         />)
        ];
    if (selectBy==='basic') {
        extraWidgets.push( (<ExtraButton key='editADQL' text='Populate and edit ADQL'
                                         onClick={() => populateAndEditAdql(tapBrowserState, setSelectBy)}
                                         style={{marginLeft: 10}} />));
    }
    else {
        extraWidgets.push( (<ExtraButton key='singleTable' text='Single Table (UI assisted)'
                                         onClick={() => setSelectBy('basic')}
                                         style={{marginLeft: 10}} />));

    }
    return extraWidgets;
}

function populateAndEditAdql(tapBrowserState,setSelectBy,inAdql) {
    const adql = inAdql ?? getAdqlQuery(tapBrowserState);
    if (!adql) return;
    const {TAP_UPLOAD,uploadFile}=  isTapUpload(tapBrowserState) ? getUploadConstraint(tapBrowserState) : {};
    setSelectBy('adql');
    dispatchMultiValueChange(TAP_PANEL_GROUP_KEY,   //set adql and switch tab to ADQL
        [
            {fieldKey: 'defAdqlKey', value: adql},
            {fieldKey: 'adqlQuery', value: adql},
            {fieldKey: 'TAP_UPLOAD', value: TAP_UPLOAD},
            {fieldKey: 'uploadFile', value: uploadFile},
        ]
    );
}

const getTapServiceOptions= () => getTapServices(webApiUserAddedService).map(({label,value})=>({label:value, value, labelOnly:label}));

function getTableNameFromADQL(adql) {
    if (!adql) return;
    const adqlUp= adql.toUpperCase();
    const start= adqlUp.toUpperCase().indexOf('FROM ')+5;
    const end= adqlUp.lastIndexOf('WHERE');
    if  (start>5 && end>-1) {
        const tStr=  adql.substring(start,end)?.trim();
        if (tStr) return tStr.split(/[\s,]+/)[0]; // pull the first works out between the FROM and the WHERE
    }
    return undefined;
}

function getTitle(adql, serviceUrl) {
    const tname = getTableNameFromADQL(adql);
    const host= serviceUrl?.match(/.*:\/\/(.*)\/.*/i)?.[1]; // table name or service url
    if (tname && host) return `${tname} - ${host}`;
    return tname || host;
}

const disableRowLimitMsg = (
    <div style={{width: 260}}>
        Disabling the row limit is not recommended. <br/>
        You are about to submit a query without a TOP or WHERE constraint. <br/>
        This may results in a HUGE amount of data. <br/><br/>
        Are you sure you want to continue?
    </div>
);


function onTapSearchSubmit(request,serviceUrl,tapBrowserState) {
    const isADQL = (request.selectBy === 'adql');
    let adql;
    let isUpload;
    let serverFile;
    let uploadTableName;
    let schemaEntry;
    let userColumns;

    if (isADQL) {
        adql = request.adqlQuery;
        const {TAP_UPLOAD,uploadFile}= request;
        isUpload = Boolean(TAP_UPLOAD && uploadFile);
        serverFile = isUpload && TAP_UPLOAD[uploadFile].serverFile;
        uploadTableName = isUpload && TAP_UPLOAD[uploadFile].table;
    }
    else {
        adql = getAdqlQuery(tapBrowserState);
        isUpload = isTapUpload(tapBrowserState);
        schemaEntry = getTapUploadSchemaEntry(tapBrowserState);
        const cols = schemaEntry.columns;
        userColumns = cols?.filter((col) => col.use).map((col) => col.name).join(',');
        serverFile = isUpload && getUploadServerFile(tapBrowserState);
        uploadTableName = isUpload && getUploadTableName(tapBrowserState);
    }

    if (!adql) return false;


    const maxrec = request.maxrec;
    const hasMaxrec = !isNaN(parseInt(maxrec));
    const doSubmit = () => {
        const serviceLabel= getServiceLabel(serviceUrl);
        const hips= getServiceHiPS(serviceUrl);
        const adqlClean = adql.replace(/\s/g, ' ');    // replace all whitespaces with spaces
        const params = {serviceUrl, QUERY: adqlClean};
        if (isUpload) {
            params.UPLOAD= serverFile;
            params.adqlUploadSelectTable= uploadTableName;
            if (!isADQL) params.UPLOAD_COLUMNS= userColumns;
        }
        if (hasMaxrec) params.MAXREC = maxrec;
        const treq = makeTblRequest('AsyncTapQuery', getTitle(adqlClean,serviceUrl), params);
        setNoCache(treq);
        const additionalMeta= {};
        if (!isADQL) {
            additionalMeta[PREF_KEY]= `${tapBrowserState.schemaName}-${tapBrowserState.tableName}`;
        }
        additionalMeta.serviceLabel= serviceLabel;
        if (hips) additionalMeta[MetaConst.COVERAGE_HIPS]= hips;

        treq.META_INFO= {...treq.META_INFO, ...additionalMeta };
        dispatchTableSearch(treq, {backgroundable: true, showFilters: true, showInfoButton: true});
    };

    if (!hasMaxrec && !adql.toUpperCase().match(/ TOP | WHERE /)) {
        showYesNoPopup(disableRowLimitMsg,(id, yes) => {
            if (yes) {
                doSubmit();
                dispatchHideDropDown();
            }
            dispatchHideDialog(id);
        });
    } else {
        doSubmit();
        return true;
    }
    return false;
}




/**
 *
 * @param {TapBrowserState} tapBrowserState
 * @param [showErrors]
 * @returns {string|null}
 */
function getAdqlQuery(tapBrowserState, showErrors= true) {
    const tableName = maybeQuote(tapBrowserState?.tableName, true);
    if (!tableName) return;
    const isUpload= isTapUpload(tapBrowserState);

    if (isUpload) { //check for more than one upload file (in Spatial and in ObjectID col) - should this be a utility function in constraints.js?
        const { constraintFragments } = tapBrowserState;
        const entries = [...constraintFragments.values()];
        const matchingEntries = entries.filter((c) => Boolean(c.uploadFile && c.TAP_UPLOAD && c.adqlConstraint));
        if (matchingEntries.length > 1) {
            if (showErrors) showInfoPopup('We currently do not support searches with more than one uploaded table.', 'Error');
            return;
        }
    }

    const helperFragment = getHelperConstraints(tapBrowserState);
    const tableCol = tableColumnsConstraints(tapBrowserState.columnsModel,
        isUpload?getAsEntryForTableName(tableName):undefined);

    const { table:uploadTable, asTable:uploadAsTable, columns:uploadColumns}= isUpload ?
        getTapUploadSchemaEntry(tapBrowserState) : {};

    const fromTables= isUpload ?
        `${tableName} AS ${getAsEntryForTableName(tableName)}, ${TAP_UPLOAD_SCHEMA}.${uploadTable} ${uploadAsTable ? 'AS '+uploadAsTable : ''}` :
        tableName;

    // check for errors
    if (!helperFragment.valid) {
        if (showErrors) showInfoPopup(helperFragment.messages[0], 'Error');
        return;
    }
    if (!tableCol.valid) {
        if (showErrors) showInfoPopup(tableCol.message, 'Error');
        return;
    }

    // build columns
    let selcols = tableCol.selcols || (isUpload ? `${tableName}.*` : '*');
    if (isUpload) {
        const ut= uploadAsTable ?? uploadTable ?? '';
        const tCol= uploadColumns.filter(({use}) => use).map( ({name}) => ut+'.'+name);
        selcols+= tCol.length ? ',\n' + makeColsLines(tCol,true) : '';
    }

    // build up constraints
    let constraints = helperFragment.where || '';
    if (tableCol.where) {
        const addAnd = Boolean(constraints);
        constraints += (addAnd ? ' AND ' : '') + `(${tableCol.where})`;
    }

    if (constraints) constraints = `WHERE ${constraints}`;

    // if we use TOP  when maxrec is set `${maxrec ? `TOP ${maxrec} `:''}`,
    // overflow indicator will not be included with the results
    // and we will not know if the results were truncated
    return `SELECT ${selcols} \nFROM ${fromTables} \n${constraints}`;
}