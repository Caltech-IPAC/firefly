import React, {PureComponent} from 'react';
import {get, pick, truncate} from 'lodash';
import {logError} from '../../util/WebUtil.js';
import {FormPanel} from '../../ui/FormPanel.jsx';
import {FieldGroup} from '../../ui/FieldGroup.jsx';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils';
import {getFieldVal} from '../../fieldGroup/FieldGroupUtils';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {CatalogConstraintsPanel, getTblId} from './CatalogConstraintsPanel.jsx';
import {validateSql} from './CatalogSelectViewPanel.jsx';
import {ListBoxInputField, ListBoxInputFieldView} from '../../ui/ListBoxInputField.jsx';
import {ValidationField} from '../../ui/ValidationField.jsx';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {makeTblRequest, makeFileRequest} from '../../tables/TableRequestUtil.js';
import {doFetchTable, getColumnIdx, getColumnValues, sortTableData} from '../../tables/TableUtil.js';
import {sortInfoString} from '../../tables/SortInfo.js';
import {dispatchComponentStateChange, getComponentState} from '../../core/ComponentCntlr.js';



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
        this.state = {serviceUrl};
    }

    componentDidMount() {
        if (!getFieldVal(gkey,'adqlQuery')) {
            const sampleQuery = getSampleQuery(this.state.serviceUrl);
            dispatchValueChange({groupKey: gkey, fieldKey: 'adqlQuery', placeholder: sampleQuery, value: sampleQuery});
        }
    }

    render() {
        const {serviceUrl} = this.state;

        return (
            <div style={{padding: 10}}>
                <ListBoxInputFieldView
                    options={TAP_SERVICE_OPTIONS}
                    value={serviceUrl}
                    onChange={(ev) => {
                        const selectedTapService = ev.target.value;
                        const sampleQuery = getSampleQuery(selectedTapService);
                        dispatchValueChange({groupKey: gkey, fieldKey: 'adqlQuery', placeholder: sampleQuery, value: sampleQuery});
                        this.setState({serviceUrl: selectedTapService});
                    }}
                    multiple={false}
                    tooltip='TAP Service and URL'
                    label='TAP Service:'
                    labelWidth={80}
                    wrapperStyle={{padding: 5}}
                />
                <FormPanel
                    groupKey={gkey}
                    onSubmit={(request) => onSearchSubmit(request, serviceUrl)}
                    onCancel={hideSearchPanel}>
                    <FieldGroup groupKey={gkey} keepState={true}>

                        <ValidationField style={{width: 650, height: 20}}
                                         fieldKey='adqlQuery'
                                         tooltip='ADQL to submit to the selected TAP service'
                                         label='ADQL Query:'
                                         labelWidth={80}
                        />
                        <div style={{width: '100%', textAlign: 'right'}}>
                            <button style={{padding: 3, margin: '5px 20px'}}
                                    onClick={ () => {
                                        const adql = getAdqlQuery();
                                        if (adql) {
                                            // todo: validate adql
                                            dispatchValueChange({groupKey: gkey, fieldKey: 'adqlQuery', value: adql});
                                        }
                                    }}>Set ADQL from Constraints
                            </button>
                        </div>
                        <TapSchemaBrowser serviceUrl={serviceUrl}/>
                    </FieldGroup>
                </FormPanel>


            </div>
        );
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
        const {schemaOptions, schemaName, tableName} = this.state;
        if (!schemaOptions) {
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
        const {error, schemaOptions, tableOptions, schemaName, tableName}= this.state;

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
            <div style={{padding: 5}}>
                {schemaOptions && <ListBoxInputField key='schemaList'
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
                {tableOptions && <ListBoxInputField  key='tableList'
                    fieldKey='tableName'
                    options={tableOptions}
                    value={tableName}
                    initialState={{value: tableName}}
                    onChange={(ev) => {
                        const selectedTapTable = ev.target.value;
                        this.setState({tableName: selectedTapTable});
                    }}
                    multiple={false}
                    tooltip='TAP Table'
                    label='TAP Table:'
                    labelWidth={80}
                    wrapperStyle={{paddingBottom: 2}}
                />}
                <div style={{width: '100%, padding: 5'}}>
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
                        label = truncate(label, {length: 110});
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
                        label = truncate(label, {length: 110});
                        return {label, value: e};
                    });

                    this.setState({tableOptions, tableName});
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

    if (request.adqlQuery) {
        const params = {serviceUrl, QUERY: request.adqlQuery};
        console.log(params.QUERY);
        const options = {};
        const found = serviceUrl.match(/.*:\/\/(.*)\/.*/i);
        const treq = makeTblRequest('AsyncTapQuery', found && found[1], params, options);

        dispatchTableSearch(treq, {backgroundable: true});
    }
}

function getAdqlQuery() {
    const fields = FieldGroupUtils.getGroupFields(gkey);
    const tableconstraints = get(fields, ['tableconstraints', 'value']);
    const tableName = get(fields, ['tableName', 'value']);

    if (!tableName) return;

    let constraints = '';
    let selcols = '*';
    let addAnd = false;

    if (tableconstraints) {
        if (tableconstraints.constraints.length > 0) {
            constraints += tableconstraints.constraints;
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




