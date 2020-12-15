import React, {Fragment, useState, useEffect, useRef} from 'react';
import SplitPane from 'react-split-pane';

import {SplitContent} from '../panel/DockLayoutPanel';
import {HelpIcon} from '../HelpIcon';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {getColumnValues} from '../../tables/TableUtil.js';

import {TableSearchMethods, SpatialPanelWidth} from './TableSearchMethods.jsx';
import {AdvancedADQL} from './AdvancedADQL.jsx';
import {
    loadTapColumns,
    loadTapTables,
    loadTapSchemas,
    getTapBrowserState,
    setTapBrowserState,
    tapHelpId,
    updateTapBrowserState,
    loadObsCoreSchemaTables,
} from './TapUtil.js';
import {NameSelect} from './Select.jsx';

import {TableColumnsConstraints, TableColumnsConstraintsToolbar} from './TableColumnsConstraints.jsx';


import './TableSelectViewPanel.css';
import {matchesObsCoreHeuristic} from '../../util/VOAnalyzer';



const SCHEMA_TIP= 'Select a table collection (TAP ‘schema’); type to search the schema names and descriptions.';
const TABLE_TIP= 'Select a table; type to search the table names and descriptions.';
const SCH_TAB_TITLE_TIP= 'Select a table collection (TAP ‘schema’), then select a table';

/**
 * group key for fieldgroup comp
 */

export const gkey = 'TAP_SEARCH_PANEL';

export const SectionTitle= ({title,helpId,tip}) => (
    <div className='TapSearch__section--title' title={tip}>{title}<HelpIcon helpId={tapHelpId(helpId)}/></div>);

export function AdqlUI({serviceUrl}) {

    return (

        <div className='TapSearch__section' style={{flexDirection: 'column', flexGrow: 1}}>
            <div style={{ display: 'inline-flex', alignItems: 'center'}}>
                <div className='TapSearch__section--title'>3. Advanced ADQL  <HelpIcon helpId={tapHelpId('adql')}/> </div>
                <div style={{color: 'brown', fontSize: 'larger'}}>ADQL edits below will not be reflected in <b>Single Table</b> view</div>
            </div>


            <div className='expandable'>
                <div style={{flexGrow: 1}}>
                    <AdvancedADQL groupKey={gkey} adqlKey='adqlQuery' defAdqlKey='defAdqlKey' tblNameKey='tableName' serviceUrl={serviceUrl}/>
                </div>
            </div>
        </div>

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

export function BasicUI(props) {
    const tapFluxState = getTapBrowserState();
    const [error, setError] = useState(undefined);
    const [serviceUrl, serviceUrlRef, setServiceUrl] = useStateRef(tapFluxState.serviceUrl || props.serviceUrl);
    const [schemaName, schemaRef, setSchemaName] = useStateRef(tapFluxState.schemaName || props.initArgs.schema);
    const [tableName, tableRef, setTableName] = useStateRef(tapFluxState.tableName || props.initArgs.table);
    const [obsCoreTables, setObsCoreTables] = useState(tapFluxState.obsCoreTables || undefined);
    const [obsCoreEnabled, setObsCoreEnabled] = useState(tapFluxState.obsCoreEnabled || false);
    const [schemaOptions, setSchemaOptions] = useState();
    const [tableOptions, setTableOptions] = useState();
    const [columnsModel, setColumnsModel] = useState();
    const selectBy = props.selectBy;

    const obsCoreSelected = selectBy === 'obscore';
    const tableSectionNumber = !obsCoreSelected ? '4' : '3';

    const splitDef = SpatialPanelWidth+80;
    const splitMax = SpatialPanelWidth+80;

    const loadSchemas = (requestServiceUrl, requestSchemaName=undefined, requestTableName=undefined) => {
        setError(undefined);
        setSchemaOptions(undefined);
        setTableName(undefined);
        setTableOptions(undefined);
        setColumnsModel(undefined);
        setObsCoreTables(undefined);
        setObsCoreEnabled(undefined);
        dispatchValueChange({groupKey: gkey, fieldKey: 'tableName', value: undefined});
        // update state for higher level components that might rely on obsCoreTables
        updateTapBrowserState({obsCoreTables: undefined});

        loadTapSchemas(requestServiceUrl).then((tableModel) => {
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
                const schemaDescriptions = getColumnValues(tableModel, 'description');
                const schemaOptions = schemas.map((e, i) => {
                    const label = schemaDescriptions[i] ? schemaDescriptions[i] : `[${e}]`;
                    return {label, value: e};
                });
                setSchemaName(requestSchemaName);
                setSchemaOptions(schemaOptions);
            }
        });

        loadObsCoreSchemaTables(requestServiceUrl).then((tableModel) => {
            const obsCoreTablesResponse = tableModel?.tableData.data || undefined;
            setObsCoreTables(obsCoreTablesResponse);
            // Update state early for ObsCore support
            // we'll still have to wait for loadTables and loadColumns
            updateTapBrowserState({obsCoreTables: obsCoreTablesResponse});
        });
    };

    const loadTables = (requestServiceUrl, requestSchemaName, requestTableName) => {
        // even if we have the new values - clear the current state
        setTableName(undefined);
        setTableOptions(undefined);
        setColumnsModel(undefined);
        dispatchValueChange({groupKey: gkey, fieldKey: 'tableName', value: undefined});

        loadTapTables(requestServiceUrl, requestSchemaName).then((tableModel) => {
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
                const tableDescriptions = getColumnValues(tableModel, 'description');
                const tableOptions = tables.map((e, i) => {
                    const label = tableDescriptions[i] ? tableDescriptions[i] : `[${e}]`;
                    return {label, value: e};
                });
                setTableName(requestTableName);
                setTableOptions(tableOptions);
                dispatchValueChange({groupKey: gkey, fieldKey: 'tableName', value: requestTableName});
            }
        });
    };

    const loadColumns = (requestServiceUrl, requestSchemaName, requestTableName) => {
        setColumnsModel(undefined);
        //dispatchValueChange({groupKey: gkey, fieldKey: 'columnsModel', value: undefined});
        loadTapColumns(requestServiceUrl, requestSchemaName, requestTableName).then((columnsModel) => {
            if (serviceUrlRef.current !== requestServiceUrl || schemaRef.current !== requestSchemaName || tableRef.current !== requestTableName){
                // processing a stale request
                return;
            }
            setColumnsModel(columnsModel);
            // May be redundant in a way - we know obsCoreTables should already be set,
            // and we should be able to just check the names, but this is a bit more robust (probably?)
            const matchesObsCore = matchesObsCoreHeuristic(schemaName, tableName, columnsModel);
            setObsCoreEnabled(matchesObsCore);
            setTapBrowserState({serviceUrl: requestServiceUrl, schemaName: requestSchemaName, schemaOptions,
                tableName: requestTableName, tableOptions, columnsModel, obsCoreEnabled: matchesObsCore, obsCoreTables});
        });
    };
    useEffect(() => {
        // properties changes due to changes in TapSearchPanel
        if(props.serviceUrl !== serviceUrl) {
            setServiceUrl(props.serviceUrl);
        }
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
        if (selectBy === 'obscore'){
            let seenTable = false;
            obsCoreTables.forEach((tableRow) => {
                const [schema, table, ...more] = tableRow;
                // match schema name/table name only (we omit columnsModel)
                if (matchesObsCoreHeuristic(schema, tableName, null)){
                    setSchemaName(schema);
                    setTableName(table);
                    seenTable = true;
                }
            });
            if (!seenTable){
                const [schema, table, ...more] = obsCoreTables[0];
                setSchemaName(schema);
                setTableName(table);
            }
        } else {
            setSchemaName(undefined);
            setTableName(undefined);
        }
    }, [serviceUrl, selectBy]);

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
    return (
        <Fragment>
            {!obsCoreSelected &&
            <div className='TapSearch__section'>
                <SectionTitle title='3. Select Table' helpId='selectTable' tip={SCH_TAB_TITLE_TIP}/>
                <div style={{display: 'inline-flex', width: '100%', marginRight: 3, maxWidth: 1000}}>
                    <div style={{flexGrow: 1}} title={SCHEMA_TIP}>
                        <NameSelect typeDesc='Table Collection (Schema)'
                                    typeDescPlural='Table Collections (Schemas)'
                                    options={schemaOptions}
                                    value={schemaName}
                                    internalHeight={'45px'}
                                    onSelect={(selectedTapSchema) => {
                                        setSchemaName(selectedTapSchema);
                                    }}
                        />
                    </div>
                    <div style={{width: 10}}/>
                    <div style={{flexGrow: 1}} title={TABLE_TIP}>
                        <NameSelect
                            typeDesc='Table'
                            typeDescPlural='Tables'
                            options={tableOptions}
                            value={tableName}
                            onSelect={(selectedTapTable) => {
                                setTableName(selectedTapTable);
                            }}
                        />
                    </div>
                </div>
            </div>
            }

            <div className='TapSearch__section' style={{flexDirection: 'column', flexGrow: 1}}>
                <div style={{ display: 'inline-flex', width: 'calc(100% - 3px)', justifyContent: 'space-between'}}>
                    <div className='TapSearch__section--title'>{tableSectionNumber}. Enter Constraints <HelpIcon helpId={tapHelpId('constraints')}/> </div>
                    <TableColumnsConstraintsToolbar key={tableName}
                                                    tableName={tableName}
                                                    columnsModel={columnsModel}
                    />
                </div>
                <div className='expandable'>
                    <SplitPane split='vertical' maxSize={splitMax} mixSize={20} defaultSize={splitDef}>
                        <SplitContent>
                            {columnsModel ?  <TableSearchMethods initArgs={props.initArgs} columnsModel={columnsModel} obsCoreEnabled={obsCoreEnabled}/>
                                : <div className='loading-mask'/>
                            }
                        </SplitContent>
                        <SplitContent>
                            { columnsModel ?
                                <div style={{height:'100%', display:'flex', flexDirection:'column'}}>
                                    <div style={{paddingBottom:4, fontWeight:'bold', marginTop:'-1px'}}>Output Column Selection and Constraints</div>
                                    <TableColumnsConstraints
                                        key={tableName}
                                        fieldKey={'tableconstraints'}
                                        columnsModel={columnsModel}
                                    />
                                </div>
                                : <div className='loading-mask'/>

                            }
                        </SplitContent>
                    </SplitPane>
                </div>
            </div>

        </Fragment>
    );
}



