/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {Component, PropTypes} from 'react';
import sCompare from 'react-addons-shallow-compare';
import {isEmpty, get, merge, isNil, isArray, cloneDeep, set, has} from 'lodash';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {fetchTable} from '../../rpc/SearchServicesJson.js';
import {getColumnIdx, getTblById} from '../../tables/TableUtil.js';
import {BasicTableView} from '../../tables/ui/BasicTableView.jsx';
import {createLinkCell, createInputCell} from '../../tables/ui/TableRenderer.js';
import * as TblCntlr from '../../tables/TablesCntlr.js';
import * as TblUtil from '../../tables/TableUtil.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {FilterInfo, FILTER_TTIPS} from '../../tables/FilterInfo.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {InputAreaFieldConnected} from '../../ui/InputAreaField.jsx';
import {fieldGroupConnector} from '../../ui/FieldGroupConnector.jsx';
import {LSSTDDPID} from './LSSTCatalogSelectViewPanel.jsx';
const sqlConstraintsCol = {name: 'constraints', idx: 1, type: 'char', width: 10};

import '../../tables/ui/TablePanel.css';

/**
 * update short_dd to be one of ['short', 'long', ''] based on if 'showForm' is true or not
 */
function makeFormType(showForm, short_dd) {
    return  !showForm ? '' : (isNil(short_dd) ? 'short' : (short_dd === 'true' ? 'short' : 'long'));
}

function getTblId(catName, dd_short) {
    return `${catName}${dd_short ? '-' : ''}${dd_short}-dd-table-constraint`;
}

/**
 * @summary reest table constraints state
 * @param {string} gkey
 * @param {string} fieldKey
 */
function resetConstraints(gkey, fieldKey) {
    const value = {constraints: '', selcols: '', filters: {}, errorConstraints:''};
    const modPayload= Object.assign({}, {value},  {fieldKey, groupKey: gkey});
    dispatchValueChange(modPayload);
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
        this.resetTable = this.resetTable.bind(this);
    }

    //shouldComponentUpdate(np,ns) { return sCompare(this,np,ns); }

    shouldComponentUpdate(np, ns) {
        return sCompare(this, np, ns);
    }

    componentDidMount() {
        const {catname, createDDRequest, dd_short, showFormType = true} = this.props;
        this.fetchDD(catname, makeFormType(showFormType, dd_short), createDDRequest, true); //short form as default
    }

    componentWillReceiveProps(np) {
        var ddShort = makeFormType(np.showFormType, np.dd_short);

        if (np.processId !== this.props.processId) {
            this.fetchDD(np.catname, ddShort, np.createDDRequest, true);
        } else if (np.catname !== this.props.catname || np.dd_short !== this.props.dd_short) {
            this.fetchDD(np.catname, ddShort, np.createDDRequest);
        } else if (this.state.tableModel) {
            var tblid = np.tbl_id ? np.tbl_id : getTblId(np.catname, ddShort);
            if (tblid !== this.state.tableModel.tbl_id) {
                const tableModel = getTblById(tblid);
                this.setState({tableModel});
            }
        }
    }

    render() {
        const {tableModel} = this.state;
        const {error} = tableModel || {};
        const {catname, dd_short, fieldKey, showFormType=true, createDDRequest, groupKey} = this.props;

        var resetButton = () => {
            var ddShort = makeFormType(showFormType, dd_short);

            return (
                <button style={{padding: '0 5px 0 5px', margin: showFormType ? '0 10px 0 10px' : '0'}}
                        onClick={ () => this.resetTable(catname,  ddShort, createDDRequest, groupKey, fieldKey)}>Reset
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
            return <div style={{top: 0}} className='loading-mask'></div>;
        }

        return (
            <div style={{padding:'0 0 5px'}}>
                <div
                    style={{display:'flex', flexDirection:'column',
                            margin:'0px 10px 5px 5px', padding:'0 0 0 10px',
                            border:'1px solid #a3aeb9'}}>
                    <div style={{display:'flex', flexDirection:'row', padding:'5px 5px 0'}}>
                        {!error && showFormType && formTypeList()}
                        {!error && resetButton()}
                    </div>
                    <div>
                        <TablePanelConnected {...{tableModel, fieldKey}} />
                        {!error && renderSqlArea()}
                    </div>
                </div>
            </div>
        );
    }

    resetTable(catName, dd_short, createDDRequest, groupKey, fieldKey) {
        resetConstraints(groupKey, fieldKey);
        this.fetchDD(catName, dd_short, createDDRequest, true);
    }

    /**
     * Getting dd info from catalog name catName
     * @param {string} catName string name of the catalog for searching DD information
     * @param {string} dd_short
     * @param {function} createDDRequest
     * @param {boolean} clearSelections
     */
    fetchDD(catName, dd_short, createDDRequest, clearSelections = false) {

        var tblid = getTblId(catName, dd_short);

        //// Check if it exists already - fieldgroup has a keepState property but
        //// here we are not using the table as a fieldgroup per se so we need to cache the column restrictions changes
        const tbl = getTblById(tblid);
        if (tbl && !clearSelections) {
            this.setState({tableModel: tbl});
            return;
        }

        const request = createDDRequest(); //Fetch DD master table
        const urlDef = get(FieldGroupUtils.getGroupFields(this.props.groupKey), 'cattable.coldef', 'null');

        console.log('fetch DD: ' + JSON.stringify(request));

        fetchTable(request).then((tableModel) => {
            const tableModelFetched = tableModel;
            tableModelFetched.tbl_id = tblid;
            addConstraintColumn(tableModelFetched, this.props.groupKey);
            addColumnDef(tableModelFetched, urlDef);
            //hideColumns(tableModelFetched);
            setRowsChecked(tableModelFetched,  this.props.groupKey);

            updateColumnWidth(tableModelFetched,  ['description', 'Description'], -1);
            updateColumnWidth(tableModelFetched, ['name', 'Name', 'Field', 'field'], -1);

            TblCntlr.dispatchTableReplace(tableModel);
            this.setState({tableModel: tableModelFetched});
        }).catch((reason) => {
                console.log(reason.message);
                const errTable = TblUtil.createErrorTbl(tblid, `Catalog Fetch Error: ${reason.message}`);

                TblCntlr.dispatchTableReplace(errTable);
                this.setState({tableModel: errTable});
            }
        );
    }
}

/**
 * @summary update the column width in the table model
 * @param {TableModel} anyTableModel
 * @param {string[]} colName
 * @param {number} colWidth
 */
function updateColumnWidth(anyTableModel, colName, colWidth) {
    if (!isArray(colName)) {
        colName = [colName];
    }

    colName.find((cName) => {
        var idx = getColumnIdx(anyTableModel, cName);

        if (idx >= 0) {
            if (colWidth < 0) {
                var w = anyTableModel.tableData.data.reduce((prev, d) => {
                    if (d[idx].length > prev)  {
                        prev = d[idx].length;
                    }
                    return prev;
                }, colWidth);
                colWidth = w;
            }
            anyTableModel.tableData.columns[idx].width = colWidth;
        }
        return (idx >= 0);
    });
}
/**
 * Set true checkboxes column wherever a row should be selected based on the stored status or the 'sel' value
 * the 'sel' value for each column is set as the following condition:
 * - table model is acquired the first time: add 'sel' column with all 'y' if 'sel' column doesn't exit
 * - table model status is kept already: the 'sel' column value may be updated by column selection operation,
 *                                       set the value of SelectInfo as the column 'sel' value
 * @param {TableModel} anyTableModel
 * @param {string} gkey group key
 */
function setRowsChecked(anyTableModel, gkey) {
    var {selcols = '', errorConstraints} = get(FieldGroupUtils.getGroupFields(gkey), 'tableconstraints.value', {});
    var filterAry = selcols ? selcols.split(',') : [];
    const selectInfoCls = SelectInfo.newInstance({rowCount: anyTableModel.totalRows});
    var idxColSel = getColumnIdx(anyTableModel, 'sel');

    if (idxColSel < 0) {     // no 'sel' column exist
        idxColSel = addSelColumn(anyTableModel, filterAry, errorConstraints);
    }
    if (filterAry.length > 0 || !isEmpty(errorConstraints)) {  // get kept status (some selected or none selected)
        anyTableModel.tableData.data.forEach((arow, index) => {
            if (filterAry.includes(arow[0])) {
                selectInfoCls.setRowSelect(index, true);
            }
        });
    } else { // the table model may be freshly acquired or column selection status is updated as all columns are deselected
        anyTableModel.tableData.data.forEach((arow, index) => {
            if (arow[idxColSel] === 'y') {
                selectInfoCls.setRowSelect(index, true);
            }
        });
    }
    const selectInfo = selectInfoCls.data;
    merge(anyTableModel, {selectInfo});
}

/**
 * @summary add selection column and set each column to be selected
 * @param {TableModel} tableModelFetched
 * @param {string[]} filterAry
 * @param {string} errors
 * @returns {number} the index of newly added 'sel' column in 'columns' array
 */
function addSelColumn(tableModelFetched, filterAry, errors) {
    const idx = tableModelFetched.tableData.columns.length;
    const selCol = {name: 'sel', type: 'char', visibility: 'hide', width: 6};

    tableModelFetched.tableData.columns.push(selCol);

    tableModelFetched.tableData.data.forEach((e) => {
        if (filterAry.length > 0) {             // restore from kept status
            e[idx] = filterAry.includes(e[0]) ? 'y' : 'n';
        } else {                                // if there is no error, it means the table model is freshly accessed
            e[idx] = isEmpty(errors) ? 'y' : 'n';
        }
    });
    return idx;
}

/**
 * Add to table columns an extra colDef
 * original columns from table DD are: name,constraints,description,units,indx,dbtype,tableflg,sel
 * @param {Object} tableModelFetched
 * @param {string} urlDef
 */
function addColumnDef(tableModelFetched, urlDef) {
    const nCols = tableModelFetched.tableData.columns.length;
    const u = (isEmpty(urlDef) || urlDef === 'null') ? '#' : urlDef.match(/href='([^']+)'/)[1] + '#';
    tableModelFetched.tableData.columns.splice(nCols, 0, {visibility: 'hide', name: 'coldef', type: 'char'});
    tableModelFetched.tableData.data.map((e) => {
        e.splice(nCols, 0, u + e[0]);
    });
}

/**
 * @summary add constraint column to table model
 * @param {Object} tableModelFetched
 * @param {string} gkey group key
 */
function addConstraintColumn(tableModelFetched, gkey) {
    const idxSqlCol = sqlConstraintsCol.idx; // after 'name' column
    var   filterStatus = get(FieldGroupUtils.getGroupFields(gkey), 'tableconstraints.value.filters', {});

    tableModelFetched.tableData.columns.splice(idxSqlCol, 0, {...sqlConstraintsCol});
    tableModelFetched.tableData.data.map((e) => {
        e.splice(idxSqlCol, 0, get(filterStatus, e[0], ''));
    });
}

/**
 * Hide the colunms after index = 3
 * @param tableModelFetched
 */
/*
function hideColumns(tableModelFetched) {
    tableModelFetched.tableData.columns.map(
        (e, idx) => {
            if (idx > 3) {
                e.visibility = 'hide';
            }
        });
}
*/

/*
 * proptype for component CatalogConstraintsPanel
 * @type {{onChange: (func|any), catname: (isRequired|any), constraintskey: (string|any), tbl_id: (string|any), fieldKey: (string|any), groupKey: (string|any)}}
 */
CatalogConstraintsPanel.propTypes = {
    catname: PropTypes.string.isRequired,
    dd_short: PropTypes.string,
    constraintskey: PropTypes.string,
    fieldKey: PropTypes.string.isRequired,
    groupKey: PropTypes.string.isRequired,
    showFormType: PropTypes.bool,
    processId: PropTypes.string,
    createDDRequest: PropTypes.func.isRequired
};

CatalogConstraintsPanel.defaultProps = {
    catname: '',
    processId: 'GatorDD',
    showFormType: true
};

/**
 * @summary compoent to display the data restrictions into a tabular format
 * @returns {Object} constraint table
 */
class ConstraintPanel extends Component {

    constructor(props) {
        super(props);

        this.newInputCell = createInputCell(FILTER_TTIPS,
            15,
            FilterInfo.conditionValidatorNoAutoCorrect,
            //null,
            this.props.onTableChanged);
    }

    render() {
        const {tableModel, onTableChanged} = this.props;
        const tbl_ui_id = tableModel.tbl_id + '-ui';
        const tbl = getTblById(tableModel.tbl_id);
        const {columns, data} = get(tbl, 'tableData', {});
        const selectInfoCls = has(tbl, 'selectInfo') && SelectInfo.newInstance(tbl.selectInfo, 0);
        const totalCol = columns ? (columns.length-1) : 0;

        //const {tableconstraints} = FieldGroupUtils.getGroupFields(groupKey);
        //console.log('constraint: ' + tableconstraints.value.constraints + ' errorConstraints: ' + tableconstraints.value.errorConstraints);

        return (

            <div style={{display:'inline-block',
                     width: '97%', height: '170px', padding: '5px 5px'}}>
                <div style={{ position: 'relative', width: '100%', height: '100%'}}>
                    <div className='TablePanel'>
                        <div className={'TablePanel__wrapper--border'}>
                            <div className='TablePanel__table' style={{top: 0}}>
                                <BasicTableView
                                    tbl_ui_id={tbl_ui_id}
                                    columns={columns}
                                    data={data}
                                    showToolbar={false}
                                    selectInfoCls={selectInfoCls}
                                    selectable={true}
                                    currentPage={1}
                                    hlRowIdx={0}
                                    key={tableModel.tbl_id}
                                    error={tableModel.error}
                                    callbacks={
                                        {
                                           onRowSelect: updateRowSelected(tableModel.tbl_id, onTableChanged),
                                           onSelectAll: updateRowSelected(tableModel.tbl_id, onTableChanged)
                                        }
                                    }
                                    renderers={
                                        {
                                            name:
                                                { cellRenderer:
                                                    createLinkCell(
                                                        {
                                                            hrefColIdx: totalCol
                                                        }
                                                    )
                                                },
                                            constraints:
                                                {
                                                    cellRenderer: this.newInputCell

                                                }
                                        }
                                    }
                                />
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        );
    }
}

export const TablePanelConnected = fieldGroupConnector(ConstraintPanel, getProps, null, null);

function getProps(params, fireValueChange) {
    return Object.assign({}, params,
        {
            onTableChanged: () => handleOnTableChanged(params, fireValueChange)
        });
}

/**
 * @summary update row selection (only update selectInfo, not change data in table model)
 * @param {string} tbl_id
 * @param {Function} onTableChanged
 * @returns {Function}
 */
function updateRowSelected(tbl_id, onTableChanged) {
    return (checked, rowIndex) => {
        const tbl = getTblById(tbl_id);
        const selectInfo = tbl.selectInfo ? cloneDeep(tbl.selectInfo) : {};
        const selectInfoCls = SelectInfo.newInstance(selectInfo, 0);

        if (!isNil(rowIndex)) {
            selectInfoCls.setRowSelect(rowIndex, checked);
        } else {
            selectInfoCls.setSelectAll(checked);
        }
        TblCntlr.dispatchTableSelect(tbl_id, selectInfoCls.data);
        onTableChanged && onTableChanged();
    }
}

/**
 * @summary update the state based on the change of column search constraints or column selection
 * @param {Object} params
 * @param {Function} fireValueChange
 */
function handleOnTableChanged(params, fireValueChange) {
    const {tbl_id} = params.tableModel;
    const tbl = getTblById(tbl_id);





    if (!tbl) return;

    const tbl_data = tbl.tableData.data;
    const sel_info  =  tbl.selectInfo;
    let filters = {};
    let sqlTxt = '';
    let errors = '';

    tbl_data.forEach((d) => {
        const filterStrings = d[1].trim();
        const colName = d[0];

        if (filterStrings && filterStrings.length > 0) {
            filters[colName] = filterStrings;
            const parts = filterStrings && filterStrings.split(';');

            parts.forEach((v) => {
                var {valid, value} = FilterInfo.conditionValidatorNoAutoCorrect(v);  // TODO: need more detail syntax check

                if (!valid) {
                    errors += (errors.length > 0 ? ` ${value}` : `Invalid constraints: ${value}`);
                } else if (v) {
                    var oneSql = `${colName} ${v}`;
                    sqlTxt += (sqlTxt.length > 0 ? ` AND ${oneSql}` : oneSql);
                }
            });
        }
    });

    // collect the column name of all selected column
    let selCols = tbl_data.reduce((prev, d, idx) => {
            if ((sel_info.selectAll && (!sel_info.exceptions.has(idx))) ||
                (!sel_info.selectAll && (sel_info.exceptions.has(idx)))) {
                prev += d[0] + ',';
            }
            return prev;
        }, '');

    if (isEmpty(selCols)) {
        errors = 'Invalid selection: no column is selected';
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
            value: {constraints: sqlTxt, selcols: selCols, errorConstraints: errors, filters}
        });
    }
}


//const extract_regex = new RegExp('(<|>|>=|<=|=|!=|like|in)(.+)', 'i');
/*
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
*/

/**
 * @summary component for entering customized SQL conditions
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

/*
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
*/