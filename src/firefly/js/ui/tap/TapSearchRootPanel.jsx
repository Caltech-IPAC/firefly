/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
import {Box, Button, ChipDelete, FormHelperText, Stack, Typography} from '@mui/joy';
import {getAppOptions} from 'firefly/core/AppDataCntlr.js';
import {dispatchHideDialog} from 'firefly/core/ComponentCntlr.js';
import {dispatchHideDropDown} from 'firefly/core/LayoutCntlr.js';
import {dispatchMultiValueChange} from 'firefly/fieldGroup/FieldGroupCntlr.js';
import FieldGroupUtils, {getFieldVal} from 'firefly/fieldGroup/FieldGroupUtils.js';
import {PREF_KEY} from 'firefly/tables/TablePref.js';
import {makeTblRequest, setNoCache} from 'firefly/tables/TableRequestUtil.js';
import {dispatchTableSearch} from 'firefly/tables/TablesCntlr.js';
import {FieldGroup, FieldGroupCtx} from 'firefly/ui/FieldGroup.jsx';
import {FormPanel} from 'firefly/ui/FormPanel.jsx';
import {showInfoPopup, showYesNoPopup} from 'firefly/ui/PopupUtil.jsx';

import {makeColsLines, tableColumnsConstraints} from 'firefly/ui/tap/TableColumnsConstraints.jsx';
import {
    addUserService,
    ADQL_QUERY_KEY,
    defTapBrowserState, deleteUserService, getAsEntryForTableName, getMaxrecHardLimit, getTapServices,
    loadObsCoreSchemaTables,
    makeNumberedTitle,
    makeTapSearchTitle,
    maybeQuote, TAP_UPLOAD_SCHEMA, tapHelpId, USER_ENTERED_TITLE,
} from 'firefly/ui/tap/TapUtil.js';
import {ValidationField} from 'firefly/ui/ValidationField.jsx';
import {intValidator} from 'firefly/util/Validate.js';
import {makeSearchOnce} from 'firefly/util/WebUtil.js';
import {TextButton} from 'firefly/visualize/ui/Buttons.jsx';
import {once} from 'lodash';
import {bool, object, shape, string} from 'prop-types';
import React, {useContext, useEffect, useRef, useState} from 'react';
import {MetaConst} from '../../data/MetaConst.js';
import {InputField} from '../InputField.jsx';
import {ListBoxInputFieldView} from '../ListBoxInputField.jsx';
import {useFieldGroupMetaState, useFieldGroupValue} from '../SimpleComponent.jsx';
import {SwitchInputField} from '../SwitchInputField.jsx';
import {
    ConstraintContext, getHelperConstraints, getTapUploadSchemaEntry, getUploadConstraint, getUploadServerFile,
    getUploadTableName, isTapUpload
} from './Constraints.js';
import {TitleCustomizeButton} from './TableSearchHelpers';
import {TapViewType} from './TapViewType.jsx';

const DEFAULT_TAP_PANEL_GROUP_KEY = 'TAP_PANEL_GROUP_KEY';

//-------------
//-------------
// set up the one time (once) functions to be used with the API via initArgs. initArgs is really only used the
// first time though the UI.
//-------------
//-------------

const initServiceUsingAPIOnce= makeSearchOnce(false); // call one time during first construction
const searchFromAPIOnce= makeSearchOnce(); // setup options to immediately execute the search the first time

const activateInitArgsAdqlOnce= once((tapPanelGroupKey,tapBrowserState,initArgs,setSelectBy) => {
    const {adql}= initArgs?.urlApi ?? {};
    if (adql) {
        setTimeout(() => populateAndEditAdql(tapPanelGroupKey,tapBrowserState,setSelectBy, adql), 5);
    }
});


/** if an extra service is found from the api that is not in the list then set webApiUserAddedService */
const initApiAddedServiceOnce= once((initArgs) => {
    const {service}= initArgs?.urlApi ?? {};
    if (service) {
        const listedEntry= getTapServiceOptions().find( (e) => e.value===service);
        if (!listedEntry) {
            addUserService(service);
        }
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

function getInitServiceUrl(tapBrowserState,initArgs,tapOps, lockedServiceUrl,lockedServiceName) {
    if (lockedServiceUrl) return lockedServiceUrl;
    if (lockedServiceName) {
        const url= tapOps.find( ({labelOnly}) => labelOnly===lockedServiceName)?.value;
        if (url) return url;
    }
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


export function TapSearchPanel({initArgs= {}, titleOn=false,
                                   lockService=false, lockedServiceUrl, lockedServiceName, lockObsCore=false,
                                   lockedSchemaName,
                                   obsCoreLockTitle,
                                   groupKey=DEFAULT_TAP_PANEL_GROUP_KEY }) {

    return (
        <FieldGroup groupKey={groupKey} keepState={true} key={groupKey} sx={{width: 1, height: 1}}>
            <TapSearchPanelImpl {...{initArgs, titleOn, lockService, obsCoreLockTitle, lockedServiceUrl,
                lockedSchemaName, lockedServiceName, lockObsCore}}/>
        </FieldGroup>
    );
}

function TapSearchPanelImpl({initArgs= {}, titleOn=true, lockService=false, lockedServiceUrl, lockedServiceName,
                                lockedSchemaName,
                                obsCoreLockTitle, lockObsCore}) {
    const {setVal,getVal,setFld,groupKey}= useContext(FieldGroupCtx);
    const [getTapBrowserState,setTapBrowserState]= useFieldGroupMetaState(defTapBrowserState);
    const [getUserTitle,setUserTitle]= useFieldGroupValue(USER_ENTERED_TITLE);
    const [srvNameKey, setSrvNameKey]= useState(() => getServiceNamesAsKey());
    const tapState= getTapBrowserState();
    if (!initArgs?.urlApi?.execute) searchFromAPIOnce(true); // if not execute then mark as done, i.e. disable any auto searching
    initApiAddedServiceOnce(initArgs);  // only look for the extra service the first time
    const tapOps= getTapServiceOptions();
    const {current:clickFuncRef} = useRef({clickFunc:undefined});
    const [selectBy, setSelectBy]= useState(() => {
        const val= getVal('selectBy');
        if (val) return val;
        if (initArgs?.urlApi?.adql) return 'adql';
        return initArgs?.urlApi?.selectBy || 'basic';
    });
    const [servicesShowing, setServicesShowingInternal]= useState(tapState.lastServicesShowing);
    const [obsCoreTableModel, setObsCoreTableModel] = useState();
    const [serviceUrl, setServiceUrl]= useState(() => getInitServiceUrl(tapState,initArgs,tapOps,lockedServiceUrl,lockedServiceName));
    activateInitArgsAdqlOnce(groupKey, tapState,initArgs,setSelectBy);


    const setServicesShowing= (showing) => {
        setServicesShowingInternal(showing);
        setTapBrowserState({...getTapBrowserState(), lastServicesShowing:showing});
    };

    const obsCoreEnabled = obsCoreTableModel?.tableData?.data?.length > 0;

    const onTapServiceOptionSelect= (selectedOption) => {
        if (!selectedOption) {
            setSrvNameKey(getServiceNamesAsKey());
            return;
        }
        setVal(ADQL_QUERY_KEY, '');
        setFld(ADQL_QUERY_KEY, {placeholder: '', value: ''});
        const serviceUrl= selectedOption?.value;
        setServiceUrl(serviceUrl);
        setObsCoreTableModel(undefined);
        setSrvNameKey(getServiceNamesAsKey());
        setTapBrowserState({...getTapBrowserState(), serviceUrl});
    };

    useEffect(() => {
        const {serviceUrl:u}= initArgs?.searchParams ?? {};
        u && u!==serviceUrl && setServiceUrl(u);
    }, [initArgs?.searchParams?.serviceUrl]);

    useEffect(() => {
        return FieldGroupUtils.bindToStore( groupKey, (fields) => {
            const ts= getTapBrowserState();
            setObsCoreTableModel(ts.obsCoreTableModel);

            searchFromAPIOnce( // searchFromAPIOnce only matters if the urlApi.execute is true
                () => validateAutoSearch(fields,initArgs,ts),
                () => setTimeout(() => clickFuncRef.clickFunc?.(), 5));
        });
    }, []);

    useEffect(() => {
        setVal('selectBy', selectBy);
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
                <FormPanel  onSuccess={(request) => onTapSearchSubmit(request, serviceUrl,tapState, setUserTitle)}
                            cancelText=''
                            help_id = {tapHelpId('form')}
                            slotProps={{
                                completeBtn: {
                                    getDoOnClickFunc: (clickFunc) => (clickFuncRef.clickFunc= clickFunc),
                                    requireAllValid:false,
                                    includeUnmounted:true
                                },
                                searchBar: {
                                    px:1, py:1/2, alignItems:'center',
                                    actions: makeExtraWidgets(groupKey, initArgs,selectBy,setSelectBy,
                                        getUserTitle, setUserTitle, tapState)
                                }
                            }}>

                    <TapSearchPanelComponents {...{
                        servicesShowing, setServicesShowing, lockService, lockObsCore, obsCoreLockTitle,
                        lockedSchemaName, srvNameKey,
                        initArgs, selectBy, setSelectBy, serviceUrl, onTapServiceOptionSelect, titleOn, tapOps, obsCoreEnabled}} />
                </FormPanel>
            </ConstraintContext.Provider>
        </Box>
    );

}

TapSearchPanel.propTypes= {
    titleOn: bool,
    groupKey: string,
    lockedServiceUrl: string,
    lockedServiceName: string,
    lockedSchemaName: string,
    obsCoreLockTitle: string,
    lockService: bool,
    lockObsCore: bool,
    initArgs: shape({
        searchParams: object,
        urlApi: object,
    }),
};


function TapSearchPanelComponents({initArgs, serviceUrl, servicesShowing, setServicesShowing, onTapServiceOptionSelect,
                                      lockService, lockObsCore, obsCoreLockTitle, tapOps,
                                      lockedSchemaName, titleOn=true, selectBy, setSelectBy}) {

    const serviceLabel= getServiceLabel(serviceUrl);
    const [obsCoreTableModel, setObsCoreTableModel] = useState();
    const [error, setError] = useState(undefined);
    const hasObsCoreTable = obsCoreTableModel?.tableData?.data?.length > 0;

    const loadObsCoreTables = (requestServiceUrl) => {
        loadObsCoreSchemaTables(requestServiceUrl).then((tableModel) => {
            setObsCoreTableModel(tableModel);
        });
    };

    const showWarning= error || !serviceUrl;

    useEffect(() => {
        if (error) setServicesShowing(true);
    }, [error]);

    useEffect(() => {
        if (!serviceUrl) setServicesShowing(true);
        if (serviceUrl) setError(undefined);
    }, [serviceUrl]);

    useEffect(() => {
        loadObsCoreTables(serviceUrl);
    }, [serviceUrl]);

    return (
        <Stack flexGrow={1}>
            {titleOn &&<Typography {...{level:'h3', sx:{m:1} }}> TAP Searches </Typography>}
            <Services {...{serviceUrl, servicesShowing: (servicesShowing && !lockService),
                    tapOps, onTapServiceOptionSelect}}/>
            { showWarning ?
                <ServiceWarning {...{error,serviceUrl}}/> :
                <TapViewType  {...{
                    serviceUrl, serviceLabel, selectBy, initArgs, lockService,
                    lockObsCore, obsCoreLockTitle, obsCoreTableModel, lockedSchemaName,
                    servicesShowing, setServicesShowing, hasObsCoreTable, setSelectBy, setError
                }} />
            }
        </Stack>
    );
}

function ServiceWarning({error,serviceUrl}) {

    if (!serviceUrl) {
        return (
            <Typography level='h3' color='warning' sx={{textAlign:'center', mt:5}}>Select a TAP Service</Typography>
            );
    }
    else if (error)  {
        return (
            <Stack>
                <Typography level='h3' color='warning' sx={{textAlign:'center', mt:5}}>Error</Typography>
                <Typography color='warning' sx={{textAlign:'center', mt:5}}>{error}</Typography>
            </Stack>
        );
    }
    return <div/>;

}

function Services({serviceUrl, servicesShowing, tapOps, onTapServiceOptionSelect} ) {
    const [extraStyle,setExtraStyle] = useState({overflow:'hidden'});
    const enterUrl=  useFieldGroupValue('enterUrl')[0]();

    useEffect(() => {
        if (servicesShowing) {
            setTimeout(() => setExtraStyle({overflow:'visible'}), 250);
        }
        else {
            setExtraStyle({overflow:'hidden'});
        }
    }, [servicesShowing]);

    return (
        <Stack sx={{
            height: servicesShowing ? 'auto' : 0,
            pb: servicesShowing ? 1.5 : 0,
            justifyContent:'space-between',
            alignItems:'center',
            transition: 'all .2s ease-in-out', //to animate height changes (hide/show Services)
            ...extraStyle}}>
            <Stack direction='row' spacing={1} sx={{alignItems:'center', width:1}}>
                <Stack alignItems='flex-start' spacing={1}>
                    <Typography {...{level:'title-lg', color:'primary', sx:{width:'17rem', mr:1} }}>
                        Select TAP Service
                    </Typography>
                    <SwitchInputField {...{ size:'sm', endDecorator:'Enter my URL', fieldKey:'enterUrl',
                        initState:{value:false},
                        sx: {
                            alignSelf: 'flex-start',
                            '--Switch-trackWidth': '20px',
                            '--Switch-trackHeight': '12px',
                        },
                    }} />
                </Stack>
                <Stack>
                    {enterUrl ? (
                            <InputField orientation='horizontal'
                                        placeholder='Enter TAP Url'
                                        value={serviceUrl}
                                        actOn={['enter']}
                                        tooltip='enter TAP URL'
                                        slotProps={{input:{sx:{width:'40rem', height: '2.25rem'}}}}
                                        onChange={(val) => {
                                            onTapServiceOptionSelect(val);
                                            addUserService(val.value);
                                        }}
                            />

                    ) : (
                        <ListBoxInputFieldView {...{
                            sx:{'& .MuiSelect-root':{width:'46.5rem'}},
                            options:tapOps, value:serviceUrl,
                            placeholder:'Choose TAP Service...',
                            startDecorator:!tapOps.length ? <Button loading={true}/> : undefined,
                            onChange:(ev, value) => {
                                onTapServiceOptionSelect({value});
                            },
                            renderValue:
                                ({value}) =>
                                    (<ServiceOpRender {...{ ops: tapOps, value,
                                        onTapServiceOptionSelect, clearServiceOnDelete:true}}/>),
                            decorator:
                                (label,value) => (<ServiceOpRender {...{ ops: tapOps, value, onTapServiceOptionSelect}}/>),
                        }} /> )}
                    <FormHelperText sx={{m: .25}}>
                        {enterUrl ? 'Type the url of a TAP service & press enter' : 'Choose a TAP service from the list'}
                    </FormHelperText>
                </Stack>
            </Stack>
        </Stack>
    );
}


function ServiceOpRender({ops, value, onTapServiceOptionSelect, sx, clearServiceOnDelete=false}) {
    const op = ops.find((t) => t.value === value);
    if (!op) return 'none';
    return (
        <Stack {...{alignItems:'flex-start', sx:{...sx, width:1}}}>
            <Stack {...{direction:'row', spacing:5, alignItems:'center', justifyContent:'space-between', width:1}}>
                <Stack {...{direction:'row', spacing:1, alignItems:'center'}}>
                    <Typography level='title-md'>
                        {`${op.labelOnly}: `}
                    </Typography>
                    <Typography level='body-md' >
                        {op.value}
                    </Typography>
                </Stack>
                { op.userAdded &&
                    <ChipDelete component='div' size='sm' sx={{zIndex:2}}
                                onClick={(e) => {
                                    deleteUserService(value);
                                    if (clearServiceOnDelete) onTapServiceOptionSelect({value:''});
                                    else onTapServiceOptionSelect();
                                    e.stopPropagation?.();
                                }}
                    />
                }
            </Stack>
        </Stack>
    );
}


function makeExtraWidgets(groupKey, initArgs, selectBy, setSelectBy, getUserTitle, setUserTitle, tapBrowserState) {
    const extraWidgets = [
        (<ValidationField {...{
            orientation: 'horizontal', fieldKey: 'maxrec', key: 'maxrec', groupKey,
            tooltip: 'Maximum number of rows to return (via MAXREC)', label: 'Row Limit:',
            validator: intValidator(1, getMaxrecHardLimit(), 'Maximum number of rows'),
            initialState: {
                value: Number(initArgs?.urlApi?.MAXREC) || Number(getAppOptions().tap?.defaultMaxrec ?? 50000),
            },
            sx: {pl: 3},
            slotProps : {
                input : {sx: { width: '8em' } }
            },
        }}/>)
    ];
    extraWidgets.push( <TitleCustomizeButton {...{key:'setTitle', groupKey,
        tapBrowserState,selectBy, getADQL: () => getFieldVal(groupKey,ADQL_QUERY_KEY,'') }}/> );

    extraWidgets.push(
        <Box key={'whichButton'}>
            {
                selectBy==='basic' ?
                    <TextButton text='Populate and edit ADQL' sx={{ml:5}}
                                onClick={() => populateAndEditAdql(groupKey, tapBrowserState, setSelectBy)} />
                    :
                    <TextButton text='Single Table (UI assisted)' sx={{ml:5}}
                                onClick={() => setSelectBy('basic')} />
            }
        </Box>
    );

    return extraWidgets;
}

function populateAndEditAdql(groupKey,tapBrowserState,setSelectBy,inAdql) {
    const adql = inAdql ?? getAdqlQuery(tapBrowserState);
    if (!adql) return;
    const {TAP_UPLOAD,uploadFile}=  isTapUpload(tapBrowserState) ? getUploadConstraint(tapBrowserState) : {};
    setSelectBy('adql');
    dispatchMultiValueChange(groupKey,   //set adql and switch tab to ADQL
        [
            {fieldKey: 'defAdqlKey', value: adql},
            {fieldKey: ADQL_QUERY_KEY, value: adql},
            {fieldKey: 'TAP_UPLOAD', value: TAP_UPLOAD},
            {fieldKey: 'uploadFile', value: uploadFile},
        ]
    );
}


const getServiceNamesAsKey= () => getTapServiceOptions().map(({label}) => label).join('-');

const getTapServiceOptions= () =>
    getTapServices().map(({label,value,userAdded=false})=>({label:value, value, labelOnly:label, userAdded}));

const disableRowLimitMsg = (
    <div style={{width: 260}}>
        Disabling the row limit is not recommended. <br/>
        You are about to submit a query without a TOP or WHERE constraint. <br/>
        This may results in a HUGE amount of data. <br/><br/>
        Are you sure you want to continue?
    </div>
);


function onTapSearchSubmit(request,serviceUrl,tapBrowserState,setUserTitle) {
    const isUserEnteredADQL = (request.selectBy === 'adql');
    let adql;
    let isUpload;
    let serverFile;
    let uploadTableName;
    let schemaEntry;
    let userColumns;
    const userTitle= request[USER_ENTERED_TITLE];
    console.log(userTitle);

    if (isUserEnteredADQL) {
        adql = request[ADQL_QUERY_KEY];
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
            if (!isUserEnteredADQL) params.UPLOAD_COLUMNS= userColumns;
        }
        if (hasMaxrec) params.MAXREC = maxrec;
        const title= makeNumberedTitle(userTitle || makeTapSearchTitle(adqlClean,serviceUrl));
        const treq = makeTblRequest('AsyncTapQuery', title, params);
        setNoCache(treq);
        const additionalMeta= {};
        if (!isUserEnteredADQL) {
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
        constraints += (addAnd ? '\n      AND ' : '') + `(${tableCol.where})`;
    }

    if (constraints) constraints = `WHERE ${constraints}`;

    // if we use TOP  when maxrec is set `${maxrec ? `TOP ${maxrec} `:''}`,
    // overflow indicator will not be included with the results,
    // and we will not know if the results were truncated
    return `SELECT ${selcols} \nFROM ${fromTables} \n${constraints}`;
}