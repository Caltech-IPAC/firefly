import {Button, Chip, Stack, Typography} from '@mui/joy';
import React, {useState, useEffect} from 'react';
import {cloneDeep, get, isEmpty,} from 'lodash';

import {getCellValue, getColumn, getColumns, getColumnValues, getTblById, watchTableChanges} from '../../tables/TableUtil.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {dispatchTableFilter, dispatchTableAddLocal, TABLE_LOADED, TABLE_REPLACE, TABLE_SELECT} from '../../tables/TablesCntlr.js';
import {ColumnConstraintsPanel, getTableConstraints} from './ColumnConstraintsPanel.jsx';
import {ADQL_LINE_LENGTH, maybeQuote} from './TapUtil.js';

const COLS_TO_DISPLAY_FIRST = ['column_name','unit','ucd','description','datatype','arraysize','utype','xtype','principal'];

const SELECT_ALL_COLUMNS_WHEN_NO_PRINCIPAL=false; //todo- determine what to if not of the principal columns are set

export function TableColumnsConstraints({columnsModel}) {

    const tbl_id = get(columnsModel, 'tbl_id');
    const [tableModel, setTableModel] = useState(getTblById(tbl_id));

    useEffect( () => tableEffect(columnsModel, setTableModel, tbl_id), [columnsModel]);

    if (isEmpty(tableModel)) {
        return <div/>;
    } else {
        return <ColumnConstraintsPanel {...{tableModel}} />;
    }
}

export function TableColumnsConstraintsToolbar({columnsModel}) {
    const tbl_id = get(columnsModel, 'tbl_id');
    const [tableModel, setTableModel] = useState(getTblById(tbl_id));
    const filters = get(tableModel, 'request.filters', '');
    const [filterCount, setFilterCount] = useState(filters ? filters.split(';').length : 0);
    const [selectedCount, setSelectedCount] = useState(0);

    useEffect( () => tableEffect(columnsModel, setTableModel, tbl_id, setFilterCount, setSelectedCount), [columnsModel]);

    const {error} = tableModel || {};

    const totalColumns= tableModel?.tableData?.data?.length ?? 0;
    if (isEmpty(tableModel) || !totalColumns) {
        return <div/>;
    }

    const resetButton = () => {
        return (
            <Chip
                    title='Reset Column Selections & Constraints to the default columns and no constraints'
                    onClick={ () => {
                        const tblModel = reorganizeTableModel(columnsModel, COLS_TO_DISPLAY_FIRST, true);
                        dispatchTableAddLocal(tblModel, {}, false);
                    }}>Reset Column Selections & Constraints
            </Chip>
        );
    };


    return (
        <Stack direction='row' alignItems='center' spacing={1}>
            {!error && filterCount > 0 &&
            <Button variant='soft' color='neutral'
                    title='Remove column table filters to make all columns visible'
                    onClick={() => dispatchTableFilter({tbl_id: tableModel.tbl_id, filters: ''})}>
                Remove
                <Typography color='warning' pl={.25}>{filterCount} filter{filterCount>1?'s':''}</Typography>
            </Button>}
            {selectedCount > 0 &&
                <Typography title='Number of columns to be selected' color='warning'  level='body-xs'>
                    {selectedCount} of {totalColumns} columns selected
                </Typography>
            }
            {!error && resetButton()}
        </Stack>
    );
}

function tableEffect(columnsModel, setTableModel, tbl_id, setFilterCount, setSelectedCount) {

    if (!columnsModel || columnsModel.error) {
        setTableModel(columnsModel);
    } else {
        const tbl = reorganizeTableModel(columnsModel, COLS_TO_DISPLAY_FIRST);
        setTableModel(tbl);

        if (setFilterCount || setSelectedCount) {
            if (setSelectedCount) {
                // principal columns are preselected
                const si = get(tbl, 'origTableModel.selectInfo') || get(tbl, 'selectInfo');
                const selectInfoCls = si? new SelectInfo(si) : SelectInfo.newInstance(si);
                setSelectedCount(selectInfoCls.getSelectedCount());
            }
            return watchTableChanges(tbl_id,
                [TABLE_LOADED, TABLE_SELECT],
                (action) => {
                    let selectInfo;
                    if (action.type === TABLE_LOADED) {
                        const {tableModel} = action.payload;
                        // sync filter count with table model
                        if (get(tableModel, 'tbl_id') === tbl_id) {
                            const filterInfo = get(tableModel, 'request.filters', '');
                            setFilterCount && setFilterCount(filterInfo ? filterInfo.split(';').length : 0);
                        }
                        selectInfo = get(tableModel, 'origTableModel.selectInfo');
                    } else {
                        if (action.payload.tbl_id === tbl_id) {
                            selectInfo = get(getTblById(tbl_id), 'origTableModel.selectInfo');
                        }
                    }
                    const selectInfoCls = selectInfo ? new SelectInfo(selectInfo) : SelectInfo.newInstance(selectInfo);
                    setSelectedCount && setSelectedCount(selectInfoCls.getSelectedCount());
                }, `ucd-${tbl_id}-filterCnt`); // watcher id for debugging
        } else {
            return watchTableChanges(tbl_id,
                [TABLE_REPLACE],
                (action) => {
                    const tableModel = action.payload;
                    if (get(tableModel, 'tbl_id') === tbl_id) {
                        setTableModel(tableModel);
                    }
                }, `ucd-${tbl_id}-replace`); // watcher id for debugging
        }
    }
}


/**
 * Returns true, if table model has 'principal' column and at least 3 values in it are not 0
 * @param tableModel
 * @returns {boolean}
 */
function isPrincipalSet(tableModel) {
    return getColumnValues(tableModel, 'principal').filter((v)=>v>0).length >= 1;
}

/**
 * Returns new table model with only the columns we are interested in
 * @param tableModel
 * @param columnNames - columns to show first
 * @param reset - if true, recreate table model
 */
function reorganizeTableModel(tableModel, columnNames, reset) {

    const tbl_id = get(tableModel,'tbl_id');
    let modifiedTableModel = getTblById(tbl_id);
    if (modifiedTableModel && !reset) return modifiedTableModel;

    // keep only the columns that are present in tableModel
    const colNames = columnNames.filter((c) => getColumn(tableModel, c));

    // make sure all original table columns are included, exept table_name and schema_name
    colNames.push(...getColumns(tableModel).map((c)=>c.name).filter((name)=>
        !['table_name', 'schema_name'].includes(name) && !colNames.includes(name))
    );

    const columns = colNames.map((name) => cloneDeep(getColumn(tableModel, name)));

    const data = [];
    const origData = get(tableModel, 'tableData.data', []);
    origData.forEach((r, rIdx) => {
        data.push(colNames.reduce((rval, c) => {
            rval.push(getCellValue(tableModel, rIdx, c));
            return rval;
        }, []));
    });

    // width is often the declared max char in database; remove it, TablePanel will auto-size it
    columns.forEach((col) => Reflect.deleteProperty(col, 'width'));

    // add constraints column
    const constraintsColIdx = 1;
    const constraintsCol = {name: 'constraints', idx: constraintsColIdx, type: 'char', width: 10, fixed: true};
    columns.splice(constraintsColIdx, 0, cloneDeep(constraintsCol));
    data.map((e) => {
        e.splice(constraintsColIdx, 0, '');
    });

    // default selected are either all or the principal rows
    const usingPrincipal= isPrincipalSet(tableModel);
    const selectInfoCls = SelectInfo.newInstance({selectAll:!usingPrincipal, rowCount: data.length});
    if (usingPrincipal) {
        getColumnValues(tableModel, 'principal')
            .reduce((sels, v, i) => {
                if (parseInt(v) === 1) sels.push(i);
                return sels;
            }, [])
            .forEach((idx)=>selectInfoCls.setRowSelect(idx, true));
    }

    columns.forEach((c) => {
        if (c.name==='column_name') {
            c.fixed=true;
            c.label='Name';
            c.prefWidth= 11;
        }
    });
    const selectInfo= (usingPrincipal||SELECT_ALL_COLUMNS_WHEN_NO_PRINCIPAL) ?
        selectInfoCls.data :
        SelectInfo.newInstance({rowCount: data.length}).data;

    modifiedTableModel = {tbl_id, totalRows: data.length, tableData: {columns, data}, selectInfo, request: {tbl_id}};
    return modifiedTableModel;
}

export function makeColsLines(selcolsArrayIn, firstLineOffset=false) {
    const firstOff= firstLineOffset ? '       ' : '';
    const selcolsArray= selcolsArrayIn.map( (c) => maybeQuote(c));
    const colSingleLine= selcolsArray?.join(',') ?? '';
    if (colSingleLine.length < ADQL_LINE_LENGTH) return `${firstOff}${colSingleLine}`;

    let multiLineCols = '';
    let line = `${firstOff}${selcolsArray[0]}`;
    const colsCopy = selcolsArray.slice(1);
    colsCopy.forEach((value) => {
        if (value) line+=',';
        if ((line + value).length > ADQL_LINE_LENGTH){
            multiLineCols+= line + '\n';
            line = '       ';
        }
        line += value;
    });
    multiLineCols += line;
    return multiLineCols;
}

/**
 * Get constraints as ADQL
 * @param {object} columnsModel
 * @param {string} tableName
 * @returns {Object}
 */
export function tableColumnsConstraints(columnsModel,tableName) {
    const tbl_id = columnsModel?.tbl_id;
    if (!tbl_id) {
        return {valid: false, message: 'Unable to retrieve table column constraints'};
    }

    const tableConstraints = getTableConstraints(tbl_id);
    if (!tableConstraints) {
        return {valid: false, message: 'Unable to retrieve table column constraints and selected columns'};
    }
    const {whereFragment, selcolsArray, errors} = tableConstraints;
    if (errors) return {valid: false, message: errors};

    const selcols= tableName ? makeColsLines(selcolsArray.map( (c) => `${tableName}.${c}`)) : makeColsLines(selcolsArray);
    return {valid: true, where: whereFragment, selcols, selcolsArray};
}