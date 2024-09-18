/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import {Box, Button, Stack, Typography} from '@mui/joy';
import React, {PureComponent, memo} from 'react';
import PropTypes from 'prop-types';
import {isEmpty, merge, isNil, isArray, cloneDeep, has} from 'lodash';
import FieldGroupUtils from '../../fieldGroup/FieldGroupUtils.js';
import {dispatchValueChange} from '../../fieldGroup/FieldGroupCntlr.js';
import {fetchTable} from '../../rpc/SearchServicesJson.js';
import {getColumnIdx, getTblById, createErrorTbl} from '../../tables/TableUtil.js';
import {BasicTableViewWithConnector} from '../../tables/ui/BasicTableView.jsx';
import {createLinkCell, createInputCell} from '../../tables/ui/TableRenderer.js';
import * as TblCntlr from '../../tables/TablesCntlr.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {FilterInfo, FILTER_CONDITION_TTIPS} from '../../tables/FilterInfo.js';
import {ListBoxInputField} from '../../ui/ListBoxInputField.jsx';
import {InputAreaFieldConnected} from '../../ui/InputAreaField.jsx';
import {useFieldGroupConnector} from '../../ui/FieldGroupConnector.jsx';
const sqlConstraintsCol = {name: 'constraints', idx: 1, type: 'char', width: 10};

import {TableMask} from 'firefly/ui/panel/MaskPanel.jsx';

/*
 * update short_dd to be one of ['short', 'long', ''] based on if 'showForm' is true or not
 */
function makeFormType(showForm, short_dd) {
    return  !showForm ? '' : (isNil(short_dd) ? 'short' : (short_dd === 'true' ? 'short' : 'long'));
}

export function getTblId(catName, dd_short) {
    const dds = dd_short ? `-${dd_short}` : '';
    return catName ? `${catName}${dds}-dd-table-constraint` : '';
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

export class CatalogConstraintsPanel extends PureComponent {

    /**
     *
     * @param props
     */
    constructor(props) {
        super(props);
        this.state = {};
        this.fetchDD = this.fetchDD.bind(this);
        this.resetTable = this.resetTable.bind(this);
        this.afterFetchDD = this.afterFetchDD.bind(this);
    }


    componentWillUnmount() {
        this.iAmMounted = false;
    }

    componentDidMount() {
        this.iAmMounted = true;
        const {catname, createDDRequest, dd_short, showFormType = true} = this.props;

        this.fetchDD(catname, makeFormType(showFormType, dd_short), createDDRequest, true, this.afterFetchDD); //short form as default
    }

    UNSAFE_componentWillReceiveProps(np) {
        const ddShort = makeFormType(np.showFormType, np.dd_short);

        if (np.processId !== this.props.processId) {
            this.fetchDD(np.catname, ddShort, np.createDDRequest, true, this.afterFetchDD);
        } else if (np.catname !== this.props.catname || np.dd_short !== this.props.dd_short) {
            if (np.catname !== this.props.catname) {
                resetConstraints(np.groupKey, np.fieldKey);
            }
            this.fetchDD(np.catname, ddShort, np.createDDRequest, true, this.afterFetchDD);   //column selection or constraint needs update
        } else if (this.state.tableModel) {      // TODO: when will this case happen
            const tblid = np.tbl_id ? np.tbl_id : getTblId(np.catname, ddShort);
            if (tblid && tblid !== this.state.tableModel.tbl_id) {
                this.afterFetchDD({tableModel: getTblById(tblid)});
            }
        }
    }

    afterFetchDD(updateState) {
        if (this && this.iAmMounted) {
            this.setState(updateState);
        }
    }

    render() {
        const {tableModel} = this.state;
        const {error} = tableModel || {};
        const {catname, dd_short, fieldKey, showFormType=true, createDDRequest, groupKey} = this.props;

        const resetButton = () => {
            const ddShort = makeFormType(showFormType, dd_short);

            return (
                <Button size='sm' onClick={ () => this.resetTable(catname,  ddShort, createDDRequest, groupKey, fieldKey)}>
                    Reset
                </Button>
            );
        };

        const formTypeList = () => {
                return (
                   <ListBoxInputField fieldKey={'ddform'}
                       initialState={{
                            tooltip: 'Select form',
                            value: 'false'
                       }}
                       options={[
                            {label: 'Standard', value: 'true'},
                            {label: 'Long form', value: 'false'}
                       ]}
                       label='Table Selection:'
                       size='sm'
                   />
                );
        };

        if (isEmpty(tableModel) || !tableModel.tbl_id.startsWith(catname)) return  <TableMask sx={{flexGrow:1}}/>;

        return (
            <Stack spacing={1} flexGrow={1}>
                <Stack direction='row' spacing={2}>
                    {!error && showFormType && formTypeList()}
                    {!error && resetButton()}
                </Stack>
                <Stack spacing={1} flexGrow={1}>
                    <TablePanelConnected {...{tableModel, fieldKey}} />
                    {!error && renderSqlArea()}
                </Stack>
            </Stack>
        );
    }

    resetTable(catName, dd_short, createDDRequest, groupKey, fieldKey) {
        resetConstraints(groupKey, fieldKey);
        this.fetchDD(catName, dd_short, createDDRequest, true, this.afterFetchDD);
    }

    /**
     * Getting dd info from catalog name catName
     * @param {string} catName string name of the catalog for searching DD information
     * @param {string} dd_short
     * @param {function} createDDRequest
     * @param {boolean} clearSelections
     * @param {func} afterFetch
     */
    fetchDD(catName, dd_short, createDDRequest, clearSelections = false, afterFetch) {

        const {groupKey, fieldKey} = this.props;
        const tblid = getTblId(catName, dd_short);

        //// Check if it exists already - fieldgroup has a keepState property but
        //// here we are not using the table as a fieldgroup per se so we need to cache the column restrictions changes
        const tbl = getTblById(tblid);

        if (tbl && !clearSelections) {
            afterFetch&&afterFetch({tableModel: tbl});
            return;
        }

        const request = createDDRequest(); //Fetch DD master table

        fetchTable(request).then((tableModel) => {

            const fields = FieldGroupUtils.getGroupFields(groupKey);
            const urlDef = FieldGroupUtils.getGroupFields(groupKey)?.cattable?.coldef;

            const tableModelFetched = tableModel;
            tableModelFetched.tbl_id = tblid;
            addConstraintColumn(tableModelFetched, groupKey);
            addColumnDef(tableModelFetched, urlDef);
            setRowsChecked(tableModelFetched, groupKey, fieldKey);

            updateColumnWidth(tableModelFetched, ['description', 'Description'], -1);
            updateColumnWidth(tableModelFetched, ['name', 'Name', 'Field', 'field'], -1);

            TblCntlr.dispatchTableReplace(tableModel);
            afterFetch&&afterFetch({tableModel: tableModelFetched});
        }).catch((reason) => {
                const errTable = createErrorTbl(tblid, `Failed to fetch catalog fields: ${reason.message}`);

                TblCntlr.dispatchTableReplace(errTable);
                afterFetch&&afterFetch({tableModel: errTable});
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
        const idx = getColumnIdx(anyTableModel, cName);

        if (idx >= 0) {
            if (colWidth < 0) {
                colWidth = (anyTableModel?.tableData?.data ?? []).reduce((prev, d) => {
                    if (d[idx].length > prev)  {
                        prev = d[idx].length;
                    }
                    return prev;
                }, colWidth);
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
 * @param {string} fieldKey field key
 */
function setRowsChecked(anyTableModel, gkey, fieldKey) {
    const {selcols = '', errorConstraints} = FieldGroupUtils.getGroupFields(gkey)?.[fieldKey]?.value ?? {};
    const filterAry = selcols ? selcols.split(',') : [];
    const selectInfoCls = SelectInfo.newInstance({rowCount: anyTableModel.totalRows});
    let idxColSel = getColumnIdx(anyTableModel, 'sel');

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
    const u = urlDef ? urlDef.match(/href='([^']+)'/)[1] + '#' : null;
    tableModelFetched.tableData.columns.splice(nCols, 0, {visibility: 'hide', name: 'coldef', type: 'char'});
    tableModelFetched.tableData.data.map((e) => {
        e.splice(nCols, 0, (u ? u + e[0] : null));
    });
}

/**
 * @summary add constraint column to table model
 * @param {Object} tableModelFetched
 * @param {string} gkey group key
 * @param {string} fieldKey fieldKey
 */
function addConstraintColumn(tableModelFetched, gkey, fieldKey) {
    const idxSqlCol = sqlConstraintsCol.idx; // after 'name' column
    const filterStatus = FieldGroupUtils.getGroupFields(gkey)?.[fieldKey]?.value?.filters ?? {};

    tableModelFetched.tableData.columns.splice(idxSqlCol, 0, {...sqlConstraintsCol});
    tableModelFetched.tableData.data.map((e) => {
        e.splice(idxSqlCol, 0, filterStatus?.[e[0]] ?? '');
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
 * @type {{onChange: (func|any), catname: (isRequired|any), tbl_id: (string|any), fieldKey: (string|any), groupKey: (string|any)}}
 */
CatalogConstraintsPanel.propTypes = {
    catname: PropTypes.string.isRequired,
    dd_short: PropTypes.string,
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
 * @summary component to display the data restrictions into a tabular format
 * @returns {Object} constraint table
 */
class ConstraintPanel extends PureComponent {

    constructor(props) {
        super(props);

        this.newInputCell = createInputCell(FILTER_CONDITION_TTIPS,
            15,
            FilterInfo.conditionValidatorNoAutoCorrect,
            this.props.onTableChanged, {width: '100%', boxSizing: 'border-box'});
    }

    componentDidMount() {
        // make sure field value is consistent with the table
        this.props.onTableChanged();
    }

    componentDidUpdate(prevProps) {
        if (prevProps?.tableModel?.tbl_id !== this.props?.tableModel?.tbl_id) {
            // make sure field value is consistent with the table
            this.props.onTableChanged();
        }
    }
    
    render() {
        const {tableModel, onTableChanged} = this.props;
        const tbl_ui_id = tableModel.tbl_id + '-ui';
        const tbl = getTblById(tableModel.tbl_id);
        const {columns, data} = tbl?.tableData ?? {};
        const selectInfoCls = has(tbl, 'selectInfo') && SelectInfo.newInstance(tbl.selectInfo, 0);
        const totalCol = columns ? (columns.length-1) : 0;

        return (
            <Stack sx={{direction:'row', flex: '1 1 auto',
                '& .fixedDataTableLayout_main': {borderRadius:'5px'} }}>
                <Stack {...{ direction:'row', flex: '1 1 auto', position: 'relative', width: 1, height: 1}}>
                    <Box sx={{minHeight:150, flex: '1 1 auto'}}>
                            <Stack sx={{m: '1px', position: 'absolute', inset: 0}}>
                                <BasicTableViewWithConnector
                                    tbl_ui_id={tbl_ui_id}
                                    columns={columns}
                                    data={data}
                                    showToolbar={false}
                                    selectInfoCls={selectInfoCls}
                                    selectable={true}
                                    currentPage={1}
                                    key={tableModel.tbl_id}
                                    error={tableModel.error}
                                    rowHeight={26}
                                    callbacks={{
                                        onRowSelect: updateRowSelected(tableModel.tbl_id, onTableChanged),
                                        onSelectAll: updateRowSelected(tableModel.tbl_id, onTableChanged)
                                    }}
                                    renderers={{
                                        name: { cellRenderer: createLinkCell({hrefColIdx: totalCol})},
                                        constraints: { cellRenderer: this.newInputCell}
                                    }}
                                />
                            </Stack>
                    </Box>
                </Stack>
            </Stack>
        );
    }
}

export const TablePanelConnected= memo( (props) => {
    const {viewProps, fireValueChange}=  useFieldGroupConnector(props);
    return (
        <ConstraintPanel {...viewProps}
                         onTableChanged= {() => handleOnTableChanged(viewProps, fireValueChange)} />
    );
});





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
    };
}

/**
 * @summary update the state based on the change of column search constraints or column selection
 * @param {Object} params
 * @param {Function} fireValueChange
 */
function handleOnTableChanged(params, fireValueChange) {
    const {tbl_id} = params.tableModel;
    const tbl = getTblById(tbl_id);

    if (!tbl || tbl.error) return;

    const tbl_data = tbl.tableData.data;
    const sel_info  =  tbl.selectInfo;
    const filters = {};
    let sqlTxt = '';
    let errors = '';

    tbl_data.forEach((d) => {
        const filterStrings = d[1].trim();
        const colName = d[0];

        if (filterStrings && filterStrings.length > 0) {
            filters[colName] = filterStrings;
            const parts = filterStrings && filterStrings.split(';');

            parts.forEach((v) => {
                const {valid, value} = FilterInfo.conditionValidatorNoAutoCorrect(v);  // TODO: need more detail syntax check

                if (!valid) {
                    errors += (errors.length > 0 ? ` ${value}` : `Invalid constraints: ${value}`);
                } else if (v) {
                    const oneSql = `${colName} ${v}`;
                    sqlTxt += (sqlTxt.length > 0 ? ` AND ${oneSql}` : oneSql);
                }
            });
        }
    });

    // collect the column name of all selected column
    let allSelected = true;

    const selCols = tbl_data.reduce((prev, d, idx) => {
            if ((sel_info.selectAll && (!sel_info.exceptions.has(idx))) ||
                (!sel_info.selectAll && (sel_info.exceptions.has(idx)))) {
                prev += d[0] + ',';
            } else {
                allSelected = false;
            }
            return prev;
        }, '');

    if (isEmpty(selCols)) {
        errors = 'Invalid selection: no column is selected';
    } /*else if (allSelected) {
        selCols = '';
    }*/



    // the value of this input field is a string
    const val = params?.value ?? '';
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
            value:  {constraints: sqlTxt, selcols: selCols, errorConstraints: errors, filters}
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
        <Stack spacing={.5}>
            <InputAreaFieldConnected fieldKey='txtareasql'
                                     placeholder={'Add additional constraints here (SQL)'}
                                     tooltip='Enter SQL additional constraints here'
                                     maxRows={4}
            />
            <Stack>
                <Typography level='body-sm' component='div'>
                    <span>
                        <Typography component='span' level='title-sm'>Ex: </Typography>
                        w3snr&gt;7 and (w2mpro-w3mpro)&gt;1.5 and ra&gt;102.3 and ra&lt;112.3 and dec&lt;-5.5 and
                        dec&gt;
                        -15.5
                    </span>
                    (source_id_mf = '1861p075_ac51-002577')
                    <br/>
                </Typography>
                <Typography color='warning' level='body-sm'>The format for date type is yyyy-mm-dd</Typography>
            </Stack>
        </Stack>
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
