import React, {useState, useEffect} from 'react';
import {cloneDeep, get, isEmpty,} from 'lodash';

import {getCellValue, getColumn, getColumns, getColumnValues, getTblById, watchTableChanges} from '../../tables/TableUtil.js';
import {SelectInfo} from '../../tables/SelectInfo.js';
import {dispatchTableFilter, dispatchTableAddLocal, TABLE_LOADED, TABLE_REPLACE, TABLE_SELECT} from '../../tables/TablesCntlr.js';
import {ColumnConstraintsPanel, getTableConstraints} from './ColumnConstraintsPanel.jsx';
import {ADQL_LINE_LENGTH} from './TapUtil.js';

const COLS_TO_DISPLAY_FIRST = ['column_name','unit','ucd','description','datatype','arraysize','utype','xtype','principal'];


export function TableColumnsConstraints({columnsModel}) {

    const tbl_id = get(columnsModel, 'tbl_id');
    const [tableModel, setTableModel] = useState(getTblById(tbl_id));

    useEffect( () => tableEffect(columnsModel, setTableModel, tbl_id), [columnsModel]);

    if (isEmpty(tableModel)) {
        return <div/>;
    } else {
        return <ColumnConstraintsPanel style={{height: '100%'}} {...{tableModel}} />;
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
            <button style={{padding: '0 5px 0 5px', margin: '0 2px 0 5px'}}
                    title='Reset Column Selections & Constraints to the default columns and no constraints'
                    onClick={ () => {
                        const tblModel = reorganizeTableModel(columnsModel, COLS_TO_DISPLAY_FIRST, true);
                        dispatchTableAddLocal(tblModel, {}, false);
                    }}>Reset Column Selections & Constraints
            </button>
        );
    };


    return (
        <div style={{display:'inline-flex', padding:'0 0 2px', height: 20, alignSelf: 'center'}}>
            {!error && filterCount > 0 &&
            <button style={{padding: '0 5px 0 5px', margin: '0 2px 0 5px'}}
                    title='Remove column table filters to make all columns visible'
                    onClick={() => dispatchTableFilter({tbl_id: tableModel.tbl_id, filters: ''})}>
                Remove <span style={{color: 'blue'}}>{filterCount} filter{filterCount>1?'s':''}</span>
            </button>}
            {selectedCount > 0 &&
            <span style={{color: 'blue', alignSelf: 'center', padding: '0 5px 0 5px'}} title='Number of columns to be selected'>
                {selectedCount} of {totalColumns} columns selected
            </span>}
            {!error && resetButton()}
        </div>
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
    // 3 or more principle columns (coordinates and value) might be useful
    return getColumnValues(tableModel, 'principal').filter((v)=>v>0).length >= 3;
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
    const constraintsCol = {name: 'constraints', idx: constraintsColIdx, type: 'char', width: 10};
    columns.splice(constraintsColIdx, 0, cloneDeep(constraintsCol));
    data.map((e) => {
        e.splice(constraintsColIdx, 0, '');
    });

    // set selections
    const selectInfoCls = SelectInfo.newInstance({rowCount: data.length});
    if (selectInfoCls.getSelectedCount() === 0) {
        let defaultSelected = [];
        // default selected are the principal rows
        if (isPrincipalSet(tableModel)) {
            defaultSelected = getColumnValues(tableModel, 'principal').reduce((sels, v, i) => {
                if (parseInt(v) === 1) sels.push(i);
                return sels;
            }, []);
        }
        defaultSelected.forEach((idx)=>selectInfoCls.setRowSelect(idx, true));
    }

    modifiedTableModel = {tbl_id, totalRows: data.length, tableData: {columns, data},
        selectInfo: selectInfoCls.data, request: {tbl_id}};

    return modifiedTableModel;
}

function makeColsLines(selcolsArray) {
    const colSingleLine= selcolsArray?.join(',') ?? '';
    if (colSingleLine.length < ADQL_LINE_LENGTH) return colSingleLine;

    let multiLineCols = '';
    let line = selcolsArray[0];
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
 * @returns {AdqlFragment}
 */
export function tableColumnsConstraints(columnsModel) {
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

    const selcols= makeColsLines(selcolsArray);
    return {valid: true, where: whereFragment, selcols, selcolsArray};
}