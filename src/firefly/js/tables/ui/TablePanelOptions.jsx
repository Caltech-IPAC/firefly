/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useEffect} from 'react';
import PropTypes from 'prop-types';
import {Button, Checkbox, Stack, ToggleButtonGroup, Typography} from '@mui/joy';
import {cloneDeep, get, isEmpty} from 'lodash';

import {setSqlFilter, SqlTableFilter} from './FilterEditor.jsx';
import {InputField} from '../../ui/InputField.jsx';
import {intValidator} from '../../util/Validate.js';
import {useStoreConnector} from '../../ui/SimpleComponent.jsx';
import {dispatchTableAddLocal, dispatchTableFilter, dispatchTableRemove, dispatchTableUiUpdate, TABLE_SELECT} from '../TablesCntlr.js';
import {
    calcColumnWidths, COL_TYPE, getColumn, getColumnValues, getFilterCount, getSqlFilter, getTableUiById,
    getTblById, isClientTable, isOfType, parsePrecision, watchTableChanges
} from '../TableUtil.js';
import {TablePanel} from './TablePanel';
import {FILTER_CONDITION_TTIPS, FilterInfo} from '../FilterInfo';
import {inputColumnRenderer} from './TableRenderer.js';
import {dispatchHideDialog} from '../../core/ComponentCntlr.js';
import {POPUP_DIALOG_ID} from '../../ui/PopupUtil.jsx';
import {StatefulTabs, Tab} from '../../ui/panel/TabPanel.jsx';
import {SelectInfo} from '../SelectInfo.js';
import {HelpIcon} from '../../ui/HelpIcon.jsx';
import {getTblPrefKey} from '../TablePref.js';
import {showAddOrUpdateColumn} from './AddOrUpdateColumn.jsx';
import {BY_SCROLL} from './BasicTableView.jsx';

import EDIT from 'html/images/16x16_edit_icon.png';
import {MAX_ROW} from 'firefly/tables/TableRequestUtil';


export const TablePanelOptions = React.memo(({tbl_ui_id, tbl_id, onChange, onOptionReset, clearFilter}) => {

    const uiState = useStoreConnector(() => getTableUiById(tbl_ui_id));
    const ctm_tbl_id = `${tbl_ui_id}-columnOptions`;
    const showAdvFilter = !isClientTable(tbl_id);
    const showTblPrefMsg = getTblPrefKey(tbl_ui_id);

    const {sql=''} = getSqlFilter(tbl_id);
    const advFilterName = 'Advanced Filter';
    const label = () => {
        if (!sql) return advFilterName;
        return (
            <div style={{display: 'inline-flex'}}>
                <div style={{marginRight: 10}}>{advFilterName}</div>
                <div title='Advanced filter applied.  Click on tab to view details.'
                    style={{
                        position: 'absolute',
                        right: 2,
                        borderLeft: '10px solid transparent',
                        borderTop: '10px solid rgb(71, 138, 163)'
                    }}/>
            </div>
        );
    };

    const onReset = () => {
        if (onOptionReset) {
            onOptionReset();
            createColumnTableModel(tbl_id, tbl_ui_id, ctm_tbl_id);
        }
    };

    const onRemoveFilters = () => {
        if (clearFilter) {
            clearFilter();
            createColumnTableModel(tbl_id, tbl_ui_id, ctm_tbl_id);
            setSqlFilter('', '');
        }
    };


    const onClose = () => {
        dispatchHideDialog(POPUP_DIALOG_ID);
    };

    const onSqlFilterChanged = ({op, sql}) =>  onChange({sqlFilter: sql ? `${op}::${sql}` : ''});

    return (
        <Stack height={1} p={1} pt={0} boxSizing='border-box'>
            <Options {...{uiState, tbl_id, tbl_ui_id, ctm_tbl_id, onOptionReset, onChange}} />
            <OptionsFilterStats tbl_id={ctm_tbl_id}/>
            <StatefulTabs componentKey='TablePanelOptions' defaultSelected={0} borderless={true} useFlex={true} style={{flex: '1 1 0'}}>
                <Tab name='Column Options'>
                    <ColumnOptions {...{tbl_id, tbl_ui_id, ctm_tbl_id, onChange}} />
                </Tab>
                {showAdvFilter &&
                    <Tab name={advFilterName} label={label()}>
                        <div style={{display: 'flex', flex: '1 1 0', position: 'relative'}}>
                            <SqlTableFilter {...{tbl_id, tbl_ui_id, onChange}} onChange={onSqlFilterChanged} />
                        </div>
                    </Tab>
                }
            </StatefulTabs>
            {showTblPrefMsg && <TablePrefMsg/>}
            <Stack direction='row' mt={1} sx={{'button':{mr:1}}} alignItems='center'>
                <Button variant='solid' color='primary' onClick={onClose}>Close</Button>
                <Button onClick={onReset}>Reset column selection</Button>
                <Button onClick={onRemoveFilters}>Remove all filters</Button>
                <HelpIcon helpId={'tables.options'} style={{float: 'right', marginTop: 4}}/>
            </Stack>
        </Stack>
    );
});

TablePanelOptions.propTypes = {
    tbl_ui_id: PropTypes.string,
    tbl_id: PropTypes.string,
    onChange: PropTypes.func,
    onOptionReset: PropTypes.func,
    clearFilter: PropTypes.func
};

function OptionsFilterStats({tbl_id}) {

    const filterCnt = useStoreConnector(() => getFilterCount(getTblById(tbl_id)));
    const filterStr = filterCnt === 0 ? '' : filterCnt === 1 ? '1 filter' : `${filterCnt} filters`;
    const clearFilters = () => dispatchTableFilter({tbl_id, filters: ''});;

    if (filterCnt === 0) return null;
    return (
        <div style={{ position: 'absolute', top: 30, zIndex: 1, right: 5}}>
            <button onClick={clearFilters}> remove {filterStr}</button>
        </div>
    );
}

function Options({uiState, tbl_id, tbl_ui_id, ctm_tbl_id, onOptionReset, onChange}) {
    const {pageSize, showPaging=true, showUnits, allowUnits=true, showTypes, allowTypes=true, showFilters} = uiState || {};

    const onPageSize = (pageSize) => {
        if (pageSize.valid) {
            onChange && onChange({pageSize: pageSize.value});
        }
    };
    const [value, setValue] = React.useState([
                                    showUnits && 'showUnits',
                                    showTypes && 'showTypes',
                                    showFilters && 'showFilters'
                                ].filter((v) => v));
    return (
        <Stack direction='row' alignItems='center' justifyContent='space-between' mb={1}>
            <Stack direction='row' spacing={1} alignItems='center'>
                <Typography level='title-md'>Show/Hide:</Typography>
                <ToggleButtonGroup
                    variant='outlined'
                    value={value}
                    onChange={(ev, val) => {
                        setValue(val);
                        const bname = ev.target?.value;
                        onChange({[bname]: val.includes(bname)});
                    }}
                >
                    {allowUnits &&
                    <Button value='showUnits' size='sm'>Units</Button>
                    }
                    {allowUnits &&
                    <Button value='showTypes' size='sm'>Data Type</Button>
                    }
                    <Button value='showFilters' size='sm'>Filters</Button>
                </ToggleButtonGroup>
            </Stack>
            {showPaging && pageSize !== MAX_ROW &&
            <InputField
                orientation='horizontal'
                slotProps={{input: {size: 'sm', sx: {width: '5em'}}}}
                validator={intValidator(1, 10000)}
                tooltip='Set page size'
                label='Page Size:'
                value={pageSize + ''}
                onChange={onPageSize}
                actOn={['blur', 'enter']}
            />
            }
        </Stack>
    );
}


const columns = [
    {name: 'name', fixed: true, width: 20},
    {name: 'filter', fixed: true, width: 10, sortable: false, filterable: false},
    {name: 'format'},
    {name: 'null_string'},
    {name: 'type'},
    {name: 'units'},
    {name: 'arraySize', width: 7},
    {name: 'utype'},
    {name: 'UCD'},
    {name: 'links'},
    {name: 'description'},
    {name: 'cname', visibility: 'hidden'},
    {name: 'show', visibility: 'hidden'},
];

const showIdx = columns.length-1;
const cnameIdx = columns.length-2;
const filterIdx = 1;
const typeIdx = 4;


export const ColumnOptions = React.memo(({tbl_id, tbl_ui_id, ctm_tbl_id, onChange}) => {

    const columns =  useStoreConnector( () => getTableUiById(tbl_ui_id)?.columns || []);
    const ckey = columns?.map((c) => c.name).join('|');
    const cmt_tbl_ui_id = ctm_tbl_id + '_ui';

    useEffect(() => {
        if (!isEmpty(columns)) {
            createColumnTableModel(tbl_id, tbl_ui_id, ctm_tbl_id);
        }
    }, [ckey]);

    useEffect(() => {
        watchTableChanges(ctm_tbl_id, [TABLE_SELECT], ({payload}) => {
            const {selectInfo} = payload || {};
            const selectInfoCls = SelectInfo.newInstance(selectInfo);
            const cnames = getColumnValues(getTblById(ctm_tbl_id), 'cname');
            const nColumns = cloneDeep(get(getTableUiById(tbl_ui_id), 'columns', []));
            nColumns.forEach((c) => {
                const ridx = cnames.findIndex((cn) => cn === c.name);
                if (ridx >= 0) {
                    c.visibility = selectInfoCls.isSelected(ridx) ? 'show' : 'hide';
                }
            });
            onChange({columns: nColumns});
        });

        return () => {
            dispatchTableRemove(ctm_tbl_id, false);
        };

    }, [tbl_ui_id, tbl_id, ctm_tbl_id]);     // run only if table changes



    // filters state are kept in tablemodel
    // the rest of the state are kept in the source table ui data
    const renderers =  {
                name:   {cellRenderer: makeNameRenderer(tbl_id, tbl_ui_id, cmt_tbl_ui_id)},
                filter: {cellRenderer: makeFilterRenderer(tbl_id, ctm_tbl_id, onChange)},
                // format:  {cellRenderer: makePrecisionRenderer(tbl_ui_id, ctm_tbl_id, onChange)},
                // null_string:   {cellRenderer: makeNullStringRenderer(tbl_ui_id, ctm_tbl_id, onChange)}
    };

    return (
        <div style={{flex: '1 1 0'}}>
            <TablePanel
                border={false}
                tbl_ui_id = {cmt_tbl_ui_id}
                tbl_id = {ctm_tbl_id}
                renderers = {renderers}
                showToolbar = {false}
                showOptionButton = {false}
                showTypes={false}
                showFilters={true}
                selectable = {true}
                rowHeight = {26}
                highlightedRowHandler = {()=>undefined}
            />
        </div>
    );
});

function TablePrefMsg() {

    return (
        <Typography level='body-sm' fontStyle='italic'>
            Column selection will apply to future searches of this table.
        </Typography>
    );
}

function createColumnTableModel(tbl_id, tbl_ui_id, ctm_tbl_id) {
    const filterInfoCls = FilterInfo.parse(get(getTblById(tbl_id), 'request.filters'));
    const data =  get(getTableUiById(tbl_ui_id), 'columns', [])
        .filter((c) => c.visibility !== 'hidden')
        .map( (c) => [
            c.label || c.name || '',
            filterInfoCls.getFilter(c.name) || '',
            c.precision || '',
            c.nullString || '',
            c.type || '',
            c.units || '',
            c.arraySize || '',
            c.utype || '',
            c.UCD || '',
            c.links || '',
            c.desc || '',
            c.name || '',
            !c.visibility || c.visibility === 'show'
        ]);
    const selectInfoCls = SelectInfo.newInstance({rowCount: data.length});
    for(let idx = 0; idx < data.length; idx++) {
        selectInfoCls.setRowSelect(idx, get(data,[idx, showIdx]));
    }
    const ctm = {selectInfo: selectInfoCls.data,  tableData: {columns, data}, totalRows: data.length};
    const widths = calcColumnWidths(columns, data);
    columns[0].width = widths[0] + 2;

    // hide empty columns
    ['format', 'null_string', 'arraySize', 'utype', 'UCD', 'links', 'description'].forEach((cname) => {
        const cl = getColumnValues(ctm, cname).filter((v) => v);
        if ( cl.length === 0 ) {
            getColumn(ctm, cname).visibility = 'hidden';
        }
    });

    ctm.tbl_id = ctm_tbl_id;
    dispatchTableAddLocal(ctm, undefined, false);
}


/* -------------------------------------- custom table cell renderers ------------------------------------------------------------*/

function getActiveInput(data, rowIdx, tbl_ui_id) {
    const selColName = get(data, [rowIdx, cnameIdx]);
    const nColumns = cloneDeep(get(getTableUiById(tbl_ui_id), 'columns', []));
    const selCol = nColumns.find((col) => col.name === selColName);
    return {selColName, nColumns, selCol};
}


/**
 * src/firefly/js/tables/ui/TableRenderer.js:45
 * @param tbl_id        ID of the data table
 * @param tbl_ui_id     ID of the UI table
 * @param cmt_tbl_ui_id UI ID of the column model table
 * @return Custom cell renderer for 'name' column
 */
function makeNameRenderer(tbl_id, tbl_ui_id, cmt_tbl_ui_id) {
    const tableModel = getTblById(tbl_id);

    return ({cellInfo}) => {
        const {value} = cellInfo;
        const c = getColumn(tableModel, value);
        const editCol = () => {
            const onChange = () => {
                // because we always rebuild the column table, it will be sorted ascending.  so, scroll to the bottom for derived columns.
                setTimeout(() => dispatchTableUiUpdate( {tbl_ui_id:cmt_tbl_ui_id, scrollTop:10000, triggeredBy:BY_SCROLL}), 200);
            };
            showAddOrUpdateColumn({tbl_ui_id, tbl_id, editColName:c.name, onChange});
        };
        if (c?.DERIVED_FROM) {
            return (
                <div style={{color:'maroon', display: 'inline-flex', alignItems: 'center'}} onClick={editCol}>
                    {value} <img className='clickable' style={{marginLeft: 5}} src={EDIT}/>
                </div>
            );
        } else return <div>{value}</div>;
    };
}



function makePrecisionRenderer(tbl_ui_id, ctm_tbl_id, onChange) {
    const tooltips = 'A string Tn where T is either F, E, G, HMS, or DMS\n' +
        '- When T is F or E, n is the number of significant figures after the decimal point\n' +
        '- When T is G, n is the number of significant digits\n' +
        '- When T is HMS or DMS, n is ignored\n' +
        'e.g. E5, F4, G6, or HMS';

    const onPrecision = (val, rowIdx,  data) => {
        const {nColumns, selCol} = getActiveInput(data, rowIdx, tbl_ui_id);
        if (selCol) {
            selCol.precision = val;             // set new precision value and clear all other format settings.
            selCol.format = undefined;
            selCol.fmtDisp = undefined;
        }
        onChange && onChange({columns: nColumns});
    };

    const isReadOnly = (rowIndex, data) => {
        return !isOfType(data[rowIndex][typeIdx],COL_TYPE.FLOAT);
    };

    const validator = (value) => {
        const [type] = parsePrecision(value);
        const valid = !!(!value || type);
        return {valid, value, message: tooltips};
    };

    return inputColumnRenderer({tbl_id:ctm_tbl_id, cname: 'precision', validator, isReadOnly, onChange:onPrecision, tooltips});
}


function makeFilterRenderer(tbl_id, ctm_tbl_id, onChange) {

    const collectFilterInfo = (data=[]) => {
        const filterCls = FilterInfo.parse('');
        data.filter( (row) => row[filterIdx])
            .forEach( (row) => {
                filterCls.setFilter(row[cnameIdx], row[filterIdx]);
            });

        return filterCls.serialize();
    };

    const onFilter = (value, rowIndex, data) => {
        const filterInfo = collectFilterInfo(data);
        const prevFilterInfo = get(getTblById(tbl_id), 'request.filters');
        if (filterInfo !== prevFilterInfo) {
            onChange && onChange({filterInfo});
        }
    };

    const validator = (cond, data, rowIndex) => {
        const cname = get(data, [rowIndex, cnameIdx]);
        return FilterInfo.conditionValidator(cond, tbl_id, cname);
    };

    return inputColumnRenderer({tbl_id:ctm_tbl_id, cname: 'filter', validator, onChange:onFilter,tooltips:FILTER_CONDITION_TTIPS});
}

function makeNullStringRenderer(tbl_ui_id, ctm_tbl_id, onChange) {
    const tooltips = 'The Null Values define the data values that will indicate no data, for example "null" or "9999.99"';

    const onNullString = (val, rowIdx,  data) => {
        const {nColumns, selCol} = getActiveInput(data, rowIdx, tbl_ui_id);
        selCol && (selCol.nullString = val);
        onChange && onChange({columns: nColumns});
    };

    return  inputColumnRenderer({tbl_id:ctm_tbl_id, cname: 'null_string', onChange:onNullString, tooltips});
}

