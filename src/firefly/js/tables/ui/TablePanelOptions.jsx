/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */

import React, {useCallback, useEffect} from 'react';
import {bool, func, shape, string, object} from 'prop-types';
import {Box, Button, Checkbox, Divider, Stack, Typography} from '@mui/joy';
import {badgeClasses} from '@mui/joy/Badge';

import {cloneDeep, defaultsDeep, get, isEmpty} from 'lodash';
import {BY_SCROLL} from '../reducer/TableUiReducer';

import {setSqlFilter, SqlTableFilter} from './FilterEditor.jsx';
import {InputField} from '../../ui/InputField.jsx';
import {intValidator} from '../../util/Validate.js';
import {Slot, useStoreConnector} from '../../ui/SimpleComponent.jsx';
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

import ModeEditOutlinedIcon from '@mui/icons-material/ModeEditOutlined';
import {MAX_ROW} from 'firefly/tables/TableRequestUtil';
import {ClearFilterButton} from 'firefly/visualize/ui/Buttons.jsx';
import {Stacker} from 'firefly/ui/Stacker.jsx';


export function TablePanelOptions({tbl_ui_id, tbl_id, onChange, onOptionReset, clearFilter, allowColumnSelection=true, slotProps, ...props}) {

    const uiState = useStoreConnector(() => getTableUiById(tbl_ui_id));
    const ctm_tbl_id = `${tbl_ui_id}-columnOptions`;
    const showTblPrefMsg = getTblPrefKey(tbl_ui_id) && allowColumnSelection;
    const showAdvFilter = !isClientTable(tbl_id) && slotProps?.advFilterTab?.component !== null;
    const showColumnOpt = slotProps?.columnOptTab?.component !== null;
    const {sql=''} = getSqlFilter(tbl_id);

    const advFilterName = 'Advanced Filter';
    const advFilterLabel = () => {
        if (!sql) return advFilterName;
        return (
            <Stack direction='row' spacing={1/2}>
                {advFilterName}
                <Box title='Advanced filter applied.  Click on tab to view details.'
                    sx={{
                        position: 'absolute',
                        right: 2,
                        borderLeft: '10px solid transparent',
                        borderTop: '10px solid rgb(71, 138, 163)'
                    }}/>
            </Stack>
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
    const actions = useCallback(() => <OptionsFilterStats tbl_id={ctm_tbl_id}/>, [ctm_tbl_id]);

    const mainContent = () => {
        if (showColumnOpt && showAdvFilter) {
            return (
                <Stack flexGrow={1} overflow='hidden'>
                    <StatefulTabs componentKey={`${tbl_id}-options`} actions={actions}>
                        <Tab {...defaultsDeep(slotProps?.columnOptTab, {name:'Column Options', sx:{p:1}}) }>
                            <Slot component={ColumnOptions} {...{tbl_id, tbl_ui_id, ctm_tbl_id, onChange, allowColumnSelection}} slotProps={slotProps?.columnOpt}/>
                        </Tab>
                        {showAdvFilter &&
                            <Tab {...defaultsDeep(slotProps?.advFilterTab, {name: advFilterName, label:advFilterLabel()}) }>
                                <Slot component={SqlTableFilter} {...{tbl_id, tbl_ui_id}} onChange={onSqlFilterChanged} slotProps={slotProps?.advFilter}/>
                            </Tab>
                        }
                    </StatefulTabs>
                </Stack>
            );
        } else {
            return (
                <Stack flexGrow={1} overflow='hidden'>
                    {showColumnOpt && <Slot component={ColumnOptions} {...{tbl_id, tbl_ui_id, ctm_tbl_id, onChange, allowColumnSelection}} slotProps={slotProps?.columnOpt}/>}
                    {showAdvFilter && <Slot component={SqlTableFilter} {...{tbl_id, tbl_ui_id}} onChange={onSqlFilterChanged} slotProps={slotProps?.advFilter}/>}
                </Stack>
            );
        }
    };

    return (
        <Stack height={1} p={1} pt={0} spacing={1} {...props}>
            <Slot component={Options} {...{uiState, tbl_id, tbl_ui_id, ctm_tbl_id, onOptionReset, onChange}} slotProps={slotProps?.header}/>
            {mainContent()}
            {showTblPrefMsg && <TablePrefMsg/>}
            <Slot component={Stacker} endDecorator={<HelpIcon helpId={'tables.options'}/>} slotProps={slotProps?.submitBar}>
                <Button variant='solid' color='primary' onClick={onClose}>Close</Button>
                {allowColumnSelection && <Button onClick={onReset}>Reset column selection</Button>}
                {(showColumnOpt || showColumnOpt) && <Button onClick={onRemoveFilters}>Remove all filters</Button>}
            </Slot>
        </Stack>
    );
}

TablePanelOptions.propTypes = {
    tbl_ui_id: string,
    tbl_id: string,
    onChange: func,
    onOptionReset: func,
    clearFilter: func,
    allowColumnSelection: bool,
    slotProps: shape({      // component: null to hide
        header: shape({...Stack.propTypes}),
        columnOptTab: object,       // Tab.propTypes, but cannot use Tab here because of forwardRef issue
        columnOpt: shape({...ColumnOptions.propTypes}),
        advFilterTab: object,
        advFilter: shape({...SqlTableFilter.propTypes}),
        submitBar: shape({...Stacker.propTypes}),
    })
};

function OptionsFilterStats({tbl_id}) {

    const filterCnt = useStoreConnector(() => getFilterCount(getTblById(tbl_id)));
    const clearFilters = () => dispatchTableFilter({tbl_id, filters: ''});;

    if (filterCnt === 0) return null;
    return (
        // wrapper stack added because ToolbarButton did not expose root
        <Stack sx = {{[`& .${badgeClasses.root}`]: {margin: '1px 3px 0 0'}}}>
            <ClearFilterButton iconButtonSize='34px' onClick={clearFilters}
                               badgeCount={filterCnt}
                               tip = 'Remove all Column Options filters'
            />
        </Stack>
    );
}

function Options({uiState, onChange}) {
    const {pageSize, showPaging=true, showUnits, allowUnits=true, showTypes, allowTypes=true, showFilters} = uiState || {};

    const onPageSize = (pageSize) => {
        if (pageSize.valid) {
            onChange && onChange({pageSize: pageSize.value});
        }
    };
    const handleChange =(ev) => {
        const bname = ev.target?.value;
        onChange({[bname]: ev.target.checked});
    };
    return (
        <Stack direction='row' alignItems='center' justifyContent='space-between'>
            <Stack direction='row' spacing={2} alignItems='center'>
                <Typography level='title-md'>Show/Hide:</Typography>
                <Stack direction='row' spacing={1}>
                    {allowUnits && <> <Checkbox size='sm' label='Units' value='showUnits' checked={showUnits} onChange={handleChange}/> <Divider orientation='vertical'/></>}
                    {allowTypes && <> <Checkbox size='sm' label='Data Type' value='showTypes' checked={showTypes} onChange={handleChange}/> <Divider orientation='vertical'/></>}
                    <Checkbox size='sm' label='Filters' value='showFilters' checked={showFilters} onChange={handleChange}/>
                </Stack>
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
    {name: 'arraySize'},
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


export function ColumnOptions ({tbl_id, tbl_ui_id, ctm_tbl_id, onChange, allowColumnSelection=true}) {

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
        <Stack position='relative' flexGrow={1} overflow='hidden'>
            <TablePanel
                border={false}
                tbl_ui_id = {cmt_tbl_ui_id}
                tbl_id = {ctm_tbl_id}
                renderers = {renderers}
                showToolbar = {false}
                showOptionButton = {false}
                showTypes={false}
                showFilters={true}
                selectable = {allowColumnSelection}
                rowHeight = {26}
                highlightedRowHandler = {()=>undefined}
            />
        </Stack>
    );
}
ColumnOptions.propTypes = {
    tbl_id: string,
    tbl_ui_id: string,
    ctm_tbl_id: string,
    onChange: func,
    selectable: bool
};

function TablePrefMsg() {

    return (
        <Typography level='body-sm' color='warning'>
            Column selection will apply to future searches of this table in this session.
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
                <Stack direction='row' spacing={1} alignItems='center' onClick={editCol} className='clickable'>
                    <Typography level='body-sm' color='warning'>{value}</Typography>
                    <ModeEditOutlinedIcon/>
                </Stack>
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

    const onFilter = (value, rowIndex, data) => {
        const cFilterInfo = FilterInfo.parse(getTblById(tbl_id)?.request?.filters || '');
        const cname = data[rowIndex]?.[cnameIdx];
        const conditions = data[rowIndex]?.[filterIdx];
        cFilterInfo.setFilter(cname, conditions);
        onChange?.({filterInfo: cFilterInfo.serialize()});
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

