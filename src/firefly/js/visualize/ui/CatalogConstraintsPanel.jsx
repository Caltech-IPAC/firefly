/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty, get, merge} from 'lodash';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {fieldGroupConnector} from '../../ui/FieldGroupConnector.jsx';
import {doFetchTable, getColumnIdx, getTblById} from '../../tables/TableUtil.js';
import {TablePanel} from '../../tables/ui/TablePanel.jsx';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {InputAreaFieldConnected} from '../../ui/InputAreaField.jsx';
import {createLinkCell, createInputCell} from '../../tables/ui/TableRenderer.js';
import * as TblCntlr from '../../tables/TablesCntlr.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {FilterInfo, FILTER_TTIPS} from '../../tables/FilterInfo.js';
import {isNil, isArray} from 'lodash';

const sqlConstraintsCol = {name: 'constraints', idx: 1, type: 'char', width: 10};


/**
 * regenerate short_dd to be one of ['short', 'long', '']
 */
function makeFormType(showForm, short_dd) {
    var ddShort = !showForm ? '' : (isNil(short_dd) ? 'short' : (short_dd === 'true' ? 'short' : 'long'));

    return ddShort;
}


export class CatalogConstraintsPanel extends React.Component {

    /**
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {};
        this.fetchDD = this.fetchDD.bind(this);
    }

    //shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    componentDidMount() {
        const {catname, processId, dd_short, showFormType = true} = this.props;
        this.fetchDD(catname, makeFormType(showFormType, dd_short), processId,  'true'); //short form as default
    }

    componentWillReceiveProps(np) {
        var ddShort = makeFormType(np.showFormType, np.dd_short);

        if (np.processId !== this.props.processId) {
            this.fetchDD(np.catnam, ddShort, np.processId, true);
        } else if (np.catname !== this.props.catname || np.dd_short !== this.props.dd_short) {
            this.fetchDD(np.catname, ddShort, np.processId);   //TODO: should add 'true'?
        } else if (this.state.tableModel) {
            var tblid = np.tbl_id ? np.tbl_id : `${np.catname}-${ddShort}-dd-table-constraint`;
            if (tblid !== this.state.tableModel.tbl_id) {
                const tableModel = getTblById(tblid);
                this.setState({tableModel});
            }
        }
    }

    render() {
        const {tableModel} = this.state;
        const {catname, dd_short, fieldKey, showFormType=true, processId} = this.props;

        var resetButton = () => {
            var ddShort = makeFormType(showFormType, dd_short);

            return (
                <button style={{padding: '0 5px 0 5px', margin: showFormType ? '0 10px 0 10px' : '0'}}
                        onClick={ () => this.fetchDD(catname,  ddShort, processId, true)}>Reset
                </button>
            );
        };
        var formTypeList = () => {
                return (
                   <ListBoxInputField fieldKey={'ddform'} inline={true} labelWidth={0}
                       initialState={{
                            tooltip: 'Select form',
                            value: 'false'
                       }}
                       options={[
                            {label: 'Standard', value: 'true'},
                            {label: 'Long form', value: 'false'}
                       ]}
                       labelWidth={75}
                       label='Table Selection:'
                       multiple={false}
                   />
                );
        };

        if (isEmpty(tableModel) || !tableModel.tbl_id.startsWith(catname)) {
            return <div style={{top: 0}} className='loading-mask'/>;
        }

        return (
            <div style={{padding:'0 0 5px'}}>
                <div
                    style={{display:'flex', flexDirection:'column',
                            margin:'0px 10px 5px 5px', padding:'0 0 0 10px',
                            border:'1px solid #a3aeb9'}}>
                    <div style={{display:'flex', flexDirection:'row', padding:'5px 5px 0'}}>
                        {showFormType && formTypeList()}
                        {resetButton()}
                    </div>
                    <div>
                        <TablePanelConnected {...{tableModel, fieldKey}} />
                        {renderSqlArea()}
                    </div>
                </div>
            </div>
        );
    }

    /**
     * Getting dd info from catalog name catName
     * @param catName string name of the catalog for searching DD information
     * @param dd_short
     * @param clearSelections
     */
    fetchDD(catName, dd_short, processId, clearSelections = false) {

        var tblid = `${catName}-${dd_short}-dd-table-constraint`;

        //// Check if it exists already - fieldgroup has a keepState property but
        //// here we are not using the table as a fieldgroup per se so we need to cache the column restrictions changes
        const tbl = getTblById(tblid);
        if (tbl && !clearSelections) {
            this.setState({tableModel: tbl});
            return;
        }

        const request = {id: processId, 'catalog': catName, short: dd_short}; //Fetch DD master table
        const urldef = get(FieldGroupUtils.getGroupFields(this.props.groupKey), 'cattable.coldef', 'null');

        doFetchTable(request).then((tableModel) => {
            const tableModelFetched = tableModel;
            tableModelFetched.tbl_id = tblid;
            addConstraintColumn(tableModelFetched);
            addColumnDef(tableModelFetched, urldef);
            //hideColumns(tableModelFetched);
            setRowsChecked(tableModelFetched);
            tableModelFetched.tableData.columns[getColumnIdx(tableModel, 'description')].width = 70;
            tableModelFetched.tableData.columns[getColumnIdx(tableModel, 'name')].width = 10;
            //if (clearSelections) {
            //    TblCntlr.dispatchTableReplace(tableModel);
            //}
            TblCntlr.dispatchTableReplace(tableModel);
            this.setState({tableModel: tableModelFetched});
        }).catch((reason) => {
                console.error(reason);
            }
        );
    }
}

/**
 * Set true checkboxes column wherever a row should be selected given by column named 'sel'
 * @param anyTableModel
 */
function setRowsChecked(anyTableModel) {
    const selectInfoCls = SelectInfo.newInstance({rowCount: anyTableModel.totalRows});
    const idxColSel = getColumnIdx(anyTableModel, 'sel');
    anyTableModel.tableData.data.forEach((arow, index) => {
        if (arow[idxColSel] === 'y') {
            selectInfoCls.setRowSelect(index, true);
        }
    });
    const selectInfo = selectInfoCls.data;
    merge(anyTableModel, {selectInfo});
    //TblCntlr.dispatchTableSelect(anyTableModel.tbl_id, selectInfo);
}

/**
 * Add to table columns an extra colDef
 * original columns from table DD are: name,constraints,description,units,indx,dbtype,tableflg,sel
 * @param tableModelFetched
 * @param urldef
 */
function addColumnDef(tableModelFetched, urldef) {
    const nCols = tableModelFetched.tableData.columns.length;
    const u = (isEmpty(urldef) || urldef === 'null') ? '#' : urldef.match(/href='([^']+)'/)[1] + '#';
    tableModelFetched.tableData.columns.splice(nCols, 0, {visibility: 'hide', name: 'coldef', type: 'char'});
    tableModelFetched.tableData.data.map((e) => {
        e.splice(nCols, 0, u + e[0]);
    });
}

/**
 *
 * @param tableModelFetched
 */
function addConstraintColumn(tableModelFetched) {
    const idxSqlCol = sqlConstraintsCol.idx; // after 'name' column
    //tableModelFetched.tableData.columns[idxSqlCol].prefWidth=5;
    tableModelFetched.tableData.columns.splice(idxSqlCol, 0, {...sqlConstraintsCol});
    tableModelFetched.tableData.data.map((e) => {
        e.splice(idxSqlCol, 0, '');
    });
}

/**
 * Hide the colunms after index = 3
 * @param tableModelFetched
 */
function hideColumns(tableModelFetched) {
    tableModelFetched.tableData.columns.map(
        (e, idx) => {
            if (idx > 3) {
                e.visibility = 'hide';
            }
        });

}

/**
 *
 * @type {{onChange: (func|any), catname: (isRequired|any), constraintskey: (string|any), tbl_id: (string|any), fieldKey: (string|any), groupKey: (string|any)}}
 */
CatalogConstraintsPanel.propTypes = {
    ...TablePanel.propTypes,
    onChange: PropTypes.func,
    onBlur: PropTypes.func,
    catname: PropTypes.string.isRequired,
    dd_short: PropTypes.string,
    constraintskey: PropTypes.string,
    fieldKey: PropTypes.string,
    groupKey: PropTypes.string,
    showFormType: PropTypes.bool,
    processId: PropTypes.string
};

CatalogConstraintsPanel.defaultProps = {
    catname: '',
    processId: 'GatorDD',
    showFormType: true
};

/**
 * display the data restrictions into a tabular format
 * @param {func} ontTableChanged
 * @param {Object} tableModel
 * @returns {Object} constraint table
 */
function ConstraintPanel({tableModel, onTableChanged}) {
    //define the table style only in the table div
    const tableStyle = {
        boxSizing: 'border-box',
        paddingLeft: 5,
        paddingRight: 5,
        width: '100%',
        height: 'calc(100% - 20px)', //below the table, leave space (20px) to add the text box for SQL input
        overflow: 'hidden',
        flexGrow: 1,
        display: 'flex',
        resize: 'none'
    };
    const popupPanelResizableStyle = {
        width: '70%',
        minWidth: 600,
        height: 200,
        minHeight: 100,
        maxHeight: 450,
        resize: 'both',
        overflow: 'hidden',
        position: 'relative'
    };
    const tbl_ui_id = tableModel.tbl_id + '-ui';
    return (

        <div style={{display:'inline-block',
                     width: '97%', height: '170px', padding: '5px 5px'}}>
            <TablePanel
                onTableChanged={onTableChanged}
                //onBlur={ (e) => {console.log('onChange called from table '+e.value);}}
                showToolbar={false}
                showMask={true}
                showOptionButton={false}
                key={tableModel.tbl_id}
                tbl_ui_id = {tbl_ui_id}
                tableModel={tableModel}
                renderers={
                    {
                        name:
                            { cellRenderer:
                                createLinkCell(
                                    {
                                        hrefColIdx: tableModel.tableData.columns.length-1
                                    }
                                )
                            },
                        constraints:
                            { cellRenderer:
                                createInputCell(
                                         FILTER_TTIPS,
                                         15,
                                         FilterInfo.conditionValidatorNoAutoCorrect,
                                         //null,
                                         onTableChanged
                                )
                            }
                    }
                }
            />
        </div>
    );
}

export const TablePanelConnected = fieldGroupConnector(ConstraintPanel, getProps, null, null);

function getProps(params, fireValueChange) {

    return Object.assign({}, params,
        {
            onTableChanged: () => handleOnTableChanged(params, fireValueChange)
        });
}

function handleOnTableChanged(params, fireValueChange) {
    //var {sql, selCol} = functionGetNewVal(params.tablemodel);
    //sql != params.sql;
    const {tbl_id} = params.tableModel;
    const tbl = getTblById(tbl_id);
    let tbl_data = {};
    let sel_info = {};
    if (tbl) {
        tbl_data = tbl.tableData.data;
        sel_info = tbl.selectInfo;
    } else {
        return;
    }

    let sqlTxt = '';
    let errors = '';

    tbl_data.forEach((d) => {
        var filterStrings, valid = true;
        const colName = d[0];

        filterStrings = d[1].trim();

        if (filterStrings && filterStrings.length > 0) {
            const parts = filterStrings && filterStrings.split(';');

            parts.forEach((v, idx) => {
                // const parts = v.trim().match(extract_regex);
                var {valid, value} = FilterInfo.conditionValidatorNoAutoCorrect(v);  // TODO: need more detail syntax check

                if (!valid) {
                    errors += (errors.length > 0 ? ` ${value}` : value);
                } else {
                    var oneSql = `${colName} ${v}`;
                    sqlTxt += (sqlTxt.length > 0 ? ` AND ${oneSql}` : oneSql);
                }
            });
        }
    });

    let selCols = '';
    if (sel_info.exceptions.size > 0) {
        //for now, add ra, dec as list of string array
        for (const colIdxSelected of sel_info.exceptions.keys()) {
            selCols += tbl_data[colIdxSelected][0] + ',';
        }
    }

    // the value of this input field is a string
    const val = get(params, 'value', '');
    // ++++++++++++++++++++++++++++
    // ++++++ WARNING!!!!!+++++++++
    // ++++++++++++++++++++++++++++
    // YOU ONLY CARE ABOUT CHANGES ON SELECTED COLUMNS AND CONSTRAINTS  INPUT FILED
    //
    // CHECK IF FIREVALUE IS REQUIRED HERE EVERYTIME IF PREVIOUS VALUE HAS CHANGED BECAUSE THIS WILL GET CALLED TWICE (TABLE AND FIELDGROUP) AND
    // CAN BECOME AN ENDLESS LOOP IF IT FIRES AGAIN WITHOUT CHECKING
    // BASICALLY IMPLEMENTING HERE THE 'ONCHANGE' OF THE TABLEVIEW USED IN A FORM

    if (val.constraints !== sqlTxt || val.selcols !== selCols || errors !== val.errorConstraints) {
        fireValueChange({
            value: {constraints: sqlTxt, selcols: selCols, errorConstraints: errors}
        });
    }
}

const extract_regex = new RegExp('(<|>|>=|<=|=|!=|like|in)(.+)', 'i');

const inputFieldValidator = (filterString) => {
    let retval = {valid: true, message: ''};
    if (filterString) {
        filterString && filterString.split(';').forEach((v) => {
            const parts = v.trim().match(extract_regex) || [];
            if (parts.length === 0) {
                retval = {valid: false, message: `${v} not valid: ${FILTER_TTIPS}`};
                return retval;
            }
            if (parts[0] !== v.trim()) {
                retval = {valid: false, message: `${v} not valid: ${FILTER_TTIPS}`};
                return retval;
            }
            if (!new RegExp('(<|>|>=|<=|=|!=)([-.0-9]+)').test(parts[2])) {
                retval = {valid: false, message: `${v} not valid: ${FILTER_TTIPS}`};
                return retval;
            }
        });
    }
    return retval;
};

/**
 *
 * @returns {XML}
 */
function renderSqlArea() {
    //m31, cone search 10', w3snr>7 and (w2mpro-w3mpro)>1.5 on wise source catalog = 361
    return (
        <div style={{margin: '2px 0'}}>
            <InputAreaFieldConnected fieldKey='txtareasql'
                                     wrapperStyle={{padding: 5}}
                                     style={{
                                                overflow: 'auto',
                                                display: 'flex',
                                                alignItems: 'center',
                                                height: '20px',
                                                maxHeight: '100px',
                                                width: '97%',
                                                maxWidth: '1000px'
                                            }}
                                     initialState={{
                                                tooltip: 'Enter SQL additional constraints here',
                                                labelWidth: 70
                                            }}
                                     label='Additional constraints (SQL):'
            />
            <em>Ex: w3snr&gt;7 and (w2mpro-w3mpro)&gt;1.5 and ra&gt;102.3 and ra&lt;112.3 and dec&lt;-5.5 and
                dec&gt;
                -15.5</em><br />
            (source_id_mf = '1861p075_ac51-002577')
            <br />
            <code style={{align: 'center', color: 'red'}}>The format for date type is yyyy-mm-dd</code>
        </div>
    );
}

const sqlValidator = (val) => {
    let retval = {valid: true, message: ''};
    if (true) {
        return {valid: false, message: 'SQL wrong. Please provide value or expression'};
    } else if (val.length > 0) {
        //const expr = new Expression(val,colNames);
        if (false) {
            retval = {valid: false, message: `${expr.getError().error}. Unable to parse ${val}.`};
        }
    }
    return retval;
};