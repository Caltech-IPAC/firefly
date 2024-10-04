import {Box, Button, Divider, Sheet, Skeleton, Stack, Tooltip, Typography} from '@mui/joy';
import {truncate} from 'lodash';
import {bool, string, func, object, shape} from 'prop-types';
import React, {Fragment, useContext, useEffect, useRef, useState} from 'react';
import SplitPane from 'react-split-pane';
import {getColumnIdx, getColumnValues} from '../../tables/TableUtil.js';
import {isColumnsMatchingToObsTap} from '../../voAnalyzer/ColumnsModelInfo.js';
import {FieldGroupCtx} from '../FieldGroup.jsx';
import {HelpIcon} from '../HelpIcon';
import {ListBoxInputFieldView} from '../ListBoxInputField.jsx';
import {SplitContent} from '../panel/DockLayoutPanel';
import {useFieldGroupMetaState} from '../SimpleComponent.jsx';
import {AdvancedADQL} from './AdvancedADQL.jsx';
import {showTableSelectPopup} from './TableChooser.jsx';

import {TableColumnsConstraints, TableColumnsConstraintsToolbar} from './TableColumnsConstraints.jsx';
import {
    ADQL, SINGLE, SpatialPanelWidth, NavButtons, TableTypeButton, getTapObsCoreOptions
} from './TableSearchHelpers.jsx';
import {TableSearchMethods} from './TableSearchMethods.jsx';
import {
    defTapBrowserState, getLoadedCapability, getTapServices, isCapabilityLoaded, loadTapCapabilities, loadTapColumns,
    loadTapSchemas, loadTapTables, tapHelpId, loadObsCoreMetadata, ADQL_QUERY_KEY, SERVICE_EXIST_ERROR
} from './TapUtil.js';

import MoreVertRoundedIcon from '@mui/icons-material/MoreVertRounded';
import {TableMask} from 'firefly/ui/panel/MaskPanel.jsx';


const SCHEMA_TIP= 'Select a table collection (TAP ‘schema’); type to search the schema names and descriptions.';
const TABLE_TIP= 'Select a table; type to search the table names and descriptions.';
const SCH_TAB_TITLE_TIP= 'Select a table collection (TAP ‘schema’), then select a table';

/**
 * Based on scheman name, table name, and column names - determine if this
 * is ObsCore-like enough for different ObsCore/ObsTAP widgets.
 * @param schemaName
 * @param tableName
 * @param columnsModel
 * @returns {boolean}
 */
function matchesObsCoreHeuristic(schemaName, tableName, columnsModel) {
    if (tableName?.toLowerCase() === 'ivoa.obscore') return true;
    if (schemaName?.toLowerCase() === 'ivoa' && tableName?.toLowerCase() === 'obscore') return true;
    return isColumnsMatchingToObsTap(columnsModel);
}


export function TapViewType({serviceUrl, servicesShowing, setServicesShowing, lockService, setSelectBy,
                                serviceLabel, selectBy, initArgs, lockObsCore, lockedSchemaName,
                                obsCoreLockTitle, obsCoreTableModel, hasObsCoreTable, setError}) {

    return (
        <Stack {...{pt: servicesShowing?0:1, height:1}}>
            {selectBy==='adql' ?
                <AdqlUI {...{serviceUrl, serviceLabel, servicesShowing, setServicesShowing, lockService, setSelectBy, setError}}/> :
                <BasicUI  {...{serviceUrl, serviceLabel, selectBy, initArgs, lockService,
                    lockObsCore, obsCoreLockTitle, lockedSchemaName, obsCoreTableModel,
                    servicesShowing, setServicesShowing, hasObsCoreTable, setSelectBy, setError}}/>
            }
        </Stack>
    );
}

TapViewType.propTypes= {
    serviceUrl: string,
    servicesShowing: bool,
    setServicesShowing: func,
    lockService: bool,
    setSelectBy: func,
    setError: func,
    serviceLabel: string,
    lockedSchemaName: string,
    obsCoreLockTitle: string,
    selectBy: string,
    initArgs: shape({
        searchParams: object,
        urlApi: object,
    }),
    lockObsCore: bool,
    obsCoreTableModel: object,
    hasObsCoreTable: bool,
};

const expandableTapSectionSx = {
    display: 'flex',
    position: 'relative',
    flexGrow: 1,
    width: 1,
};

function AdqlUI({serviceUrl, serviceLabel, servicesShowing, setServicesShowing, setSelectBy, lockService, setError}) {
    const [,setCapabilitiesChange] = useState(); // this is just to force a rerender
    const capabilities= getLoadedCapability(serviceUrl);
    useEffect(() => {
        if (!isCapabilityLoaded(serviceUrl)) {
            loadTapCapabilities(serviceUrl)
                .then((c) => setCapabilitiesChange(c??{}))
                .catch( (error) => {
                    setError(`Fail to retrieve capability for: ${serviceUrl}`);
                });
        }
    }, [serviceUrl]);

    return (
        <Sheet variant='outline' sx={{display:'flex', flexDirection: 'column', flexGrow: 1}}>
            <Stack spacing={1}>
                {lockService && <Typography {...{level:'title-lg', color:'primary'}}>{serviceLabel}</Typography>}
                <div style={{display:'flex', flexDirection:'column', width:'100%'}}>
                    <div style={{ display: 'inline-flex', alignItems: 'center', width: '100%', justifyContent:'space-between'}}>
                        <Stack {...{ direction: 'row', alignItems: 'center'}}>
                            <Stack {...{direction:'row', alignItems:'center', mr:4, width:'14rem', justifyContent:'space-between'}}>
                                <Typography {...{level:'title-lg', color:'primary'}}>Advanced ADQL</Typography>
                                <HelpIcon helpId={tapHelpId('adql')}/>
                            </Stack>
                            <Typography color='warning' level='body-lg'>ADQL edits below will not be reflected in <b>Single Table</b> view</Typography>
                        </Stack>
                        <NavButtons {...{setServicesShowing, servicesShowing, lockService, setNextPanel:(key) => setSelectBy(key), currentPanel:ADQL}}/>
                    </div>
                </div>
            </Stack>

            {capabilities ?
                <Box sx={expandableTapSectionSx}>
                    <AdvancedADQL {...{adqlKey:ADQL_QUERY_KEY, defAdqlKey:'defAdqlKey', serviceUrl, capabilities, setError}}/>
                </Box>
                : <Skeleton/>
            }
        </Sheet>
    );
}

function useStateRef(initialState){
    const [state, setState] = useState(initialState);
    const stateRef = useRef(initialState);

    useEffect(() => {
        stateRef.current = state;
    }, [state]);
    return [state, stateRef, setState];
}

function BasicUI(props) {
    const {initArgs={}, setSelectBy, obsCoreTableModel, servicesShowing, obsCoreLockTitle,
        setServicesShowing, lockedSchemaName, hasObsCoreTable, lockService, lockObsCore:forceLockObsCore, setError}= props;
    const {urlApi={},searchParams={}}= initArgs;
    const [getTapBrowserState,setTapBrowserState]= useFieldGroupMetaState(defTapBrowserState);
    const initState = getTapBrowserState();
    const mountedRef = useRef(false);
    const {setVal}= useContext(FieldGroupCtx);
    const serviceLabel= props.serviceLabel ?? initState.serviceLabel;
    const [serviceUrl, serviceUrlRef, setServiceUrl] = useStateRef(initState.serviceUrl || props.serviceUrl);
    const [schemaName, schemaRef, setSchemaName] = useStateRef(lockedSchemaName || searchParams.schema || initState.schemaName || urlApi.schema);
    const [tableName, tableRef, setTableName] = useStateRef(searchParams.table || initState.tableName || urlApi.table);
    const [obsCoreEnabled, setObsCoreEnabled] = useState(initState.obsCoreEnabled || initArgs.urlApi?.selectBy === 'obscore');
    const [,setCapabilitiesChange] = useState(); // this is just to force a rerender
    const [schemaOptions, setSchemaOptions] = useState();
    const [tableOptions, setTableOptions] = useState();
    const [tableTableModel, setTableTableModel] = useState();
    const [columnsModel, setColumnsModel] = useState();
    const [obsCoreMetadataModel, setObsCoreMetadataModel] = useState(undefined);
    const {schemaLabel}= getTapServices().find( ({value}) => value===serviceUrl) ?? {};

    const schemaIsLocked= !forceLockObsCore && Boolean(lockedSchemaName);

    const capabilities= getLoadedCapability(serviceUrl);

    useEffect(() => {
        if (!isCapabilityLoaded(serviceUrl)) {
            loadTapCapabilities(serviceUrl)
                .then((c) => setCapabilitiesChange(c??{}))
                .catch( (error) => {
                    setError(`${SERVICE_EXIST_ERROR}: ${serviceUrl}`);
                });
        }
    }, [serviceUrl]);

    const setLockToObsCore= (doLock) => {
        if (!hasObsCoreTable || !obsCoreTableModel?.tableData?.data) return;
        if (doLock) {
            const [schema, table] = obsCoreTableModel?.tableData?.data[0];
            setSchemaName(schema);
            setTableName(table);
        }
        else {
            const foundSchema= schemaOptions.find( (s) => {
                if (schemaOptions.length===2) return s.value==='tap_schema';
                else return s.value!=='tap_schema' && s.value!=='ivoa';
            });
            if (foundSchema) setSchemaName(foundSchema.value);
        }
    };

    const splitDef = SpatialPanelWidth+10;
    const splitMax = SpatialPanelWidth+10;

    const loadSchemas = (requestServiceUrl, requestSchemaName=undefined) => {
        setError(undefined);
        setSchemaOptions(undefined);
        setTableName(undefined);
        setTableOptions(undefined);
        setColumnsModel(undefined);
        setObsCoreEnabled(undefined);
        setVal('tableName',undefined);
        // update state for higher level components that might rely on obsCoreTables

        loadTapSchemas(requestServiceUrl).then((tableModel) => {
            if (!mountedRef.current) {
                return;
            }
            if (serviceUrlRef.current !== requestServiceUrl) {
                // stale request which won't reflect UI state if processed
                return;
            }
            if (tableModel.error) {
                setError(`${SERVICE_EXIST_ERROR}: ${serviceUrl}`);
            }
            else  {
                const schemas = getColumnValues(tableModel, 'schema_name');
                if(!(schemas.length > 0)){
                    requestSchemaName = undefined;
                }
                // Discover first schema name
                if (!requestSchemaName || !schemas.includes(requestSchemaName)) {
                    requestSchemaName = schemas[0];
                }
                const schemaDescriptions = getColumnValues(tableModel, 'schema_desc');
                const tableCnt = getColumnValues(tableModel, 'table_cnt');
                const schemaOptions = schemas.map((e, i) => {
                    const label = schemaDescriptions[i] ? schemaDescriptions[i] : `[${e}]`;
                    return {label, value: e, rows:tableCnt?.[i] ?? 0};
                });
                setSchemaName(requestSchemaName);
                setSchemaOptions(schemaOptions);
            }
        });

    };

    const loadTables = (requestServiceUrl, requestSchemaName, requestTableName) => {
        // even if we have the new values - clear the current state
        setTableName(undefined);
        setTableOptions(undefined);
        setColumnsModel(undefined);
        setVal('tableName',undefined);

        loadTapTables(requestServiceUrl, requestSchemaName).then((tableModel) => {
            if (!mountedRef.current) {
                return;
            }
            if (serviceUrlRef.current !== requestServiceUrl || schemaRef.current !== requestSchemaName){
                // Processing a stale request - skip
                return;
            }
            if (!tableModel.error) {
                const rowsCol= getRowsColumnValues(tableModel);
                const tables = getColumnValues(tableModel, 'table_name');
                if (!(tables.length > 0)) {
                    requestTableName = undefined;
                }
                if (!requestTableName || !tables.includes(requestTableName)) {
                    requestTableName = tables[0];
                }
                const tableDescriptions = getColumnValues(tableModel, 'table_desc');
                const tableOptions = tables.map((e, i) => {
                    const label = tableDescriptions[i] ? tableDescriptions[i] : `[${e}]`;
                    return {label, value: e, rows:rowsCol?.[i]};
                });
                setTableName(requestTableName);
                setTableOptions(tableOptions);
                setTableTableModel(tableModel);
                setVal('tableName',requestTableName);
            }
        });
    };

    const loadColumns = (requestServiceUrl, requestSchemaName, requestTableName) => {
        setColumnsModel(undefined);
        loadTapColumns(requestServiceUrl, requestSchemaName, requestTableName).then((columnsModel) => {
            if (!mountedRef.current) {
                return;
            }
            if (serviceUrlRef.current !== requestServiceUrl || schemaRef.current !== requestSchemaName || tableRef.current !== requestTableName){
                // processing a stale request
                return;
            }
            setColumnsModel(columnsModel);
            // May be redundant in a way - we know obsCoreTables should already be set,
            // and we should be able to just check the names, but this is a bit more robust (probably?)
            const matchesObsCore = matchesObsCoreHeuristic(schemaName, tableName, columnsModel);
            setObsCoreEnabled(matchesObsCore);
            setTapBrowserState({...getTapBrowserState(), serviceUrl: requestServiceUrl,
                schemaName: requestSchemaName, schemaOptions, tableName: requestTableName,
                tableOptions, columnsModel, obsCoreEnabled: matchesObsCore});
        });
    };

    const loadObsCoreMeta = (serviceUrl, obsCoreTableModel) => {
        const [, obsCoreTable] = obsCoreTableModel?.tableData?.data?.[0];
        const supportsObsCoreMetadataLoad = getTapObsCoreOptions(serviceLabel)?.enableMetadataLoad ?? false;
        
        if (!obsCoreTable || !supportsObsCoreMetadataLoad) {
            setObsCoreMetadataModel(undefined); //indicates loading attempt wasn't made
        }
        else {
            setObsCoreMetadataModel({loading: true}); //indicates intermediate state until tableModel is available (Promise is resolved/rejected)
            loadObsCoreMetadata(serviceUrl, obsCoreTable).then((tableModel) => {
                setObsCoreMetadataModel(tableModel);
            });
        }
    };

    useEffect(() => {
        if (forceLockObsCore && hasObsCoreTable) setLockToObsCore(true);
        mountedRef.current = true;
        // properties changes due to changes in TapSearchPanel
        if(props.serviceUrl !== serviceUrl) {
            setServiceUrl(props.serviceUrl);
        }
        return () => {
            mountedRef.current = false;
        };
    });

    useEffect(() => {
        serviceUrl && loadSchemas(serviceUrl, schemaName, tableName);
    }, [serviceUrl]);

    useEffect(() => {
        schemaName && loadTables(serviceUrl, schemaName, tableName);
    }, [serviceUrl, schemaName]);

    useEffect(() => {
        tableName && loadColumns(serviceUrl, schemaName, tableName);
    }, [serviceUrl, schemaName, tableName]);

    useEffect(() => {
        // use hasObsCoreTable instead of obsCoreEnabled to not wait until user selects the obsCore switch/table
        // fire the long-running query as soon as an ObsCore capable service is selected
        hasObsCoreTable && loadObsCoreMeta(serviceUrl, obsCoreTableModel);
    }, [hasObsCoreTable, serviceUrl, obsCoreTableModel]);

    // need to set initialState on list fields so that the initial value that is not the first index
    // is set correctly after unmount and mount
    const sOps= schemaOptions??[];
    const tOps= tableOptions??[];
    const showTableSelectors= !(hasObsCoreTable && forceLockObsCore);
    const realSchemaLabel= schemaLabel ?? 'Table Collection (Schema)';
    return (
        <Fragment>
            <Sheet sx={{display:'flex', flexDirection: 'row', justifyContent:'space-between'}}>
                <Stack {...{direction:'row', justifyContent:'space-between', width:1, spacing:1}}>
                    {showTableSelectors ?
                        <TableSelectors {...{hasObsCoreTable,obsCoreEnabled, setLockToObsCore, serviceLabel,
                            sOps,schemaName,setSchemaName, realSchemaLabel, schemaIsLocked,
                            tOps,tableTableModel, tableName,setTableName}}/> :
                        <Stack {...{width:1}}>
                            <Typography {...{level:'h4', component:'div', color:'primary' }}>
                                {obsCoreLockTitle ?? `${serviceLabel} ObsCore data product tables (images, spectra, etc.)`}
                            </Typography>
                        </Stack>
                    }
                    <div style={{display:'flex', flexDirection:'column', justifyContent: servicesShowing  ? 'center' :'space-between', alignItems:'flex-end', height:'100%'}}>
                        <NavButtons {...{setServicesShowing, servicesShowing, lockService, setNextPanel:(key) => setSelectBy(key), currentPanel:SINGLE}}/>
                    </div>
                </Stack>
            </Sheet>

            <Divider orientation='horizontal' sx={{my:1}}/>

            <Stack sx={{flexGrow: 1}}>
                <div style={{ display: 'inline-flex', width: 'calc(100% - 3px)', justifyContent: 'space-between', margin:'5px 0 -8px 0'}}>
                    <Stack spacing={1} alignItems='center' direction={'row'}>
                        <Typography {...{level:'title-lg', color:'primary'}}>Enter Constraints</Typography>
                        <HelpIcon helpId={tapHelpId('constraints')}/>
                    </Stack>
                    <TableColumnsConstraintsToolbar key={tableName} tableName={tableName} columnsModel={columnsModel} />
                </div>
                <Box sx={expandableTapSectionSx}>
                    <SplitPane split='vertical' maxSize={splitMax} mixSize={20} defaultSize={splitDef}>
                        <SplitContent>
                            {(capabilities && columnsModel) ?
                                <TableSearchMethods {...{initArgs, serviceUrl, serviceLabel, columnsModel, obsCoreEnabled,
                                    capabilities, obsCoreMetadataModel,
                                    sx:{mt:1},
                                    tableName:getTapBrowserState().tableName}}/>
                                : <Skeleton/>
                            }
                        </SplitContent>
                        <SplitContent>
                            { columnsModel ?
                                <Stack {...{height:1}}>
                                    <Typography title='Number of columns to be selected' color='neutral'  level='body-xs'>
                                        Output Column Selection and Constraints
                                    </Typography>
                                    <TableColumnsConstraints
                                        key={tableName}
                                        fieldKey={'tableconstraints'}
                                        columnsModel={columnsModel}
                                    />
                                </Stack>
                                : <TableMask/>
                            }
                        </SplitContent>
                    </SplitPane>
                </Box>
            </Stack>
        </Fragment>
    );

}

function TableSelectors({hasObsCoreTable,obsCoreEnabled, setLockToObsCore, serviceLabel,
                            sOps,schemaName,setSchemaName, realSchemaLabel, schemaIsLocked,
                            tOps,tableTableModel, tableName,setTableName}) {
    return (
        <Stack {...{direction:'row', alignItems:'center', width:1}}>
                <Stack>
                    <Stack {...{direction:'row', justifyContent:'space-between', width:'17rem', alignItems:'center', mr:1}}>
                        <Tooltip title={SCH_TAB_TITLE_TIP}>
                            <Typography {...{level:'title-lg', color:'primary', component:'div' }}>
                                <Stack {...{justifyContent:'center', height:55, overflow:'hidden'}}>
                                    <div style={{ textOverflow: 'ellipsis', whiteSpace: 'normal', overflow: 'hidden' }} >
                                        {schemaIsLocked ?
                                            `${serviceLabel}: ${schemaName}` :
                                            `${serviceLabel} Tables`}
                                    </div>
                                </Stack>
                            </Typography>
                        </Tooltip>
                        <HelpIcon helpId={tapHelpId('selectTable')}/>
                    </Stack>
                    {hasObsCoreTable && <TableTypeButton {...{
                        sx: {mr: 1},
                        lockToObsCore:obsCoreEnabled, setLockToObsCore}}/>}
                </Stack>
            <Stack {...{direction: 'row', width:1, spacing:1, mr: 1/2, maxWidth: 1000, justifyContent:'space-between'}}>
                {!schemaIsLocked &&
                    <SchemaChooser {...{sOps,schemaName,setSchemaName,schemaLabel:realSchemaLabel}}/>
                }
                <TableChooser {...{tOps,tableTableModel, tableName,setTableName,popupTitle:`${realSchemaLabel}: ${schemaName}`}}/>
                {schemaIsLocked && <Box width={1}/>}
            </Stack>
        </Stack>
    );
}

function SchemaChooser({sOps,schemaName,setSchemaName,schemaLabel }) {
    return (
        <Stack width={1}>
            <ListBoxInputFieldView {...{
                sx:{
                    width:1,
                    '& .MuiSelect-root':{minWidth:'12rem', flex:'1 1 auto', height:'5rem'}},
                title:SCHEMA_TIP,
                options:sOps, value:schemaName, placeholder:'Loading...',
                startDecorator:!sOps.length ? <Button loading={true}/> : undefined,
                onChange:(ev, selectedTapSchema) => setSchemaName(selectedTapSchema),
                renderValue:
                    ({value}) =>
                        (<OpRender {...{
                            ops: sOps, value, lineClamp:2, label: schemaLabel, rowDesc:'tables'}}/>),
                decorator:
                    (label,value) => (<OpRender {...{sx:{width:'34rem', minHeight:'3rem'},
                        ops: sOps, value, rowDesc:'tables'}}/>),
            }} />
            <Typography level='body-xs' pl={1}>{`${schemaLabel} count: ${sOps.length}`}</Typography>
        </Stack>
    );

}

function TableChooser({tOps,tableTableModel, tableName,setTableName,popupTitle}) {
    const {setVal}= useContext(FieldGroupCtx);
    return (
        <Stack width={1}>
            {(!tOps?.length || tOps.length<50) ?
                <ListBoxInputFieldView {...{
                    sx:{
                        width:1,
                        '& .MuiSelect-root':{minWidth:'12rem', flex:'1 1 auto', height:'5rem'}},
                    title:TABLE_TIP,
                    options:tOps, value:tableName, placeholder:'Loading...',
                    startDecorator:!tOps.length ? <Button loading={true}/> : undefined,
                    onChange:(ev, selectedTapTable) => {
                        setTableName(selectedTapTable);
                        setVal('tableName',selectedTapTable);
                    },
                    renderValue:
                        ({value}) =>
                            (<OpRender {...{ ops: tOps, value, label: 'Tables', lineClamp:2, rowDesc:'rows' }}/>),
                    decorator:
                        (label,value) => (<OpRender {...{sx:{width:'34rem', minHeight:'3rem', rowDesc:'rows'},
                            ops: tOps, value}}/>),
                }} /> :
                <Button {...{ color:'neutral', variant:'outlined',
                    sx:{
                        justifyContent:'space-between', overflow:'hidden',
                        fontWeight: 'unset',
                        width:1, minWidth:'12rem', flex:'1 1 auto', height:'5rem'},
                    endDecorator: <MoreVertRoundedIcon/>,
                    onClick:() => showTableSelectPopup(
                        `Choose Table- ${popupTitle}`,
                        tableTableModel,
                        (selectedTapTable) => {
                            setTableName(selectedTapTable);
                            setVal('tableName',selectedTapTable);
                        }
                    ) }}>
                    <OpRender {...{sx:{minHeight:'3rem', rowDesc:'rows'},
                        label: 'Tables',
                        ops: tOps, value:tableName}}/>
                </Button>
            }
            <Typography level='body-xs' pl={1}>{`Table count: ${tOps.length}`}</Typography>
        </Stack>
    );

}

function OpRender({ops, value, label='', sx, lineClamp, rowDesc='rows'}) {
    const op = ops.find((t) => t.value === value);
    if (!op) return 'none';
    const details= cleanUp(op.label,0);
    return (
        <Tooltip title={
                <Typography level='body-sm' component='div' width='30rem'>
                    <div dangerouslySetInnerHTML={{__html: `${details}`}}/>
                </Typography>
        }>
            <Stack {...{alignItems: 'flex-start', alignSelf:'flex-start', sx}}>
                <Stack {...{direction:'row', spacing:1, alignItems:'flex-end', flexWrap:'wrap'}}>
                    {label&&
                        <Typography level='title-md'>
                            {`${label}: `}
                        </Typography>
                    }
                    <Typography level='body-md' >
                        {op.value}
                    </Typography>
                    {op.rows &&
                        <>
                            <Typography level='body-sm'> {`(${rowDesc}:`} </Typography>
                            <Typography level='body-sm' color='warning'> {`${op.rows})`} </Typography>
                        </>}
                </Stack>
                <Typography level='body-sm' component='div'
                            style={lineClamp?
                                {
                                    overflow: 'hidden',
                                    textOverflow: 'ellipsis',
                                    WebkitLineClamp: lineClamp+'',
                                    display: '-webkit-box',
                                    WebkitBoxOrient: 'vertical',
                                } : {}}
                            sx={{whiteSpace:'normal', textAlign:'left'}}>
                <div dangerouslySetInnerHTML={{__html: `${cleanUp(op.label)}`}}/>
                </Typography>
            </Stack>
        </Tooltip>
    );
}



const achoreRE= /<a.*(\/>|<\/a>)/;

function cleanUp(s, truncLength=140) {
    if (!s.includes('<a ')) return truncLength>0 ? truncate(s, {length: truncLength}) : s;
    const aStr= s.match(achoreRE)?.[0];
    if (!aStr) return truncLength>0 ? truncate(s, {length: truncLength}) : s;
    const tmp= document.createElement('div');
    tmp.innerHTML= aStr;
    tmp.children[0].target= 'tapOpen';
    tmp.children[0].title= tmp.children[0].innerHTML;
    tmp.children[0].innerHTML= truncate(tmp.children[0].innerHTML, {length: 80});
    return s.replace(achoreRE, tmp.innerHTML);

}

function getRowsColumnValues(tableModel) {
    let rowIdx= getColumnIdx(tableModel, 'nrows');
    if (rowIdx>-1) return getColumnValues(tableModel,'nrows');
    rowIdx= getColumnIdx(tableModel, 'irsa_nrows');
    if (rowIdx>-1) return getColumnValues(tableModel,'irsa_nrows');
}
