import React, {PureComponent, Fragment} from 'react';
import SplitPane from 'react-split-pane';

import {SplitContent} from '../panel/DockLayoutPanel';
import CreatableSelect from 'react-select/lib/Creatable';
import {get, pick} from 'lodash';
import {FormPanel} from '../FormPanel.jsx';
import {FieldGroup} from '../FieldGroup.jsx';
import {dispatchMultiValueChange, dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import FieldGroupUtils, {getFieldVal} from '../../fieldGroup/FieldGroupUtils';
import {dispatchHideDropDown} from '../../core/LayoutCntlr.js';
import {dispatchTableSearch} from '../../tables/TablesCntlr.js';
import {makeTblRequest, setNoCache} from '../../tables/TableRequestUtil.js';
import {getColumnValues} from '../../tables/TableUtil.js';

import {TableSearchMethods, tableSearchMethodsConstraints, SpattialPanelWidth} from './TableSearchMethods.jsx';
import {AdvancedADQL} from './AdvancedADQL.jsx';
import {loadTapColumns, loadTapTables, loadTapSchemas, getTapBrowserState, setTapBrowserState} from './TapUtil.js';
import {NameSelect, NameSelectField, selectTheme} from './Select.jsx';
import {showYesNoPopup} from '../PopupUtil.jsx';

import {dispatchHideDialog} from '../../core/ComponentCntlr';
import {TableColumnsConstraints, TableColumnsConstraintsToolbar, tableColumnsConstraints} from './TableColumnsConstraints.jsx';
import {RadioGroupInputField} from '../RadioGroupInputField';


import './TableSelectViewPanel.css';
import {HelpIcon} from '../HelpIcon';
import {showInfoPopup} from '../PopupUtil.jsx';

const MINWIDTH = 915;

/**
 * group key for fieldgroup comp
 */

const gkey = 'TAP_SEARCH_PANEL';
export const tapHelpId = (id) => `tapSearches.${id}`;

// on the left tap tables browser
// on the bottom - column constraints
export class TapSearchPanel extends PureComponent {
    constructor(props) {
        super(props);
        const {serviceUrl=TAP_SERVICE_OPTIONS[0].value, ...others} = getTapBrowserState();
        this.state = {serviceUrl, ...others};       // initialize state.. default serviceUrl if not given


        this.populateAndEditAdql = this.populateAndEditAdql.bind(this);
        this.onTapServiceOptionSelect = this.onTapServiceOptionSelect.bind(this);
    }

    componentDidMount() {

        this.removeListener = FieldGroupUtils.bindToStore( gkey, (fields) => {
            if (!this.isUnmounted && fields) {
                const vals = Object.entries(fields)
                            .reduce((org, [k,v]) => {
                                org[k] = v.value;
                                return org;
                            }, {});
                this.setState(vals);
            }
        });

        if (!getFieldVal(gkey,'adqlQuery')) {
            const sampleQuery = ''; //getSampleQuery(this.state.serviceUrl);
            dispatchValueChange({groupKey: gkey, fieldKey: 'adqlQuery', placeholder: sampleQuery, value: sampleQuery});
        }
    }

    componentWillUnmount() {
        this.removeListener && this.removeListener();
        this.isUnmounted = true;
    }

    render() {


        const {serviceUrl, selectBy='basic'} = this.state;

        const placeholder = serviceUrl ? `Using <${serviceUrl}>. Replace...` : 'Select TAP...';

        const rightBtns = selectBy === 'basic' ?[{text: 'Populate and edit ADQL', onClick: this.populateAndEditAdql, style: {marginLeft: 50}}] :  [];

        const style = {resize: 'both', overflow: 'hidden', paddingBottom: 5, height: 600, width: 915, minHeight: 650, minWidth: MINWIDTH};

        return (
            <div style={style}>
                <FormPanel  inputStyle = {{display: 'flex', flexDirection: 'column', backgroundColor: 'transparent', padding: 'none', border: 'none'}}
                            groupKey={gkey}
                            params={{hideOnInvalid: false}}
                            onSubmit={(request) => onSearchSubmit(request, serviceUrl)}
                            extraButtons={rightBtns}
                            buttonStyle={{justifyContent: 'left'}}
                            submitBarStyle={{padding: '2px 3px 3px'}}
                            help_id = {tapHelpId('form')}
                >

                <FieldGroup groupKey={gkey} keepState={true} style={{flexGrow: 1, display: 'flex'}}>

                    <div className='TapSearch'>
                        <div className='TapSearch__title'>TAP Searches</div>

                        <div className='TapSearch__section'>
                            <div className='TapSearch__section--title'>1. TAP Service <HelpIcon helpId={tapHelpId('tapService')}/> </div>
                            <div style={{flexGrow: 1, marginRight: 3}}>
                                <CreatableSelect
                                    options={TAP_SERVICE_OPTIONS}
                                    isClearable={true}
                                    onChange={this.onTapServiceOptionSelect}
                                    placeholder={placeholder}
                                    theme={selectTheme}
                                />
                            </div>
                        </div>

                        <div className='TapSearch__section'>
                            <div className='TapSearch__section--title'>2. Select Query Type  <HelpIcon helpId={tapHelpId('selectBy')}/> </div>
                            <RadioGroupInputField
                                fieldKey = 'selectBy'
                                initialState = {{
                                    defaultValue: 'basic',
                                    options: [{label: 'Single Table', value: 'basic'}, {label: 'ADQL', value: 'adql'}],
                                    tooltip: 'Please select an interface type to use'
                                }}
                                wrapperStyle={{alignSelf: 'center'}}
                            />
                        </div>
                        {selectBy === 'basic' && <BasicUI  serviceUrl={serviceUrl}/>}
                        {selectBy === 'adql' && <AdqlUI fieldKey='adqlQuery' origFieldKey='adqlQueryOriginal' groupKey={gkey} serviceUrl={serviceUrl}/>}

                    </div>

                </FieldGroup>
            </FormPanel>
            </div>
        );
    }

    /*---------- supporting member functions --------------*/

    onTapServiceOptionSelect(selectedOption) {
        if (selectedOption) {
            const selectedTapService = selectedOption.value;
            const sampleQuery = ''; //getSampleQuery(selectedTapService);
            dispatchMultiValueChange(gkey,
                [
                    {fieldKey: 'defAdqlKey', value: sampleQuery},
                    {fieldKey: 'adqlQuery', placeholder: sampleQuery, value: sampleQuery}
                ]
            );
            this.setState({serviceUrl: selectedTapService});
        }
    }

    populateAndEditAdql() {
        const adql = getAdqlQuery();
        if (adql) {
            //set adql and switch tab to ADQL
            dispatchMultiValueChange(gkey,
                [
                    {fieldKey: 'defAdqlKey', value: adql},
                    {fieldKey: 'adqlQuery', value: adql},
                    {fieldKey: 'selectBy', value: 'adql'}
                ]
            );
        }
    }
}

function AdqlUI({serviceUrl}) {

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

class BasicUI extends PureComponent {
    constructor(props) {
        super(props);

        this.state = Object.assign({error: undefined}, getTapBrowserState());
        this.loadSchemas = this.loadSchemas.bind(this);
        this.loadTables = this.loadTables.bind(this);
        this.loadColumns = this.loadColumns.bind(this);
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
        const {schemaOptions, schemaName, tableOptions, tableName, columnsModel} = this.state;
        setTapBrowserState({serviceUrl: this.props.serviceUrl, schemaOptions, schemaName, tableOptions, tableName, columnsModel});
    }

    
    render() {
        const {serviceUrl} = this.props;
        const {error, schemaOptions, tableOptions, schemaName, tableName, columnsModel}= this.state;

        const tapSearchMinWidth = MINWIDTH - 20;  // less than the size of min-width of TAP Search panel
        const splitDef = Math.min(SpattialPanelWidth+80, tapSearchMinWidth);
        const splitMax = Math.min(SpattialPanelWidth+80, tapSearchMinWidth);

        if (error) {
            return (<div>{error}</div>);
        }

        // need to set initialState on list fields so that the initial value that is not the first index
        // is set correctly after unmount and mount
        return (
            <Fragment>
                <div className='TapSearch__section'>
                    <div className='TapSearch__section--title'>3. Select Table <HelpIcon helpId={tapHelpId('selectTable')}/> </div>
                    <div style={{display: 'inline-flex', width: '100%', marginRight: 3}}>
                        <div style={{flexGrow: 1}}>
                            <NameSelect type='Schema'
                                        options={schemaOptions}
                                        value={schemaName}
                                        onSelect = {(selectedTapSchema) => {
                                            this.loadTables(serviceUrl, selectedTapSchema);
                                        }}
                            />
                        </div>
                        <div style={{width: 10}}/>
                        <div style={{flexGrow: 1}}>
                            <NameSelectField
                                fieldKey='tableName'
                                type='Table'
                                options={tableOptions}
                                value={tableName}
                                onSelect = {(selectedTapTable) => {
                                    this.loadColumns(serviceUrl, schemaName, selectedTapTable);
                                }}
                            />
                        </div>
                    </div>
                </div>

                <div className='TapSearch__section' style={{flexDirection: 'column', flexGrow: 1}}>
                    <div style={{ display: 'inline-flex', width: 'calc(100% - 3px)', justifyContent: 'space-between'}}>
                        <div className='TapSearch__section--title'>4. Select Constraints <HelpIcon helpId={tapHelpId('constraints')}/> </div>
                        <TableColumnsConstraintsToolbar key={tableName}
                                                        tableName={tableName}
                                                        columnsModel={columnsModel}
                        />
                    </div>
                    <div className='expandable'>
                        <SplitPane split='vertical' maxSize={splitMax} mixSize={20} defaultSize={splitDef}>
                            <SplitContent>
                                {columnsModel ?  <TableSearchMethods columnsModel={columnsModel}/>
                                    : <div className='loading-mask'/>
                                }
                            </SplitContent>
                            <SplitContent>
                                { columnsModel ?
                                    <TableColumnsConstraints
                                        key={tableName}
                                        fieldKey={'tableconstraints'}
                                        columnsModel={columnsModel}
                                    />
                                    : <div className='loading-mask'/>

                                }
                            </SplitContent>
                        </SplitPane>
                    </div>
                </div>

            </Fragment>
        );
    }

    loadSchemas(serviceUrl, schemaName=undefined, tableName=undefined) {
        this.setState({error: undefined, schemaOptions: undefined, schemaName: undefined,
            tableOptions: undefined, tableName: undefined, columnsModel: undefined});
        dispatchValueChange({groupKey: gkey, fieldKey: 'tableName', value: undefined});

        loadTapSchemas(serviceUrl).then((tableModel) => {
            if (this.props.serviceUrl !== serviceUrl || !this.iAmMounted) {
                // no action if another TAP service is now used
                return;
            }
            if (tableModel.error) {
                this.setState({error: tableModel.error});
            } else  {
                const schemas = getColumnValues(tableModel, 'schema_name');
                const schemaDescriptions = getColumnValues(tableModel, 'description');

                if (schemas.length > 0) {
                    if (!schemaName || !schemas.includes(schemaName)) { schemaName = schemas[0]; }
                    this.loadTables(serviceUrl, schemaName, tableName);
                } else {
                    schemaName = undefined;
                }

                const schemaOptions = schemas.map((e, i) => {
                    const label = schemaDescriptions[i] ? schemaDescriptions[i] : `[${e}]`;
                    return {label, value: e};
                });

                this.setState({schemaOptions, schemaName});
            }
        });
    }

    loadTables(serviceUrl, schemaName, tableName) {
        this.setState({schemaName, tableOptions: undefined, tableName: undefined, columnsModel: undefined});
        dispatchValueChange({groupKey: gkey, fieldKey: 'tableName', value: undefined});

        loadTapTables(serviceUrl, schemaName).then((tableModel) => {
            if (this.props.serviceUrl !== serviceUrl || this.state.schemaName !== schemaName || !this.iAmMounted) {
                // no action if another TAP service or schema are now used
                return;
            }
            if (!tableModel.error) {
                const tables = getColumnValues(tableModel, 'table_name');
                const tableDescriptions = getColumnValues(tableModel, 'description');

                if (tables.length > 0) {
                    if (!tableName || !tables.includes(tableName)) { tableName = tables[0]; }
                    this.loadColumns(serviceUrl, schemaName, tableName);
                } else {
                    tableName = undefined;
                }

                const tableOptions = tables.map((e, i) => {
                    const label = tableDescriptions[i] ? tableDescriptions[i] : `[${e}]`;
                    return {label, value: e};
                });

                this.setState({serviceUrl, tableOptions, tableName, columnsModel: undefined});
                dispatchValueChange({groupKey: gkey, fieldKey: 'tableName', value: tableName});
            }
        });
    }

    loadColumns(serviceUrl, schemaName, tableName) {
        this.setState({tableName, columnsModel: undefined});

        loadTapColumns(serviceUrl, schemaName, tableName).then((columnsModel) => {

            if (this.props.serviceUrl !== serviceUrl || this.state.schemaName !== schemaName ||
                this.state.tableName !== tableName || !this.iAmMounted) {
                // no action if another TAP service or schema or table are now used
                return;
            }

            setTapBrowserState({serviceUrl, schemaOptions: this.state.schemaOptions, schemaName,
                tableOptions: this.state.tableOptions, tableName, columnsModel});
            this.setState({columnsModel});
        });
    }

}


function hideSearchPanel() {
    dispatchHideDropDown();
}

function onSearchSubmit(request,serviceUrl) {
    const isADQL = (request.selectBy === 'adql');
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
        adql = adql.replace(/\s/g, ' ');    // replace all whitespaces with spaces
        const doSubmit = () => {
            const params = {serviceUrl, QUERY: adql};
            const options = {};

            const treq = makeTblRequest('AsyncTapQuery', title, params, options);
            setNoCache(treq);
            dispatchTableSearch(treq, {backgroundable: true});

        };
        if (!adql.toUpperCase().match(/ TOP | WHERE /)) {
            const msg = (
                <div style={{width: 260}}>
                    You are about to submit a query without a TOP or WHERE constraint. <br/>
                    This may results in a HUGE amount of data. <br/><br/>
                    Are you sure you want to continue?
                </div>
            );
            showYesNoPopup(msg,(id, yes) => {
                if (yes) {
                    doSubmit();
                    hideSearchPanel();
                }
                dispatchHideDialog(id);
            });
        } else {
            doSubmit();
            return true;
        }
    }
    return false;
}

/**
 * @typedef {Object} AdqlFragment
 * @property {string} where - ADQL where fragment
 * @property {string} selcols - ADQL select columns fragment
 * @property {boolean} valid - true if the ADQL is valid
 * @property {string} message - error message, if invalid
 */

function getAdqlQuery() {
    const fields = FieldGroupUtils.getGroupFields(gkey);
    const tableName = get(fields, ['tableName', 'value']);

    if (!tableName) return;

    const {columnsModel} = getTapBrowserState();

    // spatial and temporal constraints
    const whereFragment = tableSearchMethodsConstraints(columnsModel);
    if (!whereFragment.valid) {
        return null;
    }
    let constraints = whereFragment.where || '';
    const addAnd = Boolean(constraints);

    // table column constraints and column selections
    const adqlFragment = tableColumnsConstraints(columnsModel);
    if (!adqlFragment.valid) {
        showInfoPopup(adqlFragment.message, 'Error');
        return null;
    }
    if (adqlFragment.where) {
        constraints += (addAnd ? ' AND ' : '') + addParens(adqlFragment.where);
    }
    const selcols = adqlFragment.selcols || '*';

    if (constraints) {
        constraints = `WHERE ${constraints}`;
    }

    return `SELECT TOP 10000 ${selcols} FROM ${tableName} ${constraints}`;
}

/**
 * Some TAP services require conditions to be surrounded by parens
 * @param condition
 */
function addParens(condition) {
    return '(' + condition + ')';
}

const TAP_SERVICES = [
    {
        label: 'IRSA https://irsa.ipac.caltech.edu/TAP',
        value: 'https://irsa.ipac.caltech.edu/TAP',
        query: 'SELECT * FROM fp_psc WHERE CONTAINS(POINT(\'ICRS\',ra,dec),CIRCLE(\'ICRS\',210.80225,54.34894,1.0))=1'
    },
    {
        label: 'NED https://ned.ipac.caltech.edu/tap',
        value: 'https://ned.ipac.caltech.edu/tap/',
        query: 'SELECT * FROM public.ned_objdir WHERE CONTAINS(POINT(\'ICRS\',ra,dec),CIRCLE(\'ICRS\',210.80225,54.34894,0.01))=1'
    },
    {
        label: 'CADC https://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/tap',
        value: 'https://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/tap',
        query: 'SELECT TOP 10000 * FROM ivoa.ObsCore WHERE CONTAINS(POINT(\'ICRS\', s_ra, s_dec),CIRCLE(\'ICRS\', 10.68479, 41.26906, 0.028))=1'
    },
    {
        label: 'GAIA https://gea.esac.esa.int/tap-server/tap',
        value: 'https://gea.esac.esa.int/tap-server/tap',
        query: 'SELECT TOP 5000 * FROM gaiadr2.gaia_source'
    },
    {
        label: 'MAST https://vao.stsci.edu/CAOMTAP/TapService.aspx',
        value: 'https://vao.stsci.edu/CAOMTAP/TapService.aspx',
        query: 'SELECT * FROM ivoa.obscore WHERE CONTAINS(POINT(\'ICRS\',s_ra,s_dec),CIRCLE(\'ICRS\',32.69,-51.01,1.0))=1'
    },
    {
        label: 'CASDA http://atoavo.atnf.csiro.au/tap',
        value: 'http://atoavo.atnf.csiro.au/tap',
        query: 'SELECT * FROM ivoa.obscore WHERE CONTAINS(POINT(\'ICRS\',s_ra,s_dec),CIRCLE(\'ICRS\',32.69,-51.01,1.0))=1'
    },
    {
        label: 'LSST lsp-stable https://lsst-lsp-stable.ncsa.illinois.edu/api/tap',
        value: 'https://lsst-lsp-stable.ncsa.illinois.edu/api/tap',
        query: 'SELECT * FROM wise_00.allwise_p3as_psd '+
            'WHERE CONTAINS(POINT(\'ICRS\', ra, decl),'+
            'POLYGON(\'ICRS\', 9.4999, -1.18268, 9.4361, -1.18269, 9.4361, -1.11891, 9.4999, -1.1189))=1'
    },
    {
        label: 'LSST lsp-int https://lsst-lsp-int.ncsa.illinois.edu/api/tap',
        value: 'https://lsst-lsp-int.ncsa.illinois.edu/api/tap',
        query: 'SELECT * FROM wise_00.allwise_p3as_psd '+
            'WHERE CONTAINS(POINT(\'ICRS\', ra, decl),'+
            'POLYGON(\'ICRS\', 9.4999, -1.18268, 9.4361, -1.18269, 9.4361, -1.11891, 9.4999, -1.1189))=1'
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




