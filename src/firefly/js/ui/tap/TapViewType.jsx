import {Button, Divider, Sheet, Stack, Tooltip, Typography} from '@mui/joy';
import {truncate} from 'lodash';
import {bool, string, func, object, shape} from 'prop-types';
import React, {Fragment, useContext, useEffect, useRef, useState} from 'react';
import SplitPane from 'react-split-pane';
import {getColumnValues} from '../../tables/TableUtil.js';
import {isColumnsMatchingToObsTap} from '../../voAnalyzer/ColumnsModelInfo.js';
import {FieldGroupCtx} from '../FieldGroup.jsx';
import {HelpIcon} from '../HelpIcon';
import {ListBoxInputFieldView} from '../ListBoxInputField.jsx';
import {SplitContent} from '../panel/DockLayoutPanel';
import {useFieldGroupMetaState} from '../SimpleComponent.jsx';
import {AdvancedADQL} from './AdvancedADQL.jsx';

import {TableColumnsConstraints, TableColumnsConstraintsToolbar} from './TableColumnsConstraints.jsx';
import {
    ADQL, SINGLE, SpatialPanelWidth, NavButtons, TableTypeButton
} from './TableSearchHelpers.jsx';
import {TableSearchMethods} from './TableSearchMethods.jsx';
import {
    defTapBrowserState, getLoadedCapability, getTapServices, isCapabilityLoaded, loadTapCapabilities, loadTapColumns,
    loadTapSchemas, loadTapTables, tapHelpId,
} from './TapUtil.js';

import './TableSelectViewPanel.css';


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
                                serviceLabel, selectBy, initArgs, lockObsCore, obsCoreTableModel, hasObsCoreTable}) {

    return (
        <Stack {...{pt: servicesShowing?0:1, height:1}}>
            {selectBy==='adql' ?
                <AdqlUI {...{serviceUrl, serviceLabel, servicesShowing, setServicesShowing, lockService, setSelectBy}}/> :
                <BasicUI  {...{serviceUrl, serviceLabel, selectBy, initArgs, lockService, lockObsCore, obsCoreTableModel,
                    servicesShowing, setServicesShowing, hasObsCoreTable, setSelectBy}}/>
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
    serviceLabel: string,
    selectBy: string,
    initArgs: shape({
        searchParams: object,
        urlApi: object,
    }),
    lockObsCore: bool,
    obsCoreTableModel: object,
    hasObsCoreTable: bool,
};


function AdqlUI({serviceUrl, serviceLabel, servicesShowing, setServicesShowing, setSelectBy, lockService}) {
    const [,setCapabilitiesChange] = useState(); // this is just to force a rerender
    const capabilities= getLoadedCapability(serviceUrl);
    useEffect(() => {
        !isCapabilityLoaded(serviceUrl) && loadTapCapabilities(serviceUrl).then((c) => setCapabilitiesChange(c??{}));
    }, [serviceUrl]);

    return (
        <Sheet className='TapSearch__section' variant='outline' sx={{display:'flex', flexDirection: 'column', flexGrow: 1}}>
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
                <div className='expandable'>
                    <div style={{flexGrow: 1}}>
                        <AdvancedADQL {...{adqlKey:'adqlQuery', defAdqlKey:'defAdqlKey', serviceUrl, capabilities}}/>
                    </div>
                </div>
                : <div className='loading-mask'/>
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
    const {initArgs={}, setSelectBy, obsCoreTableModel, servicesShowing,
        setServicesShowing, hasObsCoreTable, lockService, lockObsCore:forceLockObsCore}= props;
    const {urlApi={},searchParams={}}= initArgs;
    const [getTapBrowserState,setTapBrowserState]= useFieldGroupMetaState(defTapBrowserState);
    const initState = getTapBrowserState();
    const [error, setError] = useState(undefined);
    const mountedRef = useRef(false);
    const {setVal}= useContext(FieldGroupCtx);
    const serviceLabel= props.serviceLabel ?? initState.serviceLabel;
    const [serviceUrl, serviceUrlRef, setServiceUrl] = useStateRef(initState.serviceUrl || props.serviceUrl);
    const [schemaName, schemaRef, setSchemaName] = useStateRef(searchParams.schema || initState.schemaName || urlApi.schema);
    const [tableName, tableRef, setTableName] = useStateRef(searchParams.table || initState.tableName || urlApi.table);
    const [obsCoreEnabled, setObsCoreEnabled] = useState(initState.obsCoreEnabled || initArgs.urlApi?.selectBy === 'obscore');
    const [,setCapabilitiesChange] = useState(); // this is just to force a rerender
    const [schemaOptions, setSchemaOptions] = useState();
    const [tableOptions, setTableOptions] = useState();
    const [columnsModel, setColumnsModel] = useState();
    const {schemaLabel}= getTapServices().find( ({value}) => value===serviceUrl) ?? {};

    const capabilities= getLoadedCapability(serviceUrl);

    useEffect(() => {
        !isCapabilityLoaded(serviceUrl) && loadTapCapabilities(serviceUrl).then((c) => setCapabilitiesChange(c??{}));
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
                setError(tableModel.error);
            } else  {
                const schemas = getColumnValues(tableModel, 'schema_name');
                if(!(schemas.length > 0)){
                    requestSchemaName = undefined;
                }
                // Discover first schema name
                if (!requestSchemaName || !schemas.includes(requestSchemaName)) {
                    requestSchemaName = schemas[0];
                }
                const schemaDescriptions = getColumnValues(tableModel, 'schema_desc');
                const schemaOptions = schemas.map((e, i) => {
                    const label = schemaDescriptions[i] ? schemaDescriptions[i] : `[${e}]`;
                    return {label, value: e};
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
                    return {label, value: e};
                });
                setTableName(requestTableName);
                setTableOptions(tableOptions);
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
        if (error) setServicesShowing(true);
    }, [error]);


    if (error) {
        return (
            <div style={{display: 'flex', flexDirection: 'column', alignItems: 'center', margin: '10px 5px'}}>
                <b>Error:</b>
                <pre style={{margin: '7px 0', whiteSpace: 'pre-wrap'}}>{error}</pre>
            </div>
        );
    }

    // need to set initialState on list fields so that the initial value that is not the first index
    // is set correctly after unmount and mount
    const sOps= schemaOptions??[];
    const tOps= tableOptions??[];
    const showTableSelectors= !(hasObsCoreTable && forceLockObsCore);
    return (
        <Fragment>
            <Sheet sx={{display:'flex', flexDirection: 'row', justifyContent:'space-between'}}>
                <Stack {...{direction:'row', justifyContent:'space-between', width:1, spacing:1}}>
                    {showTableSelectors &&
                        <Stack {...{direction:'row', alignItems:'center', width:1}}>
                            <Stack>
                                <Stack {...{direction:'row', justifyContent:'space-between', width:'17rem', alignItems:'center', mr:1}}>
                                    <Tooltip title={SCH_TAB_TITLE_TIP}>
                                        <Typography {...{level:'title-lg', color:'primary', component:'div' }}>
                                            <Stack {...{justifyContent:'center', height:55, overflow:'hidden'}}>
                                                <div style={{ textOverflow: 'ellipsis', whiteSpace: 'normal', overflow: 'hidden' }} >
                                                    {`${serviceLabel} Tables`}
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
                                                ops: sOps, value, lineClamp:2,
                                                label: schemaLabel ?? 'Table Collection (Schema)' }}/>),
                                    decorator:
                                        (label,value) => (<OpRender {...{sx:{width:'34rem', minHeight:'3rem'},
                                            ops: sOps, value,}}/>),
                                }} />
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
                                            (<OpRender {...{ ops: tOps, value, label: 'Tables', lineClamp:2, }}/>),
                                    decorator:
                                        (label,value) => (<OpRender {...{sx:{width:'34rem', minHeight:'3rem'},
                                            ops: tOps, value}}/>),
                                }} />
                            </Stack>
                        </Stack>
                    }
                    {!showTableSelectors &&
                        <Stack {...{width:1}}>
                            <Typography {...{level:'h4', component:'div', color:'primary' }}>
                                {`${serviceLabel} ObsCore data product tables (images, spectra, ect)`}
                            </Typography>
                        </Stack>
                    }
                    <div style={{display:'flex', flexDirection:'column', justifyContent: servicesShowing  ? 'center' :'space-between', alignItems:'flex-end', height:'100%'}}>
                        <NavButtons {...{setServicesShowing, servicesShowing, lockService, setNextPanel:(key) => setSelectBy(key), currentPanel:SINGLE}}/>
                    </div>
                </Stack>
            </Sheet>

            <Divider orientation='horizontal' sx={{my:1}}/>

            <div className='TapSearch__section' style={{flexDirection: 'column', flexGrow: 1}}>
                <div style={{ display: 'inline-flex', width: 'calc(100% - 3px)', justifyContent: 'space-between', margin:'5px 0 -8px 0'}}>
                    <Stack spacing={1} alignItems='center' direction={'row'}>
                        <Typography {...{level:'title-lg', color:'primary'}}>Enter Constraints</Typography>
                        <HelpIcon helpId={tapHelpId('constraints')}/>
                    </Stack>
                    <TableColumnsConstraintsToolbar key={tableName} tableName={tableName} columnsModel={columnsModel} />
                </div>
                <div className='expandable'>
                    <SplitPane split='vertical' maxSize={splitMax} mixSize={20} defaultSize={splitDef}>
                        <SplitContent>
                            {(capabilities && columnsModel) ?
                                <TableSearchMethods {...{initArgs, serviceUrl, serviceLabel, columnsModel, obsCoreEnabled, capabilities,
                                    sx:{mt:1},
                                    tableName:getTapBrowserState().tableName}}/>
                                : <div className='loading-mask'/>
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
                                : <div className='loading-mask'/>
                            }
                        </SplitContent>
                    </SplitPane>
                </div>
            </div>
        </Fragment>
    );



    function OpRender({ops, value, label='', sx, lineClamp}) {
        const op = ops.find((t) => t.value === value);
        if (!op) return 'none';
        return (
            <Stack {...{alignItems:'flex-start', alignSelf:'flex-start', sx}}>
                <Stack {...{direction:'row', spacing:1, flexWrap:'wrap'}}>
                    {label&&
                        <Typography level='title-md'>
                            {`${label}: `}
                        </Typography>
                    }
                    <Typography level='body-md' >
                        {op.value}
                    </Typography>
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
        );
    }
}

const achoreRE= /<a.*(\/>|<\/a>)/;

function cleanUp(s) {
    if (!s.includes('<a ')) return truncate(s, {length: 140});
    const aStr= s.match(achoreRE)?.[0];
    if (!aStr) return truncate(s, {length: 140});
    const tmp= document.createElement('div');
    tmp.innerHTML= aStr;
    tmp.children[0].target= 'tapOpen';
    tmp.children[0].title= tmp.children[0].innerHTML;
    tmp.children[0].innerHTML= truncate(tmp.children[0].innerHTML, {length: 80});
    return s.replace(achoreRE, tmp.innerHTML);

}
