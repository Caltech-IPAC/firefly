import React, {PureComponent} from 'react';
import Select from 'react-select';
import CreatableSelect from 'react-select/lib/Creatable';
import {get, pick, truncate} from 'lodash';
import {logError} from '../../util/WebUtil.js';
import {FormPanel} from '../FormPanel.jsx';
import {FieldGroup} from '../FieldGroup.jsx';
import {FieldGroupTabs, Tab} from '../panel/TabPanel.jsx';
import {dispatchMultiValueChange, dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {ListBoxInputField, ListBoxInputFieldView} from '../ListBoxInputField.jsx';
import {ValidationField} from '../ValidationField.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {makeTblRequest, makeFileRequest} from '../../tables/TableRequestUtil.js';
import {doFetchTable, getColumnIdx, getColumnValues, sortTableData, onTableLoaded, getTblById} from '../../tables/TableUtil.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';

import {CatalogConstraintsPanel, getTblId} from '../../visualize/ui/CatalogConstraintsPanel.jsx';
import {validateSql} from '../../visualize/ui/CatalogSelectViewPanel.jsx';
import {TableSearchMethods, tableSearchMethodsConstraints} from './TableSearchMethods.jsx';

import './TableSelectViewPanel.css';

/**
 * group key for fieldgroup comp
 */
const componentKey = 'TAP_BROWSER'; // key to store component state
const gkey = 'TAP_SEARCH_PANEL';


const qFragment = '/sync?REQUEST=doQuery&LANG=ADQL&';



// on the left tap tables browser
// on the bottom - column constraints
export class TapSearchPanel extends PureComponent {
    constructor(props) {
        super(props);
        const serviceUrl = get(getComponentState(componentKey), 'serviceUrl', TAP_SERVICE_OPTIONS[0].value);
        this.state = {serviceUrl, freeForm: getFieldVal(gkey, 'tabs') === 'adql'};

        this.populateAndEditAdql = this.populateAndEditAdql.bind(this);
        this.onTapServiceOptionSelect = this.onTapServiceOptionSelect.bind(this);
        this.onTabSelect = this.onTabSelect.bind(this);
    }

    componentDidMount() {
        if (!getFieldVal(gkey,'adqlQuery')) {
            const sampleQuery = getSampleQuery(this.state.serviceUrl);
            dispatchValueChange({groupKey: gkey, fieldKey: 'adqlQuery', placeholder: sampleQuery, value: sampleQuery});
        }
    }

    render() {
        const {serviceUrl, freeForm} = this.state;

        // keep all tabs the same size
        const tabWrapper = {padding:5, minWidth:1100, minHeight:100};

        const placeholder = serviceUrl ? `Using <${serviceUrl}>. Replace...` : 'Select TAP...';

        const rightBtns = freeForm ? [] : [{text: 'Populate and edit ADQL', onClick: this.populateAndEditAdql}];

        return (
            <div style={{position: 'relative', padding: 10}}>
                <div style={{paddingBottom:5}}>
                    <CreatableSelect
                        options={TAP_SERVICE_OPTIONS}
                        isClearable={true}
                        onChange={this.onTapServiceOptionSelect}
                        placeholder={placeholder}
                    />
                </div>
                <FormPanel
                    groupKey={gkey}
                    onSubmit={(request) => onSearchSubmit(request, serviceUrl)}
                    extraButtons={rightBtns}>

                    <FieldGroup groupKey={gkey} keepState={true}>
                        <FieldGroupTabs initialState={{ value:'ui' }}
                                        fieldKey='tabs'
                                        resizable={true}
                                        onTabSelect={this.onTabSelect}>
                            <Tab name='Set Parameters' id='ui'>
                                <div style={Object.assign({position: 'relative'}, tabWrapper)}>
                                    <TapSchemaBrowser serviceUrl={serviceUrl}/>
                                </div>
                            </Tab>
                            <Tab name='ADQL' id='adql'>
                                <div style={tabWrapper}>
                                    <ValidationField style={{width: 950, height: 20}}
                                                     fieldKey='adqlQuery'
                                                     tooltip='ADQL to submit to the selected TAP service'
                                                     label='ADQL Query:'
                                                     labelWidth={80}
                                    />
                                </div>
                            </Tab>
                        </FieldGroupTabs>
                    </FieldGroup>
                    
                </FormPanel>
            </div>
        );
    }

    onTapServiceOptionSelect(selectedOption) {
        if (selectedOption) {
            const selectedTapService = selectedOption.value;
            const sampleQuery = getSampleQuery(selectedTapService);
            dispatchValueChange({
                groupKey: gkey,
                fieldKey: 'adqlQuery',
                placeholder: sampleQuery,
                value: sampleQuery
            });
            this.setState({serviceUrl: selectedTapService});
        }
    }

    onTabSelect(index,id) {
        this.setState({freeForm: id === 'adql'});
    }

    populateAndEditAdql() {
        const adql = getAdqlQuery();
        if (adql) {
            //set adql and switch tab to ADQL
            dispatchMultiValueChange(gkey,
                [
                    {fieldKey: 'adqlQuery', value: adql},
                    {fieldKey: 'tabs', value: 'adql'},
                ]
            );
            this.setState({freeForm: true});
        }
    }
}





function getTapBrowserState() {
    const tapBrowserState = getComponentState(componentKey);
    const {schemaOptions, schemaName, tableOptions, tableName} = tapBrowserState || {};
    return {error: undefined, schemaOptions, schemaName, tableOptions, tableName};
}

class TapSchemaBrowser extends PureComponent {
    constructor(props) {
        super(props);

        this.state = getTapBrowserState();
        this.loadSchemas = this.loadSchemas.bind(this);
        this.loadTables = this.loadTables.bind(this);
    }

    componentDidMount() {
        const {serviceUrl, schemaOptions, schemaName, tableName} = this.state;
        if (!schemaOptions || (serviceUrl !== this.props.serviceUrl)) {
            this.loadSchemas(this.props.serviceUrl, schemaName, tableName);
        }
        this.iAmMounted = true;
    }

    componentDidUpdate(prevProps) {
        if (this.props.serviceUrl !== prevProps.serviceUrl) {
            this.loadSchemas(this.props.serviceUrl);
        }
    }

    componentWillUnmount() {
        this.iAmMounted = false;
        const {schemaOptions, schemaName, tableOptions, tableName} = this.state;
        dispatchComponentStateChange(componentKey,
            {serviceUrl: this.props.serviceUrl, schemaOptions, schemaName, tableOptions, tableName});
    }
    
    render() {
        const {serviceUrl} = this.props;
        const {error, schemaOptions, tableOptions, schemaName, tableName, columnsModel}= this.state;

        if (error) {
            return (<div>{error}</div>);
        }
        
        let columnsUrl = undefined;
        if (tableOptions && tableOptions.length > 0) {
            columnsUrl = serviceUrl + qFragment +
                'QUERY=SELECT+column_name,description,unit,datatype,ucd,utype,principal+' +
                'FROM+TAP_SCHEMA.columns+WHERE+table_name+like+\'' + tableName + '\'';
        }
        // need to set initialState on list fields so that the initial value that is not the first index
        // is set correctly after unmount and mount
        return (
            <div>
                <div className={'taptablepanel'}>
                    <div className='tableselectors'>
                        {schemaOptions &&
                        <ListBoxInputField key='schemaList'
                                           fieldKey='schemaName'
                                           options={schemaOptions}
                                           value={schemaName}
                                           initialState={{value: schemaName}}
                                           onChange={(ev) => {
                                               const selectedTapSchema = ev.target.value;
                                               this.loadTables(serviceUrl, selectedTapSchema);
                                           }}
                                           multiple={false}
                                           tooltip='TAP Schema'
                                           label='TAP Schema:'
                                           labelWidth={80}
                                           wrapperStyle={{paddingBottom: 2}}
                        />}
                        {tableOptions &&
                        <ListBoxInputField  key='tableList'
                                            fieldKey='tableName'
                                            options={tableOptions}
                                            value={tableName}
                                            initialState={{value: tableName}}
                                            onChange={(ev) => {
                                                const selectedTapTable = ev.target.value;
                                                const columnsTblId = getTblId(selectedTapTable);
                                                onTableLoaded(getTblId(selectedTapTable)).then( () => {
                                                     this.setState({columnsModel: getTblById(columnsTblId)});
                                                });
                                                this.setState({tableOptions, tableName: selectedTapTable, columnsModel: undefined});
                                            }}
                                            multiple={false}
                                            tooltip='TAP Table'
                                            label='TAP Table:'
                                            labelWidth={80}
                                            wrapperStyle={{paddingBottom: 2}}
                        />}
                    </div>
                    <div className='searchmethods'>
                        Spatial/Time/Wavelength constraints
                        {columnsModel &&
                        <div style={{paddingTop: 20}}>
                            <TableSearchMethods columnsModel={columnsModel}/>
                        </div>}
                    </div>
                </div>
                <div className='columnconstraints'>
                    {!tableName ? false :
                            <CatalogConstraintsPanel fieldKey={'tableconstraints'}
                                                     catname={tableName}
                                                     showFormType={false}
                                                     processId={'IpacTableFromSource'}
                                                     groupKey={gkey}
                                                     createDDRequest={()=>{
                                                         return {id: 'IpacTableFromSource', source: columnsUrl};
                                                     }}
                            />
                    }
                </div>
            </div>
        );
    }

    loadSchemas(serviceUrl, schemaName=undefined, tableName=undefined) {
        this.setState({error: undefined, schemaOptions: undefined, schemaName: undefined, tableOptions: undefined, tableName: undefined});

        const url = serviceUrl + qFragment + 'QUERY=SELECT+*+FROM+TAP_SCHEMA.schemas';
        const request = makeFileRequest('schemas', url, null,
            {tbl_id: 'schemas', META_INFO: {}});

        doFetchTable(request).then((tableModel) => {
            if (tableModel.error) {
                logError(`Failed to get schemas for ${serviceUrl}`, `${tableModel.error}`);
            } else {
                if (tableModel.tableData) {
                    // check if schema_index column is present
                    // if it is, sort tabledata by schema_index
                    if (getColumnIdx(tableModel, 'schema_index') >= 0) {
                        sortTableData(tableModel.tableData.data, tableModel.tableData.columns, sortInfoString('schema_index'));
                    }
                    const schemas = getColumnValues(tableModel, 'schema_name');
                    const schemaDescriptions = getColumnValues(tableModel, 'description');

                    if (schemas.length > 0) {
                        if (!schemaName || !schemas.includes(schemaName)) { schemaName = schemas[0]; }
                        this.loadTables(serviceUrl, schemaName, tableName);
                    }

                    const schemaOptions = schemas.map((e, i) => {
                        let label = schemaDescriptions[i] ? `[${e}] ${schemaDescriptions[i]}` : `[${e}]`;
                        label = truncate(label, {length: 50});
                        return {label, value: e};
                    });
                    this.setState({schemaOptions, schemaName});
                    return;
                }
            }
            this.setState({error: 'No schemas available'});
        }).catch(
            (reason) => {
                this.setState({error: `Failed to get schemas for ${serviceUrl}: ${reason}`});
            }
        );
    }

    loadTables(serviceUrl, schemaName, tableName) {
        this.setState({schemaName, tableOptions: undefined, tableName: undefined});

        const url = serviceUrl + qFragment + 'QUERY=SELECT+*+FROM+TAP_SCHEMA.tables+WHERE+schema_name+like+\'' + schemaName + '\'';
        const request = makeFileRequest('tables', url, null,
            {tbl_id: 'tables', META_INFO: {}});

        doFetchTable(request).then((tableModel) => {
            if (tableModel.error) {
                logError(`Failed to get tables for ${serviceUrl} schema ${schemaName}`, `${tableModel.error}`);
            } else {
                if (tableModel.tableData) {
                    // check if schema_index column is present
                    // if it is, sort tabledata by schema_index
                    if (getColumnIdx(tableModel, 'table_index') >= 0) {
                        sortTableData(tableModel.tableData.data, tableModel.tableData.columns, sortInfoString('table_index'));
                    }
                    const tables = getColumnValues(tableModel, 'table_name');
                    const tableDescriptions = getColumnValues(tableModel, 'description');

                    if (tables.length > 0) {
                        if (!tableName || !tables.includes(tableName)) { tableName = tables[0]; }
                    }

                    const tableOptions = tables.map((e, i) => {
                        let label = tableDescriptions[i] ? `[${e}] ${tableDescriptions[i]}` : `[${e}]`;
                        label = truncate(label, {length: 50});
                        return {label, value: e};
                    });
                    const columnsTblId = getTblId(tableName);
                    onTableLoaded(columnsTblId).then( () => {
                         this.setState({columnsModel: getTblById(columnsTblId)});
                    });
                    this.setState({tableOptions, tableName, columnsModel: undefined});
                    return;
                }
            }
            logError(`No tables available for ${serviceUrl} schema ${schemaName}`);
        }).catch(
            (reason) => {
                logError({error: `Failed to get tables for ${serviceUrl} schema ${schemaName}: ${reason}`});
            }
        );
    }

}



function hideSearchPanel() {
    dispatchHideDropDown();
}

function onSearchSubmit(request,serviceUrl) {
    console.log(request);
    const isADQL = (request.tabs === 'adql');
    let adql = undefined;
    let title = undefined;
    if (isADQL) {
        adql = request.adqlQuery;
        // use service name for title
        const found = serviceUrl.match(/.*:\/\/(.*)\/.*/i);
        title = found && found[1];
    } else {
        adql = getAdqlQuery();
        title = request.tableName;
    }
    if (adql) {
        const params = {serviceUrl, QUERY: adql};
        const options = {};

        const treq = makeTblRequest('AsyncTapQuery', title, params, options);
        dispatchTableSearch(treq, {backgroundable: true});
    }
}

function getAdqlQuery() {
    const fields = FieldGroupUtils.getGroupFields(gkey);
    const tableconstraints = get(fields, ['tableconstraints', 'value']);
    const tableName = get(fields, ['tableName', 'value']);

    if (!tableName) return;

    let constraints = tableSearchMethodsConstraints() || '';
    let selcols = '*';
    let addAnd = Boolean(constraints);

    if (tableconstraints) {
        if (tableconstraints.constraints.length > 0) {
            constraints += (addAnd ? ' AND ' : '') + tableconstraints.constraints;
            addAnd = true;
        }
        const colsSearched = tableconstraints.selcols.lastIndexOf(',') > 0 ? tableconstraints.selcols.substring(0, tableconstraints.selcols.lastIndexOf(',')) : tableconstraints.selcols;
        if (colsSearched.length > 0) {
            selcols = colsSearched;
        }
    }

    const sqlTxt = get(FieldGroupUtils.getGroupFields(gkey), ['txtareasql', 'value'], '').trim();
    if (sqlTxt && sqlTxt.length > 0) {
        constraints += (addAnd ? ' AND ' : '') + validateSql(sqlTxt);
    }
    if (constraints) {
        constraints = `WHERE ${constraints}`;
    }


    return `SELECT TOP 10000 ${selcols} FROM ${tableName} ${constraints}`;
}

const TAP_SERVICES = [
    {
        label: 'IRSA https://irsa.ipac.caltech.edu/TAP',
        value: 'https://irsa.ipac.caltech.edu/TAP',
        query: 'SELECT * FROM fp_psc WHERE CONTAINS(POINT(\'ICRS\',ra,dec),CIRCLE(\'ICRS\',210.80225,54.34894,1.0))=1'
    },
    {
        label: 'GAIA http://gea.esac.esa.int/tap-server/tap',
        value: 'http://gea.esac.esa.int/tap-server/tap',
        query: 'SELECT TOP 5000 * FROM gaiadr2.gaia_source'
    },
    {
        label: 'CASDA http://atoavo.atnf.csiro.au/tap',
        value: 'http://atoavo.atnf.csiro.au/tap',
        query: 'SELECT * FROM ivoa.obscore WHERE CONTAINS(POINT(\'ICRS\',s_ra,s_dec),CIRCLE(\'ICRS\',32.69,-51.01,1.0))=1'
    },
    {
        label: 'MAST http://vao.stsci.edu/CAOMTAP/TapService.aspx',
        value: 'http://vao.stsci.edu/CAOMTAP/TapService.aspx',
        query: 'SELECT * FROM ivoa.obscore WHERE CONTAINS(POINT(\'ICRS\',s_ra,s_dec),CIRCLE(\'ICRS\',32.69,-51.01,1.0))=1'
    },
    {
        label: 'LSST TEST http://tap.lsst.rocks/tap',
        value: 'http://tap.lsst.rocks/tap',
        query: 'SELECT TOP 5000 * FROM gaiadr2.gaia_source'
    }
];

const TAP_SERVICE_OPTIONS = TAP_SERVICES.map((e)=>pick(e, ['label', 'value']));


function getSampleQuery(serviceUrl) {
    const idx = TAP_SERVICES.findIndex((e) => e.value === serviceUrl);
    let sampleQuery = '';
    if (idx >= 0) {
        sampleQuery = TAP_SERVICES[idx].query;
    }
    return sampleQuery;
}




